package api.hbm.block;

import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

@FunctionalInterface
public interface IConveyorLaneSelector {
    int selectLane(World world, BlockPos pos, EnumFacing facing, Vec3d probePoint, double[] laneOffsets, IConveyorVectorProvider vectorProvider);
}