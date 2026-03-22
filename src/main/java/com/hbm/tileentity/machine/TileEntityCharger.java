package com.hbm.tileentity.machine;

import api.hbm.energy.IBatteryItem;
import api.hbm.energy.IEnergyUser;
import com.hbm.blocks.machine.MachineCharger;
import com.hbm.main.MainRegistry;
import com.hbm.tileentity.IBufPacketReceiver;
import com.hbm.tileentity.TileEntityLoadedBase;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ITickable;
import net.minecraft.util.math.AxisAlignedBB;

import java.util.ArrayList;
import java.util.List;

public class TileEntityCharger extends TileEntityLoadedBase implements ITickable, IEnergyUser, IBufPacketReceiver {
	
	public static final int range = 3;

	private List<EntityPlayer> players = new ArrayList();
	private long maxChargeRate;
	public long charge = 0;
	public long actualCharge = 0;
	public long totalCapacity = 0;
	public long totalEnergy = 0;
	private int lastOp = 0;

	public boolean isOn = false;
	public boolean pointingUp = true;

	@Override
	public void update() {
		
		if(!world.isRemote) {
			MachineCharger c = (MachineCharger)world.getBlockState(pos).getBlock();
			this.maxChargeRate = c.maxThroughput;
			this.pointingUp = c.pointingUp;

			this.updateStandardConnections(world, pos);
			
			players = world.getEntitiesWithinAABB(EntityPlayer.class, new AxisAlignedBB(pos.getX(), pos.getY(), pos.getZ(), pos.getX() + 1, pos.getY() + (pointingUp ? range : -range), pos.getZ() + 1));
			
			totalCapacity = 0;
			totalEnergy = 0;
			charge = 0;
			
			for(EntityPlayer player : players) {
				InventoryPlayer inv = player.inventory;
				for(int i = 0; i < inv.getSizeInventory(); i ++){
					
					ItemStack stack = inv.getStackInSlot(i);
					if(stack != null && stack.getItem() instanceof IBatteryItem) {
						IBatteryItem battery = (IBatteryItem) stack.getItem();
						totalCapacity += battery.getMaxCharge(stack);
						totalEnergy += battery.getCharge(stack);
						charge += Math.max(0, Math.min(battery.getMaxCharge(stack) - battery.getCharge(stack), battery.getChargeRate()));
					}
				}
			}
			
			isOn = lastOp > 0;
			
			if(isOn) {
				lastOp--;
			}

            networkPackNT(50);
            actualCharge = 0;
		}
	}


    @Override
    public void serialize(ByteBuf buf) {
        buf.writeBoolean(this.isOn);
        buf.writeBoolean(this.pointingUp);
        buf.writeLong(this.totalCapacity);
        buf.writeLong(this.totalEnergy);
        buf.writeLong(this.charge);
        buf.writeLong(this.actualCharge);
    }

    @Override
    public void deserialize(ByteBuf buf) {
        this.isOn = buf.readBoolean();
        this.pointingUp = buf.readBoolean();
        this.totalCapacity = buf.readLong();
        this.totalEnergy = buf.readLong();
        this.charge = buf.readLong();
        this.actualCharge = buf.readLong();
    }

	@Override
	public long getPower() {
		return 0;
	}

	@Override
	public long getMaxPower() {
		return Math.min(charge, maxChargeRate);
	}

	@Override
	public void setPower(long power) { }
	
	@Override
	public long transferPower(long power) {
		
		if(power == 0)
			return power;
		
		actualCharge = 0;
		long chargeBudget = maxChargeRate;
		for(EntityPlayer player : players) {
			InventoryPlayer inv = player.inventory;
			for(int i = 0; i < inv.getSizeInventory(); i ++){

				if(chargeBudget > 0 && power > 0){
					ItemStack stack = inv.getStackInSlot(i);
					
					if(stack != null && stack.getItem() instanceof IBatteryItem) {
						IBatteryItem battery = (IBatteryItem) stack.getItem();
						
						long toCharge = Math.max(0, Math.min(battery.getMaxCharge(stack) - battery.getCharge(stack), battery.getChargeRate()));
						toCharge = Math.min(toCharge, chargeBudget);
						toCharge = Math.min(toCharge, power);
						if(toCharge > 0) {
							battery.chargeBattery(stack, toCharge);
							power -= toCharge;
							actualCharge += toCharge;
							chargeBudget -= toCharge;
							lastOp = 4;
						}
					}
				}
			}
		}
		
		return power;
	}
}
