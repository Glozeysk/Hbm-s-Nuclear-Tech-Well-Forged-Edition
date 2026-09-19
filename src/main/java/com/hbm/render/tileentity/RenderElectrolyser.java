package com.hbm.render.tileentity;

import org.lwjgl.opengl.GL11;

import com.hbm.blocks.BlockDummyable;
import com.hbm.main.ResourceManager;
import com.hbm.tileentity.machine.TileEntityElectrolyser;

import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;

//CE bakes this model statically (yaw 2->180, 3->0, 4->270, 5->90, double sided); drawn here as a plain TESR
public class RenderElectrolyser extends TileEntitySpecialRenderer<TileEntityElectrolyser> {

	@Override
	public void render(TileEntityElectrolyser te, double x, double y, double z, float partialTicks, int destroyStage, float alpha) {

		GlStateManager.pushMatrix();
		GlStateManager.translate(x + 0.5D, y, z + 0.5D);
		GlStateManager.enableLighting();
		GlStateManager.disableCull();

		switch(te.getBlockMetadata() - BlockDummyable.offset) {
		case 2: GlStateManager.rotate(180, 0F, 1F, 0F); break;
		case 4: GlStateManager.rotate(270, 0F, 1F, 0F); break;
		case 3: GlStateManager.rotate(0, 0F, 1F, 0F); break;
		case 5: GlStateManager.rotate(90, 0F, 1F, 0F); break;
		}

		GlStateManager.shadeModel(GL11.GL_SMOOTH);
		bindTexture(ResourceManager.electrolyser_tex);
		ResourceManager.electrolyser.renderAll();
		GlStateManager.shadeModel(GL11.GL_FLAT);

		GlStateManager.enableCull();
		GlStateManager.popMatrix();
	}
}
