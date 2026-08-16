package com.hbm.tileentity.machine.dummy;

import com.hbm.handler.DummyBlockRegistry;
import com.hbm.tileentity.TileEntityMachineBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.play.server.SPacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;

public class TileEntityDummy extends TileEntity {

	private BlockPos target;

	@Override
	public void onLoad() {
		super.onLoad();
		registerDummy();
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

	public final void setTarget(BlockPos corePos) {
		this.target = corePos;
		registerDummy();
	}

	public final BlockPos getTarget() {
		return target;
	}

	private void registerDummy() {
		if (world == null || world.isRemote || target == null) {
			return;
		}

		DummyBlockRegistry.register(world, target, pos);

		TileEntity coreTE = world.getTileEntity(target);
		if (coreTE instanceof TileEntityMachineBase) {
			((TileEntityMachineBase) coreTE).dummyBlocks.add(pos);
		}
	}
}