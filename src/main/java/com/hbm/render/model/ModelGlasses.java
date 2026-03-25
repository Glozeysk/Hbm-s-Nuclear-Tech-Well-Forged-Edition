package com.hbm.render.model;

import net.minecraft.entity.item.EntityArmorStand;
import org.lwjgl.opengl.GL11;

import com.hbm.main.ResourceManager;
import com.hbm.render.loader.ModelRendererObj;

import net.minecraft.client.Minecraft;
import net.minecraft.client.model.ModelBase;
import net.minecraft.client.model.ModelBiped;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.client.renderer.entity.RenderLivingBase;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;

public class ModelGlasses extends ModelArmorBase {

    public ModelGlasses(int type) {
        super(type);

        head = new ModelRendererObj(ResourceManager.armor_goggles);
        body = new ModelRendererObj(ResourceManager.armor_bj, "Body");
        leftArm = new ModelRendererObj(ResourceManager.armor_bj, "LeftArm").setRotationPoint(-5.0F, 2.0F, 0.0F);
        rightArm = new ModelRendererObj(ResourceManager.armor_bj, "RightArm").setRotationPoint(5.0F, 2.0F, 0.0F);
        leftLeg = new ModelRendererObj(ResourceManager.armor_bj, "LeftLeg").setRotationPoint(1.9F, 12.0F, 0.0F);
        rightLeg = new ModelRendererObj(ResourceManager.armor_bj, "RightLeg").setRotationPoint(-1.9F, 12.0F, 0.0F);
        leftFoot = new ModelRendererObj(ResourceManager.armor_bj, "LeftFoot").setRotationPoint(1.9F, 12.0F, 0.0F);
        rightFoot = new ModelRendererObj(ResourceManager.armor_bj, "RightFoot").setRotationPoint(-1.9F, 12.0F, 0.0F);
    }

    private static void syncFromModel(ModelRenderer target, ModelRenderer source) {
        target.rotateAngleX = source.rotateAngleX;
        target.rotateAngleY = source.rotateAngleY;
        target.rotateAngleZ = source.rotateAngleZ;
    }

    @Override
    public void render(Entity par1Entity, float par2, float par3, float par4, float par5, float par6, float par7) {
        setRotationAngles(par2, par3, par4, par5, par6, par7, par1Entity);

        if (par1Entity instanceof EntityArmorStand) {
            EntityArmorStand stand = (EntityArmorStand) par1Entity;
            head.rotateAngleX = (float) stand.getHeadRotation().getX() * 0.017453292F;
            head.rotateAngleY = (float) stand.getHeadRotation().getY() * 0.017453292F;
            head.rotateAngleZ = (float) stand.getHeadRotation().getZ() * 0.017453292F;
        }

        GL11.glPushMatrix();

        if(type == 0) {
            Minecraft.getMinecraft().renderEngine.bindTexture(ResourceManager.goggles);
            head.render(par7*1.001F);
        }

        GL11.glPopMatrix();
    }
}