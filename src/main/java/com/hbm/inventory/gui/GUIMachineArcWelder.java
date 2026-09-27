package com.hbm.inventory.gui;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.lwjgl.opengl.GL11;

import com.hbm.blocks.ModBlocks;
import com.hbm.forgefluid.FFUtils;
import com.hbm.forgefluid.ModForgeFluids;
import com.hbm.handler.threading.PacketThreading;
import com.hbm.inventory.container.ContainerMachineArcWelder;
import com.hbm.lib.RefStrings;
import com.hbm.packet.NBTControlPacket;
import com.hbm.tileentity.machine.TileEntityMachineArcWelder;

import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.PositionedSoundRecord;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;

//Ported from NTM:CE (GUIMachineArcWelder), rebuilt for the four welding processes
public class GUIMachineArcWelder extends GuiInfoContainer {

	//one sheet per process, they differ in panel art, button highlight and which slots are drawn
	private static final ResourceLocation[] TEXTURES = {
			new ResourceLocation(RefStrings.MODID + ":textures/gui/gui_arc_welder_base.png"),
			new ResourceLocation(RefStrings.MODID + ":textures/gui/gui_arc_welder_tig.png"),
			new ResourceLocation(RefStrings.MODID + ":textures/gui/gui_arc_welder_vacuum.png"),
			new ResourceLocation(RefStrings.MODID + ":textures/gui/gui_arc_welder_ebw.png")
	};

	private TileEntityMachineArcWelder welder;

	public GUIMachineArcWelder(InventoryPlayer invPlayer, TileEntityMachineArcWelder te) {
		super(new ContainerMachineArcWelder(invPlayer, te));
		welder = te;

		this.xSize = 176;
		this.ySize = 204;
	}

	@Override
	public void drawScreen(int mouseX, int mouseY, float f) {
		super.drawScreen(mouseX, mouseY, f);

		if(welder.usesVacuum()) {
			this.drawCustomInfoStat(mouseX, mouseY, guiLeft + 17, guiTop + 63, 34, 16, mouseX, mouseY,
					new String[] { String.format("%.1f", welder.vacuum / 1000F) + "% vacuum" });
		} else if(welder.mode == TileEntityMachineArcWelder.MODE_TIG) {
			FFUtils.renderTankInfo(this, mouseX, mouseY, guiLeft + 17, guiTop + 63, 34, 16, welder.tank, ModForgeFluids.argon);
		}

		this.drawElectricityInfo(this, mouseX, mouseY, guiLeft + 152, guiTop + 18, 16, 52, welder.power, welder.getMaxPower());

		//an empty beam slot shows what belongs there, like the solder slot of the soldering station
		if(welder.mode == TileEntityMachineArcWelder.MODE_EBW && this.mc.player.inventory.getItemStack().isEmpty() && this.isMouseOverSlot(this.inventorySlots.getSlot(7), mouseX, mouseY) && !this.inventorySlots.getSlot(7).getHasStack()) {
			List<Object[]> lines = new ArrayList<Object[]>();
			ItemStack transformer = new ItemStack(ModBlocks.machine_transformer_dnt);
			lines.add(new Object[] {transformer});
			lines.add(new Object[] {transformer.getDisplayName()});
			this.drawStackText(lines, mouseX, mouseY, this.fontRenderer, 0);
		}

		super.renderHoveredToolTip(mouseX, mouseY);
	}

	@Override
	protected void mouseClicked(int x, int y, int button) throws IOException {
		super.mouseClicked(x, y, button);

		//four 7x7 buttons in a row, one per process
		if(guiTop + 54 <= y && guiTop + 61 > y) {
			int[] buttons = { 16, 25, 36, 45 };

			for(int i = 0; i < buttons.length; i++) {
				if(guiLeft + buttons[i] <= x && guiLeft + buttons[i] + 7 > x) {
					mc.getSoundHandler().playSound(PositionedSoundRecord.getMasterRecord(SoundEvents.UI_BUTTON_CLICK, 1.0F));
					NBTTagCompound data = new NBTTagCompound();
					data.setInteger("mode", i);
					PacketThreading.createSendToServerThreadedPacket(new NBTControlPacket(data, welder.getPos()));
					return;
				}
			}
		}
	}

	@Override
	protected void drawGuiContainerForegroundLayer(int i, int j) {
		String name = this.welder.hasCustomInventoryName() ? this.welder.getInventoryName() : I18n.format(this.welder.getInventoryName());

		this.fontRenderer.drawString(name, this.xSize / 2 - this.fontRenderer.getStringWidth(name) / 2 - 18, 6, 4210752);
		this.fontRenderer.drawString(I18n.format("container.inventory"), 8, this.ySize - 96 + 2, 4210752);
	}

	@Override
	protected void drawGuiContainerBackgroundLayer(float f, int i, int j) {
		super.drawDefaultBackground();
		GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
		ResourceLocation texture = TEXTURES[Math.max(Math.min(welder.mode, 3), 0)];
		Minecraft.getMinecraft().getTextureManager().bindTexture(texture);
		drawTexturedModalRect(guiLeft, guiTop, 0, 0, xSize, ySize);

		int p = (int) (welder.power * 52 / Math.max(welder.getMaxPower(), 1));
		drawTexturedModalRect(guiLeft + 152, guiTop + 70 - p, 176, 52 - p, 16, p);

		int prog = welder.getProgressScaled(32);
		drawTexturedModalRect(guiLeft + 72, guiTop + 38, 192, 0, prog, 14);

		if(welder.power >= welder.consumption)
			drawTexturedModalRect(guiLeft + 156, guiTop + 4, 176, 52, 9, 12);

		if(welder.usesVacuum()) {
			int vac = welder.getVacuumScaled(34);
			drawTexturedModalRect(guiLeft + 17, guiTop + 63, 192, 14, vac, 16);
			return;
		}

		//MMA has no bar at all
		if(welder.mode != TileEntityMachineArcWelder.MODE_TIG)
			return;

		//the tank is horizontal, so the usual vertical fill is rotated a quarter turn like in the assembly machine
		Minecraft.getMinecraft().getTextureManager().bindTexture(TextureMap.LOCATION_BLOCKS_TEXTURE);
		GlStateManager.pushMatrix();
		GlStateManager.disableLighting();
		GlStateManager.enableBlend();
		GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);

		//after the quarter turn the fill runs from x+28 to x+62, so the anchor sits 28px left of the tank
		GlStateManager.translate(guiLeft - 11, guiTop + 63, 0);
		GlStateManager.rotate(90, 0, 0, 1);
		FFUtils.drawLiquid(welder.tank, 0, 0, zLevel, 16, 34, 0, 0);

		GlStateManager.disableBlend();
		GlStateManager.enableLighting();
		GlStateManager.popMatrix();

		Minecraft.getMinecraft().getTextureManager().bindTexture(texture);
	}
}
