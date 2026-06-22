package com.hbm.blocks.network.conveyor;

import com.hbm.blocks.network.BlockConveyor;
import com.hbm.handler.threading.PacketThreading;
import com.hbm.lib.RefStrings;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.common.network.NetworkRegistry;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Mod.EventBusSubscriber(modid = RefStrings.MODID)
public final class BeltSegmentManager {

    private BeltSegmentManager() {}

    private static final Map<World, Map<BlockPos, BeltSegment>> worldSegments = new IdentityHashMap<>();
    private static final int SYNC_INTERVAL = 4;

    public static BeltSegment getSegmentAt(World world, BlockPos pos) {
        Map<BlockPos, BeltSegment> segments = worldSegments.get(world);
        if (segments == null) return null;
        return segments.get(pos);
    }

    public static BeltSegment getOrCreateSegment(World world, BlockPos pos) {
        return getOrCreateSegment(world, pos, null);
    }

    private static BeltSegment getOrCreateSegment(World world, BlockPos pos, BlockPos excludedPos) {
        BeltSegment existing = getSegmentAt(world, pos);
        if (existing != null) return existing;

        IBlockState state = world.getBlockState(pos);
        Block block = state.getBlock();
        if (!(block instanceof BlockConveyor)) return null;

        BlockConveyor conveyor = (BlockConveyor) block;
        EnumFacing facing = conveyor.getLaneFacing(world, pos);
        double speed = conveyor.getConveyorSpeed();
        int laneCount = conveyor.getLaneCount();

        List<BlockPos> chain = buildChain(world, pos, facing, conveyor, excludedPos);
        if (chain.isEmpty()) return null;

        BeltSegment segment = new BeltSegment(chain, facing, speed, laneCount);
        registerSegment(world, segment);
        return segment;
    }

    private static List<BlockPos> buildChain(World world, BlockPos origin, EnumFacing facing, BlockConveyor conveyorType) {
        return buildChain(world, origin, facing, conveyorType, null);
    }

    private static List<BlockPos> buildChain(World world, BlockPos origin, EnumFacing facing, BlockConveyor conveyorType, BlockPos excludedPos) {
        List<BlockPos> backward = new ArrayList<>();
        BlockPos check = origin.offset(facing.getOpposite());
        while (isCompatible(world, check, facing, conveyorType, excludedPos)) {
            backward.add(0, check);
            check = check.offset(facing.getOpposite());
        }

        List<BlockPos> chain = new ArrayList<>(backward);
        chain.add(origin);

        check = origin.offset(facing);
        while (isCompatible(world, check, facing, conveyorType, excludedPos)) {
            chain.add(check);
            check = check.offset(facing);
        }

        return chain;
    }

    private static boolean isCompatible(World world, BlockPos pos, EnumFacing facing, BlockConveyor type, BlockPos excludedPos) {
        if (excludedPos != null && excludedPos.equals(pos)) return false;

        IBlockState state = world.getBlockState(pos);
        Block block = state.getBlock();
        if (!(block instanceof BlockConveyor)) return false;
        BlockConveyor conv = (BlockConveyor) block;
        if (conv.getLaneFacing(world, pos) != facing) return false;
        if (conv.getConveyorSpeed() != type.getConveyorSpeed()) return false;
        if (conv.getLaneCount() != type.getLaneCount()) return false;
        return true;
    }

    public static void registerSegment(World world, BeltSegment segment) {
        Map<BlockPos, BeltSegment> segments = worldSegments.computeIfAbsent(world, k -> new HashMap<>());
        for (BlockPos pos : segment.getBlocks()) {
            BeltSegment old = segments.get(pos);
            if (old != null && old != segment) {
                unregisterSegment(world, old);
            }
            segments.put(pos, segment);
        }
    }

    public static void unregisterSegment(World world, BeltSegment segment) {
        Map<BlockPos, BeltSegment> segments = worldSegments.get(world);
        if (segments == null) return;
        for (BlockPos pos : segment.getBlocks()) {
            if (segments.get(pos) == segment) {
                segments.remove(pos);
            }
        }

        if (world instanceof WorldServer) {
            sendRemove((WorldServer) world, segment);
        }
    }

    public static void onBlockPlaced(World world, BlockPos pos) {
        if (world.isRemote) return;
        rebuildMergedSegment(world, pos);
    }

    private static void rebuildMergedSegment(World world, BlockPos pos) {
        IBlockState state = world.getBlockState(pos);
        Block block = state.getBlock();
        if (!(block instanceof BlockConveyor)) return;

        BlockConveyor conveyor = (BlockConveyor) block;
        EnumFacing facing = conveyor.getLaneFacing(world, pos);
        double speed = conveyor.getConveyorSpeed();
        int laneCount = conveyor.getLaneCount();

        List<BlockPos> chain = buildChain(world, pos, facing, conveyor);
        if (chain.isEmpty()) return;

        Set<BeltSegment> affected = new HashSet<>();
        for (BlockPos bp : chain) {
            BeltSegment seg = getSegmentAt(world, bp);
            if (seg != null) {
                affected.add(seg);
            }
        }

        List<BeltItemSnapshot> snapshots = new ArrayList<>();
        for (BeltSegment seg : affected) {
            snapshots.addAll(snapshotSegmentItems(seg));
        }

        for (BeltSegment seg : affected) {
            unregisterSegment(world, seg);
        }

        BeltSegment merged = new BeltSegment(chain, facing, speed, laneCount);
        registerSegment(world, merged);

        restoreSnapshotsToSegment(merged, snapshots);
        merged.markDirty();

        if (world instanceof WorldServer) {
            sendSync((WorldServer) world, merged);
            merged.clearDirty();
        }
    }

    public static void onBlockRemoved(World world, BlockPos pos, IBlockState oldState) {
        if (world.isRemote) return;

        BeltSegment oldSegment = getSegmentAt(world, pos);
        if (oldSegment == null) return;

        List<BlockPos> oldBlocks = new ArrayList<>(oldSegment.getBlocks());
        if (oldBlocks.isEmpty()) return;

        List<BeltItemSnapshot> brokenItems = new ArrayList<>();
        List<BeltItemSnapshot> survivors = new ArrayList<>();

        for (BeltItemData item : oldSegment.getAllItems()) {
            int oldIndex = (int) Math.floor(item.getProgress());
            if (oldIndex < 0) oldIndex = 0;
            if (oldIndex >= oldBlocks.size()) oldIndex = oldBlocks.size() - 1;

            BlockPos itemBlock = oldBlocks.get(oldIndex);
            double localProgress = item.getProgress() - oldIndex;
            if (localProgress < 0.0D) localProgress = 0.0D;
            if (localProgress > 1.0D) localProgress = 1.0D;

            BeltItemSnapshot snapshot = new BeltItemSnapshot(
                    item.getStack(),
                    item.getLane(),
                    itemBlock,
                    localProgress,
                    item.getUniqueId(),
                    item.isStopped()
            );

            if (itemBlock.equals(pos)) {
                brokenItems.add(snapshot);
            } else {
                survivors.add(snapshot);
            }
        }

        for (BeltItemSnapshot snapshot : brokenItems) {
            dropBrokenItem(world, pos, oldState, snapshot);
        }

        unregisterSegment(world, oldSegment);

        for (BlockPos blockPos : oldBlocks) {
            if (blockPos.equals(pos)) continue;
            if (!(world.getBlockState(blockPos).getBlock() instanceof BlockConveyor)) continue;
            if (getSegmentAt(world, blockPos) == null) {
                BeltSegment rebuilt = getOrCreateSegment(world, blockPos, pos);
                if (rebuilt != null) {
                    rebuilt.markDirty();
                }
            }
        }

        for (BeltItemSnapshot snapshot : survivors) {
            BeltSegment target = getSegmentAt(world, snapshot.blockPos);
            if (target == null) continue;

            int newIndex = target.getBlockIndex(snapshot.blockPos);
            if (newIndex < 0) continue;

            BeltItemData restored = new BeltItemData(
                    snapshot.stack,
                    snapshot.lane,
                    newIndex + snapshot.localProgress
            );
            restored.setUniqueId(snapshot.uid);
            restored.setStopped(snapshot.stopped);
            target.insertItem(restored);
            target.markDirty();
        }
    }

    public static void onBlockRotated(World world, BlockPos pos) {
        if (world.isRemote) return;

        BeltSegment old = getSegmentAt(world, pos);
        if (old != null) {
            old.dropAllItems(world);
            unregisterSegment(world, old);

            for (BlockPos bp : old.getBlocks()) {
                if (!bp.equals(pos)) {
                    rebuildAt(world, bp);
                }
            }
        }

        rebuildAt(world, pos);
    }

    private static void rebuildAt(World world, BlockPos pos) {
        BeltSegment existing = getSegmentAt(world, pos);
        if (existing != null) {
            unregisterSegment(world, existing);
        }

        IBlockState state = world.getBlockState(pos);
        Block block = state.getBlock();
        if (!(block instanceof BlockConveyor)) return;

        getOrCreateSegment(world, pos);
    }

    private static List<BeltItemSnapshot> snapshotSegmentItems(BeltSegment segment) {
        List<BeltItemSnapshot> result = new ArrayList<>();
        List<BlockPos> blocks = segment.getBlocks();

        for (BeltItemData item : segment.getAllItems()) {
            int blockIndex = (int) Math.floor(item.getProgress());
            if (blockIndex < 0) blockIndex = 0;
            if (blockIndex >= blocks.size()) blockIndex = blocks.size() - 1;

            BlockPos blockPos = blocks.get(blockIndex);
            double localProgress = item.getProgress() - blockIndex;
            if (localProgress < 0.0D) localProgress = 0.0D;
            if (localProgress > 1.0D) localProgress = 1.0D;

            result.add(new BeltItemSnapshot(
                    item.getStack(),
                    item.getLane(),
                    blockPos,
                    localProgress,
                    item.getUniqueId(),
                    item.isStopped()
            ));
        }

        return result;
    }

    private static void restoreSnapshotsToSegment(BeltSegment segment, List<BeltItemSnapshot> snapshots) {
        for (BeltItemSnapshot snapshot : snapshots) {
            int newIndex = segment.getBlockIndex(snapshot.blockPos);
            if (newIndex < 0) continue;

            BeltItemData restored = new BeltItemData(
                    snapshot.stack,
                    snapshot.lane,
                    newIndex + snapshot.localProgress
            );
            restored.setUniqueId(snapshot.uid);
            restored.setStopped(snapshot.stopped);
            segment.insertItem(restored);
        }
    }

    private static void dropBrokenItem(World world, BlockPos pos, IBlockState oldState, BeltItemSnapshot snapshot) {
        Block block = oldState.getBlock();
        Vec3d dropPos;

        if (block instanceof BlockConveyor) {
            BlockConveyor conveyor = (BlockConveyor) block;
            EnumFacing facing = oldState.getValue(BlockConveyor.FACING).getOpposite();
            dropPos = conveyor.getLanePoint(pos, facing, snapshot.lane, snapshot.localProgress);
        } else {
            dropPos = new Vec3d(pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D);
        }

        EntityItem entity = new EntityItem(world, dropPos.x, dropPos.y, dropPos.z, snapshot.stack.copy());
        world.spawnEntity(entity);
    }

    @SubscribeEvent
    public static void onWorldTick(TickEvent.WorldTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (!(event.world instanceof WorldServer)) return;

        WorldServer world = (WorldServer) event.world;
        Map<BlockPos, BeltSegment> segments = worldSegments.get(world);
        if (segments == null) return;

        List<BeltSegment> uniqueSegments = new ArrayList<>();
        for (BeltSegment seg : segments.values()) {
            if (!uniqueSegments.contains(seg)) {
                uniqueSegments.add(seg);
            }
        }

        for (BeltSegment segment : uniqueSegments) {
            segment.tick(world);
        }

        if ((world.getTotalWorldTime() % SYNC_INTERVAL) == 0L) {
            for (BeltSegment segment : uniqueSegments) {
                if (segment.isDirty()) {
                    sendSync(world, segment);
                    segment.clearDirty();
                }
            }
        }
    }

    private static void sendSync(WorldServer world, BeltSegment segment) {
        BlockPos head = segment.getHeadPos();
        PacketBeltSync pkt = new PacketBeltSync(segment);
        PacketThreading.createAllAroundThreadedPacket(pkt,
                new NetworkRegistry.TargetPoint(world.provider.getDimension(),
                        head.getX() + 0.5, head.getY() + 0.5, head.getZ() + 0.5, 128.0D));
    }

    private static void sendRemove(WorldServer world, BeltSegment segment) {
        BlockPos head = segment.getHeadPos();
        PacketBeltRemove pkt = new PacketBeltRemove(segment.getSegmentId());
        PacketThreading.createAllAroundThreadedPacket(pkt,
                new NetworkRegistry.TargetPoint(world.provider.getDimension(),
                        head.getX() + 0.5, head.getY() + 0.5, head.getZ() + 0.5, 128.0D));
    }

    public static void clearWorld(World world) {
        worldSegments.remove(world);
    }

    public static void clearAll() {
        worldSegments.clear();
    }

    private static class BeltItemSnapshot {
        private final ItemStack stack;
        private final int lane;
        private final BlockPos blockPos;
        private final double localProgress;
        private final long uid;
        private final boolean stopped;

        private BeltItemSnapshot(ItemStack stack, int lane, BlockPos blockPos, double localProgress, long uid, boolean stopped) {
            this.stack = stack.copy();
            this.lane = lane;
            this.blockPos = blockPos;
            this.localProgress = localProgress;
            this.uid = uid;
            this.stopped = stopped;
        }
    }
}