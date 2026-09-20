package com.hbm.handler.jei;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.hbm.items.ModItems;
import com.hbm.items.machine.ItemFluidIcon;
import com.hbm.lib.RefStrings;
import com.hbm.tileentity.machine.oil.TileEntityMachineGasFlare;
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

//Soot byproduct of burning petroleum in the flare stack; gui_nei_one_one, 108x18 crop at (34,34): input x=18, output x=72
public class GasFlareRecipeHandler implements IRecipeCategory<GasFlareRecipeHandler.Wrapper> {

	protected final IDrawable background;

	public GasFlareRecipeHandler(IGuiHelper help) {
		background = help.createDrawable(new ResourceLocation(RefStrings.MODID + ":textures/gui/jei/gui_nei_one_one.png"), 34, 34, 108, 18);
	}

	public static List<Wrapper> getRecipes() {
		List<Wrapper> list = new ArrayList<>();
		for(Fluid fluid : TileEntityMachineGasFlare.getSootFluids()) {
			int cost = TileEntityMachineGasFlare.getSootCost(fluid);
			if(cost > 0)
				list.add(new Wrapper(ItemFluidIcon.getStackWithQuantity(fluid, cost), new ItemStack(ModItems.powder_soot)));
		}
		return list;
	}

	@Override
	public String getUid() {
		return JEIConfig.GAS_FLARE;
	}

	@Override
	public String getTitle() {
		return I18nUtil.resolveKey("tile.machine_flare.name");
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
		guiItemStacks.init(1, false, 72, 0);
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
