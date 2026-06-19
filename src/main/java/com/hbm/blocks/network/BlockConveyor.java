package com.hbm.blocks.network;

import api.hbm.block.IConveyorBelt;
import api.hbm.block.IConveyorLaneProvider;
import api.hbm.block.IConveyorLaneSelector;
import api.hbm.block.IConveyorVectorProvider;
import api.hbm.block.IToolable;
import com.hbm.blocks.ModBlocks;
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
import java.util.List;

public class BlockConveyor extends Block implements IConveyorBelt, IToolable {

	public static final PropertyDirection FACING = BlockHorizontal.FACING;
	public static final AxisAlignedBB CONVEYOR_BB = new AxisAlignedBB(0.0D, 0.0D, 0.0D, 1.0D, 0.25D, 1.0D);
	protected static final double EPS = 1.0E-5D;

	protected final IConveyorLaneProvider laneProvider;
	protected final IConveyorVectorProvider vectorProvider;
	protected final IConveyorLaneSelector laneSelector;

	public BlockConveyor(Material materialIn, String s) {
		this(materialIn, s, () -> new double[]{0.0D}, IConveyorVectorProvider.linear(), BlockConveyor::selectNearestLane);
	}

	public BlockConveyor(Material materialIn, String s, IConveyorLaneProvider laneProvider) {
		this(materialIn, s, laneProvider, IConveyorVectorProvider.linear(), BlockConveyor::selectNearestLane);
	}

	public BlockConveyor(Material materialIn, String s, IConveyorLaneProvider laneProvider, IConveyorVectorProvider vectorProvider, IConveyorLaneSelector laneSelector) {
		super(materialIn);
		this.setTranslationKey(s);
		this.setRegistryName(s);
		this.laneProvider = laneProvider;
		this.vectorProvider = vectorProvider;
		this.laneSelector = laneSelector;
		ModBlocks.ALL_BLOCKS.add(this);
	}

	@Override
	public boolean hasTileEntity(IBlockState state) {
		return usesLaneQueues();
	}

	@Nullable
	@Override
	public TileEntity createTileEntity(World world, IBlockState state) {
		return usesLaneQueues() ? new TileEntityConveyor() : null;
	}

	public boolean usesLaneQueues() {
		return true;
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

	public int getClosestLaneIndex(World world, BlockPos pos, Vec3d probePoint) {
		return laneSelector.selectLane(world, pos, getLaneFacing(world, pos), probePoint, getLaneOffsets(), vectorProvider);
	}

	public boolean canMergeForwardTo(World world, BlockPos pos, BlockPos nextPos, BlockConveyor nextConveyor) {
		if (nextConveyor == null || !nextConveyor.usesLaneQueues()) return false;
		if (!pos.offset(getLaneFacing(world, pos)).equals(nextPos)) return false;
		if (nextConveyor.getLaneFacing(world, nextPos) != getLaneFacing(world, pos)) return false;

		double[] a = getLaneOffsets();
		double[] b = nextConveyor.getLaneOffsets();

		if (a.length != b.length) return false;

		for (int i = 0; i < a.length; i++) {
			if (Math.abs(a[i] - b[i]) > EPS) return false;
		}

		return true;
	}

	public IncomingRoute resolveIncomingRoute(World world, BlockPos fromPos, BlockConveyor fromConveyor, int fromLane, BlockPos toPos) {
		IncomingRoute forward = resolveForwardIncomingRoute(world, fromPos, fromConveyor, fromLane, toPos);
		if (forward != null) {
			return forward;
		}

		if (fromConveyor != null) {
			EnumFacing fromFacing = fromConveyor.getLaneFacing(world, fromPos);
			EnumFacing toFacing = getLaneFacing(world, toPos);
			int safeFromLane = clampLane(fromLane, fromConveyor.getLaneCount());
			double sourceOffset = fromConveyor.getLaneOffsets()[safeFromLane];

			Vec3d sourceStart = fromConveyor.getLanePoint(fromPos, fromFacing, safeFromLane, 0.0D);
			Vec3d sourceEnd = fromConveyor.getLanePoint(fromPos, fromFacing, safeFromLane, 2.0D);

			int bestLane = -1;
			double bestSourceParam = Double.POSITIVE_INFINITY;
			double bestProgress = 0.0D;

			for (int lane = 0; lane < getLaneCount(); lane++) {
				Vec3d targetStart = getLanePoint(toPos, toFacing, lane, 0.0D);
				Vec3d targetEnd = getLanePoint(toPos, toFacing, lane, 1.0D);
				IntersectionResult result = intersectLinesXZ(sourceStart, sourceEnd, targetStart, targetEnd);

				if (result == null) continue;
				if (result.sourceParam < -EPS) continue;
				if (result.targetParam < -EPS || result.targetParam > 1.0D + EPS) continue;

				double snapped = quantizeProgressUp(result.targetParam);

				if (result.sourceParam < bestSourceParam) {
					bestSourceParam = result.sourceParam;
					bestLane = lane;
					bestProgress = snapped;
				}
			}

			if (bestLane >= 0) {
				Vec3d entryPoint = fromConveyor.getLanePoint(fromPos, fromFacing, safeFromLane, ConveyorItemData.EXIT_PROGRESS);
				Vec3d targetPoint = getLanePoint(toPos, toFacing, bestLane, bestProgress);
				ConveyorArc arc = ConveyorArc.createSideEntry(entryPoint, targetPoint, fromFacing, toFacing);
				return new IncomingRoute(bestLane, bestProgress, false, sourceOffset, arc);
			}
		}

		return new IncomingRoute(-1, 0.0D, false, 0.0D, null);
	}

	protected IncomingRoute resolveForwardIncomingRoute(World world, BlockPos fromPos, BlockConveyor fromConveyor, int fromLane, BlockPos toPos) {
		if (fromConveyor == null) return null;

		EnumFacing fromFacing = fromConveyor.getLaneFacing(world, fromPos);
		if (!fromPos.offset(fromFacing).equals(toPos)) return null;

		EnumFacing toFacing = getLaneFacing(world, toPos);
		if (toFacing != fromFacing) return null;

		int safeFromLane = clampLane(fromLane, fromConveyor.getLaneCount());
		double sourceOffset = fromConveyor.getLaneOffsets()[safeFromLane];

		if (fromConveyor.getLaneCount() > getLaneCount()) {
			int bestLane = -1;
			double bestDiff = Double.MAX_VALUE;

			for (int i = 0; i < getLaneCount(); i++) {
				double diff = Math.abs(getLaneOffsets()[i] - sourceOffset);
				if (diff < bestDiff) {
					bestDiff = diff;
					bestLane = i;
				}
			}

			if (bestLane >= 0) {
				boolean merged = fromConveyor.canMergeForwardTo(world, fromPos, toPos, this);
				return new IncomingRoute(bestLane, ConveyorItemData.ENTRY_PROGRESS, merged, sourceOffset, null);
			}

			return new IncomingRoute(-1, 0.0D, false, sourceOffset, null);
		}

		int bestLane = -1;
		double bestDiff = Double.MAX_VALUE;
		double maxAllowedDiff = ConveyorItemData.HALF_ITEM + EPS;

		for (int i = 0; i < getLaneCount(); i++) {
			double diff = Math.abs(getLaneOffsets()[i] - sourceOffset);
			if (diff <= maxAllowedDiff && diff < bestDiff) {
				bestDiff = diff;
				bestLane = i;
			}
		}

		if (bestLane < 0) {
			return new IncomingRoute(-1, 0.0D, false, sourceOffset, null);
		}

		boolean merged = fromConveyor.canMergeForwardTo(world, fromPos, toPos, this);
		return new IncomingRoute(bestLane, 0.0D, merged, sourceOffset, null);
	}

	protected double quantizeProgressUp(double progress) {
		if (progress <= ConveyorItemData.ENTRY_PROGRESS) {
			return ConveyorItemData.ENTRY_PROGRESS;
		}

		double steps = Math.ceil((progress - ConveyorItemData.ENTRY_PROGRESS - EPS) / ConveyorItemData.ITEM_LENGTH);
		double snapped = ConveyorItemData.ENTRY_PROGRESS + steps * ConveyorItemData.ITEM_LENGTH;
		return MathHelper.clamp(snapped, ConveyorItemData.ENTRY_PROGRESS, ConveyorItemData.EXIT_PROGRESS);
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
				ConveyorItemData.ENTRY_PROGRESS, ConveyorItemData.EXIT_PROGRESS);
		return getLanePoint(pos, facing, lane, progress);
	}

	@Override
	public boolean onBlockActivated(World world, BlockPos pos, IBlockState state, EntityPlayer player, EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ) {
		if (hand != EnumHand.MAIN_HAND) return false;
		if (player.isSneaking()) return false;
		if (world.isRemote) return true;

		TileEntity te = world.getTileEntity(pos);
		if (!(te instanceof TileEntityConveyor)) return false;
		TileEntityConveyor tile = (TileEntityConveyor) te;

		EnumFacing laneFacing = getLaneFacing(world, pos);
		Vec3d hitPos = new Vec3d(pos.getX() + hitX, pos.getY() + hitY, pos.getZ() + hitZ);
		int lane = getClosestLaneIndex(world, pos, hitPos);
		if (lane < 0 || lane >= getLaneCount()) return false;

		double hitProgress = getLaneProgress(pos, laneFacing, hitPos);
		ItemStack heldStack = player.getHeldItem(hand);

		if (!heldStack.isEmpty()) {
			int slotIdx = tile.findNearestFreeSlot(lane, hitProgress);
			if (slotIdx < 0) return true;

			ItemStack placed = heldStack.copy();
			placed.setCount(1);

			if (tile.insertStack(placed, lane, slotIdx)) {
				tile.forceSyncNow();
				if (!player.isCreative()) {
					heldStack.shrink(1);
				}
			}
			return true;
		}

		ConveyorItemData target = tile.findNearestItem(lane, hitProgress);
		if (target != null) {
			ItemStack dropped = target.getStack().copy();
			tile.removeItem(target);
			tile.forceSyncNow();

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
			TileEntity te = world.getTileEntity(pos);
			if (!(te instanceof TileEntityConveyor)) return;

			TileEntityConveyor tile = (TileEntityConveyor) te;
			Vec3d entityPos = new Vec3d(entity.posX, entity.posY, entity.posZ);
			int lane = tile.getClosestAvailableLane(entityPos);

			if (lane < 0) return;

			int slotIdx = tile.findFirstFreeSlot(lane);
			if (slotIdx < 0) return;

			ItemStack stack = ((EntityItem) entity).getItem().copy();
			if (tile.insertStack(stack, lane, slotIdx)) {
				tile.forceSyncNow();
				entity.setDead();
			}
		}
	}

	@Override
	public void breakBlock(World world, BlockPos pos, IBlockState state) {
		TileEntity te = world.getTileEntity(pos);
		if (te instanceof TileEntityConveyor) {
			((TileEntityConveyor) te).invalidate();
		}
		super.breakBlock(world, pos, state);
	}

	@Override
	public void onBlockPlacedBy(World worldIn, BlockPos pos, IBlockState state, EntityLivingBase placer, ItemStack stack) {
		worldIn.setBlockState(pos, state.withProperty(FACING, placer.getHorizontalFacing().getOpposite()));
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

	@Override
	public boolean onScrew(World world, EntityPlayer player, int x, int y, int z, EnumFacing side, float fX, float fY, float fZ, EnumHand hand, IToolable.ToolType tool) {
		if (tool != IToolable.ToolType.SCREWDRIVER) return false;
		BlockPos pos = new BlockPos(x, y, z);
		IBlockState state = world.getBlockState(pos);
		EnumFacing facing = state.getValue(FACING);
		world.setBlockState(pos, state.withProperty(FACING, facing.rotateY()));
		return true;
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

	protected static IntersectionResult intersectLinesXZ(Vec3d a1, Vec3d a2, Vec3d b1, Vec3d b2) {
		double rX = a2.x - a1.x;
		double rZ = a2.z - a1.z;
		double sX = b2.x - b1.x;
		double sZ = b2.z - b1.z;

		double denom = rX * sZ - rZ * sX;
		if (Math.abs(denom) <= EPS) return null;

		double qpx = b1.x - a1.x;
		double qpz = b1.z - a1.z;

		return new IntersectionResult(
				(qpx * sZ - qpz * sX) / denom,
				(qpx * rZ - qpz * rX) / denom
		);
	}

	public static final class IncomingRoute {
		public final int lane;
		public final double progress;
		public final boolean merged;
		public final double sourceLateralOffset;
		public final ConveyorArc arc;

		public IncomingRoute(int lane, double progress, boolean merged, double sourceLateralOffset, ConveyorArc arc) {
			this.lane = lane;
			this.progress = progress;
			this.merged = merged;
			this.sourceLateralOffset = sourceLateralOffset;
			this.arc = arc;
		}
	}

	protected static final class IntersectionResult {
		public final double sourceParam;
		public final double targetParam;

		public IntersectionResult(double sourceParam, double targetParam) {
			this.sourceParam = sourceParam;
			this.targetParam = targetParam;
		}
	}
}