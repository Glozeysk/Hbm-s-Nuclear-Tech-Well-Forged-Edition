package com.hbm.blocks.network.conveyor;

import com.hbm.packet.threading.ThreadedPacket;
import io.netty.buffer.ByteBuf;
import net.minecraft.client.Minecraft;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public class PacketBeltRemove extends ThreadedPacket {

    private long segmentId;

    public PacketBeltRemove() {}

    public PacketBeltRemove(long segmentId) {
        this.segmentId = segmentId;
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeLong(segmentId);
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        segmentId = buf.readLong();
    }

    public static class Handler implements IMessageHandler<PacketBeltRemove, IMessage> {
        @Override
        @SideOnly(Side.CLIENT)
        public IMessage onMessage(PacketBeltRemove message, MessageContext ctx) {
            Minecraft mc = Minecraft.getMinecraft();
            mc.addScheduledTask(() -> {
                ClientBeltManager.get().removeSegment(message.segmentId);
            });
            return null;
        }
    }
}