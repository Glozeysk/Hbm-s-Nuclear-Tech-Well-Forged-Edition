package com.hbm.inventory;

import java.util.ArrayList;
import java.util.List;

import com.hbm.forgefluid.ModForgeFluids;
import com.hbm.inventory.RecipesCommon.AStack;
import com.hbm.inventory.RecipesCommon.ComparableStack;
import com.hbm.inventory.RecipesCommon.OreDictStack;
import com.hbm.items.ModItems;

import net.minecraft.item.ItemStack;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidStack;

public class PyroOvenRecipes {

	public static final List<PyroOvenRecipe> recipes = new ArrayList<>();

	public static void registerRecipes() {
		addRecipe(new PyroOvenRecipe(100)
				.in(new FluidStack(ModForgeFluids.hydrogen, 500))
				.in(new OreDictStack(OreDictManager.COAL.gem()))
				.out(new FluidStack(ModForgeFluids.heavyoil, 1000)));
		addRecipe(new PyroOvenRecipe(200)
				.in(new FluidStack(ModForgeFluids.gas, 12000))
				.out(new ComparableStack(ModItems.ingot_graphite, 1))
				.out(new FluidStack(ModForgeFluids.hydrogen, 8000)));
		addRecipe(new PyroOvenRecipe(400)
				.in(new ComparableStack(ModItems.oil_tar, 4))
				.out(new ComparableStack(ModItems.powder_soot, 1)));
		addRecipe(new PyroOvenRecipe(100)
				.in(new FluidStack(ModForgeFluids.hydrogen, 500))
				.in(new OreDictStack(OreDictManager.COAL.dust()))
				.out(new FluidStack(ModForgeFluids.heavyoil, 1000)));
		for(int oreMeta : BedrockOreRegistry.oreIndexes.keySet()) {
			addRecipe(new PyroOvenRecipe(100)
					.in(new ComparableStack(ModItems.ore_bedrock_seared, 1, oreMeta))
					.out(new ItemStack(ModItems.ore_bedrock_exquisite, 1, oreMeta)));
		}

		registerSFAuto(ModForgeFluids.smear);
		registerSFAuto(ModForgeFluids.heatingoil);
		registerSFAuto(ModForgeFluids.reclaimed);
		registerSFAuto(ModForgeFluids.petroil);
		registerSFAuto(ModForgeFluids.naphtha);
		registerSFAuto(ModForgeFluids.diesel);
		registerSFAuto(ModForgeFluids.diesel_hq);
		registerSFAuto(ModForgeFluids.lightoil);
		registerSFAuto(ModForgeFluids.kerosene);
		registerSFAuto(ModForgeFluids.kerosene_hq);
		registerSFAuto(ModForgeFluids.sourgas);
		registerSFAuto(ModForgeFluids.petroleum);
		registerSFAuto(ModForgeFluids.aromatics);
		registerSFAuto(ModForgeFluids.unsaturateds);
		registerSFAuto(ModForgeFluids.reformate);
		registerSFAuto(ModForgeFluids.xylene);
	}

	public static void registerSFAuto(Fluid fluid) {
		long tuPerSF = 1_440_000L;
		long tuPerBucket = FluidCombustionRecipes.getFlameEnergy(fluid) * 1000L;
		if(tuPerBucket <= 0)
			throw new IllegalStateException("Pyrolysis oven: no combustion energy for " + fluid.getName());
		double bonus = 0.5D;

		int mB = (int) (tuPerSF * 1000L * bonus / tuPerBucket);

		if(mB > 10_000) mB -= (mB % 1000);
		else if(mB > 1_000) mB -= (mB % 100);
		else if(mB > 100) mB -= (mB % 10);

		mB = Math.max(mB, 1);

		addRecipe(new PyroOvenRecipe(60).in(new FluidStack(fluid, mB)).out(new ItemStack(ModItems.solid_fuel)));
	}

	public static void addRecipe(PyroOvenRecipe recipe) {
		if(recipe.inputFluid == null && recipe.inputItem == null)
			throw new IllegalArgumentException("Pyrolysis oven recipe has no input");
		if(recipe.outputFluid == null && recipe.outputItem == null)
			throw new IllegalArgumentException("Pyrolysis oven recipe has no output");
		if(recipe.duration <= 0)
			throw new IllegalArgumentException("Pyrolysis oven recipe duration must be positive");
		recipes.add(recipe);
	}

	public static boolean isInputFluid(Fluid fluid) {
		if(fluid == null)
			return false;
		for(PyroOvenRecipe recipe : recipes)
			if(recipe.inputFluid != null && recipe.inputFluid.getFluid() == fluid)
				return true;
		return false;
	}

	public static boolean isInputItem(ItemStack stack) {
		if(stack.isEmpty())
			return false;
		for(PyroOvenRecipe recipe : recipes)
			if(recipe.inputItem != null && recipe.inputItem.matchesRecipe(stack, true))
				return true;
		return false;
	}

	public static class PyroOvenRecipe {

		public FluidStack inputFluid;
		public AStack inputItem;
		public FluidStack outputFluid;
		public ItemStack outputItem;
		public final int duration;

		public PyroOvenRecipe(int duration) {
			this.duration = duration;
		}

		public PyroOvenRecipe in(FluidStack stack) { this.inputFluid = stack; return this; }
		public PyroOvenRecipe in(AStack stack) { this.inputItem = stack; return this; }
		public PyroOvenRecipe in(ItemStack stack) { this.inputItem = new ComparableStack(stack); return this; }
		public PyroOvenRecipe out(FluidStack stack) { this.outputFluid = stack; return this; }
		public PyroOvenRecipe out(ItemStack stack) { this.outputItem = stack.copy(); return this; }
		public PyroOvenRecipe out(AStack stack) {
			if(stack.getStackList().isEmpty())
				throw new IllegalArgumentException("Pyrolysis oven recipe output " + stack + " resolves to no item");
			this.outputItem = stack.getStack();
			return this;
		}
	}
}
