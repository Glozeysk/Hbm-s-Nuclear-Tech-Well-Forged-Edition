package com.hbm.inventory.gui;

import java.util.ArrayList;
import java.util.List;

import org.lwjgl.opengl.GL11;

import com.hbm.forgefluid.FFUtils;
import com.hbm.inventory.container.ContainerMachineCatalyticReformer;
import com.hbm.items.ModItems;
import com.hbm.lib.RefStrings;
import com.hbm.tileentity.machine.oil.TileEntityMachineCatalyticReformer;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;

//Ported from NTM:CE, adapted to this fork's FFUtils tank rendering (no FluidTankNTM)
public class GUIMachineCatalyticReformer extends GuiInfoContainer {

	private static ResourceLocation texture = new ResourceLocation(RefStrings.MODID + ":textures/gui/gui_catalytic_reformer.png");
	//feedstock tank, then output tanks 1-3; all 16x52 with the bottom edge at y=70
	private static final int[] TANK_X = {35, 107, 125, 143};
	private TileEntityMachineCatalyticReformer reformer;

	public GUIMachineCatalyticReformer(InventoryPlayer invPlayer, TileEntityMachineCatalyticReformer te) {
		super(new ContainerMachineCatalyticReformer(invPlayer, te));
		reformer = te;

		this.xSize = 176;
		this.ySize = 238;
	}

	@Override
	public void drawScreen(int mouseX, int mouseY, float f) {
		super.drawScreen(mouseX, mouseY, f);

		for(int i = 0; i < 4; i++)
			FFUtils.renderTankInfo(this, mouseX, mouseY, guiLeft + TANK_X[i], guiTop + 18, 16, 52, reformer.tanks[i], reformer.tankTypes[i]);

		this.drawElectricityInfo(this, mouseX, mouseY, guiLeft + 17, guiTop + 18, 16, 52, reformer.power, TileEntityMachineCatalyticReformer.maxPower);

		if(this.mc.player.inventory.getItemStack().isEmpty() && this.isMouseOverSlot(this.inventorySlots.getSlot(9), mouseX, mouseY) && !this.inventorySlots.getSlot(9).getHasStack()) {
			List<Object[]> lines = new ArrayList<Object[]>();
			ItemStack catalyst_cobalt = new ItemStack(ModItems.catalyst_cobalt);
			lines.add(new Object[] {catalyst_cobalt});
			lines.add(new Object[] {catalyst_cobalt.getDisplayName()});
			this.drawStackText(lines, mouseX, mouseY, this.fontRenderer, 0);
		}

		super.renderHoveredToolTip(mouseX, mouseY);
	}

	@Override
	protected void drawGuiContainerForegroundLayer(int i, int j) {
		String name = this.reformer.hasCustomInventoryName() ? this.reformer.getInventoryName() : I18n.format(this.reformer.getInventoryName());
		this.fontRenderer.drawString(name, this.xSize / 2 - this.fontRenderer.getStringWidth(name) / 2, 5, 0xffffff);
		this.fontRenderer.drawString(I18n.format("container.inventory"), 8, this.ySize - 96 + 2, 4210752);
	}

	@Override
	protected void drawGuiContainerBackgroundLayer(float p_146976_1_, int p_146976_2_, int p_146976_3_) {
		super.drawDefaultBackground();
		GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
		Minecraft.getMinecraft().getTextureManager().bindTexture(texture);
		drawTexturedModalRect(guiLeft, guiTop, 0, 0, xSize, ySize);

		//52 = bar height; CE scales by 54 here and overshoots the frame by 2px at full charge
		int j = (int) reformer.getPowerScaled(52);
		drawTexturedModalRect(guiLeft + 17, guiTop + 70 - j, 176, 52 - j, 16, j);

		//FFUtils.drawLiquid puts the liquid bottom at guiTop + offsetY - 28, so offsetY = 70 + 28 (same as the hydrotreater)
		for(int i = 0; i < 4; i++)
			FFUtils.drawLiquid(reformer.tanks[i], guiLeft, guiTop, zLevel, 16, 52, TANK_X[i], 98);
	}
}
