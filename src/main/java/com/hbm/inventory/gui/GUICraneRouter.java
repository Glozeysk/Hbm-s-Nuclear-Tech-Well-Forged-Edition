package com.hbm.inventory.gui;

import com.hbm.handler.threading.PacketThreading;
import com.hbm.inventory.container.ContainerCraneRouter;
import com.hbm.lib.RefStrings;
import com.hbm.packet.NBTControlPacket;
import com.hbm.tileentity.network.TileEntityCraneRouter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.PositionedSoundRecord;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.init.SoundEvents;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;

import java.io.IOException;
import java.util.Arrays;

public class GUICraneRouter extends GuiInfoContainer {

    private static final ResourceLocation texture =
            new ResourceLocation(RefStrings.MODID + ":textures/gui/storage/gui_crane_router.png");

    private final TileEntityCraneRouter router;

    public GUICraneRouter(InventoryPlayer invPlayer, TileEntityCraneRouter tedf) {
        super(new ContainerCraneRouter(invPlayer, tedf));
        this.router = tedf;

        this.xSize = 176;
        this.ySize = 166;
    }

    private int getModeSafe(int index) {
        if (router == null || router.modes == null || index < 0 || index >= router.modes.length) {
            return 0;
        }

        int mode = router.modes[index];
        if (mode < 0 || mode > 2) {
            return 0;
        }

        return mode;
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        super.mouseClicked(mouseX, mouseY, mouseButton);

        for (int j = 0; j < 2; j++) {
            for (int k = 0; k < 3; k++) {
                int buttonX = guiLeft + 7 + j * 144;
                int buttonY = guiTop + 16 + k * 26;

                if (buttonX <= mouseX && mouseX < buttonX + 18 && buttonY < mouseY && mouseY <= buttonY + 18) {
                    int index = j * 3 + k;
                    int currentMode = getModeSafe(index);

                    if (currentMode != TileEntityCraneRouter.MODE_INPUT) {
                        mc.getSoundHandler().playSound(
                                PositionedSoundRecord.getMasterRecord(SoundEvents.UI_BUTTON_CLICK, 1.0F)
                        );

                        NBTTagCompound data = new NBTTagCompound();
                        data.setInteger("toggle", index);
                        PacketThreading.createSendToServerThreadedPacket(new NBTControlPacket(data, router.getPos()));
                    }
                }
            }
        }
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        super.drawScreen(mouseX, mouseY, partialTicks);

        for (int j = 0; j < 2; j++) {
            for (int k = 0; k < 3; k++) {
                int buttonX = guiLeft + 7 + j * 144;
                int buttonY = guiTop + 15 + k * 26;

                if (buttonX <= mouseX && mouseX < buttonX + 18 && buttonY < mouseY && mouseY <= buttonY + 18) {
                    String[] text = new String[2];
                    int index = j * 3 + k;
                    int mode = getModeSafe(index);

                    switch (mode) {
                        case TileEntityCraneRouter.MODE_INPUT:
                            text[0] = "INPUT (auto)";
                            text[1] = "Auto-detected, cannot change";
                            break;
                        case TileEntityCraneRouter.MODE_OUTPUT:
                            text[0] = "OUTPUT";
                            text[1] = "Click to disable";
                            break;
                        default:
                            text[0] = "OFF";
                            text[1] = "Click to enable output";
                            break;
                    }

                    drawHoveringText(Arrays.asList(text), mouseX, mouseY);
                }
            }
        }

        super.renderHoveredToolTip(mouseX, mouseY);
    }

    @Override
    protected void drawGuiContainerForegroundLayer(int i, int j) {
        String name = this.router.hasCustomInventoryName()
                ? this.router.getInventoryName()
                : I18n.format(this.router.getInventoryName());

        this.fontRenderer.drawString(name, this.xSize / 2 - this.fontRenderer.getStringWidth(name) / 2, 5, 4210752);
        this.fontRenderer.drawString(I18n.format("container.inventory"), 8, this.ySize - 96 + 2, 4210752);
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(float partialTicks, int mouseX, int mouseY) {
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        Minecraft.getMinecraft().getTextureManager().bindTexture(texture);

        drawTexturedModalRect(guiLeft, guiTop, 0, 0, xSize, ySize);

        for (int j = 0; j < 2; j++) {
            for (int k = 0; k < 3; k++) {
                int index = j * 3 + k;
                int mode = getModeSafe(index);

                if (mode == TileEntityCraneRouter.MODE_INPUT) {
                    drawTexturedModalRect(guiLeft + 7 + j * 144, guiTop + 16 + k * 26, 176, 54, 18, 18);
                } else {
                    drawTexturedModalRect(guiLeft + 7 + j * 144, guiTop + 16 + k * 26, 176, mode * 18, 18, 18);
                }
            }
        }
    }
}