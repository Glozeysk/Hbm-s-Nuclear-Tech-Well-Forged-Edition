package com.hbm.inventory.gui;

import com.hbm.blocks.generic.ItemBlockStorageCrate;
import com.hbm.config.GeneralConfig;
import com.hbm.main.MainRegistry;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import org.lwjgl.opengl.GL11;

import com.hbm.inventory.container.ContainerCrateDesh;
import com.hbm.lib.RefStrings;
import com.hbm.tileentity.machine.TileEntityCrateDesh;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.util.ResourceLocation;

import java.util.Locale;

public class GUICrateDesh extends GuiContainer {

	private static ResourceLocation texture = new ResourceLocation(RefStrings.MODID + ":textures/gui/storage/gui_crate_desh.png");
	private TileEntityCrateDesh diFurnace;

	public GUICrateDesh(InventoryPlayer invPlayer, TileEntityCrateDesh tedf) {
		super(new ContainerCrateDesh(invPlayer, tedf));
		diFurnace = tedf;

		this.xSize = 248;
		this.ySize = 256;
	}

	@Override
	public void drawScreen(int mouseX, int mouseY, float partialTicks) {
		super.drawScreen(mouseX, mouseY, partialTicks);
		super.renderHoveredToolTip(mouseX, mouseY);
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
		String baseName = this.diFurnace.hasCustomInventoryName() ? this.diFurnace.getInventoryName() : I18n.format(this.diFurnace.getInventoryName());

		if (GeneralConfig.showGuiCrateFillPercentage) {
			String percentage = getFillPercentage();
			String percentText = percentage.isEmpty() ? "" : " (" + percentage + "%)";

			int nameWidth = this.fontRenderer.getStringWidth(baseName);
			int percentWidth = this.fontRenderer.getStringWidth(percentText);

			int totalWidth = nameWidth + percentWidth;
			int startX = (this.xSize / 2) - (totalWidth / 2);

			this.fontRenderer.drawString(baseName, startX, 6, 4210752);

			if (!percentText.isEmpty()) {
				this.fontRenderer.drawString(percentText, startX + nameWidth, 6, 0x55FF55);
			}
		} else {
			int nameWidth = this.fontRenderer.getStringWidth(baseName);
			int startX = (this.xSize / 2) - (nameWidth / 2);
			this.fontRenderer.drawString(baseName, startX, 6, 4210752);
		}

		this.fontRenderer.drawString(I18n.format("container.inventory"), 8, this.ySize - 96 + 2, 4210752);
	}

	@Override
	protected void drawGuiContainerBackgroundLayer(float p_146976_1_, int p_146976_2_, int p_146976_3_) {
		super.drawDefaultBackground();
		GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
		Minecraft.getMinecraft().getTextureManager().bindTexture(texture);
		drawTexturedModalRect(guiLeft, guiTop, 0, 0, xSize, ySize);
	}

	@Override
	public void onGuiClosed() {
		super.onGuiClosed();

		if (this.mc.world.isRemote) {
			ItemBlockStorageCrate.clearOpenStack();
		}
	}
}