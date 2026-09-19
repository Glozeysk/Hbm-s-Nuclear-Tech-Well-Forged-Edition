package com.hbm.handler.jei;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import com.hbm.config.BedrockOreJsonConfig;
import com.hbm.inventory.BedrockOreRegistry;
import com.hbm.items.ModItems;
import com.hbm.items.special.ItemBedrockOre;
import com.hbm.main.MainRegistry;

import mezz.jei.api.IRecipeRegistry;
import mezz.jei.api.ingredients.IIngredientBlacklist;
import mezz.jei.api.ingredients.IIngredients;
import mezz.jei.api.ingredients.VanillaTypes;
import mezz.jei.api.recipe.IIngredientType;
import mezz.jei.api.recipe.IRecipeCategory;
import mezz.jei.api.recipe.IRecipeWrapper;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

//Removes every processing stage of bedrock ores that no dimension in hbm_bedrock_ores.json can spawn from JEI, including the recipes they appear in
public class DisabledBedrockOres {

	private static Set<Integer> disabledMetas;

	public static Set<Integer> getDisabledMetas() {
		if(disabledMetas == null) {
			disabledMetas = new HashSet<>();
			for(Entry<Integer, String> entry : BedrockOreRegistry.oreIndexes.entrySet())
				if(!BedrockOreJsonConfig.canSpawnAnywhere(entry.getValue()))
					disabledMetas.add(entry.getKey());
		}
		return disabledMetas;
	}

	public static boolean isDisabled(ItemStack stack) {
		return !stack.isEmpty() && stack.getItem() instanceof ItemBedrockOre && getDisabledMetas().contains(stack.getMetadata());
	}

	public static void blacklistItems(IIngredientBlacklist blacklist) {
		for(Item item : ModItems.ALL_ITEMS) {
			if(!(item instanceof ItemBedrockOre))
				continue;
			for(int meta : getDisabledMetas())
				blacklist.addIngredientToBlacklist(new ItemStack(item, 1, meta));
		}
	}

	//a recipe goes when one of its item slots holds nothing but disabled ore; oredict slots with other options stay
	@SuppressWarnings({"unchecked", "rawtypes"})
	public static void hideRecipes(IRecipeRegistry registry) {
		if(getDisabledMetas().isEmpty())
			return;

		int hidden = 0;
		//both lists are live views: hiding a recipe can change them, so iterate over copies
		List<IRecipeCategory> categories = new ArrayList<>(registry.getRecipeCategories());
		for(IRecipeCategory category : categories) {
			List<IRecipeWrapper> wrappers = new ArrayList<>(registry.getRecipeWrappers(category));
			for(IRecipeWrapper wrapper : wrappers) {
				Collector ingredients = new Collector();
				try {
					wrapper.getIngredients(ingredients);
				} catch(Exception ex) {
					continue;
				}
				if(hasOnlyDisabled(ingredients.getInputs(VanillaTypes.ITEM)) || hasOnlyDisabled(ingredients.getOutputs(VanillaTypes.ITEM))) {
					registry.hideRecipe(wrapper, category.getUid());
					hidden++;
				}
			}
		}
		MainRegistry.logger.info("Hid " + hidden + " JEI recipes of disabled bedrock ores");
	}

	private static boolean hasOnlyDisabled(List<List<ItemStack>> slots) {
		if(slots == null)
			return false;
		for(List<ItemStack> slot : slots) {
			if(slot == null || slot.isEmpty())
				continue;
			boolean allDisabled = true;
			for(ItemStack stack : slot) {
				if(stack == null || !isDisabled(stack)) {
					allDisabled = false;
					break;
				}
			}
			if(allDisabled)
				return true;
		}
		return false;
	}

	//minimal IIngredients that just records what a wrapper declares
	private static class Collector implements IIngredients {

		private final Map<Class<?>, List<List<?>>> inputs = new HashMap<>();
		private final Map<Class<?>, List<List<?>>> outputs = new HashMap<>();

		private static <T> List<List<?>> lists(List<T> flat) {
			List<List<?>> out = new ArrayList<>();
			if(flat != null)
				for(T t : flat) {
					List<T> single = new ArrayList<>();
					single.add(t);
					out.add(single);
				}
			return out;
		}

		@SuppressWarnings("unchecked")
		private static <T> List<List<T>> get(Map<Class<?>, List<List<?>>> map, Class<? extends T> clazz) {
			List<List<?>> list = map.get(clazz);
			List<List<T>> out = new ArrayList<>();
			if(list != null)
				for(List<?> l : list)
					out.add((List<T>) l);
			return out;
		}

		@SuppressWarnings({"unchecked", "rawtypes"})
		private static <T> List<List<?>> copy(List<List<T>> nested) {
			return nested == null ? new ArrayList<>() : new ArrayList<>((List) nested);
		}

		@Override public <T> void setInput(IIngredientType<T> type, T input) { setInputs(type.getIngredientClass(), java.util.Collections.singletonList(input)); }
		@Override public <T> void setInputs(IIngredientType<T> type, List<T> input) { setInputs(type.getIngredientClass(), input); }
		@Override public <T> void setInputLists(IIngredientType<T> type, List<List<T>> input) { setInputLists(type.getIngredientClass(), input); }
		@Override public <T> void setOutput(IIngredientType<T> type, T output) { setOutputs(type.getIngredientClass(), java.util.Collections.singletonList(output)); }
		@Override public <T> void setOutputs(IIngredientType<T> type, List<T> output) { setOutputs(type.getIngredientClass(), output); }
		@Override public <T> void setOutputLists(IIngredientType<T> type, List<List<T>> output) { setOutputLists(type.getIngredientClass(), output); }
		@Override public <T> List<List<T>> getInputs(IIngredientType<T> type) { return get(inputs, type.getIngredientClass()); }
		@Override public <T> List<List<T>> getOutputs(IIngredientType<T> type) { return get(outputs, type.getIngredientClass()); }

		@Override public <T> void setInput(Class<? extends T> clazz, T input) { setInputs(clazz, java.util.Collections.singletonList(input)); }
		@Override public <T> void setInputs(Class<? extends T> clazz, List<T> input) { inputs.put(clazz, lists(input)); }
		@Override public <T> void setInputLists(Class<? extends T> clazz, List<List<T>> input) { inputs.put(clazz, copy(input)); }
		@Override public <T> void setOutput(Class<? extends T> clazz, T output) { setOutputs(clazz, java.util.Collections.singletonList(output)); }
		@Override public <T> void setOutputs(Class<? extends T> clazz, List<T> output) { outputs.put(clazz, lists(output)); }
		@Override public <T> void setOutputLists(Class<? extends T> clazz, List<List<T>> output) { outputs.put(clazz, copy(output)); }
		@Override public <T> List<List<T>> getInputs(Class<? extends T> clazz) { return get(inputs, clazz); }
		@Override public <T> List<List<T>> getOutputs(Class<? extends T> clazz) { return get(outputs, clazz); }
	}
}
