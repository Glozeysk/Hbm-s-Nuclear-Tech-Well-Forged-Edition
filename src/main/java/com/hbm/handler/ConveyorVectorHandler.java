package com.hbm.handler;

import com.hbm.blocks.network.BlockConveyor;
import com.hbm.blocks.network.ConveyorArc;
import com.hbm.blocks.network.ConveyorQueue;
import com.hbm.entity.item.EntityMovingItem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.opengl.GL11;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Mod.EventBusSubscriber(Side.CLIENT)
public class ConveyorVectorHandler {

    private static final double EPS = 1.0E-5D;
    private static final int ARC_SEGMENTS = 16;
    private static final int ALPHA = 200;

    @SubscribeEvent
    @SideOnly(Side.CLIENT)
    public static void onRenderWorldLast(RenderWorldLastEvent event) {
        Minecraft mc = Minecraft.getMinecraft();
        EntityPlayer player = mc.player;
        World world = mc.world;

        if (world == null || player == null) return;

        float pt = event.getPartialTicks();
        double ix = player.lastTickPosX + (player.posX - player.lastTickPosX) * pt;
        double iy = player.lastTickPosY + (player.posY - player.lastTickPosY) * pt;
        double iz = player.lastTickPosZ + (player.posZ - player.lastTickPosZ) * pt;

        int rd = 16;
        BlockPos pp = new BlockPos(player.posX, player.posY, player.posZ);

        GlStateManager.pushMatrix();
        GlStateManager.translate(-ix, -iy, -iz);
        GlStateManager.disableTexture2D();
        GlStateManager.disableLighting();
        GlStateManager.disableDepth();
        GlStateManager.enableBlend();
        GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
        GlStateManager.glLineWidth(3.0F);

        Tessellator t = Tessellator.getInstance();
        BufferBuilder b = t.getBuffer();

        for (int x = -rd; x <= rd; x++)
            for (int y = -rd; y <= rd; y++)
                for (int z = -rd; z <= rd; z++) {
                    BlockPos pos = pp.add(x, y, z);
                    if (world.getBlockState(pos).getBlock() instanceof BlockConveyor)
                        renderConveyorDebug(world, pos, b, t);
                }

        GlStateManager.disableBlend();
        GlStateManager.enableDepth();
        GlStateManager.enableLighting();
        GlStateManager.enableTexture2D();
        GlStateManager.popMatrix();
    }

    @SideOnly(Side.CLIENT)
    private static void renderConveyorDebug(World world, BlockPos pos, BufferBuilder b, Tessellator t) {
        BlockConveyor conveyor = (BlockConveyor) world.getBlockState(pos).getBlock();
        EnumFacing facing = conveyor.getLaneFacing(world, pos);
        double yLine = pos.getY() + 0.27D;

        for (int lane = 0; lane < conveyor.getLaneCount(); lane++) {
            List<EntityMovingItem> items = getClientLaneItems(world, pos, conveyor, lane);

            boolean blockedEntry = isClientLaneBlockedAtEntry(pos, conveyor, facing, items);
            boolean blockedExit = isClientLaneBlockedAtExit(pos, conveyor, facing, items);

            int r, g, bl;
            if (blockedEntry && blockedExit) {
                r = 255; g = 0; bl = 0;
            } else if (blockedExit) {
                r = 255; g = 165; bl = 0;
            } else if (blockedEntry) {
                r = 255; g = 255; bl = 0;
            } else {
                r = 0; g = 255; bl = 0;
            }

            Vec3d start = conveyor.getLanePoint(pos, facing, lane, 0.0D);
            Vec3d end = conveyor.getLanePoint(pos, facing, lane, 1.0D);
            drawLine(b, t, start.x, yLine, start.z, end.x, yLine, end.z, r, g, bl, ALPHA);

            Vec3d arrowBase = conveyor.getLanePoint(pos, facing, lane, 0.80D);
            EnumFacing right = facing.rotateY();
            double as = 0.06D;
            drawLine(b, t, end.x, yLine, end.z,
                    arrowBase.x + right.getXOffset() * as, yLine, arrowBase.z + right.getZOffset() * as,
                    r, g, bl, ALPHA);
            drawLine(b, t, end.x, yLine, end.z,
                    arrowBase.x - right.getXOffset() * as, yLine, arrowBase.z - right.getZOffset() * as,
                    r, g, bl, ALPHA);

            for (int slot = 0; slot < ConveyorQueue.MAX_ITEMS_PER_LANE; slot++) {
                double progress = ConveyorQueue.ENTRY_PROGRESS + slot * ConveyorQueue.ITEM_LENGTH;
                Vec3d sp = conveyor.getLanePoint(pos, facing, lane, progress);
                drawLine(b, t, sp.x, yLine, sp.z, sp.x, yLine + 0.08D, sp.z, 0, 180, 255, ALPHA - 50);
            }

            for (EntityMovingItem item : items) {
                double ip = MathHelper.clamp(
                        conveyor.getLaneProgress(pos, facing, new Vec3d(item.posX, item.posY, item.posZ)),
                        0.0D, 1.0D);
                Vec3d idp = conveyor.getLanePoint(pos, facing, lane, ip);
                drawLine(b, t, idp.x, yLine, idp.z, idp.x, yLine + 0.15D, idp.z, 255, 0, 255, ALPHA);
            }

            renderSideEntryArcs(world, pos, conveyor, facing, lane, yLine, b, t);
        }
    }

    @SideOnly(Side.CLIENT)
    private static void renderSideEntryArcs(World world, BlockPos pos, BlockConveyor conveyor, EnumFacing facing,
                                            int lane, double yLine, BufferBuilder b, Tessellator t) {
        EnumFacing[] sides = {facing.rotateY(), facing.rotateYCCW(), facing.getOpposite()};

        for (EnumFacing checkDir : sides) {
            BlockPos sidePos = pos.offset(checkDir);
            if (!(world.getBlockState(sidePos).getBlock() instanceof BlockConveyor)) continue;

            BlockConveyor sideConveyor = (BlockConveyor) world.getBlockState(sidePos).getBlock();
            EnumFacing sideFacing = sideConveyor.getLaneFacing(world, sidePos);
            if (sideFacing != checkDir.getOpposite()) continue;

            for (int sideLane = 0; sideLane < sideConveyor.getLaneCount(); sideLane++) {
                BlockConveyor.IncomingRoute route = conveyor.resolveIncomingRoute(
                        world, sidePos, sideConveyor, sideLane, pos);
                if (route.lane != lane || route.lane < 0) continue;

                Vec3d entryPoint = sideConveyor.getLanePoint(sidePos, sideFacing, sideLane, ConveyorQueue.EXIT_PROGRESS);
                Vec3d targetPoint = conveyor.getLanePoint(pos, facing, lane, route.progress);
                ConveyorArc arc = ConveyorArc.createSideEntry(entryPoint, targetPoint, sideFacing, facing);
                renderBezierArc(arc, yLine, b, t, 0, 200, 200, ALPHA - 50);
            }
        }
    }

    @SideOnly(Side.CLIENT)
    private static void renderBezierArc(ConveyorArc arc, double y, BufferBuilder b, Tessellator t,
                                        int r, int g, int bl, int a) {
        Vec3d prev = arc.p0;
        for (int i = 1; i <= ARC_SEGMENTS; i++) {
            Vec3d cur = arc.evaluate((double) i / ARC_SEGMENTS);
            drawLine(b, t, prev.x, y, prev.z, cur.x, y, cur.z, r, g, bl, a);
            prev = cur;
        }
    }

    @SideOnly(Side.CLIENT)
    private static List<EntityMovingItem> getClientLaneItems(World world, BlockPos pos, BlockConveyor conveyor, int lane) {
        AxisAlignedBB box = new AxisAlignedBB(
                pos.getX(), pos.getY(), pos.getZ(),
                pos.getX() + 1.0D, pos.getY() + 1.0D, pos.getZ() + 1.0D);
        List<EntityMovingItem> found = world.getEntitiesWithinAABB(EntityMovingItem.class, box);
        List<EntityMovingItem> result = new ArrayList<>();
        EnumFacing facing = conveyor.getLaneFacing(world, pos);

        for (EntityMovingItem item : found) {
            if (item == null || item.isDead) continue;
            BlockPos ibp = new BlockPos(Math.floor(item.posX), Math.floor(item.posY), Math.floor(item.posZ));
            if (!pos.equals(ibp)) continue;
            int il = conveyor.getClosestLaneIndex(world, pos, new Vec3d(item.posX, item.posY, item.posZ));
            if (il != lane) continue;
            result.add(item);
        }

        result.sort(Comparator.comparingDouble(
                item -> -conveyor.getLaneProgress(pos, facing, new Vec3d(item.posX, item.posY, item.posZ))));
        return result;
    }

    @SideOnly(Side.CLIENT)
    private static boolean isClientLaneBlockedAtEntry(BlockPos pos, BlockConveyor conveyor, EnumFacing facing,
                                                      List<EntityMovingItem> items) {
        if (items.isEmpty()) return false;
        if (items.size() >= ConveyorQueue.MAX_ITEMS_PER_LANE) return true;
        EntityMovingItem tail = items.get(items.size() - 1);
        double tp = conveyor.getLaneProgress(pos, facing, new Vec3d(tail.posX, tail.posY, tail.posZ));
        return tp < ConveyorQueue.ENTRY_PROGRESS + ConveyorQueue.ITEM_LENGTH - EPS;
    }

    @SideOnly(Side.CLIENT)
    private static boolean isClientLaneBlockedAtExit(BlockPos pos, BlockConveyor conveyor, EnumFacing facing,
                                                     List<EntityMovingItem> items) {
        if (items.isEmpty()) return false;
        EntityMovingItem front = items.get(0);
        double fp = conveyor.getLaneProgress(pos, facing, new Vec3d(front.posX, front.posY, front.posZ));
        return fp >= ConveyorQueue.EXIT_PROGRESS - EPS;
    }

    @SideOnly(Side.CLIENT)
    private static void drawLine(BufferBuilder b, Tessellator t,
                                 double x1, double y1, double z1,
                                 double x2, double y2, double z2,
                                 int r, int g, int bl, int a) {
        b.begin(GL11.GL_LINES, DefaultVertexFormats.POSITION_COLOR);
        b.pos(x1, y1, z1).color(r, g, bl, a).endVertex();
        b.pos(x2, y2, z2).color(r, g, bl, a).endVertex();
        t.draw();
    }
}