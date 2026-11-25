package com.hbm.compat.energy;

import api.hbm.energy.IEnergyConnector;
import com.hbm.config.GeneralConfig;
import com.hbm.lib.ForgeDirection;
import com.hbm.tileentity.network.energy.TileEntityCableBaseNT;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.energy.CapabilityEnergy;
import net.minecraftforge.energy.IEnergyStorage;
import net.minecraftforge.fml.common.Loader;

/** Side-aware direct machine ports; native HBM connectors always keep their own path. */
public final class ExternalEnergy {
    private ExternalEnergy() {}
    public static double fePerHe() {
        double rate = GeneralConfig.conversionRateHeToRF;
        return Double.isFinite(rate) && rate > 0 ? rate : 1;
    }
    public static IEnergyStorage fe(TileEntity tile, EnumFacing side) {
        if(tile == null || tile.isInvalid() || tile instanceof IEnergyConnector) return null;
        return tile.hasCapability(CapabilityEnergy.ENERGY, side) ? tile.getCapability(CapabilityEnergy.ENERGY, side) : null;
    }
    public static boolean connects(TileEntity tile, EnumFacing side) {
        if(tile == null || tile instanceof IEnergyConnector) return false;
        IEnergyStorage storage = fe(tile, side);
        if(storage != null) return storage.canReceive() || storage.canExtract();
        return Loader.isModLoaded("ic2") && IC2Ports.connects(tile, side);
    }
    public static long offer(TileEntityCableBaseNT cable, TileEntity tile, EnumFacing side, long he, boolean simulate) {
        if(he <= 0 || tile == null || tile instanceof IEnergyConnector) return 0;
        IEnergyStorage storage = fe(tile, side);
        if(storage != null) {
            if(!storage.canReceive()) return 0;
            int offer = (int)Math.min(Integer.MAX_VALUE, Math.floor(he * fePerHe()));
            int accepted = Math.max(0, Math.min(offer, storage.receiveEnergy(offer, simulate)));
            // Round the debit up, never create HE through a fractional conversion.
            return Math.min(he, (long)Math.ceil(accepted / fePerHe()));
        }
        return Loader.isModLoaded("ic2") ? IC2Ports.offer(cable, tile, side, he, simulate) : 0;
    }
    public static void pull(TileEntityCableBaseNT cable, TileEntity tile, EnumFacing side) {
        if(tile == null || tile instanceof IEnergyConnector) return;
        IEnergyStorage storage = fe(tile, side);
        if(storage != null) {
            // Bidirectional storage is treated as a sink, avoiding an automatic charging loop.
            if(storage.canExtract() && !storage.canReceive()) {
                int room = cable.externalInput.receiveEnergy(Integer.MAX_VALUE, true);
                int taken = storage.extractEnergy(room, false);
                if(taken > 0) cable.externalInput.receiveEnergy(taken, false);
            }
        } else if(Loader.isModLoaded("ic2")) IC2Ports.pull(cable, tile, side);
    }
    public static final class Receiver extends TileEntity implements IEnergyConnector {
        private final TileEntityCableBaseNT cable;
        private final ForgeDirection direction;
        public Receiver(TileEntityCableBaseNT cable, ForgeDirection direction) {
            this.cable = cable; this.direction = direction;
            setWorld(cable.getWorld()); setPos(cable.getPos());
        }
        private TileEntity target() {
            if(!isLoaded()) return null;
            return world.getTileEntity(pos.add(direction.offsetX, direction.offsetY, direction.offsetZ));
        }
        @Override public boolean isLoaded() {
            return !cable.isInvalid() && cable.isLoaded() && cable.getPowerNet() != null
                && cable.canConnect(direction) && world.isBlockLoaded(pos)
                && world.isBlockLoaded(pos.add(direction.offsetX, direction.offsetY, direction.offsetZ));
        }
        @Override public long getPower() { return 0; }
        @Override public long getMaxPower() {
            return cable.importingExternal ? 0 : offer(cable, target(), direction.getOpposite().toEnumFacing(), 100000, true);
        }
        @Override public long transferPower(long power) {
            return cable.importingExternal ? power : power - offer(cable, target(), direction.getOpposite().toEnumFacing(), power, false);
        }
    }
}
