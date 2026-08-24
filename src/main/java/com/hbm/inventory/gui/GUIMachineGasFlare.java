package com.hbm.inventory.gui;

import com.hbm.handler.threading.PacketThreading;
import org.lwjgl.opengl.GL11;

import com.hbm.packet.NBTControlPacket;
import com.hbm.util.I18nUtil;
import com.hbm.forgefluid.FFUtils;
import com.hbm.inventory.container.ContainerMachineGasFlare;
import com.hbm.lib.RefStrings;
import com.hbm.tileentity.machine.oil.TileEntityMachineGasFlare;

import net.minecraft.client.audio.PositionedSoundRecord;
import net.minecraft.init.SoundEvents;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.util.ResourceLocation;

import java.io.IOException;

public class GUIMachineGasFlare extends GuiInfoContainer {

	private static final ResourceLocation texture = new ResourceLocation(RefStrings.MODID + ":textures/gui/generators/gui_flare_stack.png");
	private static final ResourceLocation OVERLAY_TEXTURE = new ResourceLocation(RefStrings.MODID + ":textures/gui/generators/flare_overlays.png");

	private TileEntityMachineGasFlare flare;

	public GUIMachineGasFlare(InventoryPlayer invPlayer, TileEntityMachineGasFlare tedf) {
		super(new ContainerMachineGasFlare(invPlayer, tedf));
		flare = tedf;

		this.xSize = 226;
		this.ySize = 256;
	}

	@Override
	public void drawScreen(int mouseX, int mouseY, float f) {
		super.drawScreen(mouseX, mouseY, f);

		this.drawCustomInfoStat(mouseX, mouseY, guiLeft + 40, guiTop + 88, 16, 23, mouseX, mouseY, I18nUtil.resolveKeyArray("flare.valve"));
		this.drawCustomInfoStat(mouseX, mouseY, guiLeft + 147, guiTop + 119, 27, 19, mouseX, mouseY, I18nUtil.resolveKeyArray("flare.ignition"));

		FFUtils.renderTankInfo(this, mouseX, mouseY, guiLeft + 26, guiTop + 17, 16, 53, flare.tank);
		FFUtils.renderTankInfo(this, mouseX, mouseY, guiLeft + 142, guiTop + 17, 16, 53, flare.outputTank);

		super.renderHoveredToolTip(mouseX, mouseY);
	}

	@Override
	protected void mouseClicked(int x, int y, int mouseButton) throws IOException {
		super.mouseClicked(x, y, mouseButton);

		if(guiLeft + 40 <= x && guiLeft + 40 + 38 > x && guiTop + 88 <= y && guiTop + 88 + 23 > y) {
			mc.getSoundHandler().playSound(PositionedSoundRecord.getMasterRecord(SoundEvents.UI_BUTTON_CLICK, 1.0F));
			sendValvePacket();
		}
		else if(guiLeft + 147 <= x && guiLeft + 147 + 27 > x && guiTop + 119 <= y && guiTop + 119 + 19 > y) {
			mc.getSoundHandler().playSound(PositionedSoundRecord.getMasterRecord(SoundEvents.UI_BUTTON_CLICK, 1.0F));
			sendIgnitionPacket();
		}
	}

	private void sendValvePacket() {
		NBTTagCompound data = new NBTTagCompound();
		data.setBoolean("valve", true);
		PacketThreading.createSendToServerThreadedPacket(new NBTControlPacket(data, flare.getPos()));
	}

	private void sendIgnitionPacket() {
		NBTTagCompound data = new NBTTagCompound();
		data.setBoolean("dial", true);
		PacketThreading.createSendToServerThreadedPacket(new NBTControlPacket(data, flare.getPos()));
	}

	@Override
	protected void drawGuiContainerForegroundLayer(int i, int j) {

	}

	@Override
	protected void drawGuiContainerBackgroundLayer(float p_146976_1_, int p_146976_2_, int p_146976_3_) {
		super.drawDefaultBackground();
		GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);

		Minecraft.getMinecraft().getTextureManager().bindTexture(texture);
		drawTexturedModalRect(guiLeft, guiTop, 0, 0, xSize, ySize);

		FFUtils.drawLiquid(flare.tank, guiLeft, guiTop, zLevel, 16, 53, 26, 100);
		FFUtils.drawLiquid(flare.outputTank, guiLeft, guiTop, zLevel, 16, 53, 142, 100);

		Minecraft.getMinecraft().getTextureManager().bindTexture(OVERLAY_TEXTURE);

		if (flare.isOn) {
			drawTexturedModalRect(guiLeft + 14, guiTop + 94, 158, 206, 55, 45);
		}

		if (flare.doesBurn) {
			drawTexturedModalRect(guiLeft + 132, guiTop + 65, 182, 185, 11, 17);
		}

		if (flare.isProcessing && flare.doesBurn) {
			drawTexturedModalRect(guiLeft + 64, guiTop + 11, 201, 139, 51, 64);
		}

		if (flare.isOn) {
			drawTexturedModalRect(guiLeft + 40, guiTop + 88, 214, 229, 38, 24);
		}

		if (flare.doesBurn) {
			drawTexturedModalRect(guiLeft + 147, guiTop + 119, 216, 207, 28, 19);
		}

		drawTexturedModalRect(guiLeft + 7, guiTop + 204, 7, 177, 162, 54);
	}
}