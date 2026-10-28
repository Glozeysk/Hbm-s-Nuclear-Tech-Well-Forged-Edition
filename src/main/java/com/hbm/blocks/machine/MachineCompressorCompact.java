package com.hbm.blocks.machine;

import com.hbm.blocks.BlockDummyable;
import com.hbm.blocks.ModBlocks;
import com.hbm.lib.ForgeDirection;
import com.hbm.main.MainRegistry;
import com.hbm.tileentity.TileEntityProxyCombo;
import com.hbm.tileentity.machine.TileEntityMachineCompressorCompact;

import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumBlockRenderType;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

//Ported from NTM:CE (MachineCompressorCompact)
public class MachineCompressorCompact extends BlockDummyable {

	public MachineCompressorCompact(Material mat, String s) {
		super(mat, s);
	}

	@Override
	public TileEntity createNewTileEntity(World world, int meta) {

		if(meta >= 12)
			return new TileEntityMachineCompressorCompact();

		if(hasExtra(meta))
			return new TileEntityProxyCombo(true, true, true);

		return null;
	}

	@Override
	public int[] getDimensions() {
		return new int[] {2, 0, 1, 1, 3, 3};
	}

	@Override
	public int getOffset() {
		return 1;
	}

	//the six ports on the second layer; must match TileEntityMachineCompressorCompact.getConPos
	@Override
	public void fillSpace(World world, int x, int y, int z, ForgeDirection dir, int o) {
		super.fillSpace(world, x, y, z, dir, o);

		ForgeDirection rot = dir.getRotation(ForgeDirection.UP);
		x += dir.offsetX * o;
		z += dir.offsetZ * o;

		this.makeExtra(world, x + rot.offsetX * 3, y + 1, z + rot.offsetZ * 3);
		this.makeExtra(world, x - rot.offsetX * 3, y + 1, z - rot.offsetZ * 3);
		this.makeExtra(world, x + dir.offsetX + rot.offsetX, y + 1, z + dir.offsetZ + rot.offsetZ);
		this.makeExtra(world, x + dir.offsetX - rot.offsetX, y + 1, z + dir.offsetZ - rot.offsetZ);
		this.makeExtra(world, x - dir.offsetX + rot.offsetX, y + 1, z - dir.offsetZ + rot.offsetZ);
		this.makeExtra(world, x - dir.offsetX - rot.offsetX, y + 1, z - dir.offsetZ - rot.offsetZ);
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

		if(te instanceof TileEntityMachineCompressorCompact)
			player.openGui(MainRegistry.instance, ModBlocks.guiID_machine_compressor_compact, world, corePos[0], corePos[1], corePos[2]);

		return true;
	}

	@Override
	public EnumBlockRenderType getRenderType(IBlockState state) {
		return EnumBlockRenderType.ENTITYBLOCK_ANIMATED;
	}
}
