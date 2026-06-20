package com.hbm.blocks.network;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

public class ConveyorItemData {

    public static final double ITEM_LENGTH = 0.25D;
    public static final double HALF_ITEM = ITEM_LENGTH * 0.5D;
    public static final double ENTRY_PROGRESS = HALF_ITEM;
    public static final double EXIT_PROGRESS = 1.0D - HALF_ITEM;
    public static final int MAX_ITEMS_PER_LANE = 4;

    private ItemStack stack;
    private int lane;
    private double progress;
    private float yaw;
    private boolean stopped;

    private long uniqueId;
    private static long nextId = Long.MIN_VALUE;

    public ConveyorItemData(ItemStack stack, int lane, double progress) {
        this.stack = stack.copy();
        this.lane = lane;
        this.progress = progress;
        this.yaw = 0.0F;
        this.stopped = false;
        this.uniqueId = nextId++;
    }

    public long getUniqueId() { return uniqueId; }
    public void setUniqueId(long id) { this.uniqueId = id; }
    public ItemStack getStack() { return stack; }
    public void setStack(ItemStack stack) { this.stack = stack.copy(); }
    public int getLane() { return lane; }
    public void setLane(int lane) { this.lane = lane; }
    public double getProgress() { return progress; }
    public void setProgress(double progress) { this.progress = progress; }
    public float getYaw() { return yaw; }
    public void setYaw(float yaw) { this.yaw = yaw; }
    public void setYawImmediate(float yaw) { this.yaw = yaw; }
    public boolean isStopped() { return stopped; }
    public void setStopped(boolean stopped) { this.stopped = stopped; }

    public NBTTagCompound writeToNBT() {
        NBTTagCompound nbt = new NBTTagCompound();
        nbt.setTag("Stack", stack.writeToNBT(new NBTTagCompound()));
        nbt.setInteger("Lane", lane);
        nbt.setDouble("Progress", progress);
        nbt.setFloat("Yaw", yaw);
        nbt.setLong("UID", uniqueId);
        nbt.setBoolean("Stopped", stopped);
        return nbt;
    }

    public static ConveyorItemData readFromNBT(NBTTagCompound nbt) {
        ItemStack stack = new ItemStack(nbt.getCompoundTag("Stack"));
        int lane = nbt.getInteger("Lane");
        double progress = nbt.getDouble("Progress");
        ConveyorItemData data = new ConveyorItemData(stack, lane, progress);
        data.setYawImmediate(nbt.getFloat("Yaw"));
        if (nbt.hasKey("UID")) data.setUniqueId(nbt.getLong("UID"));
        data.setStopped(nbt.getBoolean("Stopped"));
        return data;
    }
}