package com.hbm.handler.jei;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.hbm.inventory.ElectrolyserMetalRecipes;
import com.hbm.inventory.ElectrolyserMetalRecipes.ElectrolysisMetalRecipe;
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

//gui_nei_two_four (gui_nei_one_four without a fluid), 108x36 crop at (34,25): fluid at (0,9), item at (18,9), outputs 2x2 at x=72/90, y=0/18
public class ElectrolyserMetalRecipeHandler implements IRecipeCategory<ElectrolyserMetalRecipeHandler.Wrapper> {

	private static IDrawable oneFour;
	private static IDrawable twoFour;
	protected final IDrawable background;

	public ElectrolyserMetalRecipeHandler(IGuiHelper help) {
		background = help.createBlankDrawable(108, 36);
		oneFour = help.createDrawable(new ResourceLocation(RefStrings.MODID + ":textures/gui/jei/gui_nei_one_four.png"), 34, 25, 108, 36);
		twoFour = help.createDrawable(new ResourceLocation(RefStrings.MODID + ":textures/gui/jei/gui_nei_two_four.png"), 34, 25, 108, 36);
	}

	public static List<Wrapper> getRecipes() {
		List<Wrapper> list = new ArrayList<>();
		for(ElectrolysisMetalRecipe recipe : ElectrolyserMetalRecipes.recipes) {
			List<ItemStack> outputs = new ArrayList<>();
			for(ItemStack out : recipe.outputs)
				outputs.add(out.copy());
			ItemStack fluid = recipe.fluid != null ? ItemFluidIcon.getStackWithQuantity(recipe.fluid) : ItemStack.EMPTY;
			list.add(new Wrapper(fluid, recipe.input.getStackList(), outputs));
		}
		return list;
	}

	@Override
	public String getUid() {
		return JEIConfig.ELECTROLYSIS_METAL;
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
		int slot = 0;
		if(!wrapper.fluid.isEmpty()) {
			guiItemStacks.init(slot, true, 0, 9);
			guiItemStacks.set(slot++, wrapper.fluid);
		}
		guiItemStacks.init(slot, true, 18, 9);
		guiItemStacks.set(slot++, wrapper.input);
		for(int i = 0; i < wrapper.outputs.size(); i++) {
			guiItemStacks.init(slot, false, 72 + (i % 2) * 18, (i / 2) * 18);
			guiItemStacks.set(slot++, wrapper.outputs.get(i));
		}
	}

	public static class Wrapper implements IRecipeWrapper {

		private final ItemStack fluid;
		private final List<ItemStack> input;
		private final List<ItemStack> outputs;

		public Wrapper(ItemStack fluid, List<ItemStack> input, List<ItemStack> outputs) {
			this.fluid = fluid;
			this.input = input;
			this.outputs = outputs;
		}

		@Override
		public void getIngredients(IIngredients ingredients) {
			List<List<ItemStack>> inputs = new ArrayList<>();
			if(!fluid.isEmpty())
				inputs.add(Arrays.asList(fluid));
			inputs.add(input);
			ingredients.setInputLists(VanillaTypes.ITEM, inputs);
			ingredients.setOutputs(VanillaTypes.ITEM, outputs);
		}

		//category background is blank, the slot art depends on whether the recipe uses a fluid
		@Override
		public void drawInfo(Minecraft minecraft, int recipeWidth, int recipeHeight, int mouseX, int mouseY) {
			(fluid.isEmpty() ? oneFour : twoFour).draw(minecraft);
		}
	}
}
