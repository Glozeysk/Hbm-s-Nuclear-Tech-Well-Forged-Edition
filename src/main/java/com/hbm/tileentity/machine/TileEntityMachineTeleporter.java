package com.hbm.tileentity.machine;

import java.util.List;

import com.hbm.lib.ModDamageSource;
import com.hbm.tileentity.IBufPacketReceiver;
import com.hbm.tileentity.TileEntityLoadedBase;

import api.hbm.energy.IEnergyUser;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.init.SoundEvents;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.ITickable;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;

public class TileEntityMachineTeleporter extends TileEntityLoadedBase implements ITickable, IEnergyUser, IBufPacketReceiver {

	public long power = 0;
	public BlockPos target = null;
	public boolean linked = false;
	public static final int consumption = 100000000;
	public static final int maxPower = 1000000000;

	@Override
	public void readFromNBT(NBTTagCompound compound) {
		power = compound.getLong("power");
		if(compound.getBoolean("hastarget")) {
			int x = compound.getInteger("x1");
			int y = compound.getInteger("y1");
			int z = compound.getInteger("z1");
			target = new BlockPos(x, y, z);
		}
		linked = compound.getBoolean("linked");
		super.readFromNBT(compound);
	}

	@Override
	public NBTTagCompound writeToNBT(NBTTagCompound compound) {
		compound.setLong("power", power);
		if(target != null) {
			compound.setBoolean("hastarget", true);
			compound.setInteger("x1", target.getX());
			compound.setInteger("y1", target.getY());
			compound.setInteger("z1", target.getZ());
		} else {
			compound.setBoolean("hastarget", false);
		}
		compound.setBoolean("linked", linked);
		return super.writeToNBT(compound);
	}

	@Override
	public void update() {
		boolean b0 = false;
		if(!this.world.isRemote) {
			this.updateStandardConnections(world, pos);
			List<Entity> entities = this.world.getEntitiesWithinAABB(Entity.class, new AxisAlignedBB(pos.getX() - 0.25, pos.getY(), pos.getZ() - 0.25, pos.getX() + 0.75, pos.getY() + 2, pos.getZ() + 0.75));
			if(!entities.isEmpty())
				for(Entity e : entities) {
					if(e.ticksExisted >= 10) {
						teleport(e);
						b0 = true;
					}
				}

			networkPackNT(150);
		}

		if(b0)
			world.spawnParticle(EnumParticleTypes.PORTAL, pos.getX() + 0.5, pos.getY() + 1, pos.getZ() + 0.5, 0.0D, 0.1D, 0.5D);
	}

	@Override
	public void serialize(ByteBuf buf) {
		buf.writeLong(this.power);
		buf.writeBoolean(this.linked);
		buf.writeBoolean(this.target != null);
		if(this.target != null) {
			buf.writeInt(this.target.getX());
			buf.writeInt(this.target.getY());
			buf.writeInt(this.target.getZ());
		}
	}

	@Override
	public void deserialize(ByteBuf buf) {
		this.power = buf.readLong();
		this.linked = buf.readBoolean();
		boolean hasTarget = buf.readBoolean();
		if(hasTarget) {
			this.target = new BlockPos(buf.readInt(), buf.readInt(), buf.readInt());
		} else {
			this.target = null;
		}
	}

	public void teleport(Entity entity) {

		if (this.power < consumption)
			return;

		world.playSound(null, entity.getPosition(), SoundEvents.ENTITY_ENDERMEN_TELEPORT, SoundCategory.BLOCKS,  1.0F, 1.0F);
		if (target == null && !this.linked) {
			entity.attackEntityFrom(ModDamageSource.teleporter, 10000);
		} else {
			if ((entity instanceof EntityPlayerMP)) {
				((EntityPlayerMP) entity).setPositionAndUpdate(target.getX() + 0.5D, target.getY() + 1.6D + entity.getYOffset(), target.getZ() + 0.5D);
			} else {
				entity.setPositionAndRotation(target.getX() + 0.5D, target.getY() + 1.6D + entity.getYOffset(), target.getZ() + 0.5D, entity.rotationYaw, entity.rotationPitch);
			}
			world.playSound(null, target, SoundEvents.ENTITY_ENDERMEN_TELEPORT, SoundCategory.BLOCKS,  1.0F, 1.0F);
		}
		
		this.power -= consumption;
	}

	@Override
	public void setPower(long i) {
		power = i;
	}

	@Override
	public long getPower() {
		return power;
	}

	@Override
	public long getMaxPower() {
		return maxPower;
	}
}