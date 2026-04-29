package com.hbm.render.item;

import com.hbm.blocks.BlockDummyable;
import com.hbm.blocks.turret.TurretBase;
import com.hbm.blocks.turret.TurretBaseNT;
import com.hbm.items.tool.ItemTurretControl;
import com.hbm.lib.ForgeDirection;

import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import org.lwjgl.opengl.GL11;

@SideOnly(Side.CLIENT)
public class RenderTurretControlOverlay {

    @SubscribeEvent
    public void onRenderWorldLast(RenderWorldLastEvent event) {
        Minecraft mc = Minecraft.getMinecraft();
        EntityPlayer player = mc.player;
        World world = mc.world;

        if(player == null || world == null) return;

        ItemStack stack = player.getHeldItem(EnumHand.MAIN_HAND);
        if(stack.isEmpty() || !(stack.getItem() instanceof ItemTurretControl)) {
            stack = player.getHeldItem(EnumHand.OFF_HAND);
        }
        if(stack.isEmpty() || !(stack.getItem() instanceof ItemTurretControl)) return;

        NBTTagCompound nbt = stack.getTagCompound();
        if(nbt == null || !nbt.hasKey("xCoord")) return;

        int x = nbt.getInteger("xCoord");
        int y = nbt.getInteger("yCoord");
        int z = nbt.getInteger("zCoord");
        BlockPos corePos = new BlockPos(x, y, z);

        double dist = player.getDistanceSq(x + 0.5, y + 0.5, z + 0.5);
        if(dist > 10000) return;

        Block block = world.getBlockState(corePos).getBlock();
        boolean isSmall = block instanceof TurretBase;
        boolean isBig = block instanceof TurretBaseNT;
        if(!isSmall && !isBig) return;

        boolean hasFocus = nbt.getBoolean("focusActive");

        float r, g, b;
        if(hasFocus) {
            r = 1.0F;
            g = 0.2F;
            b = 0.2F;
        } else {
            r = 0.2F;
            g = 1.0F;
            b = 0.4F;
        }

        float partialTicks = event.getPartialTicks();
        double dx = player.lastTickPosX + (player.posX - player.lastTickPosX) * partialTicks;
        double dy = player.lastTickPosY + (player.posY - player.lastTickPosY) * partialTicks;
        double dz = player.lastTickPosZ + (player.posZ - player.lastTickPosZ) * partialTicks;

        AxisAlignedBB bb;

        if(isSmall) {
            bb = new AxisAlignedBB(
                    corePos.getX() - 0.005D,
                    corePos.getY() - 0.005D,
                    corePos.getZ() - 0.005D,
                    corePos.getX() + 1.005D,
                    corePos.getY() + 1.005D,
                    corePos.getZ() + 1.005D
            ).offset(-dx, -dy, -dz);
        } else {
            bb = getBigTurretBB(world, corePos).grow(0.005D).offset(-dx, -dy, -dz);
        }

        GlStateManager.pushMatrix();
        GlStateManager.disableTexture2D();
        GlStateManager.disableLighting();
        GlStateManager.disableCull();
        GlStateManager.enableBlend();
        GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GlStateManager.depthMask(false);
        GlStateManager.disableDepth();

        GL11.glLineWidth(3.0F);

        Tessellator tess = Tessellator.getInstance();
        BufferBuilder buf = tess.getBuffer();

        buf.begin(GL11.GL_LINES, DefaultVertexFormats.POSITION_COLOR);

        float a = 0.85F;

        drawLine(buf, bb.minX, bb.minY, bb.minZ, bb.maxX, bb.minY, bb.minZ, r, g, b, a);
        drawLine(buf, bb.maxX, bb.minY, bb.minZ, bb.maxX, bb.minY, bb.maxZ, r, g, b, a);
        drawLine(buf, bb.maxX, bb.minY, bb.maxZ, bb.minX, bb.minY, bb.maxZ, r, g, b, a);
        drawLine(buf, bb.minX, bb.minY, bb.maxZ, bb.minX, bb.minY, bb.minZ, r, g, b, a);

        drawLine(buf, bb.minX, bb.maxY, bb.minZ, bb.maxX, bb.maxY, bb.minZ, r, g, b, a);
        drawLine(buf, bb.maxX, bb.maxY, bb.minZ, bb.maxX, bb.maxY, bb.maxZ, r, g, b, a);
        drawLine(buf, bb.maxX, bb.maxY, bb.maxZ, bb.minX, bb.maxY, bb.maxZ, r, g, b, a);
        drawLine(buf, bb.minX, bb.maxY, bb.maxZ, bb.minX, bb.maxY, bb.minZ, r, g, b, a);

        drawLine(buf, bb.minX, bb.minY, bb.minZ, bb.minX, bb.maxY, bb.minZ, r, g, b, a);
        drawLine(buf, bb.maxX, bb.minY, bb.minZ, bb.maxX, bb.maxY, bb.minZ, r, g, b, a);
        drawLine(buf, bb.maxX, bb.minY, bb.maxZ, bb.maxX, bb.maxY, bb.maxZ, r, g, b, a);
        drawLine(buf, bb.minX, bb.minY, bb.maxZ, bb.minX, bb.maxY, bb.maxZ, r, g, b, a);

        tess.draw();

        float pulseAlpha = (float)(Math.sin(System.currentTimeMillis() * 0.005D) * 0.15D + 0.15D);

        GlStateManager.enableDepth();
        buf.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);

        drawQuad(buf, bb.minX, bb.minY, bb.minZ, bb.maxX, bb.maxY, bb.minZ, r, g, b, pulseAlpha, 0);
        drawQuad(buf, bb.minX, bb.minY, bb.maxZ, bb.maxX, bb.maxY, bb.maxZ, r, g, b, pulseAlpha, 1);
        drawQuad(buf, bb.minX, bb.minY, bb.minZ, bb.minX, bb.maxY, bb.maxZ, r, g, b, pulseAlpha, 2);
        drawQuad(buf, bb.maxX, bb.minY, bb.minZ, bb.maxX, bb.maxY, bb.maxZ, r, g, b, pulseAlpha, 3);
        drawQuad(buf, bb.minX, bb.minY, bb.minZ, bb.maxX, bb.minY, bb.maxZ, r, g, b, pulseAlpha, 4);
        drawQuad(buf, bb.minX, bb.maxY, bb.minZ, bb.maxX, bb.maxY, bb.maxZ, r, g, b, pulseAlpha, 5);

        tess.draw();

        GlStateManager.depthMask(true);
        GlStateManager.enableDepth();
        GlStateManager.disableBlend();
        GlStateManager.enableCull();
        GlStateManager.enableLighting();
        GlStateManager.enableTexture2D();
        GlStateManager.popMatrix();
    }

    private AxisAlignedBB getBigTurretBB(World world, BlockPos corePos) {
        TileEntity te = world.getTileEntity(corePos);
        if(te == null) {
            return new AxisAlignedBB(corePos);
        }

        int meta = te.getBlockMetadata() - BlockDummyable.offset;

        ForgeDirection dir = ForgeDirection.getOrientation(meta).getOpposite();
        ForgeDirection rot = dir.getRotation(ForgeDirection.UP);

        int minX = corePos.getX();
        int minY = corePos.getY();
        int minZ = corePos.getZ();
        int maxX = corePos.getX() + 1;
        int maxY = corePos.getY() + 1;
        int maxZ = corePos.getZ() + 1;

        int x2 = corePos.getX() + dir.offsetX;
        int z2 = corePos.getZ() + dir.offsetZ;
        int x3 = corePos.getX() - rot.offsetX;
        int z3 = corePos.getZ() - rot.offsetZ;
        int x4 = corePos.getX() + dir.offsetX - rot.offsetX;
        int z4 = corePos.getZ() + dir.offsetZ - rot.offsetZ;

        minX = Math.min(minX, Math.min(x2, Math.min(x3, x4)));
        minZ = Math.min(minZ, Math.min(z2, Math.min(z3, z4)));
        maxX = Math.max(maxX, Math.max(x2 + 1, Math.max(x3 + 1, x4 + 1)));
        maxZ = Math.max(maxZ, Math.max(z2 + 1, Math.max(z3 + 1, z4 + 1)));

        return new AxisAlignedBB(minX, minY, minZ, maxX, maxY, maxZ);
    }

    private void drawLine(BufferBuilder buf, double x1, double y1, double z1, double x2, double y2, double z2, float r, float g, float b, float a) {
        buf.pos(x1, y1, z1).color(r, g, b, a).endVertex();
        buf.pos(x2, y2, z2).color(r, g, b, a).endVertex();
    }

    private void drawQuad(BufferBuilder buf, double x1, double y1, double z1, double x2, double y2, double z2, float r, float g, float b, float a, int face) {
        switch(face) {
            case 0:
                buf.pos(x1, y1, z1).color(r, g, b, a).endVertex();
                buf.pos(x2, y1, z1).color(r, g, b, a).endVertex();
                buf.pos(x2, y2, z1).color(r, g, b, a).endVertex();
                buf.pos(x1, y2, z1).color(r, g, b, a).endVertex();
                break;
            case 1:
                buf.pos(x1, y1, z2).color(r, g, b, a).endVertex();
                buf.pos(x1, y2, z2).color(r, g, b, a).endVertex();
                buf.pos(x2, y2, z2).color(r, g, b, a).endVertex();
                buf.pos(x2, y1, z2).color(r, g, b, a).endVertex();
                break;
            case 2:
                buf.pos(x1, y1, z1).color(r, g, b, a).endVertex();
                buf.pos(x1, y2, z1).color(r, g, b, a).endVertex();
                buf.pos(x1, y2, z2).color(r, g, b, a).endVertex();
                buf.pos(x1, y1, z2).color(r, g, b, a).endVertex();
                break;
            case 3:
                buf.pos(x2, y1, z1).color(r, g, b, a).endVertex();
                buf.pos(x2, y1, z2).color(r, g, b, a).endVertex();
                buf.pos(x2, y2, z2).color(r, g, b, a).endVertex();
                buf.pos(x2, y2, z1).color(r, g, b, a).endVertex();
                break;
            case 4:
                buf.pos(x1, y1, z1).color(r, g, b, a).endVertex();
                buf.pos(x1, y1, z2).color(r, g, b, a).endVertex();
                buf.pos(x2, y1, z2).color(r, g, b, a).endVertex();
                buf.pos(x2, y1, z1).color(r, g, b, a).endVertex();
                break;
            case 5:
                buf.pos(x1, y2, z1).color(r, g, b, a).endVertex();
                buf.pos(x2, y2, z1).color(r, g, b, a).endVertex();
                buf.pos(x2, y2, z2).color(r, g, b, a).endVertex();
                buf.pos(x1, y2, z2).color(r, g, b, a).endVertex();
                break;
        }
    }
}