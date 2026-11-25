package com.hbm.compat.energy;

import com.hbm.config.GeneralConfig;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.energy.CapabilityEnergy;
import net.minecraftforge.energy.EnergyStorage;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ExternalEnergyTest {
    static class Machine extends TileEntity {
        final EnergyStorage storage = new EnergyStorage(10000);
        @Override public boolean hasCapability(Capability<?> capability, EnumFacing side) {
            return capability == CapabilityEnergy.ENERGY && side == EnumFacing.WEST;
        }
        @Override @SuppressWarnings("unchecked") public <T> T getCapability(Capability<T> capability, EnumFacing side) {
            return hasCapability(capability, side) ? (T)storage : null;
        }
    }
    @Test void simulationAndActualTransferRespectPortAndCapacity() {
        GeneralConfig.conversionRateHeToRF = 1;
        Machine machine = new Machine();
        assertEquals(123, ExternalEnergy.offer(null, machine, EnumFacing.WEST, 123, true));
        assertEquals(0, machine.storage.getEnergyStored());
        assertEquals(123, ExternalEnergy.offer(null, machine, EnumFacing.WEST, 123, false));
        assertEquals(123, machine.storage.getEnergyStored());
        assertNull(ExternalEnergy.fe(machine, EnumFacing.EAST));
        assertEquals(9877, ExternalEnergy.offer(null, machine, EnumFacing.WEST, Long.MAX_VALUE, false));
        assertEquals(10000, machine.storage.getEnergyStored());
    }
    @Test void fractionalConversionCannotCreateEnergy() {
        for(double rate : new double[]{0.25, 1, 2.5, 4}) {
            GeneralConfig.conversionRateHeToRF = rate;
            Machine machine = new Machine();
            long charged = ExternalEnergy.offer(null, machine, EnumFacing.WEST, 103, false);
            assertTrue(machine.storage.getEnergyStored() <= charged * rate);
            assertTrue(charged <= 103);
        }
        GeneralConfig.conversionRateHeToRF = 1;
    }
    @Test void invalidConversionRateFallsBackSafely() {
        for(double rate : new double[]{0, -1, Double.NaN, Double.POSITIVE_INFINITY}) {
            GeneralConfig.conversionRateHeToRF = rate;
            assertEquals(1, ExternalEnergy.fePerHe());
        }
        GeneralConfig.conversionRateHeToRF = 1;
    }
}
