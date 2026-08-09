package com.hbm.tileentity.machine;

import java.util.ArrayList;
import java.util.List;

import com.hbm.blocks.ModBlocks;
import com.hbm.config.WeaponConfig;
import com.hbm.lib.HBMSoundHandler;
import com.hbm.lib.ForgeDirection;
import com.hbm.tileentity.IBufPacketReceiver;
import com.hbm.tileentity.TileEntityTickingBase;
import com.hbm.capability.HbmLivingProps;

import api.hbm.energy.IEnergyUser;
import api.hbm.entity.IRadarDetectable;
import api.hbm.entity.IRadarDetectable.RadarTargetType;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ITickable;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;


public class TileEntityMachineRadar extends TileEntityTickingBase implements ITickable, IEnergyUser, IBufPacketReceiver {

	public List<Entity> entList = new ArrayList();
	public volatile List<int[]> nearbyMissiles = new ArrayList<int[]>();
	public int pingTimer = 0;
	public int lastPower;
	final static int maxTimer = 40;

	public boolean scanMissiles = true;
	public boolean scanPlayers = false;
	public boolean smartMode = true;
	public boolean redMode = true;

	public boolean jammed = false;

	public float prevRotation;
	public float rotation;

	public long power = 0;
	public static final int maxPower = 100000;
	private int scanTick;
	
	
	@Override
	public String getInventoryName() {
		return "";
	}
	
	@Override
	public void update() {
		if(pos.getY() < WeaponConfig.radarAltitude)
			return;
		
		if(!world.isRemote) {

			this.updateConnectionsExcept(world, pos, ForgeDirection.UP);
			
			if(power > 0) {
				power -= 500;

				if(power < 0)
					power = 0;

				scanTick++;
				if(scanTick >= 5) {
					scanTick = 0;
					allocateMissiles();
				}
			} else {
				scanTick = 0;
				nearbyMissiles.clear();
				entList.clear();
				jammed = false;
			}
			
			if(lastPower != getRedPower())
				world.notifyNeighborsOfStateChange(pos, getBlockType(), true);

			lastPower = getRedPower();
			
			if(world.getBlockState(pos.down()).getBlock() != ModBlocks.muffler) {

				pingTimer++;

				if(power > 0 && pingTimer >= maxTimer) {
					this.world.playSound(null, pos.getX(), pos.getY(), pos.getZ(), HBMSoundHandler.sonarPing, SoundCategory.BLOCKS, 1.0F, 1.0F);
					pingTimer = 0;
				}
			}
			
			networkPackNT(15);
		} else {

			prevRotation = rotation;
			
			if(power > 0) {
				rotation += 5F;
			}
			
			if(rotation >= 360) {
				rotation -= 360F;
				prevRotation -= 360F;
			}
		}
	}


	public void handleButtonPacket(int value, int meta) {
		
		switch(meta) {
		case 0: this.scanMissiles = !this.scanMissiles; break;
		case 1: this.scanPlayers = !this.scanPlayers; break;
		case 2: this.smartMode = !this.smartMode; break;
		case 3: this.redMode = !this.redMode; break;
		}
	}

	public boolean isEntityApproaching(Entity e){
		boolean xAxisApproaching = (pos.getX() < e.posX  && e.motionX < 0) || (pos.getX() > e.posX  && e.motionX > 0);
		boolean zAxisApproaching = (pos.getZ() < e.posZ && e.motionZ < 0) || (pos.getZ() > e.posZ && e.motionZ > 0);
		return xAxisApproaching && zAxisApproaching;
	}
	
	private void allocateMissiles() {
		
		nearbyMissiles.clear();
		entList.clear();
		jammed = false;
		
		List<Entity> list = world.getEntitiesWithinAABBExcludingEntity(null, new AxisAlignedBB(pos.getX() + 0.5 - WeaponConfig.radarRange, 0D, pos.getZ() + 0.5 - WeaponConfig.radarRange, pos.getX() + 0.5 + WeaponConfig.radarRange, 10000, pos.getZ() + 0.5 + WeaponConfig.radarRange));

		for(Entity e : list) {
			
			if(e.posY < pos.getY() + WeaponConfig.radarBuffer)
				continue;
			
			if(e instanceof EntityLivingBase && HbmLivingProps.getDigamma((EntityLivingBase) e) > 0.001) {
				this.jammed = true;
				nearbyMissiles.clear();
				entList.clear();
				return;
			}

			if(e instanceof EntityPlayer && this.scanPlayers) {
				nearbyMissiles.add(new int[] { (int)e.posX, (int)e.posZ, RadarTargetType.PLAYER.ordinal(), (int)e.posY });
				entList.add(e);
			}
			
			if(e instanceof IRadarDetectable && this.scanMissiles) {
				nearbyMissiles.add(new int[] { (int)e.posX, (int)e.posZ, ((IRadarDetectable)e).getTargetType().ordinal(), (int)e.posY });
				
				if(this.smartMode){
					if(e.motionY <= 0 && isEntityApproaching(e)){
						entList.add(e);
					}
				}
				else{
					entList.add(e);
				}
			}
		}
	}
	
	public int getRedPower() {
		
		if(!entList.isEmpty()) {
			
			if(redMode) {
				
				double maxRange = WeaponConfig.radarRange * Math.sqrt(2D);
				
				int power = 0;
				
				for(int i = 0; i < entList.size(); i++) {
					
					Entity e = entList.get(i);
					double dist = Math.sqrt(Math.pow(e.posX - pos.getX(), 2) + Math.pow(e.posZ - pos.getZ(), 2));
					int p = 15 - (int)Math.floor(dist / maxRange * 15);
					
					if(p > power)
						power = p;
				}
				
				return power;
				
			} else {
				
				int power = 0;
				
				for(int i = 0; i < nearbyMissiles.size(); i++) {
					
					if(nearbyMissiles.get(i)[3] + 1 > power) {
						power = nearbyMissiles.get(i)[3] + 1;
					}
				}
				
				return power;
			}
		}
		
		return 0;
	}

	@Override
	public void serialize(ByteBuf buf) {
		buf.writeLong(this.power);
		buf.writeBoolean(this.scanMissiles);
		buf.writeBoolean(this.scanPlayers);
		buf.writeBoolean(this.smartMode);
		buf.writeBoolean(this.redMode);
		buf.writeBoolean(this.jammed);
		buf.writeInt(this.nearbyMissiles.size());
		for(int[] missile : this.nearbyMissiles) {
			buf.writeInt(missile[0]);
			buf.writeInt(missile[1]);
			buf.writeInt(missile[2]);
			buf.writeInt(missile[3]);
		}
	}

	@Override
	public void deserialize(ByteBuf buf) {
		this.power = buf.readLong();
		this.scanMissiles = buf.readBoolean();
		this.scanPlayers = buf.readBoolean();
		this.smartMode = buf.readBoolean();
		this.redMode = buf.readBoolean();
		this.jammed = buf.readBoolean();
		int count = buf.readInt();
		List<int[]> newList = new ArrayList<int[]>();
		for(int i = 0; i < count; i++) {
			int x = buf.readInt();
			int z = buf.readInt();
			int type = buf.readInt();
			int y = buf.readInt();
			newList.add(new int[] {x, z, type, y});
		}
		this.nearbyMissiles = newList;
	}

	@Override
	public void readFromNBT(NBTTagCompound compound) {
		power = compound.getLong("power");
		scanMissiles = compound.getBoolean("scanMissiles");
		scanPlayers = compound.getBoolean("scanPlayers");
		smartMode = compound.getBoolean("smartMode");
		redMode = compound.getBoolean("redMode");
		super.readFromNBT(compound);
	}
	
	@Override
	public NBTTagCompound writeToNBT(NBTTagCompound compound) {
		compound.setLong("power", power);
		compound.setBoolean("scanMissiles", scanMissiles);
		compound.setBoolean("scanPlayers", scanPlayers);
		compound.setBoolean("smartMode", smartMode);
		compound.setBoolean("redMode", redMode);
		return super.writeToNBT(compound);
	}
	
	public long getPowerScaled(long i) {
		return (power * i) / maxPower;
	}

	@Override
	public void setPower(long i) {
		if(power != i)
			markDirty();
		power = i;
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
	public AxisAlignedBB getRenderBoundingBox() {
		return TileEntity.INFINITE_EXTENT_AABB;
	}
	
	@Override
	@SideOnly(Side.CLIENT)
	public double getMaxRenderDistanceSquared()
	{
		return 65536.0D;
	}

}
