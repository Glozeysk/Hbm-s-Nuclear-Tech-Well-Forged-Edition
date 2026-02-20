package com.hbm.packet;

import java.io.IOException;

import com.hbm.items.ModItems;
import com.hbm.items.machine.ItemForgeFluidIdentifier;
import com.hbm.lib.Library;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.EnumHand;
import net.minecraft.util.text.Style;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public class ItemFluidIDPacket implements IMessage {

	ItemStack stack;
	PacketBuffer buffer;

	public ItemFluidIDPacket() {

	}

	public ItemFluidIDPacket(ItemStack stack) {
		buffer = new PacketBuffer(Unpooled.buffer());
		buffer.writeCompoundTag(stack.writeToNBT(new NBTTagCompound()));
	}

	@Override
	public void fromBytes(ByteBuf buf) {
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
		if (buffer == null) {
			buffer = new PacketBuffer(Unpooled.buffer());
		}
		buf.writeBytes(buffer);
	}

	public static class Handler implements IMessageHandler<ItemFluidIDPacket, IMessage> {

		@Override
		public IMessage onMessage(ItemFluidIDPacket m, MessageContext ctx) {
			
			EntityPlayer p = ctx.getServerHandler().player;
			if(m.stack == null)
				return null;
			p.getServer().addScheduledTask(() -> {
				
				if(p.getHeldItemMainhand().getItem() != ModItems.forge_fluid_identifier && p.getHeldItemOffhand().getItem() != ModItems.forge_fluid_identifier)
					return;
				
				ItemStack stack = m.stack;
				
				if(stack.getItem() instanceof ItemForgeFluidIdentifier) {
					if(p.getHeldItemMainhand().getItem() == ModItems.forge_fluid_identifier && p.getHeldItemOffhand().getItem() == ModItems.forge_fluid_identifier){
						p.closeScreen();
						p.sendMessage(new TextComponentString("Долбаёб, ты нахуя идентификаторы в обе руки взял!?").setStyle(new Style().setColor(TextFormatting.RED)));
					}
					if(p.getHeldItemMainhand().getItem() == ModItems.forge_fluid_identifier){
						p.setHeldItem(EnumHand.MAIN_HAND, stack);
					}
					if(p.getHeldItemOffhand().getItem() == ModItems.forge_fluid_identifier){
						p.setHeldItem(EnumHand.OFF_HAND, stack);
					}
				}
			});

			
			return null;
		}
	}
}