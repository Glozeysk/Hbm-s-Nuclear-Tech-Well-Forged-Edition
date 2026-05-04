package com.hbm.items.tool;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.hbm.blocks.turret.TurretBase;
import com.hbm.blocks.turret.TurretBaseNT;
import com.hbm.items.ModItems;
import com.hbm.lib.HBMSoundHandler;
import com.hbm.lib.Library;
import com.hbm.main.MainRegistry;
import com.hbm.tileentity.turret.TileEntityTurretBase;
import com.hbm.tileentity.turret.TileEntityTurretBaseNT;
import com.hbm.tileentity.turret.TileEntityTurretCIWS;
import com.hbm.tileentity.turret.TileEntityTurretCheapo;
import com.hbm.tileentity.turret.TileEntityTurretSpitfire;
import com.hbm.util.I18nUtil;

import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.EnumAction;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.World;

public class ItemTurretControl extends Item {

	public static class LaserData {
		public double startX, startY, startZ;
		public double endX, endY, endZ;
		public boolean active;
		public long timestamp;
		public boolean targetVisible;
		public boolean targetIsEntity;
		public int targetEntityId;
		public boolean focusMode;
	}

	public static Map<Integer, LaserData> activeLasers = new HashMap<>();
	private static int nextLaserId = 0;

	private static final double MAX_RANGE = 150.0D;
	private static final double MAX_CONTROL_RANGE = 150.0D;
	private static final double MAX_FOCUS_RANGE = 150.0D;

	public ItemTurretControl(String s) {
		this.setTranslationKey(s);
		this.setRegistryName(s);
		this.setCreativeTab(MainRegistry.weaponTab);
		ModItems.ALL_ITEMS.add(this);
	}

	private int getOrAssignLaserId(ItemStack stack) {
		if(!stack.hasTagCompound()) stack.setTagCompound(new NBTTagCompound());
		NBTTagCompound nbt = stack.getTagCompound();
		if(!nbt.hasKey("laserId")) {
			nbt.setInteger("laserId", nextLaserId++);
		}
		return nbt.getInteger("laserId");
	}

	private void reassignLaserId(ItemStack stack) {
		if(!stack.hasTagCompound()) stack.setTagCompound(new NBTTagCompound());
		stack.getTagCompound().setInteger("laserId", nextLaserId++);
	}

	private LaserData getLaserData(int id) {
		LaserData data = activeLasers.get(id);
		if(data == null) {
			data = new LaserData();
			activeLasers.put(id, data);
		}
		return data;
	}

	private void updateLaser(World world, Vec3d turretPos, Vec3d targetPos, boolean visible, boolean isEntity, int entityId, int id, boolean focusMode) {
		LaserData data = getLaserData(id);
		data.startX = turretPos.x;
		data.startY = turretPos.y;
		data.startZ = turretPos.z;
		data.endX = targetPos.x;
		data.endY = targetPos.y;
		data.endZ = targetPos.z;
		data.active = true;
		data.timestamp = world.getTotalWorldTime();
		data.targetVisible = visible;
		data.targetIsEntity = isEntity;
		data.targetEntityId = entityId;
		data.focusMode = focusMode;
	}

	private void clearLaser(int id) {
		LaserData data = activeLasers.get(id);
		if(data != null) {
			data.active = false;
			data.focusMode = false;
		}
	}

	private boolean isPlayerInRange(EntityPlayer player, BlockPos turretPos) {
		double dx = player.posX - (turretPos.getX() + 0.5D);
		double dy = player.posY - (turretPos.getY() + 0.5D);
		double dz = player.posZ - (turretPos.getZ() + 0.5D);
		return dx * dx + dy * dy + dz * dz <= MAX_CONTROL_RANGE * MAX_CONTROL_RANGE;
	}

	private boolean isFocusTargetInRange(BlockPos turretPos, Entity target) {
		double dx = target.posX - (turretPos.getX() + 0.5D);
		double dy = target.posY - (turretPos.getY() + 0.5D);
		double dz = target.posZ - (turretPos.getZ() + 0.5D);
		return dx * dx + dy * dy + dz * dz <= MAX_FOCUS_RANGE * MAX_FOCUS_RANGE;
	}

	private boolean isTargetInTurretRange(Vec3d turretPos, Vec3d targetPos) {
		double dx = targetPos.x - turretPos.x;
		double dy = targetPos.y - turretPos.y;
		double dz = targetPos.z - turretPos.z;
		return dx * dx + dy * dy + dz * dz <= MAX_FOCUS_RANGE * MAX_FOCUS_RANGE;
	}

	private RayTraceResult performWranglerRaycast(EntityPlayer player, World world) {
		Vec3d eyePos = player.getPositionEyes(1.0F);
		Vec3d lookVec = player.getLookVec();
		Vec3d endPos = eyePos.add(lookVec.x * MAX_RANGE, lookVec.y * MAX_RANGE, lookVec.z * MAX_RANGE);
		RayTraceResult blockHit = world.rayTraceBlocks(eyePos, endPos, false, true, false);
		Vec3d reachEnd = blockHit != null && blockHit.hitVec != null ? blockHit.hitVec : endPos;
		RayTraceResult bestHit = blockHit;
		double bestDist = blockHit != null && blockHit.hitVec != null ? eyePos.distanceTo(blockHit.hitVec) : MAX_RANGE;
		List<Entity> entities = world.getEntitiesWithinAABBExcludingEntity(player, player.getEntityBoundingBox().grow(MAX_RANGE, MAX_RANGE, MAX_RANGE));
		for(Entity e : entities) {
			if(e == player) continue;
			if(!e.canBeCollidedWith() && !(e instanceof EntityLivingBase)) continue;
			RayTraceResult entHit = e.getEntityBoundingBox().grow(0.3D, 0.3D, 0.3D).calculateIntercept(eyePos, reachEnd);
			if(entHit != null) {
				double dist = eyePos.distanceTo(entHit.hitVec);
				if(dist < bestDist) {
					bestDist = dist;
					bestHit = new RayTraceResult(e, entHit.hitVec);
				}
			}
		}
		return bestHit;
	}

	private Vec3d getTargetPoint(RayTraceResult hit) {
		if(hit == null) return null;
		if(hit.typeOfHit == RayTraceResult.Type.ENTITY && hit.entityHit != null) {
			Entity e = hit.entityHit;
			return new Vec3d(e.posX, e.posY + e.height * 0.5D, e.posZ);
		}
		if(hit.hitVec != null) return hit.hitVec;
		if(hit.getBlockPos() != null) {
			return new Vec3d(hit.getBlockPos().getX() + 0.5D, hit.getBlockPos().getY() + 0.5D, hit.getBlockPos().getZ() + 0.5D);
		}
		return null;
	}

	private Vec3d findBestEntityAimPoint(World world, Vec3d turretPos, Entity entity) {
		double ex = entity.posX;
		double ez = entity.posZ;
		double bottomY = entity.posY + 0.1D;
		double centerY = entity.posY + entity.height * 0.5D;
		double topY = entity.posY + entity.height - 0.1D;
		Vec3d center = new Vec3d(ex, centerY, ez);
		Vec3d top = new Vec3d(ex, topY, ez);
		Vec3d bottom = new Vec3d(ex, bottomY, ez);
		if(turretCanSee(world, turretPos, center)) return center;
		if(turretCanSee(world, turretPos, top)) return top;
		if(turretCanSee(world, turretPos, bottom)) return bottom;
		return null;
	}

	private boolean turretCanSee(World world, Vec3d from, Vec3d to) {
		Vec3d dir = to.subtract(from).normalize();
		Vec3d start = new Vec3d(from.x + dir.x, from.y + dir.y, from.z + dir.z);
		return !Library.isObstructed(world, start.x, start.y, start.z, to.x, to.y, to.z);
	}

	private BlockPos getLinkedPos(ItemStack stack) {
		NBTTagCompound nbt = stack.getTagCompound();
		return new BlockPos(nbt.getInteger("xCoord"), nbt.getInteger("yCoord"), nbt.getInteger("zCoord"));
	}

	private Vec3d getTurretPosForBase(TileEntityTurretBase turret, BlockPos pos) {
		if(turret instanceof TileEntityTurretCIWS || turret instanceof TileEntityTurretSpitfire || turret instanceof TileEntityTurretCheapo) {
			return new Vec3d(pos.getX() + 0.5D, pos.getY() + 1.5D, pos.getZ() + 0.5D);
		}
		return new Vec3d(pos.getX() + 0.5D, pos.getY() + 1.0D, pos.getZ() + 0.5D);
	}

	private void aimBaseTurret(TileEntityTurretBase turret, Vec3d turretPos, Vec3d aimPos) {
		Vec3d delta = new Vec3d(aimPos.x - turretPos.x, aimPos.y - turretPos.y, aimPos.z - turretPos.z);
		double sqrt = Math.sqrt(delta.x * delta.x + delta.z * delta.z);

		turret.oldRotationYaw = turret.rotationYaw;
		turret.oldRotationPitch = turret.rotationPitch;
		turret.rotationPitch = -Math.atan2(delta.y, sqrt) * 180.0D / Math.PI;
		turret.rotationYaw = -Math.atan2(delta.x, delta.z) * 180.0D / Math.PI;

		float maxAngle = -60.0F;
		if(turret instanceof TileEntityTurretCIWS) maxAngle = -80.0F;
		if(turret.rotationPitch < maxAngle) turret.rotationPitch = maxAngle;
		if(turret.rotationPitch > 30.0D) turret.rotationPitch = 30.0D;

		if(turret instanceof TileEntityTurretCheapo) {
			if(turret.rotationPitch < -30.0D) turret.rotationPitch = -30.0D;
			if(turret.rotationPitch > 15.0D) turret.rotationPitch = 15.0D;
		}
	}

	private Vec3d getTurretPosForTE(TileEntity te, BlockPos pos) {
		if(te instanceof TileEntityTurretBaseNT) {
			return ((TileEntityTurretBaseNT) te).getTurretPos();
		}
		if(te instanceof TileEntityTurretBase) {
			return getTurretPosForBase((TileEntityTurretBase) te, pos);
		}
		return new Vec3d(pos.getX() + 0.5D, pos.getY() + 1.0D, pos.getZ() + 0.5D);
	}

	private void setStackTarget(ItemStack stack, Vec3d pos, boolean visible) {
		NBTTagCompound nbt = stack.getTagCompound();
		if(nbt == null) return;
		if(pos != null) {
			nbt.setDouble("targetX", pos.x);
			nbt.setDouble("targetY", pos.y);
			nbt.setDouble("targetZ", pos.z);
			nbt.setBoolean("targetSet", true);
			nbt.setBoolean("targetVisible", visible);
		} else {
			nbt.setBoolean("targetSet", false);
			nbt.setBoolean("targetVisible", false);
		}
	}

	private void setFocusTarget(ItemStack stack, Entity entity, String ownerUUID) {
		NBTTagCompound nbt = stack.getTagCompound();
		if(nbt == null) return;
		nbt.setUniqueId("focusEntityUUID", entity.getUniqueID());
		nbt.setString("focusEntityName", entity.getName());
		nbt.setBoolean("focusActive", true);
		nbt.setString("focusOwner", ownerUUID);
	}

	private void clearFocusTarget(ItemStack stack) {
		NBTTagCompound nbt = stack.getTagCompound();
		if(nbt == null) return;
		nbt.setBoolean("focusActive", false);
		nbt.removeTag("focusEntityUUIDMost");
		nbt.removeTag("focusEntityUUIDLeast");
		nbt.removeTag("focusEntityName");
		nbt.removeTag("focusOwner");
	}

	private boolean hasFocusTarget(ItemStack stack) {
		NBTTagCompound nbt = stack.getTagCompound();
		if(nbt == null) return false;
		return nbt.getBoolean("focusActive");
	}

	private Entity getFocusEntity(World world, ItemStack stack) {
		NBTTagCompound nbt = stack.getTagCompound();
		if(nbt == null) return null;
		if(!nbt.hasUniqueId("focusEntityUUID")) return null;
		UUID uuid = nbt.getUniqueId("focusEntityUUID");
		for(Entity e : world.loadedEntityList) {
			if(e.getUniqueID().equals(uuid)) return e;
		}
		return null;
	}

	private String getFocusEntityName(ItemStack stack) {
		NBTTagCompound nbt = stack.getTagCompound();
		if(nbt == null) return "";
		return nbt.getString("focusEntityName");
	}

	private String getFocusOwner(ItemStack stack) {
		NBTTagCompound nbt = stack.getTagCompound();
		if(nbt == null) return "";
		return nbt.getString("focusOwner");
	}

	private EntityPlayer findOwner(World world, ItemStack stack) {
		String uuid = getFocusOwner(stack);
		if(uuid.isEmpty()) return null;
		for(EntityPlayer p : world.playerEntities) {
			if(p.getUniqueID().toString().equals(uuid)) return p;
		}
		return null;
	}

	private void sendActionBar(EntityPlayer player, String translationKey, Object... args) {
		if(player != null && !player.world.isRemote) {
			player.sendStatusMessage(new TextComponentTranslation(translationKey, args), true);
		}
	}

	private String getOrAssignControllerKey(ItemStack stack) {
		if(!stack.hasTagCompound()) stack.setTagCompound(new NBTTagCompound());
		NBTTagCompound nbt = stack.getTagCompound();
		if(!nbt.hasKey("controllerKey")) {
			nbt.setString("controllerKey", UUID.randomUUID().toString());
		}
		return nbt.getString("controllerKey");
	}

	private boolean claimTurretControl(World world, TileEntity te, ItemStack stack) {
		String key = getOrAssignControllerKey(stack);
		long now = world.getTotalWorldTime();
		long lease = now + 5L;

		if(te instanceof TileEntityTurretBaseNT) {
			TileEntityTurretBaseNT turret = (TileEntityTurretBaseNT) te;
			if(turret.manualOverride && turret.manualControlLease >= now && !key.equals(turret.manualControlKey)) {
				return false;
			}
			turret.manualControlKey = key;
			turret.manualControlLease = lease;
			return true;
		}

		if(te instanceof TileEntityTurretBase && !(te instanceof TileEntityTurretBaseNT)) {
			TileEntityTurretBase turret = (TileEntityTurretBase) te;
			if(turret.manualOverride && turret.manualControlLease >= now && !key.equals(turret.manualControlKey)) {
				return false;
			}
			turret.manualControlKey = key;
			turret.manualControlLease = lease;
			return true;
		}

		return false;
	}

	private boolean isTurretLockedByOther(TileEntity te, ItemStack stack) {
		String key = getOrAssignControllerKey(stack);

		if(te instanceof TileEntityTurretBaseNT) {
			TileEntityTurretBaseNT turret = (TileEntityTurretBaseNT) te;
			return turret.manualOverride && !key.equals(turret.manualControlKey) && !turret.manualControlKey.isEmpty();
		}

		if(te instanceof TileEntityTurretBase && !(te instanceof TileEntityTurretBaseNT)) {
			TileEntityTurretBase turret = (TileEntityTurretBase) te;
			return turret.manualOverride && !key.equals(turret.manualControlKey) && !turret.manualControlKey.isEmpty();
		}

		return false;
	}

	private boolean ownsTurretControl(TileEntity te, ItemStack stack) {
		String key = getOrAssignControllerKey(stack);

		if(te instanceof TileEntityTurretBaseNT) {
			return key.equals(((TileEntityTurretBaseNT) te).manualControlKey);
		}

		if(te instanceof TileEntityTurretBase && !(te instanceof TileEntityTurretBaseNT)) {
			return key.equals(((TileEntityTurretBase) te).manualControlKey);
		}

		return false;
	}

	private void setTurretFocus(TileEntity te, boolean focus) {
		if(te instanceof TileEntityTurretBaseNT) {
			((TileEntityTurretBaseNT) te).manualFocus = focus;
		}
		if(te instanceof TileEntityTurretBase && !(te instanceof TileEntityTurretBaseNT)) {
			((TileEntityTurretBase) te).manualFocus = focus;
		}
	}

	private void releaseTurret(World world, BlockPos pos, ItemStack stack) {
		TileEntity te = world.getTileEntity(pos);

		if(te instanceof TileEntityTurretBaseNT) {
			TileEntityTurretBaseNT turret = (TileEntityTurretBaseNT) te;
			if(stack == null || ownsTurretControl(te, stack) || turret.manualControlKey.isEmpty()) {
				turret.manualOverride = false;
				turret.manualFocus = false;
				turret.wranglerFiring = false;
				turret.target = null;
				turret.tPos = null;
				turret.manualControlKey = "";
				turret.manualControlLease = 0L;
			}
		}

		if(te instanceof TileEntityTurretBase && !(te instanceof TileEntityTurretBaseNT)) {
			TileEntityTurretBase turret = (TileEntityTurretBase) te;
			if(stack == null || ownsTurretControl(te, stack) || turret.manualControlKey.isEmpty()) {
				turret.manualOverride = false;
				turret.manualFocus = false;
				turret.manualControlKey = "";
				turret.manualControlLease = 0L;
				turret.use = 0;
			}
		}
	}

	private boolean isPlayerFiring(EntityPlayer player, ItemStack stack) {
		return player.isHandActive() && player.getActiveItemStack() == stack;
	}

	@Override
	public void onUpdate(ItemStack stack, World world, Entity entity, int itemSlot, boolean isSelected) {
		if(!(entity instanceof EntityPlayer)) return;
		EntityPlayer player = (EntityPlayer) entity;
		if(!stack.hasTagCompound()) return;
		if(!stack.getTagCompound().hasKey("xCoord")) return;

		int id = getOrAssignLaserId(stack);
		boolean held = player.getHeldItem(EnumHand.MAIN_HAND) == stack || player.getHeldItem(EnumHand.OFF_HAND) == stack;
		BlockPos pos = getLinkedPos(stack);

		if(hasFocusTarget(stack)) {
			handleFocusMode(stack, world, player, held, id);
			return;
		}

		if(!held) {
			clearLaser(id);
			setStackTarget(stack, null, false);
			if(!world.isRemote && stack.getTagCompound().getBoolean("wasActive")) {
				releaseTurret(world, pos, stack);
				stack.getTagCompound().setBoolean("wasActive", false);
			}
			return;
		}

		if(!isPlayerInRange(player, pos)) {
			clearLaser(id);
			setStackTarget(stack, null, false);
			if(!world.isRemote && stack.getTagCompound().getBoolean("wasActive")) {
				releaseTurret(world, pos, stack);
				stack.getTagCompound().setBoolean("wasActive", false);
			}
			return;
		}

		TileEntity te = world.getTileEntity(pos);

		if(te != null && isTurretLockedByOther(te, stack)) {
			clearLaser(id);
			setStackTarget(stack, null, false);
			if(!world.isRemote && stack.getTagCompound().getBoolean("wasActive")) {
				stack.getTagCompound().setBoolean("wasActive", false);
			}
			return;
		}

		handleNormalMode(stack, world, player, pos, te, id);
	}

	private void handleFocusMode(ItemStack stack, World world, EntityPlayer player, boolean held, int id) {
		BlockPos pos = getLinkedPos(stack);
		Entity focusEntity = getFocusEntity(world, stack);
		String focusName = getFocusEntityName(stack);

		if(focusEntity == null || !focusEntity.isEntityAlive() || focusEntity.isDead) {
			if(!world.isRemote) {
				releaseTurret(world, pos, stack);
				stack.getTagCompound().setBoolean("wasActive", false);
				EntityPlayer owner = findOwner(world, stack);
				sendActionBar(owner, "actionbar.turretcontrol.unfocus.killed", focusName);
			}
			clearFocusTarget(stack);
			clearLaser(id);
			return;
		}

		if(!isFocusTargetInRange(pos, focusEntity)) {
			if(!world.isRemote) {
				releaseTurret(world, pos, stack);
				stack.getTagCompound().setBoolean("wasActive", false);
				EntityPlayer owner = findOwner(world, stack);
				sendActionBar(owner, "actionbar.turretcontrol.unfocus.range", focusName);
			}
			clearFocusTarget(stack);
			clearLaser(id);
			return;
		}

		TileEntity te = world.getTileEntity(pos);

		if(te != null && isTurretLockedByOther(te, stack)) {
			clearFocusTarget(stack);
			clearLaser(id);
			return;
		}

		if(!world.isRemote) {
			if(!claimTurretControl(world, te, stack)) {
				releaseTurret(world, pos, stack);
				stack.getTagCompound().setBoolean("wasActive", false);
				clearFocusTarget(stack);
				return;
			}
			setTurretFocus(te, true);
		}

		Vec3d targetCenter = new Vec3d(focusEntity.posX, focusEntity.posY + focusEntity.height * 0.5D, focusEntity.posZ);
		Vec3d turretPos = getTurretPosForTE(te, pos);
		boolean inRange = isTargetInTurretRange(turretPos, targetCenter);

		if(te instanceof TileEntityTurretBase && !(te instanceof TileEntityTurretBaseNT)) {
			TileEntityTurretBase turret = (TileEntityTurretBase) te;

			Vec3d bestPoint = findBestEntityAimPoint(world, turretPos, focusEntity);
			boolean visible = bestPoint != null;
			Vec3d aimPos = visible ? bestPoint : targetCenter;

			boolean effectivelyVisible = visible && inRange;

			aimBaseTurret(turret, turretPos, aimPos);

			if(!world.isRemote) {
				if(!turret.manualOverride) turret.use = 0;
				turret.manualOverride = true;
				setStackTarget(stack, aimPos, effectivelyVisible);

				if(effectivelyVisible && turret.ammo > 0) {
					turret.use++;
					if(world.getBlockState(pos).getBlock() instanceof TurretBase) {
						if(((TurretBase) world.getBlockState(pos).getBlock()).executeHoldAction(world, turret.use, turret.rotationYaw, turret.rotationPitch, pos)) {
							turret.ammo--;
						}
					}
				}
			}

			if(world.isRemote) {
				updateLaser(world, turretPos, aimPos, effectivelyVisible, true, focusEntity.getEntityId(), id, true);
			}
		}

		if(te instanceof TileEntityTurretBaseNT) {
			TileEntityTurretBaseNT turret = (TileEntityTurretBaseNT) te;

			Vec3d bestPoint = findBestEntityAimPoint(world, turretPos, focusEntity);
			boolean visible = bestPoint != null;
			Vec3d aimPos = visible ? bestPoint : targetCenter;

			boolean effectivelyVisible = visible && inRange;

			if(world.isRemote) {
				updateLaser(world, turretPos, aimPos, effectivelyVisible, true, focusEntity.getEntityId(), id, true);
			} else {
				turret.manualOverride = true;
				turret.target = effectivelyVisible ? focusEntity : null;
				turret.tPos = visible ? bestPoint : targetCenter;
				turret.turnTowards(turret.tPos);
				turret.wranglerFiring = effectivelyVisible;
			}
		}
	}

	private void handleNormalMode(ItemStack stack, World world, EntityPlayer player, BlockPos pos, TileEntity te, int id) {
		RayTraceResult hit = performWranglerRaycast(player, world);
		Vec3d targetPos = getTargetPoint(hit);

		if(hit == null || targetPos == null) {
			clearLaser(id);
			setStackTarget(stack, null, false);
			return;
		}

		if(!world.isRemote) {
			if(!claimTurretControl(world, te, stack)) {
				setStackTarget(stack, null, false);
				stack.getTagCompound().setBoolean("wasActive", false);
				return;
			}
			setTurretFocus(te, false);
		}

		boolean firing = isPlayerFiring(player, stack);

		if(te instanceof TileEntityTurretBase && !(te instanceof TileEntityTurretBaseNT)) {
			TileEntityTurretBase turret = (TileEntityTurretBase) te;
			Vec3d turretPos = getTurretPosForBase(turret, pos);
			Vec3d aimPos = targetPos;
			boolean visible = true;
			boolean isEntity = false;
			int entityId = -1;

			if(hit.typeOfHit == RayTraceResult.Type.ENTITY && hit.entityHit != null) {
				isEntity = true;
				entityId = hit.entityHit.getEntityId();
				Vec3d bestPoint = findBestEntityAimPoint(world, turretPos, hit.entityHit);
				if(bestPoint != null) {
					aimPos = bestPoint;
				} else {
					aimPos = targetPos;
					visible = false;
				}
			} else {
				visible = turretCanSee(world, turretPos, targetPos);
			}

			boolean inRange = isTargetInTurretRange(turretPos, aimPos);
			boolean effectivelyVisible = visible && inRange;

			aimBaseTurret(turret, turretPos, aimPos);

			if(!world.isRemote) {
				if(!turret.manualOverride) turret.use = 0;
				turret.manualOverride = true;
				setStackTarget(stack, aimPos, effectivelyVisible);
				stack.getTagCompound().setBoolean("wasActive", true);

				if(firing && effectivelyVisible && turret.ammo > 0) {
					turret.use++;
					if(world.getBlockState(pos).getBlock() instanceof TurretBase) {
						if(((TurretBase) world.getBlockState(pos).getBlock()).executeHoldAction(world, turret.use, turret.rotationYaw, turret.rotationPitch, pos)) {
							turret.ammo--;
						}
					}
				} else if(!firing) {
					turret.use = 0;
				}
			}

			if(world.isRemote) {
				updateLaser(world, turretPos, aimPos, effectivelyVisible, isEntity, entityId, id, false);
			}
		}

		if(te instanceof TileEntityTurretBaseNT) {
			TileEntityTurretBaseNT turret = (TileEntityTurretBaseNT) te;
			Vec3d turretPos = turret.getTurretPos();
			Vec3d aimPos = targetPos;
			boolean visible = true;
			boolean isEntity = false;
			int entityId = -1;
			Entity trackedEntity = null;

			if(hit.typeOfHit == RayTraceResult.Type.ENTITY && hit.entityHit != null) {
				isEntity = true;
				entityId = hit.entityHit.getEntityId();
				Vec3d bestPoint = findBestEntityAimPoint(world, turretPos, hit.entityHit);
				if(bestPoint != null) {
					aimPos = bestPoint;
				} else {
					visible = false;
				}
				if(firing && visible) {
					trackedEntity = hit.entityHit;
				}
			} else {
				visible = turretCanSee(world, turretPos, targetPos);
			}

			boolean inRange = isTargetInTurretRange(turretPos, aimPos);
			boolean effectivelyVisible = visible && inRange;

			if(world.isRemote) {
				updateLaser(world, turretPos, aimPos, effectivelyVisible, isEntity, entityId, id, false);
			} else {
				turret.manualOverride = true;
				stack.getTagCompound().setBoolean("wasActive", true);
				turret.target = trackedEntity;
				turret.tPos = aimPos;
				turret.turnTowards(turret.tPos);
				turret.wranglerFiring = firing && effectivelyVisible;
			}
		}
	}

	@Override
	public void addInformation(ItemStack stack, World worldIn, List<String> list, ITooltipFlag flagIn) {
		if(stack.getTagCompound() != null && stack.getTagCompound().hasKey("xCoord")) {
			list.add(I18nUtil.resolveKey("desc.turrectcontrol"));
			list.add("X: " + stack.getTagCompound().getInteger("xCoord"));
			list.add("Y: " + stack.getTagCompound().getInteger("yCoord"));
			list.add("Z: " + stack.getTagCompound().getInteger("zCoord"));
			list.add(I18nUtil.resolveKey("desc.turrectcontrol.range", (int) MAX_CONTROL_RANGE));
			if(hasFocusTarget(stack)) {
				list.add("\u00a7c" + I18nUtil.resolveKey("desc.turrectcontrol.target", getFocusEntityName(stack)));
			}
		} else {
			list.add(I18nUtil.resolveKey("desc.turrectcontrol.noconnect"));
		}
	}

	@Override
	public EnumActionResult onItemUse(EntityPlayer player, World worldIn, BlockPos pos, EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ) {
		boolean valid = false;
		int x = 0, y = 0, z = 0;

		if(worldIn.getBlockState(pos).getBlock() instanceof TurretBase) {
			valid = true;
			x = pos.getX();
			y = pos.getY();
			z = pos.getZ();
		}

		if(worldIn.getBlockState(pos).getBlock() instanceof TurretBaseNT) {
			int[] cPos = ((TurretBaseNT) worldIn.getBlockState(pos).getBlock()).findCore(worldIn, pos.getX(), pos.getY(), pos.getZ());
			if(cPos != null) {
				x = cPos[0];
				y = cPos[1];
				z = cPos[2];
				valid = true;
			}
		}

		if(valid) {
			ItemStack stack = player.getHeldItem(hand);
			if(!stack.hasTagCompound()) {
				stack.setTagCompound(new NBTTagCompound());
			}

			if(!worldIn.isRemote && stack.getTagCompound().hasKey("xCoord")) {
				BlockPos oldPos = getLinkedPos(stack);
				releaseTurret(worldIn, oldPos, stack);
			}

			stack.getTagCompound().setInteger("xCoord", x);
			stack.getTagCompound().setInteger("yCoord", y);
			stack.getTagCompound().setInteger("zCoord", z);
			stack.getTagCompound().setBoolean("wasActive", false);
			stack.getTagCompound().setLong("linkTime", worldIn.getTotalWorldTime());
			clearFocusTarget(stack);
			reassignLaserId(stack);

			if(!worldIn.isRemote) {
				worldIn.playSound(null, player.posX, player.posY, player.posZ, HBMSoundHandler.techBleep, SoundCategory.PLAYERS, 1.0F, 1.0F);
				sendActionBar(player, "actionbar.turretcontrol.linked");
			}

			return EnumActionResult.SUCCESS;
		}

		return EnumActionResult.PASS;
	}

	@Override
	public EnumAction getItemUseAction(ItemStack stack) {
		return EnumAction.BOW;
	}

	@Override
	public int getMaxItemUseDuration(ItemStack stack) {
		return 72000;
	}

	@Override
	public ActionResult<ItemStack> onItemRightClick(World worldIn, EntityPlayer playerIn, EnumHand handIn) {
		ItemStack stack = playerIn.getHeldItem(handIn);

		if(!stack.hasTagCompound() || !stack.getTagCompound().hasKey("xCoord")) {
			return new ActionResult<>(EnumActionResult.PASS, stack);
		}

		long linkTime = stack.getTagCompound().getLong("linkTime");
		if(worldIn.getTotalWorldTime() - linkTime < 5L) {
			return new ActionResult<>(EnumActionResult.PASS, stack);
		}

		BlockPos pos = getLinkedPos(stack);
		TileEntity te = worldIn.getTileEntity(pos);

		if(te != null && isTurretLockedByOther(te, stack)) {
			return new ActionResult<>(EnumActionResult.FAIL, stack);
		}

		if(!isPlayerInRange(playerIn, pos)) {
			return new ActionResult<>(EnumActionResult.PASS, stack);
		}

		if(playerIn.isSneaking()) {
			RayTraceResult hit = performWranglerRaycast(playerIn, worldIn);

			if(hit != null && hit.typeOfHit == RayTraceResult.Type.ENTITY && hit.entityHit != null) {
				if(!worldIn.isRemote) {
					if(te != null && !claimTurretControl(worldIn, te, stack)) {
						return new ActionResult<>(EnumActionResult.FAIL, stack);
					}
					if(te != null) setTurretFocus(te, true);
				}

				setFocusTarget(stack, hit.entityHit, playerIn.getUniqueID().toString());

				if(!worldIn.isRemote) {
					worldIn.playSound(null, playerIn.posX, playerIn.posY, playerIn.posZ, HBMSoundHandler.turretFocus, SoundCategory.PLAYERS, 1.0F, 1.0F);
					sendActionBar(playerIn, "actionbar.turretcontrol.focused", hit.entityHit.getName());
				}

				return new ActionResult<>(EnumActionResult.SUCCESS, stack);
			} else {
				if(hasFocusTarget(stack)) {
					if(!worldIn.isRemote) {
						releaseTurret(worldIn, pos, stack);
						stack.getTagCompound().setBoolean("wasActive", false);
						sendActionBar(playerIn, "actionbar.turretcontrol.unfocus");
					}
					clearFocusTarget(stack);
					return new ActionResult<>(EnumActionResult.SUCCESS, stack);
				}
			}

			return new ActionResult<>(EnumActionResult.PASS, stack);
		}

		if(!hasFocusTarget(stack)) {
			RayTraceResult check = worldIn.rayTraceBlocks(
					playerIn.getPositionEyes(1.0F),
					playerIn.getPositionEyes(1.0F).add(playerIn.getLookVec().scale(5.0D)),
					false, false, false
			);

			Vec3d eyePos = playerIn.getPositionEyes(1.0F);
			Vec3d lookEnd = eyePos.add(playerIn.getLookVec().scale(6.0D));
			List<Entity> nearby = worldIn.getEntitiesWithinAABBExcludingEntity(playerIn, playerIn.getEntityBoundingBox().grow(6.0D));
			boolean hitNearbyEntity = false;
			double blockDist = check != null && check.hitVec != null ? eyePos.distanceTo(check.hitVec) : 6.0D;

			for(Entity e : nearby) {
				if(!e.canBeCollidedWith()) continue;
				RayTraceResult entHit = e.getEntityBoundingBox().grow(0.3D).calculateIntercept(eyePos, lookEnd);
				if(entHit != null) {
					double entDist = eyePos.distanceTo(entHit.hitVec);
					if(entDist < blockDist && entDist < 6.0D) {
						hitNearbyEntity = true;
						break;
					}
				}
			}

			if(hitNearbyEntity) {
				return new ActionResult<>(EnumActionResult.PASS, stack);
			}

			playerIn.setActiveHand(handIn);
			return new ActionResult<>(EnumActionResult.SUCCESS, stack);
		}

		return new ActionResult<>(EnumActionResult.PASS, stack);
	}

	@Override
	public void onPlayerStoppedUsing(ItemStack stack, World worldIn, EntityLivingBase entityLiving, int timeLeft) {
		if(!stack.hasTagCompound() || !stack.getTagCompound().hasKey("xCoord")) return;
		if(hasFocusTarget(stack)) return;

		BlockPos pos = getLinkedPos(stack);

		if(!worldIn.isRemote) {
			TileEntity te = worldIn.getTileEntity(pos);

			if(te instanceof TileEntityTurretBase && !(te instanceof TileEntityTurretBaseNT) && ownsTurretControl(te, stack)) {
				TileEntityTurretBase turret = (TileEntityTurretBase) te;
				if(turret.use > 0 && worldIn.getBlockState(pos).getBlock() instanceof TurretBase) {
					((TurretBase) worldIn.getBlockState(pos).getBlock()).executeReleaseAction(worldIn, turret.use, turret.rotationYaw, turret.rotationPitch, pos);
				}
			}

			releaseTurret(worldIn, pos, stack);
			stack.getTagCompound().setBoolean("wasActive", false);
		}
	}

	@Override
	public void onUsingTick(ItemStack stack, EntityLivingBase mob, int count) {
	}
}