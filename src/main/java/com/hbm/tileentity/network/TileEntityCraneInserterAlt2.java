package com.hbm.tileentity.network;

import net.minecraft.util.EnumFacing;

public class TileEntityCraneInserterAlt2 extends TileEntityCraneInserterBase {

    @Override
    public String getName() {
        return "container.craneInserterAlt2";
    }

    @Override
    public EnumFacing getOutputSide() {
        return EnumFacing.UP;
    }
}