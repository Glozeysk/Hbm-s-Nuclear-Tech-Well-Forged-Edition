package com.hbm.handler;

import com.hbm.render.layer.LayerGasMaskMod;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.entity.RenderPlayer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.Map;

@SideOnly(Side.CLIENT)
public class GasMaskLayerRegistration {

	private static boolean registered = false;

	public static void init() {
		MinecraftForge.EVENT_BUS.register(new GasMaskLayerRegistration());
	}

	@SubscribeEvent
	public void onClientTick(TickEvent.ClientTickEvent event) {
		if(registered) return;
		if(event.phase != TickEvent.Phase.START) return;

		Minecraft mc = Minecraft.getMinecraft();
		if(mc.player == null) return;

		RenderManager renderManager = mc.getRenderManager();
		Map<String, RenderPlayer> skinMap = renderManager.getSkinMap();

		if(skinMap == null || skinMap.isEmpty()) return;

		for(RenderPlayer renderPlayer : skinMap.values()) {
			renderPlayer.addLayer(new LayerGasMaskMod(renderPlayer));
		}

		registered = true;
		MinecraftForge.EVENT_BUS.unregister(this);
	}
}
