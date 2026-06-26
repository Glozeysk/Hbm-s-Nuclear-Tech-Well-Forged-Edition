package com.hbm.tileentity.network;

import net.minecraft.util.EnumFacing;

public class TileEntityCraneEjectorAlt2 extends TileEntityCraneEjectorBase {

    @Override
    public String getName() {
        return "container.craneEjectorAlt2";
    }

    @Override
    public EnumFacing getInputSide() {
        return EnumFacing.DOWN;
    }
}