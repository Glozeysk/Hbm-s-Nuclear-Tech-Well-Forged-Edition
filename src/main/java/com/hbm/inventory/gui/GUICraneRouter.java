package com.hbm.inventory.gui;

import com.hbm.handler.threading.PacketThreading;
import com.hbm.inventory.container.ContainerCraneRouter;
import com.hbm.lib.RefStrings;
import com.hbm.modules.ModulePatternMatcher;
import com.hbm.packet.NBTControlPacket;
import com.hbm.tileentity.network.TileEntityCraneRouter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.PositionedSoundRecord;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.init.SoundEvents;
import net.minecraft.inventory.Slot;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.TextFormatting;
import org.lwjgl.input.Keyboard;
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

        this.xSize = 256;
        this.ySize = 201;
    }

    private int getModeSafe(int index) {
        if (router == null || router.modes == null || index < 0 || index >= router.modes.length) {
            return 0;
        }

        int mode = router.modes[index];
        if (mode < 0 || mode > 3) {
            return 0;
        }

        return mode;
    }

    private ModulePatternMatcher getMatcherSafe(int matcherIndex) {
        if (router == null || router.patterns == null || matcherIndex < 0 || matcherIndex >= router.patterns.length) {
            return null;
        }
        return router.patterns[matcherIndex];
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        super.mouseClicked(mouseX, mouseY, mouseButton);

        for (int j = 0; j < 2; j++) {
            for (int k = 0; k < 3; k++) {
                int buttonX = guiLeft + 7 + j * 222;
                int buttonY = guiTop + 16 + k * 26;

                if (buttonX <= mouseX && mouseX < buttonX + 18 && buttonY < mouseY && mouseY <= buttonY + 18) {
                    mc.getSoundHandler().playSound(
                            PositionedSoundRecord.getMasterRecord(SoundEvents.UI_BUTTON_CLICK, 1.0F)
                    );

                    NBTTagCompound data = new NBTTagCompound();
                    data.setInteger("toggle", j * 3 + k);
                    PacketThreading.createSendToServerThreadedPacket(new NBTControlPacket(data, router.getPos()));
                }
            }
        }
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        super.drawScreen(mouseX, mouseY, partialTicks);

        for (int j = 0; j < 2; j++) {
            for (int k = 0; k < 3; k++) {
                int buttonX = guiLeft + 7 + j * 222;
                int buttonY = guiTop + 15 + k * 26;

                if (buttonX <= mouseX && mouseX < buttonX + 18 && buttonY < mouseY && mouseY <= buttonY + 18) {
                    String[] text = new String[2];
                    int index = j * 3 + k;

                    switch (getModeSafe(index)) {
                        case 0:
                            text = new String[] { "OFF" };
                            break;
                        case 1:
                            text[0] = "WHITELIST";
                            text[1] = "Route if filter matches";
                            break;
                        case 2:
                            text[0] = "BLACKLIST";
                            text[1] = "Route if filter doesn't match";
                            break;
                        case 3:
                            text[0] = "WILDCARD";
                            text[1] = "Route if no other route is valid";
                            break;
                    }

                    drawHoveringText(Arrays.asList(text), mouseX, mouseY);
                }
            }
        }

        if (mc.player.inventory.getItemStack().isEmpty()) {
            int slotCount = Math.min(30, this.inventorySlots.inventorySlots.size());

            for (int i = 0; i < slotCount; ++i) {
                Slot slot = this.inventorySlots.getSlot(i);
                ModulePatternMatcher matcher = getMatcherSafe(i / 5);
                int index = i % 5;

                if (matcher != null
                        && matcher.modes != null
                        && index < matcher.modes.length
                        && isMouseOverSlot(slot, mouseX, mouseY)
                        && matcher.modes[index] != null) {

                    String label = TextFormatting.YELLOW + "";

                    switch (matcher.modes[index]) {
                        case "exact":
                            label += "Item and meta match";
                            break;
                        case "wildcard":
                            label += "Item matches";
                            break;
                        default:
                            label += "Ore dict key matches: " + matcher.modes[index];
                            break;
                    }

                    drawHoveringText(
                            Arrays.asList(TextFormatting.RED + "Right click to change", label),
                            mouseX,
                            mouseY - 30
                    );
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
        this.fontRenderer.drawString(I18n.format("container.inventory"), 8 + 39, this.ySize - 96 + 2, 4210752);
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(float partialTicks, int mouseX, int mouseY) {
        super.drawDefaultBackground();
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        Minecraft.getMinecraft().getTextureManager().bindTexture(texture);

        drawTexturedModalRect(guiLeft, guiTop, 0, 0, xSize, 93);
        drawTexturedModalRect(guiLeft + 39, guiTop + 101, 39, 101, 176, 100);

        for (int j = 0; j < 2; j++) {
            for (int k = 0; k < 3; k++) {
                int index = j * 3 + k;
                int mode = getModeSafe(index);
                drawTexturedModalRect(guiLeft + 7 + j * 222, guiTop + 16 + k * 26, 238, 93 + mode * 18, 18, 18);
            }
        }

        if (Keyboard.isKeyDown(Keyboard.KEY_LMENU)) {
            for (int i = 0; i < this.inventorySlots.inventorySlots.size(); i++) {
                Slot s = this.inventorySlots.getSlot(i);
                this.fontRenderer.drawStringWithShadow(i + "", guiLeft + s.xPos + 2, guiTop + s.yPos, 0xffffff);
                this.fontRenderer.drawStringWithShadow(s.getSlotIndex() + "", guiLeft + s.xPos + 2, guiTop + s.yPos + 8, 0xff8080);
            }
        }
    }
}