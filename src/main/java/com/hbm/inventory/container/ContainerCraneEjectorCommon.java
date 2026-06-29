package com.hbm.inventory.container;

import com.hbm.inventory.SlotPattern;
import com.hbm.inventory.SlotUpgrade;
import com.hbm.tileentity.network.TileEntityCraneEjectorBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.ClickType;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;

public class ContainerCraneEjectorCommon extends Container {

    protected final TileEntityCraneEjectorBase extractor;

    public ContainerCraneEjectorCommon(InventoryPlayer invPlayer, TileEntityCraneEjectorBase extractor) {
        this.extractor = extractor;

        for(int i = 0; i < 3; i++) {
            for(int j = 0; j < 6; j++) {
                this.addSlotToContainer(new SlotPattern(extractor.inventory, j + i * 6, 17 + j * 18, 17 + i * 18));
            }
        }

        this.addSlotToContainer(new SlotUpgrade(extractor.inventory, 18, 152, 35));

        for(int i = 0; i < 3; i++) {
            for(int j = 0; j < 9; j++) {
                this.addSlotToContainer(new Slot(invPlayer, j + i * 9 + 9, 8 + j * 18, 103 + i * 18));
            }
        }

        for(int i = 0; i < 9; i++) {
            this.addSlotToContainer(new Slot(invPlayer, i, 8 + i * 18, 161));
        }
    }

    @Override
    public ItemStack slotClick(int slotId, int dragType, ClickType clickTypeIn, EntityPlayer player) {
        if(slotId < 0 || slotId >= 18) {
            return super.slotClick(slotId, dragType, clickTypeIn, player);
        }

        Slot slot = this.inventorySlots.get(slotId);

        ItemStack ret = ItemStack.EMPTY;
        ItemStack held = player.inventory.getItemStack();

        if(slot.getHasStack()) {
            ret = slot.getStack().copy();
        }

        if(clickTypeIn == ClickType.PICKUP && dragType == 1 && slot.getHasStack()) {
            extractor.nextMode(slotId);
            return ret;
        } else {
            slot.putStack(held.isEmpty() ? ItemStack.EMPTY : held.copy());

            if(slot.getHasStack()) {
                slot.getStack().setCount(1);
            }

            slot.onSlotChanged();
            extractor.initPattern(slot.getStack(), slotId);

            return ret;
        }
    }

    @Override
    public ItemStack transferStackInSlot(EntityPlayer player, int slot) {
        ItemStack var3 = ItemStack.EMPTY;
        Slot var4 = this.inventorySlots.get(slot);

        if(var4 != null && var4.getHasStack()) {
            ItemStack var5 = var4.getStack();
            var3 = var5.copy();

            if(slot < 18) {
                return ItemStack.EMPTY;
            }

            if(slot == 18) {
                if(!this.mergeItemStack(var5, 19, 55, true)) {
                    return ItemStack.EMPTY;
                }
            } else if(slot >= 19 && slot <= 54) {
                if(!this.mergeItemStack(var5, 18, 19, false)) {
                    return ItemStack.EMPTY;
                }
            }

            if(var5.isEmpty()) {
                var4.putStack(ItemStack.EMPTY);
            } else {
                var4.onSlotChanged();
            }
        }

        return var3;
    }

    @Override
    public boolean canInteractWith(EntityPlayer player) {
        return extractor.isUseableByPlayer(player);
    }
}