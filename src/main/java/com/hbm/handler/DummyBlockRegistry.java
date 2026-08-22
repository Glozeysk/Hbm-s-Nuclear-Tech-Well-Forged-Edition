package com.hbm.handler;

import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class DummyBlockRegistry {

    private static final Map<Integer, Map<BlockPos, Set<BlockPos>>> registry =
            new ConcurrentHashMap<>();

    private static int getDim(World world) {
        return world.provider.getDimension();
    }

    public static void register(World world, BlockPos corePos, BlockPos dummyPos) {
        registry.computeIfAbsent(getDim(world), k -> new ConcurrentHashMap<>())
                .computeIfAbsent(corePos, k -> ConcurrentHashMap.newKeySet())
                .add(dummyPos);
    }

    public static void unregister(World world, BlockPos corePos, BlockPos dummyPos) {
        Map<BlockPos, Set<BlockPos>> worldMap = registry.get(getDim(world));
        if (worldMap == null) return;
        Set<BlockPos> dummies = worldMap.get(corePos);
        if (dummies != null) {
            dummies.remove(dummyPos);
            if (dummies.isEmpty()) worldMap.remove(corePos);
        }
    }

    public static Set<BlockPos> getDummiesFor(World world, BlockPos corePos) {
        Map<BlockPos, Set<BlockPos>> worldMap = registry.get(getDim(world));
        if (worldMap == null) return Collections.emptySet();
        return worldMap.getOrDefault(corePos, Collections.emptySet());
    }

    public static void removeCore(World world, BlockPos corePos) {
        Map<BlockPos, Set<BlockPos>> worldMap = registry.get(getDim(world));
        if (worldMap != null) worldMap.remove(corePos);
    }

    public static void onWorldUnload(World world) {
        registry.remove(getDim(world));
    }
}