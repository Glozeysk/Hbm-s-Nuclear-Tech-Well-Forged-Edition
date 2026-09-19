package com.hbm.inventory;

import java.util.LinkedHashMap;

import com.hbm.forgefluid.ModForgeFluids;

import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;

public class ElectrolyserFluidRecipes {

	public static final LinkedHashMap<Fluid, ElectrolysisRecipe> recipes = new LinkedHashMap<>();

	public static void registerRecipes() {
		addRecipe(new FluidStack(FluidRegistry.WATER, 2_000), new FluidStack(ModForgeFluids.hydrogen, 200), new FluidStack(ModForgeFluids.oxygen, 200), 10);
		addRecipe(new FluidStack(ModForgeFluids.heavywater, 2_000), new FluidStack(ModForgeFluids.deuterium, 100), new FluidStack(ModForgeFluids.oxygen, 100), 10);
		addRecipe(new FluidStack(ModForgeFluids.brine, 2_000), new FluidStack(ModForgeFluids.chlorine, 200), new FluidStack(ModForgeFluids.sodiumhydroxide, 400), 40);
		addRecipe(new FluidStack(ModForgeFluids.sodiumhydroxide, 2_000), new FluidStack(ModForgeFluids.sodiumbase, 400), new FluidStack(FluidRegistry.WATER, 400), 100);
	}

	public static void addRecipe(FluidStack input, FluidStack output1, FluidStack output2, int duration) {
		if(input == null || input.getFluid() == null || output1 == null)
			throw new IllegalArgumentException("Electrolyser fluid recipe needs an input and a first output");
		if(duration <= 0)
			throw new IllegalArgumentException("Electrolyser fluid recipe duration must be positive");
		recipes.put(input.getFluid(), new ElectrolysisRecipe(input.amount, output1, output2, duration));
	}

	public static ElectrolysisRecipe getRecipe(Fluid fluid) {
		return fluid == null ? null : recipes.get(fluid);
	}

	public static class ElectrolysisRecipe {
		public final int amount;
		public final FluidStack output1;
		public final FluidStack output2;
		public final int duration;

		public ElectrolysisRecipe(int amount, FluidStack output1, FluidStack output2, int duration) {
			this.amount = amount;
			this.output1 = output1;
			this.output2 = output2;
			this.duration = duration;
		}
	}
}
