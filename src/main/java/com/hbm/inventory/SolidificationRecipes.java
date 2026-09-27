package com.hbm.inventory;

import java.util.LinkedHashMap;
import java.util.Map;

import com.hbm.forgefluid.ModForgeFluids;
import com.hbm.items.ModItems;

import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidRegistry;

//Ported from NTM:CE (SolidificationRecipes). Fuel amounts use CE's formula but our own combustion heat values,
//so this has to run after FluidCombustionRecipes.registerFluidCombustionRecipes().
public class SolidificationRecipes {

	/** 3200 burntime * 1.5 burntime bonus * 300 TU/t */
	public static final long TU_PER_SOLID_FUEL = 1_440_000L;
	public static final double PENALTY = 1.25D;

	public static Map<Fluid, SolidificationRecipe> recipes = new LinkedHashMap<Fluid, SolidificationRecipe>();

	public static void register() {
		recipes.clear();

		registerRecipe(FluidRegistry.WATER, 1000, new ItemStack(Blocks.ICE));
		registerRecipe(FluidRegistry.LAVA, 1000, new ItemStack(Blocks.OBSIDIAN));
		registerRecipe(ModForgeFluids.mercury, 125, new ItemStack(ModItems.nugget_mercury));
		registerRecipe(ModForgeFluids.bitumen, 100, new ItemStack(ModItems.oil_tar));
		registerRecipe(ModForgeFluids.oil, 200, new ItemStack(ModItems.oil_tar));
		registerRecipe(ModForgeFluids.heavyoil, 150, new ItemStack(ModItems.oil_tar));

		registerSFAuto(ModForgeFluids.smear);
		registerSFAuto(ModForgeFluids.heatingoil);
		registerSFAuto(ModForgeFluids.reclaimed);
		registerSFAuto(ModForgeFluids.petroil);
		registerSFAuto(ModForgeFluids.naphtha);
		registerSFAuto(ModForgeFluids.naphtha_pure);
		registerSFAuto(ModForgeFluids.diesel);
		registerSFAuto(ModForgeFluids.diesel_hq);
		registerSFAuto(ModForgeFluids.lightoil);
		registerSFAuto(ModForgeFluids.lightoil_pure);
		registerSFAuto(ModForgeFluids.kerosene);
		registerSFAuto(ModForgeFluids.kerosene_hq);
		registerSFAuto(ModForgeFluids.sourgas);
		registerSFAuto(ModForgeFluids.petroleum);
		registerSFAuto(ModForgeFluids.biofuel);
		registerSFAuto(ModForgeFluids.aromatics);
		registerSFAuto(ModForgeFluids.unsaturateds);
		registerSFAuto(ModForgeFluids.reformate);
		registerSFAuto(ModForgeFluids.xylene);
	}

	public static void registerSFAuto(Fluid fluid) {
		registerSFAuto(fluid, TU_PER_SOLID_FUEL, ModItems.solid_fuel);
	}

	/** how much fluid holds the heat of one fuel item, rounded like CE does */
	public static void registerSFAuto(Fluid fluid, long tuPerItem, Item fuel) {

		//our combustion values are TU per mB
		long tuPerBucket = FluidCombustionRecipes.getFlameEnergy(fluid) * 1000L;

		if(tuPerBucket <= 0)
			return;

		int mB = (int) (tuPerItem * 1000L * PENALTY / tuPerBucket);

		if(mB > 10_000) mB -= (mB % 1000);
		else if(mB > 1_000) mB -= (mB % 100);
		else if(mB > 100) mB -= (mB % 10);

		registerRecipe(fluid, Math.max(mB, 1), new ItemStack(fuel));
	}

	public static void registerRecipe(Fluid fluid, int amount, ItemStack output) {
		recipes.put(fluid, new SolidificationRecipe(amount, output));
	}

	public static SolidificationRecipe getRecipe(Fluid fluid) {
		return fluid == null ? null : recipes.get(fluid);
	}

	public static class SolidificationRecipe {

		public final int amount;
		public final ItemStack output;

		public SolidificationRecipe(int amount, ItemStack output) {
			this.amount = amount;
			this.output = output;
		}
	}
}
