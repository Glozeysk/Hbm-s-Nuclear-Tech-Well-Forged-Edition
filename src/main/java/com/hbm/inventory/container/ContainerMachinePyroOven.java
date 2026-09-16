package com.hbm.inventory.container;

import com.hbm.inventory.SlotMachineOutput;
import com.hbm.inventory.SlotUpgrade;
import com.hbm.items.ModItems;
import com.hbm.items.machine.ItemMachineUpgrade;
import com.hbm.tileentity.machine.oil.TileEntityMachinePyroOven;

import api.hbm.energy.IBatteryItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraftforge.items.SlotItemHandler;

//Ported from NTM:CE, slot positions match gui_pyrooven.png; the CE fluid identifier slot is intentionally dropped
public class ContainerMachinePyroOven extends Container {

	private TileEntityMachinePyroOven pyro;

	public ContainerMachinePyroOven(InventoryPlayer invPlayer, TileEntityMachinePyroOven te) {
		pyro = te;

		//Battery
		this.addSlotToContainer(new SlotItemHandler(te.inventory, 0, 152, 72));
		//Input
		this.addSlotToContainer(new SlotItemHandler(te.inventory, 1, 35, 45));
		//Output
		this.addSlotToContainer(new SlotMachineOutput(te.inventory, 2, 89, 45));
		//Upgrades
		this.addSlotToContainer(new SlotUpgrade(te.inventory, 3, 53, 72));
		this.addSlotToContainer(new SlotUpgrade(te.inventory, 4, 71, 72));

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

			if(index <= 4) {
				if(!this.mergeItemStack(stack, 5, this.inventorySlots.size(), true))
					return ItemStack.EMPTY;
			} else if(stack.getItem() instanceof ItemMachineUpgrade) {
				if(!this.mergeItemStack(stack, 3, 5, false))
					return ItemStack.EMPTY;
			} else if(stack.getItem() instanceof IBatteryItem || stack.getItem() == ModItems.battery_creative) {
				if(!this.mergeItemStack(stack, 0, 1, false))
					return ItemStack.EMPTY;
			} else if(!this.mergeItemStack(stack, 1, 2, false)) {
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
		return pyro.isUseableByPlayer(player);
	}
}
