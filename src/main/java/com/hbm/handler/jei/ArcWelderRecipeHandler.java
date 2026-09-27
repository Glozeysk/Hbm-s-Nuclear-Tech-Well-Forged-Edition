package com.hbm.handler.jei;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.hbm.inventory.ArcWelderRecipes;
import com.hbm.inventory.ArcWelderRecipes.ArcWelderRecipe;
import com.hbm.inventory.RecipesCommon.AStack;
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

//gui_nei_three_to_one cropped taller than usual so the duration and the power draw fit under the slot row
public class ArcWelderRecipeHandler implements IRecipeCategory<ArcWelderRecipeHandler.Wrapper> {

	private static final int WIDTH = 108;
	private static final int HEIGHT = 36;

	//the slot row follows the input count, everything below it is text
	private static final String[] NAMES = {"one_one", "two_one", "three_to_one"};
	private static final IDrawable[] LAYOUTS = new IDrawable[3];

	protected final IDrawable background;

	public ArcWelderRecipeHandler(IGuiHelper help) {
		background = help.createBlankDrawable(WIDTH, HEIGHT);
		//one_one and two_one have their output one cell further left in the sheet (x=106 instead of 124), so their crop starts one cell earlier
		for(int i = 0; i < NAMES.length; i++)
			LAYOUTS[i] = help.createDrawable(new ResourceLocation(RefStrings.MODID + ":textures/gui/jei/gui_nei_" + NAMES[i] + ".png"), i < 2 ? 16 : 34, 34, WIDTH, 18);
	}

	private static int layoutOf(int inputs) {
		return Math.min(Math.max(inputs, 1), 3) - 1;
	}

	/** every layout spans the full width: inputs from x=0, the output at x=90 */
	private static int inputX(int inputs, int index) {
		return index * 18;
	}

	private static int outputX(int inputs) {
		return 90;
	}

	public static List<Wrapper> getRecipes() {
		List<Wrapper> list = new ArrayList<>();
		for(ArcWelderRecipe recipe : ArcWelderRecipes.recipes)
			list.add(new Wrapper(recipe));
		return list;
	}

	@Override
	public String getUid() {
		return JEIConfig.ARC_WELDER;
	}

	@Override
	public String getTitle() {
		return I18nUtil.resolveKey("tile.machine_arc_welder.name");
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

		int inputs = wrapper.inputs.size();

		for(int i = 0; i < inputs; i++)
			guiItemStacks.init(i, true, inputX(inputs, i), 0);

		guiItemStacks.init(3, false, outputX(inputs), 0);
		guiItemStacks.set(ingredients);
	}

	public static class Wrapper implements IRecipeWrapper {

		private final List<List<ItemStack>> inputs = new ArrayList<>();
		private final ItemStack output;
		private final String duration;
		private final String consumption;
		private final String weld;

		public Wrapper(ArcWelderRecipe recipe) {

			//an ore dict input has to offer every variant, so each slot gets its own list
			for(AStack input : recipe.inputs)
				inputs.add(input.getStackList());

			this.output = recipe.output;
			this.weld = recipe.type.label;
			this.duration = recipe.getSeconds() + " s";
			this.consumption = String.format("%,d", recipe.getPowerPerSecond()) + " HE/s";
		}

		@Override
		public void getIngredients(IIngredients ingredients) {
			ingredients.setInputLists(VanillaTypes.ITEM, inputs);
			ingredients.setOutput(VanillaTypes.ITEM, output);
		}

		@Override
		public void drawInfo(Minecraft mc, int recipeWidth, int recipeHeight, int mouseX, int mouseY) {
			LAYOUTS[layoutOf(inputs.size())].draw(mc);
			//lines up with the left edge of the first input cell, wherever the layout put it
			mc.fontRenderer.drawString(weld, inputX(inputs.size(), 0), 18, 0x404040);
			//both lines hug the right edge
			mc.fontRenderer.drawString(duration, WIDTH - mc.fontRenderer.getStringWidth(duration), 18, 0x404040);
			mc.fontRenderer.drawString(consumption, WIDTH - mc.fontRenderer.getStringWidth(consumption), 27, 0x404040);
		}
	}
}
