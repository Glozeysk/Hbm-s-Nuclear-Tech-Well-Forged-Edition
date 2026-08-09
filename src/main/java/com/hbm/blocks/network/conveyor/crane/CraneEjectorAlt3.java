package com.hbm.blocks.network.conveyor.crane;

import com.hbm.blocks.ModBlocks;
import com.hbm.blocks.network.conveyor.block.crane.BlockCraneEjectorBase;
import com.hbm.tileentity.network.TileEntityCraneBase;
import com.hbm.tileentity.network.TileEntityCraneEjectorAlt3;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.world.World;

public class CraneEjectorAlt3 extends BlockCraneEjectorBase {

    public CraneEjectorAlt3(Material materialIn, String s) {
        super(materialIn, s);
    }

    @Override
    public TileEntityCraneBase createNewTileEntity(World world, int meta) {
        return new TileEntityCraneEjectorAlt3();
    }

    @Override
    protected Block getNextBlock() {
        return ModBlocks.crane_ejector_alt_4;
    }
}