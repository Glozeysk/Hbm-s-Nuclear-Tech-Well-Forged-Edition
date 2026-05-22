package com.hbm.render.tileentity;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.lwjgl.opengl.GL11;

import com.hbm.blocks.network.BlockFluidPipeMk2;
import com.hbm.blocks.network.BlockFluidPipeMk3;
import com.hbm.blocks.network.BlockFluidPipeMk4;
import com.hbm.forgefluid.FluidTypeHandler;
import com.hbm.render.misc.DiamondPronter;
import com.hbm.tileentity.machine.TileEntityBarrel;

import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockRendererDispatcher;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import net.minecraftforge.fluids.FluidStack;

public class RenderFluidBarrel extends TileEntitySpecialRenderer<TileEntityBarrel> {

	private static final int FRONT_X0 = 22, FRONT_Y0 = 21;
	private static final int FRONT_X1 = 27, FRONT_Y1 = 26;

	private static final int TOP_X0 = 22, TOP_Y0 = 20;
	private static final int TOP_X1 = 27, TOP_Y1 = 21;

	private static final int BOTTOM_X0 = 22, BOTTOM_Y0 = 27;
	private static final int BOTTOM_X1 = 27, BOTTOM_Y1 = 28;

	private static final int LEFT_X0 = 20, LEFT_Y0 = 21;
	private static final int LEFT_X1 = 21, LEFT_Y1 = 26;

	private static final int RIGHT_X0 = 28, RIGHT_Y0 = 21;
	private static final int RIGHT_X1 = 29, RIGHT_Y1 = 26;

	private static final float UV_INSET = 0.01F;

	private static final double PIPE_MIN = 5.0 / 16.0;
	private static final double PIPE_MAX = 11.0 / 16.0;
	private static final double WALL_MIN = 2.0 / 16.0;
	private static final double WALL_MAX = 14.0 / 16.0;

	@Override
	public void render(TileEntityBarrel barrel, double x, double y, double z, float partialTicks, int destroyStage, float alpha) {
		renderPipeStubs(barrel, x, y, z);
		renderDiamondLabels(barrel, x, y, z);
	}

	private void renderDiamondLabels(TileEntityBarrel barrel, double x, double y, double z) {
		GL11.glPushMatrix();
		GL11.glTranslated(x + 0.5, y + 0.5, z + 0.5);
		GlStateManager.disableLighting();

		if(barrel.tank.getFluid() != null) {
			FluidStack type = barrel.tank.getFluid();
			FluidTypeHandler.FluidProperties p = FluidTypeHandler.getProperties(type);

			for(int j = 0; j < 4; j++) {
				GL11.glPushMatrix();
				GL11.glTranslated(0.4, 0.25, -0.15);
				GL11.glScalef(1.0F, 0.35F, 0.35F);
				DiamondPronter.pront(p.poison, p.flammability, p.reactivity, p.symbol);
				GL11.glPopMatrix();
				GL11.glRotatef(90, 0, 1, 0);
			}
		}

		GlStateManager.enableLighting();
		GL11.glPopMatrix();
	}

	private void renderPipeStubs(TileEntityBarrel barrel, double x, double y, double z) {
		World world = barrel.getWorld();
		if(world == null || !world.isRemote) return;

		BlockPos pos = barrel.getPos();
		if(pos == null) return;

		IBlockState state = world.getBlockState(pos);

		boolean hasAny = false;
		for(EnumFacing face : EnumFacing.HORIZONTALS) {
			if(isPipe(world.getBlockState(pos.offset(face)).getBlock())) {
				hasAny = true;
				break;
			}
		}
		if(!hasAny) return;

		int combinedLight = world.getCombinedLight(pos, 0);
		int blockLight = combinedLight & 0xFFFF;
		int skyLight = (combinedLight >> 16) & 0xFFFF;

		GlStateManager.pushMatrix();
		GlStateManager.translate(x, y, z);

		Minecraft.getMinecraft().getTextureManager().bindTexture(TextureMap.LOCATION_BLOCKS_TEXTURE);

		GlStateManager.disableCull();
		GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);

		OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, blockLight, skyLight);

		Tessellator tess = Tessellator.getInstance();
		BufferBuilder buf = tess.getBuffer();
		buf.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX);

		for(EnumFacing face : EnumFacing.HORIZONTALS) {
			if(!isPipe(world.getBlockState(pos.offset(face)).getBlock())) {
				continue;
			}

			TextureAtlasSprite sprite = getDominantBarrelSideSprite(state, pos, face);

			UV front  = uvFromPixelsInset(sprite, FRONT_X0, FRONT_Y0, FRONT_X1, FRONT_Y1, UV_INSET);
			UV top    = uvFromPixelsInset(sprite, TOP_X0, TOP_Y0, TOP_X1, TOP_Y1, UV_INSET);
			UV bottom = uvFromPixelsInset(sprite, BOTTOM_X0, BOTTOM_Y0, BOTTOM_X1, BOTTOM_Y1, UV_INSET);
			UV left   = uvFromPixelsInset(sprite, LEFT_X0, LEFT_Y0, LEFT_X1, LEFT_Y1, UV_INSET);
			UV right  = uvFromPixelsInset(sprite, RIGHT_X0, RIGHT_Y0, RIGHT_X1, RIGHT_Y1, UV_INSET);

			switch(face) {
				case NORTH:
					drawNorthStub(buf, front, top, bottom, left, right);
					break;
				case SOUTH:
					drawSouthStub(buf, front, top, bottom, left, right);
					break;
				case WEST:
					drawWestStub(buf, front, top, bottom, left, right);
					break;
				case EAST:
					drawEastStub(buf, front, top, bottom, left, right);
					break;
				default:
					break;
			}
		}

		tess.draw();

		GlStateManager.enableCull();
		GlStateManager.popMatrix();
	}

	private void drawNorthStub(BufferBuilder buf, UV front, UV top, UV bottom, UV left, UV right) {
		double x0 = PIPE_MIN, x1 = PIPE_MAX;
		double y0 = PIPE_MIN, y1 = PIPE_MAX;
		double z0 = 0.0,      z1 = WALL_MIN;

		q(buf, front, x1, y1, z0, x1, y0, z0, x0, y0, z0, x0, y1, z0);
		q(buf, top,   x0, y1, z1, x0, y1, z0, x1, y1, z0, x1, y1, z1);
		q(buf, bottom,x0, y0, z0, x0, y0, z1, x1, y0, z1, x1, y0, z0);
		q(buf, left,  x0, y1, z1, x0, y0, z1, x0, y0, z0, x0, y1, z0);
		q(buf, right, x1, y1, z0, x1, y0, z0, x1, y0, z1, x1, y1, z1);
	}

	private void drawSouthStub(BufferBuilder buf, UV front, UV top, UV bottom, UV left, UV right) {
		double x0 = PIPE_MIN, x1 = PIPE_MAX;
		double y0 = PIPE_MIN, y1 = PIPE_MAX;
		double z0 = WALL_MAX, z1 = 1.0;

		q(buf, front, x0, y1, z1, x0, y0, z1, x1, y0, z1, x1, y1, z1);
		q(buf, top,   x1, y1, z0, x1, y1, z1, x0, y1, z1, x0, y1, z0);
		q(buf, bottom,x1, y0, z1, x1, y0, z0, x0, y0, z0, x0, y0, z1);
		q(buf, left,  x1, y1, z0, x1, y0, z0, x1, y0, z1, x1, y1, z1);
		q(buf, right, x0, y1, z1, x0, y0, z1, x0, y0, z0, x0, y1, z0);
	}

	private void drawWestStub(BufferBuilder buf, UV front, UV top, UV bottom, UV left, UV right) {
		double x0 = 0.0,      x1 = WALL_MIN;
		double y0 = PIPE_MIN, y1 = PIPE_MAX;
		double z0 = PIPE_MIN, z1 = PIPE_MAX;

		q(buf, front, x0, y1, z0, x0, y0, z0, x0, y0, z1, x0, y1, z1);
		q(buf, top,   x1, y1, z0, x0, y1, z0, x0, y1, z1, x1, y1, z1);
		q(buf, bottom,x0, y0, z0, x1, y0, z0, x1, y0, z1, x0, y0, z1);
		q(buf, left,  x1, y1, z0, x1, y0, z0, x0, y0, z0, x0, y1, z0);
		q(buf, right, x0, y1, z1, x0, y0, z1, x1, y0, z1, x1, y1, z1);
	}

	private void drawEastStub(BufferBuilder buf, UV front, UV top, UV bottom, UV left, UV right) {
		double x0 = WALL_MAX, x1 = 1.0;
		double y0 = PIPE_MIN, y1 = PIPE_MAX;
		double z0 = PIPE_MIN, z1 = PIPE_MAX;

		q(buf, front, x1, y1, z1, x1, y0, z1, x1, y0, z0, x1, y1, z0);
		q(buf, top,   x0, y1, z1, x1, y1, z1, x1, y1, z0, x0, y1, z0);
		q(buf, bottom,x1, y0, z1, x0, y0, z1, x0, y0, z0, x1, y0, z0);
		q(buf, right, x0, y1, z1, x0, y0, z1, x1, y0, z1, x1, y1, z1);
		q(buf, left,  x1, y1, z0, x1, y0, z0, x0, y0, z0, x0, y1, z0);
	}

	private void q(BufferBuilder buf, UV uv,
				   double x1, double y1, double z1,
				   double x2, double y2, double z2,
				   double x3, double y3, double z3,
				   double x4, double y4, double z4) {
		buf.pos(x1, y1, z1).tex(uv.u0, uv.v0).endVertex();
		buf.pos(x2, y2, z2).tex(uv.u0, uv.v1).endVertex();
		buf.pos(x3, y3, z3).tex(uv.u1, uv.v1).endVertex();
		buf.pos(x4, y4, z4).tex(uv.u1, uv.v0).endVertex();
	}

	private UV uvFromPixelsInset(TextureAtlasSprite sprite, float x0, float y0, float x1, float y1, float inset) {
		float minX = Math.min(x0, x1) + inset;
		float maxX = Math.max(x0, x1) + 1.0F - inset;
		float minY = Math.min(y0, y1) + inset;
		float maxY = Math.max(y0, y1) + 1.0F - inset;

		return new UV(
				pxU(sprite, minX),
				pxV(sprite, minY),
				pxU(sprite, maxX),
				pxV(sprite, maxY)
		);
	}

	private float pxU(TextureAtlasSprite sprite, float pixel) {
		return sprite.getInterpolatedU(pixel * 16.0F / sprite.getIconWidth());
	}

	private float pxV(TextureAtlasSprite sprite, float pixel) {
		return sprite.getInterpolatedV(pixel * 16.0F / sprite.getIconHeight());
	}

	private TextureAtlasSprite getDominantBarrelSideSprite(IBlockState state, BlockPos pos, EnumFacing side) {
		BlockRendererDispatcher dispatcher = Minecraft.getMinecraft().getBlockRendererDispatcher();
		IBakedModel model = dispatcher.getModelForState(state);
		long rand = MathHelper.getPositionRandom(pos);

		Map<TextureAtlasSprite, Integer> scores = new HashMap<>();
		addSprites(scores, model.getQuads(state, side, rand));

		if(scores.isEmpty()) {
			List<BakedQuad> general = model.getQuads(state, null, rand);
			if(general != null) {
				for(BakedQuad quad : general) {
					if(quad.getFace() == side) {
						addSprite(scores, quad.getSprite());
					}
				}
			}
		}

		if(scores.isEmpty()) {
			for(EnumFacing h : EnumFacing.HORIZONTALS) {
				addSprites(scores, model.getQuads(state, h, rand));
			}
		}

		TextureAtlasSprite best = getMostCommonSprite(scores);
		if(best != null) return best;

		TextureAtlasSprite particle = model.getParticleTexture();
		if(particle != null) return particle;

		return Minecraft.getMinecraft().getTextureMapBlocks().getMissingSprite();
	}

	private void addSprites(Map<TextureAtlasSprite, Integer> map, List<BakedQuad> quads) {
		if(quads == null) return;
		for(BakedQuad quad : quads) {
			addSprite(map, quad.getSprite());
		}
	}

	private void addSprite(Map<TextureAtlasSprite, Integer> map, TextureAtlasSprite sprite) {
		if(sprite == null) return;
		Integer prev = map.get(sprite);
		map.put(sprite, prev == null ? 1 : prev + 1);
	}

	private TextureAtlasSprite getMostCommonSprite(Map<TextureAtlasSprite, Integer> map) {
		TextureAtlasSprite best = null;
		int bestScore = -1;
		for(Map.Entry<TextureAtlasSprite, Integer> e : map.entrySet()) {
			if(e.getValue() > bestScore) {
				bestScore = e.getValue();
				best = e.getKey();
			}
		}
		return best;
	}

	private boolean isPipe(Block b) {
		return b instanceof BlockFluidPipeMk2 ||
				b instanceof BlockFluidPipeMk3 ||
				b instanceof BlockFluidPipeMk4;
	}

	private static class UV {
		final float u0, v0, u1, v1;
		UV(float u0, float v0, float u1, float v1) {
			this.u0 = u0; this.v0 = v0; this.u1 = u1; this.v1 = v1;
		}
	}
}