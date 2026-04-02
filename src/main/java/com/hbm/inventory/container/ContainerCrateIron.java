package com.hbm.inventory.container;

import com.hbm.blocks.generic.ItemBlockStorageCrate;
import com.hbm.tileentity.machine.TileEntityCrateIron;

import invtweaks.api.container.ChestContainer;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.ClickType;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraftforge.items.SlotItemHandler;

@ChestContainer(rowSize = 9)
public class ContainerCrateIron extends Container {

	private TileEntityCrateIron crate;
	private int lockedSlotIndex = -1;

	public ContainerCrateIron(InventoryPlayer invPlayer, TileEntityCrateIron tedf) {
		crate = tedf;

		// Если открыт из руки - запоминаем слот
		if (crate.isFromItemStack()) {
			lockedSlotIndex = crate.getSourceSlotIndex();
		}

		for(int i = 0; i < 4; i++) {
			for(int j = 0; j < 9; j++) {
				this.addSlotToContainer(new SlotCrate(tedf.inventory, j + i * 9, 8 + j * 18, 18 + i * 18));
			}
		}

		for(int i = 0; i < 3; i++) {
			for(int j = 0; j < 9; j++) {
				this.addSlotToContainer(new Slot(invPlayer, j + i * 9 + 9, 8 + j * 18, 84 + i * 18 + 20));
			}
		}

		for(int i = 0; i < 9; i++) {
			if (i == lockedSlotIndex) {
				this.addSlotToContainer(new SlotLocked(invPlayer, i, 8 + i * 18, 142 + 20));
			} else {
				this.addSlotToContainer(new Slot(invPlayer, i, 8 + i * 18, 142 + 20));
			}
		}
	}

	@Override
	public ItemStack transferStackInSlot(EntityPlayer player, int par2) {
		ItemStack var3 = ItemStack.EMPTY;
		Slot var4 = (Slot) this.inventorySlots.get(par2);

		if (var4 != null && var4.getHasStack()) {
			ItemStack var5 = var4.getStack();

			if (par2 >= crate.inventory.getSlots() && ItemBlockStorageCrate.isContainer(var5)) {
				return ItemStack.EMPTY;
			}

			var3 = var5.copy();

			if (par2 <= crate.inventory.getSlots() - 1) {
				if (!this.mergeItemStack(var5, crate.inventory.getSlots(), this.inventorySlots.size(), true)) {
					return ItemStack.EMPTY;
				}
			} else if (!this.mergeItemStack(var5, 0, crate.inventory.getSlots(), false)) {
				return ItemStack.EMPTY;
			}

			if (var5.isEmpty()) {
				var4.putStack(ItemStack.EMPTY);
			} else {
				var4.onSlotChanged();
			}
		}

		return var3;
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
		if (!held.isEmpty() && ItemBlockStorageCrate.isContainer(held)) {
			if (slotId >= 0 && slotId < crate.inventory.getSlots()) {
				return ItemStack.EMPTY;
			}
		}

		return super.slotClick(slotId, dragType, clickTypeIn, player);
	}

	@Override
	public boolean canInteractWith(EntityPlayer player) {
		return crate.isUseableByPlayer(player);
	}

	private class SlotCrate extends SlotItemHandler {
		public SlotCrate(net.minecraftforge.items.ItemStackHandler inventory, int index, int xPosition, int yPosition) {
			super(inventory, index, xPosition, yPosition);
		}

		@Override
		public boolean isItemValid(ItemStack stack) {
			return !ItemBlockStorageCrate.isContainer(stack) && super.isItemValid(stack);
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