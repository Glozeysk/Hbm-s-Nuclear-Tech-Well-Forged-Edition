package com.hbm.tileentity.network;

import com.hbm.inventory.container.ContainerCraneInserterCommon;
import com.hbm.inventory.gui.GUICraneInserterCommon;
import com.hbm.lib.Library;
import com.hbm.tileentity.IGUIProvider;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.world.World;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public abstract class TileEntityCraneInserterBase extends TileEntityCraneBase implements IGUIProvider {

    private static final int[] ALL_SLOTS = {0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20};

    public TileEntityCraneInserterBase() {
        super(21);
    }

    @Override
    public void update() {
        super.update();
        if(!world.isRemote) {
            tryFillTe();
        }
    }

    public void tryFillTe() {
        EnumFacing outputSide = getOutputSide();
        TileEntity te = world.getTileEntity(pos.offset(outputSide));

        if(te != null) {
            if(te instanceof TileEntityCraneEjectorBase) {
                TileEntityCraneEjectorBase ejector = (TileEntityCraneEjectorBase) te;
                for(int i = 0; i < inventory.getSlots(); i++) {
                    ItemStack stack = inventory.getStackInSlot(i);
                    if(!stack.isEmpty()) {
                        int toInsert = stack.getCount();
                        ItemStack extracted = inventory.extractItem(i, toInsert, true);
                        if(!extracted.isEmpty()) {
                            int accepted = ejector.tryInsertDirect(extracted.copy());
                            if(accepted > 0) {
                                inventory.extractItem(i, accepted, false);
                            }
                        }
                    }
                }
                return;
            }

            EnumFacing accessFace = outputSide.getOpposite();
            IItemHandler cap = null;

            if(te.hasCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, accessFace)) {
                cap = te.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, accessFace);
            } else if(te.hasCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, null)) {
                cap = te.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, null);
            }

            if(cap != null) {
                for(int i = 0; i < inventory.getSlots(); i++) {
                    tryFillContainerCap(cap, i);
                }
            }
        }
    }

    public int tryInsertDirect(ItemStack stack) {
        if(stack.isEmpty()) return 0;

        int filledAmount = 0;

        for(int i : ALL_SLOTS) {
            if(stack.isEmpty() || stack.getCount() < 1) {
                return filledAmount;
            }

            ItemStack outputStack = stack.copy();
            ItemStack chestItem = inventory.getStackInSlot(i).copy();

            if(chestItem.isEmpty() || (Library.areItemStacksCompatible(outputStack, chestItem, false) && chestItem.getCount() < chestItem.getMaxStackSize())) {
                int fillAmount = Math.min(chestItem.getMaxStackSize() - chestItem.getCount(), outputStack.getCount());
                outputStack.setCount(fillAmount);

                ItemStack rest = inventory.insertItem(i, outputStack, true);

                if(rest.getCount() < outputStack.getCount()) {
                    stack.shrink(fillAmount - rest.getCount());
                    filledAmount += fillAmount - rest.getCount();
                    inventory.insertItem(i, outputStack, false);
                }
            }
        }

        return filledAmount;
    }

    public boolean tryFillTeDirect(ItemStack stack) {
        return tryInsertItemCap(inventory, stack);
    }

    public boolean tryFillContainerCap(IItemHandler chest, int slot) {
        if(inventory.getStackInSlot(slot).isEmpty())
            return false;

        return tryInsertItemCap(chest, inventory.getStackInSlot(slot));
    }

    public boolean tryInsertItemCap(IItemHandler chest, ItemStack stack) {
        if(stack.isEmpty())
            return false;

        for(int i = 0; i < chest.getSlots(); i++) {
            ItemStack outputStack = stack.copy();

            if(outputStack.isEmpty() || outputStack.getCount() == 0)
                return true;

            ItemStack chestItem = chest.getStackInSlot(i).copy();

            if(chestItem.isEmpty() || (Library.areItemStacksCompatible(outputStack, chestItem, false) && chestItem.getCount() < chestItem.getMaxStackSize())) {
                int fillAmount = Math.min(chestItem.getMaxStackSize() - chestItem.getCount(), outputStack.getCount());
                outputStack.setCount(fillAmount);

                ItemStack rest = chest.insertItem(i, outputStack, true);

                if(rest.getCount() < outputStack.getCount()) {
                    stack.shrink(outputStack.getCount() - rest.getCount());
                    chest.insertItem(i, outputStack, false);
                }
            }
        }

        return false;
    }

    @Override
    public Container provideContainer(int ID, EntityPlayer player, World world, int x, int y, int z) {
        return new ContainerCraneInserterCommon(player.inventory, this);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public GuiScreen provideGUI(int ID, EntityPlayer player, World world, int x, int y, int z) {
        return new GUICraneInserterCommon(player.inventory, this);
    }

    @Override
    public int[] getAccessibleSlotsFromSide(EnumFacing e) {
        return ALL_SLOTS;
    }
}