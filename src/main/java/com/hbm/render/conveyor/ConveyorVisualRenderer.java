package com.hbm.render.conveyor;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms.TransformType;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import net.minecraftforge.client.ForgeHooksClient;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.opengl.GL11;

import java.util.List;

@Mod.EventBusSubscriber(Side.CLIENT)
public class ConveyorVisualRenderer {

    @SubscribeEvent
    @SideOnly(Side.CLIENT)
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.world == null || mc.isGamePaused()) return;

        ConveyorVisualManager.get().tick();
    }

    @SubscribeEvent
    @SideOnly(Side.CLIENT)
    public static void onRenderWorld(RenderWorldLastEvent event) {
        Minecraft mc = Minecraft.getMinecraft();
        EntityPlayer player = mc.player;
        World world = mc.world;
        if (world == null || player == null) return;

        List<VisualItem> items = ConveyorVisualManager.get().getVisibleItems();
        if (items.isEmpty()) return;

        float pt = event.getPartialTicks();
        double camX = player.lastTickPosX + (player.posX - player.lastTickPosX) * pt;
        double camY = player.lastTickPosY + (player.posY - player.lastTickPosY) * pt;
        double camZ = player.lastTickPosZ + (player.posZ - player.lastTickPosZ) * pt;

        GlStateManager.pushMatrix();
        GlStateManager.translate(-camX, -camY, -camZ);

        GlStateManager.enableLighting();
        GlStateManager.enableLight(0);
        GlStateManager.enableLight(1);
        GlStateManager.enableColorMaterial();
        GlStateManager.colorMaterial(GL11.GL_FRONT_AND_BACK, GL11.GL_AMBIENT_AND_DIFFUSE);
        RenderHelper.enableStandardItemLighting();

        GlStateManager.enableRescaleNormal();
        GlStateManager.enableDepth();
        GlStateManager.depthMask(true);
        GlStateManager.disableBlend();
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        GlStateManager.enableAlpha();
        GlStateManager.alphaFunc(GL11.GL_GREATER, 0.1F);

        mc.getTextureManager().bindTexture(TextureMap.LOCATION_BLOCKS_TEXTURE);

        for (VisualItem item : items) {
            if (item.stack.isEmpty()) continue;

            double rx = item.getInterpolatedX(pt);
            double ry = item.getInterpolatedY(pt);
            double rz = item.getInterpolatedZ(pt);
            float yaw = -item.getInterpolatedYaw(pt);

            boolean isBlock = item.stack.getItem() instanceof ItemBlock;

            GlStateManager.pushMatrix();
            GlStateManager.translate(rx, ry, rz);
            GlStateManager.rotate(yaw, 0.0F, 1.0F, 0.0F);
            GlStateManager.scale(0.5F, 0.5F, 0.5F);

            if (isBlock) {
                GlStateManager.translate(0.0F, 0.25F, 0.0F);
            } else {
                GlStateManager.rotate(90.0F, 1.0F, 0.0F, 0.0F);
                GlStateManager.translate(0.0F, 0.0F, -0.03F);
            }

            IBakedModel model = mc.getRenderItem().getItemModelWithOverrides(item.stack, world, null);
            model = ForgeHooksClient.handleCameraTransforms(model, TransformType.FIXED, false);
            mc.getRenderItem().renderItem(item.stack, model);

            GlStateManager.popMatrix();
        }

        RenderHelper.disableStandardItemLighting();
        GlStateManager.disableRescaleNormal();
        GlStateManager.popMatrix();
    }
}