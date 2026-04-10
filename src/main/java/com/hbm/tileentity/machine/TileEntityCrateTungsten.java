package com.hbm.tileentity.machine;

import java.util.Random;

import com.hbm.items.ModItems;
import com.hbm.items.tool.ItemKeyPin;
import com.hbm.lib.HBMSoundHandler;
import com.hbm.interfaces.ILaserable;
import com.hbm.items.weapon.ItemCrucible;
import com.hbm.packet.AuxParticlePacket;
import com.hbm.packet.PacketDispatcher;
import com.hbm.inventory.DFCRecipes;
import com.hbm.tileentity.IBufPacketReceiver;

import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.FurnaceRecipes;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ITickable;
import net.minecraft.util.SoundCategory;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.fml.common.network.NetworkRegistry.TargetPoint;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.ItemStackHandler;

public class TileEntityCrateTungsten extends TileEntityLockableBase implements ITickable, ILaserable, IBufPacketReceiver {

	public ItemStackHandler inventory;

	private Random rand = new Random();

	public int heatTimer = 0;
	public int age = 0;
	public long joules = 0;

	private ItemStack sourceStack = ItemStack.EMPTY;
	private EntityPlayer sourcePlayer = null;
	private int sourceSlotIndex = -1;
	private boolean isLoading = false;

	public TileEntityCrateTungsten() {
		inventory = new ItemStackHandler(27){
			@Override
			protected void onContentsChanged(int slot){
				markDirty();
				if (!isLoading) {
					saveToSourceStack();
				}
			}
		};
	}

	public static TileEntityCrateTungsten fromItemStack(ItemStack stack, EntityPlayer player) {
		TileEntityCrateTungsten te = new TileEntityCrateTungsten();
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

	public int getSourceSlotIndex() {
		return sourceSlotIndex;
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

	@Override
	public void update() {
		if (isFromItemStack()) return;

		if(!world.isRemote) {
			if(heatTimer > 0)
				heatTimer--;

			if(heatTimer > 0) {
				PacketDispatcher.wrapper.sendToAllAround(new AuxParticlePacket(pos.getX(), pos.getY(), pos.getZ(), 4), new TargetPoint(world.provider.getDimension(), pos.getX(), pos.getY(), pos.getZ(), 50));
			}
			age++;
			if(age > 20){
				networkPackNT(150);
				age = 0;
			}
		}
	}

	@Override
	public void serialize(ByteBuf buf) {
		buf.writeInt(heatTimer);
		buf.writeLong(joules);
	}

	@Override
	public void deserialize(ByteBuf buf) {
		this.heatTimer = buf.readInt();
		this.joules = buf.readLong();
	}

	@Override
	public void addEnergy(long energy, EnumFacing dir) {
		if (isFromItemStack()) return;

		heatTimer = 5;

		for(int i = 0; i < inventory.getSlots(); i++) {

			if(inventory.getStackInSlot(i).isEmpty())
				continue;

			ItemStack result = FurnaceRecipes.instance().getSmeltingResult(inventory.getStackInSlot(i));

			long requiredEnergy = DFCRecipes.getRequiredFlux(inventory.getStackInSlot(i));
			int count = inventory.getStackInSlot(i).getCount();
			requiredEnergy *= 0.9D;
			if(requiredEnergy > -1 && energy > requiredEnergy){
				if(0.001D > count * rand.nextDouble() * ((double)requiredEnergy/(double)energy)){
					result = DFCRecipes.getOutput(inventory.getStackInSlot(i));
				}
			}

			if(inventory.getStackInSlot(i).getItem() == ModItems.crucible && ItemCrucible.getCharges(inventory.getStackInSlot(i)) < 3 && energy > 10000000)
				ItemCrucible.charge(inventory.getStackInSlot(i));

			if(result != null && !result.isEmpty()){
				int size = inventory.getStackInSlot(i).getCount();

				if(result.getCount() * size <= result.getMaxStackSize()) {
					inventory.setStackInSlot(i, result.copy());
					inventory.getStackInSlot(i).setCount(inventory.getStackInSlot(i).getCount()*size);
				}
			}
		}
		joules = energy;
	}

	@Override
	public void readFromNBT(NBTTagCompound compound) {
		if(compound.hasKey("inventory"))
			inventory.deserializeNBT(compound.getCompoundTag("inventory"));
		if(compound.hasKey("heatTimer"))
			this.heatTimer = compound.getInteger("heatTimer");
		super.readFromNBT(compound);
	}

	@Override
	public NBTTagCompound writeToNBT(NBTTagCompound compound) {
		compound.setTag("inventory", inventory.serializeNBT());
		compound.setInteger("heatTimer", this.heatTimer);
		return super.writeToNBT(compound);
	}

	@Override
	public boolean hasCapability(Capability<?> capability, EnumFacing facing) {
		return capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY || super.hasCapability(capability, facing);
	}

	@Override
	public <T> T getCapability(Capability<T> capability, EnumFacing facing) {
		return capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY ? CapabilityItemHandler.ITEM_HANDLER_CAPABILITY.cast(inventory) : super.getCapability(capability, facing);
	}
}