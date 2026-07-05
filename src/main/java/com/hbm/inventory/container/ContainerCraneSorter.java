package com.hbm.inventory.container;

import com.hbm.inventory.SlotPattern;
import com.hbm.tileentity.network.TileEntityCraneSorter;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.ClickType;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;

public class ContainerCraneSorter extends Container {
    private TileEntityCraneSorter router;

    public ContainerCraneSorter(InventoryPlayer invPlayer, TileEntityCraneSorter router) {
        this.router = router;

        for(int j = 0; j < 2; j++) {
            for(int i = 0; i < 3; i++) {
                for(int k = 0; k < 5; k++) {
                    this.addSlotToContainer(new SlotPattern(router.inventory, k + j * 15 + i * 5, 34 + k * 18 + j * 98, 17 + i * 26));
                }
            }
        }

        for(int i = 0; i < 3; i++) {
            for(int j = 0; j < 9; j++) {
                this.addSlotToContainer(new Slot(invPlayer, j + i * 9 + 9, 47 + j * 18, 119 + i * 18));
            }
        }

        for(int i = 0; i < 9; i++) {
            this.addSlotToContainer(new Slot(invPlayer, i, 47 + i * 18, 177));
        }
    }

    @Override
    public ItemStack slotClick(int slotId, int dragType, ClickType clickTypeIn, EntityPlayer player) {
        if (slotId < 0 || slotId >= 30) {
            return super.slotClick(slotId, dragType, clickTypeIn, player);
        }

        Slot slot = this.inventorySlots.get(slotId);

        ItemStack ret = ItemStack.EMPTY;
        ItemStack held = player.inventory.getItemStack();

        if (slot.getHasStack()) {
            ret = slot.getStack().copy();
        }

        if (clickTypeIn == ClickType.PICKUP && dragType == 1 && slot.getHasStack()) {
            router.nextMode(slotId);
            return ret;
        } else {
            if (!held.isEmpty()) {
                int currentMode = router.modes[slotId / 5];
                if (currentMode == TileEntityCraneSorter.MODE_WHITELIST || currentMode == TileEntityCraneSorter.MODE_BLACKLIST) {
                    if (isItemAlreadyFiltered(held, slotId / 5, currentMode)) {
                        return ret;
                    }
                }
            }

            slot.putStack(held.isEmpty() ? ItemStack.EMPTY : held.copy());

            if (slot.getHasStack()) {
                slot.getStack().setCount(1);
            }

            slot.onSlotChanged();
            router.initPattern(slot.getStack(), slotId);

            return ret;
        }
    }

    private boolean isItemAlreadyFiltered(ItemStack stack, int currentMatcherIndex, int currentMode) {
        for (int i = 0; i < 6; i++) {
            if (i == currentMatcherIndex) continue;
            if (router.modes[i] != currentMode) continue;

            for (int slot = 0; slot < 5; slot++) {
                ItemStack filter = router.inventory.getStackInSlot(i * 5 + slot);
                if (filter.isEmpty()) continue;

                if (ItemStack.areItemsEqual(filter, stack) && ItemStack.areItemStackTagsEqual(filter, stack)) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public ItemStack transferStackInSlot(EntityPlayer player, int index) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean canInteractWith(EntityPlayer player) {
        return router.isUseableByPlayer(player);
    }
}