package com.hbm.blocks.network;

import com.hbm.blocks.ModBlocks;
import com.hbm.tileentity.network.TileEntityCraneBase;
import com.hbm.tileentity.network.TileEntityCraneEjectorAlt2;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.world.World;

public class CraneEjectorAlt2 extends BlockCraneEjectorBase {

    public CraneEjectorAlt2(Material materialIn, String s) {
        super(materialIn, s);
    }

    @Override
    public TileEntityCraneBase createNewTileEntity(World world, int meta) {
        return new TileEntityCraneEjectorAlt2();
    }

    @Override
    protected Block getNextBlock() {
        return ModBlocks.crane_ejector;
    }
}