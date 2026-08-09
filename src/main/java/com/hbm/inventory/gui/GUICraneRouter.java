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
import java.util.List;

public class GUICraneRouter extends GuiInfoContainer {

    private static final ResourceLocation texture =
            new ResourceLocation(RefStrings.MODID + ":textures/gui/storage/gui_crane_router.png");

    private final TileEntityCraneRouter router;

    private static final int[][] PORT_POSITIONS = {
            {71, 21},
            {71, 39},
            {53, 39},
            {71, 57},
            {89, 57},
            {89, 39}
    };

    private static final String[] SIDE_NAMES = {
            "NORTH", "UP", "WEST", "SOUTH", "DOWN", "EAST"
    };

    public GUICraneRouter(InventoryPlayer invPlayer, TileEntityCraneRouter tedf) {
        super(new ContainerCraneRouter(invPlayer, tedf));
        this.router = tedf;

        this.xSize = 176;
        this.ySize = 200;
    }

    @Override
    public void initGui() {
        super.initGui();
        this.guiTop += 30;
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

    private int getButtonX(int index) {
        return guiLeft + PORT_POSITIONS[index][0];
    }

    private int getButtonY(int index) {
        return guiTop + PORT_POSITIONS[index][1];
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        super.mouseClicked(mouseX, mouseY, mouseButton);

        for (int i = 0; i < 6; i++) {
            int buttonX = getButtonX(i);
            int buttonY = getButtonY(i);

            if (buttonX <= mouseX && mouseX < buttonX + 18 && buttonY <= mouseY && mouseY < buttonY + 18) {
                int currentMode = getModeSafe(i);

                if (currentMode != TileEntityCraneRouter.MODE_INPUT) {
                    mc.getSoundHandler().playSound(
                            PositionedSoundRecord.getMasterRecord(SoundEvents.UI_BUTTON_CLICK, 1.0F)
                    );

                    NBTTagCompound data = new NBTTagCompound();
                    data.setInteger("toggle", i);
                    PacketThreading.createSendToServerThreadedPacket(new NBTControlPacket(data, router.getPos()));
                }
            }
        }
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        super.drawScreen(mouseX, mouseY, partialTicks);

        for (int i = 0; i < 6; i++) {
            int buttonX = getButtonX(i);
            int buttonY = getButtonY(i);

            if (buttonX <= mouseX && mouseX < buttonX + 18 && buttonY <= mouseY && mouseY < buttonY + 18) {
                int mode = getModeSafe(i);
                String sideName = SIDE_NAMES[i];

                String modeText;
                switch (mode) {
                    case TileEntityCraneRouter.MODE_INPUT:
                        modeText = "INPUT (auto)";
                        break;
                    case TileEntityCraneRouter.MODE_OUTPUT:
                        modeText = "OUTPUT";
                        break;
                    default:
                        modeText = "OFF";
                        break;
                }

                List<String> tooltip = Arrays.asList(
                        sideName + " (" + modeText + ")",
                        mode == TileEntityCraneRouter.MODE_INPUT ? "Auto-detected, cannot change" :
                                mode == TileEntityCraneRouter.MODE_OUTPUT ? "Click to disable" :
                                        "Click to enable output"
                );

                drawHoveringText(tooltip, mouseX, mouseY);
            }
        }

        super.renderHoveredToolTip(mouseX, mouseY);
    }

    @Override
    protected void drawGuiContainerForegroundLayer(int i, int j) {
        String name = this.router.hasCustomInventoryName()
                ? this.router.getInventoryName()
                : I18n.format(this.router.getInventoryName());

        this.fontRenderer.drawString(name, this.xSize / 2 - this.fontRenderer.getStringWidth(name) / 2, 6, 4210752);
        this.fontRenderer.drawString(I18n.format("container.inventory"), 8, 105, 4210752);
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(float partialTicks, int mouseX, int mouseY) {
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        Minecraft.getMinecraft().getTextureManager().bindTexture(texture);

        drawTexturedModalRect(guiLeft, guiTop, 0, 0, xSize, ySize);

        for (int i = 0; i < 6; i++) {
            int mode = getModeSafe(i);
            int buttonX = getButtonX(i);
            int buttonY = getButtonY(i);

            if (mode == TileEntityCraneRouter.MODE_INPUT) {
                drawTexturedModalRect(buttonX, buttonY, 176, 54, 18, 18);
            } else {
                drawTexturedModalRect(buttonX, buttonY, 176, mode * 18, 18, 18);

                if (mode == TileEntityCraneRouter.MODE_OUTPUT) {
                    drawTexturedModalRect(buttonX + 4, buttonY + 4, 176, 33, 10, 10);
                }
            }
        }
    }
}