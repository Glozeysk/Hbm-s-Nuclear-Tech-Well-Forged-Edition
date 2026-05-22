package com.hbm.tileentity.machine;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.hbm.blocks.BlockDummyable;
import com.hbm.blocks.ModBlocks;
import com.hbm.blocks.machine.MachineFluidTank;
import com.hbm.forgefluid.FFUtils;
import com.hbm.forgefluid.FluidTypeHandler;
import com.hbm.forgefluid.FluidTypeHandler.FluidTrait;
import com.hbm.lib.ForgeDirection;

import net.minecraft.block.BlockLadder;
import net.minecraft.block.state.IBlockState;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fluids.FluidTank;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public class TileEntityMachineFluidTank extends TileEntityBarrel {

	private List<BlockPos> ladderPositions = new ArrayList<>();
	private EnumFacing ladderFacing = EnumFacing.NORTH;
	private boolean pendingLadderRestore = false;
	private int ladderRestoreCooldown = 0;

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

	@Override
	public void update() {
		super.update();

		if(pendingLadderRestore) {
			if(ladderRestoreCooldown > 0) {
				ladderRestoreCooldown--;
				return;
			}

			boolean complete = restoreLadders();

			if(complete) {
				pendingLadderRestore = false;
			} else {
				ladderRestoreCooldown = 20;
			}
		}
	}

	private void bootstrapLadderData() {
		if(world == null)
			return;
		if(!ladderPositions.isEmpty())
			return;

		IBlockState state = world.getBlockState(pos);

		if(state.getBlock() != ModBlocks.machine_fluidtank)
			return;

		int meta = state.getValue(BlockDummyable.META);

		if(meta < 12)
			return;

		ForgeDirection dir = ForgeDirection.getOrientation(meta - BlockDummyable.offset);
		MachineFluidTank block = (MachineFluidTank) state.getBlock();

		this.ladderPositions = new ArrayList<>(Arrays.asList(
				block.getSavedLadderPositions(pos.getX(), pos.getY(), pos.getZ(), dir)
		));
		this.ladderFacing = block.getSavedLadderFacing(dir);

		if(!world.isRemote) {
			this.markDirty();
		}
	}

	public void setLadderData(List<BlockPos> positions, EnumFacing facing) {
		this.ladderPositions = new ArrayList<>(positions);
		this.ladderFacing = facing;
		this.pendingLadderRestore = false;
		this.ladderRestoreCooldown = 0;
		this.markDirty();
	}

	public boolean restoreLadders() {
		if(world == null)
			return true;

		if(ladderPositions.isEmpty()) {
			bootstrapLadderData();
		}

		if(ladderPositions.isEmpty())
			return true;

		boolean complete = true;

		IBlockState ladderState = ModBlocks.machine_fluidtank_ladder.getDefaultState()
				.withProperty(BlockLadder.FACING, ladderFacing);

		for(BlockPos lpos : ladderPositions) {
			if(!world.isBlockLoaded(lpos)) {
				complete = false;
				continue;
			}

			IBlockState state = world.getBlockState(lpos);
			if(world.isAirBlock(lpos) || state.getBlock() == ModBlocks.machine_fluidtank_ladder) {
				world.setBlockState(lpos, ladderState, 3);
			}
		}

		return complete;
	}

	public void removeLadders() {
		if(world == null || world.isRemote)
			return;

		if(ladderPositions.isEmpty()) {
			bootstrapLadderData();
		}

		if(ladderPositions.isEmpty())
			return;

		for(BlockPos lpos : ladderPositions) {
			if(!world.isBlockLoaded(lpos))
				continue;

			if(world.getBlockState(lpos).getBlock() == ModBlocks.machine_fluidtank_ladder) {
				world.setBlockToAir(lpos);
			}
		}

		ladderPositions.clear();
		pendingLadderRestore = false;
		ladderRestoreCooldown = 0;
		markDirty();
	}

	@Override
	public void validate() {
		super.validate();
		pendingLadderRestore = true;
		ladderRestoreCooldown = 5;
	}

	@Override
	public void readFromNBT(NBTTagCompound compound) {
		super.readFromNBT(compound);

		ladderPositions.clear();
		if(compound.hasKey("ladderPositions")) {
			NBTTagList list = compound.getTagList("ladderPositions", 10);
			for(int i = 0; i < list.tagCount(); i++) {
				NBTTagCompound tag = list.getCompoundTagAt(i);
				ladderPositions.add(new BlockPos(tag.getInteger("x"), tag.getInteger("y"), tag.getInteger("z")));
			}
		}
		if(compound.hasKey("ladderFacing")) {
			ladderFacing = EnumFacing.byIndex(compound.getInteger("ladderFacing"));
		}
	}

	@Override
	public NBTTagCompound writeToNBT(NBTTagCompound compound) {
		NBTTagList list = new NBTTagList();

		for(BlockPos lpos : ladderPositions) {
			NBTTagCompound tag = new NBTTagCompound();
			tag.setInteger("x", lpos.getX());
			tag.setInteger("y", lpos.getY());
			tag.setInteger("z", lpos.getZ());
			list.appendTag(tag);
		}

		compound.setTag("ladderPositions", list);
		compound.setInteger("ladderFacing", ladderFacing.getIndex());

		return super.writeToNBT(compound);
	}

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