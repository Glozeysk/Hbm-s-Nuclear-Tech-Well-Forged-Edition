package com.hbm.inventory.gui;

import org.lwjgl.opengl.GL11;

import com.hbm.forgefluid.FFUtils;
import com.hbm.inventory.container.ContainerMachinePyroOven;
import com.hbm.lib.RefStrings;
import com.hbm.tileentity.machine.oil.TileEntityMachinePyroOven;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.util.ResourceLocation;

//Ported from NTM:CE, adapted to this fork's FFUtils tank rendering (no FluidTankNTM)
public class GUIMachinePyroOven extends GuiInfoContainer {

	private static ResourceLocation texture = new ResourceLocation(RefStrings.MODID + ":textures/gui/processing/gui_pyrooven.png");
	private TileEntityMachinePyroOven pyro;

	public GUIMachinePyroOven(InventoryPlayer invPlayer, TileEntityMachinePyroOven te) {
		super(new ContainerMachinePyroOven(invPlayer, te));
		pyro = te;

		this.xSize = 176;
		this.ySize = 204;
	}

	@Override
	public void drawScreen(int mouseX, int mouseY, float f) {
		super.drawScreen(mouseX, mouseY, f);

		FFUtils.renderTankInfo(this, mouseX, mouseY, guiLeft + 8, guiTop + 18, 16, 70, pyro.tanks[0]);
		FFUtils.renderTankInfo(this, mouseX, mouseY, guiLeft + 116, guiTop + 18, 16, 70, pyro.tanks[1]);
		this.drawElectricityInfo(this, mouseX, mouseY, guiLeft + 152, guiTop + 18, 16, 52, pyro.power, TileEntityMachinePyroOven.maxPower);

		super.renderHoveredToolTip(mouseX, mouseY);
	}

	@Override
	protected void drawGuiContainerForegroundLayer(int i, int j) {
		String name = this.pyro.hasCustomInventoryName() ? this.pyro.getInventoryName() : I18n.format(this.pyro.getInventoryName());
		this.fontRenderer.drawString(name, this.xSize / 2 - this.fontRenderer.getStringWidth(name) / 2 - 18, 6, 4210752);
		this.fontRenderer.drawString(I18n.format("container.inventory"), 8, this.ySize - 96 + 2, 4210752);
	}

	@Override
	protected void drawGuiContainerBackgroundLayer(float interp, int x, int y) {
		super.drawDefaultBackground();
		GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
		Minecraft.getMinecraft().getTextureManager().bindTexture(texture);
		drawTexturedModalRect(guiLeft, guiTop, 0, 0, xSize, ySize);

		int i = (int) (pyro.power * 52 / TileEntityMachinePyroOven.maxPower);
		drawTexturedModalRect(guiLeft + 152, guiTop + 70 - i, 176, 64 - i, 16, i);

		int p = (int) (pyro.progress * 27);
		drawTexturedModalRect(guiLeft + 57, guiTop + 47, 176, 0, p, 12);

		//FFUtils.drawLiquid puts the liquid bottom at guiTop + offsetY - 28, so offsetY = 88 + 28
		FFUtils.drawLiquid(pyro.tanks[0], guiLeft, guiTop, zLevel, 16, 70, 8, 116);
		FFUtils.drawLiquid(pyro.tanks[1], guiLeft, guiTop, zLevel, 16, 70, 116, 116);
	}
}
