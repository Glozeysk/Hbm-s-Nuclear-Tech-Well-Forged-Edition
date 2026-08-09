package com.hbm.packet;

import com.hbm.handler.MeteorControlHandler;
import com.hbm.packet.PacketDispatcher;
import com.hbm.packet.threading.PrecompiledPacket;

import io.netty.buffer.ByteBuf;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import net.minecraftforge.fml.relauncher.Side;

public class MeteorControlPacket extends PrecompiledPacket {

	private boolean enabled;

	public MeteorControlPacket() {
	}

	public MeteorControlPacket(boolean enabled) {
		this.enabled = enabled;
	}

	@Override
	public void fromBytes(ByteBuf buf) {
		enabled = buf.readBoolean();
	}

	@Override
	public void toBytes(ByteBuf buf) {
		buf.writeBoolean(enabled);
	}

	public static class Handler implements IMessageHandler<MeteorControlPacket, IMessage> {

		@Override
		public IMessage onMessage(MeteorControlPacket message, MessageContext ctx) {
			if(ctx.side == Side.SERVER) {
				ctx.getServerHandler().player.server.addScheduledTask(() -> {
					EntityPlayerMP player = ctx.getServerHandler().player;
					if(player == null || player.world == null) {
						return;
					}
					if(player.canUseCommand(2, "hbm")) {
						MeteorControlHandler.setEnabled(player.world, message.enabled);
						PacketDispatcher.sendTo(new MeteorControlPacket(message.enabled), player);
					}
				});
			} else {
				Minecraft.getMinecraft().addScheduledTask(() -> MeteorControlHandler.syncClient(message.enabled));
			}
			return null;
		}
	}
}
