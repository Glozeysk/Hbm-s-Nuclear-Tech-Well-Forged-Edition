package com.hbm.packet;

import com.hbm.packet.threading.ThreadedPacket;
import com.hbm.tileentity.machine.TileEntityMachineChemical;

import io.netty.buffer.ByteBuf;
import net.minecraft.client.Minecraft;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public class TEChemicalPacket extends ThreadedPacket {

	private int x;
	private int y;
	private int z;
	private boolean isProgressing;

	public TEChemicalPacket() {
	}

	public TEChemicalPacket(int x, int y, int z, boolean isProgressing) {
		this.x = x;
		this.y = y;
		this.z = z;
		this.isProgressing = isProgressing;
	}

	@Override
	public void fromBytes(ByteBuf buf) {
		x = buf.readInt();
		y = buf.readInt();
		z = buf.readInt();
		isProgressing = buf.readBoolean();
	}

	@Override
	public void toBytes(ByteBuf buf) {
		buf.writeInt(x);
		buf.writeInt(y);
		buf.writeInt(z);
		buf.writeBoolean(isProgressing);
	}

	public static class Handler implements IMessageHandler<TEChemicalPacket, IMessage> {

		@Override
		@SideOnly(Side.CLIENT)
		public IMessage onMessage(TEChemicalPacket m, MessageContext ctx) {
			Minecraft.getMinecraft().addScheduledTask(() -> {
				if (Minecraft.getMinecraft().world == null) return;

				TileEntity te = Minecraft.getMinecraft().world.getTileEntity(new BlockPos(m.x, m.y, m.z));

				if (te instanceof TileEntityMachineChemical) {
					((TileEntityMachineChemical) te).isProgressing = m.isProgressing;
				}
			});

			return null;
		}
	}
}