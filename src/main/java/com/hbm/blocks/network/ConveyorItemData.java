package com.hbm.blocks.network;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.Vec3d;

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
    private float prevYaw;

    private boolean onArc;
    private ConveyorArc arc;
    private double arcParam;
    private int arcDestLane;

    private boolean stopped;

    private long uniqueId;
    private static long nextId = Long.MIN_VALUE;

    private int arcDestSlot = -1;

    public ConveyorItemData(ItemStack stack, int lane, double progress) {
        this.stack = stack.copy();
        this.lane = lane;
        this.progress = progress;
        this.yaw = 0.0F;
        this.prevYaw = 0.0F;
        this.onArc = false;
        this.arc = null;
        this.arcParam = 0.0D;
        this.arcDestLane = -1;
        this.stopped = false;
        this.uniqueId = nextId++;
    }



    public void startArc(ConveyorArc arc, int destLane) {
        this.onArc = true;
        this.arc = arc;
        this.arcParam = 0.0D;
        this.arcDestLane = destLane;
        this.arcDestSlot = -1;
    }

    public void clearArc() {
        this.onArc = false;
        this.arc = null;
        this.arcParam = 0.0D;
        this.arcDestLane = -1;
        this.arcDestSlot = -1;
    }

    public int getArcDestSlot() {
        return arcDestSlot;
    }

    public void setArcDestSlot(int slot) {
        this.arcDestSlot = slot;
    }

    public long getUniqueId() {
        return uniqueId;
    }

    public void setUniqueId(long id) {
        this.uniqueId = id;
    }

    public ItemStack getStack() {
        return stack;
    }

    public void setStack(ItemStack stack) {
        this.stack = stack.copy();
    }

    public int getLane() {
        return lane;
    }

    public void setLane(int lane) {
        this.lane = lane;
    }

    public double getProgress() {
        return progress;
    }

    public void setProgress(double progress) {
        this.progress = progress;
    }

    public float getYaw() {
        return yaw;
    }

    public float getPrevYaw() {
        return prevYaw;
    }

    public void setYaw(float yaw) {
        this.prevYaw = this.yaw;
        this.yaw = yaw;
    }

    public void setYawImmediate(float yaw) {
        this.yaw = yaw;
        this.prevYaw = yaw;
    }

    public float getInterpolatedYaw(float partialTicks) {
        float diff = yaw - prevYaw;
        while (diff <= -180.0F) diff += 360.0F;
        while (diff > 180.0F) diff -= 360.0F;
        return prevYaw + diff * partialTicks;
    }

    public boolean isOnArc() {
        return onArc;
    }

    public ConveyorArc getArc() {
        return arc;
    }

    public double getArcParam() {
        return arcParam;
    }

    public int getArcDestLane() {
        return arcDestLane;
    }

    public void setArcParam(double param) {
        this.arcParam = param;
    }

    public boolean isStopped() {
        return stopped;
    }

    public void setStopped(boolean stopped) {
        this.stopped = stopped;
    }

    public NBTTagCompound writeToNBT() {
        NBTTagCompound nbt = new NBTTagCompound();
        nbt.setTag("Stack", stack.writeToNBT(new NBTTagCompound()));
        nbt.setInteger("Lane", lane);
        nbt.setDouble("Progress", progress);
        nbt.setFloat("Yaw", yaw);
        nbt.setLong("UID", uniqueId);
        nbt.setBoolean("Stopped", stopped);
        nbt.setBoolean("OnArc", onArc);
        if (onArc && arc != null) {
            nbt.setDouble("ArcParam", arcParam);
            nbt.setInteger("ArcDestLane", arcDestLane);
            nbt.setInteger("ArcDestSlot", arcDestSlot);
            nbt.setDouble("ArcP0X", arc.p0.x);
            nbt.setDouble("ArcP0Y", arc.p0.y);
            nbt.setDouble("ArcP0Z", arc.p0.z);
            nbt.setDouble("ArcCPX", arc.cp.x);
            nbt.setDouble("ArcCPY", arc.cp.y);
            nbt.setDouble("ArcCPZ", arc.cp.z);
            nbt.setDouble("ArcP1X", arc.p1.x);
            nbt.setDouble("ArcP1Y", arc.p1.y);
            nbt.setDouble("ArcP1Z", arc.p1.z);
        }
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
        if (nbt.getBoolean("OnArc")) {
            ConveyorArc arc = new ConveyorArc(
                    new Vec3d(nbt.getDouble("ArcP0X"), nbt.getDouble("ArcP0Y"), nbt.getDouble("ArcP0Z")),
                    new Vec3d(nbt.getDouble("ArcCPX"), nbt.getDouble("ArcCPY"), nbt.getDouble("ArcCPZ")),
                    new Vec3d(nbt.getDouble("ArcP1X"), nbt.getDouble("ArcP1Y"), nbt.getDouble("ArcP1Z"))
            );
            data.startArc(arc, nbt.getInteger("ArcDestLane"));
            data.setArcParam(nbt.getDouble("ArcParam"));
            data.setArcDestSlot(nbt.getInteger("ArcDestSlot"));
        }
        return data;
    }
}