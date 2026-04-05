package com.hbm.tileentity.machine;

import com.hbm.blocks.generic.ItemBlockStorageCrate;
import com.hbm.items.ModItems;
import com.hbm.items.tool.ItemKeyPin;
import com.hbm.lib.HBMSoundHandler;

import net.minecraft.item.ItemStack;
import net.minecraft.util.SoundCategory;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;

import javax.annotation.Nonnull;

public class TileEntityCrateSteel extends TileEntityLockableBase {

	public ItemStackHandler inventory;
	private IItemHandler filteredInventory;

	private String customName;

	private ItemStack sourceStack = ItemStack.EMPTY;
	private EntityPlayer sourcePlayer = null;
	private int sourceSlotIndex = -1;
	private boolean isLoading = false;

	public TileEntityCrateSteel() {
		this(54);
	}

	public TileEntityCrateSteel(int size) {
		inventory = new ItemStackHandler(size){
			@Override
			protected void onContentsChanged(int slot) {
				markDirty();
				if (!isLoading) {
					saveToSourceStack();
				}
				super.onContentsChanged(slot);
			}
		};
		filteredInventory = new FilteredItemHandler(inventory);
	}

	public static TileEntityCrateSteel fromItemStack(ItemStack stack, EntityPlayer player) {
		TileEntityCrateSteel te = new TileEntityCrateSteel();
		te.sourceStack = stack;
		te.sourcePlayer = player;
		te.sourceSlotIndex = player.inventory.currentItem;
		te.loadFromItemStack();
		return te;
	}

	private void loadFromItemStack() {
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

	private void saveToSourceStack() {
		if (sourceStack.isEmpty() || sourcePlayer == null) return;

		ItemStack currentStack = sourcePlayer.inventory.getStackInSlot(sourceSlotIndex);
		if (currentStack != sourceStack) return;

		NBTTagCompound nbt = sourceStack.hasTagCompound() ? sourceStack.getTagCompound() : new NBTTagCompound();

		for (int i = 0; i < inventory.getSlots(); i++) {
			nbt.removeTag("slot" + i);
		}

		for (int i = 0; i < inventory.getSlots(); i++) {
			ItemStack stack = inventory.getStackInSlot(i);
			if (!stack.isEmpty()) {
				NBTTagCompound slot = new NBTTagCompound();
				stack.writeToNBT(slot);
				nbt.setTag("slot" + i, slot);
			}
		}

		if (nbt.isEmpty()) {
			sourceStack.setTagCompound(null);
		} else {
			sourceStack.setTagCompound(nbt);
		}
	}

	public boolean isFromItemStack() {
		return !sourceStack.isEmpty();
	}

	public ItemStack getSourceStack() {
		return sourceStack;
	}

	public int getSourceSlotIndex() {
		return sourceSlotIndex;
	}

	public boolean canAccess(EntityPlayer player) {

		if(!this.isLocked() || player == null) {
			return true;
		} else {
			ItemStack stack = player.getHeldItemMainhand();

			if(stack.getItem() instanceof ItemKeyPin && ItemKeyPin.getPins(stack) == this.lock) {
				world.playSound(null, player.posX, player.posY, player.posZ, HBMSoundHandler.lockOpen, SoundCategory.BLOCKS, 1.0F, 1.0F);
				return true;
			}

			if(stack.getItem() == ModItems.key_red) {
				world.playSound(null, player.posX, player.posY, player.posZ, HBMSoundHandler.lockOpen, SoundCategory.BLOCKS, 1.0F, 1.0F);
				return true;
			}

			return this.tryPick(player);
		}
	}

	public String getInventoryName() {
		return this.hasCustomInventoryName() ? this.customName : "container.crateSteel";
	}

	public boolean hasCustomInventoryName() {
		return this.customName != null && this.customName.length() > 0;
	}

	public void setCustomName(String name) {
		this.customName = name;
	}

	public boolean isUseableByPlayer(EntityPlayer player) {
		if (isFromItemStack()) {
			ItemStack currentStack = player.inventory.getStackInSlot(sourceSlotIndex);
			return currentStack == sourceStack && sourcePlayer == player;
		}

		if (world.getTileEntity(pos) != this) {
			return false;
		} else {
			return player.getDistanceSq(pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D) <= 64;
		}
	}

	@Override
	public void readFromNBT(NBTTagCompound compound) {
		if(compound.hasKey("inventory"))
			inventory.deserializeNBT(compound.getCompoundTag("inventory"));
		super.readFromNBT(compound);
	}

	@Override
	public NBTTagCompound writeToNBT(NBTTagCompound compound) {
		compound.setTag("inventory", inventory.serializeNBT());
		return super.writeToNBT(compound);
	}

	@Override
	public <T> T getCapability(Capability<T> capability, EnumFacing facing) {
		if (capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY) {
			return CapabilityItemHandler.ITEM_HANDLER_CAPABILITY.cast(filteredInventory);
		}
		return super.getCapability(capability, facing);
	}

	@Override
	public boolean hasCapability(Capability<?> capability, EnumFacing facing) {
		return capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY || super.hasCapability(capability, facing);
	}

	private class FilteredItemHandler implements IItemHandler {

		private final ItemStackHandler wrapped;

		public FilteredItemHandler(ItemStackHandler wrapped) {
			this.wrapped = wrapped;
		}

		@Override
		public int getSlots() {
			return wrapped.getSlots();
		}

		@Nonnull
		@Override
		public ItemStack getStackInSlot(int slot) {
			return wrapped.getStackInSlot(slot);
		}

		@Nonnull
		@Override
		public ItemStack insertItem(int slot, @Nonnull ItemStack stack, boolean simulate) {
			if (ItemBlockStorageCrate.isContainer(stack)) {
				return stack;
			}
			return wrapped.insertItem(slot, stack, simulate);
		}

		@Nonnull
		@Override
		public ItemStack extractItem(int slot, int amount, boolean simulate) {
			return wrapped.extractItem(slot, amount, simulate);
		}

		@Override
		public int getSlotLimit(int slot) {
			return wrapped.getSlotLimit(slot);
		}
	}
}