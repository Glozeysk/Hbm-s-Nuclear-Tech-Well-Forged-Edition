package com.hbm.packet;

import com.hbm.items.tool.ItemToolAbility;
import com.hbm.packet.threading.ThreadedPacket;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public class NBTItemControlPacket extends ThreadedPacket {
    private NBTTagCompound tag;

    public NBTItemControlPacket() {}
    public NBTItemControlPacket(NBTTagCompound tag) { this.tag = tag; }

    @Override public void fromBytes(ByteBuf buf) { tag = ByteBufUtils.readTag(buf); }
    @Override public void toBytes(ByteBuf buf) { ByteBufUtils.writeTag(buf, tag); }

    public static class Handler implements IMessageHandler<NBTItemControlPacket, IMessage> {
        @Override
        public IMessage onMessage(NBTItemControlPacket message, MessageContext ctx) {
            ctx.getServerHandler().player.getServerWorld().addScheduledTask(() -> {
                EntityPlayerMP player = ctx.getServerHandler().player;
                ItemStack stack = player.getHeldItemMainhand();
                if (stack.getItem() instanceof ItemToolAbility && message.tag != null) {
                    stack.setTagCompound(message.tag);
                }
            });
            return null;
        }
    }
}