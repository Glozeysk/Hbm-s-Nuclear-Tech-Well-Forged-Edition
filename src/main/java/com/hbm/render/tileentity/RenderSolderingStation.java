package com.hbm.render.tileentity;

import com.hbm.blocks.BlockDummyable;
import com.hbm.main.ResourceManager;
import com.hbm.tileentity.machine.TileEntityMachineSolderingStation;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.item.ItemStack;

//Ported from NTM:CE (RenderSolderingStation)
public class RenderSolderingStation extends TileEntitySpecialRenderer<TileEntityMachineSolderingStation> {

	@Override
	public void render(TileEntityMachineSolderingStation station, double x, double y, double z, float partialTicks, int destroyStage, float alpha) {

		GlStateManager.pushMatrix();
		GlStateManager.translate(x + 0.5D, y, z + 0.5D);
		GlStateManager.enableLighting();
		GlStateManager.enableCull();

		switch(station.getBlockMetadata() - BlockDummyable.offset) {
		case 2: GlStateManager.rotate(90, 0F, 1F, 0F); break;
		case 4: GlStateManager.rotate(180, 0F, 1F, 0F); break;
		case 3: GlStateManager.rotate(270, 0F, 1F, 0F); break;
		case 5: GlStateManager.rotate(0, 0F, 1F, 0F); break;
		}

		GlStateManager.translate(-0.5, 0, 0.5);

		renderModel();

		//the recipe's product lies on the table while it is being soldered
		if(!station.display.isEmpty()) {
			GlStateManager.pushMatrix();
			GlStateManager.translate(0D, 1.125D, 0D);
			GlStateManager.rotate(90, 0F, 1F, 0F);
			GlStateManager.rotate(-90, 1F, 0F, 0F);
			//1.12 renders items at 1.5x, this cancels it out
			GlStateManager.scale(0.76923075F, 0.76923075F, 0.76923075F);

			ItemStack stack = station.display.copy();
			stack.setCount(1);
			Minecraft.getMinecraft().getRenderItem().renderItem(stack, ItemCameraTransforms.TransformType.NONE);
			GlStateManager.popMatrix();
		}

		GlStateManager.popMatrix();
	}

	public static void renderModel() {
		Minecraft.getMinecraft().renderEngine.bindTexture(ResourceManager.soldering_station_tex);
		ResourceManager.soldering_station.renderAll();
	}
}
