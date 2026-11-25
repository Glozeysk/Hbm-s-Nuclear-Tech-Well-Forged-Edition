package com.hbm.world.generator;

import com.hbm.blocks.ModBlocks;
import com.hbm.world.generator.room.VaultDungeonRoom;
import net.minecraft.world.World;

import java.util.Random;

public class VaultDungeon extends CellularDungeon {

    public boolean hasElevator = false;
    public int eX, eZ;
    public VaultDungeonRoom elevatorRoom;

    public VaultDungeon(int width, int height, int dimX, int dimZ, int tries, int branches) {
		super(width, height, dimX, dimZ, tries, branches);

		this.floor.add(ModBlocks.brick_compound.getDefaultState());
		this.wall.add(ModBlocks.concrete_smooth.getDefaultState());
		this.ceiling.add(ModBlocks.ducrete.getDefaultState());
	}

    @Override
    public void compose(Random random) {
        super.compose(random);
        // Even a tiny layout must have a room capable of hosting its surface entrance.
        boolean entranceRoom = false;
        for(CellularDungeonRoom[] row : cells) for(CellularDungeonRoom room : row)
            entranceRoom |= room instanceof com.hbm.world.generator.room.VaultDungeonRoomElevator;
        if(!entranceRoom) cells[dimX / 2][dimZ / 2] = rooms.get(0);
    }
    @Override
    public void generate(World world, int x, int y, int z, Random random) {
        super.generate(world, x, y, z, random);
        if(world.isRemote) return;
        TimedGenerator.addOp(world, () -> {
            // Bedrock has no HBM block callbacks. Refresh already cached radiation pockets
            // after the queued walls and entrance have been placed, without loading new chunks.
            int radius = Math.max(dimX, dimZ) * width / 2 + 32;
            for(int cx = (x - radius) >> 4; cx <= (x + radius) >> 4; cx++)
                for(int cz = (z - radius) >> 4; cz <= (z + radius) >> 4; cz++)
                    for(int cy = 0; cy < 16; cy++) {
                        net.minecraft.util.math.BlockPos pos = new net.minecraft.util.math.BlockPos(cx << 4, cy << 4, cz << 4);
                        if(com.hbm.handler.RadiationSystemNT.isSubChunkLoaded(world, pos))
                            com.hbm.handler.RadiationSystemNT.markChunkForRebuild(world, pos);
                    }
        });
    }
}
