package com.hbm.entity.item;

import api.hbm.block.IConveyorItem;
import api.hbm.block.IEnterableBlock;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.world.World;

public class EntityMovingItem extends EntityMovingConveyorObject implements IConveyorItem {

	public static final DataParameter<ItemStack> STACK = EntityDataManager.createKey(EntityMovingItem.class, DataSerializers.ITEM_STACK);
	private static final DataParameter<Float> SYNC_YAW = EntityDataManager.createKey(EntityMovingItem.class, DataSerializers.FLOAT);
	private static final int ARC_RESOLUTION = 16;
	private static final double EPS = 1.0E-5D;

	private float clientYaw = 0.0F;
	private float clientYawTarget = 0.0F;
	private boolean clientYawInitialized = false;

	public EntityMovingItem(World world) {
		super(world);
		this.setSize(0.25F, 0.25F);
	}

	@Override
	protected void entityInit() {
		this.getDataManager().register(STACK, ItemStack.EMPTY);
		this.getDataManager().register(SYNC_YAW, 0.0F);
	}

	public void setItemStack(ItemStack stack) {
		this.getDataManager().set(STACK, stack);
	}

	public ItemStack getItemStack() {
		ItemStack stack = this.getDataManager().get(STACK);
		return stack == null ? new ItemStack(Blocks.STONE) : stack;
	}

	public void updateYawFromFacing(EnumFacing facing) {
		float yaw;
		switch (facing) {
			case SOUTH: yaw = 0.0F; break;
			case WEST: yaw = 90.0F; break;
			case NORTH: yaw = 180.0F; break;
			case EAST: yaw = -90.0F; break;
			default: return;
		}
		setSyncedYaw(yaw);
	}

	public void updateYawSmooth(float yaw) {
		setSyncedYaw(yaw);
	}

	private void setSyncedYaw(float yaw) {
		yaw = wrapDegrees(yaw);
		this.rotationYaw = yaw;
		this.getDataManager().set(SYNC_YAW, yaw);
	}

	private static float wrapDegrees(float value) {
		while (value <= -180.0F) value += 360.0F;
		while (value > 180.0F) value -= 360.0F;
		return value;
	}

	@Override
	public void notifyDataManagerChange(DataParameter<?> key) {
		super.notifyDataManagerChange(key);
		if (world.isRemote && SYNC_YAW.equals(key)) {
			float yaw = wrapDegrees(this.getDataManager().get(SYNC_YAW));
			if (!clientYawInitialized) {
				clientYaw = yaw;
				clientYawTarget = yaw;
				rotationYaw = yaw;
				prevRotationYaw = yaw;
				clientYawInitialized = true;
			} else {
				clientYawTarget = yaw;
			}
		}
	}

	@Override
	public void onUpdate() {
		super.onUpdate();

		if (world.isRemote) {
			if (!clientYawInitialized) {
				float yaw = wrapDegrees(this.getDataManager().get(SYNC_YAW));
				clientYaw = yaw;
				clientYawTarget = yaw;
				rotationYaw = yaw;
				prevRotationYaw = yaw;
				clientYawInitialized = true;
			}

			float diff = wrapDegrees(clientYawTarget - clientYaw);

			if (Math.abs(diff) < 1.0F) {
				clientYaw = clientYawTarget;
			} else {
				clientYaw = wrapDegrees(clientYaw + diff * 0.4F);
			}

			this.rotationYaw = clientYaw;
		}
	}

	@Override
	public void setDead() {
		super.setDead();
	}

	@Override
	public boolean canBeCollidedWith() {
		return true;
	}

	@Override
	public boolean attackEntityFrom(DamageSource source, float amount) {
		if (source.isProjectile()) return false;
		return super.attackEntityFrom(source, amount);
	}

	private int schedule = 0;

	@Override
	protected void readEntityFromNBT(NBTTagCompound nbt) {
		this.setItemStack(new ItemStack(nbt.getCompoundTag("Item")));
		this.schedule = nbt.getInteger("schedule");
		ItemStack stack = this.getDataManager().get(STACK);
		if (stack == null || stack.isEmpty()) this.setDead();
	}

	@Override
	protected void writeEntityToNBT(NBTTagCompound nbt) {
		if (this.getItemStack() != null)
			nbt.setTag("Item", this.getItemStack().writeToNBT(new NBTTagCompound()));
		nbt.setInteger("schedule", schedule);
	}

	@Override
	public void enterBlock(IEnterableBlock enterable, BlockPos pos, EnumFacing dir) {
		if (enterable.canItemEnter(world, pos.getX(), pos.getY(), pos.getZ(), dir, this)) {
			enterable.onItemEnter(world, pos.getX(), pos.getY(), pos.getZ(), dir, this);
			this.setDead();
		}
	}

	@Override
	public boolean onLeaveConveyor() {
		this.setDead();
		EntityItem item = new EntityItem(world, posX + motionX * 2, posY + motionY * 2, posZ + motionZ * 2, this.getItemStack());
		item.motionX = this.motionX * 2;
		item.motionY = 0.1D;
		item.motionZ = this.motionZ * 2;
		item.velocityChanged = true;
		world.spawnEntity(item);
		return true;
	}

	@Override
	public ItemStack getPickedResult(RayTraceResult target) {
		if (target.entityHit instanceof EntityMovingItem)
			return ((EntityMovingItem) target.entityHit).getItemStack();
		return null;
	}
}