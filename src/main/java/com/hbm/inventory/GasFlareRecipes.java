package com.hbm.inventory;

import com.hbm.forgefluid.ModForgeFluids;
import com.hbm.items.machine.ItemFlareCatalyst;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidRegistry;

import java.util.ArrayList;
import java.util.List;

public class GasFlareRecipes {

    public static class Recipe {
        public final Fluid inputFluid;
        public final int inputAmount; // Скорость сжигания (мб/тик)
        public final Fluid outputFluid;
        public final int outputAmount;
        public final ItemFlareCatalyst.CatalystType requiredCatalyst;

        public Recipe(Fluid inputFluid, int inputAmount, Fluid outputFluid, int outputAmount, ItemFlareCatalyst.CatalystType requiredCatalyst) {
            this.inputFluid = inputFluid;
            this.inputAmount = inputAmount;
            this.outputFluid = outputFluid;
            this.outputAmount = outputAmount;
            this.requiredCatalyst = requiredCatalyst;
        }
    }

    public static final List<Recipe> recipes = new ArrayList<>();

    public static void addRecipe(Fluid input, int inAmount, Fluid output, int outAmount, ItemFlareCatalyst.CatalystType catalyst) {
        recipes.add(new Recipe(input, inAmount, output, outAmount, catalyst));
    }

    public static Recipe getRecipe(Fluid input, int availableAmount, ItemFlareCatalyst.CatalystType currentCatalyst) {
        if (input == null) return null;

        for (Recipe recipe : recipes) {
            // Сравниваем И по ссылке, И по имени, чтобы гарантированно найти рецепт
            boolean fluidMatches = (recipe.inputFluid == input) ||
                    (recipe.inputFluid != null && recipe.inputFluid.getName().equals(input.getName()));

            if (fluidMatches && availableAmount >= recipe.inputAmount) {
                if (recipe.requiredCatalyst == currentCatalyst) {
                    return recipe;
                }
            }
        }
        return null;
    }

    public static void registerRecipes() {
        // Пример 1: Газ с катализатором FILTER дает побочку
        addRecipe(
                ModForgeFluids.gas,
                20,
                ModForgeFluids.heavy_syngas,
                5,
                ItemFlareCatalyst.CatalystType.BARRIER
        );

        // Пример 2: Керосин БЕЗ катализатора (NONE) дает побочку (если хотите)
        // Если хотите, чтобы керосин просто горел без побочки, не добавляйте его сюда,
        // он автоматически подпадет под fallback (простое горение).
        /*
        addRecipe(
                ModForgeFluids.kerosene,
                10,
                ModForgeFluids.some_byproduct,
                2,
                ItemFlareCatalyst.CatalystType.NONE
        );
        */
    }
}