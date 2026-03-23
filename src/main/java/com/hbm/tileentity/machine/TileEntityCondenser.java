package com.hbm.tileentity.machine;

import com.hbm.forgefluid.FFUtils;
import com.hbm.forgefluid.ModForgeFluids;
import com.hbm.lib.ForgeDirection;
import com.hbm.tileentity.IBufPacketReceiver;
import com.hbm.tileentity.TileEntityLoadedBase;

import io.netty.buffer.ByteBuf;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ITickable;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidTank;
import net.minecraftforge.fluids.capability.CapabilityFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.IFluidTankProperties;

public class TileEntityCondenser extends TileEntityLoadedBase implements ITickable, IFluidHandler, IBufPacketReceiver {

	public int age = 0;
	public FluidTank[] tanks;
	
	public int waterTimer = 0;
	
	public TileEntityCondenser() {
		tanks = new FluidTank[2];
		//spentsteam
		tanks[0] = new FluidTank(100);
		//water
		tanks[1] = new FluidTank(100);
	}
	
	@Override
	public void update() {
		
		if(!world.isRemote) {
			
			age++;
			if(age >= 2) {
				age = 0;
			}

			if(this.waterTimer > 0)
				this.waterTimer--;

			int convert = Math.min(tanks[0].getFluidAmount(), tanks[1].getCapacity() - tanks[1].getFluidAmount());
			if(convert > 0)
				this.waterTimer = 20;

			tanks[0].drain(convert, true);
			tanks[1].fill(new FluidStack(FluidRegistry.WATER, convert), true);
			
			networkPackNT(150);
			fillFluidInit(tanks[1]);
		}
	}

	@Override
	public void serialize(ByteBuf buf) {
		buf.writeInt(tanks[0].getFluidAmount());
		buf.writeInt(tanks[1].getFluidAmount());
		buf.writeByte(waterTimer);
	}

	@Override
	public void deserialize(ByteBuf buf) {
		tanks[0].setFluid(new FluidStack(ModForgeFluids.spentsteam, buf.readInt()));
		tanks[1].setFluid(new FluidStack(FluidRegistry.WATER, buf.readInt()));
		this.waterTimer = buf.readByte();
	}
	
	@Override
	public void readFromNBT(NBTTagCompound nbt) {
		super.readFromNBT(nbt);
		tanks[0].readFromNBT(nbt.getCompoundTag("steam"));
		tanks[1].readFromNBT(nbt.getCompoundTag("water"));
	}
	
	@Override
	public NBTTagCompound writeToNBT(NBTTagCompound nbt) {
		super.writeToNBT(nbt);
		nbt.setTag("steam", tanks[0].writeToNBT(new NBTTagCompound()));
		nbt.setTag("water", tanks[1].writeToNBT(new NBTTagCompound()));
		return nbt;
	}

	public void fillFluidInit(FluidTank tank) {
		
		for(ForgeDirection dir : ForgeDirection.VALID_DIRECTIONS)
			fillFluid(pos.getX() + dir.offsetX, pos.getY() + dir.offsetY, pos.getZ() + dir.offsetZ, tank);
	}

	public void fillFluid(int x, int y, int z, FluidTank type) {
		FFUtils.fillFluid(this, type, world, new BlockPos(x, y, z), type.getCapacity());
	}

	@Override
	public IFluidTankProperties[] getTankProperties(){
		return new IFluidTankProperties[]{tanks[0].getTankProperties()[0], tanks[1].getTankProperties()[0]};
	}

	@Override
	public int fill(FluidStack resource, boolean doFill){
		if(resource != null && resource.getFluid() == ModForgeFluids.spentsteam){
			return tanks[0].fill(resource, doFill);
		}
		return 0;
	}

	@Override
	public FluidStack drain(FluidStack resource, boolean doDrain){
		if(resource != null && resource.getFluid() == FluidRegistry.WATER){
			return tanks[1].drain(resource, doDrain);
		}
		return null;
	}

	@Override
	public FluidStack drain(int maxDrain, boolean doDrain){
		return tanks[1].drain(maxDrain, doDrain);
	}
	
	@Override
	public <T> T getCapability(Capability<T> capability, EnumFacing facing){
		if(capability == CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY){
			return CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY.cast(this);
		}
		return super.getCapability(capability, facing);
	}
	
	@Override
	public boolean hasCapability(Capability<?> capability, EnumFacing facing){
		return capability == CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY || super.hasCapability(capability, facing);
	}
}