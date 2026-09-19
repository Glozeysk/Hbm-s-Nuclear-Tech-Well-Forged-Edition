package com.hbm.inventory.container;

import com.hbm.inventory.SlotMachineOutput;
import com.hbm.inventory.SlotUpgrade;
import com.hbm.items.ModItems;
import com.hbm.items.machine.ItemMachineUpgrade;
import com.hbm.tileentity.machine.TileEntityElectrolyser;

import api.hbm.energy.IBatteryItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fluids.FluidUtil;
import net.minecraftforge.items.SlotItemHandler;

//Slot positions match gui_electrolyser_fluid.png; no fluid identifier and no byproduct slots
public class ContainerElectrolyserFluid extends Container {

	private static final int MACHINE_SLOTS = 9;
	private TileEntityElectrolyser electrolyser;

	public ContainerElectrolyserFluid(InventoryPlayer invPlayer, TileEntityElectrolyser te) {
		electrolyser = te;

		//Battery
		this.addSlotToContainer(new SlotItemHandler(te.inventory, 0, 154, 80));
		//Upgrades
		this.addSlotToContainer(new SlotUpgrade(te.inventory, 1, 102, 83));
		this.addSlotToContainer(new SlotUpgrade(te.inventory, 2, 120, 83));
		//Input tank containers
		this.addSlotToContainer(new SlotItemHandler(te.inventory, 3, 8, 17));
		this.addSlotToContainer(new SlotMachineOutput(te.inventory, 4, 8, 53));
		//Output tank 1 containers
		this.addSlotToContainer(new SlotItemHandler(te.inventory, 5, 62, 17));
		this.addSlotToContainer(new SlotMachineOutput(te.inventory, 6, 62, 53));
		//Output tank 2 containers
		this.addSlotToContainer(new SlotItemHandler(te.inventory, 7, 118, 17));
		this.addSlotToContainer(new SlotMachineOutput(te.inventory, 8, 118, 53));

		for(int i = 0; i < 3; i++) {
			for(int j = 0; j < 9; j++) {
				this.addSlotToContainer(new Slot(invPlayer, j + i * 9 + 9, 8 + j * 18, 124 + i * 18));
			}
		}

		for(int i = 0; i < 9; i++) {
			this.addSlotToContainer(new Slot(invPlayer, i, 8 + i * 18, 182));
		}
	}

	@Override
	public ItemStack transferStackInSlot(EntityPlayer player, int index) {
		ItemStack ret = ItemStack.EMPTY;
		Slot slot = this.inventorySlots.get(index);

		if(slot != null && slot.getHasStack()) {
			ItemStack stack = slot.getStack();
			ret = stack.copy();

			if(index < MACHINE_SLOTS) {
				if(!this.mergeItemStack(stack, MACHINE_SLOTS, this.inventorySlots.size(), true))
					return ItemStack.EMPTY;
			} else if(stack.getItem() instanceof ItemMachineUpgrade) {
				if(!this.mergeItemStack(stack, 1, 3, false))
					return ItemStack.EMPTY;
			} else if(stack.getItem() instanceof IBatteryItem || stack.getItem() == ModItems.battery_creative) {
				if(!this.mergeItemStack(stack, 0, 1, false))
					return ItemStack.EMPTY;
			} else if(FluidUtil.getFluidContained(stack) != null) {
				if(!this.mergeItemStack(stack, 3, 4, false))
					return ItemStack.EMPTY;
			} else if(!this.mergeItemStack(stack, 5, 6, false) && !this.mergeItemStack(stack, 7, 8, false)) {
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
		return electrolyser.isUseableByPlayer(player);
	}
}
