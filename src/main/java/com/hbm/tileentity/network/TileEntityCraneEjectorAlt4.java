package com.hbm.tileentity.network;

import net.minecraft.block.BlockHorizontal;
import net.minecraft.util.EnumFacing;

public class TileEntityCraneEjectorAlt4 extends TileEntityCraneEjectorBase {

    @Override
    public String getName() {
        return "container.craneEjectorAlt4";
    }

    @Override
    public EnumFacing getOutputSide() {
        EnumFacing facing = world.getBlockState(pos).getValue(BlockHorizontal.FACING);
        return facing.rotateYCCW();
    }

    @Override
    public EnumFacing getInputSide() {
        return world.getBlockState(pos).getValue(BlockHorizontal.FACING);
    }
}