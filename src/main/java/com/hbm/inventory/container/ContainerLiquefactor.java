package com.hbm.inventory.container;

import com.hbm.inventory.LiquefactionRecipes;
import com.hbm.inventory.SlotUpgrade;
import com.hbm.items.machine.ItemMachineUpgrade;
import com.hbm.tileentity.machine.oil.TileEntityMachineLiquefactor;

import api.hbm.energy.IBatteryItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraftforge.items.SlotItemHandler;

//Ported from NTM:CE, slot positions match gui_liquefactor.png
public class ContainerLiquefactor extends Container {

	private TileEntityMachineLiquefactor liquefactor;

	public ContainerLiquefactor(InventoryPlayer invPlayer, TileEntityMachineLiquefactor te) {
		liquefactor = te;

		//Input
		this.addSlotToContainer(new SlotItemHandler(te.inventory, 0, 35, 54) {
			@Override
			public boolean isItemValid(ItemStack stack) {
				return LiquefactionRecipes.getOutput(stack) != null;
			}
		});
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
			} else if(LiquefactionRecipes.getOutput(stack) != null) {
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
		return liquefactor.isUseableByPlayer(player);
	}
}
