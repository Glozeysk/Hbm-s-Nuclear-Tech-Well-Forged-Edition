package com.hbm.inventory.gui;

import org.lwjgl.opengl.GL11;

import com.hbm.forgefluid.FFUtils;
import com.hbm.forgefluid.ModForgeFluids;
import com.hbm.inventory.container.ContainerMachineDiFurnaceSPK;
import com.hbm.lib.RefStrings;
import com.hbm.tileentity.machine.TileEntityMachineDiFurnaceSPK;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fluids.FluidStack;

public class GUIMachineDiFurnaceSPK extends GuiInfoContainer {
	
	private static ResourceLocation texture = new ResourceLocation(RefStrings.MODID, "textures/gui/processing/gui_spk_difurnace.png");
	private TileEntityMachineDiFurnaceSPK diFurnace;

	public GUIMachineDiFurnaceSPK(InventoryPlayer invPlayer, TileEntityMachineDiFurnaceSPK tedf) {
		super(new ContainerMachineDiFurnaceSPK(invPlayer, tedf));
		diFurnace = tedf;
		
		this.xSize = 176;
		this.ySize = 166;
	}
	
	@Override
	public void drawScreen(int mouseX, int mouseY, float f) {
		super.drawScreen(mouseX, mouseY, f);

		FFUtils.renderTankInfo(this, mouseX, mouseY, guiLeft + 15 + 18, guiTop + 17, 16, 52, diFurnace.tank, diFurnace.tankType);
		this.drawElectricityInfo(this, mouseX, mouseY, guiLeft + 15, guiTop + 17, 16, 52, diFurnace.power, TileEntityMachineDiFurnaceSPK.maxPower);
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

		FluidStack fluid = diFurnace.tank.getFluid();

		if(fluid != null && fluid.getFluid() != null && (fluid.getFluid() == ModForgeFluids.sparkfuel || fluid.getFluid() == ModForgeFluids.uu_matter)) {
			drawTexturedModalRect(guiLeft + 94, guiTop + 34, 208, 18, 30, 18);
		}

		if(diFurnace.power > 0) {
			int i = (int)diFurnace.getPowerScaled(52);
			drawTexturedModalRect(guiLeft + 15, guiTop + 69 - i, 176, 52 - i, 16, i);
		}

		if(fluid != null && fluid.getFluid() != null && (fluid.getFluid() == ModForgeFluids.sparkfuel || fluid.getFluid() == ModForgeFluids.uu_matter) && diFurnace.canProcess()) {
			drawTexturedModalRect(guiLeft + 94, guiTop + 34, 208, 0, 30, 18);
		}

		if(fluid != null && fluid.getFluid() != null && (fluid.getFluid() == ModForgeFluids.nitan || fluid.getFluid() == ModForgeFluids.balefire) && diFurnace.canProcess()) {
			int j1 = diFurnace.getProgressScaled(30);
			drawTexturedModalRect(guiLeft + 94, guiTop + 34, 208, 36, j1, 18);
		}

		FFUtils.drawLiquid(diFurnace.tank, guiLeft + 7, guiTop, this.zLevel, 16, 52, 26, 97);
	}
}
