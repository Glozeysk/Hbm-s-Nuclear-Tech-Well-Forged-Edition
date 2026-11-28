package com.hbm.render.world;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.lwjgl.opengl.GL11;

import com.hbm.handler.MultiblockHandlerXR;
import com.hbm.handler.MultiblockPlacement;
import com.hbm.lib.ForgeDirection;

import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * Port of CE's multiblock placement boundary renderer.
 * Draws only the exposed edges of every block in the footprint, so non-cuboid
 * multiblocks get a matching outline instead of a single bounding box.
 */
@SideOnly(Side.CLIENT)
public class MultiblockPlacementBoundary {

	public static void drawPlacement(EntityPlayer player, float interp, MultiblockPlacement placement, boolean canPlace) {
		drawPlacement(player, interp, placement, canPlace, true);
	}

	public static void drawPlacement(EntityPlayer player, float interp, MultiblockPlacement placement,
			boolean canPlace, boolean handlerDimensions) {
		int[] dimensions = placement.dimensions;

		// MultiblockHandler uses +X/-X/+Y/-Y/+Z/-Z, while MultiblockHandlerXR
		// uses U/D/N/S/W/E. IMultiBlock previews use the former.
		if(handlerDimensions && dimensions != null && dimensions.length >= 6) {
			dimensions = new int[] {
					dimensions[2], dimensions[3],
					dimensions[5], dimensions[4],
					dimensions[1], dimensions[0]
			};
		}

		draw(player, interp, placement.corePos, placement.direction,
				dimensions == null ? new int[0][] : new int[][] { dimensions },
				new double[0][0], canPlace, false);
	}

	public static void draw(EntityPlayer player, float interp, BlockPos origin, ForgeDirection facing,
			int[][] dimensions, double[][] extras, boolean canPlace, boolean rotateDimensions) {
		double dX = player.lastTickPosX + (player.posX - player.lastTickPosX) * (double) interp;
		double dY = player.lastTickPosY + (player.posY - player.lastTickPosY) * (double) interp;
		double dZ = player.lastTickPosZ + (player.posZ - player.lastTickPosZ) * (double) interp;

		Tessellator tess = Tessellator.getInstance();
		BufferBuilder buffer = tess.getBuffer();

		GlStateManager.pushMatrix();
		GlStateManager.disableLighting();
		GlStateManager.disableTexture2D();
		OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, 240F, 240F);
		GlStateManager.glLineWidth(2.0F);
		GlStateManager.depthMask(false);
		GlStateManager.enableBlend();
		GlStateManager.tryBlendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA,
				GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SourceFactor.ONE,
				GlStateManager.DestFactor.ZERO);

		buffer.begin(GL11.GL_LINES, DefaultVertexFormats.POSITION_COLOR);

		double timer = (System.currentTimeMillis() % (1000D * Math.PI)) / 250D;
		double sine = Math.sin(timer);
		int color = (int) (255 * (sine * 0.25 + 0.75));

		int r = canPlace ? 0 : color;
		int g = canPlace ? color : 0;
		int b = 0;
		int a = 255;

		List<BlockPos> blocks = new ArrayList<>();
		Set<BlockPos> set = new HashSet<>();

		for(int[] dims : dimensions) {
			if(dims == null || dims.length < 6) {
				continue;
			}

			// CE supports three optional offsets after the six dimensions:
			// forward, up and lateral relative to the machine facing.
			int offFwd = dims.length > 6 ? dims[6] : 0;
			int offUp = dims.length > 7 ? dims[7] : 0;
			int offLat = dims.length > 8 ? dims[8] : 0;

			ForgeDirection rotation = facing.getRotation(ForgeDirection.UP);
			int worldOffX = facing.offsetX * offFwd + rotation.offsetX * offLat;
			int worldOffY = offUp;
			int worldOffZ = facing.offsetZ * offFwd + rotation.offsetZ * offLat;

			int[] rot = rotateDimensions ? MultiblockHandlerXR.rotate(dims, facing.toEnumFacing()) : dims;

			for(int bx = -rot[4] + worldOffX; bx <= rot[5] + worldOffX; bx++) {
				for(int by = -rot[1] + worldOffY; by <= rot[0] + worldOffY; by++) {
					for(int bz = -rot[2] + worldOffZ; bz <= rot[3] + worldOffZ; bz++) {
						BlockPos bp = new BlockPos(origin.getX() + bx, origin.getY() + by, origin.getZ() + bz);
						blocks.add(bp);
						set.add(bp);
					}
				}
			}
		}

		// Draws only the faces between occupied and empty cells, so the outline
		// follows the actual shape instead of a rectangular hull.
		for(BlockPos pos : blocks) {
			boolean px = set.contains(pos.add(1, 0, 0));
			boolean nx = set.contains(pos.add(-1, 0, 0));
			boolean ppy = set.contains(pos.add(0, 1, 0));
			boolean ny = set.contains(pos.add(0, -1, 0));
			boolean ppz = set.contains(pos.add(0, 0, 1));
			boolean nz = set.contains(pos.add(0, 0, -1));

			double minX = pos.getX() - dX;
			double maxX = pos.getX() + 1 - dX;
			double minY = pos.getY() - dY;
			double maxY = pos.getY() + 1 - dY;
			double minZ = pos.getZ() - dZ;
			double maxZ = pos.getZ() + 1 - dZ;

			if(!ppy) {
				if(!nx) line(buffer, minX, maxY, minZ, minX, maxY, maxZ, r, g, b, a);
				if(!ppz) line(buffer, minX, maxY, maxZ, maxX, maxY, maxZ, r, g, b, a);
				if(!px) line(buffer, maxX, maxY, maxZ, maxX, maxY, minZ, r, g, b, a);
				if(!nz) line(buffer, maxX, maxY, minZ, minX, maxY, minZ, r, g, b, a);
			}
			if(!ny) {
				if(!nx) line(buffer, minX, minY, minZ, minX, minY, maxZ, r, g, b, a);
				if(!ppz) line(buffer, minX, minY, maxZ, maxX, minY, maxZ, r, g, b, a);
				if(!px) line(buffer, maxX, minY, maxZ, maxX, minY, minZ, r, g, b, a);
				if(!nz) line(buffer, maxX, minY, minZ, minX, minY, minZ, r, g, b, a);
			}
			if(!nz) {
				if(!nx) line(buffer, minX, minY, minZ, minX, maxY, minZ, r, g, b, a);
				if(!ppy) line(buffer, minX, maxY, minZ, maxX, maxY, minZ, r, g, b, a);
				if(!px) line(buffer, maxX, maxY, minZ, maxX, minY, minZ, r, g, b, a);
				if(!ny) line(buffer, maxX, minY, minZ, minX, minY, minZ, r, g, b, a);
			}
			if(!ppz) {
				if(!nx) line(buffer, minX, minY, maxZ, minX, maxY, maxZ, r, g, b, a);
				if(!ppy) line(buffer, minX, maxY, maxZ, maxX, maxY, maxZ, r, g, b, a);
				if(!px) line(buffer, maxX, maxY, maxZ, maxX, minY, maxZ, r, g, b, a);
				if(!ny) line(buffer, maxX, minY, maxZ, minX, minY, maxZ, r, g, b, a);
			}
			if(!nx) {
				if(!nz) line(buffer, minX, minY, minZ, minX, maxY, minZ, r, g, b, a);
				if(!ppy) line(buffer, minX, maxY, minZ, minX, maxY, maxZ, r, g, b, a);
				if(!ppz) line(buffer, minX, maxY, maxZ, minX, minY, maxZ, r, g, b, a);
				if(!ny) line(buffer, minX, minY, maxZ, minX, minY, minZ, r, g, b, a);
			}
			if(!px) {
				if(!nz) line(buffer, maxX, minY, minZ, maxX, maxY, minZ, r, g, b, a);
				if(!ppy) line(buffer, maxX, maxY, minZ, maxX, maxY, maxZ, r, g, b, a);
				if(!ppz) line(buffer, maxX, maxY, maxZ, maxX, minY, maxZ, r, g, b, a);
				if(!ny) line(buffer, maxX, minY, maxZ, maxX, minY, minZ, r, g, b, a);
			}
		}

		// CE extras are arbitrary boxes, currently used by machines whose
		// construction extends beyond the regular dimension arrays.
		b = color;
		r = 0;
		g = 0;

		if(extras != null) {
			for(double[] extra : extras) {
				if(extra == null || extra.length < 6) {
					continue;
				}

				ForgeDirection rotation = facing.getRotation(ForgeDirection.UP);
				double cX = origin.getX() - dX + 0.5D;
				double cY = origin.getY() - dY;
				double cZ = origin.getZ() - dZ + 0.5D;

				double upr = extra[0];
				double lwr = extra[1];
				double fwd = extra[2];
				double bwd = extra[3];
				double lft = extra[4];
				double rgt = extra[5];

				double x0 = cX + fwd * facing.offsetX + lft * rotation.offsetX;
				double x1 = cX + bwd * facing.offsetX + rgt * rotation.offsetX;
				double y0 = cY + lwr;
				double y1 = cY + upr;
				double z0 = cZ + fwd * facing.offsetZ + lft * rotation.offsetZ;
				double z1 = cZ + bwd * facing.offsetZ + rgt * rotation.offsetZ;

				line(buffer, x0, y0, z0, x0, y0, z1, r, g, b, a);
				line(buffer, x1, y0, z0, x1, y0, z1, r, g, b, a);
				line(buffer, x0, y0, z0, x1, y0, z0, r, g, b, a);
				line(buffer, x0, y0, z1, x1, y0, z1, r, g, b, a);

				line(buffer, x0, y1, z0, x0, y1, z1, r, g, b, a);
				line(buffer, x1, y1, z0, x1, y1, z1, r, g, b, a);
				line(buffer, x0, y1, z0, x1, y1, z0, r, g, b, a);
				line(buffer, x0, y1, z1, x1, y1, z1, r, g, b, a);

				line(buffer, x0, y0, z0, x0, y1, z0, r, g, b, a);
				line(buffer, x1, y0, z0, x1, y1, z0, r, g, b, a);
				line(buffer, x0, y0, z1, x0, y1, z1, r, g, b, a);
				line(buffer, x1, y0, z1, x1, y1, z1, r, g, b, a);
			}
		}

		tess.draw();

		GlStateManager.depthMask(true);
		OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit,
				OpenGlHelper.lastBrightnessX, OpenGlHelper.lastBrightnessY);
		GlStateManager.glLineWidth(1.0F);
		GlStateManager.enableTexture2D();
		GlStateManager.enableLighting();
		GlStateManager.disableBlend();
		GlStateManager.popMatrix();
	}

	private static void line(BufferBuilder buffer, double x1, double y1, double z1,
			double x2, double y2, double z2, int r, int g, int b, int a) {
		buffer.pos(x1, y1, z1).color(r, g, b, a).endVertex();
		buffer.pos(x2, y2, z2).color(r, g, b, a).endVertex();
	}
}
