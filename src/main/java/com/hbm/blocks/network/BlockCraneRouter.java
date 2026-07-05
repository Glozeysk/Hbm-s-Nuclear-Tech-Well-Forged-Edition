package com.hbm.blocks.network;

import api.hbm.block.IConveyorItem;
import api.hbm.block.IConveyorPackage;
import api.hbm.block.IEnterableBlock;
import com.hbm.blocks.ModBlocks;
import com.hbm.items.tool.ItemTooling;
import com.hbm.main.MainRegistry;
import com.hbm.tileentity.network.TileEntityCraneRouter;
import com.hbm.util.I18nUtil;
import net.minecraft.block.BlockContainer;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumBlockRenderType;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class BlockCraneRouter extends BlockContainer implements IEnterableBlock {

    public BlockCraneRouter(Material materialIn, String s) {
        super(materialIn);
        this.setTranslationKey(s);
        this.setRegistryName(s);
        ModBlocks.ALL_BLOCKS.add(this);
    }

    @Override
    public TileEntity createNewTileEntity(World world, int meta) {
        return new TileEntityCraneRouter();
    }

    @Override
    public boolean canItemEnter(World world, int x, int y, int z, EnumFacing dir, IConveyorItem entity) {
        BlockPos pos = new BlockPos(x, y, z);
        TileEntity te = world.getTileEntity(pos);
        if (te instanceof TileEntityCraneRouter) {
            TileEntityCraneRouter router = (TileEntityCraneRouter) te;
            int index = getIndexFromDirection(dir);
            return index >= 0 && router.modes[index] == TileEntityCraneRouter.MODE_INPUT;
        }
        return false;
    }

    @Override
    public void onItemEnter(World world, int x, int y, int z, EnumFacing dir, IConveyorItem entity) {
        TileEntity te = world.getTileEntity(new BlockPos(x, y, z));
        if (te instanceof TileEntityCraneRouter) {
            TileEntityCraneRouter router = (TileEntityCraneRouter) te;
            ItemStack stack = entity.getItemStack().copy();
            int accepted = router.tryInsertDirect(stack);
            if (accepted <= 0) {
                net.minecraft.entity.item.EntityItem drop = new net.minecraft.entity.item.EntityItem(world, x + 0.5, y + 0.5, z + 0.5, entity.getItemStack().copy());
                world.spawnEntity(drop);
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

    @Override
    public boolean onBlockActivated(World worldIn, BlockPos pos, IBlockState state, EntityPlayer playerIn, EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ) {
        if (playerIn.getHeldItem(hand).getItem() instanceof ItemTooling) {
            return false;
        } else if (worldIn.isRemote) {
            return true;
        } else if (!playerIn.isSneaking()) {
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

    private int getIndexFromDirection(EnumFacing dir) {
        EnumFacing[] customEnumOrder = new EnumFacing[]{
                EnumFacing.NORTH, EnumFacing.UP, EnumFacing.EAST,
                EnumFacing.SOUTH, EnumFacing.DOWN, EnumFacing.WEST
        };
        for (int i = 0; i < customEnumOrder.length; i++) {
            if (customEnumOrder[i] == dir) return i;
        }
        return -1;
    }
}