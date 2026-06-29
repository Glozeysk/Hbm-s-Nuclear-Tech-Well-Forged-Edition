package com.hbm.tileentity.network;

import net.minecraft.block.BlockHorizontal;
import net.minecraft.util.EnumFacing;

public class TileEntityCraneInserterAlt4 extends TileEntityCraneInserterBase {

    @Override
    public String getName() {
        return "container.craneInserterAlt4";
    }

    @Override
    public EnumFacing getOutputSide() {
        EnumFacing facing = world.getBlockState(pos).getValue(BlockHorizontal.FACING);
        return facing.rotateYCCW();
    }
}