package com.hbm.inventory.gui;

import org.lwjgl.opengl.GL11;

import com.hbm.forgefluid.FFUtils;
import com.hbm.inventory.container.ContainerLiquefactor;
import com.hbm.lib.RefStrings;
import com.hbm.tileentity.machine.oil.TileEntityMachineLiquefactor;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.util.ResourceLocation;

//Ported from NTM:CE (GUILiquefactor); CE draws no title, ours has one in the dark grey of our other machines
public class GUILiquefactor extends GuiInfoContainer {

	private static final ResourceLocation texture = new ResourceLocation(RefStrings.MODID + ":textures/gui/processing/gui_liquefactor.png");

	private TileEntityMachineLiquefactor liquefactor;

	public GUILiquefactor(InventoryPlayer invPlayer, TileEntityMachineLiquefactor te) {
		super(new ContainerLiquefactor(invPlayer, te));
		liquefactor = te;

		this.xSize = 176;
		this.ySize = 204;
	}

	@Override
	public void drawScreen(int mouseX, int mouseY, float f) {
		super.drawScreen(mouseX, mouseY, f);

		FFUtils.renderTankInfo(this, mouseX, mouseY, guiLeft + 71, guiTop + 36, 16, 52, liquefactor.tank);
		this.drawElectricityInfo(this, mouseX, mouseY, guiLeft + 134, guiTop + 18, 16, 52, liquefactor.power, liquefactor.getMaxPower());

		super.renderHoveredToolTip(mouseX, mouseY);
	}

	@Override
	protected void drawGuiContainerForegroundLayer(int i, int j) {
		String name = this.liquefactor.hasCustomInventoryName() ? this.liquefactor.getInventoryName() : I18n.format(this.liquefactor.getInventoryName());

		this.fontRenderer.drawString(name, 70 - this.fontRenderer.getStringWidth(name) / 2, 6, 4210752);
		this.fontRenderer.drawString(I18n.format("container.inventory"), 8, this.ySize - 96 + 2, 4210752);
	}

	@Override
	protected void drawGuiContainerBackgroundLayer(float f, int mouseX, int mouseY) {
		super.drawDefaultBackground();
		GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
		Minecraft.getMinecraft().getTextureManager().bindTexture(texture);
		drawTexturedModalRect(guiLeft, guiTop, 0, 0, xSize, ySize);

		int p = (int) (liquefactor.power * 52 / liquefactor.getMaxPower());
		drawTexturedModalRect(guiLeft + 134, guiTop + 70 - p, 176, 52 - p, 16, p);

		int prog = liquefactor.getProgressScaled(42);
		drawTexturedModalRect(guiLeft + 42, guiTop + 17, 192, 0, prog, 35);

		if(p > 0)
			drawTexturedModalRect(guiLeft + 138, guiTop + 4, 176, 52, 9, 12);

		//FFUtils.drawLiquid puts the liquid bottom at guiTop + offsetY - 28, the tank ends at y=88
		FFUtils.drawLiquid(liquefactor.tank, guiLeft, guiTop, zLevel, 16, 52, 71, 88 + 28);
	}
}
