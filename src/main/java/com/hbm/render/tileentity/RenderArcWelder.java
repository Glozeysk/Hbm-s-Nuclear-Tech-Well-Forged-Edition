package com.hbm.render.tileentity;

import com.hbm.blocks.BlockDummyable;
import com.hbm.main.ResourceManager;
import com.hbm.tileentity.machine.TileEntityMachineArcWelder;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;

//Ported from NTM:CE (RenderArcWelder)
public class RenderArcWelder extends TileEntitySpecialRenderer<TileEntityMachineArcWelder> {

	@Override
	public void render(TileEntityMachineArcWelder welder, double x, double y, double z, float partialTicks, int destroyStage, float alpha) {

		GlStateManager.pushMatrix();
		GlStateManager.translate(x + 0.5D, y, z + 0.5D);
		GlStateManager.enableLighting();
		GlStateManager.enableCull();

		switch(welder.getBlockMetadata() - BlockDummyable.offset) {
		case 2: GlStateManager.rotate(90, 0F, 1F, 0F); break;
		case 4: GlStateManager.rotate(180, 0F, 1F, 0F); break;
		case 3: GlStateManager.rotate(270, 0F, 1F, 0F); break;
		case 5: GlStateManager.rotate(0, 0F, 1F, 0F); break;
		}

		GlStateManager.translate(-0.5, 0, 0);

		renderParts(welder.mode == TileEntityMachineArcWelder.MODE_EBW, getDome(welder));

		if(!welder.display.isEmpty()) {
			GlStateManager.pushMatrix();
			GlStateManager.translate(0D, 1.125D, 0D);
			GlStateManager.rotate(90, 0F, 1F, 0F);
			GlStateManager.rotate(-90, 1F, 0F, 0F);
			//1.12 renders items at 1.5x, this cancels it out
			GlStateManager.scale(0.76923075F, 0.76923075F, 0.76923075F);

			ItemStack stack = welder.display.copy();
			stack.setCount(1);
			Minecraft.getMinecraft().getRenderItem().renderItem(stack, ItemCameraTransforms.TransformType.NONE);
			GlStateManager.popMatrix();
		}

		GlStateManager.popMatrix();
	}

	/** MMA welds in open air, TIG shows its shielding gas once the chamber is flooded enough to weld */
	private static ResourceLocation getDome(TileEntityMachineArcWelder welder) {

		if(welder.mode == TileEntityMachineArcWelder.MODE_MMA)
			return null;

		return ResourceManager.arc_welder_dome_tex;
	}

	/** the dome has its own sheet and is left out when domeTex is null; the electron beam process swaps the welding head for the blaster */
	public static void renderParts(boolean blaster, ResourceLocation domeTex) {
		renderBody(blaster);
		renderDome(domeTex);
	}

	public static void renderBody(boolean blaster) {
		Minecraft.getMinecraft().renderEngine.bindTexture(ResourceManager.arc_welder_tex);
		ResourceManager.arc_welder.renderPart("Main");
		ResourceManager.arc_welder.renderPart(blaster ? "Blaster" : "Welder");
	}

	public static void renderDome(ResourceLocation domeTex) {

		if(domeTex == null)
			return;

		Minecraft.getMinecraft().renderEngine.bindTexture(domeTex);
		ResourceManager.arc_welder.renderPart("Dome");
	}
}
