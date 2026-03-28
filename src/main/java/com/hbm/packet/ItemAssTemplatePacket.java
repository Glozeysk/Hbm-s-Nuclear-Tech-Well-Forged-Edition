package com.hbm.packet;

import java.io.IOException;

import com.hbm.items.machine.ItemAssemblyTemplate;
import com.hbm.items.machine.ItemChemistryTemplate;
import com.hbm.tileentity.machine.TileEntityMachineAssembler;
import com.hbm.tileentity.machine.TileEntityMachineAssembly;
import com.hbm.tileentity.machine.TileEntityMachineChemplant;
import com.hbm.tileentity.machine.TileEntityMachineChemfac;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.PacketBuffer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public class ItemAssTemplatePacket implements IMessage {

	int x;
	int y;
	int z;
	ItemStack stack;
	PacketBuffer buffer;
	int slot;

	public ItemAssTemplatePacket() {

	}

	public ItemAssTemplatePacket(BlockPos pos, ItemStack stack, int slot) {
		this.x = pos.getX();
		this.y = pos.getY();
		this.z = pos.getZ();
		this.slot = slot;
		buffer = new PacketBuffer(Unpooled.buffer());
		buffer.writeCompoundTag(stack.writeToNBT(new NBTTagCompound()));
	}

	@Override
	public void fromBytes(ByteBuf buf) {
		x = buf.readInt();
		y = buf.readInt();
		z = buf.readInt();
		slot = buf.readInt();
		if (buffer == null) {
			buffer = new PacketBuffer(Unpooled.buffer());
		}
		buffer.writeBytes(buf);
		try {
			stack = new ItemStack(buffer.readCompoundTag());
		} catch(IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void toBytes(ByteBuf buf) {
		buf.writeInt(x);
		buf.writeInt(y);
		buf.writeInt(z);
		buf.writeInt(slot);
		if (buffer == null) {
			buffer = new PacketBuffer(Unpooled.buffer());
		}
		buf.writeBytes(buffer);
	}

	public static class Handler implements IMessageHandler<ItemAssTemplatePacket, IMessage> {

    @Override
    public IMessage onMessage(ItemAssTemplatePacket m, MessageContext ctx) {
        EntityPlayerMP p = ctx.getServerHandler().player;
        if (m.stack == null || m.stack.isEmpty())
            return null;

        // Fix: Use Server World Scheduler (p.getServerWorld()), NOT Minecraft.getMinecraft() (Client)
        p.getServerWorld().addScheduledTask(() -> {

            ItemStack stack = m.stack;
            BlockPos pos = new BlockPos(m.x, m.y, m.z);
			int slot = m.slot;

            // Fix: Check if block is loaded to prevent crashes/issues
            if (p.world.isBlockLoaded(pos)) {
                // Fix: Use server world (p.world)
                TileEntity te = p.world.getTileEntity(pos);

                if (te instanceof TileEntityMachineAssembler) {
                    TileEntityMachineAssembler gen = (TileEntityMachineAssembler) te;
                    if (stack.getItem() instanceof ItemAssemblyTemplate) {

                        // Update TileEntity
                        gen.inventory.setStackInSlot(slot, stack.copy());
                        gen.markDirty(); // Fix: Mark dirty to ensure data saves/syncs

                        // Fix: Force block update to sync TileEntity data to client
                        IBlockState state = p.world.getBlockState(pos);
                        p.world.notifyBlockUpdate(pos, state, state, 3);
                    }
                }
                if (te instanceof TileEntityMachineAssembly) {
                    TileEntityMachineAssembly gen = (TileEntityMachineAssembly) te;
                    if (stack.getItem() instanceof ItemAssemblyTemplate) {

                        // Update TileEntity
                        gen.inventory.setStackInSlot(slot, stack.copy());
                        gen.markDirty(); // Fix: Mark dirty to ensure data saves/syncs

                        // Fix: Force block update to sync TileEntity data to client
                        IBlockState state = p.world.getBlockState(pos);
                        p.world.notifyBlockUpdate(pos, state, state, 3);
                    }
                }
				if (te instanceof TileEntityMachineChemplant) {
                    TileEntityMachineChemplant gen = (TileEntityMachineChemplant) te;
                    if (stack.getItem() instanceof ItemChemistryTemplate) {

                        // Update TileEntity
                        gen.inventory.setStackInSlot(slot, stack.copy());
                        gen.markDirty(); // Fix: Mark dirty to ensure data saves/syncs

                        // Fix: Force block update to sync TileEntity data to client
                        IBlockState state = p.world.getBlockState(pos);
                        p.world.notifyBlockUpdate(pos, state, state, 3);
                    }
                }
				if (te instanceof TileEntityMachineChemfac) {
                    TileEntityMachineChemfac gen = (TileEntityMachineChemfac) te;
                    if (stack.getItem() instanceof ItemChemistryTemplate) {

                        // Update TileEntity
                        gen.inventory.setStackInSlot(slot, stack.copy());
                        gen.markDirty(); // Fix: Mark dirty to ensure data saves/syncs

                        // Fix: Force block update to sync TileEntity data to client
                        IBlockState state = p.world.getBlockState(pos);
                        p.world.notifyBlockUpdate(pos, state, state, 3);
                    }
                }
            }
        });

        return null;
    }
}
}