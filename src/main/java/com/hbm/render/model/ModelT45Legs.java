package com.hbm.render.model;

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

public class ModelT45Legs extends ModelBiped {
    ModelRenderer leftleg;
    ModelRenderer rightleg;
    ModelRenderer Shape1;
    ModelRenderer Shape2;
    ModelRenderer Shape3;
    ModelRenderer Shape4;
    ModelRenderer Shape5;
    ModelRenderer Shape6;

    public ModelT45Legs() {
        textureWidth = 64;
        textureHeight = 32;

        leftleg = new ModelRenderer(this, 0, 0);
        rightleg = new ModelRenderer(this, 0, 0);
        Shape1 = new ModelRenderer(this, 0, 0);
        Shape1.addBox(0F, 0F, 0F, 4, 12, 4);
        Shape1.setRotationPoint(-4F + 2, 0F - 0.5F, -2F);
        Shape1.setTextureSize(64, 32);
        Shape1.mirror = true;
        setRotation(Shape1, 0F, 0F, 0F);
        convertToChild(rightleg, Shape1);
        Shape2 = new ModelRenderer(this, 16, 0);
        Shape2.addBox(0F, 0F, 0F, 4, 12, 4);
        Shape2.setRotationPoint(0F - 2, 0F - 0.5F, -2F);
        Shape2.setTextureSize(64, 32);
        Shape2.mirror = true;
        setRotation(Shape2, 0F, 0F, 0F);
        convertToChild(leftleg, Shape2);
        Shape3 = new ModelRenderer(this, 0, 16);
        Shape3.addBox(0F, -6F, 0F, 5, 6, 4);
        Shape3.setRotationPoint(-5F + 2, 10F - 0.5F, -2F);
        Shape3.setTextureSize(64, 32);
        Shape3.mirror = true;
        setRotation(Shape3, 0.1745329F, 0F, 0F);
        convertToChild(rightleg, Shape3);
        Shape4 = new ModelRenderer(this, 18, 16);
        Shape4.addBox(0F, -6F, 0F, 5, 6, 4);
        Shape4.setRotationPoint(0F - 2, 10F - 0.5F, -2F);
        Shape4.setTextureSize(64, 32);
        Shape4.mirror = true;
        setRotation(Shape4, 0.1745329F, 0F, 0F);
        convertToChild(leftleg, Shape4);
        Shape5 = new ModelRenderer(this, 34, 0);
        Shape5.addBox(0F, 0F, 0F, 5, 2, 4);
        Shape5.setRotationPoint(-5F + 2, 1F - 0.5F, -3F);
        Shape5.setTextureSize(64, 32);
        Shape5.mirror = true;
        setRotation(Shape5, 0F, 0F, 0F);
        convertToChild(rightleg, Shape5);
        Shape6 = new ModelRenderer(this, 34, 8);
        Shape6.addBox(0F, 0F, 0F, 5, 2, 4);
        Shape6.setRotationPoint(0F - 2, 1F - 0.5F, -3F);
        Shape6.setTextureSize(64, 32);
        Shape6.mirror = true;
        setRotation(Shape6, 0F, 0F, 0F);
        convertToChild(leftleg, Shape6);
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
                    syncFromModel(this.bipedBody, playerModel.bipedBody);
                    syncFromModel(this.bipedLeftArm, playerModel.bipedLeftArm);
                    syncFromModel(this.bipedRightArm, playerModel.bipedRightArm);
                    syncFromModel(this.bipedLeftLeg, playerModel.bipedLeftLeg);
                    syncFromModel(this.bipedRightLeg, playerModel.bipedRightLeg);
                }
            }
        }

        this.leftleg.rotationPointX = this.bipedLeftLeg.rotationPointX;
        this.leftleg.rotationPointY = this.bipedLeftLeg.rotationPointY - 1.5F;
        this.leftleg.rotationPointZ = this.bipedLeftLeg.rotationPointZ;
        this.leftleg.rotateAngleX = this.bipedLeftLeg.rotateAngleX;
        this.leftleg.rotateAngleY = this.bipedLeftLeg.rotateAngleY;
        this.leftleg.rotateAngleZ = this.bipedLeftLeg.rotateAngleZ;

        this.rightleg.rotationPointX = this.bipedRightLeg.rotationPointX;
        this.rightleg.rotationPointY = this.bipedRightLeg.rotationPointY - 1.5F;
        this.rightleg.rotationPointZ = this.bipedRightLeg.rotationPointZ;
        this.rightleg.rotateAngleX = this.bipedRightLeg.rotateAngleX;
        this.rightleg.rotateAngleY = this.bipedRightLeg.rotateAngleY;
        this.rightleg.rotateAngleZ = this.bipedRightLeg.rotateAngleZ;

        if (this.isSneak) {
            this.rightleg.offsetZ = 0.25F;
            this.leftleg.offsetZ = 0.25F;

            this.rightleg.rotationPointY = 11F;
            this.leftleg.rotationPointY = 11F;

            this.rightleg.rotationPointZ = -0.0625F;
            this.leftleg.rotationPointZ = -0.0625F;

        } else {
            this.rightleg.offsetZ = 0F;
            this.leftleg.offsetZ = 0F;
        }
    }

    @Override
    public void render(Entity par1Entity, float par2, float par3, float par4, float par5, float par6, float par7) {
        setRotationAngles(par2, par3, par4, par5, par6, par7, par1Entity);
        GL11.glPushMatrix();
        GL11.glScalef(1.13F, 1.13F, 1.13F);
        if(this.isChild) {
            GL11.glScalef(0.75F, 0.75F, 0.75F);
            GL11.glTranslatef(0.0F, 16.0F * par7, 0.0F);
            GL11.glScalef(0.75F, 0.75F, 0.75F);
        }
        this.leftleg.render(par7);

        this.rightleg.render(par7);
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