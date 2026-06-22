package com.hbm.blocks.network.conveyor;

import io.netty.buffer.ByteBuf;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.fml.common.network.ByteBufUtils;

public final class BeltBufCodec {

    private BeltBufCodec() {}

    public static void writeItemStack(ByteBuf buf, ItemStack stack) {
        if (stack.isEmpty()) {
            ByteBufUtils.writeVarInt(buf, 0, 5);
            return;
        }
        int id = Item.getIdFromItem(stack.getItem());
        ByteBufUtils.writeVarInt(buf, id, 5);
        buf.writeByte(stack.getCount());
        buf.writeShort(stack.getMetadata());
        NBTTagCompound tag = stack.getTagCompound();
        if (tag != null) {
            buf.writeBoolean(true);
            ByteBufUtils.writeTag(buf, tag);
        } else {
            buf.writeBoolean(false);
        }
    }

    public static ItemStack readItemStack(ByteBuf buf) {
        int id = ByteBufUtils.readVarInt(buf, 5);
        if (id == 0) return ItemStack.EMPTY;
        Item item = Item.getItemById(id);
        if (item == null) return ItemStack.EMPTY;
        int count = buf.readByte() & 0xFF;
        int meta = buf.readShort();
        ItemStack stack = new ItemStack(item, count, meta);
        if (buf.readBoolean()) {
            stack.setTagCompound(ByteBufUtils.readTag(buf));
        }
        return stack;
    }

    public static void writeBeltItem(ByteBuf buf, BeltItemData item) {
        buf.writeLong(item.getUniqueId());
        buf.writeByte(item.getLane());
        buf.writeFloat((float) item.getProgress());
        buf.writeBoolean(item.isStopped());
        buf.writeByte(item.getRouteType());
        writeItemStack(buf, item.getStack());
    }

    public static BeltItemData readBeltItem(ByteBuf buf) {
        long uid = buf.readLong();
        int lane = buf.readByte();
        double progress = buf.readFloat();
        boolean stopped = buf.readBoolean();
        int routeType = buf.readByte();
        ItemStack stack = readItemStack(buf);

        BeltItemData item = new BeltItemData(stack, lane, progress);
        item.setUniqueId(uid);
        item.setStopped(stopped);
        item.setRouteType(routeType);
        return item;
    }
}