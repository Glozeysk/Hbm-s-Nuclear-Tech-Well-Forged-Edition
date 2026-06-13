package com.hbm.world;

import java.util.Random;

import com.hbm.blocks.ModBlocks;

import net.minecraft.block.Block;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockPos.MutableBlockPos;
import net.minecraft.world.World;

public class Sellafield {

	private double depthFunc(double x, double rad, double depth) {
		return -Math.pow(x, 2) / Math.pow(rad, 2) * depth + depth;
	}

	public void generate(World world, Random rand, int x, int z, double radius, double depth) {

		if(world.isRemote)
			return;

		int iRad = (int)Math.round(radius);
		MutableBlockPos pos = new BlockPos.MutableBlockPos();

		for(int a = -iRad - 5; a <= iRad + 5; a++) {
			int worldX = x + a;

			for(int b = -iRad - 5; b <= iRad + 5; b++) {
				int worldZ = z + b;

				double r = Math.sqrt(a * a + b * b);

				if(r - rand.nextInt(3) <= radius) {

					int dep = (int)depthFunc(r, radius, depth);
					int surfaceY = world.getHeight(worldX, worldZ) - 1;

					if(surfaceY < dep * 2)
						continue;

					for(int i = 0; i < dep; i++) {
						world.setBlockToAir(pos.setPos(worldX, surfaceY - i, worldZ));
					}

					Block block;
					if(r + rand.nextInt(3) <= radius / 6D) {
						block = ModBlocks.sellafield_4;
					} else if(r - rand.nextInt(3) <= radius / 6D * 2D) {
						block = ModBlocks.sellafield_3;
					} else if(r - rand.nextInt(3) <= radius / 6D * 3D) {
						block = ModBlocks.sellafield_2;
					} else if(r - rand.nextInt(3) <= radius / 6D * 4D) {
						block = ModBlocks.sellafield_1;
					} else if(r - rand.nextInt(3) <= radius / 6D * 5D) {
						block = ModBlocks.sellafield_0;
					} else {
						block = ModBlocks.sellafield_slaked;
					}

					for(int i = 0; i < 3; i++) {
						world.setBlockState(pos.setPos(worldX, surfaceY - i, worldZ), block.getDefaultState());
					}
				}
			}
		}

		placeCore(world, x, z);
	}

	private void placeCore(World world, int x, int z) {
		int y = world.getHeight(x, z) - 1;
		world.setBlockState(new BlockPos(x, y, z), ModBlocks.sellafield_core.getDefaultState());
	}
}