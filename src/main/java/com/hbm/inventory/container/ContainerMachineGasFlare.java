package com.hbm.inventory.container;

import com.hbm.items.machine.ItemFlareCatalyst;
import com.hbm.tileentity.machine.oil.TileEntityMachineGasFlare;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraftforge.items.SlotItemHandler;

import javax.annotation.Nonnull;

public class ContainerMachineGasFlare extends Container {

	private TileEntityMachineGasFlare flare;

	public ContainerMachineGasFlare(InventoryPlayer invPlayer, TileEntityMachineGasFlare tedf) {
		flare = tedf;

		this.addSlotToContainer(new SlotItemHandler(tedf.inventory, 0, 8, 19));

		this.addSlotToContainer(new SlotItemHandler(tedf.inventory, 1, 8, 55));

		this.addSlotToContainer(new SlotItemHandler(tedf.inventory, 2, 203, 109) {
			@Override
			public boolean isItemValid(@Nonnull ItemStack stack) {
				return stack.getItem() instanceof ItemFlareCatalyst;
			}
		});

		this.addSlotToContainer(new SlotItemHandler(tedf.inventory, 3, 159, 19));

		this.addSlotToContainer(new SlotItemHandler(tedf.inventory, 4, 159, 55) {
			@Override
			public boolean isItemValid(@Nonnull ItemStack stack) {
				return false;
			}
		});

		int offset = 39;

		for(int i = 0; i < 3; i++) {
			for(int j = 0; j < 9; j++) {
				this.addSlotToContainer(new Slot(invPlayer, j + i * 9 + 9, 8 + j * 18, 111 + i * 18 + offset));
			}
		}

		for(int i = 0; i < 9; i++) {
			this.addSlotToContainer(new Slot(invPlayer, i, 8 + i * 18, 169 + offset));
		}
	}

	@Override
	public ItemStack transferStackInSlot(EntityPlayer p_82846_1_, int par2) {
		ItemStack var3 = ItemStack.EMPTY;
		Slot var4 = this.inventorySlots.get(par2);

		if (var4 != null && var4.getHasStack()) {
			ItemStack var5 = var4.getStack();
			var3 = var5.copy();

			if (par2 >= 0 && par2 <= 4) {
				if (!this.mergeItemStack(var5, 5, this.inventorySlots.size(), true)) {
					return ItemStack.EMPTY;
				}
			}
			else {
				if (!this.mergeItemStack(var5, 0, 5, false)) {
					return ItemStack.EMPTY;
				}
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
	public boolean canInteractWith(EntityPlayer player) {
		return flare.isUseableByPlayer(player);
	}
}