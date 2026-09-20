package com.hbm.tileentity.machine;

import com.hbm.forgefluid.FFUtils;
import com.hbm.forgefluid.ModForgeFluids;
import com.hbm.lib.DirPos;
import com.hbm.lib.HBMSoundHandler;
import com.hbm.lib.Library;
import com.hbm.tileentity.TileEntityLoadedBase;

import api.hbm.energy.IEnergyUser;
import io.netty.buffer.ByteBuf;
import net.minecraft.block.material.Material;
import net.minecraft.init.SoundEvents;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ITickable;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraftforge.common.BiomeDictionary;
import net.minecraftforge.common.BiomeDictionary.Type;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidTank;
import net.minecraftforge.fluids.capability.CapabilityFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.IFluidTankProperties;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

//Ported from NTM:CE (MachinePumpElectric): base and electric tile merged into one class, reworked into an ocean brine pump.
public class TileEntityMachinePumpOcean extends TileEntityLoadedBase implements ITickable, IEnergyUser, IFluidHandler {

	public static final long maxPower = 50_000;
	public static final int consumption = 250;
	public static final int pumpSpeed = 50;
	public static final int tankSize = 32_000;
	//how many water blocks have to sit directly below the core
	public static final int waterDepth = 5;
	private static final int checkInterval = 20;

	public long power;
	public FluidTank tank;

	public boolean isOn = false;
	public boolean validBiome = false;
	public boolean validHeight = false;
	public boolean validWater = false;

	public float rotor;
	public float lastRotor;

	public TileEntityMachinePumpOcean() {
		tank = new FluidTank(tankSize);
	}

	@Override
	public void update() {
		if(!world.isRemote) {

			if(world.getTotalWorldTime() % checkInterval == 0) {
				this.validBiome = this.checkBiome();
				this.validHeight = this.checkHeight();
				this.validWater = this.checkWater();

				for(DirPos con : getConPos())
					this.trySubscribe(world, con.getPos(), con.getDir());
			}

			for(DirPos con : getConPos())
				FFUtils.fillFluid(this, tank, world, con.getPos(), pumpSpeed * checkInterval);

			this.isOn = false;
			if(this.canOperate()) {
				this.isOn = true;
				this.operate();
			}

			networkPackNT(150);

		} else {

			this.lastRotor = this.rotor;
			if(this.isOn) this.rotor += 10F;

			if(this.rotor >= 360F) {
				this.rotor -= 360F;
				this.lastRotor -= 360F;

				world.playSound(null, pos.getX(), pos.getY(), pos.getZ(), HBMSoundHandler.steamEngineOperate, SoundCategory.BLOCKS, 0.5F, 0.75F);
				world.playSound(null, pos.getX(), pos.getY(), pos.getZ(), SoundEvents.ENTITY_GENERIC_SPLASH, SoundCategory.BLOCKS, 1F, 0.5F);
			}
		}
	}

	private boolean canOperate() {
		return this.validBiome && this.validHeight && this.validWater && power >= consumption && tank.getFluidAmount() < tank.getCapacity();
	}

	private void operate() {
		this.power -= consumption;
		tank.fill(new FluidStack(ModForgeFluids.brine, pumpSpeed), true);
	}

	private boolean checkBiome() {
		return BiomeDictionary.hasType(world.getBiome(pos), Type.OCEAN);
	}

	private boolean checkHeight() {
		return pos.getY() == world.getSeaLevel();
	}

	private boolean checkWater() {
		for(int i = 1; i <= waterDepth; i++)
			if(world.getBlockState(pos.down(i)).getMaterial() != Material.WATER)
				return false;
		return true;
	}

	//the four ports sit next to the core, so pipes and cables are two blocks out
	private DirPos[] getConPos() {
		return new DirPos[] {
				new DirPos(pos.add(2, 0, 0), Library.POS_X),
				new DirPos(pos.add(-2, 0, 0), Library.NEG_X),
				new DirPos(pos.add(0, 0, 2), Library.POS_Z),
				new DirPos(pos.add(0, 0, -2), Library.NEG_Z)
		};
	}

	@Override
	public void readFromNBT(NBTTagCompound compound) {
		super.readFromNBT(compound);
		this.power = compound.getLong("power");
		if(compound.hasKey("tank"))
			tank.readFromNBT(compound.getCompoundTag("tank"));
	}

	@Override
	public NBTTagCompound writeToNBT(NBTTagCompound compound) {
		compound.setLong("power", power);
		compound.setTag("tank", tank.writeToNBT(new NBTTagCompound()));
		return super.writeToNBT(compound);
	}

	@Override
	public void serialize(ByteBuf buf) {
		buf.writeBoolean(isOn);
		buf.writeBoolean(validBiome);
		buf.writeBoolean(validHeight);
		buf.writeBoolean(validWater);
		buf.writeLong(power);
		buf.writeInt(tank.getFluidAmount());
	}

	@Override
	public void deserialize(ByteBuf buf) {
		this.isOn = buf.readBoolean();
		this.validBiome = buf.readBoolean();
		this.validHeight = buf.readBoolean();
		this.validWater = buf.readBoolean();
		this.power = buf.readLong();
		//only ever brine, so the amount is all the client needs
		tank.setFluid(new FluidStack(ModForgeFluids.brine, buf.readInt()));
	}

	@Override
	public void setPower(long i) {
		power = i;
	}

	@Override
	public long getPower() {
		return power;
	}

	@Override
	public long getMaxPower() {
		return maxPower;
	}

	@Override
	public IFluidTankProperties[] getTankProperties() {
		return tank.getTankProperties();
	}

	@Override
	public int fill(FluidStack resource, boolean doFill) {
		return 0;
	}

	@Override
	public FluidStack drain(FluidStack resource, boolean doDrain) {
		if(resource == null || resource.getFluid() != ModForgeFluids.brine)
			return null;
		return tank.drain(resource.amount, doDrain);
	}

	@Override
	public FluidStack drain(int maxDrain, boolean doDrain) {
		return tank.drain(maxDrain, doDrain);
	}

	@Override
	public boolean hasCapability(Capability<?> capability, EnumFacing facing) {
		if(capability == CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY)
			return true;
		return super.hasCapability(capability, facing);
	}

	@Override
	public <T> T getCapability(Capability<T> capability, EnumFacing facing) {
		if(capability == CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY)
			return CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY.cast(this);
		return super.getCapability(capability, facing);
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
					pos.getY() + 5,
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
}
