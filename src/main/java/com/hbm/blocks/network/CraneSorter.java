package com.hbm.blocks.network;

import api.hbm.block.IConveyorBelt;
import api.hbm.block.IConveyorInput;
import api.hbm.block.IConveyorItem;
import api.hbm.block.IConveyorPackage;
import api.hbm.block.IEnterableBlock;
import com.hbm.items.tool.ItemTooling;
import com.hbm.blocks.ModBlocks;
import com.hbm.main.MainRegistry;
import com.hbm.tileentity.network.TileEntityCraneSorter;

import net.minecraft.block.Block;
import net.minecraft.block.BlockContainer;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumBlockRenderType;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class CraneSorter extends BlockContainer implements IEnterableBlock {
    public CraneSorter(Material materialIn, String s) {
        super(materialIn);
        this.setTranslationKey(s);
        this.setRegistryName(s);
        ModBlocks.ALL_BLOCKS.add(this);
    }

    @Override
    public TileEntity createNewTileEntity(World world, int meta) {
        return new TileEntityCraneSorter();
    }

    @Override
    public boolean canItemEnter(World world, int x, int y, int z, EnumFacing dir, IConveyorItem entity) {
        TileEntity te = world.getTileEntity(new BlockPos(x, y, z));
        if (te instanceof IConveyorInput) {
            return ((IConveyorInput) te).canAcceptAny();
        }
        return false;
    }

    @Override
    public boolean onBlockActivated(World worldIn, BlockPos pos, IBlockState state, EntityPlayer playerIn, EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ) {
        if(playerIn.getHeldItem(hand).getItem() instanceof ItemTooling) {
            return false;
        } else if(worldIn.isRemote) {
            return true;
        } else if(!playerIn.isSneaking()) {
            playerIn.openGui(MainRegistry.instance, 0, worldIn, pos.getX(), pos.getY(), pos.getZ());
            return true;
        } else {
            return false;
        }
    }

    @Override
    public EnumBlockRenderType getRenderType(IBlockState state) {
        return EnumBlockRenderType.MODEL;
    }

    @Override
    public void onItemEnter(World world, int x, int y, int z, EnumFacing dir, IConveyorItem entity) {
        TileEntity te = world.getTileEntity(new BlockPos(x, y, z));
        if (te instanceof IConveyorInput) {
            ItemStack stack = entity.getItemStack().copy();
            int accepted = ((IConveyorInput) te).tryInsertDirect(stack);
            if (accepted <= 0) {
                world.spawnEntity(new EntityItem(world, x + 0.5, y + 0.5, z + 0.5, entity.getItemStack().copy()));
            }
        }
    }

    @Override
    public boolean canPackageEnter(World world, int x, int y, int z, EnumFacing dir, IConveyorPackage entity) {
        return false;
    }

    @Override
    public void onPackageEnter(World world, int x, int y, int z, EnumFacing dir, IConveyorPackage entity) {
    }
}