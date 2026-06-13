package com.hbm.inventory.gui;

import com.hbm.blocks.generic.ItemBlockStorageCrate;
import com.hbm.config.GeneralConfig;
import com.hbm.inventory.container.ContainerCrateTungsten;
import com.hbm.lib.Library;
import com.hbm.lib.RefStrings;
import com.hbm.tileentity.machine.TileEntityCrateTungsten;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;

import java.util.Locale;

public class GUICrateTungsten extends GuiContainer {

	private static ResourceLocation texture = new ResourceLocation(RefStrings.MODID + ":textures/gui/storage/gui_crate_tungsten.png");
	private static ResourceLocation texture_hot = new ResourceLocation(RefStrings.MODID + ":textures/gui/storage/gui_crate_tungsten_hot.png");
	private TileEntityCrateTungsten diFurnace;

	public GUICrateTungsten(InventoryPlayer invPlayer, TileEntityCrateTungsten tedf) {
		super(new ContainerCrateTungsten(invPlayer, tedf));
		diFurnace = tedf;

		this.xSize = 176;
		this.ySize = 168;
	}

	private String getFillPercentage() {
		if (diFurnace == null) return "";

		int totalSlots = diFurnace.getSizeInventory();
		if (totalSlots == 0) return "";
		int totalCapacity = totalSlots * 64;
		int currentItems = 0;

		for (int i = 0; i < totalSlots; i++) {
			ItemStack stack = diFurnace.getStackInSlot(i);
			if (!stack.isEmpty()) {
				currentItems += stack.getCount();
			}
		}

		double percentage = (currentItems * 100.0) / totalCapacity;
		return String.format(Locale.US, "%.1f", percentage);
	}

	@Override
	protected void drawGuiContainerForegroundLayer(int i, int j) {
		String title = I18n.format("container.crateTungsten");
		int dynamicColor = diFurnace.heatTimer == 0 ? 0xA0A0A0 : 0xFFCA53;

		if (GeneralConfig.showGuiCrateFillPercentage) {
			String percentage = getFillPercentage();
			String percentText = percentage.isEmpty() ? "" : " (" + percentage + "%)";

			int titleWidth = this.fontRenderer.getStringWidth(title);
			int percentWidth = this.fontRenderer.getStringWidth(percentText);
			int totalWidth = titleWidth + percentWidth;
			int startX = (this.xSize / 2) - (totalWidth / 2);

			this.fontRenderer.drawString(title, startX, 6, dynamicColor);

			if (!percentText.isEmpty()) {
				this.fontRenderer.drawString(percentText, startX + titleWidth, 6, 0x55FF55);
			}
		} else {
			int titleWidth = this.fontRenderer.getStringWidth(title);
			int startX = (this.xSize / 2) - (titleWidth / 2);
			this.fontRenderer.drawString(title, startX, 6, dynamicColor);
		}

		this.fontRenderer.drawString(I18n.format("container.inventory"), 8, this.ySize - 96 + 2, dynamicColor);

		String sparks = Library.getShortNumber(diFurnace.joules) + "SPK";
		this.fontRenderer.drawString(sparks, this.xSize - 8 - this.fontRenderer.getStringWidth(sparks), this.ySize - 96 + 2, dynamicColor);
	}

	@Override
	public void drawScreen(int mouseX, int mouseY, float partialTicks){
		super.drawScreen(mouseX, mouseY, partialTicks);
		super.renderHoveredToolTip(mouseX, mouseY);
	}

	@Override
	protected void drawGuiContainerBackgroundLayer(float p_146976_1_, int p_146976_2_, int p_146976_3_) {
		super.drawDefaultBackground();
		GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);

		if(diFurnace.heatTimer == 0)
			Minecraft.getMinecraft().getTextureManager().bindTexture(texture);
		else
			Minecraft.getMinecraft().getTextureManager().bindTexture(texture_hot);

		drawTexturedModalRect(guiLeft, guiTop, 0, 0, xSize, ySize);
	}

	@Override
	public void onGuiClosed() {
		super.onGuiClosed();

		if (this.mc.world.isRemote) {
			if (ItemBlockStorageCrate.isOpen()) {
				ItemBlockStorageCrate.clearOpenStack();
			}
		}
	}
}