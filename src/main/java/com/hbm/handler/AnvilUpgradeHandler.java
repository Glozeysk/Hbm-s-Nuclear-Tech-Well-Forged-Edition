package com.hbm.handler;

import com.hbm.inventory.AnvilRecipes;
import com.hbm.inventory.AnvilSmithingRecipe;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumHand;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public class AnvilUpgradeHandler {

    @SubscribeEvent(priority = EventPriority.HIGH)
    public void onRightClick(PlayerInteractEvent.RightClickBlock event) {
        World world = event.getWorld();
        BlockPos pos = event.getPos();
        EntityPlayer player = event.getEntityPlayer();
        EnumHand hand = event.getHand();
        ItemStack held = player.getHeldItem(hand);

        if (world.isRemote) return;
        if (!player.isSneaking() || held.isEmpty() || held.getCount() < 10) return;

        IBlockState oldState = world.getBlockState(pos);
        Block currentBlock = oldState.getBlock();
        Item blockItem = Item.getItemFromBlock(currentBlock);

        for (AnvilSmithingRecipe recipe : AnvilRecipes.getSmithing()) {
            boolean matchesAnvil = false;
            boolean matchesMaterial = false;

            for (ItemStack leftStack : recipe.getLeft()) {
                if (leftStack.getItem() == blockItem) {
                    matchesAnvil = true;
                    break;
                }
            }
            for (ItemStack rightStack : recipe.getRight()) {
                if (ItemStack.areItemsEqual(held, rightStack)) {
                    matchesMaterial = true;
                    break;
                }
            }

            if (matchesAnvil && matchesMaterial) {
                Block newBlock = Block.getBlockFromItem(recipe.getSimpleOutput().getItem());
                if (newBlock != null && newBlock != net.minecraft.init.Blocks.AIR) {

                    int meta = currentBlock.getMetaFromState(oldState);
                    IBlockState newState = newBlock.getStateFromMeta(meta);

                    world.setBlockState(pos, newState, 3);
                    held.shrink(10);

                    if (player instanceof EntityPlayerMP) {
                        ((EntityPlayerMP) player).inventoryContainer.detectAndSendChanges();
                    }

                    world.playSound(null, pos, SoundEvents.BLOCK_ANVIL_PLACE, SoundCategory.BLOCKS, 1.0F, 1.0F);
                    event.setCanceled(true);
                    event.setCancellationResult(EnumActionResult.SUCCESS);
                }
                return;
            }
        }
    }
}