package com.hbm.tileentity.conductor;

import com.hbm.interfaces.IFluidPipeMk2;

import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.CapabilityFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandler;

public class TileEntityFFFluidSuccMk4 extends TileEntityFFFluidDuctMk4 {

    @Override
    public int getPipeTier() {
        return 4;
    }

    @Override
    public void update() {
        super.update();
        if(world.isRemote || network == null || network.getType() == null)
            return;
        for(EnumFacing e : EnumFacing.VALUES) {
            TileEntity te = world.getTileEntity(pos.offset(e));
            if(te != null && !(te instanceof IFluidPipeMk2) && te.hasCapability(CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY, e.getOpposite())) {
                IFluidHandler toDrain = te.getCapability(CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY, e.getOpposite());
                int maxNetFill = network.fill(new FluidStack(network.getType(), Integer.MAX_VALUE), false);
                if(maxNetFill <= 0) continue;
                FluidStack drained = toDrain.drain(new FluidStack(network.getType(), maxNetFill), true);
                if(drained != null && drained.amount > 0) {
                    network.fill(drained, true);
                }
            }
        }
    }
}