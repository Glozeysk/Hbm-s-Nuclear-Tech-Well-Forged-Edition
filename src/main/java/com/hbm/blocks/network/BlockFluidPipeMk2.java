package com.hbm.blocks.network;

import java.util.ArrayList;
import java.util.List;

import com.hbm.blocks.ModBlocks;
import com.hbm.blocks.ILookOverlay;
import com.hbm.forgefluid.ModForgeFluids;
import com.hbm.util.I18nUtil;
import com.hbm.tileentity.conductor.TileEntityFFDuctBaseMk2;
import com.hbm.tileentity.conductor.TileEntityFFFluidDuctMk2;

import api.hbm.block.IToolable;
import net.minecraft.block.Block;
import net.minecraft.block.BlockContainer;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.state.BlockFaceShape;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumBlockRenderType;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.client.event.RenderGameOverlayEvent.Pre;
import net.minecraftforge.fluids.Fluid;

public class BlockFluidPipeMk2 extends BlockContainer implements ILookOverlay {

	public static final PropertyBool EXTRACTS = PropertyBool.create("extracts");
	private static final AxisAlignedBB DUCT_BB = new AxisAlignedBB(1, 1, 1, -1, -1, -1);

	public BlockFluidPipeMk2(Material materialIn, String s) {
		super(materialIn);
		this.setTranslationKey(s);
		this.setRegistryName(s);
		this.setDefaultState(this.blockState.getBaseState().withProperty(EXTRACTS, true));
		ModBlocks.ALL_BLOCKS.add(this);
	}

	@Override public TileEntity createNewTileEntity(World worldIn, int meta) { return new TileEntityFFFluidDuctMk2(); }

	@Override
	public void addInformation(ItemStack stack, World player, List<String> tooltip, ITooltipFlag advanced) {
		tooltip.add(TextFormatting.GREEN + I18nUtil.resolveKey("desc.cancor"));
		tooltip.add(TextFormatting.YELLOW + I18nUtil.resolveKey("desc.cannothighcor"));
		tooltip.add(TextFormatting.YELLOW + I18nUtil.resolveKey("desc.cannotam"));
		tooltip.add(TextFormatting.RED + I18nUtil.resolveKey("desc.cannotreallyhot"));
	}

	@Override public void onNeighborChange(IBlockAccess world, BlockPos pos, BlockPos neighbor) {
		TileEntity te = world.getTileEntity(pos);
		if(te instanceof TileEntityFFDuctBaseMk2) ((TileEntityFFDuctBaseMk2)te).onNeighborChange();
	}
	@Override public void neighborChanged(IBlockState state, World worldIn, BlockPos pos, Block blockIn, BlockPos fromPos) {
		TileEntity te = worldIn.getTileEntity(pos);
		if(te instanceof TileEntityFFDuctBaseMk2) ((TileEntityFFDuctBaseMk2)te).onNeighborChange();
	}
	@Override public IBlockState getActualState(IBlockState state, IBlockAccess worldIn, BlockPos pos) {
		TileEntity te = worldIn.getTileEntity(pos);
		if(te instanceof TileEntityFFDuctBaseMk2) ((TileEntityFFDuctBaseMk2)te).onNeighborChange();
		return state;
	}
	@Override public void breakBlock(World worldIn, BlockPos pos, IBlockState state) {
		TileEntity te = worldIn.getTileEntity(pos);
		if(te instanceof TileEntityFFDuctBaseMk2) TileEntityFFDuctBaseMk2.breakBlock(worldIn, pos);
		super.breakBlock(worldIn, pos, state);
	}

	@Override
	public AxisAlignedBB getBoundingBox(IBlockState state, IBlockAccess world, BlockPos pos) {
		if (world.getTileEntity(pos) instanceof TileEntityFFDuctBaseMk2) {
			TileEntityFFDuctBaseMk2 te = (TileEntityFFDuctBaseMk2) world.getTileEntity(pos);
			if (te != null) {
				boolean pX = te.connections[3] != null, nX = te.connections[5] != null;
				boolean pY = te.connections[0] != null, nY = te.connections[1] != null;
				boolean pZ = te.connections[4] != null, nZ = te.connections[2] != null;
				int mask = (pX?32:0)+(nX?16:0)+(pY?8:0)+(nY?4:0)+(pZ?2:0)+(nZ?1:0);
				if(mask==0) return new AxisAlignedBB(0,0,0,1,1,1);
				else if(mask==0b100000||mask==0b010000||mask==0b110000) return new AxisAlignedBB(0,0.3125F,0.3125F,1,0.6875F,0.6875F);
				else if(mask==0b001000||mask==0b000100||mask==0b001100) return new AxisAlignedBB(0.3125F,0,0.3125F,0.6875F,1,0.6875F);
				else if(mask==0b000010||mask==0b000001||mask==0b000011) return new AxisAlignedBB(0.3125F,0.3125F,0,0.6875F,0.6875F,1);
				else return new AxisAlignedBB(nX?0:0.3125F,nY?0:0.3125F,nZ?0:0.3125F,pX?1:0.6875F,pY?1:0.6875F,pZ?1:0.6875F);
			}
		}
		return DUCT_BB;
	}

	@Override public EnumBlockRenderType getRenderType(IBlockState state) { return EnumBlockRenderType.ENTITYBLOCK_ANIMATED; }
	@Override public boolean isFullBlock(IBlockState state) { return false; }
	@Override public boolean isFullCube(IBlockState state) { return false; }
	@Override public boolean isBlockNormalCube(IBlockState state) { return false; }
	@Override public boolean isNormalCube(IBlockState state) { return false; }
	@Override public boolean isNormalCube(IBlockState state, IBlockAccess world, BlockPos pos) { return false; }
	@Override public boolean isOpaqueCube(IBlockState state) { return false; }
	@Override public BlockFaceShape getBlockFaceShape(IBlockAccess worldIn, IBlockState state, BlockPos pos, EnumFacing face){ return BlockFaceShape.CENTER; }

	@Override protected BlockStateContainer createBlockState() { return new BlockStateContainer(this, new PropertyBool[]{ EXTRACTS }); }
	@Override public int getMetaFromState(IBlockState state) { return state.getValue(EXTRACTS) ? 1 : 0; }
	@Override public IBlockState getStateFromMeta(int meta) { return meta > 0 ? getDefaultState().withProperty(EXTRACTS, true) : getDefaultState().withProperty(EXTRACTS, false); }


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