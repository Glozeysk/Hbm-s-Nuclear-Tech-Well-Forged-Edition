package com.hbm.handler.jei;

import java.util.ArrayList;
import java.util.List;

import com.hbm.inventory.RecipesCommon.AStack;
import com.hbm.inventory.SolderingRecipes;
import com.hbm.inventory.SolderingRecipes.SolderingRecipe;
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

//Same idea as the arc welder category: the slot layout follows the ingredient count (inputs and fluid together, at most six).
//The lead wire solder is burnt by every recipe and is not shown.
//The two-row layouts are 36px tall, so the single-row ones are drawn 9px lower to keep every output cell in the same place.
public class SolderingStationRecipeHandler implements IRecipeCategory<SolderingStationRecipeHandler.Wrapper> {

	private static final int WIDTH = 108;
	private static final int HEIGHT = 54;
	private static final int MAX = 6;

	private static final String[] NAMES = {"one_one", "two_one", "three_to_one", "four_one", "five_one", "six_one"};
	private static final IDrawable[] LAYOUTS = new IDrawable[MAX];

	/** [ingredient count - 1][slot] = {x, y}; every layout spans the full width: inputs from x=0, the output at x=90 */
	private static final int[][][] SLOTS = {
			{ {0, 9} },
			{ {0, 9}, {18, 9} },
			{ {0, 9}, {18, 9}, {36, 9} },
			{ {0, 0}, {18, 0}, {0, 18}, {18, 18} },
			{ {0, 0}, {18, 0}, {36, 0}, {9, 18}, {27, 18} },
			{ {0, 0}, {18, 0}, {36, 0}, {0, 18}, {18, 18}, {36, 18} }
	};

	protected final IDrawable background;

	public SolderingStationRecipeHandler(IGuiHelper help) {
		background = help.createBlankDrawable(WIDTH, HEIGHT);
		for(int i = 0; i < MAX; i++) {
			ResourceLocation tex = new ResourceLocation(RefStrings.MODID + ":textures/gui/jei/gui_nei_" + NAMES[i] + ".png");
			//one_one, two_one and four_one have their output one cell further left in the sheet (x=106 instead of 124), so their crop starts one cell earlier
			int u = i == 0 || i == 1 || i == 3 ? 16 : 34;
			LAYOUTS[i] = i < 3 ? help.createDrawable(tex, u, 34, WIDTH, 18) : help.createDrawable(tex, u, 25, WIDTH, 36);
		}
	}

	private static int layoutOf(int count) {
		return Math.min(Math.max(count, 1), MAX) - 1;
	}

	public static List<Wrapper> getRecipes() {
		List<Wrapper> list = new ArrayList<>();
		for(SolderingRecipe recipe : SolderingRecipes.recipes)
			list.add(new Wrapper(recipe));
		return list;
	}

	@Override
	public String getUid() {
		return JEIConfig.SOLDERING_STATION;
	}

	@Override
	public String getTitle() {
		return I18nUtil.resolveKey("tile.machine_soldering_station.name");
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

		int count = Math.min(wrapper.inputs.size(), MAX);
		int[][] slots = SLOTS[layoutOf(count)];

		for(int i = 0; i < count; i++)
			guiItemStacks.init(i, true, slots[i][0], slots[i][1]);

		guiItemStacks.init(MAX, false, 90, 9);
		guiItemStacks.set(ingredients);
	}

	public static class Wrapper implements IRecipeWrapper {

		private final List<List<ItemStack>> inputs = new ArrayList<>();
		private final ItemStack output;
		private final String duration;
		private final String consumption;

		public Wrapper(SolderingRecipe recipe) {

			//an ore dict input has to offer every variant, so each slot gets its own list
			for(AStack input : recipe.inputs)
				inputs.add(input.getStackList());

			if(recipe.fluid != null) {
				List<ItemStack> fluid = new ArrayList<>();
				fluid.add(ItemFluidIcon.getStackWithQuantity(recipe.fluid));
				inputs.add(fluid);
			}

			this.output = recipe.output;
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
			int layout = layoutOf(inputs.size());
			LAYOUTS[layout].draw(mc, 0, layout < 3 ? 9 : 0);
			//both lines hug the right edge under the slots
			mc.fontRenderer.drawString(duration, WIDTH - mc.fontRenderer.getStringWidth(duration), 36, 0x404040);
			mc.fontRenderer.drawString(consumption, WIDTH - mc.fontRenderer.getStringWidth(consumption), 45, 0x404040);
		}
	}
}
