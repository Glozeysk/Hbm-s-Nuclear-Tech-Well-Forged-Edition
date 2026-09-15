package com.hbm.inventory;

import java.util.LinkedHashMap;

import com.hbm.forgefluid.ModForgeFluids;
import com.hbm.util.Tuple.Quartet;

import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidStack;

public class CatalyticReformerRecipes {

	//feedstock -> <feedstock consumed per operation, output tank 1, output tank 2, output tank 3>; linked to keep JEI order
	public static LinkedHashMap<Fluid, Quartet<FluidStack, FluidStack, FluidStack, FluidStack>> recipes = new LinkedHashMap<>();

	public static void registerRecipes() {
		addRecipe(new FluidStack(ModForgeFluids.naphtha, 1000),
				new FluidStack(ModForgeFluids.reformate, 500),
				new FluidStack(ModForgeFluids.petroleum, 150),
				new FluidStack(ModForgeFluids.hydrogen, 100));
		addRecipe(new FluidStack(ModForgeFluids.naphtha_pure, 1000),
				new FluidStack(ModForgeFluids.reformate, 500),
				new FluidStack(ModForgeFluids.aromatics, 150),
				new FluidStack(ModForgeFluids.hydrogen, 50));
		addRecipe(new FluidStack(ModForgeFluids.lightoil, 1000),
				new FluidStack(ModForgeFluids.aromatics, 500),
				new FluidStack(ModForgeFluids.reformate, 50),
				new FluidStack(ModForgeFluids.hydrogen, 200));
		addRecipe(new FluidStack(ModForgeFluids.lightoil_pure, 1000),
				new FluidStack(ModForgeFluids.aromatics, 500),
				new FluidStack(ModForgeFluids.reformate, 100),
				new FluidStack(ModForgeFluids.hydrogen, 150));
		addRecipe(new FluidStack(ModForgeFluids.petroleum, 1000),
				new FluidStack(ModForgeFluids.unsaturateds, 850),
				new FluidStack(ModForgeFluids.reformate, 100),
				new FluidStack(ModForgeFluids.hydrogen, 50));
		addRecipe(new FluidStack(ModForgeFluids.sourgas, 1000),
				new FluidStack(ModForgeFluids.sulfuric_acid, 750),
				new FluidStack(ModForgeFluids.petroleum, 100),
				new FluidStack(ModForgeFluids.hydrogen, 150));
		addRecipe(new FluidStack(ModForgeFluids.heatingoil, 1000),
				new FluidStack(ModForgeFluids.naphtha, 500),
				new FluidStack(ModForgeFluids.petroleum, 150),
				new FluidStack(ModForgeFluids.hydrogen, 100));
	}

	public static void addRecipe(FluidStack input, FluidStack out1, FluidStack out2, FluidStack out3) {
		recipes.put(input.getFluid(), new Quartet<>(input, out1, out2, out3));
	}

	public static Quartet<FluidStack, FluidStack, FluidStack, FluidStack> getRecipe(Fluid input) {
		return recipes.get(input);
	}
}
