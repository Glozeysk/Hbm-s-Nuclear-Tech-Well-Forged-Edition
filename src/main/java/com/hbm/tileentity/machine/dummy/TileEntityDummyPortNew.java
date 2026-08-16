package com.hbm.tileentity.machine.dummy;

import net.minecraft.util.EnumFacing;
import net.minecraftforge.common.capabilities.Capability;

public class TileEntityDummyPortNew extends TileEntityDummy {

	@Override
	public boolean hasCapability(Capability<?> capability, EnumFacing facing) {
		if(super.getTarget() != null && world.getTileEntity(super.getTarget()) != null){
			return world.getTileEntity(super.getTarget()).hasCapability(capability, facing);
		}
		return false;
	}
	
	@Override
	public <T> T getCapability(Capability<T> capability, EnumFacing facing) {
		if(super.getTarget() != null && world.getTileEntity(super.getTarget()) != null){
			return world.getTileEntity(super.getTarget()).getCapability(capability, facing);
		}
		return null;
	}
}
