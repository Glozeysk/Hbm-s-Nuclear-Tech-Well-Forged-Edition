package com.hbm.blocks.machine;

import java.util.List;
import java.util.Random;

import com.hbm.blocks.BlockDummyable;
import com.hbm.blocks.ModBlocks;
import com.hbm.handler.MultiblockHandlerXR;
import com.hbm.lib.ForgeDirection;
import com.hbm.main.MainRegistry;
import com.hbm.tileentity.TileEntityProxyCombo;
import com.hbm.tileentity.machine.TileEntityMachineFluidTank;
import com.hbm.util.I18nUtil;

import net.minecraft.block.BlockLadder;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.network.internal.FMLNetworkHandler;

public class MachineFluidTank extends BlockDummyable {

	public MachineFluidTank(Material mat, String s) {
		super(mat, s);
	}

	@Override
	public TileEntity createNewTileEntity(World world, int meta) {
		if(meta >= 12) return new TileEntityMachineFluidTank();
		if(meta >= 6) return new TileEntityProxyCombo(false, false, true);
		return null;
	}

	@Override
	public int[] getDimensions() {
		return new int[] {2, 0, 1, 1, 2, 2};
	}

	@Override
	public int getOffset() {
		return 1;
	}

	@Override
	public void addInformation(ItemStack stack, World player, List<String> list, ITooltipFlag advanced) {
		list.add(TextFormatting.RED + I18nUtil.resolveKey("desc.cannot1300hot"));
		list.add(TextFormatting.GREEN + I18nUtil.resolveKey("desc.cancor"));
		list.add(TextFormatting.YELLOW + I18nUtil.resolveKey("desc.cannothighcor"));
		list.add(TextFormatting.YELLOW + I18nUtil.resolveKey("desc.cannotam"));
	}

	@Override
	public void fillSpace(World world, int x, int y, int z, ForgeDirection dir, int o) {
		super.fillSpace(world, x, y, z, dir, o);
		MultiblockHandlerXR.fillSpace(world, x + dir.offsetX * o, y + dir.offsetY * o, z + dir.offsetZ * o, new int[] {2, 0, 1, 1, 2, -2}, this, dir);
		MultiblockHandlerXR.fillSpace(world, x + dir.offsetX * o, y + dir.offsetY * o, z + dir.offsetZ * o, new int[] {2, 0, 1, 1, -2, 2}, this, dir);

		this.makeExtra(world, x + dir.offsetX * o + 1, y, z + dir.offsetZ * o + 1);
		this.makeExtra(world, x + dir.offsetX * o - 1, y, z + dir.offsetZ * o + 1);
		this.makeExtra(world, x + dir.offsetX * o + 1, y, z + dir.offsetZ * o - 1);
		this.makeExtra(world, x + dir.offsetX * o - 1, y, z + dir.offsetZ * o - 1);

		placeLadderBlocks(world, x + dir.offsetX * o, y + dir.offsetY * o, z + dir.offsetZ * o, dir);
	}

	public void placeLadderBlocks(World world, int cx, int cy, int cz, ForgeDirection dir) {
		EnumFacing facing = getLadderFacing(dir);
		IBlockState ladderState = ModBlocks.machine_fluidtank_ladder.getDefaultState().withProperty(BlockLadder.FACING, facing);

		for(BlockPos pos : getLadderPositions(cx, cy, cz, dir)) {
			IBlockState state = world.getBlockState(pos);
			if(world.isAirBlock(pos) || state.getBlock() == ModBlocks.machine_fluidtank_ladder) {
				world.setBlockState(pos, ladderState, 2);
			}
		}
	}

	private void removeLadderBlocks(World world, int cx, int cy, int cz, ForgeDirection dir) {
		for(BlockPos pos : getLadderPositions(cx, cy, cz, dir)) {
			if(world.getBlockState(pos).getBlock() == ModBlocks.machine_fluidtank_ladder) {
				world.setBlockToAir(pos);
			}
		}
	}

	private EnumFacing getLadderFacing(ForgeDirection dir) {
		switch(dir) {
			case NORTH:
				return EnumFacing.WEST;
			case SOUTH:
				return EnumFacing.EAST;
			case EAST:
				return EnumFacing.NORTH;
			case WEST:
				return EnumFacing.SOUTH;
			default:
				return EnumFacing.NORTH;
		}
	}

	private BlockPos[] getLadderPositions(int cx, int cy, int cz, ForgeDirection dir) {
		switch(dir) {
			case NORTH:
				return new BlockPos[] {
						new BlockPos(cx - 3, cy, cz),
						new BlockPos(cx - 3, cy + 1, cz),
						new BlockPos(cx - 3, cy + 2, cz),
						new BlockPos(cx - 3, cy, cz - 1),
						new BlockPos(cx - 3, cy + 1, cz - 1),
						new BlockPos(cx - 3, cy + 2, cz - 1)
				};
			case SOUTH:
				return new BlockPos[] {
						new BlockPos(cx + 3, cy, cz),
						new BlockPos(cx + 3, cy + 1, cz),
						new BlockPos(cx + 3, cy + 2, cz),
						new BlockPos(cx + 3, cy, cz + 1),
						new BlockPos(cx + 3, cy + 1, cz + 1),
						new BlockPos(cx + 3, cy + 2, cz + 1)
				};
			case EAST:
				return new BlockPos[] {
						new BlockPos(cx, cy, cz - 3),
						new BlockPos(cx, cy + 1, cz - 3),
						new BlockPos(cx, cy + 2, cz - 3),
						new BlockPos(cx + 1, cy, cz - 3),
						new BlockPos(cx + 1, cy + 1, cz - 3),
						new BlockPos(cx + 1, cy + 2, cz - 3)
				};
			case WEST:
				return new BlockPos[] {
						new BlockPos(cx, cy, cz + 3),
						new BlockPos(cx, cy + 1, cz + 3),
						new BlockPos(cx, cy + 2, cz + 3),
						new BlockPos(cx - 1, cy, cz + 3),
						new BlockPos(cx - 1, cy + 1, cz + 3),
						new BlockPos(cx - 1, cy + 2, cz + 3)
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
	protected boolean checkRequirement(World world, int x, int y, int z, ForgeDirection dir, int o) {
		if(!MultiblockHandlerXR.checkSpace(world, x + dir.offsetX * o, y + dir.offsetY * o, z + dir.offsetZ * o, getDimensions(), x, y, z, dir)) return false;
		if(!MultiblockHandlerXR.checkSpace(world, x + dir.offsetX * o, y + dir.offsetY * o, z + dir.offsetZ * o, new int[] {2, 0, 1, 1, 2, -2}, x, y, z, dir)) return false;
		if(!MultiblockHandlerXR.checkSpace(world, x + dir.offsetX * o, y + dir.offsetY * o, z + dir.offsetZ * o, new int[] {2, 0, 1, 1, -2, 2}, x, y, z, dir)) return false;

		return true;
	}

	@Override
	public boolean onBlockActivated(World world, BlockPos pos1, IBlockState state, EntityPlayer player, EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ) {
		if(world.isRemote) {
			return true;
		} else if(!player.isSneaking()) {
			int[] pos = this.findCore(world, pos1.getX(), pos1.getY(), pos1.getZ());

			if(pos == null)
				return false;

			FMLNetworkHandler.openGui(player, MainRegistry.instance, ModBlocks.guiID_barrel, world, pos[0], pos[1], pos[2]);
			return true;
		} else {
			return true;
		}
	}
}