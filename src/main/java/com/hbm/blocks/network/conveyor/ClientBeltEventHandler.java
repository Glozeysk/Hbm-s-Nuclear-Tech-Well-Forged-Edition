package com.hbm.blocks.network.conveyor;

import com.hbm.lib.RefStrings;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.network.FMLNetworkEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@Mod.EventBusSubscriber(modid = RefStrings.MODID, value = Side.CLIENT)
@SideOnly(Side.CLIENT)
public class ClientBeltEventHandler {

    @SubscribeEvent
    public static void onDisconnect(FMLNetworkEvent.ClientDisconnectionFromServerEvent event) {
        ClientBeltManager.get().clear();
    }
}