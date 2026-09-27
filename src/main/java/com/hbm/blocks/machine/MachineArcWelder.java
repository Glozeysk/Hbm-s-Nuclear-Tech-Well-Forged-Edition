package com.hbm.blocks.machine;

import com.hbm.blocks.BlockDummyable;
import com.hbm.blocks.ModBlocks;
import com.hbm.lib.ForgeDirection;
import com.hbm.main.MainRegistry;
import com.hbm.tileentity.TileEntityProxyCombo;
import com.hbm.tileentity.machine.TileEntityMachineArcWelder;

import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumBlockRenderType;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

//Ported from NTM:CE (MachineArcWelder)
public class MachineArcWelder extends BlockDummyable {

	public MachineArcWelder(Material mat, String s) {
		super(mat, s);
	}

	@Override
	public TileEntity createNewTileEntity(World world, int meta) {

		if(meta >= 12)
			return new TileEntityMachineArcWelder();

		if(hasExtra(meta))
			return new TileEntityProxyCombo(true, true, true);

		return null;
	}

	@Override
	public int[] getDimensions() {
		return new int[] {1, 0, 1, 0, 1, 1};
	}

	@Override
	public int getOffset() {
		return 0;
	}

	@Override
	public void fillSpace(World world, int x, int y, int z, ForgeDirection dir, int o) {
		super.fillSpace(world, x, y, z, dir, o);

		ForgeDirection rot = dir.getRotation(ForgeDirection.UP);
		x += dir.offsetX * o;
		z += dir.offsetZ * o;

		//only the bottom layer carries ports, the blocks above stay plain dummies
		for(int a = -1; a <= 1; a++) {
			for(int b = -1; b <= 0; b++) {
				if(a == 0 && b == 0)
					continue;
				this.makeExtra(world, x + rot.offsetX * a + dir.offsetX * b, y, z + rot.offsetZ * a + dir.offsetZ * b);
			}
		}
	}

	@Override
	public boolean onBlockActivated(World world, BlockPos pos, IBlockState state, EntityPlayer player, EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ) {

		if(world.isRemote)
			return true;

		if(player.isSneaking())
			return false;

		int[] corePos = this.findCore(world, pos.getX(), pos.getY(), pos.getZ());

		if(corePos == null)
			return false;

		TileEntity te = world.getTileEntity(new BlockPos(corePos[0], corePos[1], corePos[2]));

		if(te instanceof TileEntityMachineArcWelder)
			player.openGui(MainRegistry.instance, ModBlocks.guiID_machine_arc_welder, world, corePos[0], corePos[1], corePos[2]);

		return true;
	}

	@Override
	public EnumBlockRenderType getRenderType(IBlockState state) {
		return EnumBlockRenderType.ENTITYBLOCK_ANIMATED;
	}
}
