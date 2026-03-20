package com.hbm.inventory.gui;

import org.lwjgl.opengl.GL11;

import com.hbm.forgefluid.FFUtils;
import com.hbm.forgefluid.ModForgeFluids;
import com.hbm.inventory.container.ContainerDiFurnaceBig;
import com.hbm.lib.RefStrings;
import com.hbm.tileentity.machine.TileEntityMachineDiFurnaceBig;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fluids.FluidStack;

public class GUIDiFurnaceBig extends GuiInfoContainer {
	
	private static ResourceLocation texture = new ResourceLocation(RefStrings.MODID, "textures/gui/processing/gui_big_difurnace.png");
	private TileEntityMachineDiFurnaceBig diFurnace;

	public GUIDiFurnaceBig(InventoryPlayer invPlayer, TileEntityMachineDiFurnaceBig tedf) {
		super(new ContainerDiFurnaceBig(invPlayer, tedf));
		diFurnace = tedf;
		
		this.xSize = 208;
		this.ySize = 201;
	}
	
	@Override
	public void drawScreen(int mouseX, int mouseY, float f) {
		super.drawScreen(mouseX, mouseY, f);

		FFUtils.renderTankInfo(this, mouseX, mouseY, guiLeft + 35, guiTop + 18, 16, 70, diFurnace.tank, diFurnace.tankType);
		this.drawElectricityInfo(this, mouseX, mouseY, guiLeft + 184, guiTop + 18, 16, 52, diFurnace.power, TileEntityMachineDiFurnaceBig.maxPower);
		super.renderHoveredToolTip(mouseX, mouseY);
	}
	
	@Override
	protected void drawGuiContainerForegroundLayer(int i, int j) {
		String name = this.diFurnace.hasCustomInventoryName() ? this.diFurnace.getInventoryName() : I18n.format(this.diFurnace.getInventoryName());

		this.fontRenderer.drawString(name, (this.xSize / 2 - this.fontRenderer.getStringWidth(name) / 2) - 16, 6, 4210752);
		this.fontRenderer.drawString(I18n.format("container.inventory"), 8, this.ySize - 96 + 2, 4210752);
	}

	@Override
	protected void drawGuiContainerBackgroundLayer(float p_146976_1_, int p_146976_2_, int p_146976_3_) {
		super.drawDefaultBackground();
		GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
		Minecraft.getMinecraft().getTextureManager().bindTexture(texture);
		drawTexturedModalRect(guiLeft, guiTop, 0, 0, xSize, ySize);

		FluidStack fluid = diFurnace.tank.getFluid();

		if(fluid != null && fluid.getFluid() != null && (fluid.getFluid() == ModForgeFluids.sparkfuel)) {
			drawTexturedModalRect(guiLeft + 95, guiTop + 44, 226, 18, 30, 18);
		}
		if(fluid != null && fluid.getFluid() != null && (fluid.getFluid() == ModForgeFluids.uu_matter)) {
			drawTexturedModalRect(guiLeft + 95, guiTop + 44, 226, 72, 30, 18);
		}

		if(diFurnace.power > 0) {
			int i = (int)diFurnace.getPowerScaled(52);
			drawTexturedModalRect(guiLeft + 184, guiTop + 70 - i, 240, 52 + 108 - i, 16, i);
		}

		if(diFurnace.power > 0) {
			drawTexturedModalRect(guiLeft + 188, guiTop + 4, 247, 160, 9, 12);
		}

		if(fluid != null && fluid.getFluid() != null && (fluid.getFluid() == ModForgeFluids.sparkfuel) && diFurnace.isRunning) {
			drawTexturedModalRect(guiLeft + 95, guiTop + 44, 226, 0, 30, 18);
		}
		if(fluid != null && fluid.getFluid() != null && (fluid.getFluid() == ModForgeFluids.uu_matter) && diFurnace.isRunning) {
			drawTexturedModalRect(guiLeft + 95, guiTop + 44, 226, 90, 30, 18);
		}

		if(fluid != null && fluid.getFluid() != null && (fluid.getFluid() == ModForgeFluids.nitan) && diFurnace.isRunning) {
			int j1 = diFurnace.getProgressScaled(30);
			drawTexturedModalRect(guiLeft + 95, guiTop + 44, 226, 36, j1, 18);
		}
		if(fluid != null && fluid.getFluid() != null && (fluid.getFluid() == ModForgeFluids.balefire) && diFurnace.isRunning) {
			int j1 = diFurnace.getProgressScaled(30);
			drawTexturedModalRect(guiLeft + 95, guiTop + 44, 226, 54, j1, 18);
		}

		FFUtils.drawLiquid(diFurnace.tank, guiLeft + 7, guiTop + 19, this.zLevel, 16, 70, 26, 97);
	}
}
