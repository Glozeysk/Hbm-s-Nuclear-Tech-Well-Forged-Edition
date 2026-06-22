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

    public static ClientBeltManager get() {
        return INSTANCE;
    }

    public void applySync(long segmentId, List<BlockPos> blocks, EnumFacing direction,
                          float speed, int laneCount, BeltItemData[] items) {
        ClientBeltSegment existing = segmentsById.get(segmentId);

        if (existing != null) {
            existing.applyItems(items);
            return;
        }

        ClientBeltSegment segment = new ClientBeltSegment(segmentId, blocks, direction, speed, laneCount);
        segment.applyItems(items);

        segmentsById.put(segmentId, segment);
        for (BlockPos pos : blocks) {
            segmentsByPos.put(pos, segment);
        }
    }

    public void removeSegment(long segmentId) {
        ClientBeltSegment segment = segmentsById.remove(segmentId);
        if (segment == null) return;

        for (BlockPos pos : segment.blocks) {
            if (segmentsByPos.get(pos) == segment) {
                segmentsByPos.remove(pos);
            }
        }
    }

    public ClientBeltSegment getSegmentAt(BlockPos pos) {
        return segmentsByPos.get(pos);
    }

    public void tick() {
        for (ClientBeltSegment segment : segmentsById.values()) {
            segment.tick();
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