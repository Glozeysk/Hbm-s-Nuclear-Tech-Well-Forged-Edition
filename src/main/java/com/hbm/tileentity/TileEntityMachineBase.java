package com.hbm.tileentity;

import com.hbm.blocks.ModBlocks;
import com.hbm.interfaces.Spaghetti;
import com.hbm.lib.ItemStackHandlerWrapper;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.fluids.FluidTank;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.ItemStackHandler;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Spaghetti("Not spaghetti in itself, but for the love of god please use this base class for all machines")
public abstract class TileEntityMachineBase extends TileEntityLoadedBase {

	public ItemStackHandler inventory;
	public Set<BlockPos> dummyBlocks = ConcurrentHashMap.newKeySet();
	private String customName;

	public TileEntityMachineBase(int scount) {
		this(scount, 64);
	}

	public TileEntityMachineBase(int scount, int slotlimit) {
		inventory = getNewInventory(scount, slotlimit);
	}

	public ItemStackHandler getNewInventory(int scount, int slotlimit){
		return new ItemStackHandler(scount){
			@Override
			protected void onContentsChanged(int slot) {
				super.onContentsChanged(slot);
				markDirty();
			}

			@Override
			public int getSlotLimit(int slot) {
				return slotlimit;
			}
		};
	}

	public String getInventoryName() {
		return this.hasCustomInventoryName() ? this.customName : getName();
	}

	public abstract String getName();

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
			return player.getDistanceSq(pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D) <=128;
		}
	}

	public int[] getAccessibleSlotsFromSide(EnumFacing e) {
		return new int[] {};
	}

	public int getGaugeScaled(int i, FluidTank tank) {
		return tank.getFluidAmount() * i / tank.getCapacity();
	}

	public void handleButtonPacket(int value, int meta) { }

	public boolean isItemValidForSlot(int i, ItemStack stack) {
		return true;
	}

	public boolean canInsertItem(int slot, ItemStack itemStack, int amount) {
		return this.isItemValidForSlot(slot, itemStack);
	}

	public boolean canExtractItem(int slot, ItemStack itemStack, int amount) {
		return true;
	}

	public int countMufflers() {

		int count = 0;

		for(EnumFacing dir : EnumFacing.VALUES)
			if(world.getBlockState(pos.offset(dir)).getBlock() == ModBlocks.muffler)
				count++;

		return count;
	}

	public float getVolume(int toSilence) {

		float volume = 1 - (countMufflers() / (float)toSilence);

		return Math.max(volume, 0);
	}

	@Override
	public <T> T getCapability(Capability<T> capability, EnumFacing facing) {
		if(capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY && inventory != null){
			if(facing == null)
				return CapabilityItemHandler.ITEM_HANDLER_CAPABILITY.cast(inventory);

			final int[] accessibleSlots = getAccessibleSlotsFromSide(facing);
			return CapabilityItemHandler.ITEM_HANDLER_CAPABILITY.cast(new ItemStackHandlerWrapper(inventory, accessibleSlots){
				@Override
				public ItemStack extractItem(int slot, int amount, boolean simulate) {
					int realSlot = mapSlotPublic(slot);
					if(realSlot == -1)
						return ItemStack.EMPTY;
					if(canExtractItem(realSlot, inventory.getStackInSlot(realSlot), amount))
						return super.extractItem(slot, amount, simulate);
					return ItemStack.EMPTY;
				}

				@Override
				public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
					int realSlot = mapSlotPublic(slot);
					if(realSlot == -1)
						return stack;
					if(canInsertItem(realSlot, stack, stack.getCount()))
						return super.insertItem(slot, stack, simulate);
					return stack;
				}
			});
		}
		return super.getCapability(capability, facing);
	}

	@Override
	public boolean hasCapability(Capability<?> capability, EnumFacing facing) {
		return (capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY && inventory != null) || super.hasCapability(capability, facing);
	}

	@Override
	public NBTTagCompound writeToNBT(NBTTagCompound compound) {
		compound.setTag("inventory", inventory.serializeNBT());

		net.minecraft.nbt.NBTTagList dummyList = new net.minecraft.nbt.NBTTagList();
		for (BlockPos p : dummyBlocks) {
			NBTTagCompound tag = new NBTTagCompound();
			tag.setInteger("dx", p.getX());
			tag.setInteger("dy", p.getY());
			tag.setInteger("dz", p.getZ());
			dummyList.appendTag(tag);
		}
		compound.setTag("dummyBlocks", dummyList);

		return super.writeToNBT(compound);
	}

	@Override
	public void readFromNBT(NBTTagCompound compound) {
		if (compound.hasKey("inventory"))
			inventory.deserializeNBT(compound.getCompoundTag("inventory"));

		dummyBlocks.clear();
		net.minecraft.nbt.NBTTagList dummyList = compound.getTagList("dummyBlocks", 10);
		for (int i = 0; i < dummyList.tagCount(); i++) {
			NBTTagCompound tag = dummyList.getCompoundTagAt(i);
			dummyBlocks.add(new BlockPos(tag.getInteger("dx"), tag.getInteger("dy"), tag.getInteger("dz")));
		}

		super.readFromNBT(compound);
	}
}