package com.hbm.packet;

import java.io.IOException;

import com.hbm.capability.HbmLivingCapability.IEntityHbmProps;
import com.hbm.capability.HbmLivingProps;
import com.hbm.packet.threading.PrecompiledPacket;

import io.netty.buffer.ByteBuf;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public class ExtPropPacket extends PrecompiledPacket {

    private NBTTagCompound nbt;

    public ExtPropPacket(){
    }

    public ExtPropPacket(NBTTagCompound nbt){
        this.nbt = nbt;
    }

    @Override
    public void fromBytes(ByteBuf buf){
        PacketBuffer pbuf = new PacketBuffer(buf);
        try {
            this.nbt = pbuf.readCompoundTag();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void toBytes(ByteBuf buf){
        if (this.nbt != null) {
            PacketBuffer pbuf = new PacketBuffer(buf);
            pbuf.writeCompoundTag(this.nbt);
        }
    }

    public static class Handler implements IMessageHandler<ExtPropPacket, IMessage> {

        @Override
        @SideOnly(Side.CLIENT)
        public IMessage onMessage(ExtPropPacket m, MessageContext ctx){
            Minecraft.getMinecraft().addScheduledTask(() -> {
                if(Minecraft.getMinecraft().world == null || m.nbt == null)
                    return;

                IEntityHbmProps props = HbmLivingProps.getData(Minecraft.getMinecraft().player);
                props.loadNBTData(m.nbt);
            });

            return null;
        }
    }
}