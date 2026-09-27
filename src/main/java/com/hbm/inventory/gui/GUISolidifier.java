package com.hbm.inventory.gui;

import org.lwjgl.opengl.GL11;

import com.hbm.forgefluid.FFUtils;
import com.hbm.inventory.container.ContainerSolidifier;
import com.hbm.lib.RefStrings;
import com.hbm.tileentity.machine.oil.TileEntityMachineSolidifier;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.util.ResourceLocation;

//Ported from NTM:CE (GUISolidifier); the title uses the dark grey of our other machines instead of CE's beige
public class GUISolidifier extends GuiInfoContainer {

	private static final ResourceLocation texture = new ResourceLocation(RefStrings.MODID + ":textures/gui/processing/gui_solidifier.png");

	private TileEntityMachineSolidifier solidifier;

	public GUISolidifier(InventoryPlayer invPlayer, TileEntityMachineSolidifier te) {
		super(new ContainerSolidifier(invPlayer, te));
		solidifier = te;

		this.xSize = 176;
		this.ySize = 204;
	}

	@Override
	public void drawScreen(int mouseX, int mouseY, float f) {
		super.drawScreen(mouseX, mouseY, f);

		FFUtils.renderTankInfo(this, mouseX, mouseY, guiLeft + 35, guiTop + 36, 16, 52, solidifier.tank);
		this.drawElectricityInfo(this, mouseX, mouseY, guiLeft + 134, guiTop + 18, 16, 52, solidifier.power, solidifier.getMaxPower());

		super.renderHoveredToolTip(mouseX, mouseY);
	}

	@Override
	protected void drawGuiContainerForegroundLayer(int i, int j) {
		String name = this.solidifier.hasCustomInventoryName() ? this.solidifier.getInventoryName() : I18n.format(this.solidifier.getInventoryName());

		this.fontRenderer.drawString(name, 70 - this.fontRenderer.getStringWidth(name) / 2, 6, 4210752);
		this.fontRenderer.drawString(I18n.format("container.inventory"), 8, this.ySize - 96 + 2, 4210752);
	}

	@Override
	protected void drawGuiContainerBackgroundLayer(float f, int mouseX, int mouseY) {
		super.drawDefaultBackground();
		GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
		Minecraft.getMinecraft().getTextureManager().bindTexture(texture);
		drawTexturedModalRect(guiLeft, guiTop, 0, 0, xSize, ySize);

		int p = (int) (solidifier.power * 52 / solidifier.getMaxPower());
		drawTexturedModalRect(guiLeft + 134, guiTop + 70 - p, 176, 52 - p, 16, p);

		int prog = solidifier.getProgressScaled(42);
		drawTexturedModalRect(guiLeft + 42, guiTop + 17, 192, 0, prog, 35);

		if(p > 0)
			drawTexturedModalRect(guiLeft + 138, guiTop + 4, 176, 52, 9, 12);

		//FFUtils.drawLiquid puts the liquid bottom at guiTop + offsetY - 28, the tank ends at y=88
		FFUtils.drawLiquid(solidifier.tank, guiLeft, guiTop, zLevel, 16, 52, 35, 88 + 28);
	}
}
