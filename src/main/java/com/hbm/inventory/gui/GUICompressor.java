package com.hbm.inventory.gui;

import java.io.IOException;

import org.lwjgl.opengl.GL11;

import com.hbm.forgefluid.FFUtils;
import com.hbm.handler.threading.PacketThreading;
import com.hbm.inventory.container.ContainerCompressor;
import com.hbm.lib.RefStrings;
import com.hbm.packet.NBTControlPacket;
import com.hbm.tileentity.machine.TileEntityMachineCompressorCompact;
import com.hbm.util.I18nUtil;

import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.PositionedSoundRecord;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.init.SoundEvents;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;

//Ported from NTM:CE (GUICompressor): three mode buttons instead of five pressure levels, left to right high vacuum, vacuum, compression
public class GUICompressor extends GuiInfoContainer {

	private static final ResourceLocation texture = new ResourceLocation(RefStrings.MODID + ":textures/gui/processing/gui_compressor.png");

	/** button index = mode, see TileEntityMachineCompressorCompact.MODE_* */
	private static final String[] MODE_KEYS = { "gui.compressor.high_vacuum", "gui.compressor.vacuum", "gui.compressor.compression" };
	private static final int BUTTON_X = 53, BUTTON_Y = 45, BUTTON_STEP = 11, BUTTON_W = 10, BUTTON_H = 16;

	private TileEntityMachineCompressorCompact compressor;

	public GUICompressor(InventoryPlayer invPlayer, TileEntityMachineCompressorCompact te) {
		super(new ContainerCompressor(invPlayer, te));
		compressor = te;

		this.xSize = 176;
		this.ySize = 204;
	}

	@Override
	public void drawScreen(int mouseX, int mouseY, float f) {
		super.drawScreen(mouseX, mouseY, f);

		FFUtils.renderTankInfo(this, mouseX, mouseY, guiLeft + 17, guiTop + 18, 16, 52, compressor.tanks[0]);
		FFUtils.renderTankInfo(this, mouseX, mouseY, guiLeft + 107, guiTop + 18, 16, 52, compressor.tanks[1]);
		this.drawElectricityInfo(this, mouseX, mouseY, guiLeft + 152, guiTop + 18, 16, 52, compressor.power, compressor.getMaxPower());

		for(int i = 0; i < MODE_KEYS.length; i++)
			this.drawCustomInfoStat(mouseX, mouseY, guiLeft + BUTTON_X + i * BUTTON_STEP, guiTop + BUTTON_Y, BUTTON_W, BUTTON_H, mouseX, mouseY, new String[] { I18nUtil.resolveKey(MODE_KEYS[i]) });

		super.renderHoveredToolTip(mouseX, mouseY);
	}

	@Override
	protected void mouseClicked(int x, int y, int button) throws IOException {
		super.mouseClicked(x, y, button);

		if(guiTop + BUTTON_Y > y || guiTop + BUTTON_Y + BUTTON_H <= y)
			return;

		for(int i = 0; i < MODE_KEYS.length; i++) {
			int bx = guiLeft + BUTTON_X + i * BUTTON_STEP;
			if(bx <= x && bx + BUTTON_W > x) {
				mc.getSoundHandler().playSound(PositionedSoundRecord.getMasterRecord(SoundEvents.UI_BUTTON_CLICK, 1.0F));
				NBTTagCompound data = new NBTTagCompound();
				data.setInteger("mode", i);
				PacketThreading.createSendToServerThreadedPacket(new NBTControlPacket(data, compressor.getPos()));
				return;
			}
		}
	}

	@Override
	protected void drawGuiContainerForegroundLayer(int i, int j) {
		String name = this.compressor.hasCustomInventoryName() ? this.compressor.getInventoryName() : I18n.format(this.compressor.getInventoryName());

		this.fontRenderer.drawString(name, 70 - this.fontRenderer.getStringWidth(name) / 2, 6, 4210752);
		this.fontRenderer.drawString(I18n.format("container.inventory"), 8, this.ySize - 96 + 2, 4210752);
	}

	@Override
	protected void drawGuiContainerBackgroundLayer(float f, int mouseX, int mouseY) {
		super.drawDefaultBackground();
		GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
		Minecraft.getMinecraft().getTextureManager().bindTexture(texture);
		drawTexturedModalRect(guiLeft, guiTop, 0, 0, xSize, ySize);

		if(compressor.power >= compressor.powerRequirement)
			drawTexturedModalRect(guiLeft + 156, guiTop + 4, 176, 52, 9, 12);

		//the pressed button of the active mode
		drawTexturedModalRect(guiLeft + BUTTON_X + compressor.mode * BUTTON_STEP, guiTop + BUTTON_Y, 192, 17, BUTTON_W, BUTTON_H);

		int prog = compressor.getProgressScaled(55);
		drawTexturedModalRect(guiLeft + 42, guiTop + 26, 192, 0, prog, 17);

		int p = (int) (compressor.power * 52 / compressor.getMaxPower());
		drawTexturedModalRect(guiLeft + 152, guiTop + 70 - p, 176, 52 - p, 16, p);

		//FFUtils.drawLiquid puts the liquid bottom at guiTop + offsetY - 28, both tanks end at y=70
		FFUtils.drawLiquid(compressor.tanks[0], guiLeft, guiTop, zLevel, 16, 52, 17, 70 + 28);
		FFUtils.drawLiquid(compressor.tanks[1], guiLeft, guiTop, zLevel, 16, 52, 107, 70 + 28);
	}
}
