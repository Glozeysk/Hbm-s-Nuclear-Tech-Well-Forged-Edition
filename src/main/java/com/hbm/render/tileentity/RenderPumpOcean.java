package com.hbm.render.tileentity;

import org.lwjgl.opengl.GL11;

import com.hbm.blocks.BlockDummyable;
import com.hbm.main.ResourceManager;
import com.hbm.tileentity.machine.TileEntityMachinePumpOcean;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;

//Ported from NTM:CE (RenderPump)
public class RenderPumpOcean extends TileEntitySpecialRenderer<TileEntityMachinePumpOcean> {

	@Override
	public void render(TileEntityMachinePumpOcean tile, double x, double y, double z, float partialTicks, int destroyStage, float alpha) {

		GlStateManager.pushMatrix();
		GlStateManager.translate(x + 0.5D, y, z + 0.5D);
		GlStateManager.enableLighting();

		switch(tile.getBlockMetadata() - BlockDummyable.offset) {
		case 3:
			GlStateManager.rotate(90, 0F, 1F, 0F);
			break;
		case 5:
			GlStateManager.rotate(180, 0F, 1F, 0F);
			break;
		case 2:
			GlStateManager.rotate(270, 0F, 1F, 0F);
			break;
		case 4:
			GlStateManager.rotate(0, 0F, 1F, 0F);
			break;
		}

		renderCommon(tile.lastRotor + (tile.rotor - tile.lastRotor) * partialTicks);

		GlStateManager.popMatrix();
	}

	public static void renderCommon(double rot) {
		GlStateManager.disableCull();
		GlStateManager.shadeModel(GL11.GL_SMOOTH);

		//static so the item renderer can reuse it, hence the texture manager instead of bindTexture
		Minecraft.getMinecraft().renderEngine.bindTexture(ResourceManager.pump_ocean_tex);
		ResourceManager.pump_ocean.renderPart("Base");

		GlStateManager.pushMatrix();
		GlStateManager.translate(0, 2.25, 0);
		GlStateManager.rotate((float) (rot - 90), 0, 0, 1);
		GlStateManager.translate(0, -2.25, 0);
		ResourceManager.pump_ocean.renderPart("Rotor");
		GlStateManager.popMatrix();

		double sin = Math.sin(rot * Math.PI / 180D) * 0.5D - 0.5D;
		double cos = Math.cos(rot * Math.PI / 180D) * 0.5D;
		double ang = Math.acos(cos / 2D);
		double cath = Math.sqrt(1 + (cos * cos) / 2);

		GlStateManager.pushMatrix();
		GlStateManager.translate(0, 1 - cath + sin, 0);
		GlStateManager.translate(0, 4.75, 0);
		GlStateManager.rotate((float) (ang * 180D / Math.PI - 90D), 0, 0, -1);
		GlStateManager.translate(0, -4.75, 0);
		ResourceManager.pump_ocean.renderPart("Arms");
		GlStateManager.popMatrix();

		GlStateManager.pushMatrix();
		GlStateManager.translate(0, 1 - cath + sin, 0);
		ResourceManager.pump_ocean.renderPart("Piston");
		GlStateManager.popMatrix();

		GlStateManager.shadeModel(GL11.GL_FLAT);
		GlStateManager.enableCull();
	}
}
