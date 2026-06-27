package com.hbm.blocks.network.conveyor;

public class ConveyorEntryPoints {
    private final double[][][] leftPoints;
    private final double[][][] rightPoints;
    private final double mergeProgress;

    public ConveyorEntryPoints(double[][][] leftPoints, double[][][] rightPoints, double mergeProgress) {
        this.leftPoints = leftPoints;
        this.rightPoints = rightPoints;
        this.mergeProgress = mergeProgress;
    }

    public double[][][] getLeftPoints() {
        return leftPoints;
    }

    public double[][][] getRightPoints() {
        return rightPoints;
    }

    public double getMergeProgress() {
        return mergeProgress;
    }

    public double[][] getEntryPoint(boolean isLeft, int laneIndex) {
        double[][][] points = isLeft ? leftPoints : rightPoints;
        if (points == null || laneIndex < 0 || laneIndex >= points.length) {
            return points != null && points.length > 0 ? points[0] : null;
        }
        return points[laneIndex];
    }

    public int getLaneCount() {
        return leftPoints != null ? leftPoints.length : 0;
    }
}