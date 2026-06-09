package com.hbm.tileentity.turret;

import java.util.ArrayList;
import java.util.List;

import com.hbm.blocks.turret.TurretBase;
import com.hbm.entity.logic.EntityBomber;
import com.hbm.entity.missile.EntityMIRV;
import com.hbm.entity.missile.EntityMissileBaseAdvanced;
import com.hbm.entity.missile.EntityMissileCustom;
import com.hbm.lib.Library;
import com.hbm.packet.PacketDispatcher;
import com.hbm.packet.TETurretCIWSPacket;
import com.hbm.packet.TETurretPacket;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.play.server.SPacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ITickable;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.fml.common.network.NetworkRegistry.TargetPoint;

public class TileEntityTurretBase extends TileEntity implements ITickable {

	public double rotationYaw;
	public double rotationPitch;

	public double oldRotationYaw = 0.0D;
	public double oldRotationPitch = 0.0D;
	public boolean isAI = false;
	public boolean manualOverride = false;
	public boolean manualFocus = false;
	public String manualControlKey = "";
	public long manualControlLease = 0L;
	public List<String> players = new ArrayList<String>();
	public int use;
	public int ammo = 0;

	@Override
	public void update() {
		oldRotationPitch = rotationPitch;
		oldRotationYaw = rotationYaw;

		if(!world.isRemote && manualOverride && manualControlLease > 0 && manualControlLease <= world.getTotalWorldTime()) {
			manualOverride = false;
			manualFocus = false;
			manualControlKey = "";
			manualControlLease = 0L;
			use = 0;
		}

		if(isAI && !manualOverride) {
			Object[] iter = world.loadedEntityList.toArray();
			double radius = 1000;
			if(this instanceof TileEntityTurretFlamer)
				radius /= 3;
			if(this instanceof TileEntityTurretSpitfire)
				radius *= 3;
			if(this instanceof TileEntityTurretCIWS)
				radius *= 250;

			Entity target = null;
			for(int i = 0; i < iter.length; i++) {
				Entity e = (Entity) iter[i];
				if(e.isEntityAlive() && isInSight(e)) {
					double distance = e.getDistanceSq(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
					if(distance < radius) {
						radius = distance;
						target = e;
					}
				}
			}

			if(target != null) {
				Vec3d turret = new Vec3d(target.posX - (pos.getX() + 0.5), target.posY + target.getEyeHeight() - (pos.getY() + 1), target.posZ - (pos.getZ() + 0.5));
				if(this instanceof TileEntityTurretCIWS || this instanceof TileEntityTurretSpitfire || this instanceof TileEntityTurretCheapo && target.getEntityBoundingBox() != null) {
					try {
						Vec3d targetPos = target.getEntityBoundingBox().getCenter();
						turret = new Vec3d(targetPos.x - (pos.getX() + 0.5), targetPos.y - (pos.getY() + 1.5), targetPos.z - (pos.getZ() + 0.5));
					} catch(Throwable t) { }
				}

				double sqrt = MathHelper.sqrt(turret.x * turret.x + turret.z * turret.z);
				rotationPitch = -Math.atan2(turret.y, sqrt) * 180 / Math.PI;
				rotationYaw = -Math.atan2(turret.x, turret.z) * 180 / Math.PI;

				float maxAngle = -60;
				if(this instanceof TileEntityTurretCIWS)
					maxAngle = -80;
				if(rotationPitch < maxAngle)
					rotationPitch = maxAngle;
				if(rotationPitch > 30)
					rotationPitch = 30;

				if(this instanceof TileEntityTurretCheapo) {
					if(rotationPitch < -30)
						rotationPitch = -30;
					if(rotationPitch > 15)
						rotationPitch = 15;
				}

				if(world.getBlockState(pos).getBlock() instanceof TurretBase && ammo > 0) {
					if(((TurretBase) world.getBlockState(pos).getBlock()).executeHoldAction(world, use, rotationYaw, rotationPitch, pos))
						ammo--;
				}

				use++;
			} else {
				use = 0;
			}
		}

		if(!world.isRemote) {
			detectAndSendChanges();
		}
	}

	private boolean isInSight(Entity e) {
		if(!(e instanceof EntityLivingBase) && !(e instanceof EntityMissileBaseAdvanced) && !(e instanceof EntityBomber) && !(e instanceof EntityMIRV) && !(e instanceof EntityMissileCustom))
			return false;

		if(this instanceof TileEntityTurretCIWS && !(e instanceof EntityMissileBaseAdvanced || e instanceof EntityMissileCustom || e instanceof EntityBomber || e instanceof EntityMIRV))
			return false;
		if(e instanceof EntityPlayer && players.contains((((EntityPlayer) e).getDisplayName().getUnformattedText())))
			return false;

		if(this instanceof TileEntityTurretTau)
			return true;
		if(e instanceof EntityBomber && !((EntityBomber) e).isBomberAlive())
			return false;

		Vec3d turret;
		if(this instanceof TileEntityTurretSpitfire || this instanceof TileEntityTurretCIWS || this instanceof TileEntityTurretCheapo)
			turret = new Vec3d(pos.getX() + 0.5, pos.getY() + 1.5, pos.getZ() + 0.5);
		else
			turret = new Vec3d(pos.getX() + 0.5, pos.getY() + 1, pos.getZ() + 0.5);

		Vec3d entity = new Vec3d(e.posX, e.posY + e.getEyeHeight(), e.posZ);
		Vec3d side = new Vec3d(entity.x - turret.x, entity.y - turret.y, entity.z - turret.z);
		side = side.normalize();

		Vec3d check = new Vec3d(turret.x + side.x, turret.y + side.y, turret.z + side.z);
		return !Library.isObstructed(world, check.x, check.y, check.z, entity.x, entity.y, entity.z);
	}

	@Override
	public AxisAlignedBB getRenderBoundingBox() {
		return INFINITE_EXTENT_AABB;
	}

	@Override
	public double getMaxRenderDistanceSquared() {
		return 65536.0D;
	}

	@Override
	public void readFromNBT(NBTTagCompound nbt) {
		rotationYaw = nbt.getDouble("yaw");
		rotationPitch = nbt.getDouble("pitch");
		isAI = nbt.getBoolean("AI");
		ammo = nbt.getInteger("ammo");
		manualOverride = nbt.getBoolean("manualOverride");
		manualFocus = nbt.getBoolean("manualFocus");
		manualControlKey = nbt.getString("manualControlKey");
		manualControlLease = nbt.getLong("manualControlLease");

		playerListChanged = true;

		players.clear();
		int playercount = nbt.getInteger("playercount");

		for(int i = 0; i < playercount; i++) {
			if(nbt.hasKey("player_" + i))
				players.add(nbt.getString("player_" + i));
		}
		super.readFromNBT(nbt);
	}

	@Override
	public NBTTagCompound writeToNBT(NBTTagCompound nbt) {
		nbt.setDouble("yaw", rotationYaw);
		nbt.setDouble("pitch", rotationPitch);
		nbt.setBoolean("AI", isAI);
		nbt.setInteger("ammo", ammo);
		nbt.setBoolean("manualOverride", manualOverride);
		nbt.setBoolean("manualFocus", manualFocus);
		nbt.setString("manualControlKey", manualControlKey);
		nbt.setLong("manualControlLease", manualControlLease);

		nbt.setInteger("playercount", players.size());

		for(int i = 0; i < players.size(); i++) {
			nbt.setString("player_" + i, players.get(i));
		}
		return super.writeToNBT(nbt);
	}

	private int detectAmmo;
	private boolean detectIsAI;
	private double detectYaw;
	private double detectPitch;
	private boolean detectManualOverride;
	private boolean detectManualFocus;
	private String detectManualControlKey;
	private long detectManualControlLease;
	public boolean playerListChanged = true;

	private void detectAndSendChanges() {
		boolean mark = false;
		boolean syncNBT = false;

		if(isAI != detectIsAI) {
			detectIsAI = isAI;
			mark = true;
			syncNBT = true;
		}
		if(ammo != detectAmmo) {
			detectAmmo = ammo;
			mark = true;
			syncNBT = true;
		}
		if(manualOverride != detectManualOverride) {
			detectManualOverride = manualOverride;
			mark = true;
			syncNBT = true;
		}
		if(manualFocus != detectManualFocus) {
			detectManualFocus = manualFocus;
			mark = true;
			syncNBT = true;
		}
		if(manualControlKey != null ? !manualControlKey.equals(detectManualControlKey) : detectManualControlKey != null) {
			detectManualControlKey = manualControlKey;
			mark = true;
			syncNBT = true;
		}
		if(manualControlLease != detectManualControlLease) {
			detectManualControlLease = manualControlLease;
			mark = true;
			syncNBT = true;
		}
		if(Math.abs(rotationYaw - detectYaw) > 0.01D || Math.abs(rotationPitch - detectPitch) > 0.01D) {
			detectYaw = rotationYaw;
			detectPitch = rotationPitch;
			mark = true;
			syncNBT = true;
		}
		if(playerListChanged) {
			playerListChanged = false;
			mark = true;
			syncNBT = true;
		}

		if(syncNBT) {
			NBTTagCompound tag = writeToNBT(new NBTTagCompound());
			for(EntityPlayer player : this.world.playerEntities) {
				if(player instanceof EntityPlayerMP) {
					((EntityPlayerMP) player).connection.sendPacket(new SPacketUpdateTileEntity(pos, 0, tag));
				}
			}
		}

		if(isAI && this instanceof TileEntityTurretCIWS) {
			PacketDispatcher.wrapper.sendToAllTracking(new TETurretCIWSPacket(pos.getX(), pos.getY(), pos.getZ(), rotationYaw, rotationPitch), new TargetPoint(world.provider.getDimension(), pos.getX(), pos.getY(), pos.getZ(), 1000));
		}

		PacketDispatcher.wrapper.sendToAllTracking(new TETurretPacket(pos.getX(), pos.getY(), pos.getZ(), isAI), new TargetPoint(world.provider.getDimension(), pos.getX(), pos.getY(), pos.getZ(), 10));

		if(mark)
			this.markDirty();
	}

	@Override
	public void onDataPacket(net.minecraft.network.NetworkManager net, SPacketUpdateTileEntity pkt) {
		this.readFromNBT(pkt.getNbtCompound());
	}

	@Override
	public void handleUpdateTag(NBTTagCompound tag) {
		this.readFromNBT(tag);
	}

	@Override
	public SPacketUpdateTileEntity getUpdatePacket() {
		return new SPacketUpdateTileEntity(pos, 0, this.getUpdateTag());
	}

	@Override
	public NBTTagCompound getUpdateTag() {
		return this.writeToNBT(new NBTTagCompound());
	}
}