package com.hbm.tileentity.conductor;

import com.hbm.forgefluid.FluidTypeHandler;
import com.hbm.forgefluid.FluidTypeHandler.FluidTrait;
import com.hbm.interfaces.IFluidPipeMk2;

import net.minecraft.init.SoundEvents;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ITickable;
import net.minecraft.util.SoundCategory;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.CapabilityFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandler;

//Drillgon200: Thank Bob for making me realize I could make a new tile entity for this and not be an idiot.
public class TileEntityFFFluidSuccMk3Solid extends TileEntityFFFluidDuctMk2 implements ITickable {

    @Override
    public int getPipeTier() {
        return 3;
    }

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
            if (FluidTypeHandler.isExtremelyHot(resource.getFluid())) {
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
		if(world.isRemote || network == null || network.getType() == null)
			return;
		for(EnumFacing e : EnumFacing.VALUES){
			TileEntity te = world.getTileEntity(pos.offset(e));
			if(te != null && !(te instanceof IFluidPipeMk2) && te.hasCapability(CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY, e.getOpposite())){
				IFluidHandler toDrain = te.getCapability(CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY, e.getOpposite());
				int maxNetFill = network.fill(new FluidStack(network.getType(), Integer.MAX_VALUE), false);
				network.fill(toDrain.drain(new FluidStack(network.getType(), maxNetFill), true), true);
			}
		}
	}

}
