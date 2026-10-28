package com.hbm.blocks.network;

import java.util.List;

import com.hbm.tileentity.conductor.TileEntityAirDuct;

import net.minecraft.block.BlockContainer;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.Entity;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumBlockRenderType;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

//Looks like a regular pipe, links compressors and arc welders into one vacuum network; see AirDuctNetwork
public class BlockAirDuct extends BlockContainer {

	private static final double MIN = 0.3125D;
	private static final double MAX = 0.6875D;

	public BlockAirDuct(Material mat, String s) {
		super(mat);
		this.setTranslationKey(s);
		this.setRegistryName(s);
		com.hbm.blocks.ModBlocks.ALL_BLOCKS.add(this);
	}

	@Override
	public TileEntity createNewTileEntity(World world, int meta) {
		return new TileEntityAirDuct();
	}

	@Override
	public EnumBlockRenderType getRenderType(IBlockState state) {
		return EnumBlockRenderType.ENTITYBLOCK_ANIMATED;
	}

	@Override
	public boolean isOpaqueCube(IBlockState state) {
		return false;
	}

	@Override
	public boolean isFullCube(IBlockState state) {
		return false;
	}

	/** the core plus an arm towards every connected side */
	private static AxisAlignedBB getBox(IBlockAccess world, BlockPos pos, EnumFacing side) {
		switch(side) {
		case UP: return new AxisAlignedBB(MIN, 0.5, MIN, MAX, 1, MAX);
		case DOWN: return new AxisAlignedBB(MIN, 0, MIN, MAX, 0.5, MAX);
		case NORTH: return new AxisAlignedBB(MIN, MIN, 0, MAX, MAX, 0.5);
		case SOUTH: return new AxisAlignedBB(MIN, MIN, 0.5, MAX, MAX, 1);
		case WEST: return new AxisAlignedBB(0, MIN, MIN, 0.5, MAX, MAX);
		default: return new AxisAlignedBB(0.5, MIN, MIN, 1, MAX, MAX);
		}
	}

	@Override
	public AxisAlignedBB getBoundingBox(IBlockState state, IBlockAccess world, BlockPos pos) {
		AxisAlignedBB box = new AxisAlignedBB(MIN, MIN, MIN, MAX, MAX, MAX);
		for(EnumFacing side : EnumFacing.VALUES)
			if(AirDuctNetwork.connects(world, pos, side))
				box = box.union(getBox(world, pos, side));
		return box;
	}

	@Override
	public void addCollisionBoxToList(IBlockState state, World world, BlockPos pos, AxisAlignedBB entityBox, List<AxisAlignedBB> collidingBoxes, Entity entity, boolean isActualState) {
		addCollisionBoxToList(pos, entityBox, collidingBoxes, new AxisAlignedBB(MIN, MIN, MIN, MAX, MAX, MAX));
		for(EnumFacing side : EnumFacing.VALUES)
			if(AirDuctNetwork.connects(world, pos, side))
				addCollisionBoxToList(pos, entityBox, collidingBoxes, getBox(world, pos, side));
	}

	@Override
	public void addInformation(ItemStack stack, World world, List<String> tooltip, ITooltipFlag flag) {
		tooltip.add(com.hbm.util.I18nUtil.resolveKey("desc.air_duct"));
	}
}
