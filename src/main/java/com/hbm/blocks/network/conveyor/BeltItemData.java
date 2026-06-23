package com.hbm.blocks.network.conveyor;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

import java.util.concurrent.atomic.AtomicLong;

public class BeltItemData {

    public static final int ROUTE_FORWARD = 0;
    public static final int ROUTE_LEFT_ENTRY = 1;
    public static final int ROUTE_RIGHT_ENTRY = 2;
    public static final int ROUTE_CHUTE_ENTRY = 3;
    public static final int ROUTE_LIFT_ENTRY = 4;

    private static final AtomicLong UID_COUNTER = new AtomicLong(Long.MIN_VALUE);

    private long uniqueId;
    private ItemStack stack;
    private int lane;
    private double progress;
    private boolean stopped;
    private int routeType;

    public BeltItemData(ItemStack stack, int lane, double progress) {
        this.uniqueId = UID_COUNTER.getAndIncrement();
        this.stack = stack.copy();
        this.lane = lane;
        this.progress = progress;
        this.stopped = false;
        this.routeType = ROUTE_FORWARD;
    }

    private BeltItemData() {}

    public long getUniqueId() { return uniqueId; }
    public void setUniqueId(long uid) { this.uniqueId = uid; }
    public ItemStack getStack() { return stack; }
    public void setStack(ItemStack stack) { this.stack = stack.copy(); }
    public int getLane() { return lane; }
    public void setLane(int lane) { this.lane = lane; }
    public double getProgress() { return progress; }
    public void setProgress(double progress) { this.progress = progress; }
    public boolean isStopped() { return stopped; }
    public void setStopped(boolean stopped) { this.stopped = stopped; }
    public int getRouteType() { return routeType; }
    public void setRouteType(int routeType) { this.routeType = routeType; }

    public NBTTagCompound writeToNBT() {
        NBTTagCompound nbt = new NBTTagCompound();
        nbt.setLong("UID", uniqueId);
        nbt.setTag("Stack", stack.writeToNBT(new NBTTagCompound()));
        nbt.setInteger("Lane", lane);
        nbt.setDouble("Progress", progress);
        nbt.setBoolean("Stopped", stopped);
        nbt.setInteger("RouteType", routeType);
        return nbt;
    }

    public static BeltItemData readFromNBT(NBTTagCompound nbt) {
        BeltItemData data = new BeltItemData();
        data.uniqueId = nbt.getLong("UID");
        data.stack = new ItemStack(nbt.getCompoundTag("Stack"));
        data.lane = nbt.getInteger("Lane");
        data.progress = nbt.getDouble("Progress");
        data.stopped = nbt.getBoolean("Stopped");
        data.routeType = nbt.getInteger("RouteType");
        return data;
    }
}