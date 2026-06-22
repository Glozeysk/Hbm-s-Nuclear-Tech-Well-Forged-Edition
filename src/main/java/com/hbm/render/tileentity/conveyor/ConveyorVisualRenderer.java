package com.hbm.render.tileentity.conveyor;

import com.hbm.blocks.network.BlockConveyor;
import com.hbm.blocks.network.conveyor.BeltLane;
import com.hbm.blocks.network.conveyor.ClientBeltItem;
import com.hbm.blocks.network.conveyor.ClientBeltManager;
import com.hbm.blocks.network.conveyor.ClientBeltSegment;
import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms.TransformType;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemBlock;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
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
        ClientBeltManager.get().tick();
    }

    @SubscribeEvent
    @SideOnly(Side.CLIENT)
    public static void onRenderWorld(RenderWorldLastEvent event) {
        Minecraft mc = Minecraft.getMinecraft();
        EntityPlayer player = mc.player;
        World world = mc.world;
        if (world == null || player == null) return;

        List<ClientBeltSegment> segments = ClientBeltManager.get().getAllSegments();
        if (segments.isEmpty()) return;

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

        for (ClientBeltSegment segment : segments) {
            List<ClientBeltItem> items = segment.getAllItems();
            if (items.isEmpty()) continue;

            List<BlockPos> blocks = segment.blocks;
            EnumFacing facing = segment.direction;

            for (ClientBeltItem item : items) {
                if (item.stack.isEmpty()) continue;

                double interpProgress = item.getInterpolatedProgress(pt);

                int blockIndex = (int) interpProgress;
                if (blockIndex < 0) blockIndex = 0;
                if (blockIndex >= blocks.size()) blockIndex = blocks.size() - 1;

                double localProgress = interpProgress - blockIndex;
                BlockPos blockPos = blocks.get(blockIndex);

                if (!world.isBlockLoaded(blockPos)) continue;
                Block block = world.getBlockState(blockPos).getBlock();
                if (!(block instanceof BlockConveyor)) continue;

                BlockConveyor conveyor = (BlockConveyor) block;
                double[] offsets = conveyor.getLaneOffsets();
                int lane = item.lane;
                if (lane < 0 || lane >= offsets.length) lane = 0;

                Vec3d wp = conveyor.getWorldPosition(blockPos, facing, offsets[lane], localProgress);

                float yaw;
                switch (facing) {
                    case SOUTH: yaw = 0.0F; break;
                    case WEST: yaw = -90.0F; break;
                    case NORTH: yaw = -180.0F; break;
                    case EAST: yaw = 90.0F; break;
                    default: yaw = 0.0F;
                }

                boolean isBlock = item.stack.getItem() instanceof ItemBlock;

                GlStateManager.pushMatrix();
                GlStateManager.translate(wp.x, wp.y, wp.z);
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
        }

        RenderHelper.disableStandardItemLighting();
        GlStateManager.disableRescaleNormal();
        GlStateManager.popMatrix();
    }
}