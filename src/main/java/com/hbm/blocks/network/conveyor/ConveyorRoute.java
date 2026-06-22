package com.hbm.blocks.network.conveyor;

import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

public class ConveyorRoute {

    private final double[][] points;
    private final int mergeIndex;

    public ConveyorRoute(double[][] points, int mergeIndex) {
        this.points = points;
        this.mergeIndex = mergeIndex;
    }

    public int getPointCount() {
        return points.length;
    }

    public int getMergeIndex() {
        return mergeIndex;
    }

    public double getMergeProgress() {
        if (points.length <= 1) return 0.0D;
        return (double) mergeIndex / (points.length - 1);
    }

    public Vec3d samplePosition(BlockPos pos, EnumFacing facing, double localProgress) {
        if (points.length <= 1) {
            return toWorld(pos, facing, points[0][0], points[0][1]);
        }

        double scaled = localProgress * (points.length - 1);
        int idx = (int) Math.floor(scaled);
        double frac = scaled - idx;

        if (idx >= points.length - 1) {
            return toWorld(pos, facing, points[points.length - 1][0], points[points.length - 1][1]);
        }
        if (idx < 0) {
            return toWorld(pos, facing, points[0][0], points[0][1]);
        }

        double x = points[idx][0] + (points[idx + 1][0] - points[idx][0]) * frac;
        double z = points[idx][1] + (points[idx + 1][1] - points[idx][1]) * frac;
        return toWorld(pos, facing, x, z);
    }

    public float sampleYaw(EnumFacing facing, double localProgress) {
        if (points.length <= 1) return facingToYaw(facing);

        double scaled = localProgress * (points.length - 1);
        int idx = (int) Math.floor(scaled);

        if (idx >= points.length - 1) idx = points.length - 2;
        if (idx < 0) idx = 0;

        double dx = points[idx + 1][0] - points[idx][0];
        double dz = points[idx + 1][1] - points[idx][1];

        float localAngle = (float) Math.toDegrees(Math.atan2(-dx, dz));

        return rotateYawByFacing(facing, localAngle);
    }

    private Vec3d toWorld(BlockPos pos, EnumFacing facing, double gridX, double gridZ) {
        double lx = gridX / 16.0D - 0.5D;
        double lz = gridZ / 16.0D - 0.5D;

        double wx, wz;
        switch (facing) {
            case NORTH:
                wx = pos.getX() + 0.5D + lx;
                wz = pos.getZ() + 0.5D + lz;
                break;
            case SOUTH:
                wx = pos.getX() + 0.5D - lx;
                wz = pos.getZ() + 0.5D - lz;
                break;
            case WEST:
                wx = pos.getX() + 0.5D - lz;
                wz = pos.getZ() + 0.5D + lx;
                break;
            case EAST:
                wx = pos.getX() + 0.5D + lz;
                wz = pos.getZ() + 0.5D - lx;
                break;
            default:
                wx = pos.getX() + 0.5D + lx;
                wz = pos.getZ() + 0.5D + lz;
                break;
        }

        return new Vec3d(wx, pos.getY() + 0.25D, wz);
    }

    private float facingToYaw(EnumFacing facing) {
        switch (facing) {
            case SOUTH: return 0.0F;
            case WEST: return -90.0F;
            case NORTH: return -180.0F;
            case EAST: return 90.0F;
            default: return 0.0F;
        }
    }

    private float rotateYawByFacing(EnumFacing facing, float localYaw) {
        switch (facing) {
            case NORTH: return localYaw;
            case SOUTH: return localYaw + 180.0F;
            case WEST: return localYaw + 90.0F;
            case EAST: return localYaw - 90.0F;
            default: return localYaw;
        }
    }

    public static final ConveyorRoute FORWARD = new ConveyorRoute(new double[][]{
            {8, 0}, {8, 16}
    }, 0);

    public static final ConveyorRoute LEFT_ENTRY = new ConveyorRoute(new double[][]{
            {2, 8}, {6, 8}, {8, 10}, {8, 14}
    }, 2);

    public static final ConveyorRoute RIGHT_ENTRY = new ConveyorRoute(new double[][]{
            {14, 8}, {10, 8}, {8, 10}, {8, 14}
    }, 2);

    public static final ConveyorRoute BACK_ENTRY = new ConveyorRoute(new double[][]{
            {8, 0}, {8, 16}
    }, 0);
}