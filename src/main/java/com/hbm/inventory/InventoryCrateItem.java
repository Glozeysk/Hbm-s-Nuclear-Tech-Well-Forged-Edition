package com.hbm.inventory;

import com.hbm.blocks.generic.ItemBlockStorageCrate;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.items.ItemStackHandler;

public class InventoryCrateItem extends ItemStackHandler {

    private final ItemStack crateStack;
    private final EntityPlayer player;
    private final int playerSlotIndex;

    public InventoryCrateItem(ItemStack stack, int size, EntityPlayer player) {
        super(size);
        this.crateStack = stack;
        this.player = player;
        this.playerSlotIndex = player.inventory.currentItem;
        loadFromStack();
    }

    private void loadFromStack() {
        if (crateStack.hasTagCompound()) {
            NBTTagCompound nbt = crateStack.getTagCompound();
            for (int i = 0; i < getSlots(); i++) {
                if (nbt.hasKey("slot" + i)) {
                    this.stacks.set(i, new ItemStack(nbt.getCompoundTag("slot" + i)));
                }
            }
        }
    }

    public void saveToStack() {
        ItemStack currentStack = player.inventory.getStackInSlot(playerSlotIndex);
        if (currentStack != crateStack) {
            return;
        }

        NBTTagCompound nbt = crateStack.hasTagCompound() ? crateStack.getTagCompound() : new NBTTagCompound();

        for (int i = 0; i < getSlots(); i++) {
            nbt.removeTag("slot" + i);
        }

        for (int i = 0; i < getSlots(); i++) {
            ItemStack stack = getStackInSlot(i);
            if (!stack.isEmpty()) {
                NBTTagCompound slot = new NBTTagCompound();
                stack.writeToNBT(slot);
                nbt.setTag("slot" + i, slot);
            }
        }

        if (nbt.isEmpty()) {
            crateStack.setTagCompound(null);
        } else {
            crateStack.setTagCompound(nbt);
        }
    }

    @Override
    public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
        if (ItemBlockStorageCrate.isContainer(stack)) {
            return stack;
        }
        return super.insertItem(slot, stack, simulate);
    }

    @Override
    public boolean isItemValid(int slot, ItemStack stack) {
        return !ItemBlockStorageCrate.isContainer(stack) && super.isItemValid(slot, stack);
    }

    @Override
    protected void onContentsChanged(int slot) {
        saveToStack();
        super.onContentsChanged(slot);
    }

    public boolean isUsableByPlayer(EntityPlayer playerIn) {
        return playerIn == this.player &&
                player.inventory.getStackInSlot(playerSlotIndex) == crateStack;
    }
}