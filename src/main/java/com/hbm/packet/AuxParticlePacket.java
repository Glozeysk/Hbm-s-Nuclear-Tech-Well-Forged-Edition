package com.hbm.packet;

import com.hbm.main.MainRegistry;
import com.hbm.packet.threading.ThreadedPacket;

import io.netty.buffer.ByteBuf;
import net.minecraft.client.Minecraft;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public class AuxParticlePacket extends ThreadedPacket {

	private double x;
	private double y;
	private double z;
	private int type;

	public AuxParticlePacket() {
	}

	public AuxParticlePacket(double x, double y, double z, int type) {
		this.x = x;
		this.y = y;
		this.z = z;
		this.type = type;
	}

	@Override
	public void fromBytes(ByteBuf buf) {
		x = buf.readDouble();
		y = buf.readDouble();
		z = buf.readDouble();
		type = buf.readInt();
	}

	@Override
	public void toBytes(ByteBuf buf) {
		buf.writeDouble(x);
		buf.writeDouble(y);
		buf.writeDouble(z);
		buf.writeInt(type);
	}

	public static class Handler implements IMessageHandler<AuxParticlePacket, IMessage> {

		@Override
		@SideOnly(Side.CLIENT)
		public IMessage onMessage(AuxParticlePacket m, MessageContext ctx) {
			Minecraft.getMinecraft().addScheduledTask(() -> {
				if (Minecraft.getMinecraft().world == null) return;
				MainRegistry.proxy.particleControl(m.x, m.y, m.z, m.type);
			});
			return null;
		}
	}
}