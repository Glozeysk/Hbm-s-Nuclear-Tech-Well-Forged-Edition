package com.hbm.inventory;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

import com.hbm.forgefluid.ModForgeFluids;
import com.hbm.forgefluid.PressurizedFluids;

import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidStack;

//Ported from NTM:CE (CompressorRecipes). CE keys recipes by fluid and 0-5 PU; we key them by fluid and the pressure flag.
//A fluid without a recipe is simply pressurized, 1000 mB at a time.
public class CompressorRecipes {

	public static final int DEFAULT_AMOUNT = 1_000;
	public static final int DEFAULT_DURATION = 100;

	public static Map<Key, CompressorRecipe> recipes = new LinkedHashMap<Key, CompressorRecipe>();

	public static void register() {
		recipes.clear();

		//test recipe, same amounts and time as CE's petroleum to LPG step
		addRecipe(ModForgeFluids.xenon, false, 2_000, new FluidStack(ModForgeFluids.argon, 1_000), 20);
	}

	public static void addRecipe(Fluid input, boolean pressurized, int inputAmount, FluidStack output, int duration) {
		recipes.put(new Key(input, pressurized), new CompressorRecipe(inputAmount, output, duration));
	}

	public static CompressorRecipe getRecipe(FluidStack input) {
		if(input == null)
			return null;
		return recipes.get(new Key(input.getFluid(), PressurizedFluids.isPressurized(input)));
	}

	public static class Key {

		public final Fluid fluid;
		public final boolean pressurized;

		public Key(Fluid fluid, boolean pressurized) {
			this.fluid = fluid;
			this.pressurized = pressurized;
		}

		@Override
		public boolean equals(Object o) {
			if(!(o instanceof Key))
				return false;
			Key other = (Key) o;
			return other.fluid == fluid && other.pressurized == pressurized;
		}

		@Override
		public int hashCode() {
			return Objects.hash(fluid, pressurized);
		}
	}

	public static class CompressorRecipe {

		public final int inputAmount;
		public final FluidStack output;
		public final int duration;

		public CompressorRecipe(int inputAmount, FluidStack output, int duration) {
			this.inputAmount = inputAmount;
			this.output = output;
			this.duration = duration;
		}
	}
}
