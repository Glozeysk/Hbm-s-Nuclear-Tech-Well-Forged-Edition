package com.hbm.tileentity.machine;

import api.hbm.energy.IEnergyUser;
import api.hbm.energy.ILoadedTile;

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
		if(target != null && world.getTileEntity(target) != null && (capability == CapabilityEnergy.ENERGY || capability == CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY)){
			return world.getTileEntity(target).hasCapability(capability, facing);
		}
		return false;
	}
	
	@Override
	public <T> T getCapability(Capability<T> capability, EnumFacing facing) {
		if(target != null && world.getTileEntity(target) != null && (capability == CapabilityEnergy.ENERGY || capability == CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY)){
			return world.getTileEntity(target).getCapability(capability, facing);
		}
		return null;
	}

	@Override
	public void setPower(long i) {

		if(world.getTileEntity(target) instanceof IEnergyUser) {
			((IEnergyUser)world.getTileEntity(target)).setPower(i);
		}
	}

	@Override
	public long getPower() {

		if(world.getTileEntity(target) instanceof IEnergyUser) {
			return ((IEnergyUser)world.getTileEntity(target)).getPower();
		}

		return 0;
	}

	@Override
	public long getMaxPower() {

		if(world.getTileEntity(target) instanceof IEnergyUser) {
			return ((IEnergyUser)world.getTileEntity(target)).getMaxPower();
		}

		return 0;
	}
}
