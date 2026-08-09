package com.hbm.tileentity.machine;

import com.hbm.interfaces.Untested;
import com.hbm.inventory.ShredderRecipes;
import com.hbm.items.machine.ItemBlades;
import com.hbm.lib.Library;
import com.hbm.packet.AuxElectricityPacket;
import com.hbm.packet.PacketDispatcher;
import com.hbm.tileentity.TileEntityMachineBase;

import api.hbm.energy.IBatteryItem;
import api.hbm.energy.IEnergyUser;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ITickable;
import net.minecraft.util.SoundCategory;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.fml.common.network.NetworkRegistry.TargetPoint;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.ItemStackHandler;

public class TileEntityMachineShreddermk2 extends TileEntityMachineBase implements ITickable, IEnergyUser {

	public long power;
	public int progress;
	public int soundCycle = 0;
	private int syncTick = 0;
	public static final long maxPower = 120000;
	public static final int processingSpeed = 5;
	
	private static final int[] slots_top = new int[] {0};
	private static final int[] slots_bottom = new int[] {1, 2, 3};
	private static final int[] slots_side = new int[] {4};
	
	public TileEntityMachineShreddermk2() {
		super(5);
	}
	
	@Override
	public String getName(){
		return "container.machineShreddermk2";
	}
	
	@Override
	public int[] getAccessibleSlotsFromSide(EnumFacing e){
		int i = e.ordinal();
		return i == 0 ? slots_bottom : (i == 1 ? slots_top : slots_side);
	}
	
	@Override
	public boolean canInsertItem(int slot, ItemStack itemStack, int amount){
		return this.isItemValidForSlot(slot, itemStack);
	}
	
	@Override
	public boolean isItemValidForSlot(int i, ItemStack stack){
		if (i < 1) {
			return true;
		} else if (i == 4 && stack.getItem() instanceof IBatteryItem) {
			return true;
		} else {
			return false;
		}
	}
	
	@Override
	public boolean canExtractItem(int slot, ItemStack itemStack, int amount){
		if(slot >= 1 && slot <= 3){
			return true;
		}
		return false;
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
		this.power = compound.getLong("powerTime");
		if(compound.hasKey("inventory"))
			inventory.deserializeNBT(compound.getCompoundTag("inventory"));
		super.readFromNBT(compound);
	}
	
	@Override
	public NBTTagCompound writeToNBT(NBTTagCompound compound) {
		compound.setLong("powerTime", power);
		compound.setTag("inventory", inventory.serializeNBT());
		return super.writeToNBT(compound);
	}
	
	public int getDiFurnaceProgressScaled(int i) {
		return (progress * i) / processingSpeed;
	}
	
	public boolean hasPower() {
		return power > 0;
	}
	
	public boolean isProcessing() {
		return this.progress > 0;
	}
	
	@Override
	public void update() {
		boolean flag1 = false;
		
		if(!world.isRemote)
		{			
			this.updateStandardConnections(world, pos);
			if(hasPower() && canProcess())
			{
				progress++;
				
				power -= 60;
				
				if(this.progress == TileEntityMachineShreddermk2.processingSpeed)
				{
					this.progress = 0;
					this.processItem();
					flag1 = true;
				}
				if(soundCycle == 0)
		        	this.world.playSound(null, pos.getX(), pos.getY(), pos.getZ(), SoundEvents.ENTITY_MINECART_RIDING, SoundCategory.BLOCKS, 1.0F, 0.75F);
				soundCycle++;
				
				if(soundCycle >= 50)
					soundCycle = 0;
			}else{
				progress = 0;
			}
			
			boolean trigger = true;
			
			if(hasPower() && canProcess() && this.progress == 0)
			{
				trigger = false;
			}
			
			if(trigger)
            {
                flag1 = true;
            }
			
			power = Library.chargeTEFromItems(inventory, 4, power, maxPower);
			
			syncTick++;
			if(syncTick >= 5) {
				syncTick = 0;
				PacketDispatcher.wrapper.sendToAllAround(new AuxElectricityPacket(pos.getX(), pos.getY(), pos.getZ(), power), new TargetPoint(world.provider.getDimension(), pos.getX(), pos.getY(), pos.getZ(), 15));
			}
		}
		
		if(flag1)
		{
			this.markDirty();
		}
	}
	
	public void processItem() {
		for(int inpSlot = 0; inpSlot < 1; inpSlot++)
		{
			if(!inventory.getStackInSlot(inpSlot).isEmpty() && hasSpace(inventory.getStackInSlot(inpSlot)))
			{
				ItemStack inp = inventory.getStackInSlot(inpSlot);
				ItemStack outp = ShredderRecipes.getShredderResult(inp);
				boolean flag = false;
				
				for (int outSlot = 1; outSlot < 4; outSlot++)
				{
					if (inventory.getStackInSlot(outSlot).getItem() == outp.getItem() && 
							inventory.getStackInSlot(outSlot).getItemDamage() == outp.getItemDamage() &&
									inventory.getStackInSlot(outSlot).getCount() + outp.getCount() <= outp.getMaxStackSize()) {

						inventory.getStackInSlot(outSlot).grow(outp.getCount());
						inventory.getStackInSlot(inpSlot).shrink(1);
						flag = true;
						break;
					}
				}
				
				if(!flag)
					for (int outSlot = 1; outSlot < 4; outSlot++)
					{
						if (inventory.getStackInSlot(outSlot).isEmpty()) {
							inventory.setStackInSlot(outSlot, outp.copy());
							inventory.getStackInSlot(inpSlot).shrink(1);
							break;
						}
					}
				
				if(inventory.getStackInSlot(inpSlot).isEmpty())
					inventory.setStackInSlot(inpSlot, ItemStack.EMPTY);
			}
		}
	}
	
	@Untested
	public boolean canProcess() {
		for(int i = 0; i < 1; i++)
		{
			if(!inventory.getStackInSlot(i).isEmpty() && inventory.getStackInSlot(i).getCount() > 0 && hasSpace(inventory.getStackInSlot(i)))
			{
				return true;
			}
		}
		
		return false;
	}
	
	public boolean hasSpace(ItemStack stack) {
		
		ItemStack result = ShredderRecipes.getShredderResult(stack);
		
		if (result != null)
			for (int i = 1; i < 4; i++) {
				if (inventory.getStackInSlot(i).isEmpty()) {
					return true;
				}

				if (inventory.getStackInSlot(i).getItem().equals(result.getItem())
						&& inventory.getStackInSlot(i).getCount() + result.getCount() <= result.getMaxStackSize()) {
					return true;
				}
			}
		
		return false;
	}

	@Override
	public void setPower(long i) {
		this.power = i;
		
	}
	
	public long getPowerScaled(long i) {
		return (power * i) / maxPower;
	}

	@Override
	public long getPower() {
		return this.power;
	}

	@Override
	public long getMaxPower() {
		return TileEntityMachineShreddermk2.maxPower;
	}
	
}
