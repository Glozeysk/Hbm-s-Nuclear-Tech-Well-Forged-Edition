package com.hbm.blocks.network;

import api.hbm.block.IConveyorItem;
import api.hbm.block.IConveyorPackage;
import api.hbm.block.IEnterableBlock;
import api.hbm.block.IToolable;
import com.hbm.blocks.ModBlocks;
import com.hbm.lib.InventoryHelper;
import com.hbm.tileentity.network.TileEntityCraneInserterBase;
import com.hbm.util.I18nUtil;
import net.minecraft.block.Block;
import net.minecraft.block.BlockHorizontal;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public abstract class BlockCraneInserterBase extends BlockCraneBase implements IEnterableBlock, IToolable {

    private static boolean switching;

    public BlockCraneInserterBase(Material materialIn, String s) {
        super(materialIn);
        this.setTranslationKey(s);
        this.setRegistryName(s);
        ModBlocks.ALL_BLOCKS.add(this);
    }

    protected abstract Block getNextBlock();

    @Override
    public boolean canItemEnter(World world, int x, int y, int z, EnumFacing dir, IConveyorItem entity) {
        BlockPos pos = new BlockPos(x, y, z);
        IBlockState state = world.getBlockState(pos);
        EnumFacing orientation = state.getValue(BlockHorizontal.FACING);
        return dir == orientation;
    }

    @Override
    public void addInformation(ItemStack stack, @Nullable World worldIn, List<String> tooltip, ITooltipFlag flagIn) {
        tooltip.add(I18nUtil.resolveKey("desc.crane_inserter"));
    }

    @Override
    public void onItemEnter(World world, int x, int y, int z, EnumFacing dir, IConveyorItem entity) {
        if(entity == null || entity.getItemStack() == ItemStack.EMPTY || entity.getItemStack().getCount() <= 0) {
            return;
        }

        ItemStack toAdd = entity.getItemStack().copy();

        TileEntity te = world.getTileEntity(new BlockPos(x, y, z));
        boolean worked = false;

        if(te instanceof TileEntityCraneInserterBase) {
            worked = ((TileEntityCraneInserterBase) te).tryFillTeDirect(toAdd);
        }

        if(!worked) {
            EntityItem drop = new EntityItem(world, x + 0.5, y + 0.5, z + 0.5, toAdd.copy());
            world.spawnEntity(drop);
        }
    }

    @Override
    public boolean canPackageEnter(World world, int x, int y, int z, EnumFacing dir, IConveyorPackage entity) {
        return false;
    }

    @Override
    public void onPackageEnter(World world, int x, int y, int z, EnumFacing dir, IConveyorPackage entity) {
    }

    @Override
    public void breakBlock(World world, BlockPos pos, IBlockState state) {
        TileEntity tileentity = world.getTileEntity(pos);

        if(!switching && tileentity instanceof TileEntityCraneInserterBase) {
            InventoryHelper.dropInventoryItems(world, pos, tileentity);
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

        if(!(tile instanceof TileEntityCraneInserterBase)) {
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

        if(newTile instanceof TileEntityCraneInserterBase) {
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