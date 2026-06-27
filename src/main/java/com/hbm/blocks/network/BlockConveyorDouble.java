package com.hbm.blocks.network;

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
        double[][][] rightPoints = {
                {{14, 2}, {10, 2}, {8, 5}},
                {{14, 6}, {10, 6}, {8, 9}}
        };
        double[][][] leftPoints = {
                {{2, 2}, {6, 2}, {8, 5}},
                {{2, 6}, {6, 6}, {8, 9}}
        };
        return new ConveyorEntryPoints(leftPoints, rightPoints, 0.5D);
    }
}