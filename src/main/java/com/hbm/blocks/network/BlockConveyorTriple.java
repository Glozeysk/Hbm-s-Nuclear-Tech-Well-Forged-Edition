package com.hbm.blocks.network;

import api.hbm.block.IConveyorVectorProvider;
import com.hbm.blocks.network.conveyor.ConveyorEntryPoints;
import net.minecraft.block.material.Material;

public class BlockConveyorTriple extends BlockConveyor {

    public BlockConveyorTriple(Material materialIn, String s) {
        super(materialIn, s, () -> new double[]{-0.3125D, 0.0D, 0.3125D},
                IConveyorVectorProvider.linear(),
                BlockConveyor::selectNearestLane,
                createTripleEntryPoints());
    }

    private static ConveyorEntryPoints createTripleEntryPoints() {
        double[][][] rightPoints = {
                {{14, 1}, {10, 1}, {8, 4}},
                {{14, 8}, {10, 8}, {8, 8}},
                {{14, 15}, {10, 15}, {8, 12}}
        };
        double[][][] leftPoints = {
                {{2, 1}, {6, 1}, {8, 4}},
                {{2, 8}, {6, 8}, {8, 8}},
                {{2, 15}, {6, 15}, {8, 12}}
        };
        return new ConveyorEntryPoints(leftPoints, rightPoints, 0.5D);
    }
}