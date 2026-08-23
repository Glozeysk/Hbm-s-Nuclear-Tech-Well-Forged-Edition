package com.hbm.tileentity.machine.dummy;

import com.hbm.handler.DummyBlockRegistry;
import com.hbm.tileentity.TileEntityMachineBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.play.server.SPacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ITickable;
import net.minecraft.util.math.BlockPos;

public class TileEntityDummy extends TileEntity implements ITickable {

	private BlockPos target;
	private boolean isRegistered = false;

	@Override
	public void onLoad() {
		super.onLoad();
	}

	@Override
	public void update() {
		if (isRegistered || world.isRemote || target == null) {
			return;
		}
		tryRegisterDummy();
	}

	@Override
	public void invalidate() {
		super.invalidate();
		unregisterDummy();
	}

	@Override
	public void onChunkUnload() {
		super.onChunkUnload();
		unregisterDummy();
	}
	private void tryRegisterDummy() {
		if (this.world == null || this.world.isRemote || this.target == null || this.isRegistered) {
			return;
		}

		if (!this.world.isBlockLoaded(this.target)) {
			return;
		}

		if (this.world.isAirBlock(this.target)) {
			this.world.setBlockState(this.pos, net.minecraft.init.Blocks.AIR.getDefaultState(), 2);
			return;
		}

		TileEntity coreTE = this.world.getTileEntity(this.target);
		if (coreTE instanceof TileEntityMachineBase) {
			DummyBlockRegistry.register(this.world, this.target, this.pos);
			((TileEntityMachineBase) coreTE).dummyBlocks.add(this.pos);
			this.isRegistered = true;
		}
	}

	private void unregisterDummy() {
		if (this.world != null && !this.world.isRemote && this.target != null && this.isRegistered) {
			DummyBlockRegistry.unregister(this.world, this.target, this.pos);

			if (this.world.isBlockLoaded(this.target)) {
				TileEntity coreTE = this.world.getTileEntity(this.target);
				if (coreTE instanceof TileEntityMachineBase) {
					((TileEntityMachineBase) coreTE).dummyBlocks.remove(this.pos);
				}
			}
			this.isRegistered = false;
		}
	}

	@Override
	public NBTTagCompound writeToNBT(NBTTagCompound compound) {
		super.writeToNBT(compound);
		if (this.target != null) {
			compound.setInteger("tx", this.target.getX());
			compound.setInteger("ty", this.target.getY());
			compound.setInteger("tz", this.target.getZ());
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
		return new SPacketUpdateTileEntity(this.pos, 0, getUpdateTag());
	}

	@Override
	public NBTTagCompound getUpdateTag() {
		return this.writeToNBT(new NBTTagCompound());
	}

	public final void setTarget(BlockPos corePos) {
		this.target = corePos;
		this.isRegistered = false;
		tryRegisterDummy();
	}

	public final BlockPos getTarget() {
		return this.target;
	}
}