package com.hbm.core;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import net.minecraft.network.Packet;
import net.minecraft.network.PacketBuffer;
import net.minecraft.network.play.INetHandlerPlayClient;
import net.minecraft.network.play.client.CPacketCustomPayload;
import net.minecraft.network.play.server.SPacketCustomPayload;
import net.minecraftforge.fml.common.network.handshake.NetworkDispatcher;
import net.minecraftforge.fml.common.network.internal.FMLProxyPacket;
import net.minecraftforge.fml.relauncher.Side;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import static com.hbm.lib.internal.UnsafeHolder.U;

@SuppressWarnings("unused")
public final class FMLNetworkHook {
    private static final int PART_SIZE = 0x100000 - 0x50;
    private static final int MAX_PARTS = 255;
    private static final long SIDE_OFF = fieldOffset(NetworkDispatcher.class, Side.class, "side");
    private static final long SP_DATA_OFF = fieldOffset(SPacketCustomPayload.class, ByteBuf.class, "data", "field_149171_b");
    private static final long SP_CHAN_OFF = fieldOffset(SPacketCustomPayload.class, String.class, "channel", "field_149172_a");
    private static final long CP_DATA_OFF = fieldOffset(CPacketCustomPayload.class, ByteBuf.class, "data", "field_149561_c");
    private static final long CP_CHAN_OFF = fieldOffset(CPacketCustomPayload.class, String.class, "channel", "field_149562_a");

    private FMLNetworkHook() {}

    private static long fieldOffset(Class<?> clz, Class<?> type, String... names) {
        try {
            Field field = findField(clz, type, names);
            return U.objectFieldOffset(field);
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    private static Field findField(Class<?> clz, Class<?> type, String... names) throws NoSuchFieldException {
        for (Field field : clz.getDeclaredFields()) {
            if (type.isAssignableFrom(field.getType()) || field.getType().isAssignableFrom(type)) {
                field.setAccessible(true);
                return field;
            }
        }
        for (String name : names) {
            try {
                Field field = clz.getDeclaredField(name);
                field.setAccessible(true);
                return field;
            } catch (NoSuchFieldException ignored) {}
        }
        throw new NoSuchFieldException(clz.getName());
    }

    private static void releaseCustomPayloadData(Object pkt) {
        try {
            if (pkt instanceof SPacketCustomPayload sp) {
                Object o = U.getReference(sp, SP_DATA_OFF);
                if (o != null) {
                    U.putReference(sp, SP_DATA_OFF, null);
                    ((ByteBuf) o).release();
                }
            } else if (pkt instanceof CPacketCustomPayload cp) {
                Object o = U.getReference(cp, CP_DATA_OFF);
                if (o != null) {
                    U.putReference(cp, CP_DATA_OFF, null);
                    ((ByteBuf) o).release();
                }
            }
        } catch (Throwable ignored) {}
    }

    @SuppressWarnings("unused")
    public static void networkDispatcherWrite(NetworkDispatcher self, ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Throwable {
        if (!(msg instanceof FMLProxyPacket pkt)) {
            ctx.write(msg, promise);
            return;
        }

        try {
            final Side side = (Side) U.getReference(self, SIDE_OFF);
            if (side == Side.CLIENT) {
                ctx.write(pkt.toC17Packet(), promise);
            } else {
                List<Packet<INetHandlerPlayClient>> parts = pkt.toS3FPackets();
                int sizeMinusOne = parts.size() - 1;
                for (int i = 0; i < sizeMinusOne; i++) {
                    ctx.write(parts.get(i), ctx.voidPromise());
                }
                ctx.write(parts.get(sizeMinusOne), promise);
            }
        } finally {
            if (pkt.payload() != null && pkt.payload().refCnt() > 0) {
                pkt.payload().release();
            }
        }
    }

    public static List<Packet<INetHandlerPlayClient>> fmlProxyPacketToS3FPackets(FMLProxyPacket self) throws Throwable {
        final ByteBuf buf = self.payload();
        final int len = buf.readableBytes();
        final int ri = buf.readerIndex();

        final ArrayList<Packet<INetHandlerPlayClient>> ret = new ArrayList<>(Math.min(4, (len / (PART_SIZE - 1)) + 2));

        try {
            if (len < PART_SIZE) {
                PacketBuffer pb = new PacketBuffer(buf.retainedSlice(ri, len));
                ret.add(new SPacketCustomPayload(self.channel(), pb));
                return ret;
            }

            int parts = (int) Math.ceil(len / (double) (PART_SIZE - 1));
            if (parts > MAX_PARTS) throw new IllegalArgumentException("Payload too large");

            PacketBuffer preamble = new PacketBuffer(Unpooled.buffer());
            preamble.writeString(self.channel());
            preamble.writeByte(parts);
            preamble.writeInt(len);
            ret.add(new SPacketCustomPayload("FML|MP", preamble));

            int offset = 0;
            for (int x = 0; x < parts; x++) {
                int dataLen = Math.min(PART_SIZE - 1, len - offset);
                ByteBuf slice = buf.retainedSlice(ri + offset, dataLen);
                ByteBuf header = Unpooled.buffer(1, 1).writeByte(x & 0xFF);
                ByteBuf combined = Unpooled.wrappedBuffer(header, slice);
                PacketBuffer pb = new PacketBuffer(combined);

                try {
                    ret.add(new SPacketCustomPayload("FML|MP", pb));
                } catch (Throwable t) {
                    combined.release();
                    throw t;
                }
                offset += dataLen;
            }
            return ret;
        } catch (Throwable t) {
            for (Packet<INetHandlerPlayClient> p : ret) releaseCustomPayloadData(p);
            throw t;
        }
    }
}