package com.hbm.render.layer;

import com.hbm.handler.ArmorModHandler;
import com.hbm.items.armor.ItemArmorMod;
import com.hbm.items.armor.JetpackBase;
import com.hbm.render.model.ModelJetPack;

import net.minecraft.client.Minecraft;
import net.minecraft.client.model.ModelBiped;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.entity.RenderLivingBase;
import net.minecraft.client.renderer.entity.layers.LayerRenderer;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class LayerJetpack implements LayerRenderer<EntityPlayer> {

	private final RenderLivingBase<?> renderer;
	private ModelJetPack modelJetpack = new ModelJetPack();

	public LayerJetpack(RenderLivingBase<?> renderer) {
		this.renderer = renderer;
	}

	@Override
	public void doRenderLayer(EntityPlayer player, float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks, float netHeadYaw, float headPitch, float scale) {

		JetpackBase jetpackItem = null;
		ItemStack jetpackStack = null;

		for(int i = 0; i < 4; i++) {
			ItemStack armor = player.inventory.armorItemInSlot(i);

			if(armor != null && !armor.isEmpty()) {
				// Проверяем как мод в слоте брони
				if(ArmorModHandler.hasMods(armor)) {
					ItemStack[] mods = ArmorModHandler.pryMods(armor);
					if(mods != null) {
						for(ItemStack mod : mods) {
							if(mod != null && !mod.isEmpty() && mod.getItem() instanceof JetpackBase) {
								jetpackItem = (JetpackBase) mod.getItem();
								jetpackStack = mod;
								break;
							}
						}
					}
				}

				// Проверяем как самостоятельный предмет брони
				if(jetpackItem == null && armor.getItem() instanceof JetpackBase) {
					jetpackItem = (JetpackBase) armor.getItem();
					jetpackStack = armor;
				}
			}

			if(jetpackItem != null) break;
		}

		if(jetpackItem == null || jetpackStack == null) return;

		ModelBiped playerModel = (ModelBiped) this.renderer.getMainModel();

		modelJetpack.syncBodyRotation(playerModel);

		Minecraft.getMinecraft().renderEngine.bindTexture(new ResourceLocation(
				jetpackItem.getArmorTexture(jetpackStack, player, EntityEquipmentSlot.CHEST, null)));

		modelJetpack.renderJetpack(scale);
	}

	@Override
	public boolean shouldCombineTextures() {
		return false;
	}
}