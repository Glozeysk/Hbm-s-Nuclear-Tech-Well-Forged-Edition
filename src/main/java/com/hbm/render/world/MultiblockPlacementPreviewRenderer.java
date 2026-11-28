package com.hbm.render.world;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL14;

import com.hbm.blocks.BlockDummyable;
import com.hbm.handler.MultiblockPlacement;
import com.hbm.interfaces.IMultiBlock;

import net.minecraft.block.Block;
import net.minecraft.block.BlockContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.tileentity.TileEntityRendererDispatcher;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.world.World;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.ReflectionHelper;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * Renders a translucent preview of the multiblock that would be placed at the targeted position.
 * Green means the space check passed, red means at least one block is in the way.
 */
@SideOnly(Side.CLIENT)
public class MultiblockPlacementPreviewRenderer {

	private static final Map<Block, TileEntity> PREVIEW_TILES = new HashMap<>();
	private static final Set<Block> FAILED_TILES = new HashSet<>();
	private static Field tileBlockType;
	private static Field tileBlockMetadata;
	private static boolean tileFieldsInitialized;

	@SubscribeEvent(priority = EventPriority.LOWEST)
	public void onRenderWorldLast(RenderWorldLastEvent event) {
		Minecraft mc = Minecraft.getMinecraft();
		EntityPlayer player = mc.player;
		World world = mc.world;

		if(player == null || world == null || mc.currentScreen != null || mc.gameSettings.hideGUI) {
			return;
		}

		ItemStack stack = getHeldMultiblock(player);
		if(stack.isEmpty()) {
			return;
		}

		Block block = ((ItemBlock) stack.getItem()).getBlock();
		if(!(block instanceof BlockDummyable) && !(block instanceof IMultiBlock)) {
			return;
		}

		RayTraceResult mop = mc.objectMouseOver;
		if(mop == null || mop.typeOfHit != RayTraceResult.Type.BLOCK || mop.sideHit == null) {
			return;
		}

		BlockPos hitPos = mop.getBlockPos();
		if(!world.isBlockLoaded(hitPos)) {
			return;
		}

		IBlockState hitState = world.getBlockState(hitPos);
		BlockPos placedPos = hitState.getBlock().isReplaceable(world, hitPos) ? hitPos : hitPos.offset(mop.sideHit);

		if(!world.isBlockLoaded(placedPos) || placedPos.getY() < 0 || placedPos.getY() >= 256) {
			return;
		}

		if(!player.canPlayerEdit(placedPos, mop.sideHit, stack)) {
			return;
		}

		MultiblockPlacement placement;
		if(block instanceof BlockDummyable) {
			placement = ((BlockDummyable) block).getPlacementPreview(world, placedPos, player);
		} else {
			placement = ((IMultiBlock) block).getPlacementPreview(world, placedPos, player);
		}

		if(placement == null) {
			return;
		}

		boolean valid = placement.valid
				&& world.getBlockState(placedPos).getBlock().isReplaceable(world, placedPos)
				&& block.canPlaceBlockAt(world, placedPos);

		Entity view = mc.getRenderViewEntity();
		double viewX = view.lastTickPosX + (view.posX - view.lastTickPosX) * (double) event.getPartialTicks();
		double viewY = view.lastTickPosY + (view.posY - view.lastTickPosY) * (double) event.getPartialTicks();
		double viewZ = view.lastTickPosZ + (view.posZ - view.lastTickPosZ) * (double) event.getPartialTicks();

		renderPreview(event.getPartialTicks(), viewX, viewY, viewZ, placement, valid);
	}

	private static ItemStack getHeldMultiblock(EntityPlayer player) {
		ItemStack main = player.getHeldItemMainhand();
		if(isMultiblockItem(main)) {
			return main;
		}

		ItemStack off = player.getHeldItemOffhand();
		if(isMultiblockItem(off)) {
			return off;
		}

		return ItemStack.EMPTY;
	}

	private static boolean isMultiblockItem(ItemStack stack) {
		if(stack.isEmpty() || !(stack.getItem() instanceof ItemBlock)) {
			return false;
		}

		Block block = ((ItemBlock) stack.getItem()).getBlock();
		return block instanceof BlockDummyable || block instanceof IMultiBlock;
	}

	private static void renderPreview(float partialTicks, double viewX, double viewY, double viewZ,
			MultiblockPlacement placement, boolean valid) {
		float red = valid ? 0.25F : 1.0F;
		float green = valid ? 1.0F : 0.15F;
		float blue = valid ? 0.25F : 0.15F;

		GlStateManager.pushAttrib();
		try {
			renderGhostTile(placement, partialTicks, viewX, viewY, viewZ, red, green, blue);
		} finally {
			GlStateManager.popAttrib();
		}
	}

	private static void renderGhostTile(MultiblockPlacement placement, float partialTicks,
			double viewX, double viewY, double viewZ, float red, float green, float blue) {
		TileEntity tile = getPreviewTile(placement);
		if(tile == null) {
			return;
		}

		GlStateManager.pushMatrix();
		try {
			GlStateManager.translate(
					placement.corePos.getX() - viewX,
					placement.corePos.getY() - viewY,
					placement.corePos.getZ() - viewZ);
			GlStateManager.enableBlend();
			GlStateManager.enableAlpha();
			GlStateManager.alphaFunc(GL11.GL_GREATER, 0.1F);
			GlStateManager.depthMask(true);
			GlStateManager.disableCull();
			float alpha = 1.0F;

			/*
			 * Do not rely on glColor for the tint: TESRs frequently reset it themselves.
			 * Constant blend factors multiply every fragment by the preview color regardless
			 * of what the model renderer does with the current GL color.
			 */
			GL14.glBlendColor(red, green, blue, alpha);
			GlStateManager.tryBlendFuncSeparate(
					GlStateManager.SourceFactor.CONSTANT_COLOR, GlStateManager.DestFactor.ONE_MINUS_CONSTANT_ALPHA,
					GlStateManager.SourceFactor.CONSTANT_ALPHA, GlStateManager.DestFactor.ONE_MINUS_CONSTANT_ALPHA);
			GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);

			TileEntityRendererDispatcher.instance.render(tile, 0.0D, 0.0D, 0.0D, partialTicks, -1, alpha);
		} catch(Throwable throwable) {
			// A renderer that does not like the temporary tile entity should never break the game.
			FAILED_TILES.add(placement.block);
			PREVIEW_TILES.remove(placement.block);
		} finally {
			GL14.glBlendColor(1.0F, 1.0F, 1.0F, 1.0F);
			GlStateManager.tryBlendFuncSeparate(
					GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA,
					GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
			GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
			GlStateManager.enableCull();
			GlStateManager.depthMask(true);
			GlStateManager.popMatrix();
		}
	}

	private static TileEntity getPreviewTile(MultiblockPlacement placement) {
		if(FAILED_TILES.contains(placement.block)) {
			return null;
		}

		TileEntity tile = PREVIEW_TILES.get(placement.block);
		if(tile == null) {
			if(!(placement.block instanceof BlockContainer)) {
				FAILED_TILES.add(placement.block);
				return null;
			}

			tile = ((BlockContainer) placement.block).createNewTileEntity(Minecraft.getMinecraft().world, placement.metadata);
			if(tile == null) {
				FAILED_TILES.add(placement.block);
				return null;
			}
			PREVIEW_TILES.put(placement.block, tile);
		}

		tile.setWorld(Minecraft.getMinecraft().world);
		tile.setPos(placement.corePos);
		setTileField(tile, true, placement.block);
		setTileField(tile, false, placement.metadata);

		return tile;
	}

	private static void setTileField(TileEntity tile, boolean blockTypeField, Object value) {
		initializeTileFields();
		Field field = blockTypeField ? tileBlockType : tileBlockMetadata;
		if(field == null) {
			return;
		}

		try {
			field.set(tile, value);
		} catch(IllegalAccessException ignored) {
		}
	}

	private static void initializeTileFields() {
		if(tileFieldsInitialized) {
			return;
		}
		tileFieldsInitialized = true;

		try {
			tileBlockType = ReflectionHelper.findField(TileEntity.class, "blockType", "field_145854_h");
			tileBlockType.setAccessible(true);
		} catch(Throwable ignored) {
		}

		try {
			tileBlockMetadata = ReflectionHelper.findField(TileEntity.class, "blockMetadata", "field_145847_g");
			tileBlockMetadata.setAccessible(true);
		} catch(Throwable ignored) {
		}
	}

}
