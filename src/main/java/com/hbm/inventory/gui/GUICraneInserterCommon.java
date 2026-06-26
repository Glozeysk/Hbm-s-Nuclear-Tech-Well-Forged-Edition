package com.hbm.inventory.gui;

import com.hbm.inventory.container.ContainerCraneInserterCommon;
import com.hbm.lib.RefStrings;
import com.hbm.tileentity.network.TileEntityCraneInserterBase;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;

public class GUICraneInserterCommon extends GuiInfoContainer {

    private static final ResourceLocation texture = new ResourceLocation(RefStrings.MODID + ":textures/gui/storage/gui_crane_inserter.png");
    private final TileEntityCraneInserterBase inserter;

    public GUICraneInserterCommon(InventoryPlayer invPlayer, TileEntityCraneInserterBase tedf) {
        super(new ContainerCraneInserterCommon(invPlayer, tedf));
        this.inserter = tedf;
        this.xSize = 176;
        this.ySize = 185;
    }

    @Override
    protected void drawGuiContainerForegroundLayer(int i, int j) {
        String name = this.inserter.hasCustomInventoryName() ? this.inserter.getInventoryName() : I18n.format(this.inserter.getInventoryName());
        this.fontRenderer.drawString(name, this.xSize / 2 - this.fontRenderer.getStringWidth(name) / 2, 6, 4210752);
        this.fontRenderer.drawString(I18n.format("container.inventory"), 8, this.ySize - 96 + 2, 4210752);
    }

    @Override
    public void drawScreen(int x, int y, float interp) {
        super.drawScreen(x, y, interp);
        this.renderHoveredToolTip(x, y);
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(float partialTicks, int mouseX, int mouseY) {
        super.drawDefaultBackground();
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        Minecraft.getMinecraft().getTextureManager().bindTexture(texture);
        drawTexturedModalRect(guiLeft, guiTop, 0, 0, xSize, ySize);
    }
}