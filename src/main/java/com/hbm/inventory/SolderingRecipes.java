package com.hbm.inventory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.hbm.inventory.RecipesCommon.AStack;
import com.hbm.inventory.RecipesCommon.ComparableStack;
import com.hbm.inventory.RecipesCommon.OreDictStack;
import com.hbm.config.GeneralConfig;
import com.hbm.forgefluid.ModForgeFluids;
import com.hbm.items.ModItems;

import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidStack;

import static com.hbm.inventory.OreDictManager.*;

//Ported from NTM:CE (SolderingRecipes). Five shared input slots plus a solder slot; recipes are written in ticks and HE per tick.
//Every recipe burns one lead wire from the solder slot, so the solder is not part of the recipe itself.
public class SolderingRecipes {

	public static final int MAX_INPUTS = 5;
	public static final int SOLDER_PER_RECIPE = 1;

	public static List<SolderingRecipe> recipes = new ArrayList<SolderingRecipe>();

	public static void register() {
		recipes.clear();

		//circuits
		addRecipe(new ItemStack(ModItems.circuit_copper), 100, 100, null,
				new ComparableStack(ModItems.circuit_aluminium), new ComparableStack(ModItems.wire_copper, 4), new OreDictStack(SI.billet()), new OreDictStack(CU.plate()));
		addRecipe(new ItemStack(ModItems.circuit_red_copper), 200, 250, null,
				new ComparableStack(ModItems.circuit_copper), new ComparableStack(ModItems.wire_red_copper, 4), new OreDictStack(GOLD.dust()), new ComparableStack(ModItems.plate_polymer));
		addRecipe(new ItemStack(ModItems.circuit_gold), 300, 500, new FluidStack(ModForgeFluids.acid, 500),
				new ComparableStack(ModItems.circuit_red_copper), new ComparableStack(ModItems.wire_gold, 4), new OreDictStack(LAPIS.dust()), new OreDictStack(ANY_PLASTIC.ingot()));
		addRecipe(new ItemStack(ModItems.circuit_schrabidium), 600, 1_250, new FluidStack(ModForgeFluids.solvent, 1_000),
				new ComparableStack(ModItems.circuit_gold), new ComparableStack(ModItems.wire_schrabidium, 4), new OreDictStack(DIAMOND.dust()), new OreDictStack(DESH.ingot()));
		addRecipe(new ItemStack(ModItems.circuit_multi), 1_200, 5_000, new FluidStack(ModForgeFluids.radiosolvent, 4_000),
				new ComparableStack(ModItems.circuit_schrabidium), new ComparableStack(ModItems.wire_schrabidium, 16), new ComparableStack(ModItems.powder_tantalium), new OreDictStack(ANY_BISMOID.ingot()));

		//targeting chips, half the time and power of the circuit they are built from
		addRecipe(new ItemStack(ModItems.circuit_targeting_tier2), 50, 50, null,
				new ComparableStack(ModItems.circuit_copper, 2), new OreDictStack(SI.billet()));
		addRecipe(new ItemStack(ModItems.circuit_targeting_tier3), 100, 125, null,
				new ComparableStack(ModItems.circuit_red_copper, 2), new OreDictStack(GOLD.dust()));
		addRecipe(new ItemStack(ModItems.circuit_targeting_tier4), 150, 250, null,
				new ComparableStack(ModItems.circuit_gold, 2), new OreDictStack(LAPIS.dust()));
		addRecipe(new ItemStack(ModItems.circuit_targeting_tier5), 300, 625, null,
				new ComparableStack(ModItems.circuit_schrabidium, 2), new OreDictStack(DIAMOND.dust()));
		addRecipe(new ItemStack(ModItems.circuit_targeting_tier6), 600, 2_500, null,
				new ComparableStack(ModItems.circuit_multi, 2), new ComparableStack(ModItems.powder_tantalium));

		//chipset and capacitor board assemblies
		AStack insulator = GeneralConfig.enable528 ? new ComparableStack(ModItems.circuit_tantalium, 2) : new OreDictStack(ASBESTOS.ingot(), 2);
		addRecipe(new ItemStack(ModItems.circuit_bismuth_raw), 1_200, 2_500, new FluidStack(ModForgeFluids.petroil_hq, 2_000),
				new OreDictStack(REDSTONE.dust(), 4), new OreDictStack(ANY_PLASTIC.ingot(), 2), insulator, new OreDictStack(ANY_BISMOID.ingot()));
		addRecipe(new ItemStack(ModItems.circuit_arsenic_raw), 1_200, 2_500, new FluidStack(ModForgeFluids.petroil_hq, 2_000),
				new OreDictStack(REDSTONE.dust(), 4), new OreDictStack(ANY_PLASTIC.ingot(), 2), insulator, new OreDictStack(AS.ingot()));
		addRecipe(new ItemStack(ModItems.circuit_tantalium_raw), 1_200, 2_500, new FluidStack(ModForgeFluids.petroil_hq, 2_000),
				new OreDictStack(REDSTONE.dust(), 4), new ComparableStack(ModItems.wire_gold, 2), new ComparableStack(ModItems.plate_welded_copper, 2), new OreDictStack(TA.nugget()));

		//upgrades
		addRecipe(new ItemStack(ModItems.upgrade_template), 100, 100, null,
				new ComparableStack(ModItems.plate_welded_steel), new ComparableStack(ModItems.plate_welded_iron, 4), new ComparableStack(ModItems.plate_welded_copper, 2), new ComparableStack(ModItems.wire_copper, 6));

		addUpgradeTiers(ModItems.upgrade_speed_1, ModItems.upgrade_speed_2, ModItems.upgrade_speed_3, new OreDictStack(MINGRADE.dust()), new ComparableStack(Items.REDSTONE));
		addUpgradeTiers(ModItems.upgrade_effect_1, ModItems.upgrade_effect_2, ModItems.upgrade_effect_3, new OreDictStack(DURA.dust()), new OreDictStack(STEEL.dust()));
		addUpgradeTiers(ModItems.upgrade_power_1, ModItems.upgrade_power_2, ModItems.upgrade_power_3, new OreDictStack(LAPIS.dust()), new ComparableStack(Items.GLOWSTONE_DUST));
		addUpgradeTiers(ModItems.upgrade_fortune_1, ModItems.upgrade_fortune_2, ModItems.upgrade_fortune_3, new OreDictStack(DIAMOND.dust()), new OreDictStack(IRON.dust()));
		addUpgradeTiers(ModItems.upgrade_afterburn_1, ModItems.upgrade_afterburn_2, ModItems.upgrade_afterburn_3, new OreDictStack(ANY_PLASTIC.dust()), new OreDictStack(W.dust()));

		addRecipe(new ItemStack(ModItems.upgrade_radius), 200, 250, null,
				new ComparableStack(ModItems.upgrade_template), new ComparableStack(Items.GLOWSTONE_DUST, 6), new OreDictStack(DIAMOND.dust(), 4), new ComparableStack(ModItems.wire_red_copper, 8));
		addRecipe(new ItemStack(ModItems.upgrade_health), 200, 250, null,
				new ComparableStack(ModItems.upgrade_template), new ComparableStack(Items.GLOWSTONE_DUST, 6), new OreDictStack(TI.dust(), 4), new ComparableStack(ModItems.wire_red_copper, 8));
	}

	/** every tiered upgrade uses the same two resources: 4+6 to make it, 2+4 to reach tier 2, 2+6 to reach tier 3 */
	private static void addUpgradeTiers(Item tier1, Item tier2, Item tier3, AStack first, AStack second) {
		addRecipe(new ItemStack(tier1), 200, 250, null,
				new ComparableStack(ModItems.upgrade_template), withCount(first, 4), withCount(second, 6), new ComparableStack(ModItems.wire_red_copper, 8));
		addRecipe(new ItemStack(tier2), 300, 500, new FluidStack(ModForgeFluids.acid, 1_000),
				new ComparableStack(tier1), withCount(first, 2), withCount(second, 4), new ComparableStack(ModItems.circuit_red_copper, 4), new OreDictStack(ANY_PLASTIC.ingot(), 2));
		addRecipe(new ItemStack(tier3), 600, 1_250, new FluidStack(ModForgeFluids.sulfuric_acid, 1_000),
				new ComparableStack(tier2), withCount(first, 2), withCount(second, 6), new OreDictStack(DESH.ingot(), 4));
	}

	private static AStack withCount(AStack stack, int count) {
		AStack copy = stack.copy();
		copy.setCount(count);
		return copy;
	}

	/** fluid may be null; at most five inputs */
	public static void addRecipe(ItemStack output, int ticks, long powerPerTick, FluidStack fluid, AStack... inputs) {

		if(inputs.length > MAX_INPUTS)
			throw new IllegalArgumentException("Soldering recipe for " + output + " has " + inputs.length + " inputs, the machine has " + MAX_INPUTS);

		recipes.add(new SolderingRecipe(output, ticks, powerPerTick, fluid, inputs));
	}

	/** inputs are the five shared slots; every filled slot has to be used by the recipe */
	public static SolderingRecipe getRecipe(ItemStack[] inputs) {

		outer:
		for(SolderingRecipe recipe : recipes) {

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

	public static boolean isSolder(ItemStack stack) {
		return !stack.isEmpty() && stack.getItem() == ModItems.wire_lead;
	}

	public static boolean isInput(ItemStack stack) {
		for(SolderingRecipe recipe : recipes)
			for(AStack input : recipe.inputs)
				if(input.matchesRecipe(stack, true))
					return true;
		return false;
	}

	public static boolean isValidFluid(Fluid fluid) {
		for(SolderingRecipe recipe : recipes)
			if(recipe.fluid != null && recipe.fluid.getFluid() == fluid)
				return true;
		return false;
	}

	public static class SolderingRecipe {

		public final AStack[] inputs;
		public final FluidStack fluid;
		public final ItemStack output;
		public final int duration;
		public final long consumption;

		public SolderingRecipe(ItemStack output, int duration, long consumption, FluidStack fluid, AStack... inputs) {
			this.output = output;
			this.duration = duration;
			this.consumption = consumption;
			this.fluid = fluid;
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
