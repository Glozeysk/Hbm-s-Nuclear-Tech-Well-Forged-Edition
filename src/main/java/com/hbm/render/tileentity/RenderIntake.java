package com.hbm.render.tileentity;

import com.hbm.main.ResourceManager;
import com.hbm.tileentity.machine.TileEntityMachineIntake;

import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;

//Ported from NTM:CE (RenderIntake)
public class RenderIntake extends TileEntitySpecialRenderer<TileEntityMachineIntake> {

	@Override
	public void render(TileEntityMachineIntake intake, double x, double y, double z, float partialTicks, int destroyStage, float alpha) {
		GlStateManager.pushMatrix();
		GlStateManager.translate(x + 0.5D, y, z + 0.5D);
		GlStateManager.enableLighting();
		GlStateManager.disableCull();

		switch(intake.getBlockMetadata() - 10) {
		case 2: GlStateManager.rotate(90, 0F, 1F, 0F); break;
		case 4: GlStateManager.rotate(180, 0F, 1F, 0F); break;
		case 3: GlStateManager.rotate(270, 0F, 1F, 0F); break;
		case 5: GlStateManager.rotate(0, 0F, 1F, 0F); break;
		}

		GlStateManager.translate(-0.5, 0, 0.5);

		bindTexture(ResourceManager.intake_tex);
		ResourceManager.intake.renderPart("Base");

		float rot = intake.prevFan + (intake.fan - intake.prevFan) * partialTicks;

		GlStateManager.pushMatrix();
		GlStateManager.rotate(-rot, 0, 1, 0);
		ResourceManager.intake.renderPart("Fan");
		GlStateManager.popMatrix();

		GlStateManager.enableCull();
		GlStateManager.popMatrix();
	}
}
