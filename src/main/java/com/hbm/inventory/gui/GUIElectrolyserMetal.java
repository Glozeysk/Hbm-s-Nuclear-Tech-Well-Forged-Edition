package com.hbm.inventory.gui;

import java.io.IOException;

import org.lwjgl.opengl.GL11;

import com.hbm.forgefluid.FFUtils;
import com.hbm.handler.threading.PacketThreading;
import com.hbm.inventory.container.ContainerElectrolyserMetal;
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

//Coordinates measured on gui_electrolyser_metal.png: metal-fluid tank 16x52 at (41,17), bath progress 22x25 at (12,45)
public class GUIElectrolyserMetal extends GuiInfoContainer {

	private static ResourceLocation texture = new ResourceLocation(RefStrings.MODID + ":textures/gui/processing/gui_electrolyser_metal.png");
	private TileEntityElectrolyser electrolyser;

	public GUIElectrolyserMetal(InventoryPlayer invPlayer, TileEntityElectrolyser te) {
		super(new ContainerElectrolyserMetal(invPlayer, te));
		electrolyser = te;

		this.xSize = 178;
		this.ySize = 204;
	}

	@Override
	public void drawScreen(int mouseX, int mouseY, float f) {
		super.drawScreen(mouseX, mouseY, f);

		FFUtils.renderTankInfo(this, mouseX, mouseY, guiLeft + 41, guiTop + 17, 16, 52, electrolyser.tanks[3]);
		this.drawElectricityInfo(this, mouseX, mouseY, guiLeft + 154, guiTop + 18, 16, 60, electrolyser.power, TileEntityElectrolyser.maxPower);

		super.renderHoveredToolTip(mouseX, mouseY);
	}

	@Override
	protected void mouseClicked(int x, int y, int button) throws IOException {
		super.mouseClicked(x, y, button);

		//left button switches to the fluid page
		if(guiLeft + 8 <= x && guiLeft + 8 + 16 > x && guiTop + 83 <= y && guiTop + 83 + 16 > y) {
			mc.getSoundHandler().playSound(PositionedSoundRecord.getMasterRecord(SoundEvents.UI_BUTTON_CLICK, 1.0F));
			NBTTagCompound data = new NBTTagCompound();
			data.setBoolean("fluid", true);
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

		if(electrolyser.power >= electrolyser.usageOre)
			drawTexturedModalRect(guiLeft + 158, guiTop + 4, 194, 25, 9, 12);

		//the bath fills from the bottom
		int o = electrolyser.durationMetal > 0 ? electrolyser.progressOre * 25 / electrolyser.durationMetal : 0;
		drawTexturedModalRect(guiLeft + 12, guiTop + 70 - o, 194, 25 - o, 22, o);

		FFUtils.drawLiquid(electrolyser.tanks[3], guiLeft, guiTop, zLevel, 16, 52, 41, 97);
	}
}
