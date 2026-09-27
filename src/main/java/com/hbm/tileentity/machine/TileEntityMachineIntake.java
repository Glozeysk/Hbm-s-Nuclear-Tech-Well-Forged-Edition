package com.hbm.tileentity.machine;

import com.hbm.forgefluid.FFUtils;
import com.hbm.forgefluid.ModForgeFluids;
import com.hbm.lib.DirPos;
import com.hbm.lib.ForgeDirection;
import com.hbm.lib.HBMSoundHandler;
import com.hbm.main.MainRegistry;
import com.hbm.sound.AudioWrapper;
import com.hbm.tileentity.TileEntityLoadedBase;

import api.hbm.energy.IEnergyUser;
import io.netty.buffer.ByteBuf;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ITickable;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidTank;
import net.minecraftforge.fluids.capability.CapabilityFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.IFluidTankProperties;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

//Ported from NTM:CE (TileEntityMachineIntake)
public class TileEntityMachineIntake extends TileEntityLoadedBase implements ITickable, IEnergyUser, IFluidHandler {

	public static final long maxPower = 10_000;
	public static final long consumption = maxPower / 20;

	public FluidTank compair;
	public long power;
	public boolean isOn;
	public float fan = 0;
	public float prevFan = 0;
	private AxisAlignedBB bb = null;
	private AudioWrapper audio;

	public TileEntityMachineIntake() {
		this.compair = new FluidTank(1_000);
	}

	@Override
	public void update() {

		if(!world.isRemote) {

			//CE drew power every tick even with a full tank; only pay for the air that was actually taken
			this.isOn = false;
			if(this.power >= consumption && compair.getFluidAmount() < compair.getCapacity()) {
				compair.setFluid(new FluidStack(ModForgeFluids.air, compair.getCapacity()));
				this.power -= consumption;
				this.isOn = true;
			}

			for(DirPos con : getConPos()) {
				if(compair.getFluidAmount() > 0)
					FFUtils.fillFluid(this, compair, world, con.getPos(), compair.getCapacity());
				this.trySubscribe(world, con.getPos(), con.getDir());
			}

			this.networkPackNT(50);

		} else {

			this.prevFan = this.fan;

			if(this.isOn) {
				this.fan += 45;

				if(this.fan >= 360) {
					this.fan -= 360;
					this.prevFan -= 360;
				}

				if(audio == null) {
					audio = MainRegistry.proxy.getLoopedSound(HBMSoundHandler.motor, SoundCategory.BLOCKS, pos.getX(), pos.getY(), pos.getZ(), 0.25F, 1.0F);
					audio.updateRange(10F);
					audio.startSound();
				}
				audio.updateVolume(this.getVolume(0.25F));

			} else if(audio != null) {
				audio.stopSound();
				audio = null;
			}
		}
	}

	public DirPos[] getConPos() {
		ForgeDirection dir = ForgeDirection.getOrientation(this.getBlockMetadata() - 10);
		ForgeDirection rot = dir.getRotation(ForgeDirection.UP);
		int x = pos.getX(), y = pos.getY(), z = pos.getZ();
		return new DirPos[] {
				new DirPos(x + dir.offsetX, y, z + dir.offsetZ, dir),
				new DirPos(x + dir.offsetX + rot.offsetX, y, z + dir.offsetZ + rot.offsetZ, dir),

				new DirPos(x - dir.offsetX * 2, y, z - dir.offsetZ * 2, dir.getOpposite()),
				new DirPos(x - dir.offsetX * 2 + rot.offsetX, y, z - dir.offsetZ * 2 + rot.offsetZ, dir.getOpposite()),

				new DirPos(x + rot.offsetX * 2, y, z + rot.offsetZ * 2, rot),
				new DirPos(x + rot.offsetX * 2 - dir.offsetX, y, z + rot.offsetZ * 2 - dir.offsetZ, rot),

				new DirPos(x - rot.offsetX, y, z - rot.offsetZ, rot.getOpposite()),
				new DirPos(x - rot.offsetX - dir.offsetX, y, z - rot.offsetZ - dir.offsetZ, rot.getOpposite())
		};
	}

	@Override
	public void onChunkUnload() {
		super.onChunkUnload();
		if(audio != null) {
			audio.stopSound();
			audio = null;
		}
	}

	@Override
	public void invalidate() {
		super.invalidate();
		if(audio != null) {
			audio.stopSound();
			audio = null;
		}
	}

	@Override
	public void serialize(ByteBuf buf) {
		super.serialize(buf);
		buf.writeBoolean(isOn);
		buf.writeLong(power);
		buf.writeInt(compair.getFluidAmount());
	}

	@Override
	public void deserialize(ByteBuf buf) {
		super.deserialize(buf);
		this.isOn = buf.readBoolean();
		this.power = buf.readLong();
		int amount = buf.readInt();
		compair.setFluid(amount > 0 ? new FluidStack(ModForgeFluids.air, amount) : null);
	}

	@Override
	public void readFromNBT(NBTTagCompound nbt) {
		super.readFromNBT(nbt);
		this.power = nbt.getLong("power");
		if(nbt.hasKey("compair"))
			compair.readFromNBT(nbt.getCompoundTag("compair"));
	}

	@Override
	public NBTTagCompound writeToNBT(NBTTagCompound nbt) {
		super.writeToNBT(nbt);
		nbt.setLong("power", power);
		nbt.setTag("compair", compair.writeToNBT(new NBTTagCompound()));
		return nbt;
	}

	@Override
	public boolean canConnect(ForgeDirection dir) {
		return dir != ForgeDirection.UP && dir != ForgeDirection.DOWN;
	}

	@Override
	public long getPower() {
		return power;
	}

	@Override
	public void setPower(long i) {
		power = i;
	}

	@Override
	public long getMaxPower() {
		return maxPower;
	}

	@Override
	public IFluidTankProperties[] getTankProperties() {
		return compair.getTankProperties();
	}

	@Override
	public int fill(FluidStack resource, boolean doFill) {
		return 0;
	}

	@Override
	public FluidStack drain(FluidStack resource, boolean doDrain) {
		if(resource == null || resource.getFluid() != ModForgeFluids.air)
			return null;
		return compair.drain(resource.amount, doDrain);
	}

	@Override
	public FluidStack drain(int maxDrain, boolean doDrain) {
		return compair.drain(maxDrain, doDrain);
	}

	@Override
	public boolean hasCapability(Capability<?> capability, EnumFacing facing) {
		if(capability == CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY)
			return true;
		return super.hasCapability(capability, facing);
	}

	@Override
	public <T> T getCapability(Capability<T> capability, EnumFacing facing) {
		if(capability == CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY)
			return CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY.cast(this);
		return super.getCapability(capability, facing);
	}

	@Override
	public AxisAlignedBB getRenderBoundingBox() {

		if(bb == null)
			bb = new AxisAlignedBB(pos.getX() - 1, pos.getY(), pos.getZ() - 1, pos.getX() + 2, pos.getY() + 1, pos.getZ() + 2);

		return bb;
	}

	@Override
	@SideOnly(Side.CLIENT)
	public double getMaxRenderDistanceSquared() {
		return 65536.0D;
	}
}
