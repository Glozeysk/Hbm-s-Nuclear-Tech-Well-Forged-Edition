package com.hbm.render.tileentity;

import com.hbm.main.ResourceManager;
import com.hbm.tileentity.network.energy.TileEntityConnector;
import com.hbm.tileentity.network.energy.TileEntityPylonBase;
import net.minecraft.client.renderer.GlStateManager;

import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;

public class RenderConnector extends TileEntitySpecialRenderer<TileEntityConnector> {

    @Override
	public boolean isGlobalRenderer(TileEntityConnector te) {
		return RenderPerformance.renderGlobalInLow();
	}

    @Override
    public void render(TileEntityConnector tile, double x, double y, double z, float f, int destroyStage, float alpha) {
		if(RenderPerformance.skipDistant(tile, 1024.0D))
			return;
        GlStateManager.enableLighting();

        GlStateManager.pushMatrix();

        GlStateManager.translate(x + 0.5D, y + 0.5D, z + 0.5D);

        switch (tile.getBlockMetadata()) {
            case 0:
                GlStateManager.rotate(180, 1, 0, 0);
                break;
            case 1:
                break;
            case 2:
                GlStateManager.rotate(90, 1, 0, 0);
                GlStateManager.rotate(180, 0, 0, 1);
                break;
            case 3:
                GlStateManager.rotate(90, 1, 0, 0);
                break;
            case 4:
                GlStateManager.rotate(90, 1, 0, 0);
                GlStateManager.rotate(90, 0, 0, 1);
                break;
            case 5:
                GlStateManager.rotate(90, 1, 0, 0);
                GlStateManager.rotate(270, 0, 0, 1);
                break;
        }

        GlStateManager.translate(0, -0.5F, 0);

        bindTexture(ResourceManager.red_connector_tex);
        ResourceManager.red_connector.renderAll();
        GlStateManager.popMatrix();

        if(!RenderPerformance.isLow()) {
            GlStateManager.pushMatrix();
            RenderPylon.renderPowerLines(tile, x, y, z);
            GlStateManager.popMatrix();
        }
    }
}
