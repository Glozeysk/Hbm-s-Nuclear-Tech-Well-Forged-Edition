package com.hbm.blocks.network.conveyor;

import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ClientBeltSegment {

    public final long segmentId;
    public final List<BlockPos> blocks;
    public final EnumFacing direction;
    public final double speed;
    public final int laneCount;
    public final boolean isVertical;
    public final boolean isUpward;

    private final Map<Long, ClientBeltItem> itemMap = new HashMap<>();
    private final List<ClientBeltItem>[] lanes;

    @SuppressWarnings("unchecked")
    public ClientBeltSegment(long segmentId, List<BlockPos> blocks, EnumFacing direction, double speed, int laneCount) {
        this(segmentId, blocks, direction, speed, laneCount, false, false);
    }

    @SuppressWarnings("unchecked")
    public ClientBeltSegment(long segmentId, List<BlockPos> blocks, EnumFacing direction, double speed, int laneCount, boolean isVertical, boolean isUpward) {
        this.segmentId = segmentId;
        this.blocks = new ArrayList<>(blocks);
        this.direction = direction;
        this.speed = speed;
        this.laneCount = Math.max(1, laneCount);
        this.isVertical = isVertical;
        this.isUpward = isUpward;
        this.lanes = new List[this.laneCount];
        for (int i = 0; i < this.laneCount; i++) {
            lanes[i] = new ArrayList<>();
        }
    }

    public double getMaxProgress() {
        return blocks.size();
    }

    public boolean containsPos(BlockPos pos) {
        return blocks.contains(pos);
    }

    public void applyItems(BeltItemData[] serverItems) {
        Map<Long, BeltItemData> serverMap = new HashMap<>();
        for (BeltItemData si : serverItems) {
            serverMap.put(si.getUniqueId(), si);
        }

        List<Long> toRemove = new ArrayList<>();
        for (Long uid : itemMap.keySet()) {
            if (!serverMap.containsKey(uid)) {
                toRemove.add(uid);
            }
        }

        for (Long uid : toRemove) {
            ClientBeltItem removed = itemMap.remove(uid);
            if (removed != null) {
                int lane = removed.lane;
                if (lane >= 0 && lane < laneCount) {
                    lanes[lane].remove(removed);
                }
            }
        }

        for (BeltItemData si : serverItems) {
            int newLane = si.getLane();
            if (newLane < 0 || newLane >= laneCount) {
                newLane = 0;
            }

            ClientBeltItem existing = itemMap.get(si.getUniqueId());
            if (existing != null) {
                int oldLane = existing.lane;
                existing.updateFromServer(
                        si.getProgress(),
                        si.isStopped(),
                        si.getStack(),
                        newLane,
                        si.getRouteType()
                );
                if (oldLane != newLane) {
                    if (oldLane >= 0 && oldLane < laneCount) {
                        lanes[oldLane].remove(existing);
                    }
                    if (newLane >= 0 && newLane < laneCount && !lanes[newLane].contains(existing)) {
                        lanes[newLane].add(existing);
                    }
                }
            } else {
                ClientBeltItem newItem = new ClientBeltItem(
                        si.getUniqueId(),
                        si.getStack(),
                        newLane,
                        si.getProgress(),
                        si.isStopped(),
                        si.getRouteType()
                );
                itemMap.put(si.getUniqueId(), newItem);
                if (newLane >= 0 && newLane < laneCount) {
                    lanes[newLane].add(newItem);
                }
            }
        }
    }

    public void tick() {
        double maxProg = getMaxProgress();

        for (int l = 0; l < laneCount; l++) {
            List<ClientBeltItem> laneItems = lanes[l];
            laneItems.sort((a, b) -> Double.compare(b.renderProgress, a.renderProgress));

            for (int i = 0; i < laneItems.size(); i++) {
                ClientBeltItem item = laneItems.get(i);

                double limit;
                if (i == 0) {
                    limit = maxProg - BeltLane.ITEM_LENGTH * 0.5;
                } else {
                    ClientBeltItem ahead = laneItems.get(i - 1);
                    limit = ahead.renderProgress - BeltLane.ITEM_LENGTH;
                }

                item.tick(speed, limit);
            }
        }
    }

    public List<ClientBeltItem> getAllItems() {
        return new ArrayList<>(itemMap.values());
    }
}