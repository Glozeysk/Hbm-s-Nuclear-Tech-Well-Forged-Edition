package com.hbm.blocks.network.conveyor;

import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ClientBeltManager {

    private static final ClientBeltManager INSTANCE = new ClientBeltManager();

    private final Map<Long, ClientBeltSegment> segmentsById = new HashMap<>();
    private final Map<BlockPos, ClientBeltSegment> segmentsByPos = new HashMap<>();

    private ClientBeltManager() {}

    public static ClientBeltManager get() {
        return INSTANCE;
    }

    public void putSegment(ClientBeltSegment segment) {
        ClientBeltSegment old = segmentsById.put(segment.segmentId, segment);
        if (old != null) {
            for (BlockPos pos : old.blocks) {
                ClientBeltSegment mapped = segmentsByPos.get(pos);
                if (mapped == old) {
                    segmentsByPos.remove(pos);
                }
            }
        }
        for (BlockPos pos : segment.blocks) {
            segmentsByPos.put(pos, segment);
        }
    }

    public ClientBeltSegment getSegment(long segmentId) {
        return segmentsById.get(segmentId);
    }

    public ClientBeltSegment getSegmentAt(BlockPos pos) {
        return segmentsByPos.get(pos);
    }

    public void removeSegment(long segmentId) {
        ClientBeltSegment old = segmentsById.remove(segmentId);
        if (old == null) return;
        for (BlockPos pos : old.blocks) {
            ClientBeltSegment mapped = segmentsByPos.get(pos);
            if (mapped == old) {
                segmentsByPos.remove(pos);
            }
        }
    }

    public void applySync(long segmentId, List<BlockPos> blocks, EnumFacing facing, double speed, int laneCount, double[] laneOffsets, BeltItemData[] items) {
        applySync(segmentId, blocks, facing, speed, laneCount, false, false, laneOffsets, items);
    }

    public void applySync(long segmentId, List<BlockPos> blocks, EnumFacing facing, double speed, int laneCount, boolean isVertical, boolean isUpward, double[] laneOffsets, BeltItemData[] items) {
        int actualLaneCount = Math.max(1, laneCount);

        ClientBeltSegment segment = segmentsById.get(segmentId);
        if (segment == null
                || segment.laneCount != actualLaneCount
                || segment.isVertical != isVertical
                || segment.isUpward != isUpward
                || segment.direction != facing
                || !sameBlocks(segment.blocks, blocks)) {
            segment = new ClientBeltSegment(segmentId, blocks, facing, speed, actualLaneCount, isVertical, isUpward, laneOffsets);
            putSegment(segment);
        }

        segment.applyItems(items);
    }

    private boolean sameBlocks(List<BlockPos> a, List<BlockPos> b) {
        if (a.size() != b.size()) return false;
        for (int i = 0; i < a.size(); i++) {
            if (!a.get(i).equals(b.get(i))) return false;
        }
        return true;
    }

    public void tick() {
        for (ClientBeltSegment seg : segmentsById.values()) {
            seg.tick();
        }
    }

    public List<ClientBeltSegment> getAllSegments() {
        return new ArrayList<>(segmentsById.values());
    }

    public void clear() {
        segmentsById.clear();
        segmentsByPos.clear();
    }
}