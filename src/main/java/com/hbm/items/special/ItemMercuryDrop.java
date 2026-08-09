package com.hbm.items.special;

import com.hbm.forgefluid.ModForgeFluids;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.CapabilityFluidHandler;
import net.minecraftforge.fluids.capability.FluidTankProperties;
import net.minecraftforge.fluids.capability.IFluidHandlerItem;
import net.minecraftforge.fluids.capability.IFluidTankProperties;

public class ItemMercuryDrop extends ItemHazard {

	public static final int AMOUNT = 125;

	public ItemMercuryDrop(String s) {
		super(s);
	}

	@Override
	public ICapabilityProvider initCapabilities(ItemStack stack, NBTTagCompound nbt) {
		return new MercuryDropFluidHandler(stack);
	}

	private static class MercuryDropFluidHandler implements ICapabilityProvider, IFluidHandlerItem {

		private final ItemStack container;

		private MercuryDropFluidHandler(ItemStack container) {
			this.container = container;
		}

		@Override
		public IFluidTankProperties[] getTankProperties() {
			return new IFluidTankProperties[] { new FluidTankProperties(container.isEmpty() ? null : new FluidStack(ModForgeFluids.mercury, AMOUNT), AMOUNT, false, true) };
		}

		@Override
		public int fill(FluidStack resource, boolean doFill) {
			return 0;
		}

		@Override
		public FluidStack drain(FluidStack resource, boolean doDrain) {
			if(resource == null || resource.getFluid() != ModForgeFluids.mercury) {
				return null;
			}
			return drain(resource.amount, doDrain);
		}

		@Override
		public FluidStack drain(int maxDrain, boolean doDrain) {
			if(container.isEmpty() || maxDrain < AMOUNT) {
				return null;
			}
			if(doDrain) {
				container.shrink(1);
			}
			return new FluidStack(ModForgeFluids.mercury, AMOUNT);
		}

		@Override
		public ItemStack getContainer() {
			return container;
		}

		@Override
		public boolean hasCapability(Capability<?> capability, EnumFacing facing) {
			return capability == CapabilityFluidHandler.FLUID_HANDLER_ITEM_CAPABILITY;
		}

		@Override
		@SuppressWarnings("unchecked")
		public <T> T getCapability(Capability<T> capability, EnumFacing facing) {
			return capability == CapabilityFluidHandler.FLUID_HANDLER_ITEM_CAPABILITY ? (T)this : null;
		}
	}
}
