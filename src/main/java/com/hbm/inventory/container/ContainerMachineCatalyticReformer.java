package com.hbm.inventory.container;

import com.hbm.inventory.SlotMachineOutput;
import com.hbm.tileentity.machine.oil.TileEntityMachineCatalyticReformer;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraftforge.items.SlotItemHandler;

//Ported from NTM:CE, slot positions match gui_catalytic_reformer.png; the CE fluid identifier slot is intentionally dropped
public class ContainerMachineCatalyticReformer extends Container {

	private TileEntityMachineCatalyticReformer reformer;

	public ContainerMachineCatalyticReformer(InventoryPlayer invPlayer, TileEntityMachineCatalyticReformer te) {

		reformer = te;

		//Battery
		this.addSlotToContainer(new SlotItemHandler(te.inventory, 0, 17, 90));
		//Feedstock Input/Output
		this.addSlotToContainer(new SlotItemHandler(te.inventory, 1, 35, 90));
		this.addSlotToContainer(new SlotMachineOutput(te.inventory, 2, 35, 108));
		//Output tank 1 Input/Output
		this.addSlotToContainer(new SlotItemHandler(te.inventory, 3, 107, 90));
		this.addSlotToContainer(new SlotMachineOutput(te.inventory, 4, 107, 108));
		//Output tank 2 Input/Output
		this.addSlotToContainer(new SlotItemHandler(te.inventory, 5, 125, 90));
		this.addSlotToContainer(new SlotMachineOutput(te.inventory, 6, 125, 108));
		//Output tank 3 Input/Output
		this.addSlotToContainer(new SlotItemHandler(te.inventory, 7, 143, 90));
		this.addSlotToContainer(new SlotMachineOutput(te.inventory, 8, 143, 108));
		//Catalyst
		this.addSlotToContainer(new SlotItemHandler(te.inventory, 9, 71, 36));

		for(int i = 0; i < 3; i++) {
			for(int j = 0; j < 9; j++) {
				this.addSlotToContainer(new Slot(invPlayer, j + i * 9 + 9, 8 + j * 18, 156 + i * 18));
			}
		}

		for(int i = 0; i < 9; i++) {
			this.addSlotToContainer(new Slot(invPlayer, i, 8 + i * 18, 214));
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
			} else if(!this.mergeItemStack(stack, 0, 1, false))
				if(!this.mergeItemStack(stack, 1, 2, false))
					if(!this.mergeItemStack(stack, 3, 4, false))
						if(!this.mergeItemStack(stack, 5, 6, false))
							if(!this.mergeItemStack(stack, 7, 8, false))
								if(!this.mergeItemStack(stack, 9, 10, false))
									return ItemStack.EMPTY;

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
		return reformer.isUseableByPlayer(player);
	}
}
