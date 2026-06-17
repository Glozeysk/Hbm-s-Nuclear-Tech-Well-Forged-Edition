package com.hbm.blocks.network;

import net.minecraft.block.Block;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class ConveyorLine {

    public final List<BlockPos> positions;
    public final EnumFacing direction;
    public final int length;

    private ConveyorLine(List<BlockPos> positions, EnumFacing direction) {
        this.positions = Collections.unmodifiableList(positions);
        this.direction = direction;
        this.length = positions.size();
    }

    public int getIndexOf(BlockPos pos) {
        return positions.indexOf(pos);
    }

    public int getPriority(BlockPos sideEntryPos) {
        int idx = getIndexOf(sideEntryPos);
        if (idx < 0) return Integer.MAX_VALUE;
        return length - 1 - idx;
    }

    public double getGlobalProgress(BlockPos pos, double localProgress) {
        int idx = getIndexOf(pos);
        if (idx < 0) return localProgress;
        return idx + localProgress;
    }

    public BlockPos getHead() {
        return positions.get(positions.size() - 1);
    }

    public BlockPos getTail() {
        return positions.get(0);
    }

    public static ConveyorLine build(World world, BlockPos startPos) {
        Block startBlock = world.getBlockState(startPos).getBlock();
        if (!(startBlock instanceof BlockConveyor)) {
            return new ConveyorLine(Collections.singletonList(startPos), EnumFacing.NORTH);
        }

        BlockConveyor startConveyor = (BlockConveyor) startBlock;
        EnumFacing facing = startConveyor.getLaneFacing(world, startPos);

        List<BlockPos> forward = new ArrayList<>();
        forward.add(startPos);

        BlockPos cursor = startPos.offset(facing);
        for (int i = 0; i < 256; i++) {
            Block b = world.getBlockState(cursor).getBlock();
            if (!(b instanceof BlockConveyor)) break;
            BlockConveyor c = (BlockConveyor) b;
            if (c.getLaneFacing(world, cursor) != facing) break;
            forward.add(cursor);
            cursor = cursor.offset(facing);
        }

        List<BlockPos> backward = new ArrayList<>();
        cursor = startPos.offset(facing.getOpposite());
        for (int i = 0; i < 256; i++) {
            Block b = world.getBlockState(cursor).getBlock();
            if (!(b instanceof BlockConveyor)) break;
            BlockConveyor c = (BlockConveyor) b;
            if (c.getLaneFacing(world, cursor) != facing) break;
            backward.add(cursor);
            cursor = cursor.offset(facing.getOpposite());
        }

        Collections.reverse(backward);
        List<BlockPos> full = new ArrayList<>(backward.size() + forward.size());
        full.addAll(backward);
        full.addAll(forward);

        return new ConveyorLine(full, facing);
    }

    public boolean contains(BlockPos pos) {
        return positions.contains(pos);
    }
}