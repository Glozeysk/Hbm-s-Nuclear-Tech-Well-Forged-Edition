package com.hbm.tileentity.machine.oil;

import com.hbm.forgefluid.FFUtils;
import com.hbm.forgefluid.ModForgeFluids;
import com.hbm.inventory.RefineryRecipes;
import com.hbm.lib.ForgeDirection;
import com.hbm.util.Tuple.Quartet;
import com.hbm.tileentity.IBufPacketReceiver;
import com.hbm.tileentity.TileEntityLoadedBase;

import api.hbm.tile.IHeatSource;

import io.netty.buffer.ByteBuf;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ITickable;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidTank;
import net.minecraftforge.fluids.capability.CapabilityFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.IFluidTankProperties;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public class TileEntityMachineFractionTower extends TileEntityLoadedBase implements IBufPacketReceiver, ITickable, IFluidHandler {
	
	public FluidTank[] tanks;
	public Fluid[] types;

	public int heat = 0;
	public static final int maxHeat = 200_000;
	//per tower per fractionate call, and that call drains 100mB once a second
	public static final int heatPerOp = 3_000;
	public static final double diffusion = 0.05D;
	//towers are 3 blocks tall, a stacked one sits exactly this far up
	private static final int stackStep = 3;
	private static final int maxChain = 64;

	public TileEntityMachineFractionTower() {
		super();
		
		tanks = new FluidTank[3];
		types = new Fluid[3];
		types[0] = ModForgeFluids.heavyoil;
		types[1] = ModForgeFluids.bitumen;
		types[2] = ModForgeFluids.smear;
		tanks[0] = new FluidTank(ModForgeFluids.heavyoil, 0, 4000);
		tanks[1] = new FluidTank(ModForgeFluids.bitumen, 0, 4000);
		tanks[2] = new FluidTank(ModForgeFluids.smear, 0, 4000);
	}
	
	public void setTankType(int idx, Fluid type){
		if(types[idx] != type){
			types[idx] = type;
			if(type != null){
				tanks[idx].setFluid(new FluidStack(type, 0));
			}else {
				tanks[idx].setFluid(null);
			}
		}
	}
	
	@Override
	public void update() {

		if(!world.isRemote) {

			this.heat *= 0.999;
			this.tryPullHeat();

			TileEntity stack = world.getTileEntity(pos.up(stackStep));


			if(stack instanceof TileEntityMachineFractionTower) {
				TileEntityMachineFractionTower frac = (TileEntityMachineFractionTower) stack;

				//the stack shares one heat pool, otherwise only the bottom tower could ever pay for an operation
				int pool = this.heat + frac.heat;
				this.heat = pool - pool / 2;
				frac.heat = pool / 2;

				//make types equal
				for(int i = 0; i < 3; i++) {
					frac.setTankType(i, types[i]);
				}
				
				//the feedstock is levelled out like the heat, so every tower runs dry at the same time
				if(types[0] != null) {
					int move = (tanks[0].getFluidAmount() - frac.tanks[0].getFluidAmount()) / 2;

					if(move > 0) {
						move = Math.min(move, frac.tanks[0].getCapacity() - frac.tanks[0].getFluidAmount());
						if(move > 0) {
							tanks[0].drain(move, true);
							frac.tanks[0].fill(new FluidStack(frac.types[0], move), true);
						}
					} else if(move < 0) {
						move = Math.min(-move, tanks[0].getCapacity() - tanks[0].getFluidAmount());
						if(move > 0) {
							frac.tanks[0].drain(move, true);
							tanks[0].fill(new FluidStack(types[0], move), true);
						}
					}
				}

				//the products still go all the way down, they leave the stack at the bottom
				int left = Math.min(frac.tanks[1].getFluidAmount(), tanks[1].getCapacity() - tanks[1].getFluidAmount());
				int right = Math.min(frac.tanks[2].getFluidAmount(), tanks[2].getCapacity() - tanks[2].getFluidAmount());

				tanks[1].fill(new FluidStack(types[1], left), true);
				tanks[2].fill(new FluidStack(types[2], right), true);
				frac.tanks[1].drain(left, true);
				frac.tanks[2].drain(right, true);
			}
			
			setupTanks();
			
			if(world.getTotalWorldTime() % 20 == 0)
				fractionate();
			
			if(world.getTotalWorldTime() % 10 == 0) {
				fillFluidInit(tanks[1]);
				fillFluidInit(tanks[2]);
			}

			networkPackNT(25);
		}
	}

	@Override
	public void serialize(ByteBuf buf) {
		for(int i = 0; i < tanks.length; i++) {
			if(types[i] != null) {
				buf.writeBoolean(true);
				ByteBufUtils.writeUTF8String(buf, types[i].getName());
				buf.writeInt(tanks[i].getFluidAmount());
			} else {
				buf.writeBoolean(false);
			}
		}
		buf.writeInt(heat);
	}

	@Override
	public void deserialize(ByteBuf buf) {
		for(int i = 0; i < tanks.length; i++) {
			if(buf.readBoolean()) {
				String fluidName = ByteBufUtils.readUTF8String(buf);
				int amount = buf.readInt();
				Fluid fluid = FluidRegistry.getFluid(fluidName);
				types[i] = fluid;
				if(fluid != null) {
					tanks[i].setFluid(new FluidStack(fluid, amount));
				} else {
					tanks[i].setFluid(null);
				}
			} else {
				types[i] = null;
				tanks[i].setFluid(null);
			}
		}
		this.heat = buf.readInt();
	}
	
	private void setupTanks() {
		
		Quartet<Fluid, Fluid, Integer, Integer> quart = RefineryRecipes.getFractions(types[0]);
		
		if(quart != null) {
			setTankType(1, quart.getW());
			setTankType(2, quart.getX());
		}
	}
	
	private void fractionate() {
		
		Quartet<Fluid, Fluid, Integer, Integer> quart = RefineryRecipes.getFractions(types[0]);
		
		if(quart != null) {
			
			int left = quart.getY();
			int right = quart.getZ();
			
			if(heat >= heatPerOp && tanks[0].getFluidAmount() >= 100 && hasSpace(left, right)) {
				tanks[0].drain(100, true);
				tanks[1].fill(new FluidStack(types[1], left), true);
				tanks[2].fill(new FluidStack(types[2], right), true);
				heat -= heatPerOp;
			}
		}
	}
	
	//only the bottom tower can reach a heater, everything above it sits on another tower
	private void tryPullHeat() {

		if(this.heat >= maxHeat) return;

		TileEntity con = world.getTileEntity(pos.down());

		if(con instanceof IHeatSource source) {
			int diff = source.getHeatStored() - this.heat;

			if(diff > 0) {
				diff = (int) Math.ceil(diff * diffusion);
				source.useUpHeat(diff);
				this.heat = Math.min(this.heat + diff, maxHeat);
			}
		}
	}

	public TileEntityMachineFractionTower getBottomTower() {

		TileEntityMachineFractionTower bottom = this;

		for(int i = 0; i < maxChain; i++) {
			TileEntity te = world.getTileEntity(bottom.pos.down(stackStep));
			if(!(te instanceof TileEntityMachineFractionTower)) break;
			bottom = (TileEntityMachineFractionTower) te;
		}

		return bottom;
	}

	/** {stored heat, tower count} of the whole stack this tower belongs to */
	public int[] getChainHeat() {

		int stored = 0;
		int count = 0;
		TileEntityMachineFractionTower tower = getBottomTower();

		while(tower != null && count < maxChain) {
			stored += tower.heat;
			count++;
			TileEntity te = world.getTileEntity(tower.pos.up(stackStep));
			tower = te instanceof TileEntityMachineFractionTower ? (TileEntityMachineFractionTower) te : null;
		}

		return new int[] { stored, count };
	}

	private boolean hasSpace(int left, int right) {
		return tanks[1].getFluidAmount() + left <= tanks[1].getCapacity() && tanks[2].getFluidAmount() + right <= tanks[2].getCapacity();
	}
	
	@Override
	public void readFromNBT(NBTTagCompound nbt) {
		super.readFromNBT(nbt);
		heat = nbt.getInteger("heat");
		FFUtils.deserializeTankArray(nbt.getTagList("tanks", 10), tanks);
		for(int i=0; i<tanks.length; i++){
			if(tanks[i].getFluid() != null){
				types[i] = tanks[i].getFluid().getFluid();
			} else {
				types[i] = null;
			}
		}
	}
	
	@Override
	public NBTTagCompound writeToNBT(NBTTagCompound nbt) {
		super.writeToNBT(nbt);
		for(int i=0; i<tanks.length; i++){
			if(types[i] != null){
				tanks[i].setFluid(new FluidStack(types[i], tanks[i].getFluidAmount()));
			} else {
				tanks[i].setFluid(null);
			}
		}
		nbt.setTag("tanks", FFUtils.serializeTankArray(tanks));
		nbt.setInteger("heat", heat);
		return nbt;
	}

	public void fillFluidInit(FluidTank tank) {
		for(int i = 2; i < 6; i++) {
			ForgeDirection dir = ForgeDirection.getOrientation(i);
			fillFluid(pos.getX() + dir.offsetX * 2, pos.getY(), pos.getZ() + dir.offsetZ * 2, tank);
		}
	}

	public void fillFluid(int x, int y, int z, FluidTank tank) {
		FFUtils.fillFluid(this, tank, world, new BlockPos(x, y, z), tank.getCapacity());
	}

	AxisAlignedBB bb = null;
	
	@Override
	public AxisAlignedBB getRenderBoundingBox() {
		
		if(bb == null) {
			bb = new AxisAlignedBB(
					pos.getX() - 1,
					pos.getY(),
					pos.getZ() - 1,
					pos.getX() + 2,
					pos.getY() + 3,
					pos.getZ() + 2
					);
		}
		
		return bb;
	}
	
	@Override
	@SideOnly(Side.CLIENT)
	public double getMaxRenderDistanceSquared() {
		return 65536.0D;
	}

	@Override
	public IFluidTankProperties[] getTankProperties(){
		return new IFluidTankProperties[]{tanks[0].getTankProperties()[0], tanks[1].getTankProperties()[0], tanks[2].getTankProperties()[0]};
	}

	@Override
	public int fill(FluidStack resource, boolean doFill){
		if(resource != null && resource.getFluid() == types[0])
			return tanks[0].fill(resource, doFill);
		return 0;
	}

	@Override
	public FluidStack drain(FluidStack resource, boolean doDrain){
		FluidStack drain = null;
		if(resource.getFluid() == types[1])
			drain = tanks[1].drain(resource, doDrain);
		if(resource.getFluid() == types[2])
			drain = tanks[2].drain(resource, doDrain);
		return drain;
	}

	@Override
	public FluidStack drain(int maxDrain, boolean doDrain){
		FluidStack drain = tanks[1].drain(maxDrain, doDrain);
		if(drain == null)
			drain = tanks[2].drain(maxDrain, doDrain);
		return drain;
	}
	
	@Override
	public <T> T getCapability(Capability<T> capability, EnumFacing facing){
		if(capability == CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY)
			return CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY.cast(this);
		return super.getCapability(capability, facing);
	}
	
	@Override
	public boolean hasCapability(Capability<?> capability, EnumFacing facing){
		return capability == CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY || super.hasCapability(capability, facing);
	}
}