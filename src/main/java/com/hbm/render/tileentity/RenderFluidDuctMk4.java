package com.hbm.render.tileentity;

import org.lwjgl.opengl.GL11;

import com.hbm.forgefluid.ModForgeFluids;
import com.hbm.forgefluid.FFUtils;
import com.hbm.main.ResourceManager;
import com.hbm.blocks.ModBlocks;
import com.hbm.tileentity.conductor.TileEntityFFDuctBaseMk2;
import com.hbm.tileentity.conductor.TileEntityFFFluidSuccMk4;

import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.fluids.Fluid;

public class RenderFluidDuctMk4<T extends TileEntityFFDuctBaseMk2> extends TileEntitySpecialRenderer<T> {
	
	@Override
	public void render(T te, double x, double y, double z, float partialTicks, int destroyStage, float alpha) {
		if(te.getBlockType() == ModBlocks.fluid_duct_solid || te.getBlockType() == ModBlocks.fluid_duct_solid_sealed || te.getBlockType() == ModBlocks.fluid_duct_mk3_solid || te.getBlockType() == ModBlocks.fluid_duct_mk3_solid_sealed)
			return;
		GL11.glPushMatrix();
		GlStateManager.enableLighting();
		GlStateManager.shadeModel(GL11.GL_SMOOTH);

		boolean pX = te.connections[3] != null;
		boolean nX = te.connections[5] != null;
		boolean pY = te.connections[0] != null;
		boolean nY = te.connections[1] != null;
		boolean pZ = te.connections[4] != null;
		boolean nZ = te.connections[2] != null;

		int mask = 0 + (pX ? 32 : 0) + (nX ? 16 : 0) + (pY ? 8 : 0) + (nY ? 4 : 0) + (pZ ? 2 : 0) + (nZ ? 1 : 0);

		GL11.glTranslated(x + 0.5F, y + 0.5F, z + 0.5F);

		// First pass - base texture without color tint
		GlStateManager.color(1, 1, 1, 1);
		if(te instanceof TileEntityFFFluidSuccMk4){
			bindTexture(ResourceManager.pipe_neo_mk4_succ_tex);
		} else {
			bindTexture(ResourceManager.pipe_neo_mk4_tex);
		}
		renderParts(mask, pX, nX, pY, nY, pZ, nZ);

		// Second pass - overlay with fluid color
		if(te.getType() != null){
			GlStateManager.enableBlend();
			GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
			FFUtils.setRGBFromHex(ModForgeFluids.getFluidColor(te.getType()));
			bindTexture(ResourceManager.pipe_neo_mk4_overlay_tex);
			renderParts(mask, pX, nX, pY, nY, pZ, nZ);
			GlStateManager.disableBlend();
		}

		GlStateManager.shadeModel(GL11.GL_FLAT);
		GlStateManager.color(1, 1, 1, 1);
		GL11.glTranslated(-x - 0.5F, -y - 0.5F, -z - 0.5F);
		GL11.glPopMatrix();
	}

	private void renderParts(int mask, boolean pX, boolean nX, boolean pY, boolean nY, boolean pZ, boolean nZ) {
		if(mask == 0) {
			ResourceManager.pipe_neo_mk4.renderPart("pX");
			ResourceManager.pipe_neo_mk4.renderPart("nX");
			ResourceManager.pipe_neo_mk4.renderPart("pY");
			ResourceManager.pipe_neo_mk4.renderPart("nY");
			ResourceManager.pipe_neo_mk4.renderPart("pZ");
			ResourceManager.pipe_neo_mk4.renderPart("nZ");
			ResourceManager.pipe_neo_mk4.renderPart("Core");
		} else if(mask == 0b100000 || mask == 0b010000) {
			ResourceManager.pipe_neo_mk4.renderPart("pX");
			ResourceManager.pipe_neo_mk4.renderPart("nX");
		} else if(mask == 0b001000 || mask == 0b000100) {
			ResourceManager.pipe_neo_mk4.renderPart("pY");
			ResourceManager.pipe_neo_mk4.renderPart("nY");
		} else if(mask == 0b000010 || mask == 0b000001) {
			ResourceManager.pipe_neo_mk4.renderPart("pZ");
			ResourceManager.pipe_neo_mk4.renderPart("nZ");
		} else {
			if(pX) ResourceManager.pipe_neo_mk4.renderPart("pX");
			if(nX) ResourceManager.pipe_neo_mk4.renderPart("nX");
			if(pY) ResourceManager.pipe_neo_mk4.renderPart("pY");
			if(nY) ResourceManager.pipe_neo_mk4.renderPart("nY");
			if(pZ) ResourceManager.pipe_neo_mk4.renderPart("nZ");
			if(nZ) ResourceManager.pipe_neo_mk4.renderPart("pZ");

			if(!pX && !pY && !pZ) ResourceManager.pipe_neo_mk4.renderPart("Core");
			if(!pX && !pY && !nZ) ResourceManager.pipe_neo_mk4.renderPart("Core");
			if(!nX && !pY && !pZ) ResourceManager.pipe_neo_mk4.renderPart("Core");
			if(!nX && !pY && !nZ) ResourceManager.pipe_neo_mk4.renderPart("Core");
			if(!pX && !nY && !pZ) ResourceManager.pipe_neo_mk4.renderPart("Core");
			if(!pX && !nY && !nZ) ResourceManager.pipe_neo_mk4.renderPart("Core");
			if(!nX && !nY && !pZ) ResourceManager.pipe_neo_mk4.renderPart("Core");
			if(!nX && !nY && !nZ) ResourceManager.pipe_neo_mk4.renderPart("Core");
		}
	}
}