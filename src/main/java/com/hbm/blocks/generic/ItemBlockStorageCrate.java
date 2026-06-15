package com.hbm.blocks.generic;

import com.hbm.blocks.ModBlocks;
import com.hbm.items.tool.ItemKeyPin;
import com.hbm.lib.HBMSoundHandler;

import com.hbm.main.MainRegistry;
import net.minecraft.block.Block;
import net.minecraft.block.BlockShulkerBox;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.IItemPropertyGetter;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.*;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;
import java.util.List;

public class ItemBlockStorageCrate extends ItemBlock {

    private static ItemStack openStackClient = null;

    public ItemBlockStorageCrate(Block block) {
        super(block);
        this.addPropertyOverride(new ResourceLocation("open"), new IItemPropertyGetter() {
            @Override
            public float apply(ItemStack stack, @Nullable World worldIn, @Nullable EntityLivingBase entityIn) {
                boolean isOpen = (openStackClient != null);

                return isOpen ? 1.0F : 0.0F;
            }
        });
    }

    @Override
    public ActionResult<ItemStack> onItemRightClick(World world, EntityPlayer player, EnumHand hand) {
        ItemStack stack = player.getHeldItem(hand);

        if (hand == EnumHand.OFF_HAND && isContainer(player.getHeldItemMainhand())) {
            return new ActionResult<>(EnumActionResult.PASS, stack);
        }

        if (stack.getCount() > 1) {
            if (!world.isRemote) player.sendStatusMessage(new TextComponentTranslation("message.crate.split"), true);
            return new ActionResult<>(EnumActionResult.FAIL, stack);
        }

        if (!hasMatchingKey(stack, player)) {
            return new ActionResult<>(EnumActionResult.FAIL, stack);
        }

        if (!world.isRemote) {
            int guiId = getGuiId(this.getBlock());
            if (guiId != -1) {
                if (stack.hasTagCompound() && stack.getTagCompound().hasKey("lock")) {
                    world.playSound(null, player.posX, player.posY, player.posZ,
                            HBMSoundHandler.lockOpen, SoundCategory.BLOCKS, 1.0F, 1.0F);
                }
                world.playSound(null, player.posX, player.posY, player.posZ,
                        HBMSoundHandler.crateOpen, SoundCategory.BLOCKS, 1.0F, 1.0F);
                player.openGui(MainRegistry.instance, guiId, world, 0, -999, 0);
                return new ActionResult<>(EnumActionResult.SUCCESS, stack);
            }
        } else {
            openStackClient = stack;
        }

        return new ActionResult<>(EnumActionResult.PASS, stack);
    }

    public static void clearOpenStack() {
        openStackClient = null;
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
        tooltip.add(I18n.format("crate.item.info"));
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

    public static boolean isOpen() {
        return openStackClient != null;
    }
}