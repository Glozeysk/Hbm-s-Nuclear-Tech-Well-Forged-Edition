package com.hbm.blocks.network.conveyor.crane;

import com.hbm.blocks.ModBlocks;
import com.hbm.blocks.network.conveyor.block.crane.BlockCraneInserterBase;
import com.hbm.tileentity.network.TileEntityCraneBase;
import com.hbm.tileentity.network.TileEntityCraneInserter;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.world.World;

public class CraneInserter extends BlockCraneInserterBase {

    public CraneInserter(Material materialIn, String s) {
        super(materialIn, s);
    }

    @Override
    public TileEntityCraneBase createNewTileEntity(World world, int meta) {
        return new TileEntityCraneInserter();
    }

    @Override
    protected Block getNextBlock() {
        return ModBlocks.crane_inserter_alt;
    }
}