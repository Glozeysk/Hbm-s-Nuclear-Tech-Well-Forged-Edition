package com.hbm.inventory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.hbm.items.ModItems;
import com.hbm.inventory.RecipesCommon.AStack;
import com.hbm.inventory.RecipesCommon.ComparableStack;
import com.hbm.inventory.RecipesCommon.OreDictStack;

import net.minecraft.item.ItemStack;

import static com.hbm.inventory.OreDictManager.*;

//Ported from NTM:CE (ArcWelderRecipes). Recipes are written in ticks and HE per tick, JEI converts both to seconds.
public class ArcWelderRecipes {

	/** a recipe can be welded by its own process and by every later one, never by an earlier one */
	public static enum WeldType {

		MMA("MMA", 200_000L),
		TIG("TIG", 5_000_000L),
		VAW("VAW", 20_000_000L),
		EBW("EBW", 4_000_000_000L);

		public final String label;
		/** each process runs off its own buffer, switching down clips whatever does not fit */
		public final long maxPower;

		private WeldType(String label, long maxPower) {
			this.label = label;
			this.maxPower = maxPower;
		}
	}

	public static List<ArcWelderRecipe> recipes = new ArrayList<ArcWelderRecipe>();

	public static void register() {
		recipes.clear();

		//welded plates, three of the material's own plates (or ingots where no plate exists) make one
		addRecipe(WeldType.MMA, new ItemStack(ModItems.plate_welded_iron), 100, 100, new OreDictStack(IRON.plate(), 2));
		addRecipe(WeldType.MMA, new ItemStack(ModItems.plate_welded_steel), 100, 500, new OreDictStack(STEEL.plate(), 2));
		addRecipe(WeldType.MMA, new ItemStack(ModItems.plate_welded_copper), 200, 1_000, new OreDictStack(CU.plate(), 2));
		addRecipe(WeldType.MMA, new ItemStack(ModItems.plate_welded_aluminium), 300, 2_500, new OreDictStack(AL.plate(), 2));

		addRecipe(WeldType.TIG, new ItemStack(ModItems.plate_welded_titanium), 600, 2_500, new OreDictStack(TI.plate(), 2));
		addRecipe(WeldType.TIG, new ItemStack(ModItems.plate_welded_advanced_alloy), 300, 10_000, new OreDictStack(ALLOY.plate(), 2));
		addRecipe(WeldType.TIG, new ItemStack(ModItems.plate_desh, 4), 300, 20_000, new OreDictStack(DESH.ingot(), 4), new OreDictStack(ANY_PLASTIC.dust(), 2), new OreDictStack(DURA.ingot(), 1));
		addRecipe(WeldType.TIG, new ItemStack(ModItems.plate_welded_zirconium), 600, 10_000, new OreDictStack(ZR.ingot(), 2));
		addRecipe(WeldType.TIG, new ItemStack(ModItems.plate_welded_niobium), 600, 20_000, new OreDictStack(NB.ingot(), 2));

		addRecipe(WeldType.VAW, new ItemStack(ModItems.plate_welded_saturnite), 600, 50_000, new OreDictStack(BIGMT.plate(), 2));
		addRecipe(WeldType.VAW, new ItemStack(ModItems.plate_welded_tcalloy), 1200, 100_000, new OreDictStack(TCALLOY.ingot(), 2));
		addRecipe(WeldType.VAW, new ItemStack(ModItems.plate_welded_cdalloy), 1200, 100_000, new OreDictStack(CDALLOY.ingot(), 2));

		addRecipe(WeldType.EBW, new ItemStack(ModItems.plate_welded_combine_steel), 1200, 2_000_000, new OreDictStack(CMB.plate(), 2));
		addRecipe(WeldType.EBW, new ItemStack(ModItems.plate_dineutronium, 4), 3000, 10_000_000, new OreDictStack(DNT.ingot(), 4), new ComparableStack(ModItems.powder_spark_mix, 2), new OreDictStack(DESH.ingot(), 1));
		addRecipe(WeldType.EBW, new ItemStack(ModItems.plate_welded_osmiridium), 6000, 20_000_000, new OreDictStack(OSMIRIDIUM.ingot(), 2));
	}

	public static void addRecipe(WeldType type, ItemStack output, int ticks, long powerPerTick, AStack... inputs) {
		recipes.add(new ArcWelderRecipe(type, output, ticks, powerPerTick, inputs));
	}

	/** the machine has three input slots, so the recipe is found by whatever sits in them */
	public static ArcWelderRecipe getRecipe(ItemStack... inputs) {

		outer:
		for(ArcWelderRecipe recipe : recipes) {

			List<AStack> remaining = new ArrayList<AStack>(Arrays.asList(recipe.inputs));

			for(ItemStack input : inputs) {

				if(input.isEmpty())
					continue;

				boolean match = false;

				for(AStack aStack : remaining) {
					if(aStack.matchesRecipe(input, true) && input.getCount() >= aStack.count()) {
						match = true;
						remaining.remove(aStack);
						break;
					}
				}

				if(!match)
					continue outer;
			}

			if(remaining.isEmpty())
				return recipe;
		}

		return null;
	}

	public static class ArcWelderRecipe {

		public final WeldType type;
		public final AStack[] inputs;
		public final ItemStack output;
		public final int duration;
		public final long consumption;

		public ArcWelderRecipe(WeldType type, ItemStack output, int duration, long consumption, AStack... inputs) {
			this.type = type;
			this.output = output;
			this.duration = duration;
			this.consumption = consumption;
			this.inputs = inputs;
		}

		public int getSeconds() {
			return duration / 20;
		}

		public long getPowerPerSecond() {
			return consumption * 20;
		}
	}
}
