package com.hbm.blocks.network;

import com.hbm.blocks.ModBlocks;
import com.hbm.tileentity.network.TileEntityCraneBase;
import com.hbm.tileentity.network.TileEntityCraneInserterAlt2;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.world.World;

public class CraneInserterAlt2 extends BlockCraneInserterBase {

    public CraneInserterAlt2(Material materialIn, String s) {
        super(materialIn, s);
    }

    @Override
    public TileEntityCraneBase createNewTileEntity(World world, int meta) {
        return new TileEntityCraneInserterAlt2();
    }

    @Override
    protected Block getNextBlock() {
        return ModBlocks.crane_inserter;
    }
}