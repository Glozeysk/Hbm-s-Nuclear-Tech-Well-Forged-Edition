package com.hbm.items.armor;

import com.hbm.render.model.ModelArmorAJRAlpha;

import net.minecraft.client.model.ModelBiped;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public class ArmorAJRAlpha extends ArmorFSBPowered {

	public ArmorAJRAlpha(ArmorMaterial material, int layer, EntityEquipmentSlot slot, String texture, long maxPower, long chargeRate, long consumption, long drain, String s) {
		super(material, layer, slot, texture, maxPower, chargeRate, consumption, drain, s);
	}

	@SideOnly(Side.CLIENT)
	ModelArmorAJRAlpha[] models;

	@Override
	@SideOnly(Side.CLIENT)
	public ModelBiped getArmorModel(EntityLivingBase entityLiving, ItemStack itemStack, EntityEquipmentSlot armorSlot, ModelBiped _default){
		if(models == null) {
			models = new ModelArmorAJRAlpha[4];
			
			for(int i = 0; i < 4; i++)
				models[i] = new ModelArmorAJRAlpha(i);
		}
		
		return models[armorSlot.getIndex()];
	}
	
}
