package api.hbm.block;

import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

public interface IConveyorBelt {

    boolean canItemStay(World world, int x, int y, int z, Vec3d itemPos);

    Vec3d getTravelLocation(World world, int x, int y, int z, Vec3d itemPos, double speed);

    Vec3d getClosestSnappingPosition(World world, BlockPos pos, Vec3d itemPos);
}