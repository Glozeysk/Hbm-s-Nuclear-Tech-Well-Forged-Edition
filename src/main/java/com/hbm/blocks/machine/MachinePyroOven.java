package com.hbm.blocks.machine;

import com.hbm.blocks.BlockDummyable;
import com.hbm.blocks.ModBlocks;
import com.hbm.lib.ForgeDirection;
import com.hbm.main.MainRegistry;
import com.hbm.tileentity.TileEntityProxyCombo;
import com.hbm.tileentity.machine.oil.TileEntityMachinePyroOven;

import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumBlockRenderType;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

//Ported from NTM:CE (Warfactory-Official/Hbm-s-Nuclear-Tech-CE): MachinePyroOven, without the chimney smoke port
public class MachinePyroOven extends BlockDummyable {

	public MachinePyroOven(Material mat, String s) {
		super(mat, s);
	}

	@Override
	public TileEntity createNewTileEntity(World world, int meta) {
		if(meta >= 12)
			return new TileEntityMachinePyroOven();

		if(meta >= 6)
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

			TileEntity entity = world.getTileEntity(new BlockPos(pos1[0], pos1[1], pos1[2]));
			if(entity instanceof TileEntityMachinePyroOven) {
				player.openGui(MainRegistry.instance, ModBlocks.guiID_machine_pyrooven, world, pos1[0], pos1[1], pos1[2]);
			}
			return true;
		} else {
			return false;
		}
	}

	@Override
	public int[] getDimensions() {
		return new int[] {2, 0, 3, 3, 2, 2};
	}

	@Override
	public int getOffset() {
		return 3;
	}

	@Override
	protected void fillSpace(World world, int x, int y, int z, ForgeDirection dir, int o) {
		super.fillSpace(world, x, y, z, dir, o);
		x += dir.offsetX * o;
		z += dir.offsetZ * o;

		ForgeDirection rot = dir.getRotation(ForgeDirection.DOWN);

		//must match TileEntityMachinePyroOven.updateConnections
		for(int i = -2; i <= 2; i++) {
			this.makeExtra(world, x + dir.offsetX * i + rot.offsetX * 2, y, z + dir.offsetZ * i + rot.offsetZ * 2);
		}
	}

	@Override
	public EnumBlockRenderType getRenderType(IBlockState state) {
		return EnumBlockRenderType.ENTITYBLOCK_ANIMATED;
	}
}
