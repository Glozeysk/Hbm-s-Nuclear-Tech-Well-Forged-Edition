package com.hbm.packet;

import api.hbm.energy.IEnergyUser;
import com.hbm.packet.threading.ThreadedPacket;

import io.netty.buffer.ByteBuf;
import net.minecraft.client.Minecraft;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public class AuxElectricityPacket extends ThreadedPacket {

	private int x;
	private int y;
	private int z;
	private long charge;

	public AuxElectricityPacket() {
	}

	public AuxElectricityPacket(BlockPos pos, long charge) {
		this.x = pos.getX();
		this.y = pos.getY();
		this.z = pos.getZ();
		this.charge = charge;
	}

	public AuxElectricityPacket(int x, int y, int z, long power) {
		this.x = x;
		this.y = y;
		this.z = z;
		this.charge = power;
	}

	@Override
	public void fromBytes(ByteBuf buf) {
		x = buf.readInt();
		y = buf.readInt();
		z = buf.readInt();
		charge = buf.readLong();
	}

	@Override
	public void toBytes(ByteBuf buf) {
		buf.writeInt(x);
		buf.writeInt(y);
		buf.writeInt(z);
		buf.writeLong(charge);
	}

	public static class Handler implements IMessageHandler<AuxElectricityPacket, IMessage> {

		@Override
		@SideOnly(Side.CLIENT)
		public IMessage onMessage(AuxElectricityPacket m, MessageContext ctx) {
			Minecraft.getMinecraft().addScheduledTask(() -> {
				if (Minecraft.getMinecraft().world == null) return;
				BlockPos pos = new BlockPos(m.x, m.y, m.z);
				TileEntity te = Minecraft.getMinecraft().world.getTileEntity(pos);
				if (te instanceof IEnergyUser) {
					((IEnergyUser) te).setPower(m.charge);
				}
			});
			return null;
		}
	}
}