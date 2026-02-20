package com.hbm.items.gear;

import com.hbm.items.ModItems;
import com.hbm.items.armor.ArmorFSBPowered;
import com.hbm.render.model.ModelT45MURBoots;
import com.hbm.render.model.ModelT45MURChest;
import com.hbm.render.model.ModelT45MURHelmet;
import com.hbm.render.model.ModelT45MURLegs;

import net.minecraft.client.model.ModelBiped;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public class ArmorT45MUR extends ArmorFSBPowered {

	@SideOnly(Side.CLIENT)
	private ModelT45MURHelmet helmet;
	@SideOnly(Side.CLIENT)
	private ModelT45MURChest plate;
	@SideOnly(Side.CLIENT)
	private ModelT45MURLegs legs;
	@SideOnly(Side.CLIENT)
	private ModelT45MURBoots boots;
	
	public ArmorT45MUR(ArmorMaterial materialIn, int renderIndexIn, EntityEquipmentSlot equipmentSlotIn, long maxPower, long chargeRate, long consumption, long drain, String s) {
		super(materialIn, renderIndexIn, equipmentSlotIn, "", maxPower, chargeRate, consumption, drain, s);
	}

	@Override
	public boolean isValidArmor(ItemStack stack, EntityEquipmentSlot armorType, Entity entity) {
		if (stack.getItem() == ModItems.t45_mur_helmet)
			return armorType == EntityEquipmentSlot.HEAD;
		if (stack.getItem() == ModItems.t45_mur_plate)
			return armorType == EntityEquipmentSlot.CHEST;
		if (stack.getItem() == ModItems.t45_mur_legs)
			return armorType == EntityEquipmentSlot.LEGS;
		if (stack.getItem() == ModItems.t45_mur_boots)
			return armorType == EntityEquipmentSlot.FEET;
		return super.isValidArmor(stack, armorType, entity);
	}
	
	@Override
	@SideOnly(Side.CLIENT)
	public ModelBiped getArmorModel(EntityLivingBase entityLiving, ItemStack itemStack, EntityEquipmentSlot armorSlot, ModelBiped _default) {
		if (this == ModItems.t45_mur_helmet) {
			if (armorSlot == EntityEquipmentSlot.HEAD) {
				if (this.helmet == null) {
					this.helmet = new ModelT45MURHelmet();
				}
				return this.helmet;
			}
		}
		if (this == ModItems.t45_mur_plate) {
			if (armorSlot == EntityEquipmentSlot.CHEST) {
				if (this.plate == null) {
					this.plate = new ModelT45MURChest();
				}
				return this.plate;
			}
		}
		if (this == ModItems.t45_mur_legs) {
			if (armorSlot == EntityEquipmentSlot.LEGS) {
				if (this.legs == null) {
					this.legs = new ModelT45MURLegs();
				}
				return this.legs;
			}
		}
		if (this == ModItems.t45_mur_boots) {
			if (armorSlot == EntityEquipmentSlot.FEET) {
				if (this.boots == null) {
					this.boots = new ModelT45MURBoots();
				}
				return this.boots;
			}
		}
		return super.getArmorModel(entityLiving, itemStack, armorSlot, _default);
	}
	
	@Override
	@SideOnly(Side.CLIENT)
	public String getArmorTexture(ItemStack stack, Entity entity, EntityEquipmentSlot slot, String type) {
		if (stack.getItem() == ModItems.t45_mur_helmet) {
			return "hbm:textures/armor/T45MURHelmet.png";
		}
		if (stack.getItem() == ModItems.t45_mur_plate) {
			return "hbm:textures/armor/T45MURChest.png";
		}
		if (stack.getItem() == ModItems.t45_mur_legs) {
			return "hbm:textures/armor/T45MURLegs.png";
		}
		if (stack.getItem() == ModItems.t45_mur_boots) {
			return "hbm:textures/armor/T45MURBoots.png";
		}
		return super.getArmorTexture(stack, entity, slot, type);
	}
}
