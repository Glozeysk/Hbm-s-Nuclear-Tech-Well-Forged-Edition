package com.hbm.inventory.container;

import com.hbm.inventory.SlotMachineOutput;
import com.hbm.inventory.SlotUpgrade;
import com.hbm.items.machine.ItemMachineUpgrade;
import com.hbm.tileentity.machine.oil.TileEntityMachineSolidifier;

import api.hbm.energy.IBatteryItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraftforge.items.SlotItemHandler;

//Ported from NTM:CE, slot positions match gui_solidifier.png; the CE fluid identifier slot is dropped
public class ContainerSolidifier extends Container {

	private TileEntityMachineSolidifier solidifier;

	public ContainerSolidifier(InventoryPlayer invPlayer, TileEntityMachineSolidifier te) {
		solidifier = te;

		//Output
		this.addSlotToContainer(new SlotMachineOutput(te.inventory, 0, 71, 45));
		//Battery
		this.addSlotToContainer(new SlotItemHandler(te.inventory, 1, 134, 72));
		//Upgrades
		this.addSlotToContainer(new SlotUpgrade(te.inventory, 2, 98, 36));
		this.addSlotToContainer(new SlotUpgrade(te.inventory, 3, 98, 54));

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

			if(index <= 3) {
				if(!this.mergeItemStack(stack, 4, this.inventorySlots.size(), true))
					return ItemStack.EMPTY;
			} else if(stack.getItem() instanceof ItemMachineUpgrade) {
				if(!this.mergeItemStack(stack, 2, 4, false))
					return ItemStack.EMPTY;
			} else if(stack.getItem() instanceof IBatteryItem) {
				if(!this.mergeItemStack(stack, 1, 2, false))
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
		return solidifier.isUseableByPlayer(player);
	}
}
