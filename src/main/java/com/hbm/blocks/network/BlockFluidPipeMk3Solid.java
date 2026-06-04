package com.hbm.blocks.network;

import java.util.ArrayList;
import java.util.List;

import com.hbm.blocks.ModBlocks;
import com.hbm.blocks.ILookOverlay;
import com.hbm.forgefluid.ModForgeFluids;
import com.hbm.util.I18nUtil;
import com.hbm.tileentity.conductor.TileEntityFFDuctBaseMk2;
import com.hbm.tileentity.conductor.TileEntityFFFluidDuctMk3;

import api.hbm.block.IToolable;
import net.minecraft.block.Block;
import net.minecraft.block.BlockContainer;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumBlockRenderType;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.client.event.RenderGameOverlayEvent.Pre;
import net.minecraftforge.fluids.Fluid;

public class BlockFluidPipeMk3Solid extends BlockContainer implements ILookOverlay {

	public static final PropertyBool EXTRACTS = PropertyBool.create("extracts");

	public BlockFluidPipeMk3Solid(Material materialIn, String s) {
		super(materialIn);
		this.setTranslationKey(s);
		this.setRegistryName(s);
		this.setDefaultState(this.blockState.getBaseState().withProperty(EXTRACTS, true));
		ModBlocks.ALL_BLOCKS.add(this);
	}

	@Override
	public TileEntity createNewTileEntity(World worldIn, int meta) {
		return new TileEntityFFFluidDuctMk3();
	}

	@Override
	public void addInformation(ItemStack stack, World player, List<String> tooltip, ITooltipFlag advanced) {
		tooltip.add(TextFormatting.GREEN + I18nUtil.resolveKey("desc.canhighcor"));
		tooltip.add(TextFormatting.YELLOW + I18nUtil.resolveKey("desc.cannotam"));
		tooltip.add(TextFormatting.RED + I18nUtil.resolveKey("desc.cannotextremelyhot"));
	}

	@Override
	public void onNeighborChange(IBlockAccess world, BlockPos pos, BlockPos neighbor) {
		TileEntity te = world.getTileEntity(pos);
		if(te instanceof TileEntityFFDuctBaseMk2){
			((TileEntityFFDuctBaseMk2)te).onNeighborChange();
		}
	}

	@Override
	public void neighborChanged(IBlockState state, World worldIn, BlockPos pos, Block blockIn, BlockPos fromPos) {
		TileEntity te = worldIn.getTileEntity(pos);
		if(te instanceof TileEntityFFDuctBaseMk2){
			((TileEntityFFDuctBaseMk2)te).onNeighborChange();
		}
	}

	@Override
	public IBlockState getActualState(IBlockState state, IBlockAccess worldIn, BlockPos pos) {
		TileEntity te = worldIn.getTileEntity(pos);
		if(te instanceof TileEntityFFDuctBaseMk2)
			((TileEntityFFDuctBaseMk2)te).onNeighborChange();
		return state;
	}

	@Override
	public void breakBlock(World worldIn, BlockPos pos, IBlockState state) {
		TileEntity te = worldIn.getTileEntity(pos);
		if(te instanceof TileEntityFFDuctBaseMk2){
			TileEntityFFDuctBaseMk2.breakBlock(worldIn, pos);
		}
		super.breakBlock(worldIn, pos, state);
	}

	@Override
	public EnumBlockRenderType getRenderType(IBlockState state) {
		return EnumBlockRenderType.MODEL;
	}

	@Override
	protected BlockStateContainer createBlockState() {
		return new BlockStateContainer(this, new PropertyBool[]{ EXTRACTS });
	}

	@Override
	public int getMetaFromState(IBlockState state) {
		return state.getValue(EXTRACTS) ? 1 : 0;
	}

	@Override
	public IBlockState getStateFromMeta(int meta) {
		return meta > 0 ? this.getDefaultState().withProperty(EXTRACTS, true) : this.getDefaultState().withProperty(EXTRACTS, false);
	}

	@Override
	public void printHook(Pre event, World world, int x, int y, int z) {
		TileEntity te = world.getTileEntity(new BlockPos(x, y, z));
		if(!(te instanceof TileEntityFFDuctBaseMk2)) return;

		TileEntityFFDuctBaseMk2 pipe = (TileEntityFFDuctBaseMk2) te;
		Fluid ductFluid = pipe.getType();

		List<String> text = new ArrayList<>();
		if(ductFluid == null){
			text.add("§7" + I18nUtil.resolveKey("desc.none"));
		} else {
			int color = ModForgeFluids.getFluidColor(ductFluid);
			text.add("&[" + color + "&]" + I18nUtil.resolveKey(ductFluid.getUnlocalizedName()));
		}

		ILookOverlay.printGeneric(event, I18nUtil.resolveKey(getTranslationKey() + ".name"),
				0xffff00, 0x404000, text);
	}
}