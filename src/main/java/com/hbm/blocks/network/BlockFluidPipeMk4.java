package com.hbm.blocks.network;

import java.util.ArrayList;
import java.util.List;

import com.hbm.blocks.ModBlocks;
import com.hbm.blocks.ILookOverlay;
import com.hbm.forgefluid.ModForgeFluids;
import com.hbm.lib.Library;
import com.hbm.util.I18nUtil;
import com.hbm.tileentity.conductor.TileEntityFFDuctBaseMk2;
import com.hbm.tileentity.conductor.TileEntityFFFluidDuctMk4;

import api.hbm.block.IToolable;
import api.hbm.block.IToolable.ToolType;
import net.minecraft.block.Block;
import net.minecraft.block.BlockContainer;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.state.BlockFaceShape;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumBlockRenderType;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.client.event.RenderGameOverlayEvent.Pre;
import net.minecraftforge.fluids.Fluid;

public class BlockFluidPipeMk4 extends BlockContainer implements ILookOverlay, IToolable {

	public static final PropertyBool EXTRACTS = PropertyBool.create("extracts");
	private static final float p = 1F / 16F;
	private static final AxisAlignedBB DUCT_BB = new AxisAlignedBB(1, 1, 1, -1, -1, -1);
	private static final int[] THROUGHPUT_TIERS = {1000, 5000, 10000, 20000, 50000};

	public BlockFluidPipeMk4(Material materialIn, String s) {
		super(materialIn);
		this.setTranslationKey(s);
		this.setRegistryName(s);
		this.setDefaultState(this.blockState.getBaseState().withProperty(EXTRACTS, false));
		ModBlocks.ALL_BLOCKS.add(this);
	}

	@Override
	public TileEntity createNewTileEntity(World worldIn, int meta) {
		return new TileEntityFFFluidDuctMk4();
	}

	@Override
	public void addInformation(ItemStack stack, World player, List<String> tooltip, ITooltipFlag advanced) {
		tooltip.add(TextFormatting.GREEN + I18nUtil.resolveKey("desc.canhighcor"));
		tooltip.add(TextFormatting.GREEN + I18nUtil.resolveKey("desc.canam"));
		tooltip.add(TextFormatting.GREEN + I18nUtil.resolveKey("desc.canhot"));
		tooltip.add("");
		tooltip.add(TextFormatting.AQUA + I18nUtil.resolveKey("desc.screwdriver_throughput"));
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
	public AxisAlignedBB getBoundingBox(IBlockState state, IBlockAccess world, BlockPos pos) {
		if (world.getTileEntity(pos) instanceof TileEntityFFDuctBaseMk2) {
			TileEntityFFDuctBaseMk2 te = (TileEntityFFDuctBaseMk2) world.getTileEntity(pos);
			if (te != null) {
				boolean pX = te.connections[3] != null;
				boolean nX = te.connections[5] != null;
				boolean pY = te.connections[0] != null;
				boolean nY = te.connections[1] != null;
				boolean pZ = te.connections[4] != null;
				boolean nZ = te.connections[2] != null;

				int mask = 0 + (pX ? 32 : 0) + (nX ? 16 : 0) + (pY ? 8 : 0) + (nY ? 4 : 0) + (pZ ? 2 : 0) + (nZ ? 1 : 0);

				if(mask == 0) {
					return new AxisAlignedBB(0F, 0F, 0F, 1F, 1F, 1F);
				} else if(mask == 0b100000 || mask == 0b010000 || mask == 0b110000) {
					return new AxisAlignedBB(0F, 0.3125F, 0.3125F, 1F, 0.6875F, 0.6875F);
				} else if(mask == 0b001000 || mask == 0b000100 || mask == 0b001100) {
					return new AxisAlignedBB(0.3125F, 0F, 0.3125F, 0.6875F, 1F, 0.6875F);
				} else if(mask == 0b000010 || mask == 0b000001 || mask == 0b000011) {
					return new AxisAlignedBB(0.3125F, 0.3125F, 0F, 0.6875F, 0.6875F, 1F);
				} else {
					return new AxisAlignedBB(
							nX ? 0F : 0.3125F,
							nY ? 0F : 0.3125F,
							nZ ? 0F : 0.3125F,
							pX ? 1F : 0.6875F,
							pY ? 1F : 0.6875F,
							pZ ? 1F : 0.6875F);
				}
			}
		}
		return DUCT_BB;
	}

	@Override
	public EnumBlockRenderType getRenderType(IBlockState state) {
		return EnumBlockRenderType.ENTITYBLOCK_ANIMATED;
	}

	@Override
	public boolean isFullBlock(IBlockState state) { return false; }
	@Override
	public boolean isFullCube(IBlockState state) { return false; }
	@Override
	public boolean isBlockNormalCube(IBlockState state) { return false; }
	@Override
	public boolean isNormalCube(IBlockState state) { return false; }
	@Override
	public boolean isNormalCube(IBlockState state, IBlockAccess world, BlockPos pos) { return false; }
	@Override
	public boolean isOpaqueCube(IBlockState state) { return false; }

	@Override
	public BlockFaceShape getBlockFaceShape(IBlockAccess worldIn, IBlockState state, BlockPos pos, EnumFacing face){
		return BlockFaceShape.CENTER;
	}

	@Override
	protected BlockStateContainer createBlockState() {
		return new BlockStateContainer(this, new IProperty[]{ EXTRACTS });
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
	public ItemStack getPickBlock(IBlockState state, RayTraceResult target, World world, BlockPos pos, EntityPlayer player){
		return super.getPickBlock(state, target, world, pos, player);
	}

	@Override
	public boolean onScrew(World world, EntityPlayer player, int x, int y, int z, EnumFacing side, float fX, float fY, float fZ, EnumHand hand, ToolType tool) {
		if (tool == ToolType.SCREWDRIVER) {
			BlockPos pos = new BlockPos(x, y, z);
			TileEntity te = world.getTileEntity(pos);
			if (te instanceof TileEntityFFDuctBaseMk2) {
				TileEntityFFDuctBaseMk2 pipe = (TileEntityFFDuctBaseMk2) te;
				int current = pipe.getThroughput();
				int nextIndex = 0;

				for (int i = 0; i < THROUGHPUT_TIERS.length; i++) {
					if (THROUGHPUT_TIERS[i] == current) {
						nextIndex = (i + 1) % THROUGHPUT_TIERS.length;
						break;
					}
				}

				pipe.setThroughput(THROUGHPUT_TIERS[nextIndex]);
				player.swingArm(hand);
				world.playSound(null, pos, SoundEvents.BLOCK_METAL_PLACE, SoundCategory.BLOCKS, 0.5F, 1.2F);
				return true;
			}
		}
		return false;
	}

	@Override
	public void printHook(Pre event, World world, int x, int y, int z) {
		TileEntity te = world.getTileEntity(new BlockPos(x, y, z));

		if(!(te instanceof TileEntityFFDuctBaseMk2))
			return;

		TileEntityFFDuctBaseMk2 pipe = (TileEntityFFDuctBaseMk2) te;
		Fluid ductFluid = pipe.getType();

		List<String> text = new ArrayList<>();
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
			text.add(TextFormatting.AQUA + I18nUtil.resolveKey("desc.throughput") + ": " + pipe.getThroughput() + " mB/t");
		}

		ILookOverlay.printGeneric(event, I18nUtil.resolveKey(getTranslationKey() + ".name"), 0xffff00, 0x404000, text);
	}
}