package com.hbm.render.tileentity;

import net.minecraft.client.renderer.OpenGlHelper;
import org.lwjgl.opengl.GL11;

import com.hbm.main.ResourceManager;
import com.hbm.forgefluid.ModForgeFluids;
import com.hbm.render.RenderHelper;
import com.hbm.tileentity.machine.TileEntityMachineDiFurnaceBig;

import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.GlStateManager.DestFactor;
import net.minecraft.client.renderer.GlStateManager.SourceFactor;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraftforge.fluids.FluidStack;

public class RenderDiFurnaceBig extends TileEntitySpecialRenderer<TileEntityMachineDiFurnaceBig> {

	@Override
	public boolean isGlobalRenderer(TileEntityMachineDiFurnaceBig te) {
		return RenderPerformance.renderGlobalInLow();
	}
	
	@Override
	public void render(TileEntityMachineDiFurnaceBig diFurnace, double x, double y, double z, float partialTicks, int destroyStage, float alpha) {
		GL11.glPushMatrix();
		GL11.glTranslated(x + 0.5D, y, z + 0.5D);
		GlStateManager.enableLighting();
		GlStateManager.disableCull();

		FluidStack fluid = diFurnace.tank.getFluid();

		switch(diFurnace.getBlockMetadata() - 10) {
		case 2: GL11.glRotatef(0, 0F, 1F, 0F); break;
		case 4: GL11.glRotatef(90, 0F, 1F, 0F); break;
		case 3: GL11.glRotatef(180, 0F, 1F, 0F); break;
		case 5: GL11.glRotatef(270, 0F, 1F, 0F); break;
		}

		GlStateManager.shadeModel(GL11.GL_SMOOTH);

		if(fluid != null && fluid.getFluid() != null && fluid.getFluid() == ModForgeFluids.nitan) {
			bindTexture(ResourceManager.difurnacebig_nitan_tex);
		} else if(fluid != null && fluid.getFluid() != null && fluid.getFluid() == ModForgeFluids.balefire) {
			bindTexture(ResourceManager.difurnacebig_balefire_tex);
		} else if(fluid != null && fluid.getFluid() != null && fluid.getFluid() == ModForgeFluids.sparkfuel) {
			bindTexture(ResourceManager.difurnacebig_spark_tex);
		} else if(fluid != null && fluid.getFluid() != null && fluid.getFluid() == ModForgeFluids.uu_matter) {
			bindTexture(ResourceManager.difurnacebig_uu_tex);
		} else {
			bindTexture(ResourceManager.difurnacebig_tex);
		}

		ResourceManager.difurnacebig.renderPart("main");

		if(diFurnace.isRunning) {
			GL11.glPushMatrix();
			GL11.glPushAttrib(GL11.GL_LIGHTING_BIT);
			
			GL11.glDisable(GL11.GL_LIGHTING);
			GL11.glDisable(GL11.GL_CULL_FACE);
			OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, 240F, 240F);
			ResourceManager.difurnacebig.renderPart("on");
			GL11.glEnable(GL11.GL_LIGHTING);
			
			GL11.glPopAttrib();
			GL11.glPopMatrix();
		} else {
			ResourceManager.difurnacebig.renderPart("off");
		}
		
		GlStateManager.shadeModel(GL11.GL_FLAT);
		GlStateManager.enableCull();

		GL11.glPopMatrix();
	}
}
