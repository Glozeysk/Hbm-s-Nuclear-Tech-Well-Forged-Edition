package com.hbm.blocks.network;

import com.hbm.tileentity.conductor.TileEntityFFFluidDuctMk3;
import com.hbm.util.I18nUtil;
import net.minecraft.block.material.Material;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import java.util.List;

public class BlockFluidPipeMk3 extends BlockFluidPipeBase {

	public BlockFluidPipeMk3(Material materialIn, String s) {
		super(materialIn, s);
	}

	@Override public net.minecraft.tileentity.TileEntity createNewTileEntity(World worldIn, int meta) { return new TileEntityFFFluidDuctMk3(); }

	@Override public net.minecraft.util.EnumBlockRenderType getRenderType(net.minecraft.block.state.IBlockState state) { return net.minecraft.util.EnumBlockRenderType.ENTITYBLOCK_ANIMATED; }

	@Override
	public void addInformation(ItemStack stack, World player, List<String> tooltip, net.minecraft.client.util.ITooltipFlag advanced) {
		tooltip.add(TextFormatting.GREEN + I18nUtil.resolveKey("desc.canhighcor"));
		tooltip.add(TextFormatting.YELLOW + I18nUtil.resolveKey("desc.cannotam"));
		tooltip.add(TextFormatting.RED + I18nUtil.resolveKey("desc.cannotextremelyhot"));
		tooltip.add("");
		tooltip.add(TextFormatting.AQUA + I18nUtil.resolveKey("desc.screwdriver_throughput"));
	}
}