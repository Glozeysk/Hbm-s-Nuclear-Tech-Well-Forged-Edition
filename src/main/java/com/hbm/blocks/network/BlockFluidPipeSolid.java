package com.hbm.blocks.network;

import com.hbm.tileentity.conductor.TileEntityFFFluidDuctMk2;
import com.hbm.util.I18nUtil;
import net.minecraft.block.material.Material;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import java.util.List;

public class BlockFluidPipeSolid extends BlockFluidPipeBase {

	public BlockFluidPipeSolid(Material materialIn, String s) {
		super(materialIn, s);
	}

	@Override public net.minecraft.tileentity.TileEntity createNewTileEntity(World worldIn, int meta) { return new TileEntityFFFluidDuctMk2(); }

	@Override public net.minecraft.util.EnumBlockRenderType getRenderType(net.minecraft.block.state.IBlockState state) { return net.minecraft.util.EnumBlockRenderType.MODEL; }

	@Override
	public void addInformation(ItemStack stack, World player, List<String> tooltip, net.minecraft.client.util.ITooltipFlag advanced) {
		tooltip.add(TextFormatting.GREEN + I18nUtil.resolveKey("desc.cancor"));
		tooltip.add(TextFormatting.YELLOW + I18nUtil.resolveKey("desc.cannothighcor"));
		tooltip.add(TextFormatting.YELLOW + I18nUtil.resolveKey("desc.cannotam"));
		tooltip.add(TextFormatting.RED + I18nUtil.resolveKey("desc.cannotreallyhot"));
		tooltip.add("");
		tooltip.add(TextFormatting.AQUA + I18nUtil.resolveKey("desc.screwdriver_throughput"));
	}
}