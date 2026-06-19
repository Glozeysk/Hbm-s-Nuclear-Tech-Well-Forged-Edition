package com.hbm.render.tileentity;

import com.hbm.blocks.network.BlockConveyor;
import com.hbm.blocks.network.ConveyorItemData;
import com.hbm.tileentity.network.TileEntityConveyor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms.TransformType;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.client.ForgeHooksClient;
import org.lwjgl.opengl.GL11;

import java.util.List;

public class TileEntityConveyorRenderer extends TileEntitySpecialRenderer<TileEntityConveyor> {

    @Override
    public void render(TileEntityConveyor te, double x, double y, double z, float partialTicks, int destroyStage, float alpha) {
        BlockConveyor conveyor = te.getConveyor();
        if (conveyor == null) return;

        List<ConveyorItemData> items = te.getItems();
        if (items.isEmpty()) return;

        BlockPos pos = te.getPos();
        EnumFacing facing = conveyor.getLaneFacing(te.getWorld(), pos);

        Minecraft.getMinecraft().getTextureManager().bindTexture(TextureMap.LOCATION_BLOCKS_TEXTURE);

        for (ConveyorItemData item : items) {
            renderConveyorItem(te, conveyor, pos, facing, item, x, y, z, partialTicks);
        }
    }

    private void renderConveyorItem(TileEntityConveyor te, BlockConveyor conveyor, BlockPos pos,
                                    EnumFacing facing, ConveyorItemData item,
                                    double baseX, double baseY, double baseZ,
                                    float partialTicks) {
        ItemStack stack = item.getStack();
        if (stack.isEmpty()) return;

        Vec3d worldPos;

        if (item.isOnArc() && item.getArc() != null) {
            worldPos = item.getArc().evaluate(item.getArcParam());
        } else {
            double[] offsets = conveyor.getLaneOffsets();
            int lane = item.getLane();
            if (lane < 0 || lane >= offsets.length) lane = 0;

            double renderProgress = item.getProgress();
            worldPos = conveyor.getWorldPosition(pos, facing, offsets[lane], renderProgress);
        }

        double renderX = baseX + (worldPos.x - pos.getX());
        double renderY = baseY + (worldPos.y - pos.getY());
        double renderZ = baseZ + (worldPos.z - pos.getZ());

        float yaw = -item.getInterpolatedYaw(partialTicks);
        boolean isBlock = stack.getItem() instanceof ItemBlock;

        GL11.glPushMatrix();
        GL11.glTranslated(renderX, renderY, renderZ);
        GL11.glRotatef(yaw, 0.0F, 1.0F, 0.0F);
        GL11.glScaled(0.5D, 0.5D, 0.5D);

        if (isBlock) {
            GL11.glTranslated(0.0D, 0.25D, 0.0D);
        } else {
            GL11.glRotatef(90.0F, 1.0F, 0.0F, 0.0F);
            GL11.glTranslated(0.0D, 0.0D, -0.03D);
        }

        IBakedModel model = Minecraft.getMinecraft().getRenderItem().getItemModelWithOverrides(stack, te.getWorld(), null);
        model = ForgeHooksClient.handleCameraTransforms(model, TransformType.FIXED, false);
        Minecraft.getMinecraft().getRenderItem().renderItem(stack, model);

        GL11.glPopMatrix();
    }

    @Override
    public boolean isGlobalRenderer(TileEntityConveyor te) {
        return false;
    }
}