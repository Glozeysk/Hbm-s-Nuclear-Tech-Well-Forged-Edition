package com.hbm.tileentity.machine;

import com.hbm.blocks.machine.MachineDiFurnaceSPK;
import com.hbm.forgefluid.FFUtils;
import com.hbm.forgefluid.ModForgeFluids;
import com.hbm.interfaces.ITankPacketAcceptor;
import com.hbm.inventory.DiFurnaceRecipes;
import com.hbm.items.ModItems;
import com.hbm.lib.Library;
import com.hbm.packet.AuxElectricityPacket;
import com.hbm.packet.FluidTankPacket;
import com.hbm.packet.PacketDispatcher;
import com.hbm.tileentity.TileEntityMachineBase;

import api.hbm.energy.IBatteryItem;
import api.hbm.energy.IEnergyUser;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
// import net.minecraft.init.SoundEvents;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ITickable;
// import net.minecraft.util.SoundCategory;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidTank;
import net.minecraftforge.fluids.FluidUtil;
import net.minecraftforge.fluids.capability.CapabilityFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.IFluidTankProperties;
import net.minecraftforge.fml.common.network.NetworkRegistry.TargetPoint;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.ItemStackHandler;

public class TileEntityMachineDiFurnaceSPK extends TileEntityMachineBase implements ITickable, IEnergyUser, IFluidHandler, ITankPacketAcceptor {

	public long power = 0;
	public int process = 0;
	// public int soundCycle = 0;
	public static final long maxPower = 500000000;
	public static final int processSpeed = 1;
	public FluidTank tank;
	public Fluid tankType = ModForgeFluids.sparkfuel;
	public boolean needsUpdate = false;
	private boolean lastTrigger = false;

	private static final int[] slots_top = new int[] {0, 1};
	private static final int[] slots_bottom = new int[] {2};
	private static final int[] slots_side = new int[] {};
	
	public TileEntityMachineDiFurnaceSPK() {
		super(3);
		tank = new FluidTank(8000);
	}
	
	@Override
	public String getName(){
		return "container.machinedifurnaceSPK";
	}
	
	@Override
	public int[] getAccessibleSlotsFromSide(EnumFacing e){
		int i = e.ordinal();
		return i == 0 ? slots_bottom : (i == 1 ? slots_top : slots_side);
	}
	
	@Override
	public boolean canInsertItem(int slot, ItemStack stack, int amount){
		if(slot == 0 && isItemValidForSlot(slot, stack)) return inventory.getStackInSlot(1).getItem() != stack.getItem();
		if(slot == 1 && isItemValidForSlot(slot, stack)) return inventory.getStackInSlot(0).getItem() != stack.getItem();
		return this.isItemValidForSlot(slot, stack);
	}
	
	@Override
	public boolean canExtractItem(int i, ItemStack itemStack, int amount){
		if(i == 2)
			return true;
		// if(i == 0)
		// 	if (itemStack.getItem() instanceof IBatteryItem && ((IBatteryItem)itemStack.getItem()).getCharge(itemStack) == 0)
		// 		return true;
		// if(i == 3)
		// 	if(FFUtils.containsFluid(itemStack, ModForgeFluids.sparkfuel))
		// 		return true;
		return false;
	}
	
	@Override
	public boolean isItemValidForSlot(int i, ItemStack stack){
		if(i == 2) {
			return false;
		}
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
		return (process * i) / processSpeed;
	}

	public boolean canProcess() {
		if(inventory.getStackInSlot(0) == null || inventory.getStackInSlot(1) == null)
		{
			return false;
		}
		if(tank.getFluidAmount() < 5)
		{
			return false;
		}
		if(power < 500000)
		{
			return false;
		}
		ItemStack itemStack = DiFurnaceRecipes.getFurnaceProcessingResult(inventory.getStackInSlot(0), inventory.getStackInSlot(1));
		if(itemStack == null)
		{	
			return false;
		}
		
		if(inventory.getStackInSlot(2) == ItemStack.EMPTY)
		{
			return true;
		}
		if(inventory.getStackInSlot(2).getItem() != ItemStack.EMPTY.getItem() && !inventory.getStackInSlot(2).isItemEqual(itemStack)) {
			return false;
		}
		
		if(inventory.getStackInSlot(2).getCount() < inventory.getSlotLimit(2) && inventory.getStackInSlot(2).getCount() < inventory.getStackInSlot(2).getMaxStackSize()) {
			return true;
		}else{
			return inventory.getStackInSlot(2).getCount() < itemStack.getMaxStackSize();
		}
	}
	
	public boolean isProcessing() {
		return process > 0;
	}
	
	public void process() {
		tank.drain(5, true);
		needsUpdate = true;
		power -= 500000;
		
		process++;
		
		if(process >= processSpeed) {
			
			ItemStack itemStack = DiFurnaceRecipes.getFurnaceProcessingResult(inventory.getStackInSlot(0), inventory.getStackInSlot(1));
			
			if(inventory.getStackInSlot(2).isEmpty())
			{
				inventory.setStackInSlot(2, itemStack.copy());
			}else if(inventory.getStackInSlot(2).isItemEqual(itemStack)) {
				inventory.getStackInSlot(2).grow(itemStack.getCount());
			}
			
			for(int i = 0; i < 2; i++)
			{
				if(inventory.getStackInSlot(i).getCount() <= 0)
				{
					inventory.setStackInSlot(i, new ItemStack(inventory.getStackInSlot(i).getItem().setFull3D()));
				}else{
					inventory.getStackInSlot(i).shrink(1);
				}
				if(inventory.getStackInSlot(i).getCount() <= 0)
				{
					inventory.setStackInSlot(i, ItemStack.EMPTY);
				}
			}
			
			process = 0;
		}
	}
	
	@Override
	public void update() {
		if (!world.isRemote) {

			this.updateStandardConnections(world, pos);

			long prevPower = power;
			int prevAmount = tank.getFluidAmount();
			if (needsUpdate) {
				needsUpdate = false;
			}
			// power = Library.chargeTEFromItems(inventory, 0, power, maxPower);
			
			// if(this.inputValidForTank(-1, 3))
			// 	if(FFUtils.fillFromFluidContainer(inventory, tank, 3, 5))
			// 		needsUpdate = true;

			if (canProcess()) {
				process();
				// if(soundCycle == 0)
			    //     this.world.playSound(null, pos.getX(), pos.getY(), pos.getZ(), SoundEvents.ENTITY_MINECART_RIDING, SoundCategory.BLOCKS, 1.0F, 1.5F);
				// soundCycle++;
					
				// if(soundCycle >= 25)
				// 	soundCycle = 0;
			} else {
				process = 0;
			}

			boolean trigger = isProcessing() || canProcess();
			if(trigger != lastTrigger)
				MachineDiFurnaceSPK.updateBlockState(trigger, this.world, pos);
			lastTrigger = trigger;

			PacketDispatcher.wrapper.sendToAllAround(new AuxElectricityPacket(pos, power), new TargetPoint(world.provider.getDimension(), pos.getX(), pos.getY(), pos.getZ(), 10));
			PacketDispatcher.wrapper.sendToAllAround(new FluidTankPacket(pos, new FluidTank[] {tank}), new TargetPoint(world.provider.getDimension(), pos.getX(), pos.getY(), pos.getZ(), 10));
			if(prevPower != power || prevAmount != tank.getFluidAmount()){
				markDirty();
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
		return stack.getFluid() == ModForgeFluids.sparkfuel;
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
