package com.hbm.handler.jei;

import com.hbm.handler.jei.JeiRecipes.AssemblerRecipeWrapper;
import com.hbm.lib.RefStrings;
import com.hbm.util.I18nUtil;
import mezz.jei.api.IGuiHelper;
import mezz.jei.api.gui.IDrawable;
import mezz.jei.api.gui.IDrawableAnimated;
import mezz.jei.api.gui.IDrawableStatic;
import mezz.jei.api.gui.IGuiItemStackGroup;
import mezz.jei.api.gui.IRecipeLayout;
import mezz.jei.api.gui.IDrawableAnimated.StartDirection;
import mezz.jei.api.ingredients.IIngredients;
import mezz.jei.api.recipe.IRecipeCategory;
import net.minecraft.client.Minecraft;
import net.minecraft.util.ResourceLocation;

public class AssemblerRecipeHandler implements IRecipeCategory<AssemblerRecipeWrapper> {

	public static final ResourceLocation gui_rl = new ResourceLocation(RefStrings.MODID, "textures/gui/jei/gui_nei_assembler.png");

	public static final int[] inputSlots = {0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11};
	public static final int outputSlot = 13;
	public static final int templateSlot = 12;
	public static final int fluidSlot = 14;

	protected final IDrawable background;
	protected final IDrawableStatic staticArrow;
	protected final IDrawableAnimated animatedArrow;
	protected final IDrawableStatic staticPower;
	protected final IDrawableAnimated animatedPower;

	public AssemblerRecipeHandler(IGuiHelper help) {
		background = help.createDrawable(gui_rl, 6, 15, 156, 56);
		staticArrow = help.createDrawable(gui_rl, 16, 86, 36, 18);
		animatedArrow = help.createAnimatedDrawable(staticArrow, 48, StartDirection.LEFT, false);
		staticPower = help.createDrawable(gui_rl, 0, 86, 16, 52);
		animatedPower = help.createAnimatedDrawable(staticPower, 480, StartDirection.TOP, true);
	}

	@Override public String getUid() { return JEIConfig.ASSEMBLY; }
	@Override public String getTitle() { return I18nUtil.resolveKey("tile.machine_assembler.name"); }
	@Override public String getModName() { return RefStrings.MODID; }
	@Override public IDrawable getBackground() { return background; }
	@Override public IDrawable getIcon() { return IRecipeCategory.super.getIcon(); }

	@Override
	public void drawExtras(Minecraft minecraft) {
		animatedArrow.draw(minecraft, 100, 19);
		animatedPower.draw(minecraft, 2, 2);
	}

	@Override
	public void setRecipe(IRecipeLayout recipeLayout, AssemblerRecipeWrapper recipeWrapper, IIngredients ingredients) {
		IGuiItemStackGroup guiItemStacks = recipeLayout.getItemStacks();

		for(int i = 0; i < 12; i++) {
			guiItemStacks.init(inputSlots[i], true, 28 + (i % 4) * 18, 1 + (i / 4) * 18);
		}

		guiItemStacks.init(templateSlot, true, 109, 1);
		guiItemStacks.init(outputSlot, false, 136, 19);

		guiItemStacks.init(fluidSlot, true, 100, 37);

		guiItemStacks.set(ingredients);
	}
}