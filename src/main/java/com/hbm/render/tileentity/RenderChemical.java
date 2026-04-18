package com.hbm.render.tileentity;

import com.hbm.blocks.BlockDummyable;
import com.hbm.main.ResourceManager;
import com.hbm.tileentity.machine.TileEntityMachineChemical;
import com.hbm.util.BobMathUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidRegistry;
import org.lwjgl.opengl.GL11;

import java.io.IOException;

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
            Fluid renderFluid = getRenderFluid(chemplant);
            ResourceLocation fluidTex = getFluidTexture(renderFluid);

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

    private Fluid getRenderFluid(TileEntityMachineChemical chemplant) {
        if (chemplant.tankTypes[2] != null) {
            return chemplant.tankTypes[2];
        }
        if (chemplant.tanks[2].getFluid() != null && chemplant.tanks[2].getFluid().getFluid() != null) {
            return chemplant.tanks[2].getFluid().getFluid();
        }
        if (chemplant.tankTypes[3] != null) {
            return chemplant.tankTypes[3];
        }
        if (chemplant.tanks[3].getFluid() != null && chemplant.tanks[3].getFluid().getFluid() != null) {
            return chemplant.tanks[3].getFluid().getFluid();
        }
        if (chemplant.tanks[0].getFluid() != null && chemplant.tanks[0].getFluid().getFluid() != null) {
            return chemplant.tanks[0].getFluid().getFluid();
        }
        if (chemplant.tanks[1].getFluid() != null && chemplant.tanks[1].getFluid().getFluid() != null) {
            return chemplant.tanks[1].getFluid().getFluid();
        }
        if (chemplant.tankTypes[0] != null) {
            return chemplant.tankTypes[0];
        }
        if (chemplant.tankTypes[1] != null) {
            return chemplant.tankTypes[1];
        }
        return null;
    }

    private ResourceLocation getFluidTexture(Fluid fluid) {
        if (fluid == null) {
            return null;
        }

        if (fluid == FluidRegistry.WATER) {
            ResourceLocation waterChem = new ResourceLocation("hbm", "textures/blocks/forgefluid/water_still_chem.png");
            if (resourceExists(waterChem)) {
                return waterChem;
            }
        }

        if (fluid.getStill() == null) {
            return null;
        }

        String still = fluid.getStill().toString();
        String[] split = still.split(":", 2);

        if (split.length != 2) {
            return null;
        }

        ResourceLocation chemTexture = new ResourceLocation(split[0], "textures/" + split[1] + "_chem.png");
        if (resourceExists(chemTexture)) {
            return chemTexture;
        }

        return null;
    }

    private boolean resourceExists(ResourceLocation loc) {
        try {
            Minecraft.getMinecraft().getResourceManager().getResource(loc);
            return true;
        } catch (IOException e) {
            return false;
        }
    }
}