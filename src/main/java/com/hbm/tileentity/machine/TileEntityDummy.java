package com.hbm.tileentity.machine;

import com.hbm.interfaces.IMultiBlock;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.play.server.SPacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ITickable;
import net.minecraft.util.math.BlockPos;

public class TileEntityDummy extends TileEntity implements ITickable {

	public BlockPos target;

	// === ОПТИМИЗАЦИЯ: Счётчик тиков для редких проверок ===
	private int tickCounter = 0;
	private static final int CHECK_INTERVAL = 20; // Проверяем раз в секунду вместо каждого тика

	@Override
	public void update() {
		if (this.world.isRemote) return;

		// === ОПТИМИЗАЦИЯ: Проверяем раз в 20 тиков вместо каждого ===
		tickCounter++;
		if (tickCounter < CHECK_INTERVAL) return;
		tickCounter = 0;

		// Проверяем, существует ли ещё главная машина
		if (target != null && !(this.world.getBlockState(target).getBlock() instanceof IMultiBlock)) {
			// Используем тихое удаление, чтобы не спавнить партиклы и обновления
			if (this.world.getBlockState(pos).getBlock() instanceof com.hbm.blocks.machine.dummy.DummyBlockBase) {
				com.hbm.blocks.machine.dummy.DummyBlockBase.destroyQuietly(world, pos, false);
			} else {
				world.setBlockToAir(pos);
			}
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
}