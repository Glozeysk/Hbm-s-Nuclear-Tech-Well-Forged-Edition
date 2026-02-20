package com.hbm.tileentity.conductor;

import com.hbm.forgefluid.FluidTypeHandler;
import com.hbm.forgefluid.FluidTypeHandler.FluidTrait;

import net.minecraft.init.SoundEvents;
import net.minecraft.util.ITickable;
import net.minecraft.util.SoundCategory;
import net.minecraftforge.fluids.FluidStack;

public class TileEntityFFFluidDuctMk2Solid extends TileEntityFFDuctBaseMk2 implements ITickable {

    @Override
    public int fill(FluidStack resource, boolean doFill) {
        if (resource != null && resource.getFluid() != null && !world.isRemote) {
            if (FluidTypeHandler.containsTrait(resource.getFluid(), FluidTrait.AMAT)) {
                if (doFill) {
                    world.destroyBlock(pos, false);
                    world.newExplosion(null, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 4, true, true);
                }
                return resource.amount;
            }
            if (FluidTypeHandler.isReallyHot(resource.getFluid()) || FluidTypeHandler.isCorrosivePlastic(resource.getFluid())) {
                if (doFill) {
                    world.destroyBlock(pos, false);
                    world.playSound(null, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, SoundEvents.BLOCK_LAVA_EXTINGUISH, SoundCategory.BLOCKS, 1.0F, 1.0F);
                }
                return resource.amount;
            }
        }
        return super.fill(resource, doFill);
    }

    @Override
	public void update() {
    }

}
