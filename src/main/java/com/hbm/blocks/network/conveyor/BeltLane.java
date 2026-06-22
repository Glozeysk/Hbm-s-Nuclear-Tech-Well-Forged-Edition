package com.hbm.blocks.network.conveyor;

import java.util.ArrayList;
import java.util.List;

public class BeltLane {

    private static final double EPS = 1.0E-5D;
    public static final double ITEM_LENGTH = 0.25D;

    private final List<BeltItemData> items = new ArrayList<>();
    private final int maxItemsPerBlock;
    private final int segmentBlocks;

    public BeltLane(int segmentBlocks, int maxItemsPerBlock) {
        this.segmentBlocks = segmentBlocks;
        this.maxItemsPerBlock = maxItemsPerBlock;
    }

    public int size() {
        return items.size();
    }

    public BeltItemData get(int index) {
        return items.get(index);
    }

    public boolean addSorted(BeltItemData item) {
        int maxTotal = segmentBlocks * maxItemsPerBlock;
        if (items.size() >= maxTotal) return false;

        if (!isSlotFree(item.getProgress())) return false;

        insertSorted(item);
        return true;
    }

    public boolean addSortedDirect(BeltItemData item) {
        insertSorted(item);
        return true;
    }

    private void insertSorted(BeltItemData item) {
        int insertAt = items.size();
        for (int i = 0; i < items.size(); i++) {
            if (items.get(i).getProgress() <= item.getProgress()) {
                insertAt = i;
                break;
            }
        }
        items.add(insertAt, item);
    }

    public BeltItemData removeFirst() {
        if (items.isEmpty()) return null;
        return items.remove(0);
    }

    public boolean remove(BeltItemData item) {
        return items.remove(item);
    }

    public boolean removeByUid(long uid) {
        for (int i = 0; i < items.size(); i++) {
            if (items.get(i).getUniqueId() == uid) {
                items.remove(i);
                return true;
            }
        }
        return false;
    }

    public BeltItemData findByUid(long uid) {
        for (BeltItemData item : items) {
            if (item.getUniqueId() == uid) return item;
        }
        return null;
    }

    public BeltItemData findNearest(double progress) {
        BeltItemData best = null;
        double bestDist = Double.MAX_VALUE;
        for (BeltItemData item : items) {
            double dist = Math.abs(item.getProgress() - progress);
            if (dist < bestDist) {
                bestDist = dist;
                best = item;
            }
        }
        if (best != null && bestDist < ITEM_LENGTH) return best;
        return null;
    }

    public int findFirstFreeSlot(double segmentLength) {
        for (int block = 0; block < segmentBlocks; block++) {
            for (int slot = 0; slot < maxItemsPerBlock; slot++) {
                double slotProgress = block + ITEM_LENGTH * 0.5 + slot * ITEM_LENGTH;
                if (slotProgress >= segmentLength) continue;
                if (isSlotFree(slotProgress)) return block * maxItemsPerBlock + slot;
            }
        }
        return -1;
    }

    public boolean isSlotFree(double slotProgress) {
        for (BeltItemData item : items) {
            if (Math.abs(item.getProgress() - slotProgress) < ITEM_LENGTH - EPS) {
                return false;
            }
        }
        return true;
    }

    public void clear() {
        items.clear();
    }

    public List<BeltItemData> getItems() {
        return items;
    }

    public void copyTo(List<BeltItemData> out) {
        out.addAll(items);
    }
}