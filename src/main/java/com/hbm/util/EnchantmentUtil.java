package com.hbm.util;

import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Enchantments;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;

import java.util.HashMap;
import java.util.Map;

public class EnchantmentUtil {

	private static final String TEMP_TAG = "hbmTempAbilityEnchants";

	public static void addEnchantment(ItemStack stack, Enchantment enchantment, int level) {
		setEnchantmentLevel(stack, enchantment, level);
	}

	public static void addEnchantmentToBook(ItemStack stack, Enchantment enchantment, int level) {
		Map<Enchantment, Integer> ench = new HashMap<>();
		ench.put(enchantment, level);
		EnchantmentHelper.setEnchantments(ench, stack);
	}

	public static void removeEnchantment(ItemStack stack, Enchantment enchantment) {
		setEnchantmentLevel(stack, enchantment, 0);
	}

	public static void setEnchantmentLevel(ItemStack stack, Enchantment enchantment, int level) {
		if (stack.isEmpty() || enchantment == null) return;

		NBTTagCompound root = getOrCreateTag(stack);
		NBTTagList enchList = stack.getEnchantmentTagList();
		short enchId = (short) Enchantment.getEnchantmentID(enchantment);

		for (int i = enchList.tagCount() - 1; i >= 0; i--) {
			NBTTagCompound tag = enchList.getCompoundTagAt(i);
			if (tag.getShort("id") == enchId) {
				enchList.removeTag(i);
			}
		}

		if (level > 0) {
			NBTTagCompound enchTag = new NBTTagCompound();
			enchTag.setShort("id", enchId);
			enchTag.setShort("lvl", (short) level);
			enchList.appendTag(enchTag);
		}

		if (enchList.tagCount() > 0) {
			root.setTag("ench", enchList);
		} else {
			root.removeTag("ench");
			if (isEmptyTag(root)) {
				stack.setTagCompound(null);
			}
		}
	}

	public static void applyTemporaryEnchantment(ItemStack stack, Enchantment enchantment, int level, String key) {
		if (stack.isEmpty() || enchantment == null) return;

		NBTTagCompound root = getOrCreateTag(stack);
		NBTTagCompound temp = root.hasKey(TEMP_TAG, 10) ? root.getCompoundTag(TEMP_TAG) : new NBTTagCompound();

		if (!temp.hasKey(key)) {
			temp.setInteger(key, EnchantmentHelper.getEnchantmentLevel(enchantment, stack));
		}

		root.setTag(TEMP_TAG, temp);
		setEnchantmentLevel(stack, enchantment, level);
	}

	public static void restoreTemporaryEnchantment(ItemStack stack, Enchantment enchantment, String key) {
		if (stack.isEmpty() || enchantment == null || !stack.hasTagCompound()) return;

		NBTTagCompound root = stack.getTagCompound();
		if (root == null || !root.hasKey(TEMP_TAG, 10)) return;

		NBTTagCompound temp = root.getCompoundTag(TEMP_TAG);
		if (!temp.hasKey(key)) return;

		int oldLevel = temp.getInteger(key);
		setEnchantmentLevel(stack, enchantment, oldLevel);

		if (!stack.hasTagCompound()) return;

		root = stack.getTagCompound();
		if (root == null || !root.hasKey(TEMP_TAG, 10)) return;

		temp = root.getCompoundTag(TEMP_TAG);
		temp.removeTag(key);

		if (isEmptyTag(temp)) {
			root.removeTag(TEMP_TAG);
		} else {
			root.setTag(TEMP_TAG, temp);
		}

		if (isEmptyTag(root)) {
			stack.setTagCompound(null);
		}
	}

	public static void normalizeEnchantment(ItemStack stack, Enchantment enchantment) {
		if (stack.isEmpty() || enchantment == null) return;
		int level = EnchantmentHelper.getEnchantmentLevel(enchantment, stack);
		setEnchantmentLevel(stack, enchantment, level);
	}

	public static void clearTemporaryAbilityEnchants(ItemStack stack) {
		if (stack.isEmpty()) return;

		normalizeEnchantment(stack, Enchantments.SILK_TOUCH);
		normalizeEnchantment(stack, Enchantments.FORTUNE);

		restoreTemporaryEnchantment(stack, Enchantments.SILK_TOUCH, "prevSilk");
		restoreTemporaryEnchantment(stack, Enchantments.FORTUNE, "prevFortune");
	}

	private static NBTTagCompound getOrCreateTag(ItemStack stack) {
		if (!stack.hasTagCompound()) {
			stack.setTagCompound(new NBTTagCompound());
		}
		return stack.getTagCompound();
	}

	private static boolean isEmptyTag(NBTTagCompound tag) {
		return tag == null || tag.getKeySet().isEmpty();
	}

	/**
	 * Removes an amount of experience from a player and updates their level
	 * @param entityPlayer the player to remove experience from
	 * @param amount the amount of experience to remove
	 */
	public static void removeExperience(EntityPlayer entityPlayer, float amount) {
		if (entityPlayer.experienceTotal < amount) {
			entityPlayer.experienceLevel = 0;
			entityPlayer.experience = 0;
			entityPlayer.experienceTotal = 0;
			return;
		}

		entityPlayer.experienceTotal -= amount;
		if (entityPlayer.experience * (float) entityPlayer.xpBarCap() < amount) {
			amount -= entityPlayer.experience * (float) entityPlayer.xpBarCap();
			entityPlayer.experience = 1.0f;
			entityPlayer.experienceLevel--;
		}

		while (entityPlayer.xpBarCap() < amount) {
			amount -= entityPlayer.xpBarCap();
			entityPlayer.experienceLevel--;
		}
		entityPlayer.experience -= amount / (float) entityPlayer.xpBarCap();
	}
}