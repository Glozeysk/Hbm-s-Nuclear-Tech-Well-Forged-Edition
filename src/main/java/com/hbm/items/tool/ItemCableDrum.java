package com.hbm.items.tool;

import com.hbm.items.ModItems;
import com.hbm.items.ItemBase;
import com.hbm.main.MainRegistry;

import com.hbm.util.I18nUtil;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class ItemCableDrum extends ItemBase {

    public ItemCableDrum(String s) {
        super(s);
        this.setMaxStackSize(1);
        this.setCreativeTab(MainRegistry.consumableTab);
    }

    @Override
    public ActionResult<ItemStack> onItemRightClick(World worldIn, EntityPlayer playerIn, EnumHand handIn) {
        return tryConvertToWiring(playerIn, worldIn, handIn);
    }

    @Override
    public EnumActionResult onItemUse(EntityPlayer player, World worldIn, BlockPos pos, EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ) {
        ActionResult<ItemStack> result = tryConvertToWiring(player, worldIn, hand);
        if (result.getType() == EnumActionResult.SUCCESS) {
            return EnumActionResult.SUCCESS;
        }
        return EnumActionResult.PASS;
    }

    @Override
    public void addInformation(ItemStack stack, World worldIn, List<String> tooltip, ITooltipFlag flagIn) {
        tooltip.add(I18nUtil.resolveKey("desc.cable_drum"));
    }

    private ActionResult<ItemStack> tryConvertToWiring(EntityPlayer player, World world, EnumHand hand) {
        ItemStack mainStack = player.getHeldItemMainhand();
        ItemStack offStack = player.getHeldItemOffhand();

        ItemStack drum = null;
        ItemStack wire = null;
        EnumHand drumHand = null;

        if (mainStack.getItem() == this && offStack.getItem() == ModItems.wire_red_copper) {
            drum = mainStack; wire = offStack; drumHand = EnumHand.MAIN_HAND;
        } else if (offStack.getItem() == this && mainStack.getItem() == ModItems.wire_red_copper) {
            drum = offStack; wire = mainStack; drumHand = EnumHand.OFF_HAND;
        }

        if (drum != null && wire != null) {
            if (!world.isRemote) {
                wire.shrink(1);
                ItemStack newDrum = new ItemStack(ModItems.wiring_red_copper, 1, 95);
                player.setHeldItem(drumHand, newDrum);
            }
            return new ActionResult<>(EnumActionResult.SUCCESS, player.getHeldItem(hand));
        }

        return new ActionResult<>(EnumActionResult.PASS, player.getHeldItem(hand));
    }
}