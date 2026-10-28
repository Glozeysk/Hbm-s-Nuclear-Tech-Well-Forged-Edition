package com.hbm.inventory.container;

import com.hbm.inventory.SlotUpgrade;
import com.hbm.items.machine.ItemMachineUpgrade;
import com.hbm.tileentity.machine.TileEntityMachineCompressorCompact;

import api.hbm.energy.IBatteryItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraftforge.items.SlotItemHandler;

//Ported from NTM:CE, slot positions match gui_compressor.png; the CE fluid identifier slot is dropped
public class ContainerCompressor extends Container {

	private TileEntityMachineCompressorCompact compressor;

	public ContainerCompressor(InventoryPlayer invPlayer, TileEntityMachineCompressorCompact te) {
		compressor = te;

		//Battery
		this.addSlotToContainer(new SlotItemHandler(te.inventory, 0, 152, 72));
		//Upgrades
		this.addSlotToContainer(new SlotUpgrade(te.inventory, 1, 52, 72));
		this.addSlotToContainer(new SlotUpgrade(te.inventory, 2, 70, 72));

		for(int i = 0; i < 3; i++) {
			for(int j = 0; j < 9; j++) {
				this.addSlotToContainer(new Slot(invPlayer, j + i * 9 + 9, 8 + j * 18, 122 + i * 18));
			}
		}

		for(int i = 0; i < 9; i++) {
			this.addSlotToContainer(new Slot(invPlayer, i, 8 + i * 18, 180));
		}
	}

	@Override
	public ItemStack transferStackInSlot(EntityPlayer player, int index) {
		ItemStack ret = ItemStack.EMPTY;
		Slot slot = this.inventorySlots.get(index);

		if(slot != null && slot.getHasStack()) {
			ItemStack stack = slot.getStack();
			ret = stack.copy();

			if(index <= 2) {
				if(!this.mergeItemStack(stack, 3, this.inventorySlots.size(), true))
					return ItemStack.EMPTY;
			} else if(stack.getItem() instanceof ItemMachineUpgrade) {
				if(!this.mergeItemStack(stack, 1, 3, false))
					return ItemStack.EMPTY;
			} else if(stack.getItem() instanceof IBatteryItem) {
				if(!this.mergeItemStack(stack, 0, 1, false))
					return ItemStack.EMPTY;
			} else {
				return ItemStack.EMPTY;
			}

			if(stack.isEmpty()) {
				slot.putStack(ItemStack.EMPTY);
			} else {
				slot.onSlotChanged();
			}
		}

		return ret;
	}

	@Override
	public boolean canInteractWith(EntityPlayer player) {
		return compressor.isUseableByPlayer(player);
	}
}
