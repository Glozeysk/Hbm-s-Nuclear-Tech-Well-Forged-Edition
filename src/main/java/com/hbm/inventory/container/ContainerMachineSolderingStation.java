package com.hbm.inventory.container;

import com.hbm.inventory.SlotMachineOutput;
import com.hbm.inventory.SlotUpgrade;
import com.hbm.inventory.SolderingRecipes;
import com.hbm.items.machine.ItemMachineUpgrade;
import com.hbm.tileentity.machine.TileEntityMachineSolderingStation;

import api.hbm.energy.IBatteryItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraftforge.items.SlotItemHandler;

//Ported from NTM:CE, slot positions match gui_soldering_station.png; the CE fluid identifier slot is dropped
public class ContainerMachineSolderingStation extends Container {

	private TileEntityMachineSolderingStation station;

	public ContainerMachineSolderingStation(InventoryPlayer invPlayer, TileEntityMachineSolderingStation te) {
		station = te;

		//Inputs 0-4 and the solder slot 5, a 3x2 grid where the last cell is the solder
		for(int i = 0; i < 2; i++) {
			for(int j = 0; j < 3; j++) {
				final int index = i * 3 + j;
				this.addSlotToContainer(new SlotItemHandler(te.inventory, index, 17 + j * 18, 18 + i * 18) {
					@Override
					public boolean isItemValid(ItemStack stack) {
						return station.isItemValidForSlot(index, stack);
					}
				});
			}
		}
		//Output
		this.addSlotToContainer(new SlotMachineOutput(te.inventory, 6, 107, 27));
		//Battery
		this.addSlotToContainer(new SlotItemHandler(te.inventory, 7, 152, 72));
		//Upgrades
		this.addSlotToContainer(new SlotUpgrade(te.inventory, 8, 89, 63));
		this.addSlotToContainer(new SlotUpgrade(te.inventory, 9, 107, 63));

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

			if(index <= 9) {
				if(!this.mergeItemStack(stack, 10, this.inventorySlots.size(), true))
					return ItemStack.EMPTY;
			} else if(stack.getItem() instanceof ItemMachineUpgrade) {
				if(!this.mergeItemStack(stack, 8, 10, false))
					return ItemStack.EMPTY;
			} else if(stack.getItem() instanceof IBatteryItem) {
				if(!this.mergeItemStack(stack, 7, 8, false))
					return ItemStack.EMPTY;
			} else if(SolderingRecipes.isSolder(stack)) {
				if(!this.mergeItemStack(stack, 5, 6, false))
					return ItemStack.EMPTY;
			} else {
				if(!this.mergeItemStack(stack, 0, 5, false))
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
		return station.isUseableByPlayer(player);
	}
}
