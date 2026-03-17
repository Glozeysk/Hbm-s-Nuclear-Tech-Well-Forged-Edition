package com.hbm.tileentity.machine;

import com.hbm.blocks.BlockDummyable;
import com.hbm.entity.particle.EntityGasFlameFX;
import com.hbm.explosion.ExplosionThermo;
import com.hbm.forgefluid.ModForgeFluids;
import com.hbm.interfaces.ITankPacketAcceptor;
import com.hbm.inventory.DiFurnaceRecipes;
import com.hbm.lib.ForgeDirection;
import com.hbm.lib.HBMSoundHandler;
import com.hbm.packet.AuxElectricityPacket;
import com.hbm.packet.FluidTankPacket;
import com.hbm.packet.PacketDispatcher;
import com.hbm.tileentity.TileEntityMachineBase;

import api.hbm.energy.IEnergyUser;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ITickable;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidTank;
import net.minecraftforge.fluids.FluidUtil;
import net.minecraftforge.fluids.capability.CapabilityFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.IFluidTankProperties;
import net.minecraftforge.fml.common.network.NetworkRegistry.TargetPoint;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public class TileEntityMachineDiFurnaceBig extends TileEntityMachineBase implements ITickable, IEnergyUser, IFluidHandler, ITankPacketAcceptor {

	public long power = 0;
	public int process = 0;
	public static final long maxPower = 500000000;
	public FluidTank tank;
	public Fluid tankType = ModForgeFluids.nitan;
	public boolean needsUpdate = false;
	
	public TileEntityMachineDiFurnaceBig() {
		super(8);
		tank = new FluidTank(8000);
	}
	
	@Override
	public String getName(){
		return "container.machinedifurnacebig";
	}

	public int getProcessSpeed() {
		if(tank.getFluid() != null) {
			if(tank.getFluid().getFluid() == ModForgeFluids.nitan) return 20;
			if(tank.getFluid().getFluid() == ModForgeFluids.balefire) return 4;
			if(tank.getFluid().getFluid() == ModForgeFluids.sparkfuel) return 2;
			if(tank.getFluid().getFluid() == ModForgeFluids.uu_matter) return 1;
		}
		return 20;
	}
	
	@Override
	public int[] getAccessibleSlotsFromSide(EnumFacing face) {
		return new int[] { 0, 1, 2, 3, 4, 5, 6, 7 };
	}

	private void updateConnections() {

		ForgeDirection dir = ForgeDirection.getOrientation(this.getBlockMetadata() - BlockDummyable.offset);

		if(dir == ForgeDirection.NORTH || dir == ForgeDirection.SOUTH) {
			this.trySubscribe(world, pos.add(2, 1, 0), ForgeDirection.EAST);
			this.trySubscribe(world, pos.add(-2, 1, 0), ForgeDirection.WEST);
		} else if(dir == ForgeDirection.EAST || dir == ForgeDirection.WEST) {
			this.trySubscribe(world, pos.add(0, 1, 2), ForgeDirection.SOUTH);
			this.trySubscribe(world, pos.add(0, 1, -2), ForgeDirection.NORTH);
		}
	}
	
	@Override
	public boolean canInsertItem(int slot, ItemStack stack, int amount){
		if(!isItemValidForSlot(slot, stack))
			return false;
		if(slot == 0 || slot == 1) {
			int partner = slot == 0 ? 1 : 0;
			if(!inventory.getStackInSlot(partner).isEmpty() && inventory.getStackInSlot(partner).getItem() != stack.getItem())
				return false;
			if(!inventory.getStackInSlot(2).isEmpty() && inventory.getStackInSlot(2).getItem() == stack.getItem())
				return false;
			if(!inventory.getStackInSlot(3).isEmpty() && inventory.getStackInSlot(3).getItem() == stack.getItem())
				return false;
			return true;
		}
		if(slot == 2 || slot == 3) {
			int partner = slot == 2 ? 3 : 2;
			if(!inventory.getStackInSlot(partner).isEmpty() && inventory.getStackInSlot(partner).getItem() != stack.getItem())
				return false;
			if(!inventory.getStackInSlot(0).isEmpty() && inventory.getStackInSlot(0).getItem() == stack.getItem())
				return false;
			if(!inventory.getStackInSlot(1).isEmpty() && inventory.getStackInSlot(1).getItem() == stack.getItem())
				return false;
			return true;
		}
		return true;
	}
	
	@Override
	public boolean canExtractItem(int i, ItemStack itemStack, int amount){
		return i == 4 || i == 5 || i == 6 || i == 7;
	}
	
	@Override
	public boolean isItemValidForSlot(int i, ItemStack stack){
		if(i == 4 || i == 5 || i == 6 || i == 7) return false;
		return true;
	}
	
	public boolean isUseableByPlayer(EntityPlayer player) {
		if(world.getTileEntity(pos) != this)
		{
			return false;
		}else{
			return player.getDistanceSq(pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D) <=64;
		}
	}
	
	@Override
	public void readFromNBT(NBTTagCompound compound) {
		power = compound.getLong("power");
		tank.readFromNBT(compound);
		process = compound.getShort("process");
		super.readFromNBT(compound);
	}
	
	@Override
	public NBTTagCompound writeToNBT(NBTTagCompound compound) {
		compound.setLong("power", power);
		tank.writeToNBT(compound);
		compound.setShort("process", (short) process);
		return super.writeToNBT(compound);
	}
	
	public long getPowerScaled(long i) {
		return (power * i) / maxPower;
	}
	
	public int getProgressScaled(int i) {
		return (process * i) / getProcessSpeed();
	}

	public boolean canProcess() {
		if(tank.getFluidAmount() < 5)
			return false;
		if(power < 500000)
			return false;

		ItemStack result1 = null;
		ItemStack result2 = null;

		if(!inventory.getStackInSlot(0).isEmpty() && !inventory.getStackInSlot(2).isEmpty())
			result1 = DiFurnaceRecipes.getFurnaceProcessingResult(inventory.getStackInSlot(0), inventory.getStackInSlot(2));
		if(!inventory.getStackInSlot(1).isEmpty() && !inventory.getStackInSlot(3).isEmpty())
			result2 = DiFurnaceRecipes.getFurnaceProcessingResult(inventory.getStackInSlot(1), inventory.getStackInSlot(3));

		if((result1 == null || result1.isEmpty()) && (result2 == null || result2.isEmpty()))
			return false;

		int totalCount = 0;
		ItemStack resultItem = null;
		if(result1 != null && !result1.isEmpty()) {
			totalCount += result1.getCount();
			resultItem = result1;
		}
		if(result2 != null && !result2.isEmpty()) {
			totalCount += result2.getCount();
			if(resultItem == null) resultItem = result2;
		}

		int space = 0;
		for(int s : new int[]{4, 5, 6, 7}) {
			ItemStack slotStack = inventory.getStackInSlot(s);
			if(slotStack.isEmpty()) {
				space += resultItem.getMaxStackSize();
			} else if(slotStack.isItemEqual(resultItem)) {
				space += resultItem.getMaxStackSize() - slotStack.getCount();
			}
			if(space >= totalCount)
				break;
		}

		return space >= totalCount;
	}

	public boolean isProcessing() {
		return process > 0;
	}
	
	public void process() {
		tank.drain(5, true);
		needsUpdate = true;
		power -= 500000;
		process++;

		if(process >= getProcessSpeed()) {
			ItemStack result1 = null;
			ItemStack result2 = null;

			if(!inventory.getStackInSlot(0).isEmpty() && !inventory.getStackInSlot(2).isEmpty())
				result1 = DiFurnaceRecipes.getFurnaceProcessingResult(inventory.getStackInSlot(0), inventory.getStackInSlot(2));
			if(!inventory.getStackInSlot(1).isEmpty() && !inventory.getStackInSlot(3).isEmpty())
				result2 = DiFurnaceRecipes.getFurnaceProcessingResult(inventory.getStackInSlot(1), inventory.getStackInSlot(3));

			int totalCount = 0;
			ItemStack resultItem = null;
			if(result1 != null && !result1.isEmpty()) {
				totalCount += result1.getCount();
				resultItem = result1;
			}
			if(result2 != null && !result2.isEmpty()) {
				totalCount += result2.getCount();
				if(resultItem == null) resultItem = result2;
			}

			for(int s : new int[]{4, 5, 6, 7}) {
				if(totalCount <= 0)
					break;
				ItemStack slotStack = inventory.getStackInSlot(s);
				if(slotStack.isEmpty()) {
					ItemStack output = resultItem.copy();
					int count = Math.min(totalCount, resultItem.getMaxStackSize());
					output.setCount(count);
					inventory.setStackInSlot(s, output);
					totalCount -= count;
				} else if(slotStack.isItemEqual(resultItem)) {
					int count = Math.min(totalCount, resultItem.getMaxStackSize() - slotStack.getCount());
					slotStack.grow(count);
					totalCount -= count;
				}
			}

			if(result1 != null && !result1.isEmpty()) {
				for(int i : new int[]{0, 2}) {
					inventory.getStackInSlot(i).shrink(1);
					if(inventory.getStackInSlot(i).getCount() <= 0)
						inventory.setStackInSlot(i, ItemStack.EMPTY);
				}
			}

			if(result2 != null && !result2.isEmpty()) {
				for(int i : new int[]{1, 3}) {
					inventory.getStackInSlot(i).shrink(1);
					if(inventory.getStackInSlot(i).getCount() <= 0)
						inventory.setStackInSlot(i, ItemStack.EMPTY);
				}
			}

			process = 0;
		}
	}
	
	@Override
	public void update() {
		if (!world.isRemote) {

			this.updateConnections();

			long prevPower = power;
			int prevAmount = tank.getFluidAmount();
			if (needsUpdate) {
				needsUpdate = false;
			}

			if(!inventory.getStackInSlot(1).isEmpty() && inventory.getStackInSlot(0).isEmpty()) {
				inventory.setStackInSlot(0, inventory.getStackInSlot(1).copy());
				inventory.setStackInSlot(1, ItemStack.EMPTY);
			}
			if(!inventory.getStackInSlot(3).isEmpty() && inventory.getStackInSlot(2).isEmpty()) {
				inventory.setStackInSlot(2, inventory.getStackInSlot(3).copy());
				inventory.setStackInSlot(3, ItemStack.EMPTY);
			}

			if (canProcess()) {
				process();
				// world.spawnEntity(new EntityGasFlameFX(world, pos.getX() + 0.5F, pos.getY() + 3.8F, pos.getZ() + 0.5F, 0.0, 0.0, 0.0, 0.5F));
				// world.spawnEntity(new EntityGasFlameFX(world, pos.getX() + 0.5F, pos.getY() + 3.7F, pos.getZ() + 0.5F, 0.0, 0.0, 0.0, 0.5F));
				// ExplosionThermo.setEntitiesOnFire(world, pos.getX() + 0.5F, pos.getY() + 4, pos.getZ() + 0.5F, 2);

				// if(this.world.getTotalWorldTime() % 5 == 0)
				// 	this.world.playSound(null, pos.getX() + 0.5F, pos.getY() + 0.5F, pos.getZ() + 0.5F, HBMSoundHandler.flamethrowerShoot, SoundCategory.BLOCKS, 2F, 0.2F);
			} else {
				process = 0;
			}

			PacketDispatcher.wrapper.sendToAllAround(new AuxElectricityPacket(pos, power), new TargetPoint(world.provider.getDimension(), pos.getX(), pos.getY(), pos.getZ(), 10));
			PacketDispatcher.wrapper.sendToAllAround(new FluidTankPacket(pos, new FluidTank[] {tank}), new TargetPoint(world.provider.getDimension(), pos.getX(), pos.getY(), pos.getZ(), 10));
			if(prevPower != power || prevAmount != tank.getFluidAmount()){
				markDirty();
			}
		}
	}
	
	private boolean isValidFluid(FluidStack stack) {
		if(stack == null)
			return false;
		return stack.getFluid() == ModForgeFluids.sparkfuel
			|| stack.getFluid() == ModForgeFluids.nitan
			|| stack.getFluid() == ModForgeFluids.uu_matter
			|| stack.getFluid() == ModForgeFluids.balefire;
	}

	@Override
	public AxisAlignedBB getRenderBoundingBox() {
		return TileEntity.INFINITE_EXTENT_AABB;
	}

	@Override
	@SideOnly(Side.CLIENT)
	public double getMaxRenderDistanceSquared() {
		return 65536.0D;
	}

	
	@Override
	public void setPower(long i) {
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
	public IFluidTankProperties[] getTankProperties() {
		return new IFluidTankProperties[]{tank.getTankProperties()[0]};
	}

	@Override
	public int fill(FluidStack resource, boolean doFill) {
		if (isValidFluid(resource)) {
			if(tank.fill(resource, false) > 0)
				needsUpdate = true;
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
	public void recievePacket(NBTTagCompound[] tags) {
		if(tags.length != 1) {
			return;
		} else {
			tank.readFromNBT(tags[0]);
		}
	}
	
	@Override
	public <T> T getCapability(Capability<T> capability, EnumFacing facing) {
		if(capability == CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY){
			return CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY.cast(this);
		} else {
			return super.getCapability(capability, facing);
		}
	}
	
	@Override
	public boolean hasCapability(Capability<?> capability, EnumFacing facing) {
		if(capability == CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY){
			return true;
		} else {
			return super.hasCapability(capability, facing);
		}
	}

}
