package com.hbm.render.tileentity;

import com.hbm.blocks.BlockDummyable;
import com.hbm.main.ResourceManager;
import com.hbm.tileentity.machine.TileEntityMachineChemical;
import com.hbm.util.BobMathUtil;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fluids.FluidTank;
import org.lwjgl.opengl.GL11;

public class RenderChemical extends TileEntitySpecialRenderer<TileEntityMachineChemical> {

    @Override
    public void render(TileEntityMachineChemical chemplant, double x, double y, double z, float interp, int destroyStage, float alpha) {
        GlStateManager.enableAlpha();
        GlStateManager.pushMatrix();
        GlStateManager.translate(x + 0.5, y, z + 0.5);
        GlStateManager.rotate(90F, 0F, 1F, 0F);
        GlStateManager.shadeModel(GL11.GL_SMOOTH);

        switch (chemplant.getBlockMetadata() - BlockDummyable.offset) {
            case 2 -> GlStateManager.rotate(0F, 0F, 1F, 0F);
            case 4 -> GlStateManager.rotate(90F, 0F, 1F, 0F);
            case 3 -> GlStateManager.rotate(180F, 0F, 1F, 0F);
            case 5 -> GlStateManager.rotate(270F, 0F, 1F, 0F);
        }
        float anim = chemplant.prevAnim + (chemplant.anim - chemplant.prevAnim) * interp;

        bindTexture(ResourceManager.chemical_tex);
        ResourceManager.chemical.renderPart("Base");
        if (chemplant.frame) ResourceManager.chemical.renderPart("Frame");

        GlStateManager.pushMatrix();
        GlStateManager.translate(BobMathUtil.sps(anim * 0.125) * 0.375, 0, 0);
        ResourceManager.chemical.renderPart("Slider");
        GlStateManager.popMatrix();

        GlStateManager.pushMatrix();
        GlStateManager.translate(0.5, 0, 0.5);
        GlStateManager.rotate((anim * 15) % 360F, 0, 1, 0);
        GlStateManager.translate(-0.5, 0, -0.5);
        ResourceManager.chemical.renderPart("Spinner");
        GlStateManager.popMatrix();

        if (chemplant.isProgressing) {
            ResourceLocation fluidTex = null;

            if (chemplant.tanks[2].getFluid() != null && chemplant.tanks[2].getFluidAmount() > 0
                    && chemplant.tanks[2].getFluid().getFluid() != null
                    && chemplant.tanks[2].getFluid().getFluid().getStill() != null) {
                String s = chemplant.tanks[2].getFluid().getFluid().getStill().toString();
                String[] test1 = s.split(":");
                fluidTex = new ResourceLocation(test1[0] + ":textures/" + test1[1] + "_chem.png");
            } else if (chemplant.tankTypes[2] != null && chemplant.tankTypes[2].getStill() != null) {
                String s = chemplant.tankTypes[2].getStill().toString();
                String[] test1 = s.split(":");
                fluidTex = new ResourceLocation(test1[0] + ":textures/" + test1[1] + "_chem.png");
            } else if (chemplant.tanks[0].getFluid() != null && chemplant.tanks[0].getFluidAmount() > 0
                    && chemplant.tanks[0].getFluid().getFluid() != null
                    && chemplant.tanks[0].getFluid().getFluid().getStill() != null) {
                String s = chemplant.tanks[0].getFluid().getFluid().getStill().toString();
                String[] test1 = s.split(":");
                fluidTex = new ResourceLocation(test1[0] + ":textures/" + test1[1] + "_chem.png");
            }

            if (fluidTex != null) {
                GlStateManager.enableBlend();
                GlStateManager.depthMask(false);
                GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
                bindTexture(fluidTex);
                ResourceManager.chemical.renderPart("Fluid");
                GlStateManager.depthMask(true);
                GlStateManager.disableBlend();
            }
        }

        GlStateManager.shadeModel(GL11.GL_FLAT);
        GlStateManager.popMatrix();
    }
}