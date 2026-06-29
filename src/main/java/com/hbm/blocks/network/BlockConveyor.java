package com.hbm.blocks.network;

import api.hbm.block.IConveyorBelt;
import api.hbm.block.IConveyorLaneProvider;
import api.hbm.block.IConveyorLaneSelector;
import api.hbm.block.IConveyorVectorProvider;
import api.hbm.block.IToolable;
import com.hbm.blocks.ModBlocks;
import com.hbm.blocks.network.conveyor.*;
import com.hbm.tileentity.network.TileEntityConveyor;
import net.minecraft.block.Block;
import net.minecraft.block.BlockHorizontal;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.properties.PropertyDirection;
import net.minecraft.block.state.BlockFaceShape;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumBlockRenderType;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.Mirror;
import net.minecraft.util.Rotation;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

import javax.annotation.Nullable;

public class BlockConveyor extends Block implements IConveyorBelt, IToolable {

	public static final PropertyDirection FACING = BlockHorizontal.FACING;
	public static final AxisAlignedBB CONVEYOR_BB = new AxisAlignedBB(0.0D, 0.0D, 0.0D, 1.0D, 0.25D, 1.0D);
	protected static final double EPS = 1.0E-5D;

	protected final IConveyorLaneProvider laneProvider;
	protected final IConveyorVectorProvider vectorProvider;
	protected final IConveyorLaneSelector laneSelector;
	protected final ConveyorEntryPoints entryPoints;

	public BlockConveyor(Material materialIn, String s) {
		this(materialIn, s, () -> new double[]{0.0D}, IConveyorVectorProvider.linear(), BlockConveyor::selectNearestLane, createSingleEntryPoints());
	}

	public BlockConveyor(Material materialIn, String s, IConveyorLaneProvider laneProvider) {
		this(materialIn, s, laneProvider, IConveyorVectorProvider.linear(), BlockConveyor::selectNearestLane, null);
	}

	public BlockConveyor(Material materialIn, String s, IConveyorLaneProvider laneProvider, IConveyorVectorProvider vectorProvider, IConveyorLaneSelector laneSelector) {
		this(materialIn, s, laneProvider, vectorProvider, laneSelector, null);
	}

	public BlockConveyor(Material materialIn, String s, IConveyorLaneProvider laneProvider, IConveyorVectorProvider vectorProvider, IConveyorLaneSelector laneSelector, ConveyorEntryPoints entryPoints) {
		super(materialIn);
		this.setTranslationKey(s);
		this.setRegistryName(s);
		this.laneProvider = laneProvider;
		this.vectorProvider = vectorProvider;
		this.laneSelector = laneSelector;
		this.entryPoints = entryPoints;
		ModBlocks.ALL_BLOCKS.add(this);
	}

	public ConveyorEntryPoints getEntryPoints() {
		return entryPoints;
	}

	static ConveyorEntryPoints createSingleEntryPoints() {
		double[][][] rightPoints = {
				{{14, 3}, {10, 3}, {8, 6}},
				{{14, 4}, {10, 4}, {8, 6}},
				{{14, 8}, {10, 8}, {8, 10}},
				{{14, 12}, {10, 12}, {8, 14}},
				{{14, 13}, {10, 13}, {8, 14}}
		};
		double[][][] leftPoints = {
				{{2, 3}, {6, 3}, {8, 6}},
				{{2, 4}, {6, 4}, {8, 6}},
				{{2, 8}, {6, 8}, {8, 10}},
				{{2, 12}, {6, 12}, {8, 14}},
				{{2, 13}, {6, 13}, {8, 14}}
		};
		return new ConveyorEntryPoints(leftPoints, rightPoints, 0.625D);
	}

	@Override
	public boolean hasTileEntity(IBlockState state) {
		return true;
	}

	@Nullable
	@Override
	public TileEntity createTileEntity(World world, IBlockState state) {
		return new TileEntityConveyor();
	}

	public double getConveyorSpeed() {
		return 0.0625D;
	}

	public double[] getLaneOffsets() {
		return laneProvider.getLaneOffsets();
	}

	public int getLaneCount() {
		return getLaneOffsets().length;
	}

	public EnumFacing getLaneFacing(World world, BlockPos pos) {
		return world.getBlockState(pos).getValue(FACING).getOpposite();
	}

	public IConveyorVectorProvider getVectorProvider() {
		return vectorProvider;
	}

	public Vec3d getWorldPosition(BlockPos pos, EnumFacing facing, double lateralOffset, double progress) {
		return vectorProvider.getPoint(pos, facing, lateralOffset, progress);
	}

	public Vec3d getLanePoint(BlockPos pos, EnumFacing facing, int laneIndex, double progress) {
		double[] offsets = getLaneOffsets();
		if (laneIndex < 0 || laneIndex >= offsets.length) {
			laneIndex = 0;
		}
		return getWorldPosition(pos, facing, offsets[laneIndex], progress);
	}

	public Vec3d getLanePosition(BlockPos pos, EnumFacing facing, int laneIndex) {
		return getLanePoint(pos, facing, laneIndex, 0.5D);
	}

	public double getLaneProgress(BlockPos pos, EnumFacing facing, Vec3d itemPos) {
		double dx = itemPos.x - (pos.getX() + 0.5D);
		double dz = itemPos.z - (pos.getZ() + 0.5D);
		return 0.5D + dx * facing.getXOffset() + dz * facing.getZOffset();
	}

	public double getLaneLateralOffset(BlockPos pos, EnumFacing facing, Vec3d itemPos) {
		EnumFacing right = facing.rotateY();
		double dx = itemPos.x - (pos.getX() + 0.5D);
		double dz = itemPos.z - (pos.getZ() + 0.5D);
		return dx * right.getXOffset() + dz * right.getZOffset();
	}

	public boolean isVerticalConveyor() {
		return false;
	}

	public boolean usesLaneQueues() {
		return true;
	}

	public int getClosestLaneIndex(World world, BlockPos pos, Vec3d probePoint) {
		return laneSelector.selectLane(world, pos, getLaneFacing(world, pos), probePoint, getLaneOffsets(), vectorProvider);
	}

	@Override
	public boolean canItemStay(World world, int x, int y, int z, Vec3d itemPos) {
		return true;
	}

	@Override
	public Vec3d getTravelLocation(World world, int x, int y, int z, Vec3d itemPos, double speed) {
		BlockPos pos = new BlockPos(x, y, z);
		EnumFacing facing = getLaneFacing(world, pos);
		int lane = getClosestLaneIndex(world, pos, itemPos);
		double moveSpeed = speed;

		double currentProgress = getLaneProgress(pos, facing, itemPos);
		double currentLateral = getLaneLateralOffset(pos, facing, itemPos);
		double targetLateral = getLaneOffsets()[lane];
		double lateralDiff = targetLateral - currentLateral;
		double lateralStep = MathHelper.clamp(lateralDiff, -moveSpeed, moveSpeed);

		return getWorldPosition(pos, facing, currentLateral + lateralStep, currentProgress + moveSpeed);
	}

	@Override
	public Vec3d getClosestSnappingPosition(World world, BlockPos pos, Vec3d itemPos) {
		EnumFacing facing = getLaneFacing(world, pos);
		int lane = getClosestLaneIndex(world, pos, itemPos);
		double progress = MathHelper.clamp(getLaneProgress(pos, facing, itemPos),
				BeltLane.ITEM_LENGTH * 0.5, 1.0 - BeltLane.ITEM_LENGTH * 0.5);
		return getLanePoint(pos, facing, lane, progress);
	}

	@Override
	public boolean onBlockActivated(World world, BlockPos pos, IBlockState state, EntityPlayer player, EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ) {
		if (hand != EnumHand.MAIN_HAND) return false;
		if (player.isSneaking()) return false;
		if (world.isRemote) return true;

		BeltSegment segment = BeltSegmentManager.getOrCreateSegment(world, pos);
		if (segment == null) return false;

		int blockIndex = segment.getBlockIndex(pos);
		if (blockIndex < 0) return false;

		EnumFacing laneFacing = getLaneFacing(world, pos);
		Vec3d hitPos = new Vec3d(pos.getX() + hitX, pos.getY() + hitY, pos.getZ() + hitZ);
		int lane = getClosestLaneIndex(world, pos, hitPos);
		if (lane < 0 || lane >= getLaneCount()) return false;

		double localProgress = getLaneProgress(pos, laneFacing, hitPos);
		double hitProgress = blockIndex + localProgress;
		ItemStack heldStack = player.getHeldItem(hand);

		if (!heldStack.isEmpty()) {
			BeltLane beltLane = segment.getLane(lane);
			double slotProgress = blockIndex + 0.5D;
			if (!beltLane.isSlotFree(slotProgress)) return true;

			ItemStack placed = heldStack.copy();
			placed.setCount(1);

			if (segment.insertStack(placed, lane, slotProgress)) {
				if (!player.isCreative()) {
					heldStack.shrink(1);
				}
			}
			return true;
		}

		BeltLane beltLane = segment.getLane(lane);
		BeltItemData target = beltLane.findNearest(hitProgress);
		if (target != null) {
			ItemStack dropped = target.getStack().copy();
			segment.removeItem(target.getUniqueId());

			if (!player.inventory.addItemStackToInventory(dropped)) {
				EntityItem entityItem = new EntityItem(world, player.posX, player.posY + 0.5D, player.posZ, dropped);
				entityItem.setNoPickupDelay();
				world.spawnEntity(entityItem);
			}
			return true;
		}

		return true;
	}

	@Override
	public void onEntityCollision(World world, BlockPos pos, IBlockState state, Entity entity) {
		if (world.isRemote) return;

		if (entity instanceof EntityItem && entity.ticksExisted > 10 && !entity.isDead) {
			BeltSegment segment = BeltSegmentManager.getOrCreateSegment(world, pos);
			if (segment == null) return;

			int blockIndex = segment.getBlockIndex(pos);
			if (blockIndex < 0) return;

			Vec3d entityPos = new Vec3d(entity.posX, entity.posY, entity.posZ);
			int lane = getClosestLaneIndex(world, pos, entityPos);
			if (lane < 0 || lane >= segment.getLaneCount()) return;

			double slotProgress = blockIndex + BeltLane.ITEM_LENGTH * 0.5;
			BeltLane beltLane = segment.getLane(lane);
			if (!beltLane.isSlotFree(slotProgress)) return;

			ItemStack stack = ((EntityItem) entity).getItem().copy();
			if (segment.insertStack(stack, lane, slotProgress)) {
				entity.setDead();
			}
		}
	}

	@Override
	public void breakBlock(World world, BlockPos pos, IBlockState state) {
		if (!world.isRemote) {
			BeltSegmentManager.onBlockRemoved(world, pos, state);
		}
		super.breakBlock(world, pos, state);
	}

	@Override
	public void onBlockPlacedBy(World worldIn, BlockPos pos, IBlockState state, EntityLivingBase placer, ItemStack stack) {
		worldIn.setBlockState(pos, state.withProperty(FACING, placer.getHorizontalFacing().getOpposite()));
		if (!worldIn.isRemote) {
			BeltSegmentManager.onBlockPlaced(worldIn, pos);
		}
	}

	@Override
	public boolean onScrew(World world, EntityPlayer player, int x, int y, int z, EnumFacing side, float fX, float fY, float fZ, EnumHand hand, IToolable.ToolType tool) {
		if (tool != IToolable.ToolType.SCREWDRIVER) return false;
		BlockPos pos = new BlockPos(x, y, z);
		IBlockState state = world.getBlockState(pos);
		EnumFacing currentFacing = state.getValue(FACING);

		if (!world.isRemote) {
			BeltSegmentManager.onBlockRotated(world, pos);
		}

		world.setBlockState(pos, state.withProperty(FACING, currentFacing.rotateY()));

		if (!world.isRemote) {
			BeltSegmentManager.onBlockPlaced(world, pos);
		}

		return true;
	}

	@Override
	public EnumBlockRenderType getRenderType(IBlockState state) {
		return EnumBlockRenderType.MODEL;
	}

	@Override
	public boolean isFullCube(IBlockState state) {
		return false;
	}

	@Override
	public boolean isBlockNormalCube(IBlockState state) {
		return false;
	}

	@Override
	public boolean isNormalCube(IBlockState state) {
		return false;
	}

	@Override
	public boolean isNormalCube(IBlockState state, IBlockAccess world, BlockPos pos) {
		return false;
	}

	@Override
	public boolean isOpaqueCube(IBlockState state) {
		return false;
	}

	@Override
	public BlockFaceShape getBlockFaceShape(IBlockAccess worldIn, IBlockState state, BlockPos pos, EnumFacing face) {
		return BlockFaceShape.CENTER;
	}

	@Override
	public AxisAlignedBB getBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
		return CONVEYOR_BB;
	}

	@Override
	public BlockStateContainer createBlockState() {
		return new BlockStateContainer(this, new IProperty[]{FACING});
	}

	@Override
	public int getMetaFromState(IBlockState state) {
		return state.getValue(FACING).getIndex();
	}

	@Override
	public IBlockState getStateFromMeta(int meta) {
		EnumFacing enumFacing = EnumFacing.byIndex(meta);
		if (enumFacing.getAxis() == EnumFacing.Axis.Y) enumFacing = EnumFacing.NORTH;
		return this.getDefaultState().withProperty(FACING, enumFacing);
	}

	@Override
	public IBlockState withRotation(IBlockState state, Rotation rot) {
		return state.withProperty(FACING, rot.rotate(state.getValue(FACING)));
	}

	@Override
	public IBlockState withMirror(IBlockState state, Mirror mirrorIn) {
		return state.withRotation(mirrorIn.toRotation(state.getValue(FACING)));
	}

	protected static int clampLane(int lane, int laneCount) {
		if (laneCount <= 0) return 0;
		if (lane < 0) return 0;
		if (lane >= laneCount) return laneCount - 1;
		return lane;
	}

	public static int selectNearestLane(World world, BlockPos pos, EnumFacing facing, Vec3d probePoint, double[] laneOffsets, IConveyorVectorProvider vectorProvider) {
		int bestLane = 0;
		double bestDist = Double.MAX_VALUE;

		for (int i = 0; i < laneOffsets.length; i++) {
			Vec3d a = vectorProvider.getPoint(pos, facing, laneOffsets[i], 0.0D);
			Vec3d b = vectorProvider.getPoint(pos, facing, laneOffsets[i], 1.0D);
			double dist = distanceSqToSegmentXZ(probePoint, a, b);

			if (dist < bestDist) {
				bestDist = dist;
				bestLane = i;
			}
		}

		return bestLane;
	}

	public static double distanceSqToSegmentXZ(Vec3d p, Vec3d a, Vec3d b) {
		double abx = b.x - a.x;
		double abz = b.z - a.z;
		double apx = p.x - a.x;
		double apz = p.z - a.z;

		double abLenSq = abx * abx + abz * abz;
		if (abLenSq <= 1.0E-8D) {
			return (p.x - a.x) * (p.x - a.x) + (p.z - a.z) * (p.z - a.z);
		}

		double t = MathHelper.clamp((apx * abx + apz * abz) / abLenSq, 0.0D, 1.0D);
		double cx = a.x + abx * t;
		double cz = a.z + abz * t;
		return (p.x - cx) * (p.x - cx) + (p.z - cz) * (p.z - cz);
	}

	public static class IncomingRoute {
		public final int lane;
		public final double progress;

		public IncomingRoute(int lane, double progress) {
			this.lane = lane;
			this.progress = progress;
		}

		public static final IncomingRoute NONE = new IncomingRoute(-1, 0.0D);
	}

	public IncomingRoute resolveIncomingRoute(World world, BlockPos fromPos, BlockConveyor fromConveyor, int fromLane, BlockPos toPos) {
		EnumFacing fromFacing = fromConveyor.getLaneFacing(world, fromPos);

		EnumFacing enterDir = EnumFacing.getFacingFromVector(
				(float)(toPos.getX() - fromPos.getX()),
				0,
				(float)(toPos.getZ() - fromPos.getZ())
		);

		if (enterDir != fromFacing) return IncomingRoute.NONE;

		int targetLane = fromLane < getLaneCount() ? fromLane : getLaneCount() - 1;
		double entryProgress = BeltLane.ITEM_LENGTH * 0.5;

		return new IncomingRoute(targetLane, entryProgress);
	}
}