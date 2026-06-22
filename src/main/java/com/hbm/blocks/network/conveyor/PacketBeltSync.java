package com.hbm.blocks.network.conveyor;

import com.hbm.packet.threading.ThreadedPacket;
import io.netty.buffer.ByteBuf;
import net.minecraft.client.Minecraft;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.ArrayList;
import java.util.List;

public class PacketBeltSync extends ThreadedPacket {

    private long segmentId;
    private BlockPos[] blocks;
    private int facingIndex;
    private float speed;
    private int laneCount;
    private BeltItemData[] items;

    public PacketBeltSync() {}

    public PacketBeltSync(BeltSegment segment) {
        this.segmentId = segment.getSegmentId();
        List<BlockPos> blockList = segment.getBlocks();
        this.blocks = blockList.toArray(new BlockPos[0]);
        this.facingIndex = segment.getDirection().getIndex();
        this.speed = (float) segment.getSpeed();
        this.laneCount = segment.getLaneCount();
        List<BeltItemData> allItems = segment.getAllItems();
        this.items = allItems.toArray(new BeltItemData[0]);
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeLong(segmentId);
        buf.writeByte(blocks.length);
        for (BlockPos pos : blocks) {
            buf.writeLong(pos.toLong());
        }
        buf.writeByte(facingIndex);
        buf.writeFloat(speed);
        buf.writeByte(laneCount);
        buf.writeShort(items.length);
        for (BeltItemData item : items) {
            BeltBufCodec.writeBeltItem(buf, item);
        }
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        segmentId = buf.readLong();
        int blockCount = buf.readByte() & 0xFF;
        blocks = new BlockPos[blockCount];
        for (int i = 0; i < blockCount; i++) {
            blocks[i] = BlockPos.fromLong(buf.readLong());
        }
        facingIndex = buf.readByte();
        speed = buf.readFloat();
        laneCount = buf.readByte();
        int itemCount = buf.readShort() & 0xFFFF;
        items = new BeltItemData[itemCount];
        for (int i = 0; i < itemCount; i++) {
            items[i] = BeltBufCodec.readBeltItem(buf);
        }
    }

    public static class Handler implements IMessageHandler<PacketBeltSync, IMessage> {
        @Override
        @SideOnly(Side.CLIENT)
        public IMessage onMessage(PacketBeltSync message, MessageContext ctx) {
            Minecraft mc = Minecraft.getMinecraft();
            mc.addScheduledTask(() -> {
                if (mc.world == null) return;
                List<BlockPos> blockList = new ArrayList<>();
                for (BlockPos pos : message.blocks) {
                    blockList.add(pos);
                }
                EnumFacing facing = EnumFacing.byIndex(message.facingIndex);
                ClientBeltManager.get().applySync(
                        message.segmentId,
                        blockList,
                        facing,
                        message.speed,
                        message.laneCount,
                        message.items
                );
            });
            return null;
        }
    }
}