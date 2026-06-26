package com.hbm.blocks.network;

import api.hbm.block.IToolable;
import com.hbm.blocks.ModBlocks;
import com.hbm.lib.InventoryHelper;
import com.hbm.tileentity.network.TileEntityCraneEjectorBase;
import com.hbm.util.I18nUtil;
import net.minecraft.block.Block;
import net.minecraft.block.BlockHorizontal;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public abstract class BlockCraneEjectorBase extends BlockCraneBase implements IToolable {

    private static boolean switching;

    public BlockCraneEjectorBase(Material materialIn, String s) {
        super(materialIn);
        this.setTranslationKey(s);
        this.setRegistryName(s);
        ModBlocks.ALL_BLOCKS.add(this);
    }

    @Override
    public void addInformation(ItemStack stack, @Nullable World worldIn, List<String> tooltip, ITooltipFlag flagIn) {
        tooltip.add(I18nUtil.resolveKey("desc.crane_ejector"));
    }

    protected abstract Block getNextBlock();

    @Override
    public boolean canConnectRedstone(IBlockState state, IBlockAccess world, BlockPos pos, EnumFacing side) {
        return true;
    }

    @Override
    public void breakBlock(World world, BlockPos pos, IBlockState state) {
        TileEntity tileentity = world.getTileEntity(pos);

        if(!switching && tileentity instanceof TileEntityCraneEjectorBase) {
            InventoryHelper.dropInventoryItems(world, pos, tileentity, 9, 19);
        }

        super.breakBlock(world, pos, state);
    }

    @Override
    public boolean onScrew(World world, EntityPlayer player, int x, int y, int z, EnumFacing side, float fX, float fY, float fZ, EnumHand hand, ToolType tool) {
        if(tool != ToolType.SCREWDRIVER || !player.isSneaking()) {
            return false;
        }

        if(world.isRemote) {
            return true;
        }

        BlockPos pos = new BlockPos(x, y, z);
        TileEntity tile = world.getTileEntity(pos);

        if(!(tile instanceof TileEntityCraneEjectorBase)) {
            return false;
        }

        NBTTagCompound data = tile.writeToNBT(new NBTTagCompound());
        IBlockState oldState = world.getBlockState(pos);
        IBlockState newState = copyState(oldState, getNextBlock().getDefaultState());

        try {
            switching = true;
            world.setBlockState(pos, newState, 3);
        } finally {
            switching = false;
        }

        TileEntity newTile = world.getTileEntity(pos);

        if(newTile instanceof TileEntityCraneEjectorBase) {
            newTile.readFromNBT(data);
            newTile.markDirty();
        }

        world.notifyBlockUpdate(pos, oldState, newState, 3);
        return true;
    }

    private IBlockState copyState(IBlockState oldState, IBlockState newState) {
        if(oldState.getPropertyKeys().contains(BlockHorizontal.FACING) && newState.getPropertyKeys().contains(BlockHorizontal.FACING)) {
            return newState.withProperty(BlockHorizontal.FACING, oldState.getValue(BlockHorizontal.FACING));
        }

        return newState;
    }
}