package com.hbm.render.tileentity;

import java.awt.Color;

import org.lwjgl.opengl.GL11;

import com.hbm.blocks.BlockDummyable;
import com.hbm.forgefluid.ModForgeFluids;
import com.hbm.main.ResourceManager;
import com.hbm.tileentity.machine.oil.TileEntityMachineLiquefactor;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;

//Ported from NTM:CE (RenderLiquefactor): the fluid column is scaled by the fill level and tinted with the fluid colour, the glass is a translucent tint
public class RenderLiquefactor extends TileEntitySpecialRenderer<TileEntityMachineLiquefactor> {

	@Override
	public void render(TileEntityMachineLiquefactor liquefactor, double x, double y, double z, float partialTicks, int destroyStage, float alpha) {

		GlStateManager.pushMatrix();
		GlStateManager.translate(x + 0.5D, y, z + 0.5D);
		GlStateManager.enableLighting();
		GlStateManager.disableCull();

		switch(liquefactor.getBlockMetadata() - BlockDummyable.offset) {
		case 2: GlStateManager.rotate(90, 0F, 1F, 0F); break;
		case 4: GlStateManager.rotate(180, 0F, 1F, 0F); break;
		case 3: GlStateManager.rotate(270, 0F, 1F, 0F); break;
		case 5: GlStateManager.rotate(0, 0F, 1F, 0F); break;
		}

		GlStateManager.shadeModel(GL11.GL_SMOOTH);
		bindTexture(ResourceManager.liquefactor_tex);
		ResourceManager.liquefactor.renderPart("Main");

		GlStateManager.disableLighting();
		GlStateManager.disableTexture2D();

		if(liquefactor.tank.getFluid() != null && liquefactor.tank.getFluidAmount() > 0) {
			Color color = new Color(ModForgeFluids.getFluidColor(liquefactor.tank.getFluid().getFluid()));
			GlStateManager.color(color.getRed() / 255F, color.getGreen() / 255F, color.getBlue() / 255F, 1.0F);

			double height = (double) liquefactor.tank.getFluidAmount() / (double) liquefactor.tank.getCapacity();
			GlStateManager.pushMatrix();
			GlStateManager.translate(0, 1, 0);
			GlStateManager.scale(1, height, 1);
			GlStateManager.translate(0, -1, 0);
			ResourceManager.liquefactor.renderPart("Fluid");
			GlStateManager.popMatrix();
		}

		GlStateManager.enableBlend();
		GlStateManager.alphaFunc(GL11.GL_GREATER, 0);
		GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
		GlStateManager.color(0.75F, 1.0F, 1.0F, 0.15F);
		GlStateManager.depthMask(false);

		ResourceManager.liquefactor.renderPart("Glass");

		GlStateManager.depthMask(true);
		GlStateManager.alphaFunc(GL11.GL_GREATER, 0.1F);
		GlStateManager.disableBlend();
		GlStateManager.enableTexture2D();
		GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);

		GlStateManager.shadeModel(GL11.GL_FLAT);
		GlStateManager.enableCull();
		GlStateManager.enableLighting();
		GlStateManager.popMatrix();
	}

	public static void renderMain() {
		Minecraft.getMinecraft().renderEngine.bindTexture(ResourceManager.liquefactor_tex);
		ResourceManager.liquefactor.renderPart("Main");
	}
}
