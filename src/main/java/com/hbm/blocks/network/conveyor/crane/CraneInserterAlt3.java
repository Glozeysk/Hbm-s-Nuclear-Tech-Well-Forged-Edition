package com.hbm.blocks.network.conveyor.crane;

import com.hbm.blocks.ModBlocks;
import com.hbm.blocks.network.conveyor.block.crane.BlockCraneInserterBase;
import com.hbm.tileentity.network.TileEntityCraneBase;
import com.hbm.tileentity.network.TileEntityCraneInserterAlt3;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.world.World;

public class CraneInserterAlt3 extends BlockCraneInserterBase {

    public CraneInserterAlt3(Material materialIn, String s) {
        super(materialIn, s);
    }

    @Override
    public TileEntityCraneBase createNewTileEntity(World world, int meta) {
        return new TileEntityCraneInserterAlt3();
    }

    @Override
    protected Block getNextBlock() {
        return ModBlocks.crane_inserter_alt_4;
    }
}