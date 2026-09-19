package com.hbm.handler.jei;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map.Entry;

import com.hbm.inventory.ElectrolyserFluidRecipes;
import com.hbm.inventory.ElectrolyserFluidRecipes.ElectrolysisRecipe;
import com.hbm.items.machine.ItemFluidIcon;
import com.hbm.lib.RefStrings;
import com.hbm.util.I18nUtil;

import mezz.jei.api.IGuiHelper;
import mezz.jei.api.gui.IDrawable;
import mezz.jei.api.gui.IGuiItemStackGroup;
import mezz.jei.api.gui.IRecipeLayout;
import mezz.jei.api.ingredients.IIngredients;
import mezz.jei.api.ingredients.VanillaTypes;
import mezz.jei.api.recipe.IRecipeCategory;
import mezz.jei.api.recipe.IRecipeWrapper;
import net.minecraft.client.Minecraft;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;

//gui_nei_one_two (one_one when the recipe has no second output), 108x18 crop at (34,34): input x=18, outputs x=72/90
public class ElectrolyserFluidRecipeHandler implements IRecipeCategory<ElectrolyserFluidRecipeHandler.Wrapper> {

	private static IDrawable oneOne;
	private static IDrawable oneTwo;
	protected final IDrawable background;

	public ElectrolyserFluidRecipeHandler(IGuiHelper help) {
		background = help.createBlankDrawable(108, 18);
		oneOne = help.createDrawable(new ResourceLocation(RefStrings.MODID + ":textures/gui/jei/gui_nei_one_one.png"), 34, 34, 108, 18);
		oneTwo = help.createDrawable(new ResourceLocation(RefStrings.MODID + ":textures/gui/jei/gui_nei_one_two.png"), 34, 34, 108, 18);
	}

	public static List<Wrapper> getRecipes() {
		List<Wrapper> list = new ArrayList<>();
		for(Entry<net.minecraftforge.fluids.Fluid, ElectrolysisRecipe> entry : ElectrolyserFluidRecipes.recipes.entrySet()) {
			ElectrolysisRecipe recipe = entry.getValue();
			List<ItemStack> outputs = new ArrayList<>();
			outputs.add(ItemFluidIcon.getStackWithQuantity(recipe.output1));
			if(recipe.output2 != null)
				outputs.add(ItemFluidIcon.getStackWithQuantity(recipe.output2));
			list.add(new Wrapper(ItemFluidIcon.getStackWithQuantity(entry.getKey(), recipe.amount), outputs));
		}
		return list;
	}

	@Override
	public String getUid() {
		return JEIConfig.ELECTROLYSIS_FLUID;
	}

	@Override
	public String getTitle() {
		return I18nUtil.resolveKey("tile.machine_electrolyser.name");
	}

	@Override
	public String getModName() {
		return RefStrings.MODID;
	}

	@Override
	public IDrawable getBackground() {
		return background;
	}

	@Override
	public void setRecipe(IRecipeLayout recipeLayout, Wrapper wrapper, IIngredients ingredients) {
		IGuiItemStackGroup guiItemStacks = recipeLayout.getItemStacks();
		guiItemStacks.init(0, true, 18, 0);
		guiItemStacks.set(0, wrapper.input);
		for(int i = 0; i < wrapper.outputs.size(); i++) {
			guiItemStacks.init(1 + i, false, 72 + i * 18, 0);
			guiItemStacks.set(1 + i, wrapper.outputs.get(i));
		}
	}

	public static class Wrapper implements IRecipeWrapper {

		private final ItemStack input;
		private final List<ItemStack> outputs;

		public Wrapper(ItemStack input, List<ItemStack> outputs) {
			this.input = input;
			this.outputs = outputs;
		}

		@Override
		public void getIngredients(IIngredients ingredients) {
			ingredients.setInputs(VanillaTypes.ITEM, Arrays.asList(input));
			ingredients.setOutputs(VanillaTypes.ITEM, outputs);
		}

		@Override
		public void drawInfo(Minecraft minecraft, int recipeWidth, int recipeHeight, int mouseX, int mouseY) {
			(outputs.size() >= 2 ? oneTwo : oneOne).draw(minecraft);
		}
	}
}
