package com.hbm.render.tileentity;

import org.lwjgl.opengl.GL11;

import com.hbm.blocks.BlockDummyable;
import com.hbm.main.ResourceManager;
import com.hbm.tileentity.machine.TileEntityMachineCompressorCompact;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;

//Ported from NTM:CE (RenderCompressorCompact): the compact compressor reuses the condenser model with its own sheet
public class RenderCompressorCompact extends TileEntitySpecialRenderer<TileEntityMachineCompressorCompact> {

	@Override
	public void render(TileEntityMachineCompressorCompact compressor, double x, double y, double z, float partialTicks, int destroyStage, float alpha) {

		GlStateManager.pushMatrix();
		GlStateManager.translate(x + 0.5D, y, z + 0.5D);
		GlStateManager.enableLighting();
		GlStateManager.disableCull();

		switch(compressor.getBlockMetadata() - BlockDummyable.offset) {
		case 2: GlStateManager.rotate(90, 0F, 1F, 0F); break;
		case 4: GlStateManager.rotate(180, 0F, 1F, 0F); break;
		case 3: GlStateManager.rotate(270, 0F, 1F, 0F); break;
		case 5: GlStateManager.rotate(0, 0F, 1F, 0F); break;
		}

		GlStateManager.shadeModel(GL11.GL_SMOOTH);
		bindTexture(ResourceManager.compressor_compact_tex);
		ResourceManager.condenser.renderPart("Condenser");

		float rot = compressor.prevFanSpin + (compressor.fanSpin - compressor.prevFanSpin) * partialTicks;

		GlStateManager.pushMatrix();
		GlStateManager.translate(0, 1.5, 0);
		GlStateManager.rotate(rot, 1, 0, 0);
		GlStateManager.translate(0, -1.5, 0);
		ResourceManager.condenser.renderPart("Fan1");
		GlStateManager.popMatrix();

		GlStateManager.pushMatrix();
		GlStateManager.translate(0, 1.5, 0);
		GlStateManager.rotate(rot, -1, 0, 0);
		GlStateManager.translate(0, -1.5, 0);
		ResourceManager.condenser.renderPart("Fan2");
		GlStateManager.popMatrix();

		GlStateManager.shadeModel(GL11.GL_FLAT);
		GlStateManager.enableCull();
		GlStateManager.popMatrix();
	}

	public static void renderItem() {
		Minecraft.getMinecraft().renderEngine.bindTexture(ResourceManager.compressor_compact_tex);
		ResourceManager.condenser.renderAll();
	}
}
