package com.hbm.handler;

import com.hbm.blocks.network.BlockConveyor;
import com.hbm.blocks.network.conveyor.BeltItemData;
import com.hbm.blocks.network.conveyor.BeltLane;
import com.hbm.blocks.network.conveyor.ClientBeltItem;
import com.hbm.blocks.network.conveyor.ClientBeltManager;
import com.hbm.blocks.network.conveyor.ClientBeltSegment;
import com.hbm.blocks.network.conveyor.ConveyorRoute;
import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
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
                        renderConveyorDebug(world, pos, b, t, pt);
                }

        GlStateManager.disableBlend();
        GlStateManager.enableDepth();
        GlStateManager.enableLighting();
        GlStateManager.enableTexture2D();
        GlStateManager.popMatrix();
    }

    @SideOnly(Side.CLIENT)
    private static void renderConveyorDebug(World world, BlockPos pos, BufferBuilder b, Tessellator t, float pt) {
        BlockConveyor conveyor = (BlockConveyor) world.getBlockState(pos).getBlock();
        EnumFacing facing = conveyor.getLaneFacing(world, pos);
        double yLine = pos.getY() + 0.27D;

        ClientBeltSegment segment = ClientBeltManager.get().getSegmentAt(pos);

        for (int lane = 0; lane < conveyor.getLaneCount(); lane++) {
            List<ClientBeltItem> laneItems = getLaneItemsAtPos(segment, pos, lane);

            boolean blockedEntry = isLaneBlockedAtEntry(laneItems, segment);
            boolean blockedExit = isLaneBlockedAtExit(laneItems, segment);

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

            int slotsPerBlock = 4;
            for (int slot = 0; slot < slotsPerBlock; slot++) {
                double progress = BeltLane.ITEM_LENGTH * 0.5 + slot * BeltLane.ITEM_LENGTH;
                if (progress >= 1.0D) break;
                Vec3d sp = conveyor.getLanePoint(pos, facing, lane, progress);
                drawLine(b, t, sp.x, yLine, sp.z, sp.x, yLine + 0.08D, sp.z, 0, 180, 255, ALPHA - 50);
            }

            for (ClientBeltItem item : laneItems) {
                double ip = item.getInterpolatedProgress(pt);
                if (segment != null) {
                    int blockIndex = segment.blocks.indexOf(pos);
                    if (blockIndex >= 0) {
                        double localProgress = ip - blockIndex;
                        if (localProgress >= 0.0D && localProgress <= 1.0D) {
                            Vec3d idp = conveyor.getLanePoint(pos, facing, lane, localProgress);
                            drawLine(b, t, idp.x, yLine, idp.z, idp.x, yLine + 0.15D, idp.z, 255, 0, 255, ALPHA);
                        }
                    }
                }
            }
        }

        if (conveyor.getLaneCount() == 1) {
            renderRoutePoints(world, pos, conveyor, facing, yLine, b, t);
        }
    }

    @SideOnly(Side.CLIENT)
    private static void renderRoutePoints(World world, BlockPos pos, BlockConveyor conveyor, EnumFacing facing,
                                          double yLine, BufferBuilder b, Tessellator t) {
        if (!isTurningConveyorClient(world, pos, facing)) return;

        EnumFacing left = facing.rotateYCCW();
        EnumFacing right = facing.rotateY();

        if (hasFeedingConveyor(world, pos, left, facing)) {
            renderRoutePointChain(ConveyorRoute.LEFT_ENTRY, pos, facing, yLine, b, t, 255, 200, 0);
        }

        if (hasFeedingConveyor(world, pos, right, facing)) {
            renderRoutePointChain(ConveyorRoute.RIGHT_ENTRY, pos, facing, yLine, b, t, 0, 200, 255);
        }
    }

    private static boolean isTurningConveyorClient(World world, BlockPos pos, EnumFacing facing) {
        ClientBeltSegment segment = ClientBeltManager.get().getSegmentAt(pos);
        if (segment != null) {
            int blockIndex = segment.blocks.indexOf(pos);
            if (blockIndex != 0) return false;
        }

        BlockPos behindPos = pos.offset(facing.getOpposite());
        Block behindBlock = world.getBlockState(behindPos).getBlock();
        if (behindBlock instanceof BlockConveyor) {
            BlockConveyor behindConveyor = (BlockConveyor) behindBlock;
            EnumFacing behindFacing = behindConveyor.getLaneFacing(world, behindPos);
            if (behindFacing == facing) return false;
        }

        EnumFacing left = facing.rotateYCCW();
        EnumFacing right = facing.rotateY();

        return hasFeedingConveyor(world, pos, left, facing) || hasFeedingConveyor(world, pos, right, facing);
    }

    @SideOnly(Side.CLIENT)
    private static void renderRoutePointChain(ConveyorRoute route, BlockPos pos, EnumFacing facing,
                                              double yLine, BufferBuilder b, Tessellator t,
                                              int r, int g, int bl) {
        int count = route.getPointCount();
        Vec3d prev = null;
        for (int i = 0; i < count; i++) {
            double progress = (double) i / (count - 1);
            Vec3d point = route.samplePosition(pos, facing, progress);

            drawLine(b, t, point.x, yLine + 0.02D, point.z,
                    point.x, yLine + 0.12D, point.z, r, g, bl, 200);

            if (prev != null) {
                drawLine(b, t, prev.x, yLine + 0.02D, prev.z,
                        point.x, yLine + 0.02D, point.z, r, g, bl, 150);
            }
            prev = point;
        }

        int mergeIdx = route.getMergeIndex();
        double mergeProgress = (double) mergeIdx / (count - 1);
        Vec3d mergePoint = route.samplePosition(pos, facing, mergeProgress);
        drawLine(b, t, mergePoint.x, yLine, mergePoint.z,
                mergePoint.x, yLine + 0.2D, mergePoint.z, 255, 0, 0, 220);
    }

    private static boolean hasFeedingConveyor(World world, BlockPos pos, EnumFacing checkDir, EnumFacing myFacing) {
        BlockPos sidePos = pos.offset(checkDir);
        Block sideBlock = world.getBlockState(sidePos).getBlock();
        if (!(sideBlock instanceof BlockConveyor)) return false;
        BlockConveyor sideConveyor = (BlockConveyor) sideBlock;
        EnumFacing sideFacing = sideConveyor.getLaneFacing(world, sidePos);
        return sideFacing == checkDir.getOpposite();
    }

    @SideOnly(Side.CLIENT)
    private static List<ClientBeltItem> getLaneItemsAtPos(ClientBeltSegment segment, BlockPos pos, int lane) {
        List<ClientBeltItem> result = new ArrayList<>();
        if (segment == null) return result;

        int blockIndex = segment.blocks.indexOf(pos);
        if (blockIndex < 0) return result;

        double minProgress = blockIndex;
        double maxProgress = blockIndex + 1.0D;

        for (ClientBeltItem item : segment.getAllItems()) {
            if (item.lane == lane && item.renderProgress >= minProgress && item.renderProgress < maxProgress) {
                result.add(item);
            }
        }
        return result;
    }

    @SideOnly(Side.CLIENT)
    private static boolean isLaneBlockedAtEntry(List<ClientBeltItem> laneItems, ClientBeltSegment segment) {
        if (laneItems.isEmpty()) return false;
        int slotsPerBlock = 4;
        if (laneItems.size() >= slotsPerBlock) return true;

        for (ClientBeltItem item : laneItems) {
            double localProgress = getLocalProgress(item, segment);
            if (localProgress < BeltLane.ITEM_LENGTH * 2.0D - EPS) {
                return true;
            }
        }
        return false;
    }

    @SideOnly(Side.CLIENT)
    private static boolean isLaneBlockedAtExit(List<ClientBeltItem> laneItems, ClientBeltSegment segment) {
        if (laneItems.isEmpty()) return false;

        for (ClientBeltItem item : laneItems) {
            double localProgress = getLocalProgress(item, segment);
            if (localProgress >= 1.0D - BeltLane.ITEM_LENGTH - EPS) {
                return true;
            }
        }
        return false;
    }

    private static double getLocalProgress(ClientBeltItem item, ClientBeltSegment segment) {
        if (segment == null) return item.renderProgress;
        return item.renderProgress - Math.floor(item.renderProgress);
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