package com.hbm.blocks.network;

import net.minecraft.block.material.Material;

public class BlockConveyorTriple extends BlockConveyor {

    public BlockConveyorTriple(Material materialIn, String s) {
        super(materialIn, s, () -> new double[]{-0.3125D, 0.0D, 0.3125D});
    }
}