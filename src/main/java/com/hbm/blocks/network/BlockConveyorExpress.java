package com.hbm.blocks.network;

import com.hbm.entity.item.EntityMovingItem;
import net.minecraft.block.material.Material;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class BlockConveyorExpress extends BlockConveyor {

    public BlockConveyorExpress(Material materialIn, String s) {
        super(materialIn, s, () -> new double[]{0.0D});
    }

    @Override
    protected double getTravelSpeed(World world, BlockPos pos, double baseSpeed) {
        return baseSpeed * 3.0D;
    }

    @Override
    protected double getEffectiveSpeed(World world, BlockPos pos, EntityMovingItem item, double baseSpeed) {
        return baseSpeed * 3.0D;
    }
}