package com.hbm.handler;

import com.hbm.lib.ForgeDirection;

import net.minecraft.block.Block;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * Result of a client-side multiblock placement preview calculation.
 */
public class MultiblockPlacement {

	public final Block block;
	public final BlockPos corePos;
	public final ForgeDirection direction;
	public final int metadata;
	public final int[] dimensions;
	public final boolean valid;
	private final AxisAlignedBB bounds;

	public MultiblockPlacement(Block block, BlockPos corePos, ForgeDirection direction, int metadata, int[] dimensions, boolean valid) {
		this(block, corePos, direction, metadata, dimensions, valid, null);
	}

	public MultiblockPlacement(Block block, BlockPos corePos, ForgeDirection direction, int metadata, int[] dimensions,
			boolean valid, AxisAlignedBB bounds) {
		this.block = block;
		this.corePos = corePos;
		this.direction = direction;
		this.metadata = metadata;
		this.dimensions = dimensions;
		this.valid = valid;
		this.bounds = bounds;
	}

	public static MultiblockPlacement create(Block block, World world, BlockPos pos, ForgeDirection direction,
			int metadata, int[] dimensions) {
		boolean valid = MultiblockHandler.checkSpace(world, pos, dimensions);
		return new MultiblockPlacement(block, pos, direction, metadata, dimensions, valid);
	}

	public static MultiblockPlacement create(Block block, World world, BlockPos pos, EnumFacing facing,
			int metadata, int[] dimensions) {
		return create(block, world, pos, ForgeDirection.getOrientation(facing.getIndex()), metadata, dimensions);
	}

	public AxisAlignedBB getBounds() {
		if(bounds != null) {
			return bounds;
		}

		if(dimensions == null || dimensions.length != 6) {
			return new AxisAlignedBB(corePos).grow(0.5D);
		}

		return new AxisAlignedBB(
				corePos.getX() - dimensions[4],
				corePos.getY() - dimensions[1],
				corePos.getZ() - dimensions[2],
				corePos.getX() + dimensions[5] + 1,
				corePos.getY() + dimensions[0] + 1,
				corePos.getZ() + dimensions[3] + 1);
	}
}
