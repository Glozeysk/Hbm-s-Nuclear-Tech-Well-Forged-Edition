package com.hbm.inventory.gui;

import org.lwjgl.opengl.GL11;

import com.hbm.inventory.container.ContainerMachineShreddermk2;
import com.hbm.lib.RefStrings;
import com.hbm.tileentity.machine.TileEntityMachineShreddermk2;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.util.ResourceLocation;

public class GUIMachineShreddermk2 extends GuiInfoContainer {

	private static ResourceLocation texture = new ResourceLocation(RefStrings.MODID + ":textures/gui/gui_shreddermk2.png");
	private TileEntityMachineShreddermk2 diFurnace;

	public GUIMachineShreddermk2(InventoryPlayer invPlayer, TileEntityMachineShreddermk2 tedf) {
		super(new ContainerMachineShreddermk2(invPlayer, tedf));
		diFurnace = tedf;
		
		this.xSize = 176;
		this.ySize = 166;
	}
	
	@Override
	public void drawScreen(int mouseX, int mouseY, float f) {
		super.drawScreen(mouseX, mouseY, f);

		this.drawElectricityInfo(this, mouseX, mouseY, guiLeft + 8, guiTop + 51 - 34, 16, 34, diFurnace.power, TileEntityMachineShreddermk2.maxPower);
		super.renderHoveredToolTip(mouseX, mouseY);
	}
	
	@Override
	protected void drawGuiContainerForegroundLayer(int i, int j) {
		String name = this.diFurnace.hasCustomInventoryName() ? this.diFurnace.getInventoryName() : I18n.format(this.diFurnace.getInventoryName());
		
		this.fontRenderer.drawString(name, this.xSize / 2 - this.fontRenderer.getStringWidth(name) / 2, 6, 4210752);
		this.fontRenderer.drawString(I18n.format("container.inventory"), 8, this.ySize - 96 + 2, 4210752);
	}

	@Override
	protected void drawGuiContainerBackgroundLayer(float p_146976_1_, int p_146976_2_, int p_146976_3_) {
		super.drawDefaultBackground();
		GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
		Minecraft.getMinecraft().getTextureManager().bindTexture(texture);
		drawTexturedModalRect(guiLeft, guiTop, 0, 0, xSize, ySize);
		
		if(diFurnace.power > 0) {
			int i = (int)diFurnace.getPowerScaled(34);
			drawTexturedModalRect(guiLeft + 8, guiTop + 51 - i, 176, 34 - i, 16, i);
		}
		
		int j1 = diFurnace.getDiFurnaceProgressScaled(24);
		drawTexturedModalRect(guiLeft + 80, guiTop + 34, 192, 0, j1, 16);
		
	}
}
