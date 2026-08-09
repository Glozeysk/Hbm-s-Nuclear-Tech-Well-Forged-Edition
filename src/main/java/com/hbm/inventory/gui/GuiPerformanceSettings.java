package com.hbm.inventory.gui;

import com.hbm.config.GeneralConfig;
import com.hbm.handler.MeteorControlHandler;
import com.hbm.packet.MeteorControlPacket;
import com.hbm.packet.PacketDispatcher;
import com.hbm.util.I18nUtil;
import java.io.IOException;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;

public class GuiPerformanceSettings extends GuiScreen {

	private static final int ID_AUTO = 0;
	private static final int ID_LOW = 1;
	private static final int ID_MEDIUM = 2;
	private static final int ID_HIGH = 3;
	private static final int ID_METEOR = 4;
	private static final int ID_RIPPLE = 5;
	private static final int ID_CLOSE = 6;
	private int guiLeft;
	private int guiTop;
	private int guiWidth;
	private int guiHeight;
	private boolean canEditMeteorSettings;
	private boolean meteorStrikesEnabled;
	private GuiButton autoButton;
	private GuiButton lowButton;
	private GuiButton mediumButton;
	private GuiButton highButton;
	private GuiButton meteorButton;
	private GuiButton rippleButton;
	private GuiButton closeButton;

	@Override
	public void initGui() {
		this.buttonList.clear();
		this.guiWidth = Math.min(320, this.width - 24);
		this.guiHeight = Math.min(240, this.height - 24);
		if(this.guiWidth < 240) {
			this.guiWidth = 240;
		}
		this.guiLeft = (this.width - this.guiWidth) / 2;
		this.guiTop = (this.height - this.guiHeight) / 2;
		this.canEditMeteorSettings = Minecraft.getMinecraft().player != null && Minecraft.getMinecraft().player.canUseCommand(2, "hbm");
		this.meteorStrikesEnabled = Minecraft.getMinecraft().world == null || MeteorControlHandler.isEnabled(Minecraft.getMinecraft().world);
		int gap = 8;
		int buttonWidth = (this.guiWidth - 40 - gap) / 2;
		int xLeft = this.guiLeft + 20;
		int xRight = xLeft + buttonWidth + gap;
		int y = this.guiTop + 92;
		this.autoButton = new GuiButton(ID_AUTO, xLeft, y, buttonWidth, 20, "");
		this.lowButton = new GuiButton(ID_LOW, xRight, y, buttonWidth, 20, "");
		this.mediumButton = new GuiButton(ID_MEDIUM, xLeft, y + 26, buttonWidth, 20, "");
		this.highButton = new GuiButton(ID_HIGH, xRight, y + 26, buttonWidth, 20, "");
		this.buttonList.add(this.autoButton);
		this.buttonList.add(this.lowButton);
		this.buttonList.add(this.mediumButton);
		this.buttonList.add(this.highButton);
		if(this.canEditMeteorSettings) {
			this.meteorButton = new GuiButton(ID_METEOR, this.guiLeft + 20, y + 54, this.guiWidth - 40, 20, "");
			this.buttonList.add(this.meteorButton);
		}
		this.rippleButton = new GuiButton(ID_RIPPLE, this.guiLeft + 20, this.canEditMeteorSettings ? y + 80 : y + 54, this.guiWidth - 40, 20, "");
		this.buttonList.add(this.rippleButton);
		this.closeButton = new GuiButton(ID_CLOSE, this.guiLeft + 20, this.canEditMeteorSettings ? y + 110 : y + 84, this.guiWidth - 40, 20, "");
		this.buttonList.add(this.closeButton);
		this.refreshButtons();
	}

	@Override
	public void drawScreen(int mouseX, int mouseY, float partialTicks) {
		if(this.canEditMeteorSettings && this.mc.world != null) {
			this.meteorStrikesEnabled = MeteorControlHandler.isEnabled(this.mc.world);
		}
		this.refreshButtons();
		this.drawDefaultBackground();
		this.drawRect(this.guiLeft, this.guiTop, this.guiLeft + this.guiWidth, this.guiTop + this.guiHeight, -2013265920);
		this.drawRect(this.guiLeft + 1, this.guiTop + 1, this.guiLeft + this.guiWidth - 1, this.guiTop + this.guiHeight - 1, -1509949440);
		this.drawHorizontalLine(this.guiLeft + 12, this.guiLeft + this.guiWidth - 12, this.guiTop + 28, -8355712);
		this.drawCenteredString(this.fontRenderer, I18nUtil.resolveKey("gui.hbm.performance.title"), this.width / 2, this.guiTop + 12, 16777215);
		this.drawString(this.fontRenderer, this.getModeLine(), this.guiLeft + 20, this.guiTop + 36, 13421772);
		this.drawString(this.fontRenderer, this.getStatusLine(), this.guiLeft + 20, this.guiTop + 50, 13421772);
		if(this.canEditMeteorSettings) {
			this.drawString(this.fontRenderer, this.getMeteorLine(), this.guiLeft + 20, this.guiTop + 64, 13421772);
			this.drawString(this.fontRenderer, this.getRippleLine(), this.guiLeft + 20, this.guiTop + 78, 13421772);
		} else {
			this.drawString(this.fontRenderer, this.getRippleLine(), this.guiLeft + 20, this.guiTop + 64, 13421772);
		}
		this.drawCenteredString(this.fontRenderer, I18nUtil.resolveKey("gui.hbm.performance.saved"), this.width / 2, this.guiTop + this.guiHeight - 18, 10066329);
		super.drawScreen(mouseX, mouseY, partialTicks);
	}

	@Override
	protected void actionPerformed(GuiButton button) throws IOException {
		if(button.id == ID_CLOSE) {
			this.mc.displayGuiScreen(null);
			return;
		}
		if(button.id == ID_AUTO) {
			GeneralConfig.setClientQualityProfile(0, true);
		} else if(button.id == ID_LOW) {
			GeneralConfig.setClientQualityProfile(1, false);
		} else if(button.id == ID_MEDIUM) {
			GeneralConfig.setClientQualityProfile(2, false);
		} else if(button.id == ID_HIGH) {
			GeneralConfig.setClientQualityProfile(3, false);
		} else if(button.id == ID_METEOR) {
			this.meteorStrikesEnabled = !this.meteorStrikesEnabled;
			MeteorControlHandler.syncClient(this.meteorStrikesEnabled);
			PacketDispatcher.wrapper.sendToServer(new MeteorControlPacket(this.meteorStrikesEnabled));
		} else if(button.id == ID_RIPPLE) {
			GeneralConfig.powerArmorRadiationRipple = !GeneralConfig.powerArmorRadiationRipple;
			GeneralConfig.saveClientQualityConfig();
		}
		this.refreshButtons();
	}

	@Override
	public boolean doesGuiPauseGame() {
		return false;
	}

	private void refreshButtons() {
		this.autoButton.displayString = this.makeButtonLabel(I18nUtil.resolveKey("gui.hbm.performance.auto"), GeneralConfig.adaptiveClientQuality && GeneralConfig.clientQualityProfile == 0);
		this.lowButton.displayString = this.makeButtonLabel(I18nUtil.resolveKey("gui.hbm.performance.low"), !GeneralConfig.adaptiveClientQuality && GeneralConfig.clientQualityProfile == 1);
		this.mediumButton.displayString = this.makeButtonLabel(I18nUtil.resolveKey("gui.hbm.performance.medium"), !GeneralConfig.adaptiveClientQuality && GeneralConfig.clientQualityProfile == 2);
		this.highButton.displayString = this.makeButtonLabel(I18nUtil.resolveKey("gui.hbm.performance.high"), !GeneralConfig.adaptiveClientQuality && GeneralConfig.clientQualityProfile == 3);
		this.autoButton.enabled = !(GeneralConfig.adaptiveClientQuality && GeneralConfig.clientQualityProfile == 0);
		this.lowButton.enabled = GeneralConfig.adaptiveClientQuality || GeneralConfig.clientQualityProfile != 1;
		this.mediumButton.enabled = GeneralConfig.adaptiveClientQuality || GeneralConfig.clientQualityProfile != 2;
		this.highButton.enabled = GeneralConfig.adaptiveClientQuality || GeneralConfig.clientQualityProfile != 3;
		if(this.meteorButton != null) {
			this.meteorButton.displayString = this.makeButtonLabel(this.getMeteorButtonLabel(), this.meteorStrikesEnabled);
			this.meteorButton.enabled = true;
		}
		this.rippleButton.displayString = this.makeButtonLabel(this.getRippleButtonLabel(), GeneralConfig.powerArmorRadiationRipple);
		this.rippleButton.enabled = true;
		this.closeButton.displayString = I18nUtil.resolveKey("gui.hbm.performance.close");
	}

	private String makeButtonLabel(String label, boolean selected) {
		return selected ? "[X] " + label : "[ ] " + label;
	}

	private String getModeLine() {
		if(GeneralConfig.adaptiveClientQuality && GeneralConfig.clientQualityProfile == 0) {
			return I18nUtil.resolveKey("gui.hbm.performance.mode.auto");
		}
		if(GeneralConfig.clientQualityProfile == 1) {
			return I18nUtil.resolveKey("gui.hbm.performance.mode.low");
		}
		if(GeneralConfig.clientQualityProfile == 2) {
			return I18nUtil.resolveKey("gui.hbm.performance.mode.medium");
		}
		return I18nUtil.resolveKey("gui.hbm.performance.mode.high");
	}

	private String getStatusLine() {
		return I18nUtil.resolveKey("gui.hbm.performance.adaptive", GeneralConfig.adaptiveClientQuality ? I18nUtil.resolveKey("gui.hbm.performance.on") : I18nUtil.resolveKey("gui.hbm.performance.off"));
	}

	private String getMeteorLine() {
		return I18nUtil.resolveKey("gui.hbm.performance.meteor", this.meteorStrikesEnabled ? I18nUtil.resolveKey("gui.hbm.performance.enabled") : I18nUtil.resolveKey("gui.hbm.performance.disabled"));
	}

	private String getMeteorButtonLabel() {
		return this.meteorStrikesEnabled ? I18nUtil.resolveKey("gui.hbm.performance.meteor.disable") : I18nUtil.resolveKey("gui.hbm.performance.meteor.enable");
	}

	private String getRippleLine() {
		return I18nUtil.resolveKey("gui.hbm.performance.ripple", GeneralConfig.powerArmorRadiationRipple ? I18nUtil.resolveKey("gui.hbm.performance.enabled") : I18nUtil.resolveKey("gui.hbm.performance.disabled"));
	}

	private String getRippleButtonLabel() {
		return GeneralConfig.powerArmorRadiationRipple ? I18nUtil.resolveKey("gui.hbm.performance.ripple.disable") : I18nUtil.resolveKey("gui.hbm.performance.ripple.enable");
	}
}
