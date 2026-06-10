package com.hbm.tileentity.conductor;

import com.hbm.forgefluid.FluidTypeHandler;
import com.hbm.forgefluid.FluidTypeHandler.FluidTrait;
import net.minecraft.init.SoundEvents;
import net.minecraft.util.ITickable;
import net.minecraft.util.SoundCategory;
import net.minecraftforge.fluids.FluidStack;

public class TileEntityFFFluidDuctMk2 extends TileEntityFFDuctBaseMk2 implements ITickable {

    @Override
    protected boolean checkFluidCorrosion(FluidStack resource) {
        if (resource == null || resource.getFluid() == null) return false;
        return FluidTypeHandler.containsTrait(resource.getFluid(), FluidTrait.AMAT) ||
                FluidTypeHandler.isReallyHot(resource.getFluid()) ||
                FluidTypeHandler.isCorrosiveIron(resource.getFluid());
    }

    @Override
    protected void destroyPipe() {
        if (FluidTypeHandler.containsTrait(world.getTileEntity(pos) instanceof TileEntityFFFluidDuctMk2 ?
                ((TileEntityFFFluidDuctMk2)world.getTileEntity(pos)).getType() : null, FluidTrait.AMAT)) {
            world.destroyBlock(pos, false);
            world.newExplosion(null, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 4, true, true);
        } else {
            world.destroyBlock(pos, false);
            world.playSound(null, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                    SoundEvents.BLOCK_LAVA_EXTINGUISH, SoundCategory.BLOCKS, 1.0F, 1.0F);
        }
    }
}