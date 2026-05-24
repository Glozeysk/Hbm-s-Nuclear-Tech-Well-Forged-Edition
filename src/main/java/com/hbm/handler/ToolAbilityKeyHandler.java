package com.hbm.handler;

import com.hbm.items.tool.IKeybindReceiver;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.InputEvent.KeyInputEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class ToolAbilityKeyHandler {

    @SubscribeEvent
    public void onKeyInput(KeyInputEvent event) {
        EntityPlayer player = net.minecraft.client.Minecraft.getMinecraft().player;
        if (player == null) return;

        ItemStack stack = player.getHeldItemMainhand();
        if (stack.isEmpty() || !(stack.getItem() instanceof IKeybindReceiver)) return;

        IKeybindReceiver receiver = (IKeybindReceiver) stack.getItem();

        if (HbmKeybinds.abilityAlt.isPressed()) {
            if (receiver.canHandleKeybind(player, stack, HbmKeybinds.EnumKeybind.ABILITY_ALT)) {
                System.out.println("[DEBUG] Вызываю handleKeybindClient для ABILITY_ALT");
                receiver.handleKeybindClient(player, stack, HbmKeybinds.EnumKeybind.ABILITY_ALT, true);
            }
        }
    }
}