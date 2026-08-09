package com.hbm.render.tileentity;

import com.hbm.config.GeneralConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.tileentity.TileEntity;

public final class RenderPerformance {

	private RenderPerformance() {
	}

	public static boolean isLow() {
		return GeneralConfig.clientQualityProfile == 1 && !GeneralConfig.adaptiveClientQuality;
	}

	public static boolean skipDistant(TileEntity te, double distanceSq) {
		if(!isLow()) {
			return false;
		}
		EntityPlayerSP player = Minecraft.getMinecraft().player;
		return player != null && te.getDistanceSq(player.posX, player.posY, player.posZ) > distanceSq;
	}

	public static boolean renderGlobalInLow() {
		return !isLow();
	}
}
