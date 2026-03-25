package com.hbm.render.model;

import net.minecraft.entity.item.EntityArmorStand;
import org.lwjgl.opengl.GL11;

import net.minecraft.client.Minecraft;
import net.minecraft.client.model.ModelBase;
import net.minecraft.client.model.ModelBiped;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.client.renderer.entity.RenderLivingBase;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;

public class ModelGoggles extends ModelBiped {

    ModelRenderer Shape1;
    ModelRenderer Shape2;
    ModelRenderer Shape5;
    ModelRenderer Shape6;
    ModelRenderer Shape7;
    ModelRenderer google;

    public ModelGoggles() {
        textureWidth = 64;
        textureHeight = 32;

        google = new ModelRenderer(this, 0, 0);
        Shape1 = new ModelRenderer(this, 0, 0);
        Shape1.addBox(0F, 0F, 0F, 9, 3, 1);
        Shape1.setRotationPoint(-4.5F, -3F - 2, -4.5F);
        Shape1.setTextureSize(64, 32);
        Shape1.mirror = true;
        setRotation(Shape1, 0F, 0F, 0F);
        convertToChild(google, Shape1);
        Shape2 = new ModelRenderer(this, 0, 4);
        Shape2.addBox(0F, 0F, 0F, 9, 2, 5);
        Shape2.setRotationPoint(-4.5F, -3F - 2, -3.5F);
        Shape2.setTextureSize(64, 32);
        Shape2.mirror = true;
        setRotation(Shape2, 0F, 0F, 0F);
        convertToChild(google, Shape2);
        Shape5 = new ModelRenderer(this, 26, 0);
        Shape5.addBox(0F, 0F, 0F, 2, 2, 1);
        Shape5.setRotationPoint(1F, -2.5F - 2, -5F);
        Shape5.setTextureSize(64, 32);
        Shape5.mirror = true;
        setRotation(Shape5, 0F, 0F, 0F);
        convertToChild(google, Shape5);
        Shape6 = new ModelRenderer(this, 20, 0);
        Shape6.addBox(0F, 0F, 0F, 2, 2, 1);
        Shape6.setRotationPoint(-3F, -2.5F - 2, -5F);
        Shape6.setTextureSize(64, 32);
        Shape6.mirror = true;
        setRotation(Shape6, 0F, 0F, 0F);
        convertToChild(google, Shape6);
        Shape7 = new ModelRenderer(this, 0, 11);
        Shape7.addBox(0F, 0F, 0F, 9, 1, 4);
        Shape7.setRotationPoint(-4.5F, -3F - 2, 0.5F);
        Shape7.setTextureSize(64, 32);
        Shape7.mirror = true;
        setRotation(Shape7, 0F, 0F, 0F);
        convertToChild(google, Shape7);
    }

    private void setRotation(ModelRenderer model, float x, float y, float z) {
        model.rotateAngleX = x;
        model.rotateAngleY = y;
        model.rotateAngleZ = z;
    }

    private static void syncFromModel(ModelRenderer target, ModelRenderer source) {
        target.rotateAngleX = source.rotateAngleX;
        target.rotateAngleY = source.rotateAngleY;
        target.rotateAngleZ = source.rotateAngleZ;
    }

    @Override
    public void setRotationAngles(float f, float f1, float f2, float f3, float f4, float f5, Entity entity) {

        if (entity instanceof EntityPlayer) {
            EntityPlayer player = (EntityPlayer) entity;
            if (player.isSneaking()) {
                this.isSneak = true;
            } else {
                this.isSneak = false;
            }
        }

        super.setRotationAngles(f, f1, f2, f3, f4, f5, entity);

        if(entity instanceof EntityLivingBase) {
            Render<?> render = Minecraft.getMinecraft().getRenderManager().getEntityRenderObject((Entity) entity);
            if(render instanceof RenderLivingBase) {
                ModelBase mainModel = ((RenderLivingBase<?>) render).getMainModel();
                if(mainModel instanceof ModelBiped && mainModel != this) {
                    ModelBiped playerModel = (ModelBiped) mainModel;
                    syncFromModel(this.bipedHead, playerModel.bipedHead);
                    syncFromModel(this.bipedHeadwear, playerModel.bipedHeadwear);
                }
            }
        }

        if (entity instanceof EntityArmorStand stand) {
            this.google.rotateAngleX = (float) Math.toRadians(stand.getHeadRotation().getX());
            this.google.rotateAngleY = (float) Math.toRadians(stand.getHeadRotation().getY());
            this.google.rotateAngleZ = (float) Math.toRadians(stand.getHeadRotation().getZ());
        } else {
            this.google.rotationPointX = this.bipedHead.rotationPointX;
            this.google.rotationPointY = this.bipedHead.rotationPointY;
            this.google.rotationPointZ = this.bipedHead.rotationPointZ;
            this.google.rotateAngleY = this.bipedHead.rotateAngleY;
            this.google.rotateAngleX = this.bipedHead.rotateAngleX;
            this.google.rotateAngleZ = this.bipedHead.rotateAngleZ;
        }

        if(this.isSneak) {
            this.google.rotationPointY = 3.73F;
        }
    }
    @Override
    public void render(Entity par1Entity, float par2, float par3, float par4, float par5, float par6, float par7) {
        setRotationAngles(par2, par3, par4, par5, par6, par7, par1Entity);
        GL11.glPushMatrix();
        GL11.glScalef(1.001F, 1.001F, 1.001F);
        this.google.render(par7);
        GL11.glPopMatrix();
    }

    protected void convertToChild(ModelRenderer parParent, ModelRenderer parChild) {
        parChild.rotationPointX -= parParent.rotationPointX;
        parChild.rotationPointY -= parParent.rotationPointY;
        parChild.rotationPointZ -= parParent.rotationPointZ;
        parChild.rotateAngleX -= parParent.rotateAngleX;
        parChild.rotateAngleY -= parParent.rotateAngleY;
        parChild.rotateAngleZ -= parParent.rotateAngleZ;
        parParent.addChild(parChild);
    }
}