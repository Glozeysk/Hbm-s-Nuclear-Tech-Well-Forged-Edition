package com.hbm.tileentity.network;

import net.minecraft.block.BlockHorizontal;
import net.minecraft.util.EnumFacing;

public class TileEntityCraneEjectorAlt3 extends TileEntityCraneEjectorBase {

    @Override
    public String getName() {
        return "container.craneEjectorAlt3";
    }

    @Override
    public EnumFacing getOutputSide() {
        EnumFacing facing = world.getBlockState(pos).getValue(BlockHorizontal.FACING);
        return facing.rotateY();
    }

    @Override
    public EnumFacing getInputSide() {
        return world.getBlockState(pos).getValue(BlockHorizontal.FACING);
    }
}