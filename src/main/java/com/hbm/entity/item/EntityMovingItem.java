package com.hbm.entity.item;

import api.hbm.block.IConveyorItem;
import api.hbm.block.IEnterableBlock;
import com.hbm.blocks.network.BlockConveyor;
import com.hbm.blocks.network.ConveyorArc;
import com.hbm.blocks.network.ConveyorQueue;
import net.minecraft.block.Block;
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
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

public class EntityMovingItem extends EntityMovingConveyorObject implements IConveyorItem {

	public static final DataParameter<ItemStack> STACK = EntityDataManager.createKey(EntityMovingItem.class, DataSerializers.ITEM_STACK);
	private static final DataParameter<Float> SYNC_YAW = EntityDataManager.createKey(EntityMovingItem.class, DataSerializers.FLOAT);
	private static final int ARC_RESOLUTION = 16;
	private static final double EPS = 1.0E-5D;

	private int conveyorLane = -1;

	private boolean queueRegistered = false;
	private int queueDim = Integer.MIN_VALUE;
	private BlockPos queuePos = BlockPos.ORIGIN;
	private int queueLane = -1;

	private ConveyorArc activeArc = null;
	private double arcParam = 0.0D;
	private BlockPos arcDestinationPos = BlockPos.ORIGIN;
	private int arcDestinationLane = -1;

	private boolean pointReserved = false;
	private int reservationDim = Integer.MIN_VALUE;
	private BlockPos reservationPos = BlockPos.ORIGIN;
	private int reservationLane = -1;
	private double reservationProgress = 0.0D;

	private boolean holdReserved = false;
	private int holdReservationDim = Integer.MIN_VALUE;
	private BlockPos holdReservationPos = BlockPos.ORIGIN;
	private int holdReservationLane = -1;
	private double holdReservationProgress = 0.0D;

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

	public int getConveyorLane() {
		return conveyorLane;
	}

	public void setConveyorLane(int lane) {
		this.conveyorLane = lane;
	}

	public boolean hasQueueRegistration() {
		return queueRegistered;
	}

	public int getQueueDim() {
		return queueDim;
	}

	public BlockPos getQueuePos() {
		return queuePos;
	}

	public int getQueueLane() {
		return queueLane;
	}

	public void setQueueRegistration(int dim, BlockPos pos, int lane) {
		this.queueRegistered = true;
		this.queueDim = dim;
		this.queuePos = pos.toImmutable();
		this.queueLane = lane;
	}

	public void clearQueueRegistration() {
		this.queueRegistered = false;
		this.queueDim = Integer.MIN_VALUE;
		this.queuePos = BlockPos.ORIGIN;
		this.queueLane = -1;
	}

	public ConveyorArc getActiveArc() {
		return activeArc;
	}

	public void setActiveArc(ConveyorArc arc) {
		this.activeArc = arc;
		this.arcParam = 0.0D;
	}

	public void clearActiveArc() {
		this.activeArc = null;
		this.arcParam = 0.0D;
		this.arcDestinationPos = BlockPos.ORIGIN;
		this.arcDestinationLane = -1;
	}

	public double getArcParam() {
		return arcParam;
	}

	public void setArcParam(double param) {
		this.arcParam = param;
	}

	public void setArcDestination(BlockPos pos, int lane) {
		this.arcDestinationPos = pos.toImmutable();
		this.arcDestinationLane = lane;
	}

	public boolean hasArcDestination() {
		return arcDestinationLane >= 0;
	}

	public BlockPos getArcDestinationPos() {
		return arcDestinationPos;
	}

	public int getArcDestinationLane() {
		return arcDestinationLane;
	}

	public boolean hasPointReservation() {
		return pointReserved;
	}

	public int getReservationDim() {
		return reservationDim;
	}

	public BlockPos getReservationPos() {
		return reservationPos;
	}

	public int getReservationLane() {
		return reservationLane;
	}

	public double getReservationProgress() {
		return reservationProgress;
	}

	public void setPointReservation(int dim, BlockPos pos, int lane, double progress) {
		this.pointReserved = true;
		this.reservationDim = dim;
		this.reservationPos = pos.toImmutable();
		this.reservationLane = lane;
		this.reservationProgress = progress;
	}

	public void clearPointReservation() {
		this.pointReserved = false;
		this.reservationDim = Integer.MIN_VALUE;
		this.reservationPos = BlockPos.ORIGIN;
		this.reservationLane = -1;
		this.reservationProgress = 0.0D;
	}

	public boolean isPointReservation(World world, BlockPos pos, int lane, double progress) {
		if (!pointReserved || world == null) return false;
		return reservationDim == world.provider.getDimension()
				&& reservationLane == lane
				&& reservationPos.equals(pos)
				&& Math.abs(reservationProgress - progress) < EPS;
	}

	public boolean hasHoldReservation() {
		return holdReserved;
	}

	public int getHoldReservationDim() {
		return holdReservationDim;
	}

	public BlockPos getHoldReservationPos() {
		return holdReservationPos;
	}

	public int getHoldReservationLane() {
		return holdReservationLane;
	}

	public double getHoldReservationProgress() {
		return holdReservationProgress;
	}

	public void setHoldReservation(int dim, BlockPos pos, int lane, double progress) {
		this.holdReserved = true;
		this.holdReservationDim = dim;
		this.holdReservationPos = pos.toImmutable();
		this.holdReservationLane = lane;
		this.holdReservationProgress = progress;
	}

	public void clearHoldReservation() {
		this.holdReserved = false;
		this.holdReservationDim = Integer.MIN_VALUE;
		this.holdReservationPos = BlockPos.ORIGIN;
		this.holdReservationLane = -1;
		this.holdReservationProgress = 0.0D;
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

	public boolean advanceActiveArc(double speed) {
		if (activeArc == null) return false;

		double oldX = this.posX;
		double oldY = this.posY;
		double oldZ = this.posZ;

		double arcLen = activeArc.approximateLength(ARC_RESOLUTION);
		double currentDist = arcParam * arcLen;
		double newDist = currentDist + speed;
		double newT = activeArc.paramAtDistance(newDist, ARC_RESOLUTION);

		if (newT >= 1.0D - EPS) {
			Vec3d end = activeArc.p1;
			this.setPosition(end.x, end.y, end.z);

			double remainingSpeed = Math.max(0.0D, speed - (arcLen - currentDist));

			ConveyorQueue.releaseHoldReservation(this);

			BlockPos destPos = null;
			int destLane = -1;
			BlockConveyor destConveyor = null;

			if (hasArcDestination()) {
				destPos = arcDestinationPos;
				destLane = arcDestinationLane;
				this.setConveyorLane(destLane);

				Block destBlock = world.getBlockState(destPos).getBlock();
				if (destBlock instanceof BlockConveyor) {
					destConveyor = (BlockConveyor) destBlock;
					ConveyorQueue.sync(world, destPos, destLane, this);
					updateYawFromFacing(destConveyor.getLaneFacing(world, destPos));
				}
			}

			this.clearActiveArc();

			if (remainingSpeed > EPS && destConveyor != null && destPos != null && destLane >= 0) {
				EnumFacing destFacing = destConveyor.getLaneFacing(world, destPos);
				double entryProgress = destConveyor.getLaneProgress(destPos, destFacing, new Vec3d(this.posX, this.posY, this.posZ));
				double maxProgress = destConveyor.getMovementLimit(world, destPos, destFacing, destLane, this);
				double targetProgress = Math.min(entryProgress + remainingSpeed, maxProgress);

				Vec3d next = destConveyor.getWorldPosition(destPos, destFacing,
						destConveyor.getLaneOffsets()[destLane], targetProgress);
				this.setPosition(next.x, next.y, next.z);
			}

			this.motionX = this.posX - oldX;
			this.motionY = this.posY - oldY;
			this.motionZ = this.posZ - oldZ;
			return true;
		}

		this.arcParam = newT;
		Vec3d p = activeArc.evaluate(newT);
		this.setPosition(p.x, p.y, p.z);

		double sampleT = Math.min(newT + 0.05D, 1.0D);
		Vec3d ahead = activeArc.evaluate(sampleT);
		double dx = ahead.x - p.x;
		double dz = ahead.z - p.z;
		if (dx * dx + dz * dz > 1.0E-10D) {
			float yaw = (float) (Math.atan2(-dx, dz) * (180.0D / Math.PI));
			updateYawSmooth(yaw);
		}

		this.motionX = this.posX - oldX;
		this.motionY = this.posY - oldY;
		this.motionZ = this.posZ - oldZ;
		return true;
	}

	@Override
	public void setDead() {
		if (!this.isDead) ConveyorQueue.unregister(this);
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
		this.conveyorLane = nbt.getInteger("ConveyorLane");
		ItemStack stack = this.getDataManager().get(STACK);
		if (stack == null || stack.isEmpty()) this.setDead();
	}

	@Override
	protected void writeEntityToNBT(NBTTagCompound nbt) {
		if (this.getItemStack() != null)
			nbt.setTag("Item", this.getItemStack().writeToNBT(new NBTTagCompound()));
		nbt.setInteger("schedule", schedule);
		nbt.setInteger("ConveyorLane", conveyorLane);
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