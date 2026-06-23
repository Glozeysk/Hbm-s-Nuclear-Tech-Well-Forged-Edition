package com.hbm.blocks.network.conveyor;

import net.minecraft.item.ItemStack;

public class ClientBeltItem {

    public final long uid;
    public ItemStack stack;
    public int lane;

    public double serverProgress;
    public double renderProgress;
    public double prevRenderProgress;
    public boolean stopped;
    public int routeType;

    private static final double CORRECTION = 0.2D;
    private static final double HARD_SNAP = 0.5D;

    public ClientBeltItem(long uid, ItemStack stack, int lane, double progress, boolean stopped, int routeType) {
        this.uid = uid;
        this.stack = stack.copy();
        this.lane = lane;
        this.serverProgress = progress;
        this.renderProgress = progress;
        this.prevRenderProgress = progress;
        this.stopped = stopped;
        this.routeType = routeType;
    }

    public void updateFromServer(double progress, boolean stopped, ItemStack stack, int lane, int routeType) {
        this.stack = stack.copy();
        this.lane = lane;
        this.stopped = stopped;
        this.serverProgress = progress;
        this.routeType = routeType;

        double err = Math.abs(progress - renderProgress);
        if (err > HARD_SNAP) {
            this.renderProgress = progress;
            this.prevRenderProgress = progress;
        }
    }

    public void tick(double speed, double limit) {
        this.prevRenderProgress = this.renderProgress;
        if (stopped) return;

        this.renderProgress += speed;
        this.serverProgress += speed;

        double err = serverProgress - renderProgress;
        if (Math.abs(err) > 0.001D) {
            renderProgress += err * CORRECTION;
        }

        if (renderProgress > limit) {
            renderProgress = limit;
        }
    }

    public double getInterpolatedProgress(float partialTicks) {
        return prevRenderProgress + (renderProgress - prevRenderProgress) * partialTicks;
    }
}