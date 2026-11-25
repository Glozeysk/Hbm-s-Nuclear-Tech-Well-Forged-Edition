package com.hbm.compat.energy;

import com.hbm.config.GeneralConfig;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.energy.CapabilityEnergy;
import org.junit.jupiter.api.Test;
import zmaster587.libVulpes.cap.ForgePowerCapability;
import zmaster587.libVulpes.util.UniversalBattery;
import static org.junit.jupiter.api.Assertions.*;

/** Uses upstream LibVulpes' real FE adapter and energy buffer, without booting an AR world. */
class AdvancedRocketryPortTest {
    static class Input extends TileEntity {
        final UniversalBattery battery = new UniversalBattery(10000) {
            @Override public boolean canExtract() { return false; }
        };
        @Override public boolean hasCapability(Capability<?> cap, EnumFacing side) {
            return cap == CapabilityEnergy.ENERGY;
        }
        @Override @SuppressWarnings("unchecked") public <T> T getCapability(Capability<T> cap, EnumFacing side) {
            return cap == CapabilityEnergy.ENERGY ? (T)new ForgePowerCapability(battery) : null;
        }
    }
    @Test void freshCapabilityWrappersStillShareMachineBattery() {
        GeneralConfig.conversionRateHeToRF = 1;
        Input input = new Input();
        for(EnumFacing side : EnumFacing.values()) assertTrue(ExternalEnergy.connects(input, side));
        assertEquals(7500, ExternalEnergy.offer(null, input, EnumFacing.NORTH, 7500, true));
        assertEquals(0, input.battery.getUniversalEnergyStored());
        assertEquals(7500, ExternalEnergy.offer(null, input, EnumFacing.NORTH, 7500, false));
        assertEquals(2500, ExternalEnergy.offer(null, input, EnumFacing.SOUTH, 7500, false));
        assertEquals(10000, input.battery.getUniversalEnergyStored());
        assertEquals(0, ExternalEnergy.offer(null, input, EnumFacing.UP, 1, false));
        input.battery.extractEnergy(500, false);
        assertEquals(500, ExternalEnergy.offer(null, input, EnumFacing.DOWN, 1000, false));
    }
}
