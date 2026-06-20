package com.hbm.render.conveyor;

import net.minecraft.item.ItemStack;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ConveyorVisualManager {

    private static final ConveyorVisualManager INSTANCE = new ConveyorVisualManager();
    private static final int TIMEOUT_TICKS = 10;
    private static final int REMOVAL_GRACE_TICKS = 1;

    private final Map<Long, VisualItem> items = new ConcurrentHashMap<>();
    private int currentTick = 0;

    public static ConveyorVisualManager get() {
        return INSTANCE;
    }

    public void updateItem(long uid, ItemStack stack, double worldX, double worldY, double worldZ,
                           float yaw, double speed, boolean stopped) {
        VisualItem existing = items.get(uid);
        if (existing != null) {
            existing.updateTarget(worldX, worldY, worldZ, yaw, stack, speed, stopped, currentTick);
            existing.removalTick = -1;
        } else {
            items.put(uid, new VisualItem(uid, stack, worldX, worldY, worldZ, yaw, speed, stopped, currentTick));
        }
    }

    public void markForRemoval(long uid) {
        VisualItem item = items.get(uid);
        if (item != null && item.removalTick < 0) {
            item.removalTick = currentTick;
        }
    }

    public void removeItem(long uid) {
        items.remove(uid);
    }

    public void tick() {
        currentTick++;

        Iterator<Map.Entry<Long, VisualItem>> it = items.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<Long, VisualItem> entry = it.next();
            VisualItem item = entry.getValue();

            item.tick();

            if (item.removalTick >= 0 && currentTick - item.removalTick > REMOVAL_GRACE_TICKS) {
                it.remove();
                continue;
            }

            if (currentTick - item.lastUpdateTick > TIMEOUT_TICKS) {
                it.remove();
            }
        }
    }

    public List<VisualItem> getVisibleItems() {
        return new ArrayList<>(items.values());
    }

    public void clear() {
        items.clear();
        currentTick = 0;
    }

    public int getCurrentTick() {
        return currentTick;
    }
}