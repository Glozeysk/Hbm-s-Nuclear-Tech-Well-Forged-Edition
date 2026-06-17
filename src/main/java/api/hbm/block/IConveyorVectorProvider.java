package api.hbm.block;

import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

@FunctionalInterface
public interface IConveyorVectorProvider {

    Vec3d getPoint(BlockPos pos, EnumFacing facing, double lateralOffset, double progress);

    static IConveyorVectorProvider linear() {
        return (pos, facing, lateralOffset, progress) -> {
            double cx = pos.getX() + 0.5D;
            double cy = pos.getY() + 0.25D;
            double cz = pos.getZ() + 0.5D;

            EnumFacing right = facing.rotateY();
            double forwardOffset = progress - 0.5D;

            return new Vec3d(
                    cx + facing.getXOffset() * forwardOffset + right.getXOffset() * lateralOffset,
                    cy,
                    cz + facing.getZOffset() * forwardOffset + right.getZOffset() * lateralOffset
            );
        };
    }

    static IConveyorVectorProvider vertical() {
        return (pos, facing, lateralOffset, progress) -> {
            double cx = pos.getX() + 0.5D;
            double cy = pos.getY() + progress;
            double cz = pos.getZ() + 0.5D;

            EnumFacing right = facing.rotateY();

            return new Vec3d(
                    cx + right.getXOffset() * lateralOffset,
                    cy,
                    cz + right.getZOffset() * lateralOffset
            );
        };
    }
}