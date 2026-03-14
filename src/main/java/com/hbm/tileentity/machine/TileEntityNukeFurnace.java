package com.hbm.tileentity.machine;

import com.hbm.blocks.machine.MachineNukeFurnace;
import com.hbm.inventory.BreederRecipes;
import com.hbm.util.ContaminationUtil;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.FurnaceRecipes;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ITickable;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;//

//TODO пофиксить баг с лимитом операций на 1000, а также сделать забирание пустых топливных стержней с помощью воронки
public class TileEntityNukeFurnace extends TileEntity implements ITickable {

    public ItemStackHandler inventory;

    public int dualCookTime;
    public int dualPower;
    public static final int maxPower = 1000;
    public static final int processingSpeed = 30;

    private static final int SLOT_FUEL = 0;
    private static final int SLOT_INPUT = 1;
    private static final int SLOT_OUTPUT = 2;

    private String customName;

    public TileEntityNukeFurnace() {
        inventory = new ItemStackHandler(3){
            @Override
            protected void onContentsChanged(int slot) {
                markDirty();
                super.onContentsChanged(slot);
            }

            @Override
            public boolean isItemValid(int slot, ItemStack stack) {
                if(slot == SLOT_FUEL) {
                    return hasItemPower(stack);
                } else if(slot == SLOT_INPUT) {
                    ItemStack result = FurnaceRecipes.instance().getSmeltingResult(stack);
                    return result != null && !result.isEmpty();
                } else if(slot == SLOT_OUTPUT) {
                    return false;
                }
                return true;
            }
        };
    }

    public String getInventoryName() {
        return this.hasCustomInventoryName() ? this.customName : "container.nukeFurnace";
    }

    public boolean hasCustomInventoryName() {
        return this.customName != null && this.customName.length() > 0;
    }

    public void setCustomName(String name) {
        this.customName = name;
    }

    public boolean isUseableByPlayer(EntityPlayer player) {
        if(world.getTileEntity(pos) != this)
        {
            return false;
        }else{
            return player.getDistanceSq(pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D) <=64;
        }
    }

    public boolean hasItemPower(ItemStack itemStack) {
        return getItemPower(itemStack) > 0;
    }

    private static int getItemPower(ItemStack stack) {
        if(stack == null || stack.isEmpty()) {
            return 0;
        } else {
            int[] power = BreederRecipes.getFuelValue(stack);

            if(power == null){
                return (int)(Math.max(0, Math.sqrt(ContaminationUtil.getStackRads(stack))-7));
            }

            return power[0] * power[1] * 5;
        }
    }

    @Override
    public void readFromNBT(NBTTagCompound compound) {
        dualPower = compound.getShort("powerTime");
        dualCookTime = compound.getShort("CookTime");
        if(compound.hasKey("inventory"))
            inventory.deserializeNBT(compound.getCompoundTag("inventory"));
        super.readFromNBT(compound);
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound compound) {
        compound.setShort("powerTime", (short) dualPower);
        compound.setShort("cookTime", (short) dualCookTime);
        compound.setTag("inventory", inventory.serializeNBT());
        return super.writeToNBT(compound);
    }

    public int getDiFurnaceProgressScaled(int i) {
        return (dualCookTime * i) / processingSpeed;
    }

    public int getPowerRemainingScaled(int i) {
        return (dualPower * i) / maxPower;
    }

    public boolean canProcess() {
        if(inventory.getStackInSlot(SLOT_INPUT).isEmpty())
        {
            return false;
        }
        ItemStack itemStack = FurnaceRecipes.instance().getSmeltingResult(inventory.getStackInSlot(SLOT_INPUT));
        if(itemStack == null || itemStack.isEmpty())
        {
            return false;
        }

        if(inventory.getStackInSlot(SLOT_OUTPUT).isEmpty())
        {
            return true;
        }

        if(!inventory.getStackInSlot(SLOT_OUTPUT).isItemEqual(itemStack)) {
            return false;
        }

        int maxStack = Math.min(inventory.getStackInSlot(SLOT_OUTPUT).getMaxStackSize(), inventory.getSlotLimit(SLOT_OUTPUT));
        return inventory.getStackInSlot(SLOT_OUTPUT).getCount() < maxStack;
    }

    private void processItem() {
        if(canProcess()) {
            ItemStack itemStack = FurnaceRecipes.instance().getSmeltingResult(inventory.getStackInSlot(SLOT_INPUT));

            if(inventory.getStackInSlot(SLOT_OUTPUT).isEmpty())
            {
                inventory.setStackInSlot(SLOT_OUTPUT, itemStack.copy());
            }else if(inventory.getStackInSlot(SLOT_OUTPUT).isItemEqual(itemStack)) {
                inventory.getStackInSlot(SLOT_OUTPUT).grow(itemStack.getCount());
            }

            inventory.getStackInSlot(SLOT_INPUT).shrink(1);
            if(inventory.getStackInSlot(SLOT_INPUT).isEmpty())
            {
                inventory.setStackInSlot(SLOT_INPUT, ItemStack.EMPTY);
            }

            dualPower--;
        }
    }

    public boolean hasPower() {
        return dualPower > 0;
    }

    public boolean isProcessing() {
        return this.dualCookTime > 0;
    }

    @Override
    public void update() {
        boolean flag1 = false;

        if(!world.isRemote)
        {
            if(this.hasItemPower(inventory.getStackInSlot(SLOT_FUEL)) && this.dualPower < maxPower)
            {
                int powerToAdd = getItemPower(inventory.getStackInSlot(SLOT_FUEL));
                if(this.dualPower + powerToAdd > maxPower) {
                    powerToAdd = maxPower - this.dualPower;
                }
                this.dualPower += powerToAdd;

                if(!inventory.getStackInSlot(SLOT_FUEL).isEmpty())
                {
                    flag1 = true;
                    ItemStack container = inventory.getStackInSlot(SLOT_FUEL).getItem().getContainerItem(inventory.getStackInSlot(SLOT_FUEL));
                    inventory.getStackInSlot(SLOT_FUEL).shrink(1);
                    if(inventory.getStackInSlot(SLOT_FUEL).isEmpty())
                    {
                        if(container != null && !container.isEmpty()) {
                            inventory.setStackInSlot(SLOT_FUEL, container);
                        } else {
                            inventory.setStackInSlot(SLOT_FUEL, ItemStack.EMPTY);
                        }
                    }
                }
            }

            if(hasPower() && canProcess())
            {
                dualCookTime++;

                if(this.dualCookTime >= TileEntityNukeFurnace.processingSpeed)
                {
                    this.dualCookTime = 0;
                    this.processItem();
                    flag1 = true;
                }
            }else{
                dualCookTime = 0;
            }

            boolean trigger = true;

            if(hasPower() && canProcess() && this.dualCookTime == 0)
            {
                trigger = false;
            }

            if(trigger)
            {
                flag1 = true;
                MachineNukeFurnace.updateBlockState(this.dualCookTime > 0, this.world, pos);
            }
        }

        if(flag1)
        {
            this.markDirty();
        }
    }

    private int[] getAccessibleSlotsFromSide(EnumFacing facing) {
        if (facing == EnumFacing.UP) {
            return new int[] {SLOT_INPUT};
        } else if (facing == EnumFacing.DOWN) {
            return new int[] {SLOT_OUTPUT};
        } else {
            return new int[] {SLOT_FUEL};
        }
    }

    @Override
    public boolean hasCapability(Capability<?> capability, EnumFacing facing) {
        return capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY || super.hasCapability(capability, facing);
    }

    @Override
    public <T> T getCapability(Capability<T> capability, EnumFacing facing) {
        if (capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY) {
            if (facing == null) {
                return CapabilityItemHandler.ITEM_HANDLER_CAPABILITY.cast(inventory);
            }
            return CapabilityItemHandler.ITEM_HANDLER_CAPABILITY.cast(new SidedItemHandler(inventory, getAccessibleSlotsFromSide(facing)));
        }
        return super.getCapability(capability, facing);
    }

    private static class SidedItemHandler implements IItemHandler {
        private final ItemStackHandler handler;
        private final int[] slots;

        public SidedItemHandler(ItemStackHandler handler, int[] slots) {
            this.handler = handler;
            this.slots = slots;
        }

        @Override
        public int getSlots() {
            return slots.length;
        }

        @Override
        public ItemStack getStackInSlot(int slot) {
            if(slot >= 0 && slot < slots.length) {
                return handler.getStackInSlot(slots[slot]);
            }
            return ItemStack.EMPTY;
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            if(slot >= 0 && slot < slots.length) {
                int actualSlot = slots[slot];
                if(actualSlot == SLOT_FUEL) {
                    if(TileEntityNukeFurnace.getItemPower(stack) <= 0) {
                        return stack;
                    }
                } else if(actualSlot == SLOT_INPUT) {
                    ItemStack result = FurnaceRecipes.instance().getSmeltingResult(stack);
                    if(result == null || result.isEmpty()) {
                        return stack;
                    }
                } else if(actualSlot == SLOT_OUTPUT) {
                    return stack;
                }
                return handler.insertItem(actualSlot, stack, simulate);
            }
            return stack;
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            if(slot >= 0 && slot < slots.length) {
                int actualSlot = slots[slot];
                if(actualSlot == SLOT_OUTPUT) {
                    return handler.extractItem(actualSlot, amount, simulate);
                }
            }
            return ItemStack.EMPTY;
        }

        @Override
        public int getSlotLimit(int slot) {
            if(slot >= 0 && slot < slots.length) {
                return handler.getSlotLimit(slots[slot]);
            }
            return 0;
        }
    }
}