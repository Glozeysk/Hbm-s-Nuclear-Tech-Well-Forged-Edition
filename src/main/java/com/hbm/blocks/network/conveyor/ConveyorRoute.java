package com.hbm.blocks.network.conveyor;

import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

public class ConveyorRoute {

    private final double[][] points;
    private final double mergeProgress;

    public ConveyorRoute(double[][] points, double mergeProgress) {
        this.points = points;
        this.mergeProgress = mergeProgress;
    }

    public int getPointCount() {
        return points.length;
    }

    public int getMergeIndex() {
        return points.length - 1;
    }

    public double getMergeProgress() {
        return mergeProgress;
    }

    public Vec3d samplePosition(BlockPos pos, EnumFacing facing, double localProgress) {
        double[] grid = sampleGrid(localProgress);
        return toWorld(pos, facing, grid[0], grid[1]);
    }

    public float sampleYaw(BlockPos pos, EnumFacing facing, double localProgress, float forwardYaw) {
        if (localProgress >= mergeProgress) {
            return forwardYaw;
        }

        double start = BeltLane.ITEM_LENGTH * 0.5D;
        double delta = 0.01D;

        double t0 = localProgress - delta;
        double t1 = localProgress + delta;

        if (t0 < start) t0 = start;
        if (t1 > mergeProgress) t1 = mergeProgress;

        if (t1 <= t0) {
            return forwardYaw;
        }

        double[] a = sampleGrid(t0);
        double[] b = sampleGrid(t1);

        Vec3d wa = toWorld(pos, facing, a[0], a[1]);
        Vec3d wb = toWorld(pos, facing, b[0], b[1]);

        double dx = wb.x - wa.x;
        double dz = wb.z - wa.z;

        if (dx * dx + dz * dz < 1.0E-8D) {
            return forwardYaw;
        }

        return (float) Math.toDegrees(Math.atan2(dx, dz));
    }

    private double[] sampleGrid(double localProgress) {
        double startProgress = BeltLane.ITEM_LENGTH * 0.5D;
        double endProgress = mergeProgress;

        double t = localProgress;
        if (t < startProgress) t = startProgress;
        if (t > endProgress) t = endProgress;

        double normalized = (t - startProgress) / (endProgress - startProgress);
        if (normalized < 0.0D) normalized = 0.0D;
        if (normalized > 1.0D) normalized = 1.0D;

        double scaled = normalized * (points.length - 1);
        int idx = (int) Math.floor(scaled);
        double frac = scaled - idx;

        if (idx >= points.length - 1) {
            return new double[]{points[points.length - 1][0], points[points.length - 1][1]};
        }

        double x = points[idx][0] + (points[idx + 1][0] - points[idx][0]) * frac;
        double z = points[idx][1] + (points[idx + 1][1] - points[idx][1]) * frac;
        return new double[]{x, z};
    }

    private Vec3d toWorld(BlockPos pos, EnumFacing facing, double gridX, double gridZ) {
        double lx = gridX / 16.0D - 0.5D;
        double lz = gridZ / 16.0D - 0.5D;

        double wx;
        double wz;

        switch (facing) {
            case SOUTH:
                wx = pos.getX() + 0.5D + lx;
                wz = pos.getZ() + 0.5D + lz;
                break;
            case NORTH:
                wx = pos.getX() + 0.5D - lx;
                wz = pos.getZ() + 0.5D - lz;
                break;
            case EAST:
                wx = pos.getX() + 0.5D + lz;
                wz = pos.getZ() + 0.5D - lx;
                break;
            case WEST:
                wx = pos.getX() + 0.5D - lz;
                wz = pos.getZ() + 0.5D + lx;
                break;
            default:
                wx = pos.getX() + 0.5D + lx;
                wz = pos.getZ() + 0.5D + lz;
                break;
        }

        return new Vec3d(wx, pos.getY() + 0.25D, wz);
    }

    public static final ConveyorRoute LEFT_ENTRY = new ConveyorRoute(new double[][]{
            {14, 8},
            {10, 8},
            {8, 10}
    }, 0.625D);

    public static final ConveyorRoute RIGHT_ENTRY = new ConveyorRoute(new double[][]{
            {2, 8},
            {6, 8},
            {8, 10}
    }, 0.625D);

    public static ConveyorRoute getByType(int routeType) {
        switch (routeType) {
            case BeltItemData.ROUTE_LEFT_ENTRY: return LEFT_ENTRY;
            case BeltItemData.ROUTE_RIGHT_ENTRY: return RIGHT_ENTRY;
            default: return null;
        }
    }
}