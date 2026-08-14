package com.hbm.event;

import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.eventhandler.Event;

public class DummyBlockEvent extends Event {

    public final World world;
    public final BlockPos pos;

    public DummyBlockEvent(World world, BlockPos pos) {
        this.world = world;
        this.pos = pos;
    }

    public static class DummyBroken extends DummyBlockEvent {
        public final BlockPos corePos;

        public DummyBroken(World world, BlockPos dummyPos, BlockPos corePos) {
            super(world, dummyPos);
            this.corePos = corePos;
        }
    }

    public static class CoreBroken extends DummyBlockEvent {
        public CoreBroken(World world, BlockPos corePos) {
            super(world, corePos);
        }
    }
}