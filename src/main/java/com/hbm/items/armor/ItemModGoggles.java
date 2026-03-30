package com.hbm.items.armor;

import java.util.List;

import com.hbm.capability.HbmCapability;
import com.hbm.capability.HbmCapability.IHBMData;
import com.google.common.collect.Multimap;
import com.hbm.handler.ArmorModHandler;
import com.hbm.items.ModItems;
import com.hbm.config.PotionConfig;
import com.hbm.main.ClientProxy;

import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.init.MobEffects;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.ItemArmor;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;

public class ItemModGoggles extends ItemArmorMod {

	public ItemModGoggles(String s){
		super(ArmorModHandler.helmet_only, true, false, false, false, s);
	}
	
	@Override
	public void addInformation(ItemStack stack, World worldIn, List<String> list, ITooltipFlag flagIn){
		if(this == ModItems.night_goggles) {
			list.add(TextFormatting.DARK_PURPLE + "Grants Night Vision");
		}
		
		list.add("");
		super.addInformation(stack, worldIn, list, flagIn);
	}
	
	@Override
	public void addDesc(List<String> list, ItemStack stack, ItemStack armor){
		ItemArmor item = (ItemArmor)armor.getItem();
		
		if(item.armorType == EntityEquipmentSlot.HEAD) {

			if(this == ModItems.night_goggles) {
				list.add(TextFormatting.DARK_PURPLE + "  " + stack.getDisplayName() + " (Night Vision)");
			}
		}
	}
	
	@Override
	public void modUpdate(EntityLivingBase entity, ItemStack armor){
		if(entity.world.isRemote) return;

		ItemArmor item = (ItemArmor)armor.getItem();
		IHBMData props = HbmCapability.getData(entity);
		
		if(item.armorType == EntityEquipmentSlot.HEAD) {

			if(this == ModItems.night_goggles) {
				if(props.getEnableGoggles()){
					entity.addPotionEffect(new PotionEffect(MobEffects.NIGHT_VISION, 210, 0, false, false));
				} else {
					PotionEffect effect = entity.getActivePotionEffect(MobEffects.NIGHT_VISION);
					if(effect != null && effect.getDuration() <= 210 && effect.getAmplifier() == 0) {
						entity.removePotionEffect(MobEffects.NIGHT_VISION);
					}
				}
			}
		}
	}
}
