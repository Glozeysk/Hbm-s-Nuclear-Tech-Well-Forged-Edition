package com.hbm.util;

import com.hbm.main.MainRegistry;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public class I18nUtil {

	public static String resolveKey(String s, Object... args) {
		if (FMLCommonHandler.instance().getSide() == Side.CLIENT) {
			return resolveClient(s, args);
		}

        MainRegistry.logger.warn("Side: {} in I18nUtil", FMLCommonHandler.instance().getSide());
		return args != null && args.length > 0
				? net.minecraft.util.text.translation.I18n.translateToLocalFormatted(s, args)
				: net.minecraft.util.text.translation.I18n.translateToLocal(s);
	}

	@SideOnly(Side.CLIENT)
	private static String resolveClient(String s, Object... args) {
		return net.minecraft.client.resources.I18n.format(s, args);
	}

	public static String[] resolveKeyArray(String s, Object... args) {
		return resolveKey(s, args).split("\\$");
	}
}