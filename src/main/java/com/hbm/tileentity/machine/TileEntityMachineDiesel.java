package com.hbm.tileentity.machine;

import java.util.HashMap;

import com.hbm.forgefluid.FFUtils;
import com.hbm.lib.HBMSoundHandler;
import com.hbm.lib.Library;
import com.hbm.main.MainRegistry;
import com.hbm.forgefluid.ModForgeFluids;
import com.hbm.inventory.EngineRecipes;
import com.hbm.inventory.EngineRecipes.FuelGrade;
import com.hbm.sound.AudioWrapper;
import com.hbm.tileentity.IBufPacketReceiver;
import com.hbm.tileentity.TileEntityMachineBase;

import api.hbm.energy.IEnergyGenerator;
import io.netty.buffer.ByteBuf;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ITickable;
import net.minecraft.util.SoundCategory;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidTank;
import net.minecraftforge.fluids.FluidUtil;
import net.minecraftforge.fluids.capability.CapabilityFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.IFluidTankProperties;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.items.CapabilityItemHandler;

public class TileEntityMachineDiesel extends TileEntityMachineBase implements ITickable, IEnergyGenerator, IFluidHandler, IBufPacketReceiver {

	public long power;
	public static final long maxPower = 50000;
	public long powerCap = 50000;
	public int age = 0;
	public FluidTank tank;
	public Fluid tankType;
	public boolean needsUpdate;

	@SideOnly(Side.CLIENT)
	private AudioWrapper audio;

	private static final int[] slots_top = new int[] { 0 };
	private static final int[] slots_bottom = new int[] { 1, 2 };
	private static final int[] slots_side = new int[] { 2 };

	public static HashMap<FuelGrade, Double> fuelEfficiency = new HashMap();
	static {
		fuelEfficiency.put(FuelGrade.MEDIUM,	0.75D);
		fuelEfficiency.put(FuelGrade.HIGH,		1.0D);
		fuelEfficiency.put(FuelGrade.AERO,		0.25D);
	}

	public TileEntityMachineDiesel() {
		super(3);
		tank = new FluidTank(16000);
	}

	@Override
	public String getName() {
		return "container.machineDiesel";
	}

	@Override
	public NBTTagCompound writeToNBT(NBTTagCompound compound) {
		compound.setLong("powerTime", power);
		compound.setLong("powerCap", powerCap);
		tank.writeToNBT(compound);
		return super.writeToNBT(compound);
	}

	@Override
	public void readFromNBT(NBTTagCompound compound) {
		this.power = compound.getLong("powerTime");
		this.powerCap = compound.getLong("powerCap");
		tank.readFromNBT(compound);
		super.readFromNBT(compound);
	}

	@Override
	public int[] getAccessibleSlotsFromSide(EnumFacing e) {
		int p_94128_1_ = e.ordinal();
		return p_94128_1_ == 0 ? slots_bottom : (p_94128_1_ == 1 ? slots_top : slots_side);
	}

	public long getPowerScaled(long i) {
		return (power * i) / powerCap;
	}

	@Override
	public void update() {
		if(tank.getFluid() != null)
			tankType = tank.getFluid().getFluid();
		if (!world.isRemote) {
			if (needsUpdate) {
				needsUpdate = false;
			}
			this.sendPower(world, pos);

			//Tank Management
			if(this.inputValidForTank(-1, 0))
				if(FFUtils.fillFromFluidContainer(inventory, tank, 0, 1))
					needsUpdate = true;

			Fluid type = tank.getFluid() == null ? null : tank.getFluid().getFluid();
			if(type != null && type == ModForgeFluids.nitan)
				powerCap = maxPower * 10;
			else if(type != null && type == ModForgeFluids.sparkfuel)
				powerCap = maxPower * 20;
			else
				powerCap = maxPower;

			// Battery Item
			power = Library.chargeItemsFromTE(inventory, 2, power, powerCap);

			generate();

			networkPackNT(50);
		} else {
			boolean isGenerating = hasAcceptableFuel() && tank.getFluidAmount() > 0;
			float volume = this.getVolume(2);

			if(isGenerating && volume > 0) {
				if(audio == null) {
					audio = MainRegistry.proxy.getLoopedSound(HBMSoundHandler.diesel_operate, SoundCategory.BLOCKS, pos.getX(), pos.getY(), pos.getZ(), volume, 1.0F);
					audio.startSound();
				}
			} else {
				if(audio != null) {
					audio.stopSound();
					audio = null;
				}
			}
		}
	}

	@Override
	public void serialize(ByteBuf buf) {
		buf.writeLong(this.power);
		buf.writeLong(this.powerCap);
		ByteBufUtils.writeTag(buf, tank.writeToNBT(new NBTTagCompound()));
	}

	@Override
	public void deserialize(ByteBuf buf) {
		this.power = buf.readLong();
		this.powerCap = buf.readLong();
		tank.readFromNBT(ByteBufUtils.readTag(buf));
		if(tank.getFluid() != null)
			tankType = tank.getFluid().getFluid();
	}

	public boolean hasAcceptableFuel() {
		return getHEFromFuel() > 0;
	}

	public long getHEFromFuel() {
		if(tank.getFluid() == null) return 0;
		return getHEFromFuel(tank.getFluid().getFluid());
	}

	public static long getHEFromFuel(Fluid type) {
		if(EngineRecipes.hasFuelRecipe(type)) {
			FuelGrade grade = EngineRecipes.getFuelGrade(type);
			double efficiency = fuelEfficiency.containsKey(grade) ? fuelEfficiency.get(grade) : 0;
			return (long) (EngineRecipes.getEnergy(type) / 1000L * efficiency);
		}

		return 0;
	}

	public void generate() {
		if (hasAcceptableFuel()) {
			if (tank.getFluidAmount() > 0) {
				tank.drain(1, true);
				needsUpdate = true;
				if (power + getHEFromFuel() <= powerCap) {
					power += getHEFromFuel();
				} else {
					power = powerCap;
				}
			}
		}
	}

	protected boolean inputValidForTank(int tank, int slot){
		if(!inventory.getStackInSlot(slot).isEmpty()){
			if(isValidFluid(FluidUtil.getFluidContained(inventory.getStackInSlot(slot)))){
				return true;
			}
		}
		return false;
	}

	private boolean isValidFluid(FluidStack stack) {
		if(stack == null)
			return false;
		return getHEFromFuel(stack.getFluid()) > 0;
	}

	@Override
	public IFluidTankProperties[] getTankProperties() {
		return new IFluidTankProperties[]{tank.getTankProperties()[0]};
	}

	@Override
	public int fill(FluidStack resource, boolean doFill) {
		if (isValidFluid(resource)) {
			return tank.fill(resource, doFill);
		}
		return 0;
	}

	@Override
	public FluidStack drain(FluidStack resource, boolean doDrain) {
		return null;
	}

	@Override
	public FluidStack drain(int maxDrain, boolean doDrain) {
		return null;
	}

	@Override
	public boolean hasCapability(Capability<?> capability, EnumFacing facing) {
		if(capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY){
			return true;
		} else if(capability == CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY){
			return true;
		} else {
			return super.hasCapability(capability, facing);
		}
	}

	@Override
	public <T> T getCapability(Capability<T> capability, EnumFacing facing) {
		if(capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY){
			return CapabilityItemHandler.ITEM_HANDLER_CAPABILITY.cast(inventory);
		} else if(capability == CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY){
			return CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY.cast(this);
		} else {
			return super.getCapability(capability, facing);
		}
	}

	@Override
	public void onChunkUnload() {
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
}