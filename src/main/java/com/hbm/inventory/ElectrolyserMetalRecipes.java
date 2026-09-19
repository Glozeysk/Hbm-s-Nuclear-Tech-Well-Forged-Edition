package com.hbm.inventory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;

import com.hbm.inventory.RecipesCommon.AStack;
import com.hbm.inventory.RecipesCommon.ComparableStack;
import com.hbm.forgefluid.ModForgeFluids;
import com.hbm.items.ModItems;

import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidStack;

public class ElectrolyserMetalRecipes {

	public static final int MAX_OUTPUTS = 4;

	public static final List<ElectrolysisMetalRecipe> recipes = new ArrayList<>();

	public static void registerRecipes() {
		crystal(ModItems.crystal_rare, out(ModItems.nugget_zirconium, 6), out(ModItems.nugget_zirconium, 6), out(ModItems.powder_boron_tiny, 2), out(ModItems.powder_desh_mix, 3));
		crystal(ModItems.crystal_titanium, out(ModItems.powder_titanium, 6), out(ModItems.powder_iron, 2), out(ModItems.powder_lithium_tiny, 3));
		crystal(ModItems.crystal_iron, out(ModItems.powder_iron, 6), out(ModItems.powder_titanium, 2), out(ModItems.powder_lithium_tiny, 3));
		crystal(ModItems.crystal_gold, out(ModItems.powder_gold, 6), out(ModItems.powder_lead, 2), out(ModItems.powder_lithium_tiny, 3), out(ModItems.nugget_mercury, 2));
		crystal(ModItems.crystal_starmetal, out(ModItems.powder_dura_steel, 4), out(ModItems.powder_cobalt, 4), out(ModItems.powder_astatine, 3), out(ModItems.nugget_mercury, 8));
		crystal(ModItems.crystal_copper, out(ModItems.powder_copper, 6), out(ModItems.nugget_lead, 4), out(ModItems.powder_lithium_tiny, 3), out(ModItems.sulfur, 2));
		crystal(ModItems.crystal_tungsten, out(ModItems.powder_tungsten, 6), out(ModItems.powder_iron, 2), out(ModItems.powder_lithium_tiny, 3));
		crystal(ModItems.crystal_lithium, out(ModItems.powder_lithium, 6), out(ModItems.powder_boron, 2), out(ModItems.powder_quartz, 2), out(ModItems.fluorite, 2));
		crystal(ModItems.crystal_beryllium, out(ModItems.powder_beryllium, 6), out(ModItems.nugget_lead, 4), out(ModItems.powder_lithium_tiny, 3), out(ModItems.powder_quartz, 2));
		crystal(ModItems.crystal_schraranium, out(ModItems.nugget_schrabidium, 5), out(ModItems.nugget_uranium, 2), out(ModItems.nugget_neptunium, 2));
		crystal(ModItems.crystal_thorium, out(ModItems.powder_thorium, 6), out(ModItems.powder_uranium, 2), out(ModItems.powder_lithium_tiny, 2));
		crystal(ModItems.crystal_trixite, out(ModItems.powder_plutonium, 3), out(ModItems.powder_cobalt, 4), out(ModItems.powder_spark_mix, 2), out(ModItems.powder_nitan_mix, 4));
		crystal(ModItems.crystal_schrabidium, out(ModItems.powder_schrabidium, 6), out(ModItems.powder_plutonium, 2), out(ModItems.powder_lithium_tiny, 3));
		crystal(ModItems.crystal_lead, out(ModItems.powder_lead, 6), out(ModItems.powder_gold, 2), out(ModItems.powder_lithium_tiny, 3));
		crystal(ModItems.crystal_plutonium, out(ModItems.powder_plutonium, 6), out(ModItems.powder_plutonium, 2), out(ModItems.powder_lithium_tiny, 3));
		crystal(ModItems.crystal_aluminium, out(ModItems.powder_aluminium, 6), out(ModItems.powder_iron, 2), out(ModItems.powder_lithium_tiny, 3));
		crystal(ModItems.crystal_uranium, out(ModItems.powder_uranium, 6), out(ModItems.nugget_ra226, 4), out(ModItems.powder_lithium_tiny, 3));
		crystal(ModItems.crystal_cobalt, out(ModItems.powder_cobalt, 3), out(ModItems.powder_iron, 4), out(ModItems.powder_copper, 4), out(ModItems.powder_lithium_tiny, 3));

		addRecipe(new ComparableStack(ModItems.powder_soot, 4), new FluidStack(ModForgeFluids.electrosolvent, 4_000), 600, out(ModItems.powder_fullerene, 1));

		for(Entry<Integer, String> entry : BedrockOreRegistry.oreIndexes.entrySet()) {
			int oreMeta = entry.getKey();
			addRecipe(new ComparableStack(ModItems.ore_bedrock_perfect, 1, oreMeta), acid(), 600,
					new ItemStack(ModItems.ore_bedrock_enriched, 1, oreMeta),
					new ItemStack(ModItems.ore_bedrock_enriched, 1, oreMeta),
					CentrifugeRecipes.getNugget(entry.getValue()),
					new ItemStack(Blocks.GRAVEL, 1));
		}
	}

	private static void crystal(Item crystal, ItemStack... outputs) {
		addRecipe(new ComparableStack(crystal), acid(), 600, outputs);
	}

	private static FluidStack acid() {
		return new FluidStack(ModForgeFluids.nitric_acid, 100);
	}

	private static ItemStack out(Item item, int count) {
		return new ItemStack(item, count);
	}

	//fluid = drawn from the metal-mode tank per operation, may be null
	public static void addRecipe(AStack input, FluidStack fluid, int duration, ItemStack... outputs) {
		if(input == null)
			throw new IllegalArgumentException("Electrolyser metal recipe has no input");
		if(outputs == null || outputs.length == 0 || outputs.length > MAX_OUTPUTS)
			throw new IllegalArgumentException("Electrolyser metal recipe needs 1 to " + MAX_OUTPUTS + " outputs");
		if((fluid != null && (fluid.getFluid() == null || fluid.amount <= 0)) || duration <= 0)
			throw new IllegalArgumentException("Electrolyser metal recipe has an invalid fluid or a non-positive duration");
		recipes.add(new ElectrolysisMetalRecipe(input, fluid, duration, outputs));
	}

	public static boolean isRecipeFluid(Fluid fluid) {
		if(fluid == null)
			return false;
		for(ElectrolysisMetalRecipe recipe : recipes)
			if(recipe.fluid != null && recipe.fluid.getFluid() == fluid)
				return true;
		return false;
	}

	public static ElectrolysisMetalRecipe getRecipe(ItemStack stack) {
		if(stack.isEmpty())
			return null;
		for(ElectrolysisMetalRecipe recipe : recipes)
			if(recipe.input.matchesRecipe(stack, true))
				return recipe;
		return null;
	}

	public static class ElectrolysisMetalRecipe {
		public final AStack input;
		public final FluidStack fluid;
		public final int duration;
		public final ItemStack[] outputs;

		public ElectrolysisMetalRecipe(AStack input, FluidStack fluid, int duration, ItemStack... outputs) {
			this.input = input;
			this.fluid = fluid;
			this.duration = duration;
			this.outputs = outputs;
		}
	}
}
