package com.hbm.util;

import com.hbm.main.MainRegistry;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufInputStream;
import io.netty.buffer.ByteBufOutputStream;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

public class BufferUtil {

    private static final Charset CHARSET = StandardCharsets.UTF_8;

    /**
     * Writes a string to a byte buffer
     */
    public static void writeString(@NotNull ByteBuf buf, @Nullable String value) {
        if (value == null) {
            buf.writeInt(-1);
            return;
        }
        final int lengthIndex = buf.writerIndex();
        buf.writeInt(0);
        final int length = buf.writeCharSequence(value, CHARSET);
        buf.setInt(lengthIndex, length);
    }

    /**
     * Reads a string from a byte buffer
     */
    @Nullable
    public static String readString(@NotNull ByteBuf buf) {
        final int length = buf.readInt();
        if (length < 0) return null;
        return buf.readCharSequence(length, CHARSET).toString();
    }

    /**
     * Writes an integer array to a buffer.
     */
    public static void writeIntArray(@NotNull ByteBuf buf, int @NotNull[] array) {
        buf.writeInt(array.length);
        for (int value : array) {
            buf.writeInt(value);
        }
    }

    /**
     * Reads an integer array from a buffer.
     */
    public static int @NotNull[] readIntArray(@NotNull ByteBuf buf) {
        int length = buf.readInt();
        int[] array = new int[length];
        for (int i = 0; i < length; i++) {
            array[i] = buf.readInt();
        }
        return array;
    }

    /**
     * Writes a vector to a buffer.
     */
    public static void writeVec3(@NotNull ByteBuf buf, @Nullable Vec3d vector) {
        buf.writeBoolean(vector != null);
        if (vector == null) return;
        buf.writeDouble(vector.x);
        buf.writeDouble(vector.y);
        buf.writeDouble(vector.z);
    }

    /**
     * Reads a vector from a buffer.
     */
    @Nullable
    public static Vec3d readVec3(@NotNull ByteBuf buf) {
        boolean vectorExists = buf.readBoolean();
        if (!vectorExists) {
            return null;
        }
        double x = buf.readDouble();
        double y = buf.readDouble();
        double z = buf.readDouble();

        return new Vec3d(x, y, z);
    }

    /**
     * Writes a NBTTagCompound to a buffer.
     */
    public static void writeNBT(@NotNull ByteBuf buf, @Nullable NBTTagCompound compound) {
        if (compound == null) {
            buf.writeShort(-1);
            return;
        }
        try (ByteBufOutputStream out = new ByteBufOutputStream(buf)) {
            int lengthWriterIndex = buf.writerIndex();
            buf.writeShort(0);
            int beforeDataIndex = buf.writerIndex();
            CompressedStreamTools.writeCompressed(compound, out);
            int afterDataIndex = buf.writerIndex();
            int length = afterDataIndex - beforeDataIndex;
            buf.setShort(lengthWriterIndex, (short) length);
        } catch (IOException e) {
            MainRegistry.logger.error("Failed to write NBTTagCompound to buffer", e);
            buf.writeShort(-1);
        }
    }

    /**
     * Reads a NBTTagCompound from a buffer.
     */
    @NotNull
    public static NBTTagCompound readNBT(@NotNull ByteBuf buf) {
        final short nbtLength = buf.readShort();
        if (nbtLength <= 0) return new NBTTagCompound();
        if (buf.readableBytes() < nbtLength) {
            MainRegistry.logger.error("Invalid NBT length: {} (readable={})", nbtLength, buf.readableBytes());
            return new NBTTagCompound();
        }
        ByteBuf nbtSlice = buf.readSlice(nbtLength);
        try (ByteBufInputStream in = new ByteBufInputStream(nbtSlice)) {
            return CompressedStreamTools.readCompressed(in);
        } catch (IOException e) {
            MainRegistry.logger.error("Failed to read NBTTagCompound from buffer", e);
            return new NBTTagCompound();
        }
    }

    /**
     * Writes an ItemStack to a buffer.
     */
    public static void writeItemStack(@NotNull ByteBuf buf, @Nullable ItemStack item) {
        if (item == null || item.isEmpty()) {
            buf.writeShort(-1);
        } else {
            buf.writeShort(Item.getIdFromItem(item.getItem()));
            buf.writeByte(item.getCount());
            buf.writeShort(item.getItemDamage());
            NBTTagCompound nbtTagCompound = null;

            if (item.getItem().isDamageable() || item.getItem().getShareTag()) {
                nbtTagCompound = item.getTagCompound();
            }
            writeNBT(buf, nbtTagCompound);
        }
    }

    /**
     * Reads an ItemStack from a buffer.
     */
    @NotNull
    public static ItemStack readItemStack(@NotNull ByteBuf buf) {
        short id = buf.readShort();
        if (id < 0) return ItemStack.EMPTY;

        byte quantity = buf.readByte();
        short meta = buf.readShort();
        Item item = Item.getItemById(id);

        ItemStack itemStack = new ItemStack(item, quantity, meta);
        itemStack.setTagCompound(readNBT(buf));

        return itemStack;
    }
}
