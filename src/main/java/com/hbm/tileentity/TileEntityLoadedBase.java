package com.hbm.tileentity;

import api.hbm.energy.ILoadedTile;
import com.hbm.handler.threading.PacketThreading;
import com.hbm.lib.Library;
import com.hbm.packet.toclient.BufPacket;
import io.netty.buffer.ByteBuf;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import org.jetbrains.annotations.NotNull;

public class TileEntityLoadedBase extends TileEntity implements ILoadedTile, IBufPacketReceiver{
	
	public boolean isLoaded = true;
	public boolean muffled = false;

    private long lastPackedBufHash = 0L;

	@Override
	public boolean isLoaded() {
		return isLoaded;
	}

	@Override
	public void onChunkUnload() {
		super.onChunkUnload();
		this.isLoaded = false;
	}

	@Override
	public void readFromNBT(NBTTagCompound nbt) {
		super.readFromNBT(nbt);
		this.muffled = nbt.getBoolean("muffled");
	}

	@Override
	public @NotNull NBTTagCompound writeToNBT(NBTTagCompound nbt) {
		nbt.setBoolean("muffled", muffled);
		return super.writeToNBT(nbt);
	}

    public void markChanged() {
        this.world.markChunkDirty(this.pos, this);
    }

    /**
     * {@inheritDoc}
     * only call super.serialize() on noisy machines. It has no effect on others.<br>
     * The final ByteBuf is compared with previous packets sent in order to avoid unnecessary traffic.<br>
     * A side effect of this is that compilation effectively runs on server thread, instead of PacketThreading IO thread;
     * Override {@link #networkPackNT(int)} if this behavior is undesirable.
     */
    @Override
    public void serialize(ByteBuf buf) {
        buf.writeBoolean(muffled);
    }

    /**
     * {@inheritDoc}
     * only call super.deserialize() on noisy machines. It has no effect on others.<br>
     * This happens on the <strong>Netty Client IO thread</strong>!
     * Direct List modification is guaranteed to produce a CME.<br>
     */
    @Override
    public void deserialize(ByteBuf buf) {
        this.muffled = buf.readBoolean();
    }

    /** Sends a sync packet that uses ByteBuf for efficient information-cramming */
    public void networkPackNT(int range) {
        if (world.isRemote) return;

        BufPacket packet = new BufPacket(pos.getX(), pos.getY(), pos.getZ(), this);
        ByteBuf preBuf = packet.getCompiledBuffer();

        // Don't send unnecessary packets, except for maybe one every second or so.
        // If we stop sending duplicate packets entirely, this causes issues when
        // a client unloads and then loads back a chunk with an unchanged tile entity.
        // For that client, the tile entity will appear default until anything changes about it.
        // In my testing, this can be reliably reproduced with a full fluid barrel, for instance.
        // I think it might be fixable by doing something with getDescriptionPacket() and onDataPacket(),
        // but this sidesteps the problem for the mean time.
        long preHash = Library.fnv1a64(preBuf);
        if (preHash == lastPackedBufHash) {
            if (this.world.getTotalWorldTime() % 20 != 0) {
                packet.releaseBuffer();
                return;
            }
        }
        lastPackedBufHash = preHash;
        PacketThreading.createAllAroundThreadedPacket(packet, new NetworkRegistry.TargetPoint(this.world.provider.getDimension(), pos.getX(), pos.getY(), pos.getZ(), range));
    }
}
