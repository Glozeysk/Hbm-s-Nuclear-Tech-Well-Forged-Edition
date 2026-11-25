package com.hbm.compat.energy;

import com.hbm.tileentity.network.energy.TileEntityCableBaseNT;
import ic2.api.energy.tile.*;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;

/** Loaded only when IC2 is present. Direct adjacent machine support, without a hard IC2 dependency. */
final class IC2Ports {
    // Conventional conversion: one EU is four FE, with the configured HE:FE ratio.
    private static double euPerHe() { return ExternalEnergy.fePerHe() / 4D; }
    static boolean connects(TileEntity tile, EnumFacing side) {
        Port port = new Port(tile, side);
        return (tile instanceof IEnergySink && ((IEnergySink)tile).acceptsEnergyFrom(port, side))
            || (tile instanceof IEnergySource && ((IEnergySource)tile).emitsEnergyTo(port, side));
    }
    private static final class Port extends TileEntity implements IEnergyEmitter, IEnergyAcceptor {
        Port(TileEntityCableBaseNT cable) { setWorld(cable.getWorld()); setPos(cable.getPos()); }
        Port(TileEntity tile, EnumFacing side) { setWorld(tile.getWorld()); setPos(tile.getPos().offset(side)); }
        public boolean emitsEnergyTo(IEnergyAcceptor receiver, EnumFacing side) { return true; }
        public boolean acceptsEnergyFrom(IEnergyEmitter emitter, EnumFacing side) { return true; }
    }
    static long offer(TileEntityCableBaseNT cable, TileEntity tile, EnumFacing side, long he, boolean simulate) {
        if(!(tile instanceof IEnergySink)) return 0;
        IEnergySink sink = (IEnergySink)tile;
        if(!sink.acceptsEnergyFrom(new Port(cable), side)) return 0;
        // One packet per call, capped to the receiver's tier; never overvolt an LV machine.
        double voltage = Math.pow(2, 3 + 2 * Math.max(0, Math.min(13, sink.getSinkTier())));
        double amount = Math.min(Math.min(he * euPerHe(), voltage), sink.getDemandedEnergy());
        if(!Double.isFinite(amount) || amount <= 0) return 0;
        double accepted = simulate ? amount : amount - sink.injectEnergy(side, amount, voltage);
        if(!Double.isFinite(accepted) || accepted <= 0) return 0;
        return Math.min(he, (long)Math.ceil(Math.min(amount, accepted) / euPerHe()));
    }
    static void pull(TileEntityCableBaseNT cable, TileEntity tile, EnumFacing side) {
        if(!(tile instanceof IEnergySource)) return;
        if(tile instanceof IEnergySink && ((IEnergySink)tile).acceptsEnergyFrom(new Port(cable), side)) return;
        IEnergySource source = (IEnergySource)tile;
        if(!source.emitsEnergyTo(new Port(cable), side)) return;
        double room = cable.externalInput.receiveEnergy(Integer.MAX_VALUE, true) / 4D;
        double voltage = Math.pow(2, 3 + 2 * Math.max(0, Math.min(13, source.getSourceTier())));
        double amount = Math.min(Math.min(room, voltage), source.getOfferedEnergy());
        int fe = (int)Math.min(Integer.MAX_VALUE, Math.floor(amount * 4D));
        if(fe <= 0) return;
        source.drawEnergy(fe / 4D);
        cable.externalInput.receiveEnergy(fe, false);
    }
}
