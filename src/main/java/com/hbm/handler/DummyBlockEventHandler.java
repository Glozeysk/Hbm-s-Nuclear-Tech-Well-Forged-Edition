package com.hbm.handler;

import com.hbm.blocks.machine.dummy.DummyBlockBase;
import com.hbm.event.DummyBlockEvent;
import com.hbm.interfaces.IMultiBlock;
import com.hbm.tileentity.TileEntityMachineBase;
import com.hbm.tileentity.machine.dummy.TileEntityDummy;
import net.minecraft.block.Block;
import net.minecraft.block.BlockContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.event.world.ChunkEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.util.HashSet;
import java.util.Set;

@Mod.EventBusSubscriber
public class DummyBlockEventHandler {

    private static final ThreadLocal<Boolean> processingEvent = ThreadLocal.withInitial(() -> false);

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        World world = event.getWorld();
        BlockPos pos = event.getPos();
        EntityPlayer player = event.getPlayer();

        if (world.isRemote) return;
        if (processingEvent.get()) return;

        IBlockState state = world.getBlockState(pos);
        Block block = state.getBlock();
        boolean isCreative = player != null && player.isCreative();

        if (block instanceof BlockContainer) {
            TileEntity te = world.getTileEntity(pos);

            if (te instanceof TileEntityMachineBase || block instanceof IMultiBlock) {
                processingEvent.set(true);
                try {
                    destroyLinkedDummies(world, pos);
                } finally {
                    processingEvent.set(false);
                }
            }
        }

        if (block instanceof DummyBlockBase) {
            TileEntity te = world.getTileEntity(pos);
            if (te instanceof TileEntityDummy dummy) {
                if (dummy.getTarget() != null) {
                    processingEvent.set(true);
                    try {
                        destroyLinkedDummies(world, dummy.getTarget());

                        world.destroyBlock(dummy.getTarget(), !isCreative);
                        event.setCanceled(true);
                    } finally {
                        processingEvent.set(false);
                    }
                }
            }
        }
    }

    @SubscribeEvent
    public static void onChunkLoad(ChunkEvent.Load event) {
        World world = event.getWorld();
        if (world.isRemote) return;

        for (TileEntity te : event.getChunk().getTileEntityMap().values()) {
            if (te instanceof TileEntityDummy dummy) {
                if (dummy.getTarget() != null) {
                    DummyBlockRegistry.register(world, dummy.getTarget(), dummy.getPos());
                }
            }
        }
    }

    @SubscribeEvent
    public static void onWorldUnload(WorldEvent.Unload event) {
        DummyBlockRegistry.onWorldUnload(event.getWorld());
    }
    private static void destroyLinkedDummies(World world, BlockPos corePos) {
        DummyBlockBase.safeBreak = true;
        DummyBlockBase.batchRemovalMode = true;

        try {
            Set<BlockPos> toRemoveSet = new HashSet<>();

            Set<BlockPos> fromRegistry = DummyBlockRegistry.getDummiesFor(world, corePos);
            if (fromRegistry != null && !fromRegistry.isEmpty()) {
                toRemoveSet.addAll(fromRegistry);
            }

            for (BlockPos dummyPos : toRemoveSet) {
                if (!world.isAirBlock(dummyPos)) {
                    DummyBlockBase.destroyQuietly(world, dummyPos, false);
                }
            }

            DummyBlockRegistry.removeCore(world, corePos);
        } finally {
            DummyBlockBase.safeBreak = false;
            DummyBlockBase.batchRemovalMode = false;
        }
    }

    @SubscribeEvent
    public static void onDummyBroken(DummyBlockEvent.DummyBroken event) {
        if (event.world.isRemote) return;
        if (event.corePos == null) return;
        if (processingEvent.get()) return;

        processingEvent.set(true);
        try {
            destroyLinkedDummies(event.world, event.corePos);

            boolean isCreative = false;
            for (EntityPlayer p : event.world.playerEntities) {
                if (p.getDistanceSq(event.pos) < 64 && p.isCreative()) {
                    isCreative = true;
                    break;
                }
            }

            event.world.destroyBlock(event.corePos, !isCreative);
        } finally {
            processingEvent.set(false);
        }
    }

    public static void destroyLinkedDummiesSafe(World world, BlockPos corePos) {
        if (world == null || corePos == null || world.isRemote) {
            return;
        }
        if (processingEvent.get()) {
            return;
        }

        processingEvent.set(true);
        try {
            destroyLinkedDummies(world, corePos);
        } finally {
            processingEvent.set(false);
        }
    }

    public static boolean isProcessing() {
        return processingEvent.get();
    }

}