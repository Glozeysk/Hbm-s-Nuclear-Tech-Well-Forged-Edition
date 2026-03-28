package com.hbm.blocks.network.energy;

import java.util.List;

import com.hbm.blocks.ModBlocks;
import com.hbm.blocks.BlockDummyable;
import com.hbm.blocks.ITooltipProvider;
import com.hbm.lib.ForgeDirection;
import com.hbm.tileentity.TileEntityProxyConductor;
import com.hbm.tileentity.network.energy.TileEntityPylonBase;
import com.hbm.tileentity.network.energy.TileEntitySubstation;

import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.EnumDyeColor;
import net.minecraft.item.ItemDye;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class Substation extends BlockDummyable implements ITooltipProvider {

    public Substation(Material materialIn, String s) {
        super(materialIn, s);
    }

    @Override
    public TileEntity createNewTileEntity(World world, int meta) {

        if(meta >= 12)
            return new TileEntitySubstation();

        if(meta >= 6)
            return new TileEntityProxyConductor();

        return null;
    }

    @Override
    public int[] getDimensions() {
        return new int[] {4, 0, 1, 1, 2, 2};
    }

    @Override
    public int getOffset() {
        return 1;
    }

    @Override
    public boolean onBlockActivated(World world, BlockPos pos, IBlockState state, EntityPlayer player, EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ) {
        ItemStack held = player.getHeldItem(hand);

        if (!held.isEmpty() && held.getItem() instanceof ItemDye) {
            if (!world.isRemote) {
                int[] corePos = this.findCore(world, pos.getX(), pos.getY(), pos.getZ());
                if (corePos != null) {
                    TileEntity te = world.getTileEntity(new BlockPos(corePos[0], corePos[1], corePos[2]));
                    if (te instanceof TileEntityPylonBase) {
                        TileEntityPylonBase pylon = (TileEntityPylonBase) te;
                        EnumDyeColor dyeColor = EnumDyeColor.byDyeDamage(held.getMetadata());

                        int r, g, b;
                        if (dyeColor == EnumDyeColor.ORANGE) {
                            r = 216;
                            g = 64;
                            b = 10;
                        } else if (dyeColor == EnumDyeColor.BLACK) {
                            r = 16;
                            g = 16;
                            b = 16;
                        } else {
                            int color = dyeColor.getColorValue();
                            r = (color >> 16) & 0xFF;
                            g = (color >> 8) & 0xFF;
                            b = color & 0xFF;
                        }

                        pylon.setCableColor(r, g, b);

                        if (!player.isCreative()) {
                            held.shrink(1);
                        }
                    }
                }
            }
            return true;
        }

        return super.onBlockActivated(world, pos, state, player, hand, facing, hitX, hitY, hitZ);
    }

    @Override
    public void breakBlock(World world, BlockPos pos, IBlockState state) {
        TileEntity te = world.getTileEntity(pos);
        if (te != null && te instanceof TileEntityPylonBase) {
            ((TileEntityPylonBase)te).disconnectAll();
        }
        super.breakBlock(world, pos, state);
    }

    @Override
    protected void fillSpace(World world, int x, int y, int z, ForgeDirection dir, int o) {
        super.fillSpace(world, x, y, z, dir, o);

        this.makeExtra(world, x + dir.offsetX * o + 1, y, z + dir.offsetZ * o + 1);
        this.makeExtra(world, x + dir.offsetX * o + 1, y, z + dir.offsetZ * o - 1);
        this.makeExtra(world, x + dir.offsetX * o - 1, y, z + dir.offsetZ * o + 1);
        this.makeExtra(world, x + dir.offsetX * o - 1, y, z + dir.offsetZ * o - 1);
    }

    public void addInformation(ItemStack stack, World worldIn, List<String> list, ITooltipFlag flagIn) {
        this.addStandardInfo((List)list);
        super.addInformation(stack, worldIn, (List)list, flagIn);
    }
}