package com.hbm.world;

import java.util.Random;

import com.hbm.blocks.ModBlocks;

import net.minecraft.init.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockPos.MutableBlockPos;
import net.minecraft.world.World;

public class OilSandBubble {

	public static void spawnOil(World world, Random rand, int x, int y, int z, int radius) {
        int r2 = radius * radius;
		int r22 = r2 / 2;

		MutableBlockPos pos = new BlockPos.MutableBlockPos();
		for (int xx = -radius; xx < radius; xx++) {
			int X = xx + x;
			int XX = xx * xx;
			for (int yy = -radius; yy < radius; yy++) {
				int Y = yy + y;
				int YY = XX + yy * yy * 3;
				for (int zz = -radius; zz < radius; zz++) {
					int Z = zz + z;
					int ZZ = YY + zz * zz;
					if (ZZ < r22 + rand.nextInt(Math.max(1, r22 / 3))) {
						pos.setPos(X, Y, Z);
						if(world.getBlockState(pos).getBlock() == Blocks.SAND)
							world.setBlockState(pos, ModBlocks.ore_oil_sand.getDefaultState());
					}
				}
			}
		}
	}
}