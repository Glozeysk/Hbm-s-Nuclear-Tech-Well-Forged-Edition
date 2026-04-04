package com.hbm.lib;

import net.minecraft.item.ItemStack;
import net.minecraftforge.items.IItemHandlerModifiable;
import net.minecraftforge.items.ItemStackHandler;

public class ItemStackHandlerWrapper implements IItemHandlerModifiable {

	private ItemStackHandler handle;
	private int[] validSlots;

	public ItemStackHandlerWrapper(ItemStackHandler handle) {
		this.handle = handle;
		this.validSlots = new int[]{};
	}

	public ItemStackHandlerWrapper(ItemStackHandler handle, int[] validSlots) {
		this.handle = handle;
		this.validSlots = validSlots;
	}

	@Override
	public int getSlots() {
		return validSlots.length;
	}

	private int mapSlot(int externalSlot) {
		if(externalSlot < 0 || externalSlot >= validSlots.length)
			return -1;
		return validSlots[externalSlot];
	}

	public int mapSlotPublic(int externalSlot) {
		return mapSlot(externalSlot);
	}

	@Override
	public ItemStack getStackInSlot(int slot) {
		int mapped = mapSlot(slot);
		if(mapped == -1)
			return ItemStack.EMPTY;
		return handle.getStackInSlot(mapped);
	}

	@Override
	public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
		int mapped = mapSlot(slot);
		if(mapped == -1)
			return stack;
		return handle.insertItem(mapped, stack, simulate);
	}

	@Override
	public ItemStack extractItem(int slot, int amount, boolean simulate) {
		int mapped = mapSlot(slot);
		if(mapped == -1)
			return ItemStack.EMPTY;
		return handle.extractItem(mapped, amount, simulate);
	}

	@Override
	public int getSlotLimit(int slot) {
		int mapped = mapSlot(slot);
		if(mapped == -1)
			return 0;
		return handle.getSlotLimit(mapped);
	}

	@Override
	public void setStackInSlot(int slot, ItemStack stack) {
		int mapped = mapSlot(slot);
		if(mapped == -1)
			return;
		handle.setStackInSlot(mapped, stack);
	}
}