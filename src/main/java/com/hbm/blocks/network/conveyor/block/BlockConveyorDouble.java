package com.hbm.blocks.network.conveyor.block;

import api.hbm.block.IConveyorVectorProvider;
import com.hbm.blocks.network.conveyor.ConveyorEntryPoints;
import net.minecraft.block.material.Material;

public class BlockConveyorDouble extends BlockConveyor {

    public BlockConveyorDouble(Material materialIn, String s) {
        super(materialIn, s, () -> new double[]{-0.25D, 0.25D},
                IConveyorVectorProvider.linear(),
                BlockConveyor::selectNearestLane,
                createDoubleEntryPoints());
    }

    private static ConveyorEntryPoints createDoubleEntryPoints() {
        double[][][] leftPoints = {
                {{2, 2}, {6, 2}, {8, 5}},
                {{1, 1}, {7, 1}, {8, 6}}
        };

        double[][][] rightPoints = {
                {{15, 1}, {9, 1}, {8, 6}},
                {{14, 2}, {10, 2}, {8, 5}}
        };

        return new ConveyorEntryPoints(leftPoints, rightPoints, 0.5D);
    }
}