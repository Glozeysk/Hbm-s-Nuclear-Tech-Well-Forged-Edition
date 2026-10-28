package com.hbm.tileentity.conductor;

import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.AxisAlignedBB;

//No logic of its own; it only exists so the pipe renderer has something to draw. The network is walked by AirDuctNetwork.
public class TileEntityAirDuct extends TileEntity {

	@Override
	public AxisAlignedBB getRenderBoundingBox() {
		return new AxisAlignedBB(pos);
	}
}
