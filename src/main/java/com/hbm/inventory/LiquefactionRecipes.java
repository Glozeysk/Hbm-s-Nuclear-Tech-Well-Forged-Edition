package com.hbm.inventory;

import java.util.LinkedHashMap;
import java.util.Map;

import com.hbm.forgefluid.ModForgeFluids;
import com.hbm.inventory.RecipesCommon.ComparableStack;
import com.hbm.items.ModItems;

import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.oredict.OreDictionary;

//Ported from NTM:CE (LiquefactionRecipes), one item in, one fluid out
public class LiquefactionRecipes {

	public static Map<ComparableStack, FluidStack> recipes = new LinkedHashMap<ComparableStack, FluidStack>();

	public static void register() {
		recipes.clear();

		addRecipe(new ComparableStack(Items.SUGAR), ModForgeFluids.ethanol, 100);
		addRecipe(new ComparableStack(Items.MELON), ModForgeFluids.ethanol, 100);
		addRecipe(new ComparableStack(Items.FISH, 1, OreDictionary.WILDCARD_VALUE), ModForgeFluids.fishoil, 100);
		addRecipe(new ComparableStack(Blocks.DOUBLE_PLANT, 1, 0), ModForgeFluids.sunfloweroil, 100);
		addRecipe(new ComparableStack(ModItems.biomass), ModForgeFluids.biogas, 125);
		addRecipe(new ComparableStack(ModItems.oil_tar), ModForgeFluids.bitumen, 75);

		addRecipe(new ComparableStack(Items.SNOWBALL), FluidRegistry.WATER, 125);
		addRecipe(new ComparableStack(Blocks.SNOW), FluidRegistry.WATER, 500);
		addRecipe(new ComparableStack(Blocks.ICE), FluidRegistry.WATER, 1000);
		addRecipe(new ComparableStack(Blocks.PACKED_ICE), FluidRegistry.WATER, 1000);

		addRecipe(new ComparableStack(Blocks.COBBLESTONE), FluidRegistry.LAVA, 250);
		addRecipe(new ComparableStack(Blocks.STONE), FluidRegistry.LAVA, 250);
		addRecipe(new ComparableStack(Blocks.NETHERRACK), FluidRegistry.LAVA, 250);
		addRecipe(new ComparableStack(Blocks.OBSIDIAN), FluidRegistry.LAVA, 500);
	}

	public static void addRecipe(ComparableStack input, Fluid fluid, int amount) {
		recipes.put(input, new FluidStack(fluid, amount));
	}

	/** exact item and meta first, then a wildcard entry for the item */
	public static FluidStack getOutput(ItemStack stack) {

		if(stack == null || stack.isEmpty())
			return null;

		for(Map.Entry<ComparableStack, FluidStack> entry : recipes.entrySet()) {
			ComparableStack key = entry.getKey();
			if(key.item == stack.getItem() && (key.meta == OreDictionary.WILDCARD_VALUE || key.meta == stack.getItemDamage()))
				return entry.getValue();
		}

		return null;
	}
}
