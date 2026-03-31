package com.hbm.crafting;

import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.item.crafting.Ingredient;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.NonNullList;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import net.minecraftforge.registries.IForgeRegistryEntry;

public class RecipeArmorConversion extends IForgeRegistryEntry.Impl<IRecipe> implements IRecipe {

    private final Item inputItem;
    private final Item outputItem;
    private final Ingredient inputIngredient;

    public RecipeArmorConversion(Item input, Item output, ResourceLocation registryName) {
        this.inputItem = input;
        this.outputItem = output;
        this.inputIngredient = Ingredient.fromItem(input);
        this.setRegistryName(registryName);
    }

    @Override
    public boolean matches(InventoryCrafting inv, World worldIn) {
        ItemStack foundInput = ItemStack.EMPTY;
        int itemCount = 0;

        for (int i = 0; i < inv.getSizeInventory(); i++) {
            ItemStack stack = inv.getStackInSlot(i);
            if (!stack.isEmpty()) {
                itemCount++;
                if (stack.getItem() == inputItem) {
                    foundInput = stack;
                } else {
                    return false;
                }
            }
        }

        return itemCount == 1 && !foundInput.isEmpty();
    }

    @Override
    public ItemStack getCraftingResult(InventoryCrafting inv) {
        ItemStack inputStack = ItemStack.EMPTY;

        for (int i = 0; i < inv.getSizeInventory(); i++) {
            ItemStack stack = inv.getStackInSlot(i);
            if (!stack.isEmpty() && stack.getItem() == inputItem) {
                inputStack = stack;
                break;
            }
        }

        if (inputStack.isEmpty()) {
            return ItemStack.EMPTY;
        }

        ItemStack result = new ItemStack(outputItem);

        if (inputStack.hasTagCompound()) {
            result.setTagCompound(inputStack.getTagCompound().copy());
        }

        return result;
    }

    @Override
    public boolean canFit(int width, int height) {
        return width * height >= 1;
    }

    @Override
    public ItemStack getRecipeOutput() {
        return new ItemStack(outputItem);
    }

    @Override
    public NonNullList<ItemStack> getRemainingItems(InventoryCrafting inv) {
        return NonNullList.withSize(inv.getSizeInventory(), ItemStack.EMPTY);
    }

    @Override
    public boolean isDynamic() {
        return false;
    }

    @Override
    public NonNullList<Ingredient> getIngredients() {
        NonNullList<Ingredient> ingredients = NonNullList.create();
        ingredients.add(this.inputIngredient);
        return ingredients;
    }

    public Item getInputItem() {
        return inputItem;
    }

    public Item getOutputItem() {
        return outputItem;
    }
}