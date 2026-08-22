package com.hbm.main;

import com.hbm.handler.threading.PacketThreading;
import com.hbm.packet.threading.ThreadedPacket;
import gnu.trove.map.hash.TByteObjectHashMap;
import gnu.trove.map.hash.TObjectByteHashMap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.CodecException;
import io.netty.handler.codec.MessageToMessageCodec;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fml.common.network.FMLEmbeddedChannel;
import net.minecraftforge.fml.common.network.FMLOutboundHandler;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.network.internal.FMLProxyPacket;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.SimpleChannelHandlerWrapper;
import net.minecraftforge.fml.relauncher.Side;

import java.lang.ref.WeakReference;
import java.util.EnumMap;
import java.util.List;

import static net.minecraftforge.fml.common.network.FMLIndexedMessageToMessageCodec.INBOUNDPACKETTRACKER;

public class NetworkHandler {

    @ChannelHandler.Sharable
    private static class PrecompilingNetworkCodec extends MessageToMessageCodec<FMLProxyPacket, Object> {
        private final TByteObjectHashMap<Class<? extends IMessage>> discriminators = new TByteObjectHashMap<>();
        private final TObjectByteHashMap<Class<? extends IMessage>> types = new TObjectByteHashMap<>();
        private final String channelName;

        public PrecompilingNetworkCodec(String channelName) {
            this.channelName = channelName;
        }

        public void addDiscriminator(int discriminator, Class<? extends IMessage> type) {
            discriminators.put((byte) discriminator, type);
            types.put(type, (byte) discriminator);
        }

        @Override
        public void handlerAdded(ChannelHandlerContext ctx) {
            ctx.channel().attr(INBOUNDPACKETTRACKER).set(new ThreadLocal<>());
        }

        @Override
        protected void encode(ChannelHandlerContext ctx, Object msg, List<Object> out) {
            final byte discriminator;
            final Class<?> msgClass = msg.getClass();
            discriminator = types.get(msgClass);

            if (discriminator == 0) {
                throw new CodecException("Unregistered packet type " + msgClass.getName());
            }

            ByteBuf combined;

            if (msg instanceof ThreadedPacket packet) {
                ByteBuf payload = packet.consumeCompiledBuffer();
                try {
                    combined = Unpooled.buffer(1 + payload.readableBytes());
                    combined.writeByte(discriminator);
                    combined.writeBytes(payload, payload.readerIndex(), payload.readableBytes());
                } finally {
                    if (payload != null && payload.refCnt() > 0) {
                        payload.release();
                    }
                }
            } else if (msg instanceof IMessage message) {
                combined = Unpooled.buffer();
                combined.writeByte(discriminator);
                message.toBytes(combined);
            } else {
                throw new CodecException("Unknown packet type " + msgClass.getName());
            }

            FMLProxyPacket proxy = new FMLProxyPacket(new PacketBuffer(combined), channelName);
            out.add(proxy);
        }

        @Override
        protected void decode(ChannelHandlerContext ctx, FMLProxyPacket msg, List<Object> out) throws Exception {
            if (channelName == null || !channelName.equals(msg.channel())) {
                out.add(msg);
                return;
            }

            ByteBuf inboundBuf = msg.payload();
            byte discriminator = inboundBuf.readByte();
            Class<?> originalMsgClass = discriminators.get(discriminator);

            if (originalMsgClass == null) {
                throw new CodecException("Undefined message for discriminator " + discriminator + " in channel " + msg.channel());
            }

            Object newMsg = originalMsgClass.getDeclaredConstructor().newInstance();
            ctx.channel().attr(INBOUNDPACKETTRACKER).get().set(new WeakReference<>(msg));

            if (newMsg instanceof IMessage message) {
                message.fromBytes(inboundBuf.slice());
            } else {
                throw new CodecException("Unknown packet codec requested during decoding");
            }

            out.add(newMsg);
        }
    }

    private static FMLEmbeddedChannel clientChannel;
    private static FMLEmbeddedChannel serverChannel;
    private static PrecompilingNetworkCodec packetCodec;

    public NetworkHandler(String name) {
        packetCodec = new PrecompilingNetworkCodec(name);
        EnumMap<Side, FMLEmbeddedChannel> channels = NetworkRegistry.INSTANCE.newChannel(name, packetCodec);
        clientChannel = channels.get(Side.CLIENT);
        serverChannel = channels.get(Side.SERVER);
    }

    private static <REQ extends IMessage, REPLY extends IMessage> IMessageHandler<? super REQ, ? extends REPLY> instantiate(Class<? extends IMessageHandler<? super REQ, ? extends REPLY>> handler) {
        try {
            return handler.getDeclaredConstructor().newInstance();
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    public <REQ extends IMessage, REPLY extends IMessage> void registerMessage(Class<? extends IMessageHandler<REQ, REPLY>> messageHandler, Class<REQ> requestMessageType, int discriminator, Side side) {
        registerMessage(instantiate(messageHandler), requestMessageType, discriminator, side);
    }

    public <REQ extends IMessage, REPLY extends IMessage> void registerMessage(IMessageHandler<? super REQ, ? extends REPLY> messageHandler, Class<REQ> requestMessageType, int discriminator, Side side) {
        packetCodec.addDiscriminator(discriminator, requestMessageType);
        FMLEmbeddedChannel channel = side.isClient() ? clientChannel : serverChannel;
        String type = channel.findChannelHandlerNameForType(PrecompilingNetworkCodec.class);
        SimpleChannelHandlerWrapper<REQ, REPLY> handler = new SimpleChannelHandlerWrapper<>(messageHandler, side, requestMessageType);
        channel.pipeline().addAfter(type, messageHandler.getClass().getName(), handler);
    }

    public static void flushClientDirect() { clientChannel.flush(); }
    public static void flushServerDirect() { serverChannel.flush(); }

    public void sendToServer(IMessage message) {
        if (message instanceof ThreadedPacket packet) { PacketThreading.createSendToServerThreadedPacket(packet); return; }
        sendToServerDirect(message);
    }
    public void sendToDimension(IMessage message, int dimensionId) {
        if (message instanceof ThreadedPacket packet) { PacketThreading.createSendToDimensionThreadedPacket(packet, dimensionId); return; }
        sendToDimensionDirect(message, dimensionId);
    }
    public void sendToAllAround(IMessage message, NetworkRegistry.TargetPoint point) {
        if (message instanceof ThreadedPacket packet) { PacketThreading.createAllAroundThreadedPacket(packet, point); return; }
        sendToAllAroundDirect(message, point);
    }
    public void sendToAllTracking(IMessage message, NetworkRegistry.TargetPoint point) {
        if (message instanceof ThreadedPacket packet) { PacketThreading.createSendToAllTrackingThreadedPacket(packet, point); return; }
        sendToAllTrackingDirect(message, point);
    }
    public void sendToAllTracking(IMessage message, Entity entity) {
        if (message instanceof ThreadedPacket packet) { PacketThreading.createSendToAllTrackingThreadedPacket(packet, entity); return; }
        sendToAllTrackingDirect(message, entity);
    }
    public void sendTo(IMessage message, EntityPlayerMP player) {
        if (message instanceof ThreadedPacket packet) { PacketThreading.createSendToThreadedPacket(packet, player); return; }
        sendToDirect(message, player);
    }
    public void sendToAll(IMessage message) {
        if (message instanceof ThreadedPacket packet) { PacketThreading.createSendToAllThreadedPacket(packet); return; }
        sendToAllDirect(message);
    }

    public void sendToServerDirect(IMessage message) {
        clientChannel.attr(FMLOutboundHandler.FML_MESSAGETARGET).set(FMLOutboundHandler.OutboundTarget.TOSERVER);
        clientChannel.writeAndFlush(message);
    }
    public void sendToDimensionDirect(IMessage message, int dimensionId) {
        serverChannel.attr(FMLOutboundHandler.FML_MESSAGETARGET).set(FMLOutboundHandler.OutboundTarget.DIMENSION);
        serverChannel.attr(FMLOutboundHandler.FML_MESSAGETARGETARGS).set(dimensionId);
        serverChannel.writeAndFlush(message);
    }
    public void sendToAllAroundDirect(IMessage message, NetworkRegistry.TargetPoint point) {
        serverChannel.attr(FMLOutboundHandler.FML_MESSAGETARGET).set(FMLOutboundHandler.OutboundTarget.ALLAROUNDPOINT);
        serverChannel.attr(FMLOutboundHandler.FML_MESSAGETARGETARGS).set(point);
        serverChannel.writeAndFlush(message);
    }
    public void sendToAllTrackingDirect(IMessage message, NetworkRegistry.TargetPoint point) {
        serverChannel.attr(FMLOutboundHandler.FML_MESSAGETARGET).set(FMLOutboundHandler.OutboundTarget.TRACKING_POINT);
        serverChannel.attr(FMLOutboundHandler.FML_MESSAGETARGETARGS).set(point);
        serverChannel.writeAndFlush(message);
    }
    public void sendToAllTrackingDirect(IMessage message, Entity entity) {
        serverChannel.attr(FMLOutboundHandler.FML_MESSAGETARGET).set(FMLOutboundHandler.OutboundTarget.TRACKING_ENTITY);
        serverChannel.attr(FMLOutboundHandler.FML_MESSAGETARGETARGS).set(entity);
        serverChannel.writeAndFlush(message);
    }
    public void sendToDirect(IMessage message, EntityPlayerMP player) {
        serverChannel.attr(FMLOutboundHandler.FML_MESSAGETARGET).set(FMLOutboundHandler.OutboundTarget.PLAYER);
        serverChannel.attr(FMLOutboundHandler.FML_MESSAGETARGETARGS).set(player);
        serverChannel.writeAndFlush(message);
    }
    public void sendToAllDirect(IMessage message) {
        serverChannel.attr(FMLOutboundHandler.FML_MESSAGETARGET).set(FMLOutboundHandler.OutboundTarget.ALL);
        serverChannel.writeAndFlush(message);
    }
}