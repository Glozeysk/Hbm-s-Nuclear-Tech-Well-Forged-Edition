package com.hbm.items.machine;

import java.util.List;

import com.hbm.items.ModItems;
import com.hbm.forgefluid.FFUtils;

import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.NonNullList;
import net.minecraft.world.World;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public class ItemFluidIcon extends Item {

	public ItemFluidIcon(String s) {
		this.setTranslationKey(s);
		this.setRegistryName(s);
		this.setHasSubtypes(true);
		this.setMaxDamage(0);
		ModItems.ALL_ITEMS.add(this);
	}

	@Override
	@SideOnly(Side.CLIENT)
	public void getSubItems(CreativeTabs tab, NonNullList<ItemStack> items) {
		if(!this.isInCreativeTab(tab))
			return;
		for(Fluid f : FluidRegistry.getRegisteredFluids().values()) {
			if(f == null)
				continue;
			ItemStack stack = getStack(f);
			if(stack.hasTagCompound() && stack.getTagCompound().hasKey("type") && !stack.getTagCompound().getString("type").isEmpty())
				items.add(stack);
		}
	}

	@Override
	public void addInformation(ItemStack stack, World worldIn, List<String> tooltip, ITooltipFlag flagIn) {
		if(stack.hasTagCompound() && stack.getTagCompound().getInteger("fill") > 0)
			tooltip.add(stack.getTagCompound().getInteger("fill") + "mB");
		Fluid f = getFluid(stack);
		if(f != null)
			FFUtils.addFluidInfo(f, tooltip, true);
	}

	@Override
	public String getItemStackDisplayName(ItemStack stack) {
		Fluid f = getFluid(stack);
		if(f != null)
			return f.getLocalizedName(new FluidStack(f, 1000)).trim();
		return "Unknown";
	}

	public static ItemStack getStack(Fluid f) {
		ItemStack stack = new ItemStack(ModItems.fluid_icon, 1, 0);
		stack.setTagCompound(new NBTTagCompound());
		stack.getTagCompound().setString("type", f.getName());
		return stack;
	}

	public static ItemStack getStackWithQuantity(Fluid f, int amount) {
		ItemStack stack = new ItemStack(ModItems.fluid_icon, 1, 0);
		stack.setTagCompound(new NBTTagCompound());
		stack.getTagCompound().setString("type", f.getName());
		stack.getTagCompound().setInteger("fill", amount);
		return stack;
	}

	public static ItemStack getStackWithQuantity(FluidStack f) {
		ItemStack stack = new ItemStack(ModItems.fluid_icon, 1, 0);
		stack.setTagCompound(new NBTTagCompound());
		stack.getTagCompound().setString("type", f.getFluid().getName());
		stack.getTagCompound().setInteger("fill", f.amount);
		return stack;
	}

	public static int getQuantity(ItemStack stack) {
		if(stack.hasTagCompound())
			return stack.getTagCompound().getInteger("fill");
		return 0;
	}

	public static Fluid getFluid(ItemStack stack) {
		if(stack == null || !stack.hasTagCompound())
			return null;
		return FluidRegistry.getFluid(stack.getTagCompound().getString("type"));
	}
}