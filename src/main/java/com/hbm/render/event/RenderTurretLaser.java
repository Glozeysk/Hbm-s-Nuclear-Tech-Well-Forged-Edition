package com.hbm.render.event;

import java.util.Iterator;
import java.util.Map;

import com.hbm.items.tool.ItemTurretControl;
import com.hbm.items.tool.ItemTurretControl.LaserData;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderGlobal;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import org.lwjgl.opengl.GL11;

@SideOnly(Side.CLIENT)
public class RenderTurretLaser {

    @SubscribeEvent
    public void onRenderWorldLast(RenderWorldLastEvent event) {
        Minecraft mc = Minecraft.getMinecraft();
        EntityPlayer player = mc.player;
        if(player == null) return;

        float pt = event.getPartialTicks();
        double camX = player.lastTickPosX + (player.posX - player.lastTickPosX) * pt;
        double camY = player.lastTickPosY + (player.posY - player.lastTickPosY) * pt;
        double camZ = player.lastTickPosZ + (player.posZ - player.lastTickPosZ) * pt;

        long worldTime = player.world.getTotalWorldTime();

        Iterator<Map.Entry<Integer, LaserData>> it = ItemTurretControl.activeLasers.entrySet().iterator();
        while(it.hasNext()) {
            Map.Entry<Integer, LaserData> entry = it.next();
            LaserData data = entry.getValue();

            if(!data.active) {
                if(worldTime - data.timestamp > 20) it.remove();
                continue;
            }
            if(worldTime - data.timestamp > 5) {
                data.active = false;
                continue;
            }

            boolean visible = data.targetVisible || data.ignoreWalls;

            float laserR, laserG, laserB, laserA;
            float glowR, glowG, glowB, glowA;

            if(visible) {
                laserR = 1.0F; laserG = 0.0F; laserB = 0.0F; laserA = 0.9F;
                glowR = 1.0F; glowG = 0.2F; glowB = 0.2F; glowA = 0.25F;
            } else {
                laserR = 0.5F; laserG = 0.5F; laserB = 0.5F; laserA = 0.4F;
                glowR = 0.5F; glowG = 0.5F; glowB = 0.5F; glowA = 0.1F;
            }

            double sx = data.startX - camX;
            double sy = data.startY - camY;
            double sz = data.startZ - camZ;
            double ex = data.endX - camX;
            double ey = data.endY - camY;
            double ez = data.endZ - camZ;

            GlStateManager.pushMatrix();
            GlStateManager.disableTexture2D();
            GlStateManager.disableLighting();
            GlStateManager.enableBlend();
            GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
            GlStateManager.disableDepth();

            Tessellator tess = Tessellator.getInstance();
            BufferBuilder buf = tess.getBuffer();

            GL11.glLineWidth(3.0F);
            buf.begin(GL11.GL_LINES, DefaultVertexFormats.POSITION_COLOR);
            buf.pos(sx, sy, sz).color(laserR, laserG, laserB, laserA).endVertex();
            buf.pos(ex, ey, ez).color(laserR, laserG, laserB, laserA).endVertex();
            tess.draw();

            GL11.glLineWidth(8.0F);
            buf.begin(GL11.GL_LINES, DefaultVertexFormats.POSITION_COLOR);
            buf.pos(sx, sy, sz).color(glowR, glowG, glowB, glowA).endVertex();
            buf.pos(ex, ey, ez).color(glowR, glowG, glowB, glowA).endVertex();
            tess.draw();

            if(visible) {
                renderDot(ex, ey, ez, laserR, laserG, laserB);
            }

            if(data.targetIsEntity && data.targetEntityId >= 0) {
                Entity targetEntity = mc.world.getEntityByID(data.targetEntityId);
                if(targetEntity != null) {
                    renderEntityHighlight(targetEntity, camX, camY, camZ, pt, visible, data.focusMode);
                }
            }

            GL11.glLineWidth(1.0F);
            GlStateManager.enableDepth();
            GlStateManager.disableBlend();
            GlStateManager.enableLighting();
            GlStateManager.enableTexture2D();
            GlStateManager.popMatrix();
        }
    }

    private void renderEntityHighlight(Entity entity, double camX, double camY, double camZ, float pt, boolean visible, boolean focusMode) {
        double entX = entity.lastTickPosX + (entity.posX - entity.lastTickPosX) * pt;
        double entY = entity.lastTickPosY + (entity.posY - entity.lastTickPosY) * pt;
        double entZ = entity.lastTickPosZ + (entity.posZ - entity.lastTickPosZ) * pt;

        AxisAlignedBB bb = entity.getEntityBoundingBox();
        AxisAlignedBB renderBB = new AxisAlignedBB(
                bb.minX - entX + (entX - camX),
                bb.minY - entY + (entY - camY),
                bb.minZ - entZ + (entZ - camZ),
                bb.maxX - entX + (entX - camX),
                bb.maxY - entY + (entY - camY),
                bb.maxZ - entZ + (entZ - camZ)
        );

        float r, g, b, a;
        if(visible) {
            r = 1.0F; g = 0.0F; b = 0.0F; a = 0.8F;
        } else {
            r = 0.5F; g = 0.5F; b = 0.5F; a = 0.4F;
        }

        float lineWidth = focusMode ? 3.5F : 2.5F;

        GL11.glLineWidth(lineWidth);
        GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
        RenderGlobal.drawSelectionBoundingBox(renderBB, r, g, b, a);

        if(focusMode && visible) {
            RenderGlobal.drawSelectionBoundingBox(renderBB.grow(0.02D), r, g * 0.3F, b * 0.3F, a * 0.5F);
        }

        GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
    }

    private void renderDot(double x, double y, double z, float r, float g, float b) {
        double s = 0.06;
        Tessellator tess = Tessellator.getInstance();
        BufferBuilder buf = tess.getBuffer();
        buf.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);

        buf.pos(x - s, y - s, z - s).color(r, g, b, 1.0F).endVertex();
        buf.pos(x + s, y - s, z - s).color(r, g, b, 1.0F).endVertex();
        buf.pos(x + s, y + s, z - s).color(r, g, b, 1.0F).endVertex();
        buf.pos(x - s, y + s, z - s).color(r, g, b, 1.0F).endVertex();

        buf.pos(x - s, y - s, z + s).color(r, g, b, 1.0F).endVertex();
        buf.pos(x + s, y - s, z + s).color(r, g, b, 1.0F).endVertex();
        buf.pos(x + s, y + s, z + s).color(r, g, b, 1.0F).endVertex();
        buf.pos(x - s, y + s, z + s).color(r, g, b, 1.0F).endVertex();

        buf.pos(x - s, y - s, z - s).color(r, g, b, 1.0F).endVertex();
        buf.pos(x - s, y - s, z + s).color(r, g, b, 1.0F).endVertex();
        buf.pos(x - s, y + s, z + s).color(r, g, b, 1.0F).endVertex();
        buf.pos(x - s, y + s, z - s).color(r, g, b, 1.0F).endVertex();

        buf.pos(x + s, y - s, z - s).color(r, g, b, 1.0F).endVertex();
        buf.pos(x + s, y - s, z + s).color(r, g, b, 1.0F).endVertex();
        buf.pos(x + s, y + s, z + s).color(r, g, b, 1.0F).endVertex();
        buf.pos(x + s, y + s, z - s).color(r, g, b, 1.0F).endVertex();

        buf.pos(x - s, y + s, z - s).color(r, g, b, 1.0F).endVertex();
        buf.pos(x + s, y + s, z - s).color(r, g, b, 1.0F).endVertex();
        buf.pos(x + s, y + s, z + s).color(r, g, b, 1.0F).endVertex();
        buf.pos(x - s, y + s, z + s).color(r, g, b, 1.0F).endVertex();

        buf.pos(x - s, y - s, z - s).color(r, g, b, 1.0F).endVertex();
        buf.pos(x + s, y - s, z - s).color(r, g, b, 1.0F).endVertex();
        buf.pos(x + s, y - s, z + s).color(r, g, b, 1.0F).endVertex();
        buf.pos(x - s, y - s, z + s).color(r, g, b, 1.0F).endVertex();

        tess.draw();
    }
}