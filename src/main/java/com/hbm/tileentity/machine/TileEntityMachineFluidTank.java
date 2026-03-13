package com.hbm.tileentity.machine;

import com.hbm.forgefluid.FFUtils;
import com.hbm.forgefluid.FluidTypeHandler;
import com.hbm.forgefluid.FluidTypeHandler.FluidTrait;

import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fluids.FluidTank;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public class TileEntityMachineFluidTank extends TileEntityBarrel {

	public TileEntityMachineFluidTank() {
		super(256000);
	}
	
	@Override
	public String getName() {
		return "container.fluidtank";
	}
	
	@Override
	public void checkFluidInteraction() {
		if(tank.getFluid() != null && (FluidTypeHandler.containsTrait(tank.getFluid().getFluid(), FluidTrait.AMAT) || FluidTypeHandler.is1300Hot(tank.getFluid().getFluid()) || FluidTypeHandler.isCorrosiveIron(tank.getFluid().getFluid()))) {
			world.destroyBlock(pos, false);
			world.newExplosion(null, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 5, true, true);
		}
	}

	public void fillFluid(BlockPos pos1, FluidTank tank) {
		FFUtils.fillFluid(this, tank, world, pos1, 64000);
	}

	@Override
	public void fillFluidInit(FluidTank type) {
		fillFluid(new BlockPos(this.pos.getX() + 2, this.pos.getY(), this.pos.getZ() + 1), type);
		fillFluid(new BlockPos(this.pos.getX() - 2, this.pos.getY(), this.pos.getZ() + 1), type);
		fillFluid(new BlockPos(this.pos.getX() + 2, this.pos.getY(), this.pos.getZ() - 1), type);
		fillFluid(new BlockPos(this.pos.getX() - 2, this.pos.getY(), this.pos.getZ() - 1), type);
		fillFluid(new BlockPos(this.pos.getX() + 1, this.pos.getY(), this.pos.getZ() + 2), type);
		fillFluid(new BlockPos(this.pos.getX() - 1, this.pos.getY(), this.pos.getZ() + 2), type);
		fillFluid(new BlockPos(this.pos.getX() + 1, this.pos.getY(), this.pos.getZ() - 2), type);
		fillFluid(new BlockPos(this.pos.getX() - 1, this.pos.getY(), this.pos.getZ() - 2), type);
	}
	
	AxisAlignedBB bb = null;
	
	// @Override
	// public AxisAlignedBB getRenderBoundingBox() {
		
	// 	if(bb == null) {
	// 		bb = new AxisAlignedBB(
	// 				pos.getX() - 1,
	// 				pos.getY(),
	// 				pos.getZ() - 2,
	// 				pos.getX() + 1,
	// 				pos.getY() + 2,
	// 				pos.getZ() + 2
	// 				);
	// 	}
		
	// 	return bb;
	// }

	@Override
	public AxisAlignedBB getRenderBoundingBox() {
		return TileEntity.INFINITE_EXTENT_AABB;
	}
	
	@Override
	@SideOnly(Side.CLIENT)
	public double getMaxRenderDistanceSquared() {
		return 65536.0D;
	}
}