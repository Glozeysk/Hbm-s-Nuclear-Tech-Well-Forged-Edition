package com.hbm.render.model;

import net.minecraft.entity.item.EntityArmorStand;
import org.lwjgl.opengl.GL11;

import com.hbm.main.ResourceManager;
import com.hbm.render.loader.ModelRendererObj;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;

public class ModelArmorT51 extends ModelArmorBase {

    public ModelArmorT51(int type) {
        super(type);

        head = new ModelRendererObj(ResourceManager.armor_t51, "Helmet");
        body = new ModelRendererObj(ResourceManager.armor_t51, "Chest");
        leftArm = new ModelRendererObj(ResourceManager.armor_t51, "LeftArm").setRotationPoint(5.0F, 2.0F, 0.0F);
        rightArm = new ModelRendererObj(ResourceManager.armor_t51, "RightArm").setRotationPoint(-5.0F, 2.0F, 0.0F);
        leftLeg = new ModelRendererObj(ResourceManager.armor_t51, "LeftLeg").setRotationPoint(1.9F, 12.0F, 0.0F);
        rightLeg = new ModelRendererObj(ResourceManager.armor_t51, "RightLeg").setRotationPoint(-1.9F, 12.0F, 0.0F);
        leftFoot = new ModelRendererObj(ResourceManager.armor_t51, "LeftBoot").setRotationPoint(1.9F, 12.0F, 0.0F);
        rightFoot = new ModelRendererObj(ResourceManager.armor_t51, "RightBoot").setRotationPoint(-1.9F, 12.0F, 0.0F);
    }

    private static final float HELMET_SCALE = 1.15F;
    private static final float ARM_SCALE = 1.1F;
    private static final float BODY_SCALE = 1.05F;
    private static final float LEG_SCALE = 1.05F;

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
        GL11.glShadeModel(GL11.GL_SMOOTH);

        if (this.isChild) {
            GL11.glScalef(0.75F, 0.75F, 0.75F);
            GL11.glTranslatef(0.0F, 16.0F * par7, 0.0F);
        }

        if (type == 0) {
            Minecraft.getMinecraft().renderEngine.bindTexture(ResourceManager.t51_helmet);

            GL11.glPushMatrix();
            GL11.glTranslatef(0.0F, 0.0F, 0.0F);
            GL11.glScalef(HELMET_SCALE, HELMET_SCALE, HELMET_SCALE);
            head.render(par7);
            GL11.glPopMatrix();
        }

        if (this.isChild) {
            GL11.glScalef(0.75F, 0.75F, 0.75F);
        }

        if (type == 1) {
            Minecraft.getMinecraft().renderEngine.bindTexture(ResourceManager.t51_chest);
            GL11.glPushMatrix();
            GL11.glScalef(BODY_SCALE, BODY_SCALE, BODY_SCALE);
            body.render(par7);
            GL11.glPopMatrix();

            Minecraft.getMinecraft().renderEngine.bindTexture(ResourceManager.t51_arm);

            GL11.glPushMatrix();
            GL11.glTranslatef(5.0F * par7, 2.0F * par7, 0.0F);
            GL11.glScalef(ARM_SCALE, ARM_SCALE, ARM_SCALE);
            GL11.glTranslatef(-5.0F * par7, -2.0F * par7, 0.0F);
            leftArm.render(par7);
            GL11.glPopMatrix();

            GL11.glPushMatrix();
            GL11.glTranslatef(-5.0F * par7, 2.0F * par7, 0.0F);
            GL11.glScalef(ARM_SCALE, ARM_SCALE, ARM_SCALE);
            GL11.glTranslatef(5.0F * par7, -2.0F * par7, 0.0F);
            rightArm.render(par7);
            GL11.glPopMatrix();
        }

        if (type == 2) {
            Minecraft.getMinecraft().renderEngine.bindTexture(ResourceManager.t51_leg);

            GL11.glPushMatrix();
            GL11.glTranslatef(1.9F * par7, 12.0F * par7, 0.0F);
            GL11.glScalef(LEG_SCALE, LEG_SCALE, LEG_SCALE);
            GL11.glTranslatef(-1.9F * par7, -12.0F * par7, 0.0F);
            leftLeg.render(par7);
            GL11.glPopMatrix();

            GL11.glPushMatrix();
            GL11.glTranslatef(-1.9F * par7, 12.0F * par7, 0.0F);
            GL11.glScalef(LEG_SCALE, LEG_SCALE, LEG_SCALE);
            GL11.glTranslatef(1.9F * par7, -12.0F * par7, 0.0F);
            rightLeg.render(par7);
            GL11.glPopMatrix();
        }

        if (type == 3) {
            Minecraft.getMinecraft().renderEngine.bindTexture(ResourceManager.t51_leg);

            GL11.glPushMatrix();
            GL11.glTranslatef(1.9F * par7, 12.0F * par7, 0.0F);
            GL11.glScalef(LEG_SCALE, LEG_SCALE, LEG_SCALE);
            GL11.glTranslatef(-1.9F * par7, -12.0F * par7, 0.0F);
            leftFoot.render(par7);
            GL11.glPopMatrix();

            GL11.glPushMatrix();
            GL11.glTranslatef(-1.9F * par7, 12.0F * par7, 0.0F);
            GL11.glScalef(LEG_SCALE, LEG_SCALE, LEG_SCALE);
            GL11.glTranslatef(1.9F * par7, -12.0F * par7, 0.0F);
            rightFoot.render(par7);
            GL11.glPopMatrix();
        }

        GL11.glShadeModel(GL11.GL_FLAT);
        GL11.glPopMatrix();
    }
}