package com.hbm.handler;

import com.hbm.config.GeneralConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;

public final class HbmClientPerformance {

	private HbmClientPerformance() {
	}

	public static void applyProfile(int profile) {
		if(profile < 1 || profile > 3) {
			profile = 1;
		}
		GeneralConfig.setClientQualityProfile(profile, false);
		applyRuntimeFlags(profile);
	}

	public static void reportNearbyTiles(Minecraft mc, EntityPlayerSP player) {
	}

	public static boolean shouldProfileRender() {
		return false;
	}

	public static void recordRender(String name, long nanos) {
	}

	private static void applyRuntimeFlags(int profile) {
		if(profile == 1) {
			GeneralConfig.instancedParticles = false;
			GeneralConfig.useShaders2 = false;
			GeneralConfig.depthEffects = false;
			GeneralConfig.bloom = false;
			GeneralConfig.heatDistortion = false;
			GeneralConfig.flashlightVolumetric = false;
			GeneralConfig.bulletHoleNormalMapping = false;
			GeneralConfig.bloodFX = false;
			GeneralConfig.flowingDecalAmountMax = 0;
			GeneralConfig.enableMeteorTails = false;
			GeneralConfig.enableSkybox = false;
			return;
		}
		if(profile == 2) {
			GeneralConfig.instancedParticles = false;
			GeneralConfig.useShaders2 = false;
			GeneralConfig.depthEffects = false;
			GeneralConfig.bloom = false;
			GeneralConfig.heatDistortion = false;
			GeneralConfig.flashlightVolumetric = false;
			GeneralConfig.bulletHoleNormalMapping = false;
			GeneralConfig.bloodFX = false;
			GeneralConfig.flowingDecalAmountMax = Math.min(GeneralConfig.flowingDecalAmountMax, 4);
			GeneralConfig.enableMeteorTails = false;
			GeneralConfig.enableSkybox = false;
			return;
		}
		GeneralConfig.applyAdaptiveClientProfile();
	}
}
