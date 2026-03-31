package com.hbm.tileentity.machine.rbmk;

import java.util.Map;

import com.hbm.inventory.control_panel.DataValue;
import com.hbm.inventory.control_panel.DataValueFloat;
import com.hbm.entity.projectile.EntityRBMKDebris.DebrisType;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.SPacketUpdateTileEntity;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public abstract class TileEntityRBMKControl extends TileEntityRBMKSlottedBase {

	@SideOnly(Side.CLIENT)
	public double lastLevel;
	public double level;
	public static final double speed = 0.00277D;
	public double levelChange = 0.00277D;
	public double targetLevel;

	public byte timer = 0;
	public static final byte gamerulePollTime = 100;

	private double lastSyncedLevel = -1;

	public TileEntityRBMKControl() {
		super(0);
	}

	@Override
	public boolean isLidRemovable() {
		return false;
	}

	@Override
	public void update() {

		if(world.isRemote) {
			this.lastLevel = this.level;
		} else {
			if(timer < gamerulePollTime) {
				timer++;
			} else {
				timer = 0;
				levelChange = speed * RBMKDials.getControlSpeed(world);
			}

			if(level < targetLevel) {
				level += levelChange;

				if(level > targetLevel)
					level = targetLevel;
			} else if(level > targetLevel) {
				level -= levelChange;

				if(level < targetLevel)
					level = targetLevel;
			}

			if(Math.abs(level - lastSyncedLevel) > 0.001D) {
				lastSyncedLevel = level;
				markDirty();
				world.notifyBlockUpdate(pos, world.getBlockState(pos), world.getBlockState(pos), 3);
			}
		}

		super.update();
	}

	public void setTarget(double target) {
		this.targetLevel = target;

		if(!world.isRemote) {
			markDirty();
			world.notifyBlockUpdate(pos, world.getBlockState(pos), world.getBlockState(pos), 3);
		}
	}

	public double getMult() {
		return this.level;
	}

	@Override
	public int trackingRange() {
		return 150;
	}


	@Override
	public SPacketUpdateTileEntity getUpdatePacket() {
		return new SPacketUpdateTileEntity(this.pos, 0, this.getUpdateTag());
	}

	@Override
	public void onDataPacket(NetworkManager net, SPacketUpdateTileEntity pkt) {
		this.readFromNBT(pkt.getNbtCompound());
	}

	@Override
	public NBTTagCompound getUpdateTag() {
		return this.writeToNBT(new NBTTagCompound());
	}

	@Override
	public void handleUpdateTag(NBTTagCompound tag) {
		this.readFromNBT(tag);
	}


	@Override
	public void readFromNBT(NBTTagCompound nbt) {
		super.readFromNBT(nbt);

		this.level = nbt.getDouble("level");
		this.targetLevel = nbt.getDouble("targetLevel");
	}

	@Override
	public NBTTagCompound writeToNBT(NBTTagCompound nbt) {
		super.writeToNBT(nbt);

		nbt.setDouble("level", this.level);
		nbt.setDouble("targetLevel", this.targetLevel);
		return nbt;
	}

	@Override
	@SideOnly(Side.CLIENT)
	public double getMaxRenderDistanceSquared() {
		return 65536.0D;
	}

	@Override
	public void onMelt(int reduce) {

		int count = 2 + world.rand.nextInt(2);

		for(int i = 0; i < count; i++) {
			spawnDebris(DebrisType.ROD);
		}

		this.standardMelt(reduce);
	}

	@Override
	public NBTTagCompound getNBTForConsole() {
		NBTTagCompound data = new NBTTagCompound();
		data.setDouble("level", this.level);
		return data;
	}

	@Override
	public Map<String, DataValue> getQueryData() {
		Map<String, DataValue> data = super.getQueryData();
		data.put("level", new DataValueFloat((float) this.level * 100));
		return data;
	}
}