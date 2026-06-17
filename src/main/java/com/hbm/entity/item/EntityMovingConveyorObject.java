package com.hbm.entity.item;

import api.hbm.block.IConveyorBelt;
import api.hbm.block.IEnterableBlock;
import com.hbm.blocks.network.BlockConveyor;
import com.hbm.blocks.network.ConveyorQueue;
import com.hbm.tileentity.network.TileEntityCraneBase;
import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.entity.MoverType;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public abstract class EntityMovingConveyorObject extends Entity {

    protected int turnProgress;
    protected double syncPosX;
    protected double syncPosY;
    protected double syncPosZ;
    @SideOnly(Side.CLIENT)
    protected double velocityX;
    @SideOnly(Side.CLIENT)
    protected double velocityY;
    @SideOnly(Side.CLIENT)
    protected double velocityZ;

    public EntityMovingConveyorObject(World world) {
        super(world);
        this.noClip = true;
    }

    @Override
    public boolean canBeCollidedWith() {
        return true;
    }

    @Override
    public boolean hitByEntity(Entity attacker) {
        if (attacker instanceof EntityPlayer) this.setDead();
        return false;
    }

    @Override
    protected boolean canTriggerWalking() {
        return true;
    }

    @Override
    public void onUpdate() {
        this.prevPosX = this.posX;
        this.prevPosY = this.posY;
        this.prevPosZ = this.posZ;
        this.prevRotationYaw = this.rotationYaw;
        this.prevRotationPitch = this.rotationPitch;

        if (world.isRemote) {
            if (this.turnProgress > 0) {
                double interpX = this.posX + (this.syncPosX - this.posX) / (double) this.turnProgress;
                double interpY = this.posY + (this.syncPosY - this.posY) / (double) this.turnProgress;
                double interpZ = this.posZ + (this.syncPosZ - this.posZ) / (double) this.turnProgress;

                --this.turnProgress;
                this.setPosition(interpX, interpY, interpZ);
            } else {
                this.setPosition(this.posX, this.posY, this.posZ);
            }
            return;
        }

        ticksExisted++;
        if (this.ticksExisted <= 5) return;

        if (this instanceof EntityMovingItem) {
            EntityMovingItem movingItem = (EntityMovingItem) this;
            if (movingItem.getActiveArc() != null) {
                movingItem.advanceActiveArc(getMoveSpeed());
                return;
            }
        }

        int blockX = (int) Math.floor(posX);
        int blockY = (int) Math.floor(posY);
        int blockZ = (int) Math.floor(posZ);
        BlockPos blockPos = new BlockPos(blockX, blockY, blockZ);
        Block block = world.getBlockState(blockPos).getBlock();

        boolean isOnConveyor = block instanceof IConveyorBelt
                && ((IConveyorBelt) block).canItemStay(world, blockX, blockY, blockZ, new Vec3d(posX, posY, posZ));

        if (!isOnConveyor) {
            if (this instanceof EntityMovingItem) ConveyorQueue.unregister((EntityMovingItem) this);
            if (onLeaveConveyor()) return;
        } else {
            Vec3d target;

            if (block instanceof BlockConveyor
                    && ((BlockConveyor) block).usesLaneQueues()
                    && this instanceof EntityMovingItem) {

                BlockConveyor conveyor = (BlockConveyor) block;
                EntityMovingItem movingItem = (EntityMovingItem) this;

                int lane = movingItem.getConveyorLane();
                if (lane < 0 || lane >= conveyor.getLaneCount()) {
                    lane = conveyor.getClosestLaneIndex(world, blockPos, new Vec3d(posX, posY, posZ));
                    movingItem.setConveyorLane(lane);
                }

                ConveyorQueue.sync(world, blockPos, lane, movingItem);
                target = conveyor.getTravelLocationForItem(world, blockPos, movingItem, getMoveSpeed());
                movingItem.updateYawFromFacing(conveyor.getLaneFacing(world, blockPos));
            } else {
                target = ((IConveyorBelt) block).getTravelLocation(
                        world, blockX, blockY, blockZ,
                        new Vec3d(posX, posY, posZ), getMoveSpeed());

                if (this instanceof EntityMovingItem && block instanceof BlockConveyor) {
                    ((EntityMovingItem) this).updateYawFromFacing(
                            ((BlockConveyor) block).getLaneFacing(world, blockPos));
                }
            }

            this.motionX = target.x - posX;
            this.motionY = target.y - posY;
            this.motionZ = target.z - posZ;
        }

        BlockPos lastPos = new BlockPos(posX, posY, posZ);
        this.move(MoverType.SELF, motionX, motionY, motionZ);
        BlockPos newPos = new BlockPos(posX, posY, posZ);

        if (!lastPos.equals(newPos)) {
            handleBlockTransition(lastPos, newPos);
        } else if (this instanceof EntityMovingItem) {
            EntityMovingItem movingItem = (EntityMovingItem) this;
            Block currentBlock = world.getBlockState(blockPos).getBlock();
            if (currentBlock instanceof BlockConveyor && ((BlockConveyor) currentBlock).usesLaneQueues()) {
                ConveyorQueue.sync(world, blockPos, movingItem.getConveyorLane(), movingItem);
            }
        }
    }

    private void handleBlockTransition(BlockPos lastPos, BlockPos newPos) {
        Block newBlock = world.getBlockState(newPos).getBlock();

        if (this instanceof EntityMovingItem) {
            EntityMovingItem movingItem = (EntityMovingItem) this;

            if (newBlock instanceof BlockConveyor && ((BlockConveyor) newBlock).usesLaneQueues()) {
                BlockConveyor newConveyor = (BlockConveyor) newBlock;
                int newLane = newConveyor.getClosestLaneIndex(world, newPos, new Vec3d(posX, posY, posZ));
                movingItem.setConveyorLane(newLane);
                ConveyorQueue.sync(world, newPos, newLane, movingItem);
                movingItem.updateYawFromFacing(newConveyor.getLaneFacing(world, newPos));
            } else {
                ConveyorQueue.unregister(movingItem);
            }
        }

        if (newBlock instanceof IEnterableBlock) {
            IEnterableBlock enterable = (IEnterableBlock) newBlock;
            EnumFacing dir = getTransitionDirection(lastPos, newPos);

            TileEntity tileEntity = world.getTileEntity(newPos);
            if (tileEntity instanceof TileEntityCraneBase) {
                TileEntityCraneBase craneBase = (TileEntityCraneBase) tileEntity;
                if (dir == craneBase.getInputSide()) enterBlock(enterable, newPos, dir);
            } else {
                enterBlock(enterable, newPos, dir);
            }
        } else {
            if (!newBlock.getMaterial(world.getBlockState(newPos)).isSolid()) {
                Block belowBlock = world.getBlockState(newPos.down()).getBlock();
                if (belowBlock instanceof IEnterableBlock) {
                    enterBlockFalling((IEnterableBlock) belowBlock, newPos);
                }
            }
        }
    }

    private EnumFacing getTransitionDirection(BlockPos from, BlockPos to) {
        int dx = to.getX() - from.getX();
        int dy = to.getY() - from.getY();
        int dz = to.getZ() - from.getZ();

        if (dx == 1 && dy == 0 && dz == 0) return EnumFacing.WEST;
        if (dx == -1 && dy == 0 && dz == 0) return EnumFacing.EAST;
        if (dx == 0 && dy == 1 && dz == 0) return EnumFacing.DOWN;
        if (dx == 0 && dy == -1 && dz == 0) return EnumFacing.UP;
        if (dx == 0 && dy == 0 && dz == 1) return EnumFacing.NORTH;
        if (dx == 0 && dy == 0 && dz == -1) return EnumFacing.SOUTH;
        return EnumFacing.NORTH;
    }

    public abstract void enterBlock(IEnterableBlock enterable, BlockPos pos, EnumFacing dir);

    public void enterBlockFalling(IEnterableBlock enterable, BlockPos pos) {
        this.enterBlock(enterable, pos.add(0, -1, 0), EnumFacing.UP);
    }

    public abstract boolean onLeaveConveyor();

    public double getMoveSpeed() {
        return 0.0625D;
    }

    @SideOnly(Side.CLIENT)
    public void setVelocity(double motionX, double motionY, double motionZ) {
        this.velocityX = this.motionX = motionX;
        this.velocityY = this.motionY = motionY;
        this.velocityZ = this.motionZ = motionZ;
    }

    @SideOnly(Side.CLIENT)
    public void setPositionAndRotation2(double x, double y, double z, float yaw, float pitch, int posRotationIncrements) {
        this.syncPosX = x;
        this.syncPosY = y;
        this.syncPosZ = z;
        this.turnProgress = posRotationIncrements + 2;
        this.motionX = this.velocityX;
        this.motionY = this.velocityY;
        this.motionZ = this.velocityZ;
    }
}