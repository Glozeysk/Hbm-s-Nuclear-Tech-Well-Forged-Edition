package com.hbm.blocks.network;

import com.hbm.blocks.ILookOverlay;
import com.hbm.forgefluid.ModForgeFluids;
import com.hbm.lib.Library;
import com.hbm.tileentity.conductor.TileEntityFFDuctBaseMk2;
import com.hbm.tileentity.conductor.TileEntityFFFluidDuctMk4;
import com.hbm.util.I18nUtil;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import java.util.List;

public class BlockFluidPipeMk4 extends BlockFluidPipeBase {

	public BlockFluidPipeMk4(Material materialIn, String s) {
		super(materialIn, s);
		this.setDefaultState(this.blockState.getBaseState().withProperty(EXTRACTS, false));
	}

	@Override public net.minecraft.tileentity.TileEntity createNewTileEntity(World worldIn, int meta) { return new TileEntityFFFluidDuctMk4(); }

	@Override public net.minecraft.util.EnumBlockRenderType getRenderType(net.minecraft.block.state.IBlockState state) { return net.minecraft.util.EnumBlockRenderType.ENTITYBLOCK_ANIMATED; }

	@Override
	public ItemStack getPickBlock(IBlockState state, RayTraceResult target, World world, BlockPos pos, EntityPlayer player){
		return super.getPickBlock(state, target, world, pos, player);
	}

	@Override
	public void printHook(net.minecraftforge.client.event.RenderGameOverlayEvent.Pre event, World world, int x, int y, int z) {
		net.minecraft.tileentity.TileEntity te = world.getTileEntity(new BlockPos(x, y, z));

		if(!(te instanceof TileEntityFFDuctBaseMk2))
			return;

		TileEntityFFDuctBaseMk2 pipe = (TileEntityFFDuctBaseMk2) te;
		net.minecraftforge.fluids.Fluid ductFluid = pipe.getType();

		java.util.List<String> text = new java.util.ArrayList<>();
		if(ductFluid == null){
			text.add("\u00a77" + I18nUtil.resolveKey("desc.none"));
		} else{
			int color = ModForgeFluids.getFluidColor(ductFluid);
			text.add("&[" + color + "&]" + I18nUtil.resolveKey(ductFluid.getUnlocalizedName()));
		}

		if(te instanceof TileEntityFFFluidDuctMk4) {
			TileEntityFFFluidDuctMk4 duct = (TileEntityFFFluidDuctMk4) te;
			String status = duct.isNetworkPowered() ? "\u00a7aActive" : "\u00a7cInactive";
			text.add("Status: " + status);
			text.add("Drain: " + Library.getShortNumber(duct.getNetworkDrain()) + " HE/s");
			text.add("Pipes: " + duct.getNetworkSize());

			if (pipe.hasExternalConnections()) {
				text.add(TextFormatting.AQUA + I18nUtil.resolveKey("desc.throughput") + ": " + getThroughputText(pipe.getThroughput()));
			}
		}

		ILookOverlay.printGeneric(event, I18nUtil.resolveKey(getTranslationKey() + ".name"), 0xffff00, 0x404000, text);
	}

	@Override
	public void addInformation(ItemStack stack, World player, List<String> tooltip, net.minecraft.client.util.ITooltipFlag advanced) {
		tooltip.add(TextFormatting.GREEN + I18nUtil.resolveKey("desc.canhighcor"));
		tooltip.add(TextFormatting.GREEN + I18nUtil.resolveKey("desc.canam"));
		tooltip.add(TextFormatting.GREEN + I18nUtil.resolveKey("desc.canhot"));
		tooltip.add("");
		tooltip.add(TextFormatting.AQUA + I18nUtil.resolveKey("desc.screwdriver_throughput"));
	}
}