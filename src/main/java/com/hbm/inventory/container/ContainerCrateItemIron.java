package com.hbm.inventory.container;

import com.hbm.blocks.generic.BlockStorageCrate;
import com.hbm.inventory.InventoryCrateItem;

import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.ClickType;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraftforge.items.SlotItemHandler;

public class ContainerCrateItemIron extends Container {

    private final InventoryCrateItem crateInventory;
    private final ItemStack crateStack;
    private final int crateSlotIndex;

    public ContainerCrateItemIron(InventoryPlayer invPlayer, InventoryCrateItem inventory, ItemStack stack) {
        this.crateInventory = inventory;
        this.crateStack = stack;
        this.crateSlotIndex = invPlayer.currentItem;

        for (int i = 0; i < 4; i++) {
            for (int j = 0; j < 9; j++) {
                this.addSlotToContainer(new SlotCrateItem(inventory, j + i * 9, 8 + j * 18, 18 + i * 18));
            }
        }

        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 9; j++) {
                this.addSlotToContainer(new Slot(invPlayer, j + i * 9 + 9, 8 + j * 18, 84 + i * 18 + 20));
            }
        }

        for (int i = 0; i < 9; i++) {
            if (i == crateSlotIndex) {
                this.addSlotToContainer(new SlotLocked(invPlayer, i, 8 + i * 18, 142 + 20));
            } else {
                this.addSlotToContainer(new Slot(invPlayer, i, 8 + i * 18, 142 + 20));
            }
        }
    }

    @Override
    public ItemStack transferStackInSlot(EntityPlayer player, int index) {
        ItemStack itemstack = ItemStack.EMPTY;
        Slot slot = this.inventorySlots.get(index);

        if (slot != null && slot.getHasStack()) {
            ItemStack itemstack1 = slot.getStack();

            if (isCrate(itemstack1)) {
                return ItemStack.EMPTY;
            }

            itemstack = itemstack1.copy();

            int crateSlots = crateInventory.getSlots();

            if (index < crateSlots) {
                if (!this.mergeItemStack(itemstack1, crateSlots, this.inventorySlots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else {
                if (!this.mergeItemStack(itemstack1, 0, crateSlots, false)) {
                    return ItemStack.EMPTY;
                }
            }

            if (itemstack1.isEmpty()) {
                slot.putStack(ItemStack.EMPTY);
            } else {
                slot.onSlotChanged();
            }
        }

        return itemstack;
    }

    @Override
    public ItemStack slotClick(int slotId, int dragType, ClickType clickTypeIn, EntityPlayer player) {
        if (slotId >= 0 && slotId < this.inventorySlots.size()) {
            Slot slot = this.inventorySlots.get(slotId);
            if (slot instanceof SlotLocked) {
                return ItemStack.EMPTY;
            }
        }

        ItemStack held = player.inventory.getItemStack();
        if (!held.isEmpty() && isCrate(held)) {
            if (slotId >= 0 && slotId < crateInventory.getSlots()) {
                return ItemStack.EMPTY;
            }
        }

        return super.slotClick(slotId, dragType, clickTypeIn, player);
    }

    private boolean isCrate(ItemStack stack) {
        if (stack.isEmpty()) return false;
        if (stack.getItem() instanceof ItemBlock) {
            Block block = ((ItemBlock) stack.getItem()).getBlock();
            return block instanceof BlockStorageCrate;
        }
        return false;
    }

    @Override
    public boolean canInteractWith(EntityPlayer player) {
        ItemStack currentItem = player.inventory.getStackInSlot(crateSlotIndex);
        return currentItem == crateStack && crateInventory.isUsableByPlayer(player);
    }

    @Override
    public void onContainerClosed(EntityPlayer player) {
        super.onContainerClosed(player);
        crateInventory.saveToStack();
    }

    private class SlotCrateItem extends SlotItemHandler {
        public SlotCrateItem(InventoryCrateItem inventory, int index, int xPosition, int yPosition) {
            super(inventory, index, xPosition, yPosition);
        }

        @Override
        public boolean isItemValid(ItemStack stack) {
            return !isCrate(stack) && super.isItemValid(stack);
        }
    }

    private class SlotLocked extends Slot {
        public SlotLocked(InventoryPlayer inventory, int index, int xPosition, int yPosition) {
            super(inventory, index, xPosition, yPosition);
        }

        @Override
        public boolean isItemValid(ItemStack stack) {
            return false;
        }

        @Override
        public boolean canTakeStack(EntityPlayer player) {
            return false;
        }

        @Override
        public ItemStack decrStackSize(int amount) {
            return ItemStack.EMPTY;
        }
    }
}