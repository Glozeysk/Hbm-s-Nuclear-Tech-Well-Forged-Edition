package com.hbm.tileentity;

import api.hbm.energy.ILoadedTile;
import com.hbm.packet.PacketDispatcher;
import io.netty.buffer.ByteBuf;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.nbt.NBTTagCompound;
import org.jetbrains.annotations.NotNull;
import net.minecraftforge.fml.common.network.NetworkRegistry.TargetPoint;

public class TileEntityLoadedBase extends TileEntity implements ILoadedTile {
	
	public boolean isLoaded = true;
	public boolean muffled = false;
	
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
}
