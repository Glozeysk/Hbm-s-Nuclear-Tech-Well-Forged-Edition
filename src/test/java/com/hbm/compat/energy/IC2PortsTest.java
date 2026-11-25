package com.hbm.compat.energy;

import api.hbm.energy.PowerNet;
import com.hbm.config.GeneralConfig;
import com.hbm.tileentity.network.energy.TileEntityCableBaseNT;
import ic2.api.energy.tile.*;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class IC2PortsTest {
    static class Sink extends TileEntity implements IEnergySink {
        double stored, lastVoltage;
        public boolean acceptsEnergyFrom(IEnergyEmitter source, EnumFacing side) { return side == EnumFacing.WEST; }
        public double getDemandedEnergy() { return 1000 - stored; }
        public int getSinkTier() { return 1; }
        public double injectEnergy(EnumFacing side, double amount, double voltage) {
            lastVoltage = voltage; stored += amount / 2; return amount / 2;
        }
    }
    static class Source extends TileEntity implements IEnergySource {
        double stored = 1000;
        public boolean emitsEnergyTo(IEnergyAcceptor target, EnumFacing side) { return side == EnumFacing.EAST; }
        public double getOfferedEnergy() { return stored; }
        public int getSourceTier() { return 1; }
        public void drawEnergy(double amount) { stored -= amount; }
    }
    @Test void simulationVoltageAndPartialAcceptance() {
        GeneralConfig.conversionRateHeToRF = 1;
        TileEntityCableBaseNT cable = new TileEntityCableBaseNT();
        Sink sink = new Sink();
        assertEquals(128, IC2Ports.offer(cable, sink, EnumFacing.WEST, 10000, true));
        assertEquals(0, sink.stored);
        assertEquals(64, IC2Ports.offer(cable, sink, EnumFacing.WEST, 10000, false));
        assertEquals(16, sink.stored);
        assertEquals(32, sink.lastVoltage);
        assertEquals(0, IC2Ports.offer(cable, sink, EnumFacing.EAST, 10000, false));
        assertFalse(IC2Ports.connects(sink, EnumFacing.EAST));
    }
    @Test void sourceDrawRespectsSideTierAndBuffer() {
        TileEntityCableBaseNT cable = new TileEntityCableBaseNT();
        cable.setPowerNet(new PowerNet());
        Source source = new Source();
        IC2Ports.pull(cable, source, EnumFacing.WEST);
        assertEquals(1000, source.stored);
        IC2Ports.pull(cable, source, EnumFacing.EAST);
        assertEquals(968, source.stored);
        assertEquals(128, cable.externalInput.getEnergyStored());
        cable.externalInput.receiveEnergy(Integer.MAX_VALUE, false);
        IC2Ports.pull(cable, source, EnumFacing.EAST);
        assertEquals(968, source.stored);
    }
}
