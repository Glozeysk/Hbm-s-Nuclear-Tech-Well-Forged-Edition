package com.hbm.inventory.container;

import com.hbm.inventory.SlotMachineOutput;
import com.hbm.tileentity.machine.TileEntityMachineDiFurnaceBig;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.IContainerListener;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraftforge.items.SlotItemHandler;

public class ContainerDiFurnaceBig extends Container {
	
	private TileEntityMachineDiFurnaceBig diFurnace;
	private int progress;
	private long power;
	private int tankAmount;
	private int tankType;
	
	public ContainerDiFurnaceBig(InventoryPlayer invPlayer, TileEntityMachineDiFurnaceBig tedf) {
		
		diFurnace = tedf;
		
		// Input up
		this.addSlotToContainer(new SlotItemHandler(tedf.inventory, 0, 60, 18));
		this.addSlotToContainer(new SlotItemHandler(tedf.inventory, 1, 78, 18));
		// Input down
		this.addSlotToContainer(new SlotItemHandler(tedf.inventory, 2, 60, 72));
		this.addSlotToContainer(new SlotItemHandler(tedf.inventory, 3, 78, 72));
		// Output
		this.addSlotToContainer(new SlotMachineOutput(tedf.inventory, 4, 130, 36));
		this.addSlotToContainer(new SlotMachineOutput(tedf.inventory, 5, 148, 36));
		this.addSlotToContainer(new SlotMachineOutput(tedf.inventory, 6, 130, 54));
		this.addSlotToContainer(new SlotMachineOutput(tedf.inventory, 7, 148, 54));
		// Battery
		this.addSlotToContainer(new SlotItemHandler(tedf.inventory, 8, 184, 72));
		// Fluid
		this.addSlotToContainer(new SlotItemHandler(tedf.inventory, 9, 15, 18));
		this.addSlotToContainer(new SlotMachineOutput(tedf.inventory, 10, 15, 72));

		
		for(int i = 0; i < 3; i++)
		{
			for(int j = 0; j < 9; j++)
			{
				this.addSlotToContainer(new Slot(invPlayer, j + i * 9 + 9, 8 + j * 18, 119 + i * 18));
			}
		}
		
		for(int i = 0; i < 9; i++)
		{
			this.addSlotToContainer(new Slot(invPlayer, i, 8 + i * 18, 177));
		}
	}
	
	@Override
	public void addListener(IContainerListener crafting) {
		super.addListener(crafting);
		crafting.sendWindowProperty(this, 0, this.diFurnace.process);
		crafting.sendWindowProperty(this, 1, (int)(this.diFurnace.power & 0xFFFF));
		crafting.sendWindowProperty(this, 2, (int)((this.diFurnace.power >> 16) & 0xFFFF));
		crafting.sendWindowProperty(this, 3, (int)((this.diFurnace.power >> 32) & 0xFFFF));
		crafting.sendWindowProperty(this, 4, (int)((this.diFurnace.power >> 48) & 0xFFFF));
		crafting.sendWindowProperty(this, 5, this.diFurnace.tank.getFluidAmount());
		crafting.sendWindowProperty(this, 6, this.diFurnace.tank.getFluid() != null ? this.diFurnace.tank.getFluid().getFluid().getName().hashCode() : 0);
	}
	
	@Override
	public ItemStack transferStackInSlot(EntityPlayer player, int par2) {
		ItemStack var3 = ItemStack.EMPTY;
		Slot var4 = (Slot) this.inventorySlots.get(par2);

		if (var4 != null && var4.getHasStack()) {
			ItemStack var5 = var4.getStack();
			var3 = var5.copy();

			if (par2 <= 10) {
				if (!this.mergeItemStack(var5, 11, this.inventorySlots.size(), true)) {
					return ItemStack.EMPTY;
				}
			} else {
				ItemStack slot0 = this.inventorySlots.get(0).getStack();
				ItemStack slot1 = this.inventorySlots.get(1).getStack();
				ItemStack slot2 = this.inventorySlots.get(2).getStack();
				ItemStack slot3 = this.inventorySlots.get(3).getStack();

				boolean matchesUpper = (!slot0.isEmpty() && slot0.getItem() == var5.getItem() && slot0.getMetadata() == var5.getMetadata()) ||
									(!slot1.isEmpty() && slot1.getItem() == var5.getItem() && slot1.getMetadata() == var5.getMetadata());

				boolean matchesLower = (!slot2.isEmpty() && slot2.getItem() == var5.getItem() && slot2.getMetadata() == var5.getMetadata()) ||
									(!slot3.isEmpty() && slot3.getItem() == var5.getItem() && slot3.getMetadata() == var5.getMetadata());

				boolean upperEmpty = slot0.isEmpty() && slot1.isEmpty();
				boolean lowerEmpty = slot2.isEmpty() && slot3.isEmpty();

				if (matchesUpper) {
					if (!this.mergeItemStack(var5, 0, 2, false))
						return ItemStack.EMPTY;
				} else if (matchesLower) {
					if (!this.mergeItemStack(var5, 2, 4, false))
						return ItemStack.EMPTY;
				} else if (upperEmpty) {
					if (!this.mergeItemStack(var5, 0, 2, false))
						return ItemStack.EMPTY;
				} else if (lowerEmpty) {
					if (!this.mergeItemStack(var5, 2, 4, false))
						return ItemStack.EMPTY;
				} else {
					if (!this.mergeItemStack(var5, 0, 4, false))
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
		return diFurnace.isUseableByPlayer(player);
	}
	
	@Override
	public void detectAndSendChanges() {
		super.detectAndSendChanges();
		
		for(int i = 0; i < this.listeners.size(); i++)
		{
			IContainerListener par1 = (IContainerListener)this.listeners.get(i);
			
			if(this.progress != this.diFurnace.process)
				par1.sendWindowProperty(this, 0, this.diFurnace.process);

			long currentPower = this.diFurnace.power;
			if(this.power != currentPower) {
				par1.sendWindowProperty(this, 1, (int)(currentPower & 0xFFFF));
				par1.sendWindowProperty(this, 2, (int)((currentPower >> 16) & 0xFFFF));
				par1.sendWindowProperty(this, 3, (int)((currentPower >> 32) & 0xFFFF));
				par1.sendWindowProperty(this, 4, (int)((currentPower >> 48) & 0xFFFF));
			}

			int currentTankAmount = this.diFurnace.tank.getFluidAmount();
			if(this.tankAmount != currentTankAmount)
				par1.sendWindowProperty(this, 5, currentTankAmount);

			int currentTankType = this.diFurnace.tank.getFluid() != null ? this.diFurnace.tank.getFluid().getFluid().getName().hashCode() : 0;
			if(this.tankType != currentTankType)
				par1.sendWindowProperty(this, 6, currentTankType);
		}

		this.progress = this.diFurnace.process;
		this.power = this.diFurnace.power;
		this.tankAmount = this.diFurnace.tank.getFluidAmount();
		this.tankType = this.diFurnace.tank.getFluid() != null ? this.diFurnace.tank.getFluid().getFluid().getName().hashCode() : 0;
	}
	
	@Override
	public void updateProgressBar(int id, int data) {
		switch(id) {
			case 0:
				diFurnace.process = data;
				break;
			case 1:
				diFurnace.power = (diFurnace.power & 0xFFFFFFFFFFFF0000L) | (data & 0xFFFF);
				break;
			case 2:
				diFurnace.power = (diFurnace.power & 0xFFFFFFFF0000FFFFL) | ((long)(data & 0xFFFF) << 16);
				break;
			case 3:
				diFurnace.power = (diFurnace.power & 0xFFFF0000FFFFFFFFL) | ((long)(data & 0xFFFF) << 32);
				break;
			case 4:
				diFurnace.power = (diFurnace.power & 0x0000FFFFFFFFFFFFL) | ((long)(data & 0xFFFF) << 48);
				break;
			case 5:
				if(diFurnace.tank.getFluid() != null)
					diFurnace.tank.getFluid().amount = data;
				break;
			case 6:
				// Tank type is synced via FluidTankPacket
				break;
		}
	}
}