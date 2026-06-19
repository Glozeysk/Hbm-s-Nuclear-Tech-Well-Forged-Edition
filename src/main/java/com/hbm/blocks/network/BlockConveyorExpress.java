package com.hbm.blocks.network;

import net.minecraft.block.material.Material;

public class BlockConveyorExpress extends BlockConveyor {

    public BlockConveyorExpress(Material materialIn, String s) {
        super(materialIn, s, () -> new double[]{0.0D});
    }

    @Override
    public double getConveyorSpeed() {
        return 0.0625D * 3.0D;
    }
}