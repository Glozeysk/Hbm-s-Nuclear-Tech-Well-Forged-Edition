package com.hbm.forgefluid;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidTank;

//NTM:CE keeps 0-5 PU of pressure per tank; we only have one flag, stored on the FluidStack's NBT.
//Forge compares stack NBT in FluidStack.isFluidEqual, so a pressurized fluid and its plain form never mix in a tank.
public class PressurizedFluids {

	public static final String TAG = "pressurized";

	public static boolean isPressurized(FluidStack stack) {
		return stack != null && stack.tag != null && stack.tag.getBoolean(TAG);
	}

	/** a copy of the stack with the flag set */
	public static FluidStack pressurize(FluidStack stack) {
		if(stack == null)
			return null;
		FluidStack copy = stack.copy();
		if(copy.tag == null)
			copy.tag = new NBTTagCompound();
		copy.tag.setBoolean(TAG, true);
		return copy;
	}

	public static FluidStack make(Fluid fluid, int amount) {
		return pressurize(new FluidStack(fluid, amount));
	}

	/** the tank holds at least the needed amount, with the same pressure flag as the recipe asks for */
	public static boolean hasEnough(FluidTank tank, FluidStack need) {
		return tank.getFluidAmount() >= need.amount && isPressurized(tank.getFluid()) == isPressurized(need);
	}

	/** same fluid and same flag, amounts ignored */
	public static boolean matches(FluidStack stack, Fluid fluid, boolean pressurized) {
		return stack != null && stack.getFluid() == fluid && isPressurized(stack) == pressurized;
	}
}
