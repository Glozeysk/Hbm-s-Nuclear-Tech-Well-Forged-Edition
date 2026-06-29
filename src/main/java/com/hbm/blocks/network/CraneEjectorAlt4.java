package com.hbm.blocks.network;

import com.hbm.blocks.ModBlocks;
import com.hbm.tileentity.network.TileEntityCraneBase;
import com.hbm.tileentity.network.TileEntityCraneEjectorAlt4;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.world.World;

public class CraneEjectorAlt4 extends BlockCraneEjectorBase {

    public CraneEjectorAlt4(Material materialIn, String s) {
        super(materialIn, s);
    }

    @Override
    public TileEntityCraneBase createNewTileEntity(World world, int meta) {
        return new TileEntityCraneEjectorAlt4();
    }

    @Override
    protected Block getNextBlock() {
        return ModBlocks.crane_ejector;
    }
}