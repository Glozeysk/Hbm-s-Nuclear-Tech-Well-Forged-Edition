package com.hbm.tileentity.machine;

import com.hbm.blocks.generic.BlockStorageCrate;
import com.hbm.blocks.generic.ItemBlockStorageCrate;
import com.hbm.items.ModItems;
import com.hbm.items.tool.ItemKeyPin;
import com.hbm.lib.HBMSoundHandler;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.play.server.SPacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.IItemHandlerModifiable;
import net.minecraftforge.items.ItemStackHandler;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import static com.hbm.blocks.generic.BlockStorageCrate.OPEN;

public abstract class TileEntityCrateBase extends TileEntityLockableBase {

    public ItemStackHandler inventory;
    protected IItemHandler filteredInventory;
    protected String customName;
    protected ItemStack sourceStack = ItemStack.EMPTY;
    protected EntityPlayer sourcePlayer = null;
    protected int sourceSlotIndex = -1;
    protected boolean isLoading = false;
    private int openCount = 0;

    public TileEntityCrateBase(int size) { this(size, true); }
    public TileEntityCrateBase(int size, boolean useFiltered) {
        inventory = new ItemStackHandler(size) {
            @Override
            protected void onContentsChanged(int slot) {
                markDirty();
                if (!isLoading) saveToSourceStack();
                super.onContentsChanged(slot);
            }
        };
        filteredInventory = useFiltered ? new FilteredItemHandler(inventory) : inventory;
    }

    protected void initFromStack(ItemStack stack, EntityPlayer player) {
        this.sourceStack = stack;
        this.sourcePlayer = player;
        this.sourceSlotIndex = -1;
        this.loadFromItemStack();
    }

    protected void loadFromItemStack() {
        isLoading = true;
        try {
            if (sourceStack.hasTagCompound()) {
                NBTTagCompound nbt = sourceStack.getTagCompound();
                for (int i = 0; i < inventory.getSlots(); i++) {
                    if (nbt.hasKey("slot" + i)) {
                        inventory.setStackInSlot(i, new ItemStack(nbt.getCompoundTag("slot" + i)));
                    }
                }
            }
        } finally {
            isLoading = false;
        }
    }

    protected void saveToSourceStack() {
        if (sourceStack.isEmpty() || sourcePlayer == null || sourcePlayer.inventory == null) return;
        ItemStack target = findCurrentStack(sourcePlayer);
        if (target == null || target.isEmpty()) return;

        sourceStack = target;
        if (sourceSlotIndex == -1) {
            if (target == sourcePlayer.getHeldItemMainhand()) sourceSlotIndex = sourcePlayer.inventory.currentItem;
            else if (target == sourcePlayer.getHeldItemOffhand()) sourceSlotIndex = 40;
            else {
                for (int i = 0; i < sourcePlayer.inventory.getSizeInventory(); i++) {
                    if (sourcePlayer.inventory.getStackInSlot(i) == target) { sourceSlotIndex = i; break; }
                }
            }
        }

        NBTTagCompound nbt = target.hasTagCompound() ? target.getTagCompound() : new NBTTagCompound();
        for (int i = 0; i < inventory.getSlots(); i++) nbt.removeTag("slot" + i);
        for (int i = 0; i < inventory.getSlots(); i++) {
            ItemStack stack = inventory.getStackInSlot(i);
            if (!stack.isEmpty()) {
                NBTTagCompound slot = new NBTTagCompound();
                stack.writeToNBT(slot);
                nbt.setTag("slot" + i, slot);
            }
        }
        target.setTagCompound(nbt.isEmpty() ? null : nbt);
    }

    private ItemStack findCurrentStack(EntityPlayer player) {
        if (sourceSlotIndex != -1 && sourceSlotIndex < player.inventory.getSizeInventory()) {
            ItemStack s = player.inventory.getStackInSlot(sourceSlotIndex);
            if (isSameItem(s, sourceStack)) return s;
        }
        if (isSameItem(player.getHeldItemMainhand(), sourceStack)) return player.getHeldItemMainhand();
        if (isSameItem(player.getHeldItemOffhand(), sourceStack)) return player.getHeldItemOffhand();
        for (int i = 0; i < player.inventory.getSizeInventory(); i++) {
            ItemStack s = player.inventory.getStackInSlot(i);
            if (isSameItem(s, sourceStack)) return s;
        }
        return null;
    }

    protected boolean isSameItem(ItemStack a, ItemStack b) {
        if (a.isEmpty() && b.isEmpty()) return true;
        if (a.isEmpty() || b.isEmpty()) return false;
        return Item.getIdFromItem(a.getItem()) == Item.getIdFromItem(b.getItem()) && a.getMetadata() == b.getMetadata();
    }

    public boolean isFromItemStack() { return !sourceStack.isEmpty(); }
    public ItemStack getSourceStack() { return sourceStack; }
    public int getSourceSlotIndex() { return sourceSlotIndex; }

    public boolean canAccess(EntityPlayer player) {
        if (!this.isLocked() || player == null) return true;
        ItemStack stack = player.getHeldItemMainhand();
        if (stack.getItem() instanceof ItemKeyPin && ItemKeyPin.getPins(stack) == this.lock) {
            if (world != null) world.playSound(null, player.posX, player.posY, player.posZ, HBMSoundHandler.lockOpen, SoundCategory.BLOCKS, 1.0F, 1.0F);
            return true;
        }
        if (stack.getItem() == ModItems.key_red) {
            if (world != null) world.playSound(null, player.posX, player.posY, player.posZ, HBMSoundHandler.lockOpen, SoundCategory.BLOCKS, 1.0F, 1.0F);
            return true;
        }
        return this.tryPick(player);
    }

    public String getInventoryName() {
        return hasCustomInventoryName() ? customName : getDefaultInventoryName();
    }
    protected abstract String getDefaultInventoryName();
    public boolean hasCustomInventoryName() { return customName != null && !customName.isEmpty(); }
    public void setCustomName(String name) { this.customName = name; }

    public boolean isUseableByPlayer(EntityPlayer player) {
        if (isFromItemStack()) {
            if (sourcePlayer == null || sourceStack.isEmpty()) {
                System.out.println("Crate: sourcePlayer null or stack empty");
                return false;
            }
            if (sourceSlotIndex == -1) {
                boolean match = (isSameItem(player.getHeldItemMainhand(), sourceStack)
                        || isSameItem(player.getHeldItemOffhand(), sourceStack)) && sourcePlayer == player;
                if (!match) System.out.println("Crate: hands mismatch or player mismatch");
                return match;
            } else {
                boolean match = isSameItem(player.inventory.getStackInSlot(sourceSlotIndex), sourceStack) && sourcePlayer == player;
                if (!match) System.out.println("Crate: slot" + sourceSlotIndex + " mismatch");
                return match;
            }
        }
        if (world == null || world.getTileEntity(pos) != this) {
            System.out.println("Crate: world null or TE mismatch at " + pos);
            return false;
        }
        double dist = player.getDistanceSq(pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D);
        if (dist > 64) {
            System.out.println("Crate: too far, distance=" + dist);
            return false;
        }
        return true;
    }

    public void openInventory(EntityPlayer player) {
        openCount++;
        sendUpdateToClient();
    }

    public void closeInventory(EntityPlayer player) {
        openCount--;
        if (openCount < 0) openCount = 0;
        sendUpdateToClient();
    }

    private void sendUpdateToClient() {
        if (world != null && !world.isRemote) {
            IBlockState state = world.getBlockState(pos);
            world.notifyBlockUpdate(pos, state, state, 3);
            markDirty();
        }
    }

    @Override
    public NBTTagCompound getUpdateTag() {
        return writeToNBT(new NBTTagCompound());
    }

    @Override
    public void handleUpdateTag(NBTTagCompound tag) {
        readFromNBT(tag);
    }

    @Nullable
    @Override
    public SPacketUpdateTileEntity getUpdatePacket() {
        return new SPacketUpdateTileEntity(pos, 0, getUpdateTag());
    }

    @Override
    public void onDataPacket(net.minecraft.network.NetworkManager net, SPacketUpdateTileEntity pkt) {
        handleUpdateTag(pkt.getNbtCompound());
        world.markBlockRangeForRenderUpdate(pos, pos);
    }

    public int getOpenCount() {
        return openCount;
    }

    @Override
    public void readFromNBT(NBTTagCompound compound) {
        if (compound.hasKey("inventory")) inventory.deserializeNBT(compound.getCompoundTag("inventory"));
        if (compound.hasKey("openCount")) openCount = compound.getInteger("openCount");
        super.readFromNBT(compound);
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound compound) {
        compound.setTag("inventory", inventory.serializeNBT());
        compound.setInteger("openCount", openCount);
        return super.writeToNBT(compound);
    }

    @Override
    public boolean hasCapability(Capability<?> capability, EnumFacing facing) {
        return capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY || super.hasCapability(capability, facing);
    }

    @Override
    public <T> T getCapability(Capability<T> capability, EnumFacing facing) {
        if (capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY) return CapabilityItemHandler.ITEM_HANDLER_CAPABILITY.cast(filteredInventory);
        return super.getCapability(capability, facing);
    }

    protected class FilteredItemHandler implements IItemHandlerModifiable {
        private final ItemStackHandler wrapped;
        public FilteredItemHandler(ItemStackHandler wrapped) { this.wrapped = wrapped; }
        @Override public int getSlots() { return wrapped.getSlots(); }
        @Nonnull @Override public ItemStack getStackInSlot(int slot) { return wrapped.getStackInSlot(slot); }
        @Override public void setStackInSlot(int slot, @Nonnull ItemStack stack) { if (!ItemBlockStorageCrate.isContainer(stack)) wrapped.setStackInSlot(slot, stack); }
        @Nonnull @Override public ItemStack insertItem(int slot, @Nonnull ItemStack stack, boolean simulate) { if (ItemBlockStorageCrate.isContainer(stack)) return stack; return wrapped.insertItem(slot, stack, simulate); }
        @Nonnull @Override public ItemStack extractItem(int slot, int amount, boolean simulate) { return wrapped.extractItem(slot, amount, simulate); }
        @Override public int getSlotLimit(int slot) { return wrapped.getSlotLimit(slot); }
    }
}