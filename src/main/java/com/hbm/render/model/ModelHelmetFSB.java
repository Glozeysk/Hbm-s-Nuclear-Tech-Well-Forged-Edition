package com.hbm.render.model;

import net.minecraft.client.Minecraft;
import net.minecraft.client.model.ModelBase;
import net.minecraft.client.model.ModelBiped;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.client.renderer.entity.RenderLivingBase;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityArmorStand;
import org.lwjgl.opengl.GL11;

public class ModelHelmetFSB extends ModelBiped {

    private final boolean glassLayer;

    public ModelHelmetFSB(boolean glassLayer) {
        super(1.0F);
        this.glassLayer = glassLayer;
    }

    @Override
    public void render(Entity entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch, float scale) {
        this.setRotationAngles(limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch, scale, entity);

        if (entity instanceof EntityLivingBase && !(entity instanceof EntityArmorStand)) {
            try {
                Render<?> renderer = Minecraft.getMinecraft().getRenderManager().getEntityRenderObject(entity);
                if (renderer instanceof RenderLivingBase) {
                    ModelBase mainModel = ((RenderLivingBase<?>) renderer).getMainModel();
                    if (mainModel instanceof ModelBiped) {
                        ModelBiped m = (ModelBiped) mainModel;
                        copyModelRenderer(m.bipedHead, this.bipedHead);
                        copyModelRenderer(m.bipedHead, this.bipedHeadwear);
                    }
                }
            } catch (Exception ignored) {}
        }

        GlStateManager.pushMatrix();

        if (this.isChild) {
            GlStateManager.scale(0.75F, 0.75F, 0.75F);
            GlStateManager.translate(0.0F, 16.0F * scale, 0.0F);
        } else if (entity != null && entity.isSneaking()) {
            GlStateManager.translate(0.0F, 0.2F, 0.0F);
        }

        if (glassLayer) {
            this.bipedHead.render(scale);
            GlStateManager.enableBlend();
            GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
            this.bipedHeadwear.render(scale);
            GlStateManager.disableBlend();
        } else {
            this.bipedHead.render(scale);
            this.bipedHeadwear.render(scale);
        }

        GlStateManager.popMatrix();
    }

    private static void copyModelRenderer(ModelRenderer from, ModelRenderer to) {
        to.rotateAngleX = from.rotateAngleX;
        to.rotateAngleY = from.rotateAngleY;
        to.rotateAngleZ = from.rotateAngleZ;
        to.rotationPointX = from.rotationPointX;
        to.rotationPointY = from.rotationPointY;
        to.rotationPointZ = from.rotationPointZ;
    }
}