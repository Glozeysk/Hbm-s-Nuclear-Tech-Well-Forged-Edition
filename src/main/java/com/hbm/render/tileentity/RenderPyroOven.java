package com.hbm.render.tileentity;

import org.lwjgl.opengl.GL11;

import com.hbm.blocks.BlockDummyable;
import com.hbm.main.ResourceManager;
import com.hbm.tileentity.machine.oil.TileEntityMachinePyroOven;
import com.hbm.util.BobMathUtil;

import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;

public class RenderPyroOven extends TileEntitySpecialRenderer<TileEntityMachinePyroOven> {

	@Override
	public void render(TileEntityMachinePyroOven te, double x, double y, double z, float partialTicks, int destroyStage, float alpha) {

		GlStateManager.pushMatrix();
		GlStateManager.translate(x + 0.5D, y, z + 0.5D);
		GlStateManager.enableLighting();
		GlStateManager.enableCull();

		switch(te.getBlockMetadata() - BlockDummyable.offset) {
		case 2: GlStateManager.rotate(180, 0F, 1F, 0F); break;
		case 4: GlStateManager.rotate(270, 0F, 1F, 0F); break;
		case 3: GlStateManager.rotate(0, 0F, 1F, 0F); break;
		case 5: GlStateManager.rotate(90, 0F, 1F, 0F); break;
		}

		float anim = te.prevAnim + (te.anim - te.prevAnim) * partialTicks;

		GlStateManager.shadeModel(GL11.GL_SMOOTH);
		bindTexture(ResourceManager.pyrooven_tex);
		ResourceManager.pyrooven.renderPart("Oven");

		GlStateManager.pushMatrix();
		GlStateManager.translate(BobMathUtil.sps(anim * 0.125) / 2 - 0.5, 0, 0);
		ResourceManager.pyrooven.renderPart("Slider");
		GlStateManager.popMatrix();

		GlStateManager.pushMatrix();
		GlStateManager.translate(1.5, 0, 1.5);
		GlStateManager.rotate((float) (anim * -15D % 360D), 0, 1, 0);
		GlStateManager.translate(-1.5, 0, -1.5);
		ResourceManager.pyrooven.renderPart("Fan");
		GlStateManager.popMatrix();

		GlStateManager.shadeModel(GL11.GL_FLAT);
		GlStateManager.popMatrix();
	}
}
