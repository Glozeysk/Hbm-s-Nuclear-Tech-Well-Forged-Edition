package com.hbm.render.entity.item;

import com.hbm.entity.item.EntityMovingItem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms.TransformType;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.ForgeHooksClient;
import net.minecraftforge.fml.client.registry.IRenderFactory;
import org.lwjgl.opengl.GL11;

public class RenderMovingItem extends Render<EntityMovingItem> {

	public static final IRenderFactory<EntityMovingItem> FACTORY = man -> new RenderMovingItem(man);

	private static final float MODEL_YAW_OFFSET = 0.0F;
	private static final double ITEM_PIVOT_X = 0.0D;
	private static final double ITEM_PIVOT_Z = 0.0D;
	private static final double BLOCK_PIVOT_X = 0.0D;
	private static final double BLOCK_PIVOT_Z = 0.0D;

	protected RenderMovingItem(RenderManager renderManager) {
		super(renderManager);
	}

	@Override
	public void doRender(EntityMovingItem item, double x, double y, double z, float entityYaw, float partialTicks) {
		ItemStack stack = item.getItemStack();
		if (stack.isEmpty()) return;

		boolean isBlock = stack.getItem() instanceof ItemBlock;
		float yaw = -interpolateYaw(item, partialTicks) + MODEL_YAW_OFFSET;

		GL11.glPushMatrix();
		GL11.glTranslated(x, y, z);

		if (isBlock) {
			applyYawAroundPivot(yaw, BLOCK_PIVOT_X, BLOCK_PIVOT_Z);
		} else {
			applyYawAroundPivot(yaw, ITEM_PIVOT_X, ITEM_PIVOT_Z);
		}

		GL11.glScaled(0.5D, 0.5D, 0.5D);

		if (!isBlock) {
			GL11.glRotatef(90.0F, 1.0F, 0.0F, 0.0F);
			GL11.glTranslated(0.0D, 0.0D, -0.03D);
		} else {
			GL11.glTranslated(0.0D, 0.25D, 0.0D);
		}

		IBakedModel model = Minecraft.getMinecraft().getRenderItem().getItemModelWithOverrides(stack, item.world, null);
		model = ForgeHooksClient.handleCameraTransforms(model, TransformType.FIXED, false);
		Minecraft.getMinecraft().getTextureManager().bindTexture(TextureMap.LOCATION_BLOCKS_TEXTURE);
		Minecraft.getMinecraft().getRenderItem().renderItem(stack, model);

		GL11.glPopMatrix();
	}

	private void applyYawAroundPivot(float yaw, double pivotX, double pivotZ) {
		GL11.glTranslated(pivotX, 0.0D, pivotZ);
		GL11.glRotatef(yaw, 0.0F, 1.0F, 0.0F);
		GL11.glTranslated(-pivotX, 0.0D, -pivotZ);
	}

	private float interpolateYaw(EntityMovingItem item, float partialTicks) {
		float prev = item.prevRotationYaw;
		float cur = item.rotationYaw;
		float diff = cur - prev;

		while (diff <= -180.0F) diff += 360.0F;
		while (diff > 180.0F) diff -= 360.0F;

		return prev + diff * partialTicks;
	}

	@Override
	protected ResourceLocation getEntityTexture(EntityMovingItem entity) {
		return TextureMap.LOCATION_BLOCKS_TEXTURE;
	}
}