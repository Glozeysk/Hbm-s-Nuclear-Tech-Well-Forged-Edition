package com.hbm.blocks.generic;

import com.hbm.blocks.ModBlocks;
import com.hbm.main.MainRegistry;

import net.minecraft.block.Block;
import net.minecraft.block.BlockShulkerBox;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumHand;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.World;

public class ItemBlockStorageCrate extends ItemBlock {

    public ItemBlockStorageCrate(Block block) {
        super(block);
    }

    @Override
    public ActionResult<ItemStack> onItemRightClick(World world, EntityPlayer player, EnumHand hand) {
        ItemStack stack = player.getHeldItem(hand);

        if (stack.getCount() > 1) {
            if (!world.isRemote) {
                player.sendStatusMessage(new TextComponentTranslation("message.crate.split"), true);
            }
            return new ActionResult<>(EnumActionResult.FAIL, stack);
        }

        if (!world.isRemote && hand == EnumHand.MAIN_HAND) {
            Block block = this.getBlock();

            int guiId = getGuiId(block);

            if (guiId != -1) {
                player.openGui(MainRegistry.instance, guiId, world, 0, -999, 0);
                return new ActionResult<>(EnumActionResult.SUCCESS, stack);
            }
        }

        return new ActionResult<>(EnumActionResult.PASS, stack);
    }

    private int getGuiId(Block block) {
        if (block == ModBlocks.crate_iron) {
            return ModBlocks.guiID_crate_iron;
        } else if (block == ModBlocks.crate_steel) {
            return ModBlocks.guiID_crate_steel;
        } else if (block == ModBlocks.crate_desh) {
            return ModBlocks.guiID_crate_desh;
        } else if (block == ModBlocks.crate_tungsten) {
            return ModBlocks.guiID_crate_tungsten;
        }
        return -1;
    }

    public static boolean isContainer(ItemStack stack) {
        if (stack.isEmpty()) return false;
        if (stack.getItem() instanceof ItemBlock) {
            Block block = ((ItemBlock) stack.getItem()).getBlock();
            return block instanceof BlockStorageCrate || block instanceof BlockShulkerBox;
        }
        return false;
    }
}