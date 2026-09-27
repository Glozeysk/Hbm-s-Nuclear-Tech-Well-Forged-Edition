package com.hbm.inventory.container;

import com.hbm.inventory.SlotMachineOutput;
import com.hbm.inventory.SlotUpgrade;
import com.hbm.items.machine.ItemMachineUpgrade;
import com.hbm.tileentity.machine.TileEntityMachineArcWelder;

import api.hbm.energy.IBatteryItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraftforge.items.SlotItemHandler;

//Ported from NTM:CE, slot positions match gui_arc_welder.png; the CE fluid identifier slot is intentionally dropped
public class ContainerMachineArcWelder extends Container {

	private TileEntityMachineArcWelder welder;

	public ContainerMachineArcWelder(InventoryPlayer invPlayer, TileEntityMachineArcWelder te) {
		welder = te;

		//Inputs
		this.addSlotToContainer(new SlotItemHandler(te.inventory, 0, 17, 36));
		this.addSlotToContainer(new SlotItemHandler(te.inventory, 1, 35, 36));
		this.addSlotToContainer(new SlotItemHandler(te.inventory, 2, 53, 36));
		//Output
		this.addSlotToContainer(new SlotMachineOutput(te.inventory, 3, 107, 36));
		//Battery
		this.addSlotToContainer(new SlotItemHandler(te.inventory, 4, 152, 72));
		//Upgrades
		this.addSlotToContainer(new SlotUpgrade(te.inventory, 5, 62, 63));
		this.addSlotToContainer(new SlotUpgrade(te.inventory, 6, 80, 63));
		//DNT transformer, gates the electron beam process; parked off screen where the sheet draws no slot for it
		boolean beam = te.mode == TileEntityMachineArcWelder.MODE_EBW;
		this.addSlotToContainer(new SlotItemHandler(te.inventory, 7, beam ? 107 : -2000, beam ? 63 : -2000) {
			@Override
			public boolean isItemValid(ItemStack stack) {
				return TileEntityMachineArcWelder.isBeamTransformer(stack);
			}
		});

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

			if(index <= 7) {
				if(!this.mergeItemStack(stack, 8, this.inventorySlots.size(), true))
					return ItemStack.EMPTY;
			} else if(stack.getItem() instanceof ItemMachineUpgrade) {
				if(!this.mergeItemStack(stack, 5, 7, false))
					return ItemStack.EMPTY;
			} else if(stack.getItem() instanceof IBatteryItem) {
				if(!this.mergeItemStack(stack, 4, 5, false))
					return ItemStack.EMPTY;
			} else if(TileEntityMachineArcWelder.isBeamTransformer(stack) && welder.mode == TileEntityMachineArcWelder.MODE_EBW) {
				if(!this.mergeItemStack(stack, 7, 8, false))
					return ItemStack.EMPTY;
			} else {
				if(!this.mergeItemStack(stack, 0, 3, false))
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
		return welder.isUseableByPlayer(player);
	}
}
