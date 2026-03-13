package com.hbm.tileentity.conductor;

import com.hbm.forgefluid.FluidTypeHandler;
import com.hbm.forgefluid.FluidTypeHandler.FluidTrait;

import net.minecraft.init.SoundEvents;
import net.minecraft.util.ITickable;
import net.minecraft.util.SoundCategory;
import net.minecraftforge.fluids.FluidStack;

public class TileEntityFFFluidDuctMk3Solid extends TileEntityFFDuctBaseMk2 implements ITickable {

    @Override
    public int getPipeTier() {
        return 3;
    }

    @Override
    public int fill(FluidStack resource, boolean doFill) {
        int filled = super.fill(resource, doFill);
        if (filled > 0 && doFill && !world.isRemote) {
            if (FluidTypeHandler.containsTrait(resource.getFluid(), FluidTrait.AMAT)) {
                world.destroyBlock(pos, false);
                world.newExplosion(null, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 4, true, true);
            } else if (FluidTypeHandler.isExtremelyHot(resource.getFluid())) {
                world.destroyBlock(pos, false);
                world.playSound(null, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, SoundEvents.BLOCK_LAVA_EXTINGUISH, SoundCategory.BLOCKS, 1.0F, 1.0F);
            }
        }
        return filled;
    }

    @Override
	public void update() {
    }

}
