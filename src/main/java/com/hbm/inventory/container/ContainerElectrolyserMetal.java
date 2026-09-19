package com.hbm.inventory.container;

import com.hbm.inventory.ElectrolyserMetalRecipes;
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

//Slot positions match gui_electrolyser_metal.png; molten material slots dropped, 9 shared output slots, metal-fluid container slots added
public class ContainerElectrolyserMetal extends Container {

	private static final int MACHINE_SLOTS = 15;
	private TileEntityElectrolyser electrolyser;

	public ContainerElectrolyserMetal(InventoryPlayer invPlayer, TileEntityElectrolyser te) {
		electrolyser = te;

		//Battery
		this.addSlotToContainer(new SlotItemHandler(te.inventory, 0, 154, 80));
		//Upgrades
		this.addSlotToContainer(new SlotUpgrade(te.inventory, 1, 102, 83));
		this.addSlotToContainer(new SlotUpgrade(te.inventory, 2, 120, 83));
		//Crystal
		this.addSlotToContainer(new SlotItemHandler(te.inventory, 9, 15, 21));
		//Acid containers
		this.addSlotToContainer(new SlotItemHandler(te.inventory, 10, 59, 17));
		this.addSlotToContainer(new SlotMachineOutput(te.inventory, 11, 59, 53));
		//Outputs
		for(int i = 0; i < 3; i++) {
			for(int j = 0; j < 3; j++) {
				this.addSlotToContainer(new SlotMachineOutput(te.inventory, TileEntityElectrolyser.OUT_START + i * 3 + j, 81 + j * 18, 17 + i * 18));
			}
		}

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
			} else if(ElectrolyserMetalRecipes.getRecipe(stack) != null) {
				if(!this.mergeItemStack(stack, 3, 4, false))
					return ItemStack.EMPTY;
			} else if(FluidUtil.getFluidContained(stack) != null) {
				if(!this.mergeItemStack(stack, 4, 5, false))
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
		return electrolyser.isUseableByPlayer(player);
	}
}
