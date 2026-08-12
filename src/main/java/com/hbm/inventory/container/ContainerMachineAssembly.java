package com.hbm.inventory.container;

import com.hbm.inventory.SlotMachineOutput;
import com.hbm.items.machine.ItemAssemblyTemplate;

import com.hbm.tileentity.machine.TileEntityMachineAssembly;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraftforge.items.SlotItemHandler;

public class ContainerMachineAssembly extends Container {

    private TileEntityMachineAssembly assembler;

    public ContainerMachineAssembly(InventoryPlayer invPlayer, TileEntityMachineAssembly te) {
        assembler = te;

		//Battery
		this.addSlotToContainer(new SlotItemHandler(te.inventory, 0, 152, 92));
		//Upgrades
		this.addSlotToContainer(new SlotItemHandler(te.inventory, 1, 120, 18));
		this.addSlotToContainer(new SlotItemHandler(te.inventory, 2, 120, 36));
		this.addSlotToContainer(new SlotItemHandler(te.inventory, 3, 120, 54));
		//Schematic
		this.addSlotToContainer(new SlotItemHandler(te.inventory, 4, 80, 56){
			@Override
			public boolean isItemValid(ItemStack stack) {
				return stack != null && stack.getItem() instanceof ItemAssemblyTemplate;
			};
		});
		//Output
		this.addSlotToContainer(new SlotMachineOutput(te.inventory, 5, 113, 84));
		//Input
		this.addSlotToContainer(new SlotItemHandler(te.inventory, 6, 8, 18));
		this.addSlotToContainer(new SlotItemHandler(te.inventory, 7, 26, 18));
		this.addSlotToContainer(new SlotItemHandler(te.inventory, 8, 44, 18));
		this.addSlotToContainer(new SlotItemHandler(te.inventory, 9, 62, 18));
		this.addSlotToContainer(new SlotItemHandler(te.inventory, 10, 80, 18));
		this.addSlotToContainer(new SlotItemHandler(te.inventory, 11, 98, 18));
		this.addSlotToContainer(new SlotItemHandler(te.inventory, 12, 8, 36));
		this.addSlotToContainer(new SlotItemHandler(te.inventory, 13, 26, 36));
		this.addSlotToContainer(new SlotItemHandler(te.inventory, 14, 44, 36));
		this.addSlotToContainer(new SlotItemHandler(te.inventory, 15, 62, 36));
		this.addSlotToContainer(new SlotItemHandler(te.inventory, 16, 80, 36));
		this.addSlotToContainer(new SlotItemHandler(te.inventory, 17, 98, 36));
		
		for(int i = 0; i < 3; i++)
		{
			for(int j = 0; j < 9; j++)
			{
				this.addSlotToContainer(new Slot(invPlayer, j + i * 9 + 9, 8 + j * 18, 84 + i * 18 + 55));
			}
		}
		
		for(int i = 0; i < 9; i++)
		{
			this.addSlotToContainer(new Slot(invPlayer, i, 8 + i * 18, 142 + 55));
		}
    }

    @Override
    public ItemStack transferStackInSlot(EntityPlayer p_82846_1_, int par2)
    {
        ItemStack var3 = ItemStack.EMPTY;
        Slot var4 = (Slot) this.inventorySlots.get(par2);

        if (var4 != null && var4.getHasStack())
        {
            ItemStack var5 = var4.getStack();
            var3 = var5.copy();

            if (par2 <= 17) {
                if (!this.mergeItemStack(var5, 18, this.inventorySlots.size(), true))
                {
                    return ItemStack.EMPTY;
                }
            }
            else if (!this.mergeItemStack(var5, 6, 18, false))
                if (!this.mergeItemStack(var5, 0, 4, false))
                    return ItemStack.EMPTY;

            if (var5.getCount() == 0)
            {
                var4.putStack(ItemStack.EMPTY);
            }
            else
            {
                var4.onSlotChanged();
            }
        }

        return var3;
    }

    @Override
    public boolean canInteractWith(EntityPlayer player) {
        return assembler.isUseableByPlayer(player);
    }

}
