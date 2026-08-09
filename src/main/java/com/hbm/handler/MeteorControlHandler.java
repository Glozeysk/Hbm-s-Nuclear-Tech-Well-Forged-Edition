package com.hbm.handler;

import net.minecraft.world.GameRules;
import net.minecraft.world.World;

public class MeteorControlHandler {

	public static final String KEY_METEOR_STRIKES = "hbmMeteorStrikes";
	private static boolean clientEnabled = true;

	public static void ensure(World world) {
		if(world == null || world.isRemote || world.getGameRules() == null) {
			return;
		}
		GameRules rules = world.getGameRules();
		if(!rules.hasRule(KEY_METEOR_STRIKES)) {
			rules.setOrCreateGameRule(KEY_METEOR_STRIKES, "true");
		}
	}

	public static boolean isEnabled(World world) {
		if(world == null || world.getGameRules() == null) {
			return clientEnabled;
		}
		if(world.isRemote) {
			return clientEnabled;
		}
		ensure(world);
		return world.getGameRules().getBoolean(KEY_METEOR_STRIKES);
	}

	public static void setEnabled(World world, boolean enabled) {
		if(world == null || world.getGameRules() == null) {
			return;
		}
		world.getGameRules().setOrCreateGameRule(KEY_METEOR_STRIKES, Boolean.toString(enabled));
	}

	public static void syncClient(boolean enabled) {
		clientEnabled = enabled;
	}
}
