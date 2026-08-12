package com.hbm.items.machine;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nonnull;

import com.hbm.interfaces.IHasCustomModel;
import com.hbm.inventory.AssemblerRecipes;
import com.hbm.inventory.RecipesCommon.AStack;
import com.hbm.inventory.RecipesCommon.ComparableStack;
import com.hbm.inventory.RecipesCommon.OreDictStack;
import com.hbm.items.ModItems;
import com.hbm.lib.RefStrings;
import com.hbm.main.MainRegistry;
import com.hbm.util.I18nUtil;

import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.NonNullList;
import net.minecraft.world.World;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.oredict.OreDictionary;

public class ItemAssemblyTemplate extends Item implements IHasCustomModel {

	public static final ModelResourceLocation location = new ModelResourceLocation(
			RefStrings.MODID + ":assembly_template", "inventory");

	public ItemAssemblyTemplate(String s) {
		this.setTranslationKey(s);
		this.setRegistryName(s);
		this.setHasSubtypes(true);
		this.setMaxDamage(0);
		this.setCreativeTab(null);

		ModItems.ALL_ITEMS.add(this);
	}

	@Override
	@SideOnly(Side.CLIENT)
	public String getItemStackDisplayName(ItemStack stack) {
		String s = ("").trim();
		int damage = getTagWithRecipeNumber(stack).getInteger("type");
		ItemStack out = damage < AssemblerRecipes.recipeList.size() ? AssemblerRecipes.recipeList.get(damage).toStack() : ItemStack.EMPTY;
		String s1 = ("" + I18n.format((out != ItemStack.EMPTY ? out.getTranslationKey() : "NULL") + ".name")).trim();

		if (s1 != null) {
			s = s + "" + s1;
		}

		return s;
	}

	@Override
	public void getSubItems(CreativeTabs tab, NonNullList<ItemStack> list) {
		if (tab == this.getCreativeTab() || tab == CreativeTabs.SEARCH) {
			int count = AssemblerRecipes.recipeList.size();

			for(int i = 0; i < count; i++) {
				NBTTagCompound tag = new NBTTagCompound();
				tag.setInteger("type", i);
				ItemStack stack = new ItemStack(this, 1, 0);
				stack.setTagCompound(tag);
				list.add(stack);
			}
		}
	}

	public static ItemStack getTemplate(int id){
		NBTTagCompound tag = new NBTTagCompound();
		tag.setInteger("type", id);
		ItemStack stack = new ItemStack(ModItems.assembly_template, 1, 0);
		stack.setTagCompound(tag);
		return stack;
	}

	private static String getColorForAmount(int have, int needed) {
		if (have >= needed) return "§a";
		if (have >= Math.ceil(needed * 0.5)) return "§e";
		return "§c";
	}

	private static String getColorForFluidAmount(int have, int needed) {
		if (have >= needed) return "§a";
		if (have >= Math.ceil(needed * 0.5)) return "§e";
		return "§c";
	}

	@Override
	public void addInformation(ItemStack stack, World worldIn, List<String> list, ITooltipFlag flagIn) {
		if (!(stack.getItem() instanceof ItemAssemblyTemplate))
			return;

		int i = getTagWithRecipeNumber(stack).getInteger("type");

		if(i < 0 || i >= AssemblerRecipes.recipeList.size()) {
			list.add("I AM ERROR");
			return;
		}

		ComparableStack out = AssemblerRecipes.recipeList.get(i);

		if(out == null) {
			list.add("I AM ERROR");
			return;
		}

		Object[] in = AssemblerRecipes.recipes.get(out);

		if(in == null) {
			list.add("I AM ERROR");
			return;
		}

		ItemStack output = out.toStack();
		FluidStack[] fluidInputs = AssemblerRecipes.getFluidInputFromTempate(stack);

		List<ItemStack> currentInputs = new ArrayList<>();
		List<FluidStack> currentFluids = new ArrayList<>();
		net.minecraft.client.gui.GuiScreen screen = net.minecraft.client.Minecraft.getMinecraft().currentScreen;
		if (screen instanceof net.minecraft.client.gui.inventory.GuiContainer) {
			net.minecraft.inventory.Container container = ((net.minecraft.client.gui.inventory.GuiContainer) screen).inventorySlots;
			if (container instanceof com.hbm.inventory.container.ContainerMachineAssembly ||
					container instanceof com.hbm.inventory.container.ContainerMachineAssembler) {
				if (container.inventorySlots.size() > 17) {
					for (int k = 6; k <= 17; k++) {
						currentInputs.add(container.getSlot(k).getStack());
					}
				}
				try {
					for(java.lang.reflect.Field field : container.getClass().getDeclaredFields()) {
						if(com.hbm.tileentity.machine.TileEntityMachineAssembly.class.isAssignableFrom(field.getType())) {
							field.setAccessible(true);
							Object te = field.get(container);
							if(te != null) {
								java.lang.reflect.Field tankField = te.getClass().getField("tank");
								Object tankObj = tankField.get(te);
								if(tankObj != null) {
									java.lang.reflect.Method getFluid = tankObj.getClass().getMethod("getFluid");
									FluidStack fs = (FluidStack) getFluid.invoke(tankObj);
									if(fs != null) currentFluids.add(fs);
								}
							}
						}
					}
				} catch (Exception e) {}
			}
		}

		boolean allMet = true;
		List<Boolean> metFlags = new ArrayList<>();
		List<Integer> haveAmounts = new ArrayList<>();
		List<Boolean> fluidMetFlags = new ArrayList<>();
		List<Integer> fluidHaveAmounts = new ArrayList<>();

		for(Object o : in) {
			if (!(o instanceof AStack)) continue;
			AStack req = (AStack) o;
			int needed = req.count();
			int have = 0;
			AStack sing = req.copy();
			sing.singulize();
			for (ItemStack slotStack : currentInputs) {
				if (!slotStack.isEmpty()) {
					ItemStack compare = slotStack.copy();
					compare.setCount(1);
					if (sing.isApplicable(compare)) {
						have += slotStack.getCount();
					}
				}
			}
			boolean met = have >= needed;
			if (!met) allMet = false;
			metFlags.add(met);
			haveAmounts.add(have);
		}

		if (fluidInputs != null) {
			for (FluidStack req : fluidInputs) {
				if(req == null) continue;
				int needed = req.amount;
				int have = 0;
				for (FluidStack tankFluid : currentFluids) {
					if (tankFluid != null && tankFluid.isFluidEqual(req)) {
						have += tankFluid.amount;
					}
				}
				boolean met = have >= needed;
				if (!met) allMet = false;
				fluidMetFlags.add(met);
				fluidHaveAmounts.add(have);
			}
		}

		list.add("§l" + I18nUtil.resolveKey("info.template_out"));
		String outColor = allMet ? "§a" : "§c";
		list.add(" " + outColor + output.getCount() + "x " + output.getDisplayName());
		list.add("§l" + I18nUtil.resolveKey("info.template_in_p"));

		int flagIdx = 0;
		for(Object o : in) {
			if (!(o instanceof AStack)) continue;
			int have = haveAmounts.get(flagIdx);
			AStack req = (AStack) o;
			int needed = req.count();
			String color = getColorForAmount(have, needed);
			flagIdx++;

			if(o instanceof ComparableStack)  {
				ItemStack input = ((ComparableStack)o).toStack();
				list.add(" " + color + input.getCount() + "x " + input.getDisplayName() + " §7(" + have + "/" + needed + ")");

			} else if(o instanceof OreDictStack)  {
				OreDictStack input = (OreDictStack) o;
				NonNullList<ItemStack> ores = OreDictionary.getOres(input.name);

				if(ores.size() > 0) {
					ItemStack inStack = ores.get((int) (Math.abs(System.currentTimeMillis() / 1000) % ores.size()));
					list.add(" " + color + input.count() + "x " + inStack.getDisplayName() + " §7(" + have + "/" + needed + ")");
				} else {
					list.add("I AM ERROR - No OrdDict match found for "+o.toString());
				}
			}
		}

		if(fluidInputs != null){
			int fluidFlagIdx = 0;
			for(FluidStack inputFluid : fluidInputs){
				if(inputFluid == null) continue;
				int have = fluidHaveAmounts.get(fluidFlagIdx);
				int needed = inputFluid.amount;
				String color = getColorForFluidAmount(have, needed);
				fluidFlagIdx++;
				list.add(" " + color + inputFluid.amount + "mB " + inputFluid.getFluid().getLocalizedName(inputFluid) + " §7(" + have + "/" + needed + ")");
			}
		}

		list.add("§l" + I18nUtil.resolveKey("info.template_time"));
		list.add(" §3" + Math.floor((float)(getProcessTime(stack)) / 20 * 100) / 100 + " " + I18nUtil.resolveKey("info.template_seconds"));
	}

	public static int getProcessTime(ItemStack stack) {
		if (!(stack.getItem() instanceof ItemAssemblyTemplate))
			return 100;

		int i = getTagWithRecipeNumber(stack).getInteger("type");

		if(i < 0 || i >= AssemblerRecipes.recipeList.size())
			return 100;

		ComparableStack out = AssemblerRecipes.recipeList.get(i);
		Integer time = AssemblerRecipes.time.get(out);

		if(time != null)
			return time;
		else
			return 100;

	}

	@Override
	public ModelResourceLocation getResourceLocation() {
		return location;
	}

	public static int getRecipeIndex(ItemStack stack){
		return getTagWithRecipeNumber(stack).getInteger("type");
	}

	public static NBTTagCompound getTagWithRecipeNumber(@Nonnull ItemStack stack){
		if(!stack.hasTagCompound()){
			stack.setTagCompound(new NBTTagCompound());
			stack.getTagCompound().setInteger("type", 0);
		}
		return stack.getTagCompound();
	}
}