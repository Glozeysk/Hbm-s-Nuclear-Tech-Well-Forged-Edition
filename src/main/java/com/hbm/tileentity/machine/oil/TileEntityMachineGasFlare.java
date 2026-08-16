package com.hbm.tileentity.machine.oil;

import api.hbm.energy.IBatteryItem;
import api.hbm.energy.IEnergyGenerator;
import com.hbm.entity.particle.EntityGasFlameFX;
import com.hbm.explosion.ExplosionThermo;
import com.hbm.forgefluid.FFUtils;
import com.hbm.forgefluid.ModForgeFluids;
import com.hbm.interfaces.IControlReceiver;
import com.hbm.inventory.EngineRecipes;
import com.hbm.inventory.EngineRecipes.FuelGrade;
import com.hbm.inventory.UpgradeManager;
import com.hbm.inventory.container.ContainerMachineGasFlare;
import com.hbm.inventory.gui.GUIMachineGasFlare;
import com.hbm.items.ModItems;
import com.hbm.items.machine.ItemMachineUpgrade;
import com.hbm.items.machine.ItemMachineUpgrade.UpgradeType;
import com.hbm.lib.HBMSoundHandler;
import com.hbm.lib.Library;
import com.hbm.main.MainRegistry;
import com.hbm.sound.AudioWrapper;
import com.hbm.tileentity.IBufPacketReceiver;
import com.hbm.tileentity.IGUIProvider;
import com.hbm.tileentity.TileEntityMachineBase;

import io.netty.buffer.ByteBuf;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ITickable;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidTank;
import net.minecraftforge.fluids.FluidUtil;
import net.minecraftforge.fluids.capability.CapabilityFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.IFluidTankProperties;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.HashMap;

public class TileEntityMachineGasFlare extends TileEntityMachineBase implements ITickable, IEnergyGenerator, IFluidHandler, IGUIProvider, IControlReceiver, IBufPacketReceiver {
	public long power;
	public static final long maxPower = 1000000;
	public FluidTank tank;
	public boolean isOn = false;
	public boolean doesBurn = false;
	public boolean needsUpdate;

	private final UpgradeManager upgradeManager = new UpgradeManager();

	@SideOnly(Side.CLIENT)
	private AudioWrapper audio;

	public static HashMap<FuelGrade, Double> fuelEfficiency = new HashMap();
	static {
		fuelEfficiency.put(FuelGrade.GAS, 1.0D);
	}

	public TileEntityMachineGasFlare() {
		super(6);
		tank = new FluidTank(64000);
		needsUpdate = false;
	}

	public static long getHEFromFuel(Fluid type) {
		if(EngineRecipes.hasFuelRecipe(type)) {
			FuelGrade grade = EngineRecipes.getFuelGrade(type);
			double efficiency = fuelEfficiency.containsKey(grade) ? fuelEfficiency.get(grade) : 0;
			return (long) (EngineRecipes.getEnergy(type) / 1000L * efficiency);
		}

		return 0;
	}

	public long getHEFromFuel() {
		if(tank.getFluid() == null) return 0;
		return getHEFromFuel(tank.getFluid().getFluid());
	}

	public boolean hasAcceptableFuel() {
		return getHEFromFuel() > 0;
	}

	@Override
	public String getName() {
		return "container.gasFlare";
	}

	public boolean isUseableByPlayer(EntityPlayer player) {
		if(world.getTileEntity(pos) != this)
		{
			return false;
		}else{
			return player.getDistanceSq(pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D) <=128;
		}
	}

	@Override
	public void readFromNBT(NBTTagCompound compound) {
		this.power = compound.getLong("powerTime");
		tank.readFromNBT(compound);
		isOn = compound.getBoolean("isOn");
		doesBurn = compound.getBoolean("doesBurn");
		super.readFromNBT(compound);
	}

	@Override
	public NBTTagCompound writeToNBT(NBTTagCompound compound) {
		compound.setLong("powerTime", power);
		tank.writeToNBT(compound);
		compound.setBoolean("isOn", isOn);
		compound.setBoolean("doesBurn", doesBurn);
		return super.writeToNBT(compound);
	}

	public long getPowerScaled(long i) {
		return (power * i) / maxPower;
	}

	@Override
	public void update() {

		if(!world.isRemote) {

			this.sendPower(world, pos.add(2, 0, 0), Library.POS_X);
			this.sendPower(world, pos.add(-2, 0, 0), Library.NEG_X);
			this.sendPower(world, pos.add(0, 0, 2), Library.POS_Z);
			this.sendPower(world, pos.add(0, 0, -2), Library.NEG_Z);

			long prevPower = power;
			int prevAmount = tank.getFluidAmount();
			if(needsUpdate) {
				needsUpdate = false;
			}

			if(this.inputValidForTank(1))
				if(FFUtils.fillFromFluidContainer(inventory, tank, 1, 2))
					needsUpdate = true;

			int maxVent = 50;
			int maxBurn = 10;

			if(isOn && tank.getFluidAmount() >= 10) {
				upgradeManager.eval(inventory, 4, 5);

				int burn = Math.min(upgradeManager.getLevel(UpgradeType.SPEED), 6);
				int yield = Math.min(upgradeManager.getLevel(UpgradeType.EFFECT), 6);

				maxVent += maxVent * burn;
				maxBurn += maxBurn * burn;

				long energyPerUnit = getHEFromFuel();

				if (doesBurn && energyPerUnit > 0) {
					int eject = Math.min(maxBurn, tank.getFluidAmount());
					tank.drain(eject, true);
					needsUpdate = true;

					long powerGen = energyPerUnit * eject;
					powerGen += powerGen * yield / 3;

					this.power += powerGen;
					if (this.power > maxPower) {
						this.power = maxPower;
					}

					world.spawnEntity(new EntityGasFlameFX(world, pos.getX() + 0.5F, pos.getY() + 11F, pos.getZ() + 0.5F, 0.0, 0.0, 0.0));
					ExplosionThermo.setEntitiesOnFire(world, pos.getX(), pos.getY() + 11, pos.getZ(), 5);
				} else {
					tank.drain(maxVent, true);
					needsUpdate = true;
				}
			}

			power = Library.chargeItemsFromTE(inventory, 0, power, maxPower);

			networkPackNT(25);

			if(prevPower != power || prevAmount != tank.getFluidAmount() || needsUpdate){
				markDirty();
			}
		} else {
			boolean isRunning = isOn && doesBurn && hasAcceptableFuel() && tank.getFluidAmount() >= 10;
			float volume = this.getVolume(2);

			if(isRunning && volume > 0) {
				if(audio == null) {
					audio = MainRegistry.proxy.getLoopedSound(HBMSoundHandler.flare_operate, SoundCategory.BLOCKS, pos.getX(), pos.getY(), pos.getZ(), volume, 1.0F);
					audio.updateRange(32.0F);
					audio.startSound();
				}
			} else {
				if(audio != null) {
					audio.stopSound();
					audio = null;
				}
			}
		}
	}

	@Override
	public void serialize(ByteBuf buf) {
		buf.writeBoolean(isOn);
		buf.writeBoolean(doesBurn);
		buf.writeLong(power);
		ByteBufUtils.writeTag(buf, tank.writeToNBT(new NBTTagCompound()));
	}

	@Override
	public void deserialize(ByteBuf buf) {
		this.isOn = buf.readBoolean();
		this.doesBurn = buf.readBoolean();
		this.power = buf.readLong();
		tank.readFromNBT(ByteBufUtils.readTag(buf));
	}

	@Override
	public int[] getAccessibleSlotsFromSide(EnumFacing e) {
		return new int[] {0, 1, 2, 3, 4, 5};
	}

	@Override
	public boolean canInsertItem(int slot, ItemStack itemStack, int amount) {
		return this.isItemValidForSlot(slot, itemStack);
	}

	@Override
	public boolean isItemValidForSlot(int i, ItemStack stack) {
		if(stack.getItem() instanceof IBatteryItem || stack.getItem() == ModItems.battery_creative){
			return i == 0;
		}
		if(stack.getItem() instanceof ItemMachineUpgrade){
			return i == 4 || i == 5;
		}
		return i == 1 || i == 2;
	}

	protected boolean inputValidForTank(int slot){
		if(!inventory.getStackInSlot(slot).isEmpty()){
			if(isValidFluid(FluidUtil.getFluidContained(inventory.getStackInSlot(slot)))){
				return true;
			}
		}
		return false;
	}

	private boolean isValidFluid(FluidStack stack) {
		if(stack == null)
			return false;
		return getHEFromFuel(stack.getFluid()) > 0;
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
	public IFluidTankProperties[] getTankProperties() {
		return new IFluidTankProperties[]{tank.getTankProperties()[0]};
	}

	@Override
	public int fill(FluidStack resource, boolean doFill) {
		if (isValidFluid(resource)) {
			return tank.fill(resource, doFill);
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
	public boolean hasCapability(Capability<?> capability, EnumFacing facing) {
		if(capability == CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY){
			return true;
		} else {
			return super.hasCapability(capability, facing);
		}
	}

	@Override
	public <T> T getCapability(Capability<T> capability, EnumFacing facing) {
		if(capability == CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY){
			return CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY.cast(this);
		} else {
			return super.getCapability(capability, facing);
		}
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

	@Override
	public Container provideContainer(int ID, EntityPlayer player, World world, int x, int y, int z) {
		return new ContainerMachineGasFlare(player.inventory, this);
	}

	@Override
	@SideOnly(Side.CLIENT)
	public GuiScreen provideGUI(int ID, EntityPlayer player, World world, int x, int y, int z) {
		return new GUIMachineGasFlare(player.inventory, this);
	}

	@Override
	public boolean hasPermission(EntityPlayer player) {
		return player.getDistanceSq(pos) <= 256D;
	}

	@Override
	public void receiveControl(NBTTagCompound data) {
		if(data.hasKey("valve")) this.isOn = !this.isOn;
		if(data.hasKey("dial")) this.doesBurn = !this.doesBurn;
		markDirty();
	}

	@Override
	public void onChunkUnload() {
		super.onChunkUnload();
		if(audio != null) {
			audio.stopSound();
			audio = null;
		}
	}

	@Override
	public void invalidate() {
		super.invalidate();
		if(audio != null) {
			audio.stopSound();
			audio = null;
		}
	}
}