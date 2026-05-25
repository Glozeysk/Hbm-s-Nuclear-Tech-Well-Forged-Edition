package com.hbm.blocks.generic;

import com.hbm.blocks.ModBlocks;
import com.hbm.items.tool.ItemKeyPin;
import com.hbm.lib.HBMSoundHandler;

import com.hbm.main.MainRegistry;
import net.minecraft.block.Block;
import net.minecraft.block.BlockShulkerBox;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumHand;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.List;

public class ItemBlockStorageCrate extends ItemBlock {

    public ItemBlockStorageCrate(Block block) {
        super(block);
    }

    @Override
    public ActionResult<ItemStack> onItemRightClick(World world, EntityPlayer player, EnumHand hand) {
        ItemStack stack = player.getHeldItem(hand);

        // Приоритет основной руки
        if (hand == EnumHand.OFF_HAND && isContainer(player.getHeldItemMainhand())) {
            return new ActionResult<>(EnumActionResult.PASS, stack);
        }

        // Запрет открытия стаков
        if (stack.getCount() > 1) {
            if (!world.isRemote) player.sendStatusMessage(new TextComponentTranslation("message.crate.split"), true);
            return new ActionResult<>(EnumActionResult.FAIL, stack);
        }

        if (!world.isRemote) {
            if (!hasMatchingKey(stack, player)) {
                return new ActionResult<>(EnumActionResult.FAIL, stack);
            }

            int guiId = getGuiId(this.getBlock());
            if (guiId != -1) {
                if (stack.hasTagCompound() && stack.getTagCompound().hasKey("lock")) {
                    world.playSound(null, player.posX, player.posY, player.posZ, HBMSoundHandler.lockHang, SoundCategory.PLAYERS, 1.0F, 1.0F);
                }
                world.playSound(null, player.posX, player.posY, player.posZ, HBMSoundHandler.crateOpen, SoundCategory.BLOCKS, 1.0F, 1.0F);
                player.openGui(MainRegistry.instance, guiId, world, 0, -999, 0);
                return new ActionResult<>(EnumActionResult.SUCCESS, stack);
            }
        }

        return new ActionResult<>(EnumActionResult.PASS, stack);
    }

    private boolean hasMatchingKey(ItemStack crate, EntityPlayer player) {
        if (!crate.hasTagCompound() || !crate.getTagCompound().hasKey("lock")) return true;

        int lockPins = crate.getTagCompound().getInteger("lock");

        for (int i = 0; i <= 40; i++) {
            ItemStack slot = player.inventory.getStackInSlot(i);
            if (slot.getItem() instanceof ItemKeyPin && ItemKeyPin.getPins(slot) == lockPins) {
                return true;
            }
        }
        return false;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void addInformation(ItemStack stack, World worldIn, List<String> tooltip, ITooltipFlag flagIn) {
        super.addInformation(stack, worldIn, tooltip, flagIn);
        if (stack.hasTagCompound() && stack.getTagCompound().hasKey("lock")) {
            tooltip.add("§c" + I18n.format("tooltip.crate.locked"));
        }
    }

    private int getGuiId(Block block) {
        if (block == ModBlocks.crate_iron) return ModBlocks.guiID_crate_iron;
        if (block == ModBlocks.crate_steel) return ModBlocks.guiID_crate_steel;
        if (block == ModBlocks.crate_desh) return ModBlocks.guiID_crate_desh;
        if (block == ModBlocks.crate_tungsten) return ModBlocks.guiID_crate_tungsten;
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