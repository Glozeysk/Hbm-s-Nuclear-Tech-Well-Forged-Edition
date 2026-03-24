package com.hbm.tileentity.machine;

import java.util.HashMap;
import java.util.Map;

import com.google.common.collect.HashBiMap;
import com.hbm.blocks.BlockDummyable;
import com.hbm.forgefluid.FFUtils;
import com.hbm.forgefluid.ModForgeFluids;
import com.hbm.handler.MultiblockHandlerXR;
import com.hbm.items.ModItems;
import com.hbm.lib.ForgeDirection;
import com.hbm.lib.Library;
import com.hbm.tileentity.IBufPacketReceiver;
import com.hbm.tileentity.TileEntityMachineBase;

import api.hbm.energy.IEnergyGenerator;
import io.netty.buffer.ByteBuf;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityFurnace;
import net.minecraft.util.ITickable;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidTank;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.IFluidTankProperties;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public class TileEntityMachineIGenerator extends TileEntityMachineBase implements ITickable, IEnergyGenerator, IFluidHandler, IBufPacketReceiver {

	public long power;
	public static final long maxPower = 1000000;
	public int lastBurnTime;
	public int burnTime;
	public int temperature;
	public static final int maxTemperature = 1000;
	public int torque;
	public static final int maxTorque = 10000;
	public float limiter = 0.0F; /// 0 - 1 ///
	
	public static final int animSpeed = 50;

	@SideOnly(Side.CLIENT)
	public float rotation;
	@SideOnly(Side.CLIENT)
	public float prevRotation;
	
	public IGenRTG[] pellets = new IGenRTG[12];
	public FluidTank[] tanks;
	public Fluid[] tankTypes;

	private int displayHeat;
	
	public TileEntityMachineIGenerator() {
		super(13);
		tanks = new FluidTank[3];
		tanks[0] = new FluidTank(16000);
		tanks[1] = new FluidTank(16000);
		tanks[2] = new FluidTank(4000);
		tankTypes = new Fluid[3];
		tankTypes[0] = FluidRegistry.WATER;
		tankTypes[1] = ModForgeFluids.heatingoil;
		tankTypes[2] = ModForgeFluids.lubricant;
		
	}

	@Override
	public String getName() {
		return "container.iGenerator";
	}
	
	@Override
	public void update() {
		if(!world.isRemote) {
			
			this.sendIGenPower();

			FFUtils.fillFromFluidContainer(inventory, tanks[0], 7, 8);
			if(FFUtils.checkRestrictions(inventory.getStackInSlot(9), stack -> {
				if(stack != null && fluidHeat.containsKey(stack.getFluid())){
					return fluidHeat.get(stack.getFluid()) > 0;
				}
				return false;
			}))
				FFUtils.fillFromFluidContainer(inventory, tanks[1], 9, 10);
			FFUtils.fillFromFluidContainer(inventory, tanks[2], 11, 12);
			
			loadFuel();
			pelletAction();
			
			if(burnTime > 0) {
				burnTime --;
				temperature += 100;
			}
			
			fuelAction();
			
			if(temperature > maxTemperature)
				temperature = maxTemperature;
			
			displayHeat = temperature;
			
			rtgAction();
			
			rotorAction();
			generatorAction();
			
			this.power = Library.chargeItemsFromTE(inventory, 6, power, maxPower);

			networkPackNT(250);
			
		} else {
			
			this.prevRotation = this.rotation;
			
			this.rotation += this.torque * animSpeed / maxTorque;
			
			if(this.rotation >= 360) {
				this.rotation -= 360;
				this.prevRotation -= 360;
			}
		}
	}

	@Override
	public void serialize(ByteBuf buf) {
		for(int i = 0; i < pellets.length; i++) {
			buf.writeInt(pellets[i] != null ? pellets[i].ordinal() : -1);
		}
		buf.writeInt(displayHeat);
		buf.writeInt(torque);
		buf.writeInt((int) power);
		buf.writeShort(burnTime);
		buf.writeShort(lastBurnTime);
		buf.writeFloat(limiter);

		for(int i = 0; i < 3; i++) {
			FluidStack fs = tanks[i].getFluid();
			buf.writeBoolean(fs != null);
			if(fs != null) {
				ByteBufUtils.writeUTF8String(buf, fs.getFluid().getName());
				buf.writeInt(fs.amount);
			}
		}
	}

	@Override
	public void deserialize(ByteBuf buf) {
		for(int i = 0; i < pellets.length; i++) {
			int pellet = buf.readInt();
			if(pellet >= 0 && pellet < IGenRTG.values().length) {
				pellets[i] = IGenRTG.values()[pellet];
			} else {
				pellets[i] = null;
			}
		}

		this.temperature = buf.readInt();
		this.torque = buf.readInt();
		this.power = buf.readInt();
		this.burnTime = buf.readShort();
		this.lastBurnTime = buf.readShort();

		float dialValue = buf.readFloat();
		if(ignoreNext <= 0) {
			this.limiter = dialValue;
		} else {
			ignoreNext--;
		}

		for(int i = 0; i < 3; i++) {
			boolean hasFluid = buf.readBoolean();
			if(hasFluid) {
				String fluidName = ByteBufUtils.readUTF8String(buf);
				int amount = buf.readInt();
				Fluid fluid = FluidRegistry.getFluid(fluidName);
				if(fluid != null) {
					tanks[i].setFluid(new FluidStack(fluid, amount));
				} else {
					tanks[i].setFluid(null);
				}
			} else {
				tanks[i].setFluid(null);
			}
		}
	}
	
	@Override
	public void handleButtonPacket(int value, int meta) {
		if(meta == 0)
			pushPellet();
		if(meta == 1)
			popPellet();
		if(meta == 2)
			setDialByAngle(value);
	}
	
	/**
	 * Checks for solid fuel and burns it
	 */
	private void loadFuel() {
		
		if(this.burnTime <= 0) {
			
			int time = TileEntityFurnace.getItemBurnTime(inventory.getStackInSlot(0)) / 2;
			
			if(time > 0) {
				
				if(inventory.getStackInSlot(0).getItem().hasContainerItem(inventory.getStackInSlot(0)) && inventory.getStackInSlot(0).getCount() == 1) {
					inventory.setStackInSlot(0, inventory.getStackInSlot(0).getItem().getContainerItem(inventory.getStackInSlot(0)));
				} else {
					inventory.getStackInSlot(0).shrink(1);
				}
				
				this.burnTime = time;
				this.lastBurnTime = time;
				
				this.markDirty();
			}
		}
	}
	
	/**
	 * Creates heat from RTG pellets
	 */
	private void pelletAction() {
		
		for(int i = 0; i < pellets.length; i++) {
			if(pellets[i] != null)
				this.temperature += pellets[i].heat;
		}
	}
	
	/**
	 * Burns liquid fuel
	 */
	private void fuelAction() {
		
		int heat = getHeatFromFuel(tanks[1].getFluid());
		
		int maxBurn = 2;
		
		if(tanks[1].getFluidAmount() > 0) {
			
			int burn = Math.min(maxBurn, tanks[1].getFluidAmount());
			
			tanks[1].drain(burn, true);
			temperature += heat * burn;
		}
	}
	
	public static final Map<Fluid, Integer> fluidHeat = new HashMap<>();
	
	static {
		fluidHeat.put(ModForgeFluids.smear, 75);
		fluidHeat.put(ModForgeFluids.heatingoil, 150);
		fluidHeat.put(ModForgeFluids.diesel, 225);
		fluidHeat.put(ModForgeFluids.kerosene, 300);
		fluidHeat.put(ModForgeFluids.reclaimed, 100);
		fluidHeat.put(ModForgeFluids.petroil, 125);
		fluidHeat.put(ModForgeFluids.biofuel, 200);
		fluidHeat.put(ModForgeFluids.nitan, 2500);
		fluidHeat.put(ModForgeFluids.sparkfuel, 5000);
	}
	
	public int getHeatFromFuel(FluidStack fluid) {
		if(fluid == null)
			return 0;
		return fluidHeat.get(fluid.getFluid());
	}
	
	/**
	 * does the thing with the thermo elements
	 */
	private void rtgAction() {
		
		int rtg = 0;
		
		for(int i = 3; i <= 5; i++) {
			
			if(inventory.getStackInSlot(i).getItem() == ModItems.thermo_element)
				rtg += 15;
		}
		
		int pow = Math.min(this.temperature, rtg);
		
		this.temperature -= pow;
		this.power += pow;
		
		if(power > maxPower)
			power = maxPower;
	}

	/**
	 * Turns heat into rotational energy
	 */
	private void rotorAction() {
		
		int conversion = getConversion();
		
		if(temperature > 10 && tanks[0].getFluidAmount() > 0)
			tanks[0].drain(1, true);
		
		if(torque > 10 && tanks[2].getFluidAmount() > 0 && world.rand.nextInt(2) == 0)
			tanks[2].drain(1, true);
		
		this.torque += conversion * (tanks[0].getFluidAmount() > 0 ? 1.5 : 1);
		this.temperature -= conversion;
		
		if(torque > maxTorque)
			world.createExplosion(null, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 5, true);
	}
	
	/**
	 * Do the power stuff
	 */
	private void generatorAction() {
		
		double balanceFactor = 0.25D;
		
		this.power += this.torque * balanceFactor;
		torque -= getBrake();
		
		if(power > maxPower)
			power = maxPower;
	}
	
	public int getBrake() {
		return (int) Math.ceil(torque * 0.1 * (tanks[2].getFluidAmount() > 0 ? 0.5 : 1));
	}
	
	public int getConversion() {
		return (int) (temperature * limiter);
	}
	
	/**
	 * Adds a pellet onto the pile
	 */
	private void pushPellet() {
		
		if(pellets[11] != null)
			return;
		
		if(!inventory.getStackInSlot(1).isEmpty()) {
			
			IGenRTG pellet = IGenRTG.getPellet(inventory.getStackInSlot(1).getItem());
			
			if(pellet != null) {
				
				for(int i = 0; i < pellets.length; i++) {
					
					if(pellets[i] == null) {
						pellets[i] = pellet;
						inventory.getStackInSlot(1).shrink(1);
						
						this.markDirty();
						return;
					}
				}
			}
		}
	}
	
	/**
	 * Removes a pellet from the bottom of the pile
	 */
	private void popPellet() {
		
		if(!inventory.getStackInSlot(2).isEmpty())
			return;
		
		if(pellets[0] == null)
			return;
		
		inventory.setStackInSlot(2, new ItemStack(rtgPellets.inverse().get(pellets[0])));
		
		for(int i = 0; i < pellets.length - 1; i++) {
			pellets[i] = pellets[i + 1];
		}
		
		pellets[pellets.length - 1] = null;
		
		this.markDirty();
	}
	
	public double getSolidGauge() {
		return (double) burnTime / (double) lastBurnTime;
	}
	
	public double getPowerGauge() {
		return (double) power / (double) maxPower;
	}
	
	public double getTempGauge() {
		return (double) temperature / (double) maxTemperature;
	}
	
	public double getTorqueGauge() {
		return (double) torque / (double) maxTorque;
	}
	
	public float getAngleFromDial() {
		return (45F + limiter * 270F) % 360F;
	}
	
	int ignoreNext = 0;
	public void setDialByAngle(float angle) {
		this.limiter = (angle - 45F) / 270F;
		ignoreNext = 5;
	}
	
	public void sendIGenPower() {
		ForgeDirection dir = ForgeDirection.getOrientation(this.getBlockMetadata() - BlockDummyable.offset);
		
		int[] rot = MultiblockHandlerXR.rotate(new int [] {1,0,4,0,8,8}, dir.toEnumFacing());
		
		for(int ix = -rot[4]; ix <= rot[5]; ix++) {
			for(int iz = -rot[2]; iz <= rot[3]; iz++) {
				if(ix == -rot[4] || ix == rot[5] || iz == -rot[2] || iz == rot[3]) {
					this.sendPower(world, pos.add(dir.offsetX * 2 + ix, -1, dir.offsetZ * 2 + iz), ForgeDirection.DOWN);
				}
			}
		}
	}

	@Override
	public IFluidTankProperties[] getTankProperties() {
		return new IFluidTankProperties[]{tanks[0].getTankProperties()[0], tanks[1].getTankProperties()[0], tanks[2].getTankProperties()[0]};
	}

	@Override
	public int fill(FluidStack resource, boolean doFill) {
		if(resource != null){
			if(resource.getFluid() == tankTypes[0]){
				return tanks[0].fill(resource, doFill);
			} else if(fluidHeat.get(resource.getFluid()) > 0){
				return tanks[1].fill(resource, doFill);
			} else if(resource.getFluid() == tankTypes[2]){
				return tanks[2].fill(resource, doFill);
			}
		}
		return 0;
	}

	@Override
	public FluidStack drain(FluidStack resource, boolean doDrain) {
		return null;
	}

	@Override
	public FluidStack drain(int maxDrain, boolean doDrain) {
		return null;
	}
	
	@Override
	public void readFromNBT(NBTTagCompound nbt) {
		nbt.setTag("tanks", FFUtils.serializeTankArray(tanks));
		for(int i = 0; i < pellets.length; i++) {
			
			short s = nbt.getShort("pellet" + i);
			
			if(s >= 0 && s < IGenRTG.values().length) {
				pellets[i] = IGenRTG.values()[s];
			} else {
				pellets[i] = null;
			}
		}

		this.burnTime = nbt.getInteger("burn");
		this.lastBurnTime = nbt.getInteger("lastBurn");
		this.limiter = nbt.getFloat("limiter");
		super.readFromNBT(nbt);
	}
	
	@Override
	public NBTTagCompound writeToNBT(NBTTagCompound nbt) {
		FFUtils.deserializeTankArray(nbt.getTagList("tanks", 10), tanks);
		for(int i = 0; i < pellets.length; i++) {
			
			if(pellets[i] != null) {
				nbt.setShort("pellet" + i, (short) pellets[i].ordinal());
			} else {
				nbt.setShort("pellet" + i, (short)-1);
			}
		}
		
		nbt.setInteger("burn", burnTime);
		nbt.setInteger("lastBurn", lastBurnTime);
		nbt.setFloat("limiter", limiter);
		return super.writeToNBT(nbt);
	}
	
	private static HashBiMap<Item, IGenRTG> rtgPellets = HashBiMap.create();
	
	public static enum IGenRTG {
		RADIUM(ModItems.pellet_rtg_radium, 9, 3),
		URANIUM(ModItems.pellet_rtg_weak, 9, 5),
		PLUTONIUM(ModItems.pellet_rtg, 18, 10),
		STRONTIUM(ModItems.pellet_rtg_strontium, 18, 12),
		COBALT(ModItems.pellet_rtg_cobalt, 18, 16),
		ACTINIUM(ModItems.pellet_rtg_actinium, 0, 20),
		AMERICIUM(ModItems.pellet_rtg_americium, 0, 25);
		
		public int offset;
		public int heat;
		
		private IGenRTG(Item item, int offset, int heat) {
			rtgPellets.put(item, this);
			this.offset = offset;
			this.heat = heat;
		}
		
		public static IGenRTG getPellet(Item item) {
			return rtgPellets.get(item);
		}
	}
	
	@Override
	public AxisAlignedBB getRenderBoundingBox() {
		return TileEntity.INFINITE_EXTENT_AABB;
	}
	
	@Override
	@SideOnly(Side.CLIENT)
	public double getMaxRenderDistanceSquared()
	{
		return 65536.0D;
	}

	@Override
	public long getPower() {
		return power;
	}

	@Override
	public void setPower(long i) {
		power = i;
	}

	@Override
	public long getMaxPower() {
		return maxPower;
	}
}