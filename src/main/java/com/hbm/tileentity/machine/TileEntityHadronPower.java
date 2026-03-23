package com.hbm.tileentity.machine;

import com.hbm.tileentity.IBufPacketReceiver;
import com.hbm.tileentity.TileEntityTickingBase;

import api.hbm.energy.IEnergyUser;
import io.netty.buffer.ByteBuf;
import net.minecraft.nbt.NBTTagCompound;

public class TileEntityHadronPower extends TileEntityTickingBase implements IEnergyUser, IBufPacketReceiver {

	public long power;
	public static final long maxPower = 1000000000;

	@Override
	public void update() {
		if(!world.isRemote) {
			this.updateStandardConnections(world, pos);
			networkPackNT(15);
		}
	}

	@Override
	public String getInventoryName(){
		return "Hadron Power Thing";
	}

	@Override
	public void serialize(ByteBuf buf) {
		buf.writeLong(power);
	}

	@Override
	public void deserialize(ByteBuf buf) {
		this.power = buf.readLong();
	}
	
	@Override
	public void setPower(long i) {
		power = i;
		this.markDirty();
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
	public NBTTagCompound writeToNBT(NBTTagCompound compound){
		compound.setLong("power", power);
		return super.writeToNBT(compound);
	}
	
	@Override
	public void readFromNBT(NBTTagCompound compound){
		power = compound.getLong("power");
		super.readFromNBT(compound);
	}

}