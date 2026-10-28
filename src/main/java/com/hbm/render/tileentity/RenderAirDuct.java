package com.hbm.render.tileentity;

import org.lwjgl.opengl.GL11;

import com.hbm.blocks.network.AirDuctNetwork;
import com.hbm.main.ResourceManager;
import com.hbm.tileentity.conductor.TileEntityAirDuct;

import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.util.EnumFacing;

//The regular pipe model (see RenderFluidDuctMk2) with the air duct sheet and no fluid tint
public class RenderAirDuct extends TileEntitySpecialRenderer<TileEntityAirDuct> {

	@Override
	public void render(TileEntityAirDuct te, double x, double y, double z, float partialTicks, int destroyStage, float alpha) {

		boolean pX = AirDuctNetwork.connects(te.getWorld(), te.getPos(), EnumFacing.EAST);
		boolean nX = AirDuctNetwork.connects(te.getWorld(), te.getPos(), EnumFacing.WEST);
		boolean pY = AirDuctNetwork.connects(te.getWorld(), te.getPos(), EnumFacing.UP);
		boolean nY = AirDuctNetwork.connects(te.getWorld(), te.getPos(), EnumFacing.DOWN);
		boolean pZ = AirDuctNetwork.connects(te.getWorld(), te.getPos(), EnumFacing.SOUTH);
		boolean nZ = AirDuctNetwork.connects(te.getWorld(), te.getPos(), EnumFacing.NORTH);

		int mask = (pX ? 32 : 0) + (nX ? 16 : 0) + (pY ? 8 : 0) + (nY ? 4 : 0) + (pZ ? 2 : 0) + (nZ ? 1 : 0);

		GL11.glPushMatrix();
		GlStateManager.enableLighting();
		GlStateManager.shadeModel(GL11.GL_SMOOTH);
		GL11.glTranslated(x + 0.5F, y + 0.5F, z + 0.5F);
		GlStateManager.color(1, 1, 1, 1);

		bindTexture(ResourceManager.air_duct_tex);
		renderParts(mask, pX, nX, pY, nY, pZ, nZ);

		GlStateManager.shadeModel(GL11.GL_FLAT);
		GL11.glPopMatrix();
	}

	//same part selection as RenderFluidDuctMk2, the model's Z parts are named the other way round
	private void renderParts(int mask, boolean pX, boolean nX, boolean pY, boolean nY, boolean pZ, boolean nZ) {
		if(mask == 0) {
			ResourceManager.pipe_neo.renderPart("pX");
			ResourceManager.pipe_neo.renderPart("nX");
			ResourceManager.pipe_neo.renderPart("pY");
			ResourceManager.pipe_neo.renderPart("nY");
			ResourceManager.pipe_neo.renderPart("pZ");
			ResourceManager.pipe_neo.renderPart("nZ");
		} else if(mask == 0b100000 || mask == 0b010000) {
			ResourceManager.pipe_neo.renderPart("pX");
			ResourceManager.pipe_neo.renderPart("nX");
		} else if(mask == 0b001000 || mask == 0b000100) {
			ResourceManager.pipe_neo.renderPart("pY");
			ResourceManager.pipe_neo.renderPart("nY");
		} else if(mask == 0b000010 || mask == 0b000001) {
			ResourceManager.pipe_neo.renderPart("pZ");
			ResourceManager.pipe_neo.renderPart("nZ");
		} else {
			if(pX) ResourceManager.pipe_neo.renderPart("pX");
			if(nX) ResourceManager.pipe_neo.renderPart("nX");
			if(pY) ResourceManager.pipe_neo.renderPart("pY");
			if(nY) ResourceManager.pipe_neo.renderPart("nY");
			if(pZ) ResourceManager.pipe_neo.renderPart("nZ");
			if(nZ) ResourceManager.pipe_neo.renderPart("pZ");

			if(!pX && !pY && !pZ) ResourceManager.pipe_neo.renderPart("ppn");
			if(!pX && !pY && !nZ) ResourceManager.pipe_neo.renderPart("ppp");
			if(!nX && !pY && !pZ) ResourceManager.pipe_neo.renderPart("npn");
			if(!nX && !pY && !nZ) ResourceManager.pipe_neo.renderPart("npp");
			if(!pX && !nY && !pZ) ResourceManager.pipe_neo.renderPart("pnn");
			if(!pX && !nY && !nZ) ResourceManager.pipe_neo.renderPart("pnp");
			if(!nX && !nY && !pZ) ResourceManager.pipe_neo.renderPart("nnn");
			if(!nX && !nY && !nZ) ResourceManager.pipe_neo.renderPart("nnp");
		}
	}
}
