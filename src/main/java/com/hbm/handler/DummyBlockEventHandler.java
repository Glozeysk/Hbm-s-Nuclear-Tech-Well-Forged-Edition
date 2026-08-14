package com.hbm.handler;

import com.hbm.blocks.machine.dummy.DummyBlockBase;
import com.hbm.event.DummyBlockEvent;
import com.hbm.tileentity.TileEntityMachineBase;
import com.hbm.tileentity.machine.TileEntityDummy;
import net.minecraft.block.Block;
import net.minecraft.block.BlockContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.event.world.ChunkEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.util.ArrayList;
import java.util.List;
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

        // Обработка кор-блоков дамми-машин (TileEntityMachineBase)
        if (block instanceof BlockContainer) {
            TileEntity te = world.getTileEntity(pos);
            if (te instanceof TileEntityMachineBase) {
                TileEntityMachineBase machine = (TileEntityMachineBase) te;
                if (!machine.dummyBlocks.isEmpty()) {
                    processingEvent.set(true);
                    try {
                        List<BlockPos> toRemove = new ArrayList<>(machine.dummyBlocks);
                        machine.dummyBlocks.clear();

                        DummyBlockBase.safeBreak = true;
                        DummyBlockBase.batchRemovalMode = true;
                        try {
                            for (BlockPos dummyPos : toRemove) {
                                if (!world.isAirBlock(dummyPos)) {
                                    DummyBlockBase.destroyQuietly(world, dummyPos, false);
                                }
                            }
                        } finally {
                            DummyBlockBase.safeBreak = false;
                            DummyBlockBase.batchRemovalMode = false;
                        }
                    } finally {
                        processingEvent.set(false);
                    }
                }
            }
        }

        // Обработка ломания дамми-блока (DummyBlockBase)
        if (block instanceof DummyBlockBase) {
            TileEntity te = world.getTileEntity(pos);
            if (te instanceof TileEntityDummy) {
                TileEntityDummy dummy = (TileEntityDummy) te;
                if (dummy.target != null) {
                    processingEvent.set(true);
                    try {
                        destroyLinkedDummies(world, dummy.target);

                        if (isCreative) {
                            world.setBlockToAir(dummy.target);
                        } else {
                            world.destroyBlock(dummy.target, true);
                        }
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
            if (te instanceof TileEntityDummy) {
                TileEntityDummy dummy = (TileEntityDummy) te;
                if (dummy.target != null) {
                    DummyBlockRegistry.register(world, dummy.target, dummy.getPos());
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
            List<BlockPos> toRemove = new ArrayList<>();

            Set<BlockPos> fromRegistry = DummyBlockRegistry.getDummiesFor(world, corePos);
            if (!fromRegistry.isEmpty()) {
                toRemove.addAll(fromRegistry);
            }

            if (toRemove.isEmpty()) {
                for (TileEntity te : world.loadedTileEntityList) {
                    if (te instanceof TileEntityDummy) {
                        TileEntityDummy dummy = (TileEntityDummy) te;
                        if (corePos.equals(dummy.target) && !dummy.getPos().equals(corePos)) {
                            toRemove.add(dummy.getPos());
                        }
                    }
                }
            }

            for (BlockPos dummyPos : toRemove) {
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

            if (isCreative) {
                event.world.setBlockToAir(event.corePos);
            } else {
                event.world.destroyBlock(event.corePos, true);
            }
        } finally {
            processingEvent.set(false);
        }
    }
}