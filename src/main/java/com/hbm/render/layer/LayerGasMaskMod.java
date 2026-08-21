package com.hbm.render.layer;

import com.hbm.handler.ArmorModHandler;
import com.hbm.handler.ArmorUtil;
import com.hbm.items.ModItems;
import com.hbm.render.model.ModelM65;

import net.minecraft.client.Minecraft;
import net.minecraft.client.model.ModelBiped;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.entity.RenderLivingBase;
import net.minecraft.client.renderer.entity.layers.LayerRenderer;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/** Рендер прикреплённого противогаза (перенесён сюда из ItemModGasmask.modRender). Привязка к голове — как у джетпака к телу. */
@SideOnly(Side.CLIENT)
public class LayerGasMaskMod implements LayerRenderer<EntityPlayer> {

	private final RenderLivingBase<?> renderer;
	private final ModelM65 modelM65 = new ModelM65();

	private static final ResourceLocation TEX = new ResourceLocation("hbm:textures/armor/ModelM65.png");
	private static final ResourceLocation TEX_MONO = new ResourceLocation("hbm:textures/armor/ModelM65Mono.png");

	public LayerGasMaskMod(RenderLivingBase<?> renderer) {
		this.renderer = renderer;
	}

	@Override
	public void doRenderLayer(EntityPlayer player, float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks, float netHeadYaw, float headPitch, float scale) {

		// helmet_only — только слот головы (3)
		ItemStack helmet = player.inventory.armorItemInSlot(3);
		if(helmet == null || helmet.isEmpty() || !ArmorModHandler.hasMods(helmet))
			return;

		ItemStack[] mods = ArmorModHandler.pryMods(helmet);
		if(mods == null)
			return;

		ItemStack mod = mods[ArmorModHandler.helmet_only];
		if(mod == null || mod.isEmpty())
			return;

		Item maskItem = mod.getItem();
		if(maskItem != ModItems.attachment_mask && maskItem != ModItems.attachment_mask_mono)
			return;

		// Привязываем маску и фильтр к голове ПОЗИРОВАННОЙ модели именно этого рендера (как ModelJetPack.syncBodyRotation, но к bipedHead).
		ModelBiped main = (ModelBiped) this.renderer.getMainModel();
		syncTo(modelM65.mask, main.bipedHead);
		syncTo(modelM65.filter, main.bipedHead);

		Minecraft.getMinecraft().renderEngine.bindTexture(maskItem == ModItems.attachment_mask_mono ? TEX_MONO : TEX);

		GlStateManager.pushMatrix();
		double d = 1D / 16D * 18D * 1.01D; // тот же масштаб, что в ModelM65.render
		GlStateManager.scale(d, d, d);
		modelM65.mask.render(scale);
		if(ArmorUtil.getGasMaskFilterRecursively(helmet) != null)
			modelM65.filter.render(scale);
		GlStateManager.popMatrix();
	}

	private static void syncTo(ModelRenderer part, ModelRenderer bone) {
		part.rotateAngleX = bone.rotateAngleX;
		part.rotateAngleY = bone.rotateAngleY;
		part.rotateAngleZ = bone.rotateAngleZ;
		part.rotationPointX = bone.rotationPointX;
		part.rotationPointY = bone.rotationPointY;
		part.rotationPointZ = bone.rotationPointZ;
		part.offsetX = bone.offsetX;
		part.offsetY = bone.offsetY;
		part.offsetZ = bone.offsetZ;
	}

	@Override
	public boolean shouldCombineTextures() {
		return false;
	}
}
