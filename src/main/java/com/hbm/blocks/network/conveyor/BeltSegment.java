package com.hbm.blocks.network.conveyor;

import com.hbm.blocks.network.BlockConveyor;
import net.minecraft.block.Block;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraftforge.common.util.Constants;

import java.util.ArrayList;
import java.util.List;

public class BeltSegment {

    private static final double EPS = 1.0E-5D;
    private static final int MAX_ITEMS_PER_BLOCK = 4;

    private final List<BlockPos> blocks = new ArrayList<>();
    private EnumFacing direction;
    private double speed;
    private int laneCount;
    private BeltLane[] lanes;
    private boolean dirty = false;
    private long segmentId;

    private boolean[] exitBlocked;

    private static long nextSegmentId = Long.MIN_VALUE;

    public BeltSegment(List<BlockPos> blocks, EnumFacing direction, double speed, int laneCount) {
        this.blocks.addAll(blocks);
        this.direction = direction;
        this.speed = speed;
        this.laneCount = laneCount;
        this.lanes = new BeltLane[laneCount];
        for (int i = 0; i < laneCount; i++) {
            lanes[i] = new BeltLane(blocks.size(), MAX_ITEMS_PER_BLOCK);
        }
        this.exitBlocked = new boolean[laneCount];
        this.segmentId = nextSegmentId++;
    }

    public long getSegmentId() { return segmentId; }
    public List<BlockPos> getBlocks() { return blocks; }
    public EnumFacing getDirection() { return direction; }
    public double getSpeed() { return speed; }
    public int getLaneCount() { return laneCount; }
    public int getLength() { return blocks.size(); }
    public double getMaxProgress() { return blocks.size(); }
    public boolean isDirty() { return dirty; }
    public void clearDirty() { dirty = false; }
    public void markDirty() { dirty = true; }

    public BlockPos getHeadPos() {
        return blocks.get(0);
    }

    public BlockPos getTailPos() {
        return blocks.get(blocks.size() - 1);
    }

    public boolean containsPos(BlockPos pos) {
        return blocks.contains(pos);
    }

    public int getBlockIndex(BlockPos pos) {
        return blocks.indexOf(pos);
    }

    public BeltLane getLane(int lane) {
        if (lane < 0 || lane >= laneCount) return lanes[0];
        return lanes[lane];
    }

    public void tick(World world) {
        double maxProg = getMaxProgress();

        checkExitState(world);

        for (int l = 0; l < laneCount; l++) {
            BeltLane lane = lanes[l];
            List<BeltItemData> items = lane.getItems();

            for (int i = 0; i < items.size(); i++) {
                BeltItemData item = items.get(i);
                double currentProgress = item.getProgress();

                double limit;
                if (i == 0) {
                    if (exitBlocked[l]) {
                        limit = maxProg - BeltLane.ITEM_LENGTH * 0.5;
                    } else {
                        limit = maxProg + BeltLane.ITEM_LENGTH;
                    }
                } else {
                    BeltItemData ahead = items.get(i - 1);
                    limit = ahead.getProgress() - BeltLane.ITEM_LENGTH;
                }

                double newProgress = currentProgress + speed;

                if (newProgress > limit + EPS) {
                    newProgress = Math.max(limit, currentProgress);
                }

                boolean nowStopped = Math.abs(newProgress - currentProgress) < EPS;
                item.setProgress(newProgress);
                item.setStopped(nowStopped);
                if (item.getRouteType() != BeltItemData.ROUTE_FORWARD) {
                    ConveyorRoute route = ConveyorRoute.getByType(item.getRouteType());
                    if (route != null) {
                        double localProgress = newProgress - Math.floor(newProgress);
                        if (localProgress >= route.getMergeProgress() - EPS) {
                            item.setRouteType(BeltItemData.ROUTE_FORWARD);
                            dirty = true;
                        }
                    }
                }
                if (!nowStopped) {
                    dirty = true;
                }
            }

            if (!items.isEmpty()) {
                BeltItemData leader = items.get(0);
                if (leader.getProgress() >= maxProg - BeltLane.ITEM_LENGTH * 0.5 - EPS) {
                    if (!exitBlocked[l]) {
                        if (tryTransfer(world, leader, l)) {
                            items.remove(0);
                            dirty = true;
                        }
                    }
                }
            }
        }
    }

    private void checkExitState(World world) {
        BlockPos lastBlock = blocks.get(blocks.size() - 1);
        BlockPos nextPos = lastBlock.offset(direction);
        Block nextBlock = world.getBlockState(nextPos).getBlock();

        if (nextBlock instanceof BlockConveyor) {
            BeltSegment nextSegment = BeltSegmentManager.getSegmentAt(world, nextPos);
            if (nextSegment != null) {
                for (int l = 0; l < laneCount; l++) {
                    int targetLane = l < nextSegment.laneCount ? l : 0;
                    int targetBlockIndex = nextSegment.getBlockIndex(nextPos);
                    if (targetBlockIndex < 0) {
                        exitBlocked[l] = true;
                        continue;
                    }
                    double entryProgress = targetBlockIndex + BeltLane.ITEM_LENGTH * 0.5;
                    BeltLane nextLane = nextSegment.getLane(targetLane);
                    exitBlocked[l] = !nextLane.isSlotFree(entryProgress);
                }
                return;
            }
        }

        boolean isSolidBlock = !nextBlock.isAir(world.getBlockState(nextPos), world, nextPos)
                && !(nextBlock instanceof BlockConveyor);

        for (int l = 0; l < laneCount; l++) {
            exitBlocked[l] = isSolidBlock;
        }
    }

    private boolean tryTransfer(World world, BeltItemData item, int lane) {
        BlockPos lastBlock = blocks.get(blocks.size() - 1);
        BlockPos nextPos = lastBlock.offset(direction);
        Block nextBlock = world.getBlockState(nextPos).getBlock();

        if (nextBlock instanceof BlockConveyor) {
            BlockConveyor nextConveyor = (BlockConveyor) nextBlock;
            BeltSegment nextSegment = BeltSegmentManager.getSegmentAt(world, nextPos);

            if (nextSegment != null) {
                int targetBlockIndex = nextSegment.getBlockIndex(nextPos);
                if (targetBlockIndex < 0) return false;

                EnumFacing nextFacing = nextConveyor.getLaneFacing(world, nextPos);
                boolean isSideEntry = direction != nextFacing;

                int targetLane;
                int routeType = BeltItemData.ROUTE_FORWARD;

                if (!isSideEntry) {
                    targetLane = lane < nextSegment.getLaneCount() ? lane : 0;
                } else {
                    EnumFacing left = nextFacing.rotateYCCW();
                    EnumFacing right = nextFacing.rotateY();

                    if (direction == right) {
                        routeType = BeltItemData.ROUTE_LEFT_ENTRY;
                    } else if (direction == left) {
                        routeType = BeltItemData.ROUTE_RIGHT_ENTRY;
                    }

                    targetLane = findClosestLane(nextConveyor, world, nextPos, lastBlock);
                }

                double entryProgress = targetBlockIndex + BeltLane.ITEM_LENGTH * 0.5;
                BeltLane nextLaneBelt = nextSegment.getLane(targetLane);

                if (nextLaneBelt.isSlotFree(entryProgress)) {
                    BeltItemData transferred = new BeltItemData(item.getStack(), targetLane, entryProgress);
                    transferred.setUniqueId(item.getUniqueId());
                    transferred.setRouteType(routeType);
                    nextLaneBelt.addSorted(transferred);
                    nextSegment.dirty = true;
                    return true;
                }

                return false;
            }
        }

        ejectItem(world, item, lane);
        return true;
    }

    private int findClosestLane(BlockConveyor conveyor, World world, BlockPos conveyorPos, BlockPos fromPos) {
        EnumFacing facing = conveyor.getLaneFacing(world, conveyorPos);
        double[] offsets = conveyor.getLaneOffsets();
        if (offsets.length <= 1) return 0;

        Vec3d fromCenter = new Vec3d(fromPos.getX() + 0.5D, fromPos.getY() + 0.5D, fromPos.getZ() + 0.5D);

        int bestLane = 0;
        double bestDist = Double.MAX_VALUE;

        for (int i = 0; i < offsets.length; i++) {
            Vec3d lanePos = conveyor.getLanePoint(conveyorPos, facing, i, 0.5D);
            double dist = fromCenter.squareDistanceTo(lanePos);
            if (dist < bestDist) {
                bestDist = dist;
                bestLane = i;
            }
        }

        return bestLane;
    }

    private boolean isTurningConveyor(World world, BlockPos pos, EnumFacing facing, BeltSegment segment) {
        if (segment.getBlockIndex(pos) != 0) return false;

        BlockPos behindPos = pos.offset(facing.getOpposite());
        Block behindBlock = world.getBlockState(behindPos).getBlock();
        if (!(behindBlock instanceof BlockConveyor)) return true;

        BlockConveyor behindConveyor = (BlockConveyor) behindBlock;
        EnumFacing behindFacing = behindConveyor.getLaneFacing(world, behindPos);

        return behindFacing != facing;
    }

    private void ejectItem(World world, BeltItemData item, int lane) {
        BlockPos lastBlock = blocks.get(blocks.size() - 1);
        Block block = world.getBlockState(lastBlock).getBlock();
        if (!(block instanceof BlockConveyor)) return;

        BlockConveyor conveyor = (BlockConveyor) block;
        Vec3d ejectPos = conveyor.getLanePoint(lastBlock, direction, lane, 1.0D);

        EntityItem entityItem = new EntityItem(world,
                ejectPos.x + direction.getXOffset() * 0.25D,
                ejectPos.y,
                ejectPos.z + direction.getZOffset() * 0.25D,
                item.getStack().copy());
        double ejectSpeed = speed * 2.0D;
        entityItem.motionX = direction.getXOffset() * ejectSpeed;
        entityItem.motionY = 0.1D;
        entityItem.motionZ = direction.getZOffset() * ejectSpeed;
        entityItem.velocityChanged = true;
        world.spawnEntity(entityItem);
    }

    public void dropItemsAtBlock(World world, BlockPos pos) {
        int blockIndex = getBlockIndex(pos);
        if (blockIndex < 0) return;

        double minProgress = blockIndex;
        double maxProgress = blockIndex + 1.0D;

        for (int l = 0; l < laneCount; l++) {
            BeltLane lane = lanes[l];
            List<BeltItemData> items = lane.getItems();
            List<BeltItemData> toDrop = new ArrayList<>();

            for (BeltItemData item : items) {
                if (item.getProgress() >= minProgress - EPS && item.getProgress() < maxProgress + EPS) {
                    toDrop.add(item);
                }
            }

            for (BeltItemData item : toDrop) {
                double localProgress = item.getProgress() - blockIndex;
                Block block = world.getBlockState(pos).getBlock();
                Vec3d dropPos;
                if (block instanceof BlockConveyor) {
                    dropPos = ((BlockConveyor) block).getLanePoint(pos, direction, l, localProgress);
                } else {
                    dropPos = new Vec3d(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
                }

                EntityItem entityItem = new EntityItem(world, dropPos.x, dropPos.y, dropPos.z, item.getStack().copy());
                world.spawnEntity(entityItem);
                items.remove(item);
            }
        }
        dirty = true;
    }

    public List<BeltItemData> extractItemsInRange(double minProgress, double maxProgress) {
        List<BeltItemData> extracted = new ArrayList<>();
        for (int l = 0; l < laneCount; l++) {
            BeltLane lane = lanes[l];
            List<BeltItemData> items = lane.getItems();
            List<BeltItemData> toRemove = new ArrayList<>();

            for (BeltItemData item : items) {
                if (item.getProgress() >= minProgress - EPS && item.getProgress() < maxProgress + EPS) {
                    toRemove.add(item);
                }
            }

            for (BeltItemData item : toRemove) {
                items.remove(item);
                extracted.add(item);
            }
        }
        if (!extracted.isEmpty()) dirty = true;
        return extracted;
    }

    public boolean insertItem(BeltItemData item) {
        int lane = item.getLane();
        if (lane < 0 || lane >= laneCount) {
            lane = 0;
            item.setLane(0);
        }
        boolean inserted = lanes[lane].addSorted(item);
        if (inserted) dirty = true;
        return inserted;
    }

    public boolean insertStack(ItemStack stack, int lane, double progress) {
        if (lane < 0 || lane >= laneCount) return false;
        if (!lanes[lane].isSlotFree(progress)) return false;

        BeltItemData item = new BeltItemData(stack, lane, progress);
        boolean inserted = lanes[lane].addSorted(item);
        if (inserted) dirty = true;
        return inserted;
    }

    public BeltItemData removeItem(long uid) {
        for (BeltLane lane : lanes) {
            BeltItemData item = lane.findByUid(uid);
            if (item != null) {
                lane.remove(item);
                dirty = true;
                return item;
            }
        }
        return null;
    }

    public void dropAllItems(World world) {
        for (int l = 0; l < laneCount; l++) {
            BeltLane lane = lanes[l];
            for (BeltItemData item : lane.getItems()) {
                int blockIndex = Math.min((int) item.getProgress(), blocks.size() - 1);
                if (blockIndex < 0) blockIndex = 0;
                BlockPos blockPos = blocks.get(blockIndex);

                Block block = world.getBlockState(blockPos).getBlock();
                Vec3d dropPos;
                if (block instanceof BlockConveyor) {
                    double localProgress = item.getProgress() - blockIndex;
                    dropPos = ((BlockConveyor) block).getLanePoint(blockPos, direction, l, localProgress);
                } else {
                    dropPos = new Vec3d(blockPos.getX() + 0.5, blockPos.getY() + 0.5, blockPos.getZ() + 0.5);
                }

                EntityItem entityItem = new EntityItem(world, dropPos.x, dropPos.y, dropPos.z, item.getStack().copy());
                world.spawnEntity(entityItem);
            }
            lane.clear();
        }
    }

    public List<BeltItemData> getAllItems() {
        List<BeltItemData> all = new ArrayList<>();
        for (BeltLane lane : lanes) {
            lane.copyTo(all);
        }
        return all;
    }

    public NBTTagCompound writeToNBT() {
        NBTTagCompound nbt = new NBTTagCompound();
        NBTTagList itemList = new NBTTagList();
        for (BeltItemData item : getAllItems()) {
            itemList.appendTag(item.writeToNBT());
        }
        nbt.setTag("Items", itemList);
        return nbt;
    }

    public void readItemsFromNBT(NBTTagCompound nbt) {
        for (BeltLane lane : lanes) {
            lane.clear();
        }
        NBTTagList itemList = nbt.getTagList("Items", Constants.NBT.TAG_COMPOUND);
        for (int i = 0; i < itemList.tagCount(); i++) {
            BeltItemData item = BeltItemData.readFromNBT(itemList.getCompoundTagAt(i));
            if (!item.getStack().isEmpty()) {
                insertItem(item);
            }
        }
    }

    public boolean insertItemDirect(BeltItemData item) {
        int lane = item.getLane();
        if (lane < 0 || lane >= laneCount) {
            lane = 0;
            item.setLane(0);
        }
        boolean inserted = lanes[lane].addSortedDirect(item);
        if (inserted) dirty = true;
        return inserted;
    }
}