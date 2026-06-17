package com.hbm.blocks.network;

import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

public final class ConveyorArc {

    public final Vec3d p0;
    public final Vec3d cp;
    public final Vec3d p1;

    public ConveyorArc(Vec3d p0, Vec3d cp, Vec3d p1) {
        this.p0 = p0;
        this.cp = cp;
        this.p1 = p1;
    }

    public Vec3d evaluate(double t) {
        double u = 1.0D - t;
        double x = u * u * p0.x + 2 * u * t * cp.x + t * t * p1.x;
        double y = u * u * p0.y + 2 * u * t * cp.y + t * t * p1.y;
        double z = u * u * p0.z + 2 * u * t * cp.z + t * t * p1.z;
        return new Vec3d(x, y, z);
    }

    public double approximateLength(int segments) {
        double len = 0.0D;
        Vec3d prev = p0;
        for (int i = 1; i <= segments; i++) {
            Vec3d cur = evaluate((double) i / segments);
            len += prev.distanceTo(cur);
            prev = cur;
        }
        return len;
    }

    public double paramAtDistance(double dist, int segments) {
        double totalLen = 0.0D;
        Vec3d prev = p0;
        for (int i = 1; i <= segments; i++) {
            double t = (double) i / segments;
            Vec3d cur = evaluate(t);
            double segLen = prev.distanceTo(cur);
            totalLen += segLen;
            if (totalLen >= dist) {
                double overshoot = totalLen - dist;
                double prevT = (double) (i - 1) / segments;
                double ratio = 1.0D - overshoot / segLen;
                return prevT + ratio * (t - prevT);
            }
            prev = cur;
        }
        return 1.0D;
    }

    public static ConveyorArc createSideEntry(Vec3d entryPoint, Vec3d targetPoint, EnumFacing entryFacing, EnumFacing targetFacing) {
        double dist = entryPoint.distanceTo(targetPoint);
        double cpDist = dist * 0.5D;

        double cpX = entryPoint.x + entryFacing.getXOffset() * cpDist;
        double cpZ = entryPoint.z + entryFacing.getZOffset() * cpDist;
        double cpY = (entryPoint.y + targetPoint.y) * 0.5D;

        return new ConveyorArc(entryPoint, new Vec3d(cpX, cpY, cpZ), targetPoint);
    }

    public static ConveyorArc createLateralMerge(Vec3d currentPos, Vec3d targetPos, EnumFacing travelFacing) {
        double dx = targetPos.x - currentPos.x;
        double dz = targetPos.z - currentPos.z;
        double dist = Math.sqrt(dx * dx + dz * dz);
        double fwdDist = dist * 0.6D;

        double cpX = currentPos.x + travelFacing.getXOffset() * fwdDist;
        double cpZ = currentPos.z + travelFacing.getZOffset() * fwdDist;
        double cpY = (currentPos.y + targetPos.y) * 0.5D;

        return new ConveyorArc(currentPos, new Vec3d(cpX, cpY, cpZ), targetPos);
    }
}