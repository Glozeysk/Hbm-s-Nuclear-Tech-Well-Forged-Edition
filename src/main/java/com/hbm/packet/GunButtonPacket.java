package com.hbm.packet;

import com.hbm.items.weapon.ItemGunBase;
import com.hbm.packet.threading.PrecompiledPacket;

import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.EnumHand;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public class GunButtonPacket extends PrecompiledPacket {

	private boolean state;
	private byte button;
	private EnumHand hand;

	public GunButtonPacket() { }

	public GunButtonPacket(boolean m1, byte b, EnumHand hand) {
		state = m1;
		button = b;
		this.hand = hand;
	}

	@Override
	public void fromBytes(ByteBuf buf) {
		state = buf.readBoolean();
		button = buf.readByte();
		hand = buf.readBoolean() ? EnumHand.MAIN_HAND : EnumHand.OFF_HAND;
	}

	@Override
	public void toBytes(ByteBuf buf) {
		buf.writeBoolean(state);
		buf.writeByte(button);
		buf.writeBoolean(hand == EnumHand.MAIN_HAND);
	}

	public static class Handler implements IMessageHandler<GunButtonPacket, IMessage> {

		@Override
		public IMessage onMessage(GunButtonPacket m, MessageContext ctx) {
			ctx.getServerHandler().player.server.addScheduledTask(() -> {
				EntityPlayerMP p = ctx.getServerHandler().player;
				if (p == null || p.world == null) return;

				if (!p.getHeldItem(m.hand).isEmpty() && p.getHeldItem(m.hand).getItem() instanceof ItemGunBase) {
					ItemGunBase item = (ItemGunBase) p.getHeldItem(m.hand).getItem();

					switch (m.button) {
						case 0:
							ItemGunBase.setIsMouseDown(p.getHeldItem(m.hand), m.state);
							if (m.state)
								item.startAction(p.getHeldItem(m.hand), p.world, p, true, m.hand);
							else
								item.endAction(p.getHeldItem(m.hand), p.world, p, true, m.hand);
							break;
						case 1:
							ItemGunBase.setIsAltDown(p.getHeldItem(m.hand), m.state);
							if (m.state)
								item.startAction(p.getHeldItem(m.hand), p.world, p, false, m.hand);
							else
								item.endAction(p.getHeldItem(m.hand), p.world, p, false, m.hand);
							break;
						case 2:
							if (item.canReload(p.getHeldItem(m.hand), p.world, p)) {
								item.startReloadAction(p.getHeldItem(m.hand), p.world, p, m.hand);
							}
							break;
					}
				}
			});

			return null;
		}
	}
}