package com.hbm.packet;

import com.hbm.handler.threading.PacketThreading;
import com.hbm.interfaces.IControlReceiver;
import com.hbm.main.MainRegistry;
import com.hbm.packet.threading.PrecompiledPacket;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.PacketBuffer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

import java.io.IOException;
// paste to PacketDispatcher
// wrapper.registerMessage(NBTControlPacketTest.Handler.class, NBTControlPacketTest.class, i++, Side.SERVER);

// paste to GUIFwatzCore
// PacketThreading.createSendToServerThreadedPacket(new NBTControlPacketTest(new NBTTagCompound(), this.fwatz.getPos()));
public class NBTControlPacketTest extends PrecompiledPacket {

    private int x, y, z;
    private NBTTagCompound nbt;
    private ByteBuf payload;

    public NBTControlPacketTest() { }

    public NBTControlPacketTest(NBTTagCompound nbt, BlockPos pos) {
        this(nbt, pos.getX(), pos.getY(), pos.getZ());
    }

    public NBTControlPacketTest(NBTTagCompound nbt, int x, int y, int z) {
        this.nbt = nbt;
        this.x = x;
        this.y = y;
        this.z = z;
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(x);
        buf.writeInt(y);
        buf.writeInt(z);
        new PacketBuffer(buf).writeCompoundTag(nbt);
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        this.payload = buf.retain();
    }

    public static class Handler implements IMessageHandler<NBTControlPacketTest, IMessage> {

        @Override
        public IMessage onMessage(NBTControlPacketTest m, MessageContext ctx) {
            ctx.getServerHandler().player.server.addScheduledTask(() -> {
                try {
                    EntityPlayerMP p = ctx.getServerHandler().player;
                    if (p == null || p.world == null) return;
                    PacketBuffer buffer = new PacketBuffer(m.payload);
                    int x = buffer.readInt();
                    int y = buffer.readInt();
                    int z = buffer.readInt();
                    TileEntity te = p.world.getTileEntity(new BlockPos(x, y, z));
                    NBTTagCompound nbt = buffer.readCompoundTag();
                    if (nbt != null && te instanceof IControlReceiver tile) {
                        if (tile.hasPermission(p)) {
                            MainRegistry.logger.info("=============================");
                            MainRegistry.logger.info("PoolSize: " + PacketThreading.getPoolSize());
                            MainRegistry.logger.info("CorePoolSize: " + PacketThreading.getCorePoolSize());
                            MainRegistry.logger.info("GetActiveCount: " + PacketThreading.getActiveCount());
                            MainRegistry.logger.info("Maximum pool size: " + PacketThreading.getMaximumPoolSize());
                            MainRegistry.logger.info("Thread pool queue size: " + PacketThreading.getThreadPoolQueueSize());
                            MainRegistry.logger.info("Queue size: " + PacketThreading.getQueueSize());
                            tile.receiveControl(p, nbt);
                            tile.receiveControl(nbt);
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    m.payload.release();
                }
            });

            return null;
        }
    }
}