package com.hbm.tileentity.conductor;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import api.hbm.energy.IEnergyUser;
import api.hbm.energy.IEnergyConductor;
import com.hbm.forgefluid.FluidTypeHandler;
import com.hbm.forgefluid.FluidTypeHandler.FluidTrait;
import com.hbm.lib.ForgeDirection;

import net.minecraft.block.state.IBlockState;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.SPacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.init.SoundEvents;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidStack;

public class TileEntityFFFluidDuctMk4 extends TileEntityFFDuctBaseMk2 implements IEnergyUser {

    public long power;
    public static final long maxPower = 10000;
    public static final int energyPerTick = 5;

    public boolean isLoaded = true;

    private List<TileEntityFFFluidDuctMk4> networkPipeCache = null;
    private long lastCacheTime = -1;

    @Override
    public boolean isLoaded() {
        return isLoaded;
    }

    @Override
    public int getPipeTier() {
        return 4;
    }

    @Override
    public void update() {
        super.update();
        if(!world.isRemote) {
            if(world.getTotalWorldTime() % 20 == 0) {
                for(EnumFacing facing : EnumFacing.VALUES) {
                    TileEntity te = world.getTileEntity(pos.offset(facing));
                    if(te instanceof IEnergyConductor) {
                        IEnergyConductor con = (IEnergyConductor) te;
                        ForgeDirection dir = ForgeDirection.getOrientation(facing.getIndex());
                        if(con.canConnect(dir.getOpposite()) && con.getPowerNet() != null) {
                            con.getPowerNet().subscribe(this);
                        }
                    }
                }
            }

            shareEnergyWithNeighbors();

            power -= energyPerTick;
            if(power < 0) power = 0;

            if(world.getTotalWorldTime() % 10 == 0) {
                IBlockState state = world.getBlockState(pos);
                world.notifyBlockUpdate(pos, state, state, 2);
            }
        }
    }

    public boolean isNetworkPowered() {
        List<TileEntityFFFluidDuctMk4> pipes = getNetworkPipes();
        for(TileEntityFFFluidDuctMk4 pipe : pipes) {
            if(pipe.power <= 0) return false;
        }
        return true;
    }

    private void shareEnergyWithNeighbors() {
        Fluid myType = this.getType();
        int neighborCount = 0;
        long totalPower = this.power;

        TileEntityFFFluidDuctMk4[] neighbors = new TileEntityFFFluidDuctMk4[6];

        for(EnumFacing facing : EnumFacing.VALUES) {
            TileEntity te = world.getTileEntity(pos.offset(facing));
            if(te instanceof TileEntityFFFluidDuctMk4) {
                TileEntityFFFluidDuctMk4 neighbor = (TileEntityFFFluidDuctMk4) te;
                if(neighbor.getPipeTier() == this.getPipeTier()) {
                    Fluid neighborType = neighbor.getType();
                    if(myType != neighborType) continue;
                    neighbors[facing.getIndex()] = neighbor;
                    totalPower += neighbor.power;
                    neighborCount++;
                }
            }
        }

        if(neighborCount > 0) {
            int members = neighborCount + 1;
            long share = totalPower / members;
            long remainder = totalPower % members;

            this.power = share + remainder;
            for(TileEntityFFFluidDuctMk4 neighbor : neighbors) {
                if(neighbor != null) {
                    neighbor.power = share;
                }
            }
        }
    }

    protected List<TileEntityFFFluidDuctMk4> getNetworkPipes() {
        long currentTime = world.getTotalWorldTime();
        if(networkPipeCache != null && lastCacheTime == currentTime) {
            return networkPipeCache;
        }

        List<TileEntityFFFluidDuctMk4> result = new ArrayList<>();
        Set<BlockPos> visited = new HashSet<>();
        collectPipes(pos, visited, result, this.getType());
        networkPipeCache = result;
        lastCacheTime = currentTime;
        return result;
    }

    private void collectPipes(BlockPos currentPos, Set<BlockPos> visited, List<TileEntityFFFluidDuctMk4> result, Fluid fluidType) {
        if(!visited.add(currentPos)) return;
        TileEntity te = world.getTileEntity(currentPos);
        if(!(te instanceof TileEntityFFFluidDuctMk4)) return;
        TileEntityFFFluidDuctMk4 pipe = (TileEntityFFFluidDuctMk4) te;
        if(pipe.getPipeTier() != this.getPipeTier()) return;
        Fluid pipeType = pipe.getType();
        if(fluidType != pipeType) return;
        result.add(pipe);
        for(EnumFacing facing : EnumFacing.VALUES) {
            collectPipes(currentPos.offset(facing), visited, result, fluidType);
        }
    }

    @Override
    public int fill(FluidStack resource, boolean doFill) {
        if(resource == null || resource.amount <= 0) return 0;

        int filled = super.fill(resource, doFill);

        if(filled > 0 && doFill && !world.isRemote) {
            if(!isNetworkPowered()) {
                if(FluidTypeHandler.containsTrait(resource.getFluid(), FluidTrait.AMAT)) {
                    world.destroyBlock(pos, false);
                    world.newExplosion(null, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 4, true, true);
                } else if(FluidTypeHandler.isExtremelyHot(resource.getFluid())) {
                    world.destroyBlock(pos, false);
                    world.playSound(null, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, SoundEvents.BLOCK_LAVA_EXTINGUISH, SoundCategory.BLOCKS, 1.0F, 1.0F);
                }
            }
        }

        return filled;
    }

    @Override
    protected boolean canConnectTo(BlockPos neighborPos, EnumFacing facing) {
        if(super.canConnectTo(neighborPos, facing))
            return true;
        TileEntity te = world.getTileEntity(neighborPos);
        if(te instanceof IEnergyConductor || te instanceof IEnergyUser) {
            if(!(te instanceof TileEntityFFDuctBaseMk2)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void setPower(long power) {
        this.power = power;
    }

    @Override
    public long getPower() {
        return power;
    }

    @Override
    public long getMaxPower() {
        return maxPower;
    }

    @Override
    public boolean canConnect(ForgeDirection dir) {
        return true;
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound compound) {
        compound.setLong("power", power);
        return super.writeToNBT(compound);
    }

    @Override
    public void readFromNBT(NBTTagCompound compound) {
        super.readFromNBT(compound);
        power = compound.getLong("power");
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
        super.invalidate();
        isLoaded = false;
    }

    @Override
    public void onChunkUnload() {
        super.onChunkUnload();
        isLoaded = false;
    }
}