package com.hbm.blocks.machine;

import com.hbm.blocks.BlockDummyable;
import com.hbm.blocks.ModBlocks;
import com.hbm.main.MainRegistry;
import com.hbm.tileentity.TileEntityProxyCombo;
import com.hbm.tileentity.machine.TileEntityMachineSolderingStation;

import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumBlockRenderType;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

//Ported from NTM:CE (MachineSolderingStation)
public class MachineSolderingStation extends BlockDummyable {

	public MachineSolderingStation(Material mat, String s) {
		super(mat, s);
	}

	@Override
	public TileEntity createNewTileEntity(World world, int meta) {

		if(meta >= 12)
			return new TileEntityMachineSolderingStation();

		//every block of the 2x2 table is a port
		return new TileEntityProxyCombo(true, true, true);
	}

	@Override
	public int[] getDimensions() {
		return new int[] {0, 0, 1, 0, 1, 0};
	}

	@Override
	public int getOffset() {
		return 0;
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

		if(te instanceof TileEntityMachineSolderingStation)
			player.openGui(MainRegistry.instance, ModBlocks.guiID_machine_soldering_station, world, corePos[0], corePos[1], corePos[2]);

		return true;
	}

	@Override
	public EnumBlockRenderType getRenderType(IBlockState state) {
		return EnumBlockRenderType.ENTITYBLOCK_ANIMATED;
	}
}
