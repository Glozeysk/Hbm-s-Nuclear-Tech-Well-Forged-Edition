package com.hbm.inventory.container;

import com.hbm.blocks.generic.ItemBlockStorageCrate;
import com.hbm.lib.HBMSoundHandler;
import com.hbm.tileentity.machine.TileEntityCrateBase;
import invtweaks.api.container.ChestContainer;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.ClickType;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.SoundCategory;
import net.minecraftforge.items.SlotItemHandler;

import java.util.Objects;

@ChestContainer
public abstract class ContainerCrateBase<T extends TileEntityCrateBase> extends Container {

    protected final T tileEntity;
    protected final EntityPlayer player; // <-- ДОБАВЛЕНО: сохраняем ссылку на игрока
    protected int lockedSlotIndex = -1;
    private final int crateSlots;
    private final int playerInvY;
    private final int hotbarY;
    private final int slotStartX;
    private final int slotStartY;
    private final int playerInvStartX;
    private final int slotsPerRow;
    private final int rows;

    protected ContainerCrateBase(InventoryPlayer invPlayer, T te,
                                 int crateSlots, int slotsPerRow, int rows,
                                 int slotStartX, int slotStartY,
                                 int playerInvStartX,
                                 int playerInvY, int hotbarY) {
        this.tileEntity = te;
        this.player = invPlayer.player; // <-- ДОБАВЛЕНО: инициализация игрока
        this.crateSlots = crateSlots;
        this.slotsPerRow = slotsPerRow;
        this.rows = rows;
        this.slotStartX = slotStartX;
        this.slotStartY = slotStartY;
        this.playerInvStartX = playerInvStartX;
        this.playerInvY = playerInvY;
        this.hotbarY = hotbarY;

        if (te.isFromItemStack()) {
            lockedSlotIndex = te.getSourceSlotIndex();
        }

        if (!invPlayer.player.world.isRemote) {
            te.openInventory(invPlayer.player);
            playOpenSound(invPlayer.player);
        }

        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < slotsPerRow; j++) {
                int slotIndex = j + i * slotsPerRow;
                if (slotIndex < crateSlots) {
                    this.addSlotToContainer(new SlotCrate(te.inventory, slotIndex,
                            slotStartX + j * 18, slotStartY + i * 18));
                }
            }
        }

        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 9; j++) {
                this.addSlotToContainer(new Slot(invPlayer, j + i * 9 + 9,
                        playerInvStartX + j * 18, playerInvY + i * 18));
            }
        }

        for (int i = 0; i < 9; i++) {
            if (i == lockedSlotIndex) {
                this.addSlotToContainer(new SlotLocked(invPlayer, i,
                        playerInvStartX + i * 18, hotbarY));
            } else {
                this.addSlotToContainer(new Slot(invPlayer, i,
                        playerInvStartX + i * 18, hotbarY));
            }
        }
    }

    @Override
    public void detectAndSendChanges() {
        super.detectAndSendChanges();

        if (lockedSlotIndex != -1 && !player.world.isRemote) {
            ItemStack lockedStack = player.inventory.getStackInSlot(lockedSlotIndex);

            if (lockedStack.isEmpty()) {
                player.closeScreen();
            }
        }
    }

    @Override
    public void onContainerClosed(EntityPlayer player) {
        super.onContainerClosed(player);
        if (!player.world.isRemote) {
            tileEntity.closeInventory(player);

            if (tileEntity.isFromItemStack()) {
                ItemStack lockedStack = player.inventory.getStackInSlot(lockedSlotIndex);

                if (!lockedStack.isEmpty() && lockedStack.getCount() > 1) {
                    int extraCount = lockedStack.getCount() - 1;

                    ItemStack extraCrates = new ItemStack(lockedStack.getItem(), extraCount, lockedStack.getMetadata());

                    if (lockedStack.hasTagCompound()) {
                        NBTTagCompound cleanTag = lockedStack.getTagCompound().copy();
                        cleanTag.removeTag("Items");
                        cleanTag.removeTag("hbm_inventory");
                        extraCrates.setTagCompound(cleanTag);
                    }

                    lockedStack.setCount(1);

                    tileEntity.saveInventoryToStack();
                    playCloseSound(player);

                    if (!player.inventory.addItemStackToInventory(extraCrates)) {
                        player.dropItem(extraCrates, false);
                    }

                } else {
                    tileEntity.saveInventoryToStack();
                    playCloseSound(player);
                }
            }
        }
    }

    private void playOpenSound(EntityPlayer player) {
        if (tileEntity.isFromItemStack()) {
            player.world.playSound(null, player.posX, player.posY, player.posZ,
                    HBMSoundHandler.crateOpen, SoundCategory.BLOCKS, 1.0F, 1.0F);
        } else {
            player.world.playSound(null, tileEntity.getPos(),
                    HBMSoundHandler.crateOpen, SoundCategory.BLOCKS, 1.0F, 1.0F);
        }
    }

    private void playCloseSound(EntityPlayer player) {
        if (tileEntity.isFromItemStack()) {
            player.world.playSound(null, player.posX, player.posY, player.posZ,
                    HBMSoundHandler.crateClose, SoundCategory.BLOCKS, 1.0F, 1.0F);
        } else {
            player.world.playSound(null, tileEntity.getPos(),
                    HBMSoundHandler.crateClose, SoundCategory.BLOCKS, 1.0F, 1.0F);
        }
    }

    @Override
    public ItemStack transferStackInSlot(EntityPlayer player, int slotIndex) {
        ItemStack itemStack = ItemStack.EMPTY;
        Slot slot = this.inventorySlots.get(slotIndex);

        if (slot != null && slot.getHasStack()) {
            ItemStack stackInSlot = slot.getStack();

            if (slotIndex >= crateSlots && ItemBlockStorageCrate.isContainer(stackInSlot)) {
                return ItemStack.EMPTY;
            }

            itemStack = stackInSlot.copy();

            if (slotIndex < crateSlots) {
                if (!this.mergeItemStack(stackInSlot, crateSlots, this.inventorySlots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else {
                if (!this.mergeItemStack(stackInSlot, 0, crateSlots, false)) {
                    return ItemStack.EMPTY;
                }
            }

            if (stackInSlot.isEmpty()) {
                slot.putStack(ItemStack.EMPTY);
            } else {
                slot.onSlotChanged();
            }
        }
        return itemStack;
    }

    @Override
    public ItemStack slotClick(int slotId, int dragType, ClickType clickType, EntityPlayer player) {
        if (slotId >= 0 && slotId < this.inventorySlots.size()) {
            Slot slot = this.inventorySlots.get(slotId);
            if (slot.getClass() == SlotLocked.class) {
                return ItemStack.EMPTY;
            }
        }

        ItemStack held = player.inventory.getItemStack();
        if (!held.isEmpty() && ItemBlockStorageCrate.isContainer(held)) {
            if (slotId >= 0 && slotId < crateSlots) {
                return ItemStack.EMPTY;
            }
        }
        return super.slotClick(slotId, dragType, clickType, player);
    }

    @Override
    public boolean canInteractWith(EntityPlayer player) {
        return tileEntity.isUseableByPlayer(player);
    }

    public static class SlotCrate extends SlotItemHandler {
        public SlotCrate(net.minecraftforge.items.ItemStackHandler inventory,
                         int index, int xPosition, int yPosition) {
            super(inventory, index, xPosition, yPosition);
        }

        @Override
        public boolean isItemValid(ItemStack stack) {
            return !ItemBlockStorageCrate.isContainer(stack) && super.isItemValid(stack);
        }
    }

    public static class SlotLocked extends Slot {
        public SlotLocked(InventoryPlayer inventory, int index, int xPosition, int yPosition) {
            super(inventory, index, xPosition, yPosition);
        }
        @Override public boolean isItemValid(ItemStack stack) { return false; }
        @Override public boolean canTakeStack(EntityPlayer player) { return false; }
        @Override public ItemStack decrStackSize(int amount) { return ItemStack.EMPTY; }
    }
}