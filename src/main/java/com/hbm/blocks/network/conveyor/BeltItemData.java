package com.hbm.blocks.network.conveyor;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

public class BeltItemData {

    private ItemStack stack;
    private int lane;
    private double progress;
    private boolean stopped;
    private long uniqueId;

    private static long nextId = Long.MIN_VALUE;

    public BeltItemData(ItemStack stack, int lane, double progress) {
        this.stack = stack.copy();
        this.lane = lane;
        this.progress = progress;
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
    public boolean isStopped() { return stopped; }
    public void setStopped(boolean stopped) { this.stopped = stopped; }

    public NBTTagCompound writeToNBT() {
        NBTTagCompound nbt = new NBTTagCompound();
        nbt.setTag("Stack", stack.writeToNBT(new NBTTagCompound()));
        nbt.setInteger("Lane", lane);
        nbt.setDouble("Progress", progress);
        nbt.setLong("UID", uniqueId);
        nbt.setBoolean("Stopped", stopped);
        return nbt;
    }

    public static BeltItemData readFromNBT(NBTTagCompound nbt) {
        ItemStack stack = new ItemStack(nbt.getCompoundTag("Stack"));
        int lane = nbt.getInteger("Lane");
        double progress = nbt.getDouble("Progress");
        BeltItemData data = new BeltItemData(stack, lane, progress);
        if (nbt.hasKey("UID")) data.setUniqueId(nbt.getLong("UID"));
        data.setStopped(nbt.getBoolean("Stopped"));
        return data;
    }
}