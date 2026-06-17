package com.hbm.blocks.network;

import api.hbm.block.IConveyorBelt;
import api.hbm.block.IConveyorLaneProvider;
import api.hbm.block.IConveyorLaneSelector;
import api.hbm.block.IConveyorVectorProvider;
import api.hbm.block.IEnterableBlock;
import api.hbm.block.IToolable;
import com.hbm.blocks.ModBlocks;
import com.hbm.entity.item.EntityMovingItem;
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

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public class BlockConveyor extends Block implements IConveyorBelt, IToolable {

	public static final PropertyDirection FACING = BlockHorizontal.FACING;
	public static final AxisAlignedBB CONVEYOR_BB = new AxisAlignedBB(0.0D, 0.0D, 0.0D, 1.0D, 0.25D, 1.0D);
	protected static final double EPS = 1.0E-5D;
	protected static final int MAX_PATH_SCAN = 256;
	protected static final int ARC_RESOLUTION = 16;

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

	public boolean usesLaneQueues() {
		return true;
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

	public int getClosestAvailableLaneIndex(World world, BlockPos pos, Vec3d probePoint) {
		EnumFacing facing = getLaneFacing(world, pos);
		int bestLane = -1;
		double bestDist = Double.MAX_VALUE;

		for (int i = 0; i < getLaneCount(); i++) {
			if (!ConveyorQueue.canInsertAtEntry(world, pos, i, this)) continue;

			Vec3d a = getLanePoint(pos, facing, i, 0.0D);
			Vec3d b = getLanePoint(pos, facing, i, 1.0D);
			double dist = distanceSqToSegmentXZ(probePoint, a, b);

			if (dist < bestDist) {
				bestDist = dist;
				bestLane = i;
			}
		}

		return bestLane;
	}

	public int getMappedLaneToNext(World world, BlockPos pos, int laneIndex) {
		EnumFacing facing = getLaneFacing(world, pos);
		BlockPos nextPos = pos.offset(facing);
		Block nextBlock = world.getBlockState(nextPos).getBlock();

		if (!(nextBlock instanceof BlockConveyor)) {
			return -1;
		}

		BlockConveyor nextConveyor = (BlockConveyor) nextBlock;
		IncomingRoute route = nextConveyor.resolveIncomingRoute(world, pos, this, laneIndex, nextPos);
		return route.lane;
	}

	protected double getTravelSpeed(World world, BlockPos pos, double baseSpeed) {
		return baseSpeed;
	}

	protected double getEffectiveSpeed(World world, BlockPos pos, EntityMovingItem item, double baseSpeed) {
		return getTravelSpeed(world, pos, baseSpeed);
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

	public boolean canAcceptIncomingAtProgress(World world, BlockPos pos, int laneIndex, double progress) {
		return canAcceptIncomingAtProgress(world, pos, laneIndex, progress, null);
	}

	public boolean canAcceptIncomingAtProgress(World world, BlockPos pos, int laneIndex, double progress, EntityMovingItem requester) {
		if (laneIndex < 0 || laneIndex >= getLaneCount()) return false;

		double snapped = quantizeProgressUp(progress);
		List<EntityMovingItem> items = ConveyorQueue.getOrderedItems(world, pos, laneIndex, this);

		if (items.size() >= ConveyorQueue.MAX_ITEMS_PER_LANE) return false;
		if (ConveyorQueue.isPointBlocked(world, pos, laneIndex, snapped, requester)) return false;

		EnumFacing facing = getLaneFacing(world, pos);

		for (EntityMovingItem item : items) {
			if (item == requester) continue;
			double p = quantizeProgressUp(getLaneProgress(pos, facing, new Vec3d(item.posX, item.posY, item.posZ)));
			if (Math.abs(p - snapped) < EPS) {
				return false;
			}
		}

		return true;
	}

	public boolean tryReserveIncomingAtProgress(World world, BlockPos pos, int laneIndex, double progress, EntityMovingItem requester) {
		if (requester == null) {
			return canAcceptIncomingAtProgress(world, pos, laneIndex, progress, null);
		}

		double snapped = quantizeProgressUp(progress);

		if (requester.hasPointReservation()) {
			if (requester.isPointReservation(world, pos, laneIndex, snapped)) {
				return true;
			}
			ConveyorQueue.releasePointReservation(requester);
		}

		if (!canAcceptIncomingAtProgress(world, pos, laneIndex, snapped, requester)) {
			return false;
		}

		return ConveyorQueue.reservePoint(world, pos, laneIndex, snapped, requester);
	}

	protected IncomingRoute resolveForwardIncomingRoute(World world, BlockPos fromPos, BlockConveyor fromConveyor, int fromLane, BlockPos toPos) {
		if (fromConveyor == null) {
			return null;
		}

		EnumFacing fromFacing = fromConveyor.getLaneFacing(world, fromPos);
		if (!fromPos.offset(fromFacing).equals(toPos)) {
			return null;
		}

		EnumFacing toFacing = getLaneFacing(world, toPos);

		if (toFacing != fromFacing) {
			return null;
		}

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
				return new IncomingRoute(bestLane, ConveyorQueue.ENTRY_PROGRESS, merged, sourceOffset, null);
			}

			return new IncomingRoute(-1, 0.0D, false, sourceOffset, null);
		}

		int bestLane = -1;
		double bestDiff = Double.MAX_VALUE;
		double maxAllowedDiff = ConveyorQueue.HALF_ITEM + EPS;

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
				Vec3d entryPoint = fromConveyor.getLanePoint(fromPos, fromFacing, safeFromLane, ConveyorQueue.EXIT_PROGRESS);
				Vec3d targetPoint = getLanePoint(toPos, toFacing, bestLane, bestProgress);
				ConveyorArc arc = ConveyorArc.createSideEntry(entryPoint, targetPoint, fromFacing, toFacing);
				return new IncomingRoute(bestLane, bestProgress, false, sourceOffset, arc);
			}
		}

		return new IncomingRoute(-1, 0.0D, false, 0.0D, null);
	}

	protected double getExitLimit(World world, BlockPos pos, EnumFacing facing, int laneIndex, EntityMovingItem item) {
		BlockPos nextPos = pos.offset(facing);
		IBlockState nextState = world.getBlockState(nextPos);
		Block nextBlock = nextState.getBlock();

		if (nextBlock instanceof BlockConveyor) {
			BlockConveyor nextConveyor = (BlockConveyor) nextBlock;
			IncomingRoute route = nextConveyor.resolveIncomingRoute(world, pos, this, laneIndex, nextPos);

			if (route.lane < 0) {
				return ConveyorQueue.EXIT_PROGRESS;
			}

			if (route.arc != null) {
				return ConveyorQueue.EXIT_PROGRESS;
			}

			if (route.merged) {
				return Double.POSITIVE_INFINITY;
			}

			double targetProgress = route.progress <= EPS ? ConveyorQueue.ENTRY_PROGRESS : route.progress;

			if (nextConveyor.tryReserveIncomingAtProgress(world, nextPos, route.lane, targetProgress, item)) {
				return Double.POSITIVE_INFINITY;
			}

			return ConveyorQueue.EXIT_PROGRESS;
		}

		if (nextBlock instanceof IEnterableBlock) {
			return Double.POSITIVE_INFINITY;
		}

		if (nextState.isFullBlock()) {
			return ConveyorQueue.EXIT_PROGRESS;
		}

		return Double.POSITIVE_INFINITY;
	}

	protected AheadCandidate findAheadCandidate(World world, BlockPos pos, int laneIndex, EntityMovingItem current) {
		EnumFacing initialFacing = getLaneFacing(world, pos);
		double currentLocal = getLaneProgress(pos, initialFacing, new Vec3d(current.posX, current.posY, current.posZ));

		BlockPos scanPos = pos;
		BlockConveyor scanConveyor = this;
		int scanLane = laneIndex;
		EnumFacing scanFacing = initialFacing;
		double prefix = 0.0D;
		double minLocal = currentLocal + EPS;

		AheadCandidate best = null;
		double bestGlobal = Double.POSITIVE_INFINITY;

		Set<PathState> visited = new HashSet<>();

		for (int step = 0; step < MAX_PATH_SCAN; step++) {
			PathState state = new PathState(scanPos, scanLane);
			boolean revisited = !visited.add(state);

			List<EntityMovingItem> items = ConveyorQueue.getOrderedItems(world, scanPos, scanLane, scanConveyor);
			for (EntityMovingItem item : items) {
				if (item == current) continue;

				double local = scanConveyor.getLaneProgress(scanPos, scanFacing, new Vec3d(item.posX, item.posY, item.posZ));
				if (local + EPS < minLocal) continue;

				double global = prefix + local;
				if (global > currentLocal + EPS && global < bestGlobal) {
					bestGlobal = global;
					best = new AheadCandidate(item, scanPos, scanLane, local, global);
				}
			}

			Double blockedAhead = ConveyorQueue.getNearestBlockedProgressAhead(world, scanPos, scanLane, minLocal, current);
			if (blockedAhead != null) {
				double global = prefix + blockedAhead;
				if (global > currentLocal + EPS && global < bestGlobal) {
					bestGlobal = global;
					best = new AheadCandidate(null, scanPos, scanLane, blockedAhead, global);
				}
			}

			if (revisited) break;

			BlockPos nextPos = scanPos.offset(scanFacing);
			Block nextBlock = world.getBlockState(nextPos).getBlock();

			if (!(nextBlock instanceof BlockConveyor)) break;

			BlockConveyor nextConveyor = (BlockConveyor) nextBlock;
			IncomingRoute route = nextConveyor.resolveIncomingRoute(world, scanPos, scanConveyor, scanLane, nextPos);

			if (route.lane < 0 || route.lane >= nextConveyor.getLaneCount()) break;

			prefix += 1.0D - route.progress;
			scanPos = nextPos;
			scanConveyor = nextConveyor;
			scanLane = route.lane;
			scanFacing = nextConveyor.getLaneFacing(world, nextPos);
			minLocal = route.progress - EPS;
		}

		return best;
	}

	protected double quantizeProgressUp(double progress) {
		if (progress <= ConveyorQueue.ENTRY_PROGRESS) {
			return ConveyorQueue.ENTRY_PROGRESS;
		}

		double steps = Math.ceil((progress - ConveyorQueue.ENTRY_PROGRESS - EPS) / ConveyorQueue.ITEM_LENGTH);
		double snapped = ConveyorQueue.ENTRY_PROGRESS + steps * ConveyorQueue.ITEM_LENGTH;
		return MathHelper.clamp(snapped, ConveyorQueue.ENTRY_PROGRESS, ConveyorQueue.EXIT_PROGRESS);
	}

	protected double snapToNearestSlot(double progress) {
		double steps = Math.round((progress - ConveyorQueue.ENTRY_PROGRESS) / ConveyorQueue.ITEM_LENGTH);
		double snapped = ConveyorQueue.ENTRY_PROGRESS + steps * ConveyorQueue.ITEM_LENGTH;
		return MathHelper.clamp(snapped, ConveyorQueue.ENTRY_PROGRESS, ConveyorQueue.EXIT_PROGRESS);
	}

	public double getMovementLimit(World world, BlockPos pos, EnumFacing facing, int laneIndex, EntityMovingItem item) {
		double currentProgress = getLaneProgress(pos, facing, new Vec3d(item.posX, item.posY, item.posZ));

		double rawLimit;

		AheadCandidate ahead = findAheadCandidate(world, pos, laneIndex, item);

		if (ahead != null) {
			rawLimit = ahead.globalProgress - ConveyorQueue.ITEM_LENGTH;
		} else {
			rawLimit = getExitLimit(world, pos, facing, laneIndex, item);
		}

		if (rawLimit >= Double.POSITIVE_INFINITY - 1.0D) {
			return Double.POSITIVE_INFINITY;
		}

		return Math.max(currentProgress, rawLimit);
	}

	public Vec3d getTravelLocationForItem(World world, BlockPos pos, EntityMovingItem item, double speed) {
		EnumFacing facing = getLaneFacing(world, pos);

		int lane = item.getConveyorLane();
		if (lane < 0 || lane >= getLaneCount()) {
			lane = getClosestLaneIndex(world, pos, new Vec3d(item.posX, item.posY, item.posZ));
			item.setConveyorLane(lane);
		}

		double moveSpeed = getEffectiveSpeed(world, pos, item, speed);
		Vec3d itemPos = new Vec3d(item.posX, item.posY, item.posZ);
		double currentProgress = getLaneProgress(pos, facing, itemPos);

		if (item.hasPointReservation() && item.getReservationPos().equals(pos) && item.getReservationLane() == lane) {
			if (currentProgress >= item.getReservationProgress() - EPS) {
				ConveyorQueue.releasePointReservation(item);
			}
		}

		double currentLateral = getLaneLateralOffset(pos, facing, itemPos);

		BlockPos nextPos = pos.offset(facing);
		Block nextBlock = world.getBlockState(nextPos).getBlock();

		if (nextBlock instanceof BlockConveyor) {
			BlockConveyor nextConveyor = (BlockConveyor) nextBlock;
			IncomingRoute route = nextConveyor.resolveIncomingRoute(world, pos, this, lane, nextPos);

			if (route.lane >= 0 && route.arc != null && currentProgress >= ConveyorQueue.EXIT_PROGRESS - EPS) {
				double targetProgress = route.progress <= EPS ? ConveyorQueue.ENTRY_PROGRESS : route.progress;

				if (nextConveyor.tryReserveIncomingAtProgress(world, nextPos, route.lane, targetProgress, item)) {
					ConveyorQueue.reserveHoldPoint(world, pos, lane, ConveyorQueue.EXIT_PROGRESS, item);
					ConveyorQueue.unregisterQueueOnly(item);
					item.setActiveArc(route.arc);
					item.setArcDestination(nextPos, route.lane);
					item.setPosition(route.arc.p0.x, route.arc.p0.y, route.arc.p0.z);
					item.advanceActiveArc(moveSpeed);
					return new Vec3d(item.posX, item.posY, item.posZ);
				}

				return getLanePoint(pos, facing, lane, ConveyorQueue.EXIT_PROGRESS);
			}
		}

		double maxProgress = getMovementLimit(world, pos, facing, lane, item);
		double targetProgress = Math.min(currentProgress + moveSpeed, maxProgress);

		double targetLateral = getLaneOffsets()[lane];
		double lateralDiff = targetLateral - currentLateral;

		if (Math.abs(lateralDiff) > EPS) {
			Vec3d targetPos = getLanePoint(pos, facing, lane, targetProgress);
			ConveyorArc mergeArc = ConveyorArc.createLateralMerge(itemPos, targetPos, facing);
			item.setActiveArc(mergeArc);
			item.setArcParam(0.0D);
			item.setArcDestination(pos, lane);
			item.advanceActiveArc(moveSpeed);
			return new Vec3d(item.posX, item.posY, item.posZ);
		}

		return getWorldPosition(pos, facing, currentLateral, targetProgress);
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
		double moveSpeed = getTravelSpeed(world, pos, speed);

		double currentProgress = getLaneProgress(pos, facing, itemPos);
		double currentLateral = getLaneLateralOffset(pos, facing, itemPos);
		double targetLateral = getLaneOffsets()[lane];
		double lateralDiff = targetLateral - currentLateral;
		double lateralStep = MathHelper.clamp(lateralDiff, -moveSpeed, moveSpeed);

		return getWorldPosition(pos, facing, currentLateral + lateralStep, currentProgress + moveSpeed);
	}

	public EnumFacing getTravelDirection(World world, BlockPos pos, Vec3d itemPos) {
		return getLaneFacing(world, pos);
	}

	@Override
	public Vec3d getClosestSnappingPosition(World world, BlockPos pos, Vec3d itemPos) {
		EnumFacing facing = getLaneFacing(world, pos);
		int lane = getClosestLaneIndex(world, pos, itemPos);
		double progress = MathHelper.clamp(getLaneProgress(pos, facing, itemPos), ConveyorQueue.ENTRY_PROGRESS, ConveyorQueue.EXIT_PROGRESS);
		return getLanePoint(pos, facing, lane, progress);
	}

	@Override
	public boolean onBlockActivated(World world, BlockPos pos, IBlockState state, EntityPlayer player, EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ) {
		if (world.isRemote) return true;

		ItemStack heldStack = player.getHeldItem(hand);
		if (heldStack.isEmpty()) return false;

		EnumFacing laneFacing = getLaneFacing(world, pos);
		Vec3d hitPos = new Vec3d(pos.getX() + hitX, pos.getY() + hitY, pos.getZ() + hitZ);
		int lane = getClosestLaneIndex(world, pos, hitPos);

		if (lane < 0 || lane >= getLaneCount()) return false;

		List<EntityMovingItem> items = ConveyorQueue.getOrderedItems(world, pos, lane, this);
		if (items.size() >= ConveyorQueue.MAX_ITEMS_PER_LANE) return false;

		double placeProgress = ConveyorQueue.EXIT_PROGRESS;

		if (!items.isEmpty()) {
			for (int s = ConveyorQueue.MAX_ITEMS_PER_LANE - 1; s >= 0; s--) {
				double slotP = ConveyorQueue.getSlotProgress(s);
				boolean occupied = false;

				for (EntityMovingItem existing : items) {
					double ep = getLaneProgress(pos, laneFacing, new Vec3d(existing.posX, existing.posY, existing.posZ));
					if (Math.abs(ep - slotP) < ConveyorQueue.ITEM_LENGTH - EPS) {
						occupied = true;
						break;
					}
				}

				if (occupied) continue;

				if (ConveyorQueue.isPointBlocked(world, pos, lane, slotP, null)) continue;

				placeProgress = slotP;
				break;
			}
		}

		if (!canAcceptIncomingAtProgress(world, pos, lane, placeProgress)) {
			return false;
		}

		EntityMovingItem movingItem = new EntityMovingItem(world);
		ItemStack placed = heldStack.copy();
		placed.setCount(1);
		movingItem.setItemStack(placed);
		movingItem.setConveyorLane(lane);

		Vec3d start = getLanePoint(pos, laneFacing, lane, placeProgress);
		movingItem.setPositionAndRotation(start.x, start.y, start.z, 0.0F, 0.0F);

		world.spawnEntity(movingItem);
		ConveyorQueue.sync(world, pos, lane, movingItem);

		if (!player.isCreative()) {
			heldStack.shrink(1);
		}

		return true;
	}

	@Override
	public void onEntityCollision(World world, BlockPos pos, IBlockState state, Entity entity) {
		if (world.isRemote) return;

		if (entity instanceof EntityItem && entity.ticksExisted > 10 && !entity.isDead) {
			Vec3d entityPos = new Vec3d(entity.posX, entity.posY, entity.posZ);
			int lane = getClosestAvailableLaneIndex(world, pos, entityPos);

			if (lane < 0) return;

			EntityMovingItem movingItem = new EntityMovingItem(world);
			movingItem.setItemStack(((EntityItem) entity).getItem());
			movingItem.setConveyorLane(lane);

			EnumFacing f = getLaneFacing(world, pos);
			Vec3d start = getLanePoint(pos, f, lane, ConveyorQueue.ENTRY_PROGRESS);
			movingItem.setPositionAndRotation(start.x, start.y, start.z, 0.0F, 0.0F);

			world.spawnEntity(movingItem);
			ConveyorQueue.sync(world, pos, lane, movingItem);
			entity.setDead();
		}
	}

	@Override
	public void breakBlock(World world, BlockPos pos, IBlockState state) {
		if (!world.isRemote) {
			ConveyorQueue.clearBlock(world, pos, getLaneCount());
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

	protected static final class AheadCandidate {
		public final EntityMovingItem item;
		public final BlockPos pos;
		public final int lane;
		public final double localProgress;
		public final double globalProgress;

		public AheadCandidate(EntityMovingItem item, BlockPos pos, int lane, double localProgress, double globalProgress) {
			this.item = item;
			this.pos = pos;
			this.lane = lane;
			this.localProgress = localProgress;
			this.globalProgress = globalProgress;
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

	protected static final class PathState {
		public final BlockPos pos;
		public final int lane;

		public PathState(BlockPos pos, int lane) {
			this.pos = pos.toImmutable();
			this.lane = lane;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj) return true;
			if (!(obj instanceof PathState)) return false;
			PathState o = (PathState) obj;
			return lane == o.lane && Objects.equals(pos, o.pos);
		}

		@Override
		public int hashCode() {
			return Objects.hash(pos, lane);
		}
	}
}