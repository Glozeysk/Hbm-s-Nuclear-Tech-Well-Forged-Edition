package com.hbm.inventory.gui;

import java.util.ArrayList;
import java.util.List;

import org.lwjgl.opengl.GL11;

import com.hbm.forgefluid.FFUtils;
import com.hbm.inventory.container.ContainerMachineSolderingStation;
import com.hbm.items.ModItems;
import com.hbm.lib.RefStrings;
import com.hbm.tileentity.machine.TileEntityMachineSolderingStation;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;

//Ported from NTM:CE (GUIMachineSolderingStation) without the collision prevention button
public class GUIMachineSolderingStation extends GuiInfoContainer {

	private static final ResourceLocation texture = new ResourceLocation(RefStrings.MODID + ":textures/gui/processing/gui_soldering_station.png");

	private TileEntityMachineSolderingStation station;

	public GUIMachineSolderingStation(InventoryPlayer invPlayer, TileEntityMachineSolderingStation te) {
		super(new ContainerMachineSolderingStation(invPlayer, te));
		station = te;

		this.xSize = 176;
		this.ySize = 204;
	}

	@Override
	public void drawScreen(int mouseX, int mouseY, float f) {
		super.drawScreen(mouseX, mouseY, f);

		FFUtils.renderTankInfo(this, mouseX, mouseY, guiLeft + 17, guiTop + 63, 52, 16, station.tank);
		this.drawElectricityInfo(this, mouseX, mouseY, guiLeft + 152, guiTop + 18, 16, 52, station.getPower(), station.getMaxPower());

		//an empty solder slot shows what belongs there, like the catalyst slot of the reformer
		if(this.mc.player.inventory.getItemStack().isEmpty() && this.isMouseOverSlot(this.inventorySlots.getSlot(5), mouseX, mouseY) && !this.inventorySlots.getSlot(5).getHasStack()) {
			List<Object[]> lines = new ArrayList<Object[]>();
			ItemStack solder = new ItemStack(ModItems.wire_lead);
			lines.add(new Object[] {solder});
			lines.add(new Object[] {solder.getDisplayName()});
			this.drawStackText(lines, mouseX, mouseY, this.fontRenderer, 0);
		}

		super.renderHoveredToolTip(mouseX, mouseY);
	}

	@Override
	protected void drawGuiContainerForegroundLayer(int i, int j) {
		String name = this.station.hasCustomInventoryName() ? this.station.getInventoryName() : I18n.format(this.station.getInventoryName());

		this.fontRenderer.drawString(name, this.xSize / 2 - this.fontRenderer.getStringWidth(name) / 2 - 18, 6, 4210752);
		this.fontRenderer.drawString(I18n.format("container.inventory"), 8, this.ySize - 96 + 2, 4210752);
	}

	@Override
	protected void drawGuiContainerBackgroundLayer(float f, int i, int j) {
		super.drawDefaultBackground();
		GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
		Minecraft.getMinecraft().getTextureManager().bindTexture(texture);
		drawTexturedModalRect(guiLeft, guiTop, 0, 0, xSize, ySize);

		int p = (int) (station.getPower() * 52 / Math.max(station.getMaxPower(), 1));
		drawTexturedModalRect(guiLeft + 152, guiTop + 70 - p, 176, 52 - p, 16, p);

		int prog = station.getProgressScaled(33);
		drawTexturedModalRect(guiLeft + 72, guiTop + 28, 192, 0, prog, 14);

		if(station.power >= station.consumption)
			drawTexturedModalRect(guiLeft + 156, guiTop + 4, 176, 52, 9, 12);

		//the tank is horizontal, so the usual vertical fill is rotated a quarter turn like in the arc welder
		Minecraft.getMinecraft().getTextureManager().bindTexture(TextureMap.LOCATION_BLOCKS_TEXTURE);
		GlStateManager.pushMatrix();
		GlStateManager.disableLighting();
		GlStateManager.enableBlend();
		GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);

		//after the quarter turn the fill starts at x+28 and grows by its length, so the anchor sits 28px left of the tank
		GlStateManager.translate(guiLeft - 11, guiTop + 63, 0);
		GlStateManager.rotate(90, 0, 0, 1);
		FFUtils.drawLiquid(station.tank, 0, 0, zLevel, 16, 52, 0, 0);

		GlStateManager.disableBlend();
		GlStateManager.enableLighting();
		GlStateManager.popMatrix();
	}
}
