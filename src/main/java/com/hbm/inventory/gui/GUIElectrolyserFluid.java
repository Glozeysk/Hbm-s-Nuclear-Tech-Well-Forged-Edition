package com.hbm.inventory.gui;

import java.io.IOException;

import org.lwjgl.opengl.GL11;

import com.hbm.forgefluid.FFUtils;
import com.hbm.handler.threading.PacketThreading;
import com.hbm.inventory.container.ContainerElectrolyserFluid;
import com.hbm.lib.RefStrings;
import com.hbm.packet.NBTControlPacket;
import com.hbm.tileentity.machine.TileEntityElectrolyser;

import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.PositionedSoundRecord;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.init.SoundEvents;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;

//Coordinates measured on gui_electrolyser_fluid.png: tanks 16x52 with the bottom edge at y=69, power bar 16x60 at (154,18)
public class GUIElectrolyserFluid extends GuiInfoContainer {

	private static ResourceLocation texture = new ResourceLocation(RefStrings.MODID + ":textures/gui/processing/gui_electrolyser_fluid.png");
	private static final int[] TANK_X = {26, 80, 100};
	private TileEntityElectrolyser electrolyser;

	public GUIElectrolyserFluid(InventoryPlayer invPlayer, TileEntityElectrolyser te) {
		super(new ContainerElectrolyserFluid(invPlayer, te));
		electrolyser = te;

		this.xSize = 178;
		this.ySize = 204;
	}

	@Override
	public void drawScreen(int mouseX, int mouseY, float f) {
		super.drawScreen(mouseX, mouseY, f);

		for(int i = 0; i < 3; i++)
			FFUtils.renderTankInfo(this, mouseX, mouseY, guiLeft + TANK_X[i], guiTop + 17, 16, 52, electrolyser.tanks[i]);
		this.drawElectricityInfo(this, mouseX, mouseY, guiLeft + 154, guiTop + 18, 16, 60, electrolyser.power, TileEntityElectrolyser.maxPower);

		super.renderHoveredToolTip(mouseX, mouseY);
	}

	@Override
	protected void mouseClicked(int x, int y, int button) throws IOException {
		super.mouseClicked(x, y, button);

		//right button switches to the metal page
		if(guiLeft + 30 <= x && guiLeft + 30 + 16 > x && guiTop + 83 <= y && guiTop + 83 + 16 > y) {
			mc.getSoundHandler().playSound(PositionedSoundRecord.getMasterRecord(SoundEvents.UI_BUTTON_CLICK, 1.0F));
			NBTTagCompound data = new NBTTagCompound();
			data.setBoolean("metal", true);
			PacketThreading.createSendToServerThreadedPacket(new NBTControlPacket(data, electrolyser.getPos()));
		}
	}

	@Override
	protected void drawGuiContainerForegroundLayer(int i, int j) {
		String name = this.electrolyser.hasCustomInventoryName() ? this.electrolyser.getInventoryName() : I18n.format(this.electrolyser.getInventoryName());
		this.fontRenderer.drawString(name, 72 - this.fontRenderer.getStringWidth(name) / 2, 6, 0xffffff);
		this.fontRenderer.drawString(I18n.format("container.inventory"), 8, 112, 4210752);
	}

	@Override
	protected void drawGuiContainerBackgroundLayer(float interp, int x, int y) {
		super.drawDefaultBackground();
		GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
		Minecraft.getMinecraft().getTextureManager().bindTexture(texture);
		drawTexturedModalRect(guiLeft, guiTop, 0, 0, xSize, ySize);

		int p = (int) (electrolyser.power * 60 / TileEntityElectrolyser.maxPower);
		drawTexturedModalRect(guiLeft + 154, guiTop + 78 - p, 178, 60 - p, 16, p);

		if(electrolyser.power >= electrolyser.usageFluid)
			drawTexturedModalRect(guiLeft + 158, guiTop + 4, 194, 40, 9, 12);

		int e = electrolyser.durationFluid > 0 ? electrolyser.progressFluid * 40 / electrolyser.durationFluid : 0;
		drawTexturedModalRect(guiLeft + 46, guiTop + 25, 194, 0, 12, e);

		//FFUtils.drawLiquid puts the liquid bottom at guiTop + offsetY - 28, so offsetY = 69 + 28
		for(int i = 0; i < 3; i++)
			FFUtils.drawLiquid(electrolyser.tanks[i], guiLeft, guiTop, zLevel, 16, 52, TANK_X[i], 97);
	}
}
