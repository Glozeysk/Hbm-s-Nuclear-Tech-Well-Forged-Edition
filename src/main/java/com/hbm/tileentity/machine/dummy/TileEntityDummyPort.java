package com.hbm.tileentity.machine.dummy;

import api.hbm.energy.IEnergyUser;
import api.hbm.energy.ILoadedTile;

import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.energy.CapabilityEnergy;
import net.minecraftforge.fluids.capability.CapabilityFluidHandler;

public class TileEntityDummyPort extends TileEntityDummy implements IEnergyUser, ILoadedTile {

	public boolean isLoaded = true;

	@Override
	public boolean isLoaded() {
		return isLoaded;
	}

	@Override
	public void onChunkUnload() {
		super.onChunkUnload();
		this.isLoaded = false;
	}

	@Override
	public boolean hasCapability(Capability<?> capability, EnumFacing facing) {
		if(super.getTarget() != null && world.getTileEntity(super.getTarget()) != null && (capability == CapabilityEnergy.ENERGY || capability == CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY)){
			return world.getTileEntity(super.getTarget()).hasCapability(capability, facing);
		}
		return false;
	}
	
	@Override
	public <T> T getCapability(Capability<T> capability, EnumFacing facing) {
		if(super.getTarget() != null && world.getTileEntity(super.getTarget()) != null && (capability == CapabilityEnergy.ENERGY || capability == CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY)){
			return world.getTileEntity(super.getTarget()).getCapability(capability, facing);
		}
		return null;
	}

	@Override
	public void setPower(long i) {
		if (super.getTarget() == null) return;

		TileEntity te = world.getTileEntity(super.getTarget());
		if (te instanceof IEnergyUser) {
			((IEnergyUser) te).setPower(i);
		}
	}

	@Override
	public long getPower() {
		if (super.getTarget() == null) return 0;

		TileEntity te = world.getTileEntity(super.getTarget());
		if (te instanceof IEnergyUser) {
			return ((IEnergyUser) te).getPower();
		}
		return 0;
	}

	@Override
	public long getMaxPower() {
		if (super.getTarget() == null) return 0;

		TileEntity te = world.getTileEntity(super.getTarget());
		if (te instanceof IEnergyUser) {
			return ((IEnergyUser) te).getMaxPower();
		}
		return 0;
	}
}
