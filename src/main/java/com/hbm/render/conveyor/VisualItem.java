package com.hbm.render.conveyor;

import net.minecraft.item.ItemStack;

public class VisualItem {

    public final long uid;
    public ItemStack stack;

    public double posX, posY, posZ;
    public double prevX, prevY, prevZ;

    public float yaw;
    public float prevYaw;
    public float visualYaw;
    public float visualPrevYaw;

    public int lastUpdateTick;
    public double speed;

    public VisualItem(long uid, ItemStack stack, double x, double y, double z, float yaw, double speed, boolean stopped, int tick) {
        this.uid = uid;
        this.stack = stack.copy();
        this.posX = x;
        this.posY = y;
        this.posZ = z;
        this.prevX = x;
        this.prevY = y;
        this.prevZ = z;
        this.yaw = yaw;
        this.prevYaw = yaw;
        this.visualYaw = yaw;
        this.visualPrevYaw = yaw;
        this.lastUpdateTick = tick;
        this.speed = speed;
    }

    public void updateTarget(double tx, double ty, double tz, float yaw, ItemStack stack, double speed, boolean stopped, int tick) {
        this.stack = stack.copy();
        this.speed = speed;
        this.lastUpdateTick = tick;
        this.prevYaw = this.yaw;
        this.yaw = yaw;

        this.posX = tx;
        this.posY = ty;
        this.posZ = tz;
    }

    public void tick() {
        this.prevX = this.posX;
        this.prevY = this.posY;
        this.prevZ = this.posZ;
        this.visualPrevYaw = this.visualYaw;

        float yawDiff = this.yaw - this.visualYaw;
        while (yawDiff > 180.0F) yawDiff -= 360.0F;
        while (yawDiff < -180.0F) yawDiff += 360.0F;

        if (Math.abs(yawDiff) < 1.0F) {
            this.visualYaw = this.yaw;
        } else {
            this.visualYaw += yawDiff * 0.4F;
        }
    }

    public double getInterpolatedX(float pt) {
        return prevX + (posX - prevX) * pt;
    }

    public double getInterpolatedY(float pt) {
        return prevY + (posY - prevY) * pt;
    }

    public double getInterpolatedZ(float pt) {
        return prevZ + (posZ - prevZ) * pt;
    }

    public float getInterpolatedYaw(float pt) {
        float diff = visualYaw - visualPrevYaw;
        while (diff > 180.0F) diff -= 360.0F;
        while (diff < -180.0F) diff += 360.0F;
        return visualPrevYaw + diff * pt;
    }
}