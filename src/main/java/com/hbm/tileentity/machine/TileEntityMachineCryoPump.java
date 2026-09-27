package com.hbm.tileentity.machine;

import com.hbm.forgefluid.ModForgeFluids;

import io.netty.buffer.ByteBuf;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidTank;
import net.minecraftforge.fluids.capability.CapabilityFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.IFluidTankProperties;

/** Second stage of the welder's vacuum: picks up where the fore pump stops and takes the chamber all the way down. */
public class TileEntityMachineCryoPump extends TileEntityMachineVacuumPump implements IFluidHandler {

	public static final int tankSize = 4_000;
	public static final int nitrogenCost = 10;

	public FluidTank tank;

	public TileEntityMachineCryoPump() {
		tank = new FluidTank(tankSize);
	}

	@Override
	public long getConsumption() {
		return 10_000;
	}

	@Override
	public int getPumpRate() {
		return 100;
	}

	@Override
	public int getCeiling() {
		return 100_000;
	}

	@Override
	public int getFloor() {
		return 45_000;
	}

	@Override
	public long getMaxPower() {
		return 2_000_000;
	}

	@Override
	protected boolean hasExtraResources() {
		return tank.getFluidAmount() >= nitrogenCost;
	}

	@Override
	protected void useExtraResources() {
		tank.drain(nitrogenCost, true);
	}

	@Override
	public void serialize(ByteBuf buf) {
		super.serialize(buf);
		buf.writeInt(tank.getFluidAmount());
	}

	@Override
	public void deserialize(ByteBuf buf) {
		super.deserialize(buf);
		int amount = buf.readInt();
		tank.setFluid(amount > 0 ? new FluidStack(ModForgeFluids.nitrogen, amount) : null);
	}

	@Override
	public void readFromNBT(NBTTagCompound nbt) {
		super.readFromNBT(nbt);
		if(nbt.hasKey("tank"))
			tank.readFromNBT(nbt.getCompoundTag("tank"));
	}

	@Override
	public NBTTagCompound writeToNBT(NBTTagCompound nbt) {
		super.writeToNBT(nbt);
		nbt.setTag("tank", tank.writeToNBT(new NBTTagCompound()));
		return nbt;
	}

	@Override
	public IFluidTankProperties[] getTankProperties() {
		return tank.getTankProperties();
	}

	@Override
	public int fill(FluidStack resource, boolean doFill) {

		if(resource == null || resource.getFluid() != ModForgeFluids.nitrogen)
			return 0;

		return tank.fill(resource, doFill);
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
}
