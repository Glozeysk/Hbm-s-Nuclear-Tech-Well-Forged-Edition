package com.hbm.packet;

import com.hbm.capability.HbmCapability;
import com.hbm.capability.HbmCapability.IHBMData;
import com.hbm.handler.HbmKeybinds.EnumKeybind;
import com.hbm.items.gear.ArmorFSB;
import com.hbm.packet.threading.PrecompiledPacket;

import io.netty.buffer.ByteBuf;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.SoundCategory;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public class KeybindPacket extends PrecompiledPacket {

	private int id;
	private int key;
	private boolean pressed;

	public KeybindPacket() { }

	public KeybindPacket(EnumKeybind key, boolean pressed) {
		this.key = key.ordinal();
		this.pressed = pressed;
		this.id = 0;
	}

	public KeybindPacket(int id) {
		this.id = id;
	}

	@Override
	public void fromBytes(ByteBuf buf) {
		key = buf.readInt();
		pressed = buf.readBoolean();
		id = buf.readInt();
	}

	@Override
	public void toBytes(ByteBuf buf) {
		buf.writeInt(key);
		buf.writeBoolean(pressed);
		buf.writeInt(id);
	}

	public static class Handler implements IMessageHandler<KeybindPacket, IMessage> {

		@Override
		public IMessage onMessage(KeybindPacket m, MessageContext ctx) {
			if (ctx.side == Side.SERVER) {
				ctx.getServerHandler().player.server.addScheduledTask(() -> {
					EntityPlayerMP p = ctx.getServerHandler().player;
					if (p == null || p.world == null) return;

					switch (m.id) {
						case 0:
							IHBMData props = HbmCapability.getData(p);
							props.setKeyPressed(EnumKeybind.values()[m.key], m.pressed);
							break;
						case 1:
							if (ArmorFSB.hasFSBArmor(p)) {
								ItemStack stack = p.inventory.armorInventory.get(2);
								ArmorFSB fsbarmor = (ArmorFSB) stack.getItem();
								if (fsbarmor.flashlightPosition != null) {
									if (!stack.hasTagCompound()) {
										stack.setTagCompound(new NBTTagCompound());
									}
									p.world.playSound(null, p.posX, p.posY, p.posZ, SoundEvents.BLOCK_LEVER_CLICK, SoundCategory.PLAYERS, 0.5F, 1);
									stack.getTagCompound().setBoolean("flActive", !stack.getTagCompound().getBoolean("flActive"));
								}
							}
							break;
					}
				});
			} else {
				handleClient(m);
			}
			return null;
		}

		@SideOnly(Side.CLIENT)
		private void handleClient(KeybindPacket m) {
			Minecraft.getMinecraft().addScheduledTask(() -> {
				if (Minecraft.getMinecraft().player == null) return;
				IHBMData props = HbmCapability.getData(Minecraft.getMinecraft().player);
				if (EnumKeybind.values()[m.key] == EnumKeybind.TOGGLE_JETPACK) {
					props.setEnableBackpack(m.pressed);
				}
				if (EnumKeybind.values()[m.key] == EnumKeybind.TOGGLE_HEAD) {
					props.setEnableHUD(m.pressed);
				}
			});
		}
	}
}