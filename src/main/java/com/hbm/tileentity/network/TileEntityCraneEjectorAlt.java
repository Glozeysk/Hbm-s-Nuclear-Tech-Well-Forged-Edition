package com.hbm.tileentity.network;

import net.minecraft.util.EnumFacing;

public class TileEntityCraneEjectorAlt extends TileEntityCraneEjectorBase {

    @Override
    public String getName() {
        return "container.craneEjectorAlt";
    }

    @Override
    public EnumFacing getInputSide() {
        return EnumFacing.UP;
    }
}