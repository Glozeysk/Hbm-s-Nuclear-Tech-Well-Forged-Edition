package com.hbm.blocks.machine;

import java.util.List;

import com.hbm.blocks.BlockDummyable;
import com.hbm.blocks.ModBlocks;
import com.hbm.handler.MultiblockHandlerXR;
import com.hbm.lib.ForgeDirection;
import com.hbm.main.MainRegistry;
import com.hbm.tileentity.TileEntityProxyCombo;
import com.hbm.tileentity.machine.TileEntityMachineFluidTank;

import com.hbm.util.I18nUtil;
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
		MultiblockHandlerXR.fillSpace(world, x + dir.offsetX * o , y + dir.offsetY * o, z + dir.offsetZ * o, new int[] {2, 0, 1, 1, 2, -2}, this, dir);
		MultiblockHandlerXR.fillSpace(world, x + dir.offsetX * o , y + dir.offsetY * o, z + dir.offsetZ * o, new int[] {2, 0, 1, 1, -2, 2}, this, dir);

		this.makeExtra(world, x + dir.offsetX * o + 1, y, z + dir.offsetZ * o + 1);
		this.makeExtra(world, x + dir.offsetX * o - 1, y, z + dir.offsetZ * o + 1);
		this.makeExtra(world, x + dir.offsetX * o + 1, y, z + dir.offsetZ * o - 1);
		this.makeExtra(world, x + dir.offsetX * o - 1, y, z + dir.offsetZ * o - 1);
		// this.makeExtra(world, x + dir.offsetX * o + 1, y, z + dir.offsetZ * o + 1);
		// this.makeExtra(world, x + dir.offsetX * o - 1, y, z + dir.offsetZ * o + 1);
		// this.makeExtra(world, x + dir.offsetX * o + 1, y, z + dir.offsetZ * o - 1);
		// this.makeExtra(world, x + dir.offsetX * o - 1, y, z + dir.offsetZ * o - 1);
	}

	@Override
	protected boolean checkRequirement(World world, int x, int y, int z, ForgeDirection dir, int o) {

		if(!MultiblockHandlerXR.checkSpace(world, x + dir.offsetX * o , y + dir.offsetY * o, z + dir.offsetZ * o, getDimensions(), x, y, z, dir)) return false;
		if(!MultiblockHandlerXR.checkSpace(world, x + dir.offsetX * o , y + dir.offsetY * o, z + dir.offsetZ * o, new int[] {2, 0, 1, 1, 2, -2}, x, y, z, dir)) return false;
		if(!MultiblockHandlerXR.checkSpace(world, x + dir.offsetX * o , y + dir.offsetY * o, z + dir.offsetZ * o, new int[] {2, 0, 1, 1, -2, 2}, x, y, z, dir)) return false;
		
		return true;
	}
	
	@Override
	public boolean onBlockActivated(World world, BlockPos pos1, IBlockState state, EntityPlayer player, EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ){
		if(world.isRemote) {
			return true;
		} else if(!player.isSneaking()) {
			
			int[] pos = this.findCore(world, pos1.getX(), pos1.getY(), pos1.getZ());
			
			if(pos == null)
				return false;
			
			FMLNetworkHandler.openGui(player, MainRegistry.instance, ModBlocks.guiID_barrel, world, pos[0], pos[1], pos[2]); //we can do this because nobody is stopping me from doing this
			return true;
		} else {
			return true;
		}
	}
	
}