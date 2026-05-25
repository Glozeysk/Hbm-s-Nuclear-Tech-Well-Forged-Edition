package com.hbm.tileentity.machine;

import java.util.Random;
import com.hbm.interfaces.ILaserable;
import com.hbm.inventory.DFCRecipes;
import com.hbm.items.ModItems;
import com.hbm.items.weapon.ItemCrucible;
import com.hbm.packet.AuxParticlePacket;
import com.hbm.packet.PacketDispatcher;
import com.hbm.tileentity.IBufPacketReceiver;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.FurnaceRecipes;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ITickable;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.fml.common.network.NetworkRegistry.TargetPoint;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.ItemStackHandler;

public class TileEntityCrateTungsten extends TileEntityCrateBase implements ITickable, ILaserable, IBufPacketReceiver {
	private Random rand = new Random();
	public int heatTimer = 0, age = 0;
	public long joules = 0;

	public TileEntityCrateTungsten() { super(27, false); }
	@Override protected String getDefaultInventoryName() { return "container.crateTungsten"; }
	public static TileEntityCrateTungsten fromItemStack(ItemStack stack, EntityPlayer player) {
		TileEntityCrateTungsten te = new TileEntityCrateTungsten();
		te.initFromStack(stack, player);
		return te;
	}

	@Override public void update() {
		if (isFromItemStack()) return;
		if (!world.isRemote) {
			if (heatTimer > 0) {
				heatTimer--;
				PacketDispatcher.wrapper.sendToAllAround(new AuxParticlePacket(pos.getX(), pos.getY(), pos.getZ(), 4), new TargetPoint(world.provider.getDimension(), pos.getX(), pos.getY(), pos.getZ(), 50));
			}
			age++;
			if (age > 20) { networkPackNT(150); age = 0; }
		}
	}

	@Override public void serialize(ByteBuf buf) { buf.writeInt(heatTimer); buf.writeLong(joules); }
	@Override public void deserialize(ByteBuf buf) { this.heatTimer = buf.readInt(); this.joules = buf.readLong(); }

	@Override public void addEnergy(long energy, EnumFacing dir) {
		if (isFromItemStack()) return;
		heatTimer = 5;
		for (int i = 0; i < inventory.getSlots(); i++) {
			ItemStack stack = inventory.getStackInSlot(i);
			if (stack.isEmpty()) continue;
			ItemStack result = FurnaceRecipes.instance().getSmeltingResult(stack);
			long req = (long) (DFCRecipes.getRequiredFlux(stack) * 0.9D);
			if (req > -1 && energy > req && 0.001D > stack.getCount() * rand.nextDouble() * ((double)req/(double)energy)) result = DFCRecipes.getOutput(stack);
			if (stack.getItem() == ModItems.crucible && ItemCrucible.getCharges(stack) < 3 && energy > 10000000) ItemCrucible.charge(stack);
			if (result != null && !result.isEmpty()) {
				int size = stack.getCount();
				if (result.getCount() * size <= result.getMaxStackSize()) {
					inventory.setStackInSlot(i, result.copy());
					inventory.getStackInSlot(i).setCount(inventory.getStackInSlot(i).getCount() * size);
				}
			}
		}
		joules = energy;
	}

	@Override public void readFromNBT(NBTTagCompound compound) {
		super.readFromNBT(compound);
		if (compound.hasKey("heatTimer")) this.heatTimer = compound.getInteger("heatTimer");
	}
	@Override public NBTTagCompound writeToNBT(NBTTagCompound compound) {
		NBTTagCompound nbt = super.writeToNBT(compound);
		compound.setInteger("heatTimer", this.heatTimer);
		return nbt;
	}

	@Override public <T> T getCapability(Capability<T> capability, EnumFacing facing) {
		return capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY ? CapabilityItemHandler.ITEM_HANDLER_CAPABILITY.cast(inventory) : super.getCapability(capability, facing);
	}
}