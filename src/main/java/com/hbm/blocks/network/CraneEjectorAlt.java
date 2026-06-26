package com.hbm.blocks.network;

import com.hbm.blocks.ModBlocks;
import com.hbm.tileentity.network.TileEntityCraneBase;
import com.hbm.tileentity.network.TileEntityCraneEjectorAlt;
import com.hbm.util.I18nUtil;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class CraneEjectorAlt extends BlockCraneEjectorBase {

    public CraneEjectorAlt(Material materialIn, String s) {
        super(materialIn, s);
    }

    @Override
    public TileEntityCraneBase createNewTileEntity(World world, int meta) {
        return new TileEntityCraneEjectorAlt();
    }

    @Override
    protected Block getNextBlock() {
        return ModBlocks.crane_ejector_alt_2;
    }
}