package com.hbm.inventory;

import java.util.HashMap;

import com.hbm.forgefluid.ModForgeFluids;
import com.hbm.util.Tuple.Triplet;

import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidStack;

public class HydrotreaterRecipes {

	public static HashMap<Fluid, Triplet<FluidStack, FluidStack, FluidStack>> recipes = new HashMap<Fluid, Triplet<FluidStack, FluidStack, FluidStack>>();

	public static void registerRecipes() {
		recipes.put(ModForgeFluids.crackoil, new Triplet<FluidStack, FluidStack, FluidStack>(
				new FluidStack(ModForgeFluids.hydrogen, 50),
				new FluidStack(ModForgeFluids.crackoil_pure, 900),
				new FluidStack(ModForgeFluids.sourgas, 150)
		));
		recipes.put(ModForgeFluids.naphtha, new Triplet<FluidStack, FluidStack, FluidStack>(
				new FluidStack(ModForgeFluids.hydrogen, 100),
				new FluidStack(ModForgeFluids.naphtha_pure, 800),
				new FluidStack(ModForgeFluids.sourgas, 300)
		));
		recipes.put(ModForgeFluids.lightoil, new Triplet<FluidStack, FluidStack, FluidStack>(
				new FluidStack(ModForgeFluids.hydrogen, 100),
				new FluidStack(ModForgeFluids.lightoil_pure, 800),
				new FluidStack(ModForgeFluids.sourgas, 300)
		));
		recipes.put(ModForgeFluids.petroil, new Triplet<FluidStack, FluidStack, FluidStack>(
				new FluidStack(ModForgeFluids.sodiumhydroxide, 200),
				new FluidStack(ModForgeFluids.petroil_hq, 800),
				new FluidStack(ModForgeFluids.sourgas, 200)
		));
		recipes.put(ModForgeFluids.petroleum, new Triplet<FluidStack, FluidStack, FluidStack>(
				new FluidStack(ModForgeFluids.hydrogen, 150),
				new FluidStack(ModForgeFluids.methane, 500),
				new FluidStack(ModForgeFluids.sourgas, 400)
		));
	}

	public static Triplet<FluidStack, FluidStack, FluidStack> getRecipe(Fluid input) {
		return recipes.get(input);
	}
}
