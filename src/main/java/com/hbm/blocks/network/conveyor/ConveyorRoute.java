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

    public double getMergeProgress() {
        return mergeProgress;
    }

    public Vec3d samplePosition(BlockPos pos, EnumFacing facing, double localProgress) {
        double[] grid = sampleGrid(localProgress);
        return toWorld(pos, facing, grid[0], grid[1]);
    }

    public Vec3d samplePosition(BlockPos pos, EnumFacing facing, double localProgress, int targetLane, double[] laneOffsets) {
        double startProgress = BeltLane.ITEM_LENGTH * 0.5D;
        double endProgress = mergeProgress;

        if (localProgress >= endProgress) {
            double forwardProgress = localProgress - endProgress + BeltLane.ITEM_LENGTH * 0.5D;
            double laneOffset = (targetLane < laneOffsets.length) ? laneOffsets[targetLane] : 0.0D;
            double laneX = 8.0D + laneOffset * 16.0D;
            return toWorld(pos, facing, laneX, 8.0D + forwardProgress * 16.0D);
        }

        double t = localProgress;
        if (t < startProgress) t = startProgress;

        double normalized = (t - startProgress) / (endProgress - startProgress);
        normalized = Math.max(0.0D, Math.min(1.0D, normalized));

        double entryX = points[0][0];
        double entryZ = points[0][1];
        double laneOffset = (targetLane < laneOffsets.length) ? laneOffsets[targetLane] : 0.0D;
        double laneX = 8.0D + laneOffset * 16.0D;
        double turnX = 8.0D;
        double turnZ = 10.0D;

        double currentX, currentZ;
        if (normalized < 0.5D) {
            double phase = normalized * 2.0D;
            double smoothPhase = phase * phase * (3 - 2 * phase);
            currentX = entryX + (turnX - entryX) * smoothPhase;
            currentZ = entryZ + (turnZ - entryZ) * smoothPhase;
        } else {
            double phase = (normalized - 0.5D) * 2.0D;
            double smoothPhase = phase * phase * (3 - 2 * phase);
            currentX = turnX + (laneX - turnX) * smoothPhase;
            currentZ = turnZ;
        }

        return toWorld(pos, facing, currentX, currentZ);
    }

    public float sampleYaw(BlockPos pos, EnumFacing facing, double localProgress, float forwardYaw) {
        double startProgress = BeltLane.ITEM_LENGTH * 0.5D;
        double endProgress = mergeProgress;

        if (localProgress >= endProgress) {
            return forwardYaw;
        }

        double normalized = (localProgress - startProgress) / (endProgress - startProgress);
        normalized = Math.max(0.0D, Math.min(1.0D, normalized));

        float turnAngle = (points[0][0] > 8) ? -45.0F : 45.0F;
        float currentYaw = forwardYaw + turnAngle * (1.0F - (float) normalized);

        return currentYaw;
    }

    public float sampleScale(BlockPos pos, EnumFacing facing, double localProgress) {
        double startProgress = BeltLane.ITEM_LENGTH * 0.5D;
        double endProgress = mergeProgress;

        if (localProgress >= endProgress) {
            return 1.0F;
        }

        double normalized = (localProgress - startProgress) / (endProgress - startProgress);
        normalized = Math.max(0.0D, Math.min(1.0D, normalized));

        double smoothNormalized = normalized * normalized * (3 - 2 * normalized);
        float minScale = 0.3F;
        return (float) (minScale + (1.0F - minScale) * smoothNormalized);
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

        double entryX = points[0][0];
        double entryZ = points[0][1];
        double mergeX = 8.0D;
        double mergeZ = 10.0D;

        double currentX = entryX + (mergeX - entryX) * normalized;
        double currentZ = entryZ + (mergeZ - entryZ) * normalized;

        return new double[]{currentX, currentZ};
    }

    private Vec3d toWorld(BlockPos pos, EnumFacing facing, double gridX, double gridZ) {
        double lx = gridX / 16.0D - 0.5D;
        double lz = gridZ / 16.0D - 0.5D;

        double wx, wz;

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

    public static final ConveyorRoute RIGHT_ENTRY = new ConveyorRoute(new double[][]{
            {14, 8}
    }, 0.5D);

    public static final ConveyorRoute LEFT_ENTRY = new ConveyorRoute(new double[][]{
            {2, 8}
    }, 0.5D);

    public static ConveyorRoute getByType(int routeType) {
        switch (routeType) {
            case BeltItemData.ROUTE_LEFT_ENTRY: return LEFT_ENTRY;
            case BeltItemData.ROUTE_RIGHT_ENTRY: return RIGHT_ENTRY;
            default: return null;
        }
    }
}