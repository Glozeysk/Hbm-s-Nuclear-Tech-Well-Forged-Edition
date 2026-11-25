package com.hbm.tileentity.network.energy;

import com.hbm.lib.ForgeDirection;
import com.hbm.tileentity.TileEntityLoadedBase;

import api.hbm.energy.IEnergyConductor;
import api.hbm.energy.IPowerNet;
import api.hbm.energy.PowerNet;
import net.minecraft.util.ITickable;
import net.minecraft.tileentity.TileEntity;

public class TileEntityCableBaseNT extends TileEntityLoadedBase implements ITickable, IEnergyConductor {
	
	protected IPowerNet network;
    public boolean importingExternal;
    private long inputFE;
    private IPowerNet externalNetwork;
    private final com.hbm.compat.energy.ExternalEnergy.Receiver[] externalReceivers = new com.hbm.compat.energy.ExternalEnergy.Receiver[6];
    public final net.minecraftforge.energy.IEnergyStorage externalInput = new net.minecraftforge.energy.IEnergyStorage() {
        public int receiveEnergy(int amount, boolean simulate) {
            if(amount <= 0 || network == null || !network.isValid()) return 0;
            int accepted = (int)Math.min(amount, Math.max(0, 1000000L - inputFE));
            if(!simulate && accepted > 0) { inputFE += accepted; markDirty(); }
            return accepted;
        }
        public int extractEnergy(int amount, boolean simulate) { return 0; }
        public int getEnergyStored() { return (int)inputFE; }
        public int getMaxEnergyStored() { return 1000000; }
        public boolean canExtract() { return false; }
        public boolean canReceive() { return true; }
    };

	@Override
	public void update() {
		
		if(!world.isRemote && canUpdate()) {
			
			//we got here either because the net doesn't exist or because it's not valid, so that's safe to assume
			this.setPowerNet(null);
			
			this.connect();
			
			if(this.getPowerNet() == null) {
				this.setPowerNet(new PowerNet().joinLink(this));
			}
		}
        updateExternalPorts();
	}
	
    private void updateExternalPorts() {
        if(world.isRemote || network == null || !network.isValid()) return;
        boolean refresh = externalNetwork != network || Math.floorMod(world.getTotalWorldTime() + pos.toLong(), 20) == 0;
        externalNetwork = network;
        for(ForgeDirection dir : ForgeDirection.VALID_DIRECTIONS) {
            int i = dir.ordinal();
            if(!refresh && externalReceivers[i] == null) continue;
            net.minecraft.util.math.BlockPos neighbor = pos.add(dir.offsetX, dir.offsetY, dir.offsetZ);
            TileEntity tile = world.isBlockLoaded(neighbor) ? world.getTileEntity(neighbor) : null;
            if(!canConnect(dir) || !com.hbm.compat.energy.ExternalEnergy.connects(tile, dir.getOpposite().toEnumFacing())) {
                if(externalReceivers[i] != null) network.unsubscribe(externalReceivers[i]);
                externalReceivers[i] = null;
                continue;
            }
            if(externalReceivers[i] == null) externalReceivers[i] = new com.hbm.compat.energy.ExternalEnergy.Receiver(this, dir);
            network.subscribe(externalReceivers[i]);
            com.hbm.compat.energy.ExternalEnergy.pull(this, tile, dir.getOpposite().toEnumFacing());
        }
        double rate = com.hbm.compat.energy.ExternalEnergy.fePerHe();
        long offer = (long)Math.floor(inputFE / rate);
        if(offer > 0) {
            importingExternal = true;
            try {
                long used = Math.max(0, Math.min(offer, offer - network.transferPower(offer)));
                if(used > 0) { inputFE -= Math.min(inputFE, (long)Math.ceil(used * rate)); markDirty(); }
            } finally { importingExternal = false; }
        }
    }
    @Override public boolean hasCapability(net.minecraftforge.common.capabilities.Capability<?> cap, net.minecraft.util.EnumFacing side) {
        return (cap == net.minecraftforge.energy.CapabilityEnergy.ENERGY && side != null && canConnect(ForgeDirection.getOrientation(side.ordinal()))) || super.hasCapability(cap, side);
    }
    @Override @SuppressWarnings("unchecked") public <T> T getCapability(net.minecraftforge.common.capabilities.Capability<T> cap, net.minecraft.util.EnumFacing side) {
        if(cap == net.minecraftforge.energy.CapabilityEnergy.ENERGY && side != null && canConnect(ForgeDirection.getOrientation(side.ordinal()))) return (T)externalInput;
        return super.getCapability(cap, side);
    }
    @Override public void readFromNBT(net.minecraft.nbt.NBTTagCompound tag) {
        super.readFromNBT(tag); inputFE = Math.max(0, Math.min(1000000, tag.getLong("externalInputFE")));
    }
    @Override public net.minecraft.nbt.NBTTagCompound writeToNBT(net.minecraft.nbt.NBTTagCompound tag) {
        tag.setLong("externalInputFE", inputFE); return super.writeToNBT(tag);
    }

	protected void connect() {
		
		for(ForgeDirection dir : ForgeDirection.VALID_DIRECTIONS) {
			
			TileEntity te = world.getTileEntity(pos.add(dir.offsetX, dir.offsetY, dir.offsetZ));
			
			if(te instanceof IEnergyConductor) {
				
				IEnergyConductor conductor = (IEnergyConductor) te;
				
				if(!conductor.canConnect(dir.getOpposite()))
					continue;
				
				if(this.getPowerNet() == null && conductor.getPowerNet() != null) {
					conductor.getPowerNet().joinLink(this);
				}
				
				if(this.getPowerNet() != null && conductor.getPowerNet() != null && this.getPowerNet() != conductor.getPowerNet()) {
					conductor.getPowerNet().joinNetworks(this.getPowerNet());
				}
			}
		}
	}

	@Override
	public void invalidate() {
		super.invalidate();
		
		if(!world.isRemote) {
			if(this.network != null) {
				this.network.reevaluate();
				this.network = null;
			}
		}
	}

	/**
	 * Only update until a power net is formed, in >99% of the cases it should be the first tick. Everything else is handled by neighbors and the net itself.
	 */
	public boolean canUpdate() {
		return (this.network == null || !this.network.isValid()) && !this.isInvalid();
	}

	@Override
	public boolean canConnect(ForgeDirection dir) {
		return dir != ForgeDirection.UNKNOWN;
	}

	@Override
	public long getPower() {
		return 0;
	}

	@Override
	public long getMaxPower() {
		return 0;
	}

	@Override
	public void setPowerNet(IPowerNet network) {
		this.network = network;
	}

	@Override
	public long transferPower(long power) {
		
		if(this.network == null)
			return power;
		
		return this.network.transferPower(power);
	}

	@Override
	public IPowerNet getPowerNet() {
		return this.network;
	}
}
