package com.hbm.tileentity.network;

import com.hbm.inventory.container.ContainerCraneInserterCommon;
import com.hbm.inventory.gui.GUICraneInserterCommon;
import com.hbm.lib.Library;
import com.hbm.tileentity.IGUIProvider;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.world.World;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public abstract class TileEntityCraneInserterBase extends TileEntityCraneBase {

    public TileEntityCraneInserterBase() {
        super(0);
    }

    public int tryInsertDirect(ItemStack stack) {
        if (stack.isEmpty()) return 0;

        EnumFacing outputSide = getOutputSide();
        TileEntity targetTe = world.getTileEntity(pos.offset(outputSide));
        if (targetTe == null) return 0;
        if (targetTe instanceof TileEntityCraneInserterBase) return 0;
        if (targetTe instanceof TileEntityCraneEjectorBase) return 0;

        EnumFacing accessFace = outputSide.getOpposite();
        IItemHandler targetCap = null;

        if (targetTe.hasCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, accessFace)) {
            targetCap = targetTe.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, accessFace);
        } else if (targetTe.hasCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, null)) {
            targetCap = targetTe.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, null);
        }

        if (targetCap == null) return 0;

        return insertIntoTarget(targetCap, stack);
    }

    public boolean tryFillTeDirect(ItemStack stack) {
        return tryInsertDirect(stack) > 0;
    }

    public boolean canAcceptAny() {
        EnumFacing outputSide = getOutputSide();
        TileEntity targetTe = world.getTileEntity(pos.offset(outputSide));
        if (targetTe == null) return false;
        if (targetTe instanceof TileEntityCraneInserterBase) return false;
        if (targetTe instanceof TileEntityCraneEjectorBase) return false;

        EnumFacing accessFace = outputSide.getOpposite();
        IItemHandler targetCap = null;

        if (targetTe.hasCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, accessFace)) {
            targetCap = targetTe.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, accessFace);
        } else if (targetTe.hasCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, null)) {
            targetCap = targetTe.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, null);
        }

        if (targetCap == null) return false;

        for (int i = 0; i < targetCap.getSlots(); i++) {
            ItemStack slotStack = targetCap.getStackInSlot(i);
            if (slotStack.isEmpty() || slotStack.getCount() < slotStack.getMaxStackSize()) {
                return true;
            }
        }

        return false;
    }

    private int insertIntoTarget(IItemHandler target, ItemStack stack) {
        if (stack.isEmpty()) return 0;

        int totalInserted = 0;

        for (int i = 0; i < target.getSlots(); i++) {
            if (stack.isEmpty()) break;

            ItemStack slotStack = target.getStackInSlot(i);

            if (slotStack.isEmpty() || (Library.areItemStacksCompatible(stack, slotStack, false) &&
                    slotStack.getCount() < slotStack.getMaxStackSize())) {

                ItemStack toInsert = stack.copy();
                ItemStack rest = target.insertItem(i, toInsert, false);

                int inserted = toInsert.getCount() - rest.getCount();
                totalInserted += inserted;
                stack.shrink(inserted);
            }
        }

        return totalInserted;
    }

    @Override
    public int[] getAccessibleSlotsFromSide(EnumFacing e) {
        return new int[0];
    }
}