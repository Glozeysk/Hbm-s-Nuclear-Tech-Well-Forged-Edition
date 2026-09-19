package com.hbm.blocks.machine;

import com.hbm.blocks.BlockDummyable;
import com.hbm.handler.MultiblockHandlerXR;
import com.hbm.lib.ForgeDirection;
import com.hbm.main.MainRegistry;
import com.hbm.tileentity.TileEntityProxyCombo;
import com.hbm.tileentity.machine.TileEntityElectrolyser;

import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumBlockRenderType;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

//Ported from NTM:CE (Warfactory-Official/Hbm-s-Nuclear-Tech-CE): MachineElectrolyser
public class MachineElectrolyser extends BlockDummyable {

	private static final int[][] EXTRA = {
			{2, -1, 5, 5, 1, 1},
			{3, -3, 5, 5, 0, 0},
			{3, -1, 4, -4, -3, 3},
			{3, -1, 2, -2, -3, 3},
			{3, -1, 0, 0, -3, 3},
			{3, -1, -2, 2, -3, 3},
			{3, -1, -4, 4, -3, 3}
	};
	private static final int[] PIPE = {0, 0, 0, 0, -1, 2};

	public MachineElectrolyser(Material mat, String s) {
		super(mat, s);
	}

	@Override
	public TileEntity createNewTileEntity(World world, int meta) {
		if(meta >= 12)
			return new TileEntityElectrolyser();

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
			if(entity instanceof TileEntityElectrolyser) {
				player.openGui(MainRegistry.instance, ((TileEntityElectrolyser) entity).getGuiID(), world, pos1[0], pos1[1], pos1[2]);
			}
			return true;
		} else {
			return false;
		}
	}

	@Override
	public int[] getDimensions() {
		return new int[] {0, 0, 5, 5, 1, 3};
	}

	@Override
	public int getOffset() {
		return 5;
	}

	@Override
	protected boolean checkRequirement(World world, int x, int y, int z, ForgeDirection dir, int o) {
		if(!super.checkRequirement(world, x, y, z, dir, o))
			return false;

		int cx = x + dir.offsetX * o;
		int cz = z + dir.offsetZ * o;

		for(int[] dim : EXTRA)
			if(!MultiblockHandlerXR.checkSpace(world, cx, y, cz, dim, x, y, z, dir))
				return false;
		for(int i = -4; i <= 4; i += 2)
			if(!MultiblockHandlerXR.checkSpace(world, cx + dir.offsetX * i, y + 3, cz + dir.offsetZ * i, PIPE, x, y, z, dir))
				return false;
		return true;
	}

	@Override
	protected void fillSpace(World world, int x, int y, int z, ForgeDirection dir, int o) {
		super.fillSpace(world, x, y, z, dir, o);

		x += dir.offsetX * o;
		z += dir.offsetZ * o;

		for(int[] dim : EXTRA)
			MultiblockHandlerXR.fillSpace(world, x, y, z, dim, this, dir);
		for(int i = -4; i <= 4; i += 2)
			MultiblockHandlerXR.fillSpace(world, x + dir.offsetX * i, y + 3, z + dir.offsetZ * i, PIPE, this, dir);

		ForgeDirection rot = dir.getRotation(ForgeDirection.UP);

		//must match TileEntityElectrolyser.updateConnections
		for(int side = -1; side <= 1; side += 2)
			for(int r = -1; r <= 1; r++)
				this.makeExtra(world, x + dir.offsetX * 5 * side + rot.offsetX * r, y, z + dir.offsetZ * 5 * side + rot.offsetZ * r);
	}

	@Override
	public EnumBlockRenderType getRenderType(IBlockState state) {
		return EnumBlockRenderType.ENTITYBLOCK_ANIMATED;
	}
}
