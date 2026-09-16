package com.hbm.handler.jei;

import java.util.List;

import com.hbm.handler.jei.JeiRecipes.PyroOvenRecipe;
import com.hbm.lib.RefStrings;
import com.hbm.util.I18nUtil;

import mezz.jei.api.IGuiHelper;
import mezz.jei.api.gui.IDrawable;
import mezz.jei.api.gui.IGuiItemStackGroup;
import mezz.jei.api.gui.IRecipeLayout;
import mezz.jei.api.ingredients.IIngredients;
import mezz.jei.api.recipe.IRecipeCategory;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;

//The background art follows the component count (gui_nei_one_one / one_two / two_one / two_two).
//All four share the 108x18 crop at (34,34): paired cells at x 0/18 and 72/90, a single cell at 18 (input) or 72 (output).
public class PyroOvenRecipeHandler implements IRecipeCategory<PyroOvenRecipe> {

	private static final String[] NAMES = {"one_one", "one_two", "two_one", "two_two"};
	private static final IDrawable[] LAYOUTS = new IDrawable[4];

	protected final IDrawable background;

	public PyroOvenRecipeHandler(IGuiHelper help) {
		background = help.createBlankDrawable(108, 18);
		for(int i = 0; i < 4; i++)
			LAYOUTS[i] = help.createDrawable(new ResourceLocation(RefStrings.MODID + ":textures/gui/jei/gui_nei_" + NAMES[i] + ".png"), 34, 34, 108, 18);
	}

	public static IDrawable getLayout(int inputs, int outputs) {
		return LAYOUTS[(inputs >= 2 ? 2 : 0) + (outputs >= 2 ? 1 : 0)];
	}

	@Override
	public String getUid() {
		return JEIConfig.PYROLYSIS;
	}

	@Override
	public String getTitle() {
		return I18nUtil.resolveKey("tile.machine_pyrooven.name");
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
	public void setRecipe(IRecipeLayout recipeLayout, PyroOvenRecipe recipeWrapper, IIngredients ingredients) {
		IGuiItemStackGroup guiItemStacks = recipeLayout.getItemStacks();
		List<List<ItemStack>> inputs = recipeWrapper.inputs;
		List<List<ItemStack>> outputs = recipeWrapper.outputs;

		int slot = 0;
		for(int i = 0; i < inputs.size(); i++) {
			guiItemStacks.init(slot, true, inputs.size() >= 2 ? i * 18 : 18, 0);
			guiItemStacks.set(slot++, inputs.get(i));
		}
		for(int i = 0; i < outputs.size(); i++) {
			guiItemStacks.init(slot, false, 72 + i * 18, 0);
			guiItemStacks.set(slot++, outputs.get(i));
		}
	}
}
