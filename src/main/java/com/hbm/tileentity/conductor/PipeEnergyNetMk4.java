package com.hbm.tileentity.conductor;

import api.hbm.energy.IEnergyConductor;
import api.hbm.energy.IPowerNet;
import com.hbm.lib.ForgeDirection;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fluids.Fluid;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class PipeEnergyNetMk4 {

    private final List<TileEntityFFFluidDuctMk4> members = new ArrayList<>();
    private final Set<IPowerNet> subscribedNets = new HashSet<>();
    private TileEntityFFFluidDuctMk4 controller;
    private boolean valid = true;

    private long receivedThisTick = 0L;
    private boolean powered = false;

    public static final long DRAIN_PER_PIPE_PER_TICK = 5L;

    public boolean isValid() {
        return valid;
    }

    public List<TileEntityFFFluidDuctMk4> getMembers() {
        return members;
    }

    public int size() {
        return members.size();
    }

    public TileEntityFFFluidDuctMk4 getController() {
        return controller;
    }

    public boolean isPowered() {
        return powered;
    }

    public long getDrainPerTick() {
        return (long) members.size() * DRAIN_PER_PIPE_PER_TICK;
    }

    public long transferPower(long power) {
        if (!valid || power <= 0L) {
            return power;
        }

        long need = getDrainPerTick() - receivedThisTick;

        if (need <= 0L) {
            return power;
        }

        long accepted = Math.min(need, power);
        receivedThisTick += accepted;

        return power - accepted;
    }

    public void tick() {
        if (!valid) {
            return;
        }

        powered = receivedThisTick >= getDrainPerTick();
        receivedThisTick = 0L;
    }

    public void addPipe(TileEntityFFFluidDuctMk4 pipe) {
        PipeEnergyNetMk4 old = pipe.getPipeNet();

        if (old == this) {
            return;
        }

        if (old != null) {
            old.removePipe(pipe);
        }

        members.add(pipe);
        pipe.setPipeNet(this);
        updateController();

        if (controller != null) {
            controller.lastSyncedSize = -1;
        }
    }

    public void removePipe(TileEntityFFFluidDuctMk4 pipe) {
        members.remove(pipe);

        if (pipe.getPipeNet() == this) {
            pipe.setPipeNet(null);
        }

        updateController();

        if (controller != null) {
            controller.lastSyncedSize = -1;
        }
    }

    public void merge(PipeEnergyNetMk4 other) {
        if (other == null || other == this || !other.valid) {
            return;
        }

        List<TileEntityFFFluidDuctMk4> otherMembers = new ArrayList<>(other.members);
        other.destroy();

        for (TileEntityFFFluidDuctMk4 pipe : otherMembers) {
            this.addPipe(pipe);
        }
    }

    public void destroy() {
        unsubscribeAll();
        valid = false;

        for (TileEntityFFFluidDuctMk4 pipe : members) {
            if (pipe.getPipeNet() == this) {
                pipe.setPipeNet(null);
            }
        }

        members.clear();
        controller = null;
        powered = false;
        receivedThisTick = 0L;
    }

    public void split(TileEntityFFFluidDuctMk4 removed) {
        if (!valid) {
            return;
        }

        List<TileEntityFFFluidDuctMk4> oldMembers = new ArrayList<>(members);
        destroy();
        oldMembers.remove(removed);

        if (oldMembers.isEmpty()) {
            return;
        }

        Set<TileEntityFFFluidDuctMk4> oldSet = new HashSet<>(oldMembers);
        Set<TileEntityFFFluidDuctMk4> processed = new HashSet<>();

        for (TileEntityFFFluidDuctMk4 pipe : oldMembers) {
            if (processed.contains(pipe)) {
                continue;
            }
            if (pipe == null || pipe.isInvalid() || pipe.getWorld() == null) {
                continue;
            }

            List<TileEntityFFFluidDuctMk4> cluster = collectCluster(pipe, oldSet, processed);

            if (cluster.isEmpty()) {
                continue;
            }

            PipeEnergyNetMk4 newNet = new PipeEnergyNetMk4();

            for (TileEntityFFFluidDuctMk4 member : cluster) {
                newNet.addPipe(member);
                member.needsNetworkJoin = false;
            }
        }
    }

    private List<TileEntityFFFluidDuctMk4> collectCluster(TileEntityFFFluidDuctMk4 start, Set<TileEntityFFFluidDuctMk4> oldSet, Set<TileEntityFFFluidDuctMk4> processed) {
        List<TileEntityFFFluidDuctMk4> cluster = new ArrayList<>();
        List<TileEntityFFFluidDuctMk4> stack = new ArrayList<>();
        Set<BlockPos> visited = new HashSet<>();

        Fluid startType = start.getType();

        stack.add(start);
        visited.add(start.getPos());

        while (!stack.isEmpty()) {
            TileEntityFFFluidDuctMk4 current = stack.remove(stack.size() - 1);

            if (!oldSet.contains(current) || processed.contains(current)) {
                continue;
            }

            Fluid currentType = current.getType();
            if (startType != null && currentType != null && startType != currentType) {
                continue;
            }

            processed.add(current);
            cluster.add(current);

            World world = current.getWorld();
            if (world == null) {
                continue;
            }

            for (EnumFacing facing : EnumFacing.VALUES) {
                BlockPos neighborPos = current.getPos().offset(facing);

                if (!visited.add(neighborPos)) {
                    continue;
                }

                if (!world.isBlockLoaded(neighborPos)) {
                    continue;
                }

                TileEntity te = world.getTileEntity(neighborPos);
                if (!(te instanceof TileEntityFFFluidDuctMk4)) {
                    continue;
                }

                TileEntityFFFluidDuctMk4 neighbor = (TileEntityFFFluidDuctMk4) te;

                if (!oldSet.contains(neighbor)) {
                    continue;
                }
                if (neighbor.isInvalid()) {
                    continue;
                }
                if (neighbor.getPipeTier() != current.getPipeTier()) {
                    continue;
                }

                stack.add(neighbor);
            }
        }

        return cluster;
    }

    public void updateSubscriptions() {
        if (!valid || controller == null || controller.isInvalid() || controller.getWorld() == null) {
            unsubscribeAll();
            return;
        }

        World world = controller.getWorld();
        Set<IPowerNet> found = new HashSet<>();

        for (TileEntityFFFluidDuctMk4 pipe : members) {
            if (pipe == null || pipe.isInvalid()) {
                continue;
            }

            for (EnumFacing facing : EnumFacing.VALUES) {
                BlockPos neighborPos = pipe.getPos().offset(facing);

                if (!world.isBlockLoaded(neighborPos)) {
                    continue;
                }

                TileEntity te = world.getTileEntity(neighborPos);

                if (te instanceof TileEntityFFFluidDuctMk4) {
                    continue;
                }

                if (!(te instanceof IEnergyConductor)) {
                    continue;
                }

                IEnergyConductor conductor = (IEnergyConductor) te;
                ForgeDirection dir = ForgeDirection.getOrientation(facing.getIndex());

                if (!conductor.canConnect(dir.getOpposite())) {
                    continue;
                }

                IPowerNet ext = conductor.getPowerNet();
                if (ext == null || !ext.isValid()) {
                    continue;
                }

                found.add(ext);
            }
        }

        for (IPowerNet oldNet : new HashSet<>(subscribedNets)) {
            if (!found.contains(oldNet) || !oldNet.isValid()) {
                if (oldNet.isValid() && controller != null) {
                    oldNet.unsubscribe(controller);
                }
                subscribedNets.remove(oldNet);
            }
        }

        for (IPowerNet newNet : found) {
            if (!subscribedNets.contains(newNet)) {
                if (!newNet.isSubscribed(controller)) {
                    newNet.subscribe(controller);
                }
                subscribedNets.add(newNet);
            }
        }
    }

    public void unsubscribeAll() {
        if (controller != null) {
            for (IPowerNet net : new HashSet<>(subscribedNets)) {
                if (net != null && net.isValid()) {
                    net.unsubscribe(controller);
                }
            }
        }
        subscribedNets.clear();
    }

    private void updateController() {
        TileEntityFFFluidDuctMk4 oldController = controller;
        controller = null;

        for (TileEntityFFFluidDuctMk4 pipe : members) {
            if (pipe == null || pipe.isInvalid()) {
                continue;
            }

            if (controller == null || pipe.getPos().toLong() < controller.getPos().toLong()) {
                controller = pipe;
            }
        }

        if (oldController != controller && oldController != null) {
            for (IPowerNet net : new HashSet<>(subscribedNets)) {
                if (net != null && net.isValid()) {
                    net.unsubscribe(oldController);
                }
            }
            subscribedNets.clear();
        }
    }
}