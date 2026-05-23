package com.hbm.tileentity.conductor;

import api.hbm.energy.IEnergyConductor;
import api.hbm.energy.IEnergyUser;
import com.hbm.forgefluid.FluidTypeHandler;
import com.hbm.forgefluid.FluidTypeHandler.FluidTrait;
import com.hbm.lib.ForgeDirection;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.SoundEvents;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.SPacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidStack;

public class TileEntityFFFluidDuctMk4 extends TileEntityFFDuctBaseMk2 implements IEnergyUser {

    public long power = 0L;
    public static final long maxPower = 0L;
    public static final int energyPerTick = 5;

    public boolean isLoaded = true;
    PipeEnergyNetMk4 pipeNet;
    boolean needsNetworkJoin = true;
    boolean lastPoweredState = false;
    int lastSyncedSize = -1;
    int syncSize = 1;
    long syncDrain = 100L;

    @Override
    public boolean isLoaded() {
        return isLoaded;
    }

    @Override
    public int getPipeTier() {
        return 4;
    }

    public PipeEnergyNetMk4 getPipeNet() {
        return pipeNet;
    }

    public void setPipeNet(PipeEnergyNetMk4 net) {
        this.pipeNet = net;
    }

    @Override
    public void onLoad() {
        super.onLoad();
        isLoaded = true;
        needsNetworkJoin = true;
    }

    @Override
    public void setType(Fluid f) {
        Fluid oldType = this.type;
        super.setType(f);
        if (oldType != f && !world.isRemote) {
            rebuildEnergyNetwork();
        }
    }

    private void rebuildEnergyNetwork() {
        if (pipeNet != null && pipeNet.isValid()) {
            pipeNet.split(this);
        }
        pipeNet = null;
        needsNetworkJoin = true;

        for (EnumFacing facing : EnumFacing.VALUES) {
            BlockPos neighborPos = pos.offset(facing);
            if (!world.isBlockLoaded(neighborPos)) {
                continue;
            }
            TileEntity te = world.getTileEntity(neighborPos);
            if (te instanceof TileEntityFFFluidDuctMk4) {
                TileEntityFFFluidDuctMk4 neighbor = (TileEntityFFFluidDuctMk4) te;
                if (neighbor.pipeNet != null && neighbor.pipeNet.isValid()) {
                    neighbor.pipeNet.split(neighbor);
                    neighbor.pipeNet = null;
                    neighbor.needsNetworkJoin = true;
                }
            }
        }
    }

    @Override
    public void update() {
        super.update();

        if (world == null || world.isRemote) {
            return;
        }

        if (needsNetworkJoin) {
            needsNetworkJoin = false;
            joinOrCreateEnergyNetwork();
        }

        if (pipeNet == null || !pipeNet.isValid()) {
            return;
        }

        if (pipeNet.getController() != this) {
            return;
        }

        pipeNet.tick();

        if (world.getTotalWorldTime() % 20L == 0L) {
            pipeNet.updateSubscriptions();
        }

        boolean nowPowered = pipeNet.isPowered();
        int currentSize = pipeNet.size();

        boolean needsSync = false;

        if (nowPowered != lastPoweredState) {
            needsSync = true;
        }

        if (currentSize != lastSyncedSize) {
            needsSync = true;
        }

        if (needsSync) {
            lastPoweredState = nowPowered;
            lastSyncedSize = currentSize;

            for (TileEntityFFFluidDuctMk4 pipe : pipeNet.getMembers()) {
                if (pipe == null || pipe.isInvalid() || pipe.getWorld() == null) {
                    continue;
                }
                pipe.lastPoweredState = nowPowered;
                pipe.syncSize = currentSize;
                pipe.syncDrain = pipeNet.getDrainPerTick() * 20L;
                pipe.lastSyncedSize = currentSize;
            }

            for (TileEntityFFFluidDuctMk4 pipe : pipeNet.getMembers()) {
                if (pipe == null || pipe.isInvalid() || pipe.getWorld() == null) {
                    continue;
                }
                IBlockState state = pipe.getWorld().getBlockState(pipe.getPos());
                pipe.getWorld().notifyBlockUpdate(pipe.getPos(), state, state, 2);
            }
        }
    }

    private boolean canEnergyJoinWith(TileEntityFFFluidDuctMk4 other) {
        if (other == null || other.isInvalid()) {
            return false;
        }

        if (other.getPipeTier() != this.getPipeTier()) {
            return false;
        }

        Fluid myType = this.getType();
        Fluid otherType = other.getType();

        if (myType != null && otherType != null && myType != otherType) {
            return false;
        }

        if (this.network != null && other.network != null && this.network != other.network) {
            return false;
        }

        return true;
    }

    private void joinOrCreateEnergyNetwork() {
        for (EnumFacing facing : EnumFacing.VALUES) {
            BlockPos neighborPos = pos.offset(facing);

            if (!world.isBlockLoaded(neighborPos)) {
                continue;
            }

            TileEntity te = world.getTileEntity(neighborPos);

            if (!(te instanceof TileEntityFFFluidDuctMk4)) {
                continue;
            }

            TileEntityFFFluidDuctMk4 duct = (TileEntityFFFluidDuctMk4) te;

            if (!canEnergyJoinWith(duct)) {
                continue;
            }

            if (this.pipeNet == null && duct.pipeNet != null && duct.pipeNet.isValid()) {
                duct.pipeNet.addPipe(this);
            } else if (this.pipeNet != null && duct.pipeNet != null && this.pipeNet != duct.pipeNet && duct.pipeNet.isValid()) {
                duct.pipeNet.merge(this.pipeNet);
            }
        }

        if (this.pipeNet == null || !this.pipeNet.isValid()) {
            PipeEnergyNetMk4 net = new PipeEnergyNetMk4();
            net.addPipe(this);
        }

        if (this.pipeNet != null && this.pipeNet.isValid()) {
            this.pipeNet.updateSubscriptions();
        }
    }

    public boolean isNetworkPowered() {
        if (world != null && world.isRemote) {
            return lastPoweredState;
        }
        if (pipeNet != null && pipeNet.isValid()) {
            return pipeNet.isPowered();
        }
        return false;
    }

    public int getNetworkSize() {
        if (world != null && world.isRemote) {
            return syncSize;
        }
        if (pipeNet != null && pipeNet.isValid()) {
            return pipeNet.size();
        }
        return 1;
    }

    public long getNetworkDrain() {
        if (world != null && world.isRemote) {
            return syncDrain;
        }
        if (pipeNet != null && pipeNet.isValid()) {
            return pipeNet.getDrainPerTick() * 20L;
        }
        return (long) energyPerTick * 20L;
    }

    @Override
    public int fill(FluidStack resource, boolean doFill) {
        if (resource == null || resource.amount <= 0) return 0;

        if (!world.isRemote && doFill && !isNetworkPowered()) {
            if (FluidTypeHandler.containsTrait(resource.getFluid(), FluidTrait.AMAT)) {
                world.destroyBlock(pos, false);
                world.newExplosion(null, pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D, 4.0F, true, true);
                return 0;
            } else if (FluidTypeHandler.isExtremelyHot(resource.getFluid())) {
                world.destroyBlock(pos, false);
                world.playSound(null, pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D,
                        SoundEvents.BLOCK_LAVA_EXTINGUISH, SoundCategory.BLOCKS, 1.0F, 1.0F);
                return 0;
            }
        }

        return super.fill(resource, doFill);
    }

    @Override
    protected boolean canConnectTo(BlockPos neighborPos, EnumFacing facing) {
        if (super.canConnectTo(neighborPos, facing)) {
            return true;
        }

        TileEntity te = world.getTileEntity(neighborPos);

        if (te instanceof IEnergyConductor || te instanceof IEnergyUser) {
            return !(te instanceof TileEntityFFDuctBaseMk2);
        }

        return false;
    }

    @Override
    public long transferPower(long power) {
        if (power <= 0L) {
            return 0L;
        }

        if (pipeNet != null && pipeNet.isValid()) {
            return pipeNet.transferPower(power);
        }

        return power;
    }

    @Override
    public void setPower(long power) {
    }

    @Override
    public long getPower() {
        return 0L;
    }

    @Override
    public long getMaxPower() {
        if (pipeNet != null && pipeNet.isValid() && pipeNet.getController() == this) {
            return pipeNet.getDrainPerTick();
        }
        return PipeEnergyNetMk4.DRAIN_PER_PIPE_PER_TICK;
    }

    @Override
    public long getTransferWeight() {
        if (pipeNet != null && pipeNet.isValid() && pipeNet.getController() == this) {
            return pipeNet.getDrainPerTick();
        }
        return PipeEnergyNetMk4.DRAIN_PER_PIPE_PER_TICK;
    }

    @Override
    public boolean canConnect(ForgeDirection dir) {
        return true;
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound compound) {
        compound.setBoolean("poweredState", lastPoweredState);
        compound.setInteger("syncSize", syncSize);
        compound.setLong("syncDrain", syncDrain);
        return super.writeToNBT(compound);
    }

    @Override
    public void readFromNBT(NBTTagCompound compound) {
        super.readFromNBT(compound);
        lastPoweredState = compound.getBoolean("poweredState");
        syncSize = compound.getInteger("syncSize");
        syncDrain = compound.getLong("syncDrain");
    }

    @Override
    public SPacketUpdateTileEntity getUpdatePacket() {
        return new SPacketUpdateTileEntity(this.getPos(), 0, this.writeToNBT(new NBTTagCompound()));
    }

    @Override
    public NBTTagCompound getUpdateTag() {
        return this.writeToNBT(new NBTTagCompound());
    }

    @Override
    public void onDataPacket(NetworkManager net, SPacketUpdateTileEntity pkt) {
        this.readFromNBT(pkt.getNbtCompound());
    }

    @Override
    public void invalidate() {
        if (world != null && !world.isRemote && pipeNet != null && pipeNet.isValid()) {
            pipeNet.split(this);
        }
        super.invalidate();
        isLoaded = false;
    }

    @Override
    public void onChunkUnload() {
        if (world != null && !world.isRemote && pipeNet != null && pipeNet.isValid()) {
            pipeNet.removePipe(this);
        }
        super.onChunkUnload();
        isLoaded = false;
    }
}