package com.hbm.items.machine;

import java.util.ArrayList;
import java.util.List;

import com.hbm.interfaces.IHasCustomModel;
import com.hbm.inventory.ChemplantRecipes;
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
import net.minecraft.util.NonNullList;
import net.minecraft.world.World;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.oredict.OreDictionary;

public class ItemChemistryTemplate extends Item implements IHasCustomModel {

	public static final ModelResourceLocation chemModel = new ModelResourceLocation(RefStrings.MODID + ":chemistry_template", "inventory");

	public ItemChemistryTemplate(String s){
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
		String s1 = ("" + I18n.format("chem." + ChemplantRecipes.getName(stack))).trim();

		if (s1 != null) {
			s = s + "" + s1;
		}

		return s;
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
		if(!(stack.getItem() instanceof ItemChemistryTemplate))
			return;

		List<AStack> itemInputs = ChemplantRecipes.getChemInputFromTempate(stack);
		FluidStack[] fluidInputs = ChemplantRecipes.getFluidInputFromTempate(stack);
		ItemStack[] itemOutputs = ChemplantRecipes.getChemOutputFromTempate(stack);
		FluidStack[] fluidOutputs = ChemplantRecipes.getFluidOutputFromTempate(stack);
		int time = ChemplantRecipes.getProcessTime(stack);

		List<ItemStack> currentInputs = new ArrayList<>();
		List<FluidStack> currentFluids = new ArrayList<>();
		net.minecraft.client.gui.GuiScreen screen = net.minecraft.client.Minecraft.getMinecraft().currentScreen;
		if (screen instanceof net.minecraft.client.gui.inventory.GuiContainer) {
			net.minecraft.inventory.Container container = ((net.minecraft.client.gui.inventory.GuiContainer) screen).inventorySlots;
			if (container instanceof com.hbm.inventory.container.ContainerMachineChemplant) {
				for (int k = 13; k <= 16; k++) {
					currentInputs.add(container.getSlot(k).getStack());
				}
				try {
					java.lang.reflect.Field f = com.hbm.inventory.container.ContainerMachineChemplant.class.getDeclaredField("nukeBoy");
					f.setAccessible(true);
					Object te = f.get(container);
					if (te != null) {
						java.lang.reflect.Field tanksField = te.getClass().getField("tanks");
						Object[] tanks = (Object[]) tanksField.get(te);
						if (tanks != null) {
							for (Object tank : tanks) {
								if (tank != null) {
									java.lang.reflect.Method getFluid = tank.getClass().getMethod("getFluid");
									FluidStack fs = (FluidStack) getFluid.invoke(tank);
									if (fs != null) currentFluids.add(fs);
								}
							}
						}
					}
				} catch (Exception e) {}
			} else if (container instanceof com.hbm.inventory.container.ContainerMachineChemical) {
				for (int k = 13; k <= 16; k++) {
					currentInputs.add(container.getSlot(k).getStack());
				}
				try {
					java.lang.reflect.Field f = com.hbm.inventory.container.ContainerMachineChemical.class.getDeclaredField("nukeBoy");
					f.setAccessible(true);
					Object te = f.get(container);
					if (te != null) {
						java.lang.reflect.Field tanksField = te.getClass().getField("tanks");
						Object[] tanks = (Object[]) tanksField.get(te);
						if (tanks != null) {
							for (Object tank : tanks) {
								if (tank != null) {
									java.lang.reflect.Method getFluid = tank.getClass().getMethod("getFluid");
									FluidStack fs = (FluidStack) getFluid.invoke(tank);
									if (fs != null) currentFluids.add(fs);
								}
							}
						}
					}
				} catch (Exception e) {}
			}
		}

		boolean allMet = true;
		List<Boolean> itemMetFlags = new ArrayList<>();
		List<Integer> itemHaveAmounts = new ArrayList<>();
		List<Boolean> fluidMetFlags = new ArrayList<>();
		List<Integer> fluidHaveAmounts = new ArrayList<>();

		if (itemInputs != null) {
			for (AStack req : itemInputs) {
				int needed = req.count();
				int have = 0;
				AStack sing = req.copy();
				sing.singulize();
				for (ItemStack slotStack : currentInputs) {
					if (slotStack != null && !slotStack.isEmpty()) {
						ItemStack compare = slotStack.copy();
						compare.setCount(1);
						if (sing.isApplicable(compare)) {
							have += slotStack.getCount();
						}
					}
				}
				boolean met = have >= needed;
				if (!met) allMet = false;
				itemMetFlags.add(met);
				itemHaveAmounts.add(have);
			}
		}

		if (fluidInputs != null) {
			for (FluidStack req : fluidInputs) {
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

		try {
			list.add("§l" + I18nUtil.resolveKey("info.template_out_p"));
			String outColor = allMet ? "§a" : "§c";
			if(itemOutputs != null){
				for(ItemStack ouputItem : itemOutputs){
					list.add(" " + outColor + ouputItem.getCount() + "x " + ouputItem.getDisplayName());
				}
			}
			if(fluidOutputs != null){
				for(FluidStack outputFluid : fluidOutputs){
					list.add(" " + outColor + outputFluid.amount + "mB " + outputFluid.getFluid().getLocalizedName(outputFluid));
				}
			}
			list.add("§l" + I18nUtil.resolveKey("info.template_in_p"));

			if(itemInputs != null){
				int flagIdx = 0;
				for(AStack o : itemInputs){
					int have = itemHaveAmounts.get(flagIdx);
					int needed = o.count();
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
			}

			if(fluidInputs != null){
				int flagIdx = 0;
				for(FluidStack inputFluid : fluidInputs){
					int have = fluidHaveAmounts.get(flagIdx);
					int needed = inputFluid.amount;
					String color = getColorForFluidAmount(have, needed);
					flagIdx++;
					list.add(" " + color + inputFluid.amount + "mB " + inputFluid.getFluid().getLocalizedName(inputFluid) + " §7(" + have + "/" + needed + ")");
				}
			}

			list.add("§l" + I18nUtil.resolveKey("info.template_time"));
			list.add(" §3"+ Math.floor((float)(time) / 20 * 100) / 100 + " " + I18nUtil.resolveKey("info.template_seconds"));
		} catch(Exception e) {
			list.add("###INVALID###");
			list.add("0x334077-0x6A298F-0xDF3795-0x334077");
		}
	}

	@Override
	public ModelResourceLocation getResourceLocation() {
		return chemModel;
	}
}