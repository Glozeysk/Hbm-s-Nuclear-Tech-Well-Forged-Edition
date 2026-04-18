package com.hbm.blocks.machine;

import java.util.Random;

import com.hbm.blocks.BlockDummyable;
import com.hbm.blocks.ModBlocks;
import com.hbm.lib.ForgeDirection;
import com.hbm.main.MainRegistry;
import com.hbm.tileentity.TileEntityProxyCombo;
import com.hbm.tileentity.machine.TileEntityMachineCrystallizer;

import net.minecraft.block.BlockLadder;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumBlockRenderType;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class MachineCrystallizer extends BlockDummyable {

	public MachineCrystallizer(Material mat, String s) {
		super(mat, s);
	}

	@Override
	public TileEntity createNewTileEntity(World p_149915_1_, int meta) {
		if(meta >= 12)
			return new TileEntityMachineCrystallizer();

		if(meta >= 8 && meta <= 11)
			return new TileEntityProxyCombo(true, true, true);

		return null;
	}

	@Override
	public boolean onBlockActivated(World world, BlockPos pos, IBlockState state, EntityPlayer player, EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ) {
		if(world.isRemote) {
			return true;
		} else if(!player.isSneaking()) {
			int[] pos1 = this.findCore(world, pos.getX(), pos.getY(), pos.getZ());

			if(pos1 == null)
				return false;

			TileEntityMachineCrystallizer entity = (TileEntityMachineCrystallizer) world.getTileEntity(new BlockPos(pos1[0], pos1[1], pos1[2]));
			if(entity != null) {
				player.openGui(MainRegistry.instance, ModBlocks.guiID_crystallizer, world, pos1[0], pos1[1], pos1[2]);
			}
			return true;
		} else {
			return false;
		}
	}

	@Override
	public int[] getDimensions() {
		return new int[] { 5, 0, 1, 1, 1, 1 };
	}

	@Override
	public int getOffset() {
		return 1;
	}

	@Override
	protected void fillSpace(World world, int x, int y, int z, ForgeDirection dir, int o) {
		super.fillSpace(world, x, y, z, dir, o);

		this.makeExtra(world, x + dir.offsetX * o + 1, y, z + dir.offsetZ * o + 1);
		this.makeExtra(world, x + dir.offsetX * o - 1, y, z + dir.offsetZ * o + 1);
		this.makeExtra(world, x + dir.offsetX * o + 1, y, z + dir.offsetZ * o - 1);
		this.makeExtra(world, x + dir.offsetX * o - 1, y, z + dir.offsetZ * o - 1);

		placeLadderBlocks(world, x + dir.offsetX * o, y + dir.offsetY * o, z + dir.offsetZ * o, dir);
	}

	public void placeLadderBlocks(World world, int cx, int cy, int cz, ForgeDirection dir) {
		EnumFacing facing = getLadderFacing(dir);
		IBlockState ladderState = ModBlocks.crystallizer_ladder.getDefaultState().withProperty(BlockLadder.FACING, facing);

		for(BlockPos pos : getLadderPositions(cx, cy, cz, dir)) {
			IBlockState state = world.getBlockState(pos);
			if(world.isAirBlock(pos) || state.getBlock() == ModBlocks.crystallizer_ladder) {
				world.setBlockState(pos, ladderState, 2);
			}
		}
	}

	private void removeLadderBlocks(World world, int cx, int cy, int cz, ForgeDirection dir) {
		for(BlockPos pos : getLadderPositions(cx, cy, cz, dir)) {
			if(world.getBlockState(pos).getBlock() == ModBlocks.crystallizer_ladder) {
				world.setBlockToAir(pos);
			}
		}
	}

	private EnumFacing getLadderFacing(ForgeDirection dir) {
		switch(dir) {
			case NORTH:
				return EnumFacing.EAST;
			case SOUTH:
				return EnumFacing.WEST;
			case EAST:
				return EnumFacing.SOUTH;
			case WEST:
				return EnumFacing.NORTH;
			default:
				return EnumFacing.NORTH;
		}
	}

	private BlockPos[] getLadderPositions(int cx, int cy, int cz, ForgeDirection dir) {
		switch(dir) {
			case NORTH:
				return new BlockPos[] {
						new BlockPos(cx + 2, cy, cz),
						new BlockPos(cx + 2, cy + 1, cz),
						new BlockPos(cx + 2, cy + 2, cz),
						new BlockPos(cx + 2, cy + 3, cz),
						new BlockPos(cx + 2, cy + 4, cz),
						new BlockPos(cx + 2, cy + 5, cz)
				};
			case SOUTH:
				return new BlockPos[] {
						new BlockPos(cx - 2, cy, cz),
						new BlockPos(cx - 2, cy + 1, cz),
						new BlockPos(cx - 2, cy + 2, cz),
						new BlockPos(cx - 2, cy + 3, cz),
						new BlockPos(cx - 2, cy + 4, cz),
						new BlockPos(cx - 2, cy + 5, cz)
				};
			case EAST:
				return new BlockPos[] {
						new BlockPos(cx, cy, cz + 2),
						new BlockPos(cx, cy + 1, cz + 2),
						new BlockPos(cx, cy + 2, cz + 2),
						new BlockPos(cx, cy + 3, cz + 2),
						new BlockPos(cx, cy + 4, cz + 2),
						new BlockPos(cx, cy + 5, cz + 2)
				};
			case WEST:
				return new BlockPos[] {
						new BlockPos(cx, cy, cz - 2),
						new BlockPos(cx, cy + 1, cz - 2),
						new BlockPos(cx, cy + 2, cz - 2),
						new BlockPos(cx, cy + 3, cz - 2),
						new BlockPos(cx, cy + 4, cz - 2),
						new BlockPos(cx, cy + 5, cz - 2)
				};
			default:
				return new BlockPos[0];
		}
	}

	@Override
	public void updateTick(World world, BlockPos pos, IBlockState state, Random rand) {
		if(!world.isRemote) {
			int[] core;
			int meta = state.getValue(META);

			if(meta >= 12) {
				core = new int[] {pos.getX(), pos.getY(), pos.getZ()};
			} else {
				core = this.findCore(world, pos.getX(), pos.getY(), pos.getZ());
			}

			if(core == null)
				return;

			BlockPos corePos = new BlockPos(core[0], core[1], core[2]);
			IBlockState coreState = world.getBlockState(corePos);

			if(coreState.getBlock() != this)
				return;

			int coreMeta = coreState.getValue(META);
			if(coreMeta < 12)
				return;

			ForgeDirection dir = ForgeDirection.getOrientation(coreMeta - offset);
			placeLadderBlocks(world, corePos.getX(), corePos.getY(), corePos.getZ(), dir);
		}
	}

	@Override
	public void breakBlock(World world, BlockPos pos, IBlockState state) {
		if(!world.isRemote) {
			int[] core = this.findCore(world, pos.getX(), pos.getY(), pos.getZ());

			if(core != null) {
				BlockPos corePos = new BlockPos(core[0], core[1], core[2]);
				IBlockState coreState = world.getBlockState(corePos);

				if(coreState.getBlock() == this) {
					int coreMeta = coreState.getValue(META);
					if(coreMeta >= 12) {
						ForgeDirection dir = ForgeDirection.getOrientation(coreMeta - offset);
						removeLadderBlocks(world, corePos.getX(), corePos.getY(), corePos.getZ(), dir);
					}
				}
			} else {
				int meta = state.getValue(META);
				if(meta >= 12) {
					ForgeDirection dir = ForgeDirection.getOrientation(meta - offset);
					removeLadderBlocks(world, pos.getX(), pos.getY(), pos.getZ(), dir);
				}
			}
		}

		super.breakBlock(world, pos, state);
	}

	@Override
	public EnumBlockRenderType getRenderType(IBlockState state) {
		return EnumBlockRenderType.ENTITYBLOCK_ANIMATED;
	}
}