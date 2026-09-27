package com.hbm.tileentity.machine;

import com.hbm.lib.ForgeDirection;
import com.hbm.tileentity.TileEntityLoadedBase;

import api.hbm.energy.IEnergyUser;
import io.netty.buffer.ByteBuf;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ITickable;
import net.minecraft.util.math.BlockPos;

/** Backing pump for the arc welder: pulls the chamber down to a rough vacuum, nothing more. */
public class TileEntityMachineVacuumPump extends TileEntityLoadedBase implements ITickable, IEnergyUser {

	public long power;
	public boolean isOn;

	public TileEntityMachineVacuumPump() { }

	public long getConsumption() {
		return 1_000;
	}

	/** thousandths of a percent per tick, so 50 is the 1%/s of the fore pump */
	public int getPumpRate() {
		return 50;
	}

	/** this pump stops once the chamber is this far down */
	public int getCeiling() {
		return 50_000;
	}

	/** the cryo stage only starts where the fore pump gives up */
	public int getFloor() {
		return 0;
	}

	@Override
	public long getMaxPower() {
		return 200_000;
	}

	@Override
	public void update() {

		if(world.isRemote)
			return;

		if(world.getTotalWorldTime() % 20 == 0)
			for(ForgeDirection dir : ForgeDirection.VALID_DIRECTIONS)
				this.trySubscribe(world, pos.add(dir.offsetX, dir.offsetY, dir.offsetZ), dir);

		this.isOn = false;

		if(power >= getConsumption() && hasExtraResources()) {

			TileEntityMachineArcWelder welder = this.getWelder();

			if(welder != null && welder.vacuum >= getFloor() && welder.addVacuum(getPumpRate(), getCeiling()) > 0) {
				power -= getConsumption();
				this.useExtraResources();
				this.isOn = true;
			}
		}

		this.networkPackNT(50);
	}

	/** the cryo pump burns nitrogen on top of the power */
	protected boolean hasExtraResources() {
		return true;
	}

	protected void useExtraResources() { }

	/** any welder block touching the pump counts, so it can sit against any of the ports */
	public TileEntityMachineArcWelder getWelder() {

		for(ForgeDirection dir : ForgeDirection.VALID_DIRECTIONS) {
			BlockPos side = pos.add(dir.offsetX, dir.offsetY, dir.offsetZ);
			TileEntity te = world.getTileEntity(side);

			if(te instanceof TileEntityMachineArcWelder)
				return (TileEntityMachineArcWelder) te;

			if(te instanceof com.hbm.tileentity.TileEntityProxyBase) {
				TileEntity core = ((com.hbm.tileentity.TileEntityProxyBase) te).getTE();
				if(core instanceof TileEntityMachineArcWelder)
					return (TileEntityMachineArcWelder) core;
			}
		}

		return null;
	}

	@Override
	public void serialize(ByteBuf buf) {
		super.serialize(buf);
		buf.writeLong(power);
		buf.writeBoolean(isOn);
	}

	@Override
	public void deserialize(ByteBuf buf) {
		super.deserialize(buf);
		this.power = buf.readLong();
		this.isOn = buf.readBoolean();
	}

	@Override
	public void readFromNBT(NBTTagCompound nbt) {
		super.readFromNBT(nbt);
		this.power = nbt.getLong("power");
	}

	@Override
	public NBTTagCompound writeToNBT(NBTTagCompound nbt) {
		super.writeToNBT(nbt);
		nbt.setLong("power", power);
		return nbt;
	}

	@Override
	public long getPower() {
		return power;
	}

	@Override
	public void setPower(long i) {
		power = i;
	}
}
