package com.hbm.blocks.network;

import net.minecraft.block.material.Material;

public class BlockConveyorDouble extends BlockConveyor {

    public BlockConveyorDouble(Material materialIn, String s) {
        super(materialIn, s, () -> new double[]{-0.25D, 0.25D});
    }
}