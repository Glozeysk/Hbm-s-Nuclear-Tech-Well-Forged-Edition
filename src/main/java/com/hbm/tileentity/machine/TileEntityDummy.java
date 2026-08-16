package com.hbm.tileentity.machine;

import com.hbm.handler.DummyBlockRegistry;
import com.hbm.interfaces.IMultiBlock;
import com.hbm.tileentity.TileEntityMachineBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.play.server.SPacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ITickable;
import net.minecraft.util.math.BlockPos;

public class TileEntityDummy extends TileEntity implements ITickable {

	public BlockPos target;
	private boolean isRegistered = false;

	@Override
	public void onLoad() {
		super.onLoad();
		if (!world.isRemote && this.target != null) {
			DummyBlockRegistry.register(world, this.target, this.pos);
		}
	}

	@Override
	public void invalidate() {
		super.invalidate();
		if (!world.isRemote && target != null) {
			DummyBlockRegistry.unregister(world, target, pos);
		}
	}

	@Override
	public void onChunkUnload() {
		super.onChunkUnload();
		if (!world.isRemote && target != null) {
			DummyBlockRegistry.unregister(world, target, pos);
		}
	}

	@Override
	public NBTTagCompound writeToNBT(NBTTagCompound compound) {
		super.writeToNBT(compound);
		if (target != null) {
			compound.setInteger("tx", target.getX());
			compound.setInteger("ty", target.getY());
			compound.setInteger("tz", target.getZ());
		}
		return compound;
	}

	@Override
	public void readFromNBT(NBTTagCompound compound) {
		super.readFromNBT(compound);
		if (compound.hasKey("tx")) {
			int x = compound.getInteger("tx");
			int y = compound.getInteger("ty");
			int z = compound.getInteger("tz");
			this.target = new BlockPos(x, y, z);
		} else {
			this.target = null;
		}
	}

	@Override
	public SPacketUpdateTileEntity getUpdatePacket() {
		return new SPacketUpdateTileEntity(pos, 0, getUpdateTag());
	}

	@Override
	public NBTTagCompound getUpdateTag() {
		return this.writeToNBT(new NBTTagCompound());
	}

	@Override
	public void update() {
		if (!world.isRemote && !isRegistered && this.target != null) {
			DummyBlockRegistry.register(world, this.target, this.pos);
			isRegistered = true;
		}
	}
}