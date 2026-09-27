package com.hbm.handler.jei;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import com.hbm.items.machine.ItemFluidIcon;
import com.hbm.lib.RefStrings;
import com.hbm.inventory.SolidificationRecipes;
import com.hbm.inventory.SolidificationRecipes.SolidificationRecipe;
import com.hbm.util.I18nUtil;

import mezz.jei.api.IGuiHelper;
import mezz.jei.api.gui.IDrawable;
import mezz.jei.api.gui.IGuiItemStackGroup;
import mezz.jei.api.gui.IRecipeLayout;
import mezz.jei.api.ingredients.IIngredients;
import mezz.jei.api.ingredients.VanillaTypes;
import mezz.jei.api.recipe.IRecipeCategory;
import mezz.jei.api.recipe.IRecipeWrapper;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fluids.Fluid;
import net.minecraft.util.ResourceLocation;

//Solidifier: fluid in, solid item out; gui_nei_one_one, 108x18 crop at (16,34): input x=0, output x=90
public class SolidificationRecipeHandler implements IRecipeCategory<SolidificationRecipeHandler.Wrapper> {

	protected final IDrawable background;

	public SolidificationRecipeHandler(IGuiHelper help) {
		//gui_nei_one_one has its input at x=16 and output at x=106, so a crop from x=16 spans exactly input to output
		background = help.createDrawable(new ResourceLocation(RefStrings.MODID + ":textures/gui/jei/gui_nei_one_one.png"), 16, 34, 108, 18);
	}

	public static List<Wrapper> getRecipes() {
		List<Wrapper> list = new ArrayList<>();
		for(Map.Entry<Fluid, SolidificationRecipe> entry : SolidificationRecipes.recipes.entrySet())
			list.add(new Wrapper(ItemFluidIcon.getStackWithQuantity(entry.getKey(), entry.getValue().amount), entry.getValue().output));
		return list;
	}

	@Override
	public String getUid() {
		return JEIConfig.SOLIDIFICATION;
	}

	@Override
	public String getTitle() {
		return I18nUtil.resolveKey("tile.machine_solidifier.name");
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
		guiItemStacks.init(0, true, 0, 0);
		guiItemStacks.init(1, false, 90, 0);
		guiItemStacks.set(ingredients);
	}

	public static class Wrapper implements IRecipeWrapper {

		private final ItemStack input;
		private final ItemStack output;

		public Wrapper(ItemStack input, ItemStack output) {
			this.input = input;
			this.output = output;
		}

		@Override
		public void getIngredients(IIngredients ingredients) {
			ingredients.setInputs(VanillaTypes.ITEM, Arrays.asList(input));
			ingredients.setOutput(VanillaTypes.ITEM, output);
		}
	}
}
