package com.hbm.interfaces;

import com.hbm.handler.MultiblockPlacement;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public interface IMultiBlock {
	//Another dummy interface

	/**
	 * Optional client-side placement preview. Return null when this block has no preview.
	 */
	default MultiblockPlacement getPlacementPreview(World world, BlockPos pos, EntityPlayer player) {
		return null;
	}
}
