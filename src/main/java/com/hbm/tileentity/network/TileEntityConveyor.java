package com.hbm.tileentity.network;

import api.hbm.block.IEnterableBlock;
import com.hbm.blocks.network.BlockConveyor;
import com.hbm.blocks.network.ConveyorItemData;
import com.hbm.render.conveyor.ConveyorVisualManager;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.SPacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ITickable;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.common.util.Constants;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class TileEntityConveyor extends TileEntity implements ITickable {

    private static final double EPS = 1.0E-5D;

    public static final int SLOT_COUNT = ConveyorItemData.MAX_ITEMS_PER_LANE;
    public static final double[] SLOTS = new double[SLOT_COUNT];

    static {
        for (int i = 0; i < SLOT_COUNT; i++) {
            SLOTS[i] = ConveyorItemData.ENTRY_PROGRESS + i * ConveyorItemData.ITEM_LENGTH;
        }
    }

    private final List<ConveyorItemData> items = new ArrayList<>();
    public boolean needsSync = false;
    private int prevHash = 0;
    private int syncTimer = 0;
    private static final int SYNC_INTERVAL = 4;

    @Override
    public void update() {
        BlockConveyor conveyor = getConveyor();
        if (conveyor == null) return;

        if (world.isRemote) {
            clientTick(conveyor);
            return;
        }

        EnumFacing facing = conveyor.getLaneFacing(world, pos);
        double speed = getSpeed(conveyor);
        int laneCount = conveyor.getLaneCount();

        List<ConveyorItemData> toRemove = new ArrayList<>();

        for (int lane = 0; lane < laneCount; lane++) {
            tickLane(conveyor, facing, speed, lane, toRemove);
        }

        if (!toRemove.isEmpty()) {
            for (ConveyorItemData item : toRemove) {
                items.remove(item);
            }
            needsSync = true;
        }

        syncTimer++;
        int hash = computeStateHash();
        if (hash != prevHash) {
            prevHash = hash;
            needsSync = true;
        }

        if (needsSync && syncTimer >= SYNC_INTERVAL) {
            syncTimer = 0;
            markDirty();
            syncToClient();
            needsSync = false;
        }
    }

    private void clientTick(BlockConveyor conveyor) {
        EnumFacing facing = conveyor.getLaneFacing(world, pos);
        double[] offsets = conveyor.getLaneOffsets();
        double speed = getSpeed(conveyor);

        ConveyorVisualManager mgr = ConveyorVisualManager.get();

        for (ConveyorItemData item : items) {
            if (!item.isStopped()) {
                double newProgress = item.getProgress() + speed;
                double limit = ConveyorItemData.EXIT_PROGRESS;
                item.setProgress(Math.min(newProgress, limit));
            }

            int lane = item.getLane();
            if (lane < 0 || lane >= offsets.length) lane = 0;

            Vec3d wp = conveyor.getWorldPosition(pos, facing, offsets[lane], item.getProgress());

            mgr.updateItem(
                    item.getUniqueId(),
                    item.getStack(),
                    wp.x, wp.y, wp.z,
                    item.getYaw(),
                    speed,
                    item.isStopped()
            );
        }
    }

    private void clientPush(BlockConveyor conveyor) {
        if (conveyor == null) return;
        EnumFacing facing = conveyor.getLaneFacing(world, pos);
        double[] offsets = conveyor.getLaneOffsets();
        double speed = getSpeed(conveyor);

        ConveyorVisualManager mgr = ConveyorVisualManager.get();

        for (ConveyorItemData item : items) {
            int lane = item.getLane();
            if (lane < 0 || lane >= offsets.length) lane = 0;

            Vec3d wp = conveyor.getWorldPosition(pos, facing, offsets[lane], item.getProgress());

            mgr.updateItem(
                    item.getUniqueId(),
                    item.getStack(),
                    wp.x, wp.y, wp.z,
                    item.getYaw(),
                    speed,
                    item.isStopped()
            );
        }
    }

    private void tickLane(BlockConveyor conveyor, EnumFacing facing, double speed, int lane,
                          List<ConveyorItemData> toRemove) {

        List<ConveyorItemData> laneItems = getSortedLaneItems(lane);

        for (int i = 0; i < laneItems.size(); i++) {
            ConveyorItemData item = laneItems.get(i);
            double currentProgress = item.getProgress();

            double limit;
            if (i == 0) {
                limit = getLeaderLimit(conveyor, facing, lane);
            } else {
                ConveyorItemData ahead = laneItems.get(i - 1);
                limit = snapSlotDown(ahead.getProgress()) - ConveyorItemData.ITEM_LENGTH;
                if (limit < ConveyorItemData.ENTRY_PROGRESS) {
                    limit = ConveyorItemData.ENTRY_PROGRESS;
                }
            }

            double newProgress = currentProgress + speed;

            if (newProgress > limit + EPS) {
                double snapped = snapSlotDown(limit);
                newProgress = Math.max(snapped, currentProgress);
            }

            boolean wasStopped = item.isStopped();
            boolean nowStopped = Math.abs(newProgress - currentProgress) < EPS;

            item.setProgress(newProgress);
            item.setStopped(nowStopped);
            updateItemYaw(item, facing);

            if (wasStopped != nowStopped) {
                needsSync = true;
            }
        }

        if (!laneItems.isEmpty()) {
            ConveyorItemData leader = laneItems.get(0);
            if (leader.getProgress() >= ConveyorItemData.EXIT_PROGRESS - EPS) {
                tryTransferForward(leader, conveyor, facing, lane, toRemove);
            }
        }
    }

    private double getLeaderLimit(BlockConveyor conveyor, EnumFacing facing, int lane) {
        BlockPos nextPos = pos.offset(facing);
        IBlockState nextState = world.getBlockState(nextPos);
        Block nextBlock = nextState.getBlock();

        if (nextBlock instanceof BlockConveyor) {
            BlockConveyor nextConveyor = (BlockConveyor) nextBlock;
            if (!nextConveyor.usesLaneQueues()) return Double.POSITIVE_INFINITY;

            TileEntity te = world.getTileEntity(nextPos);
            if (te instanceof TileEntityConveyor) {
                TileEntityConveyor nextTile = (TileEntityConveyor) te;
                BlockConveyor.IncomingRoute route = nextConveyor.resolveIncomingRoute(
                        world, pos, conveyor, lane, nextPos);

                if (route.lane < 0) return ConveyorItemData.EXIT_PROGRESS;

                int targetSlot;
                if (route.arc != null) {
                    targetSlot = getFirstFreeSlotAtOrAbove(nextTile, route.lane, route.progress);
                } else {
                    targetSlot = 0;
                }

                if (targetSlot >= 0 && nextTile.isSlotFree(route.lane, targetSlot)) {
                    return Double.POSITIVE_INFINITY;
                }
                return ConveyorItemData.EXIT_PROGRESS;
            }
        }

        if (nextBlock instanceof IEnterableBlock) return Double.POSITIVE_INFINITY;
        if (nextState.isFullBlock()) return ConveyorItemData.EXIT_PROGRESS;
        return Double.POSITIVE_INFINITY;
    }

    private int getFirstFreeSlotAtOrAbove(TileEntityConveyor tile, int lane, double progress) {
        for (int i = 0; i < SLOT_COUNT; i++) {
            if (SLOTS[i] < progress - EPS) continue;
            if (tile.isSlotFree(lane, i)) return i;
        }
        return -1;
    }

    private double snapSlotDown(double progress) {
        double best = SLOTS[0];
        for (double slot : SLOTS) {
            if (slot <= progress + EPS) {
                best = slot;
            }
        }
        return best;
    }

    private List<ConveyorItemData> getSortedLaneItems(int lane) {
        List<ConveyorItemData> result = new ArrayList<>();
        for (ConveyorItemData item : items) {
            if (item.getLane() == lane) result.add(item);
        }
        result.sort((a, b) -> Double.compare(b.getProgress(), a.getProgress()));
        return result;
    }

    private boolean tryTransferForward(ConveyorItemData item, BlockConveyor conveyor,
                                       EnumFacing facing, int lane,
                                       List<ConveyorItemData> toRemove) {
        BlockPos nextPos = pos.offset(facing);
        IBlockState nextState = world.getBlockState(nextPos);
        Block nextBlock = nextState.getBlock();

        if (nextBlock instanceof BlockConveyor) {
            BlockConveyor nextConveyor = (BlockConveyor) nextBlock;
            if (!nextConveyor.usesLaneQueues()) {
                ejectItem(item, conveyor, facing);
                toRemove.add(item);
                return true;
            }

            TileEntity te = world.getTileEntity(nextPos);
            if (!(te instanceof TileEntityConveyor)) return false;

            TileEntityConveyor nextTile = (TileEntityConveyor) te;
            BlockConveyor.IncomingRoute route = nextConveyor.resolveIncomingRoute(
                    world, pos, conveyor, lane, nextPos);

            if (route.lane < 0) return false;

            int targetSlot;
            if (route.arc != null) {
                targetSlot = getFirstFreeSlotAtOrAbove(nextTile, route.lane, route.progress);
            } else {
                targetSlot = 0;
            }

            if (targetSlot < 0) return false;

            if (nextTile.isSlotFree(route.lane, targetSlot)) {
                toRemove.add(item);
                ConveyorItemData transferred = new ConveyorItemData(
                        item.getStack(), route.lane, SLOTS[targetSlot]);
                transferred.setUniqueId(item.getUniqueId());
                transferred.setStopped(false);
                EnumFacing nextFacing = nextConveyor.getLaneFacing(world, nextPos);
                updateItemYaw(transferred, nextFacing);
                nextTile.insertItem(transferred);
                return true;
            }
            return false;
        }

        if (nextBlock instanceof IEnterableBlock) {
            IEnterableBlock enterable = (IEnterableBlock) nextBlock;
            EnumFacing enterDir = facing.getOpposite();
            if (enterable.canItemEnter(world, nextPos.getX(), nextPos.getY(), nextPos.getZ(), enterDir, null)) {
                enterable.onItemEnter(world, nextPos.getX(), nextPos.getY(), nextPos.getZ(), enterDir, null);
                toRemove.add(item);
                return true;
            }
            return false;
        }

        if (nextState.isFullBlock()) return false;

        ejectItem(item, conveyor, facing);
        toRemove.add(item);
        return true;
    }

    public boolean isSlotFree(int lane, int slotIndex) {
        if (slotIndex < 0 || slotIndex >= SLOT_COUNT) return false;
        double slotProgress = SLOTS[slotIndex];
        for (ConveyorItemData item : items) {
            if (item.getLane() != lane) continue;
            if (Math.abs(item.getProgress() - slotProgress) < ConveyorItemData.ITEM_LENGTH - EPS) {
                return false;
            }
        }
        return true;
    }

    public int getSlotIndexAtOrAbove(double progress) {
        for (int i = 0; i < SLOT_COUNT; i++) {
            if (SLOTS[i] >= progress - EPS) return i;
        }
        return SLOT_COUNT - 1;
    }

    private int computeStateHash() {
        int hash = items.size();
        for (ConveyorItemData item : items) {
            hash = hash * 31 + Long.hashCode(item.getUniqueId());
            hash = hash * 31 + item.getLane();
            hash = hash * 31 + Long.hashCode(Double.doubleToLongBits(
                    Math.round(item.getProgress() * 1000.0D) / 1000.0D));
            hash = hash * 31 + (item.isStopped() ? 1 : 0);
        }
        return hash;
    }

    public void forceSyncNow() {
        if (world != null && !world.isRemote) {
            prevHash = computeStateHash();
            markDirty();
            syncToClient();
            needsSync = false;
            syncTimer = 0;
        }
    }

    private void updateItemYaw(ConveyorItemData item, EnumFacing facing) {
        float yaw;
        switch (facing) {
            case SOUTH: yaw = 0.0F; break;
            case WEST: yaw = 90.0F; break;
            case NORTH: yaw = 180.0F; break;
            case EAST: yaw = -90.0F; break;
            default: return;
        }
        item.setYaw(yaw);
    }

    private void ejectItem(ConveyorItemData item, BlockConveyor conveyor, EnumFacing facing) {
        Vec3d ejectPos = conveyor.getLanePoint(pos, facing, item.getLane(), 1.0D);
        EntityItem entityItem = new EntityItem(world,
                ejectPos.x + facing.getXOffset() * 0.25D,
                ejectPos.y,
                ejectPos.z + facing.getZOffset() * 0.25D,
                item.getStack().copy());
        double ejectSpeed = getSpeed(conveyor) * 2.0D;
        entityItem.motionX = facing.getXOffset() * ejectSpeed;
        entityItem.motionY = 0.1D;
        entityItem.motionZ = facing.getZOffset() * ejectSpeed;
        entityItem.velocityChanged = true;
        world.spawnEntity(entityItem);
    }

    public boolean insertItem(ConveyorItemData item) {
        BlockConveyor conveyor = getConveyor();
        if (conveyor == null) return false;
        int lane = item.getLane();
        if (lane < 0 || lane >= conveyor.getLaneCount()) {
            lane = 0;
            item.setLane(0);
        }
        items.add(item);
        needsSync = true;
        return true;
    }

    public boolean insertStack(ItemStack stack, int lane, int slotIndex) {
        BlockConveyor conveyor = getConveyor();
        if (conveyor == null) return false;
        if (lane < 0 || lane >= conveyor.getLaneCount()) return false;
        if (slotIndex < 0 || slotIndex >= SLOT_COUNT) return false;
        if (!isSlotFree(lane, slotIndex)) return false;

        ConveyorItemData item = new ConveyorItemData(stack, lane, SLOTS[slotIndex]);
        EnumFacing facing = conveyor.getLaneFacing(world, pos);
        updateItemYaw(item, facing);
        items.add(item);
        needsSync = true;
        return true;
    }

    public int findFirstFreeSlot(int lane) {
        for (int s = 0; s < SLOT_COUNT; s++) {
            if (isSlotFree(lane, s)) return s;
        }
        return -1;
    }

    public int findNearestFreeSlot(int lane, double progress) {
        int best = -1;
        double bestDist = Double.MAX_VALUE;
        for (int s = 0; s < SLOT_COUNT; s++) {
            if (!isSlotFree(lane, s)) continue;
            double dist = Math.abs(SLOTS[s] - progress);
            if (dist < bestDist) {
                bestDist = dist;
                best = s;
            }
        }
        return best;
    }

    public ConveyorItemData findNearestItem(int lane, double progress) {
        ConveyorItemData best = null;
        double bestDist = Double.MAX_VALUE;
        for (ConveyorItemData item : items) {
            if (item.getLane() != lane) continue;
            double dist = Math.abs(item.getProgress() - progress);
            if (dist < bestDist) {
                bestDist = dist;
                best = item;
            }
        }
        if (best != null && bestDist < ConveyorItemData.ITEM_LENGTH) return best;
        return null;
    }

    public void removeItem(ConveyorItemData item) {
        items.remove(item);
        needsSync = true;
    }

    public int getClosestAvailableLane(Vec3d probePoint) {
        BlockConveyor conveyor = getConveyor();
        if (conveyor == null) return -1;
        EnumFacing facing = conveyor.getLaneFacing(world, pos);
        double[] offsets = conveyor.getLaneOffsets();
        int bestLane = -1;
        double bestDist = Double.MAX_VALUE;
        for (int i = 0; i < offsets.length; i++) {
            if (findFirstFreeSlot(i) < 0) continue;
            Vec3d a = conveyor.getLanePoint(pos, facing, i, 0.0D);
            Vec3d b = conveyor.getLanePoint(pos, facing, i, 1.0D);
            double dist = BlockConveyor.distanceSqToSegmentXZ(probePoint, a, b);
            if (dist < bestDist) {
                bestDist = dist;
                bestLane = i;
            }
        }
        return bestLane;
    }

    public List<ConveyorItemData> getItems() {
        return items;
    }

    public double getSpeed(BlockConveyor conveyor) {
        return conveyor.getConveyorSpeed();
    }

    @Nullable
    public BlockConveyor getConveyor() {
        if (world == null) return null;
        Block block = world.getBlockState(pos).getBlock();
        if (block instanceof BlockConveyor) return (BlockConveyor) block;
        return null;
    }

    private void syncToClient() {
        IBlockState state = world.getBlockState(pos);
        world.notifyBlockUpdate(pos, state, state, 3);
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound nbt) {
        super.writeToNBT(nbt);
        NBTTagList list = new NBTTagList();
        for (ConveyorItemData item : items) {
            list.appendTag(item.writeToNBT());
        }
        nbt.setTag("ConveyorItems", list);
        return nbt;
    }

    @Override
    public void readFromNBT(NBTTagCompound nbt) {
        super.readFromNBT(nbt);

        NBTTagList list = nbt.getTagList("ConveyorItems", Constants.NBT.TAG_COMPOUND);

        if (world != null && world.isRemote) {
            Set<Long> oldUids = new HashSet<>();
            for (ConveyorItemData item : items) {
                oldUids.add(item.getUniqueId());
            }

            items.clear();
            Set<Long> newUids = new HashSet<>();
            for (int i = 0; i < list.tagCount(); i++) {
                ConveyorItemData item = ConveyorItemData.readFromNBT(list.getCompoundTagAt(i));
                if (!item.getStack().isEmpty()) {
                    items.add(item);
                    newUids.add(item.getUniqueId());
                }
            }

            ConveyorVisualManager mgr = ConveyorVisualManager.get();
            for (Long uid : oldUids) {
                if (!newUids.contains(uid)) {
                    mgr.removeItem(uid);
                }
            }

            clientPush(getConveyor());
        } else {
            items.clear();
            for (int i = 0; i < list.tagCount(); i++) {
                ConveyorItemData item = ConveyorItemData.readFromNBT(list.getCompoundTagAt(i));
                if (!item.getStack().isEmpty()) items.add(item);
            }
        }
    }

    @Override
    @Nullable
    public SPacketUpdateTileEntity getUpdatePacket() {
        return new SPacketUpdateTileEntity(pos, 0, getUpdateTag());
    }

    @Override
    public NBTTagCompound getUpdateTag() {
        return writeToNBT(new NBTTagCompound());
    }

    @Override
    public void onDataPacket(NetworkManager net, SPacketUpdateTileEntity pkt) {
        readFromNBT(pkt.getNbtCompound());
    }

    @Override
    public void invalidate() {
        if (world != null && !world.isRemote) {
            BlockConveyor conveyor = getConveyor();
            for (ConveyorItemData item : items) {
                Vec3d dropPos;
                if (conveyor != null) {
                    EnumFacing facing = conveyor.getLaneFacing(world, pos);
                    dropPos = conveyor.getLanePoint(pos, facing, item.getLane(), item.getProgress());
                } else {
                    dropPos = new Vec3d(pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D);
                }
                EntityItem entityItem = new EntityItem(world, dropPos.x, dropPos.y, dropPos.z, item.getStack().copy());
                world.spawnEntity(entityItem);
            }
            items.clear();
        }
        super.invalidate();
    }
}