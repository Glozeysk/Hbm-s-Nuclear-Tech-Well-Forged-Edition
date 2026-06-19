package com.hbm.handler;

import com.hbm.blocks.network.BlockConveyor;
import com.hbm.blocks.network.ConveyorArc;
import com.hbm.blocks.network.ConveyorItemData;
import com.hbm.tileentity.network.TileEntityConveyor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
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

        TileEntity te = world.getTileEntity(pos);
        List<ConveyorItemData> allItems;
        if (te instanceof TileEntityConveyor) {
            allItems = ((TileEntityConveyor) te).getItems();
        } else {
            allItems = new ArrayList<>();
        }

        for (int lane = 0; lane < conveyor.getLaneCount(); lane++) {
            List<ConveyorItemData> laneItems = getLaneItems(allItems, lane);

            boolean blockedEntry = isLaneBlockedAtEntry(laneItems);
            boolean blockedExit = isLaneBlockedAtExit(laneItems);

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

            for (int slot = 0; slot < ConveyorItemData.MAX_ITEMS_PER_LANE; slot++) {
                double progress = ConveyorItemData.ENTRY_PROGRESS + slot * ConveyorItemData.ITEM_LENGTH;
                Vec3d sp = conveyor.getLanePoint(pos, facing, lane, progress);
                drawLine(b, t, sp.x, yLine, sp.z, sp.x, yLine + 0.08D, sp.z, 0, 180, 255, ALPHA - 50);
            }

            for (ConveyorItemData item : laneItems) {
                if (item.isOnArc()) continue;
                double ip = MathHelper.clamp(item.getProgress(), 0.0D, 1.0D);
                Vec3d idp = conveyor.getLanePoint(pos, facing, lane, ip);
                drawLine(b, t, idp.x, yLine, idp.z, idp.x, yLine + 0.15D, idp.z, 255, 0, 255, ALPHA);
            }

            renderSideEntryArcs(world, pos, conveyor, facing, lane, yLine, b, t);
        }
    }

    @SideOnly(Side.CLIENT)
    private static List<ConveyorItemData> getLaneItems(List<ConveyorItemData> allItems, int lane) {
        List<ConveyorItemData> result = new ArrayList<>();
        for (ConveyorItemData item : allItems) {
            if (item.getLane() == lane) {
                result.add(item);
            }
        }
        return result;
    }

    @SideOnly(Side.CLIENT)
    private static boolean isLaneBlockedAtEntry(List<ConveyorItemData> laneItems) {
        if (laneItems.isEmpty()) return false;
        if (laneItems.size() >= ConveyorItemData.MAX_ITEMS_PER_LANE) return true;

        for (ConveyorItemData item : laneItems) {
            if (item.isOnArc()) continue;
            if (item.getProgress() < ConveyorItemData.ENTRY_PROGRESS + ConveyorItemData.ITEM_LENGTH - EPS) {
                return true;
            }
        }
        return false;
    }

    @SideOnly(Side.CLIENT)
    private static boolean isLaneBlockedAtExit(List<ConveyorItemData> laneItems) {
        if (laneItems.isEmpty()) return false;

        for (ConveyorItemData item : laneItems) {
            if (item.isOnArc()) continue;
            if (item.getProgress() >= ConveyorItemData.EXIT_PROGRESS - EPS) {
                return true;
            }
        }
        return false;
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

                Vec3d entryPoint = sideConveyor.getLanePoint(sidePos, sideFacing, sideLane, ConveyorItemData.EXIT_PROGRESS);
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