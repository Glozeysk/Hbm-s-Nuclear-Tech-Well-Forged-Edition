package com.hbm.blocks.network;

import net.minecraft.block.material.Material;

public class BlockConveyorDoubleExpress extends BlockConveyorDouble {

    public BlockConveyorDoubleExpress(Material materialIn, String s) {
        super(materialIn, s);
    }

    @Override
    public double getConveyorSpeed() {
        return 0.0625D * 3.0D;
    }
}