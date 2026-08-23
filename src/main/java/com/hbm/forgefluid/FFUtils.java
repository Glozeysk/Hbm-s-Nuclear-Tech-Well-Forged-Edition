package com.hbm.forgefluid;

import java.util.ArrayList;
import java.util.List;

import com.google.common.base.Predicate;
import com.hbm.interfaces.IFluidPipe;
import com.hbm.interfaces.IFluidPipeMk2;
import com.hbm.interfaces.IFluidVisualConnectable;
import com.hbm.interfaces.IItemFluidHandler;
import com.hbm.inventory.FluidCombustionRecipes;
import com.hbm.inventory.HeatRecipes;
import com.hbm.inventory.EngineRecipes;
import com.hbm.inventory.gui.GuiInfoContainer;
import com.hbm.items.ModItems;
import com.hbm.items.armor.JetpackBase;
import com.hbm.items.gear.JetpackGlider;
import com.hbm.items.machine.ItemFluidTank;
import com.hbm.items.special.ItemCell;
import com.hbm.handler.ArmorModHandler;
import com.hbm.items.tool.ItemFluidCanister;
import com.hbm.items.tool.ItemGasCanister;
import com.hbm.lib.Library;
import com.hbm.render.RenderHelper;
import com.hbm.tileentity.machine.dummy.TileEntityDummy;

import com.hbm.util.I18nUtil;
import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.I18n;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidTank;
import net.minecraftforge.fluids.FluidUtil;
import net.minecraftforge.fluids.capability.CapabilityFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandlerItem;
import net.minecraftforge.items.IItemHandlerModifiable;
import org.lwjgl.input.Keyboard;

import javax.annotation.Nullable;

public class FFUtils {

	public static void drawLiquid(FluidTank tank, int guiLeft, int guiTop, float zLevel, int sizeX, int sizeY, int offsetX, int offsetY){
		drawLiquid(tank, guiLeft, guiTop, zLevel, sizeX, sizeY, offsetX, offsetY, false);
	}

	public static void drawLogLiquid(FluidTank tank, int guiLeft, int guiTop, float zLevel, int sizeX, int sizeY, int offsetX, int offsetY){
		drawLiquid(tank, guiLeft, guiTop, zLevel, sizeX, sizeY, offsetX, offsetY, true);
	}

	public static void drawLiquid(FluidTank tank, int guiLeft, int guiTop, float zLevel, int sizeX, int sizeY, int offsetX, int offsetY, boolean log){
		offsetY -= 44;
		RenderHelper.bindBlockTexture();

		if(tank.getFluid() != null) {
			TextureAtlasSprite liquidIcon = getTextureFromFluid(tank.getFluid().getFluid());

			if(liquidIcon != null) {
				int level = 0;
				if(log){
					if(tank.getFluidAmount() > 0){
						level = (int)(sizeY * (Math.log(tank.getFluidAmount()) / Math.log(tank.getCapacity())));
					}
				} else{
					level = (int)(((double)tank.getFluidAmount() / (double)tank.getCapacity()) * sizeY);
				}

				drawFull(tank.getFluid().getFluid(), guiLeft, guiTop, zLevel, liquidIcon, level, sizeX, offsetX, offsetY, sizeY);
			}
		}
	}

	public static void drawLiquid(FluidStack fluid, int guiLeft, int guiTop, float zLevel, int sizeX, int sizeY, int offsetX, int offsetY){
		if(fluid == null || fluid.getFluid() == null)
			return;
		drawLiquid(fluid.getFluid(), guiLeft, guiTop, zLevel, sizeX, sizeY, offsetX, offsetY);
	}

	public static void drawLiquid(Fluid fluid, int guiLeft, int guiTop, float zLevel, int sizeX, int sizeY, int offsetX, int offsetY){
		RenderHelper.bindBlockTexture();
		if(fluid != null) {
			TextureAtlasSprite liquidIcon = getTextureFromFluid(fluid);
			if(liquidIcon != null) {
				drawFull(fluid, guiLeft, guiTop, zLevel, liquidIcon, sizeY, sizeX, offsetX, offsetY, sizeY);
			}
		}
	}

	private static void drawFull(Fluid f, int guiLeft, int guiTop, float zLevel, TextureAtlasSprite liquidIcon, int level, int sizeX, int offsetX, int offsetY, int sizeY){
		int color = f.getColor();
		RenderHelper.setColor(color);
		RenderHelper.startDrawingTexturedQuads();
		for(int i = 0; i < level; i += 16) {
			for(int j = 0; j < sizeX; j += 16) {
				int drawX = Math.min(16, sizeX - j);
				int drawY = Math.min(16, level - i);
				RenderHelper.drawScaledTexture(liquidIcon, guiLeft + offsetX + j, guiTop + offsetY - i + (16 - drawY), drawX, drawY, zLevel);
			}
		}
		RenderHelper.draw();
	}

	public static void renderTankInfo(GuiInfoContainer gui, int mouseX, int mouseY, int x, int y, int width, int height, FluidTank fluidTank){
		renderTankInfo(gui, mouseX, mouseY, x, y, width, height, fluidTank, null);
	}

	public static void renderTankInfo(GuiInfoContainer gui, int mouseX, int mouseY, int x, int y, int width, int height, FluidTank fluidTank, Fluid fluid){
		if(fluidTank.getFluid() != null) {
			renderFluidInfo(gui, mouseX, mouseY, x, y, width, height, fluidTank.getFluid().getFluid(), fluidTank.getFluidAmount(), fluidTank.getCapacity());
		} else {
			renderFluidInfo(gui, mouseX, mouseY, x, y, width, height, fluid, 0, fluidTank.getCapacity());
		}
	}

	public static void addFluidInfo(Fluid fluid, List<String> texts) {
		addFluidInfo(fluid, texts, false);
	}

	public static void addFluidInfo(Fluid fluid, List<String> texts, boolean forceDetailed) {
		int temp = fluid.getTemperature() - 273;
		if(temp != 27) {
			String tempColor = "";
			if(temp < -130) {
				tempColor = "§3";
			} else if(temp < 0) {
				tempColor = "§b";
			} else if(temp < 100) {
				tempColor = "§e";
			} else if(temp < 300) {
				tempColor = "§6";
			} else if(temp < 1000) {
				tempColor = "§c";
			} else if(temp < 3000) {
				tempColor = "§4";
			} else if(temp < 20000) {
				tempColor = "§5";
			} else {
				tempColor = "§d";
			}
			texts.add(String.format("%s%d°C", tempColor, temp));
		}

		boolean hasInfo = false;
		boolean showDetails = forceDetailed || Keyboard.isKeyDown(Keyboard.KEY_LSHIFT);

		if(FluidTypeHandler.isAntimatter(fluid)) {
			if(showDetails) {
				texts.add("§4[" + I18n.format("trait.antimatter") + "]");
			}
			hasInfo = true;
		}

		if(FluidTypeHandler.isCorrosivePlastic(fluid)) {
			if(FluidTypeHandler.isCorrosiveIron(fluid)) {
				if(showDetails) {
					texts.add("§2[" + I18n.format("trait.corrosiveIron") + "]");
				}
			} else if(showDetails) {
				texts.add("§a[" + I18n.format("trait.corrosivePlastic") + "]");
			}
			hasInfo = true;
		}

		if(FluidCombustionRecipes.hasFuelRecipe(fluid)) {
			if(showDetails) {
				texts.add("§6[" + I18n.format("trait.flammable") + "]");
				texts.add(" " + I18n.format("trait.flammable.desc", Library.getShortNumber(FluidCombustionRecipes.getFlameEnergy(fluid) * 1000L)));
			}
			hasInfo = true;
		}

		if(EngineRecipes.hasFuelRecipe(fluid)) {
			if(showDetails) {
				texts.add("§c[" + I18n.format("trait.combustable") + "]");
				texts.add(" " + I18n.format("trait.combustable.desc", Library.getShortNumber(EngineRecipes.getEnergy(fluid))));
				texts.add(" " + I18n.format("trait.combustable.desc2", I18n.format(EngineRecipes.getFuelGrade(fluid).getGrade())));
			}
			hasInfo = true;
		}

		if(HeatRecipes.hasCoolRecipe(fluid)) {
			if(showDetails) {
				String heat = Library.getShortNumber(HeatRecipes.getResultingHeat(fluid) * 1000 / HeatRecipes.getInputAmountCold(fluid));
				texts.add("§4[" + I18n.format("trait.coolable") + "]");
				texts.add(" " + I18n.format("trait.coolable.desc", heat));
			}
			hasInfo = true;
		}

		if(HeatRecipes.hasBoilRecipe(fluid)) {
			if(showDetails) {
				String heat = Library.getShortNumber(HeatRecipes.getRequiredHeat(fluid) * 1000 / HeatRecipes.getInputAmountHot(fluid));
				texts.add("§3[" + I18n.format("trait.boilable") + "]");
				texts.add(" " + I18n.format("trait.boilable.desc", heat));
			}
			hasInfo = true;
		}

		float dfcEff = FluidTypeHandler.getDFCEfficiency(fluid);

		if(dfcEff >= 1) {
			if(showDetails) {
				texts.add("§5[" + I18n.format("trait.dfcFuel") + "]");
				dfcEff = (dfcEff - 1F);
				texts.add(" " + I18n.format("trait.dfcFuel.desc", dfcEff >= 0 ? "+" + Library.getPercentage(dfcEff) : Library.getPercentage(dfcEff)));
			}
			hasInfo = true;
		}

		if(hasInfo && !showDetails) {
			texts.add(I18nUtil.resolveKey("desc.tooltip.hold", "LSHIFT"));
		}
	}

	private static void renderFluidInfo(GuiInfoContainer gui, int mouseX, int mouseY, int x, int y, int width, int height, Fluid fluid, int amount, int capacity) {
		if(x <= mouseX && x + width > mouseX && y < mouseY && y + height >= mouseY) {
			List<String> texts = new ArrayList<>();
			if(fluid != null) {
				texts.add(fluid.getLocalizedName(new FluidStack(fluid, 1)));
				texts.add(amount + "/" + capacity + "mB");
				addFluidInfo(fluid, texts);
			} else {
				texts.add(I18nUtil.resolveKey("desc.none"));
				texts.add(amount + "/" + capacity + "mB");
			}

			gui.drawFluidInfo(texts, mouseX, mouseY);
		}
	}

	public static boolean hasEnoughFluid(FluidTank t, FluidStack f){
		if(f == null || f.amount == 0) return true;
		if(t == null || t.getFluid() == null) return false;
		if(t.getFluid().isFluidEqual(f) && t.getFluidAmount() >= f.amount) return true;
		return false;
	}

	public static boolean fillFluid(TileEntity tileEntity, FluidTank tank, World world, BlockPos toFill, int maxDrain){
		if(tank.getFluidAmount() <= 0 || tank.getFluid() == null || tank.getFluid().getFluid() == null) {
			return false;
		}
		TileEntity te = world.getTileEntity(toFill);

		if(te == null) {
			return false;
		}

		if(te instanceof TileEntityDummy) {
			TileEntityDummy ted = (TileEntityDummy)te;
			if(world.getTileEntity(ted.getTarget()) == tileEntity) {
				return false;
			}
		}

		try {
			FluidStack attempt = new FluidStack(tank.getFluid(), Math.min(maxDrain, tank.getFluidAmount()));
			IFluidHandler tef = te.getCapability(CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY, null);

			if(tef != null && tef.fill(attempt, false) > 0) {
				tank.drain(tef.fill(attempt, true), true);
				return true;
			}

			for(EnumFacing facing : EnumFacing.values()) {
				if(!te.hasCapability(CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY, facing)) {
					continue;
				}
				tef = te.getCapability(CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY, facing);
				if(tef != null && tef.fill(attempt, false) > 0) {
					tank.drain(tef.fill(attempt, true), true);
					return true;
				}
			}
		} catch(Throwable t) {
			return false;
		}
		return false;
	}

	public static boolean fillFromFluidContainer(IItemHandlerModifiable slots, FluidTank tank, int slot1, int slot2) {
		if(slots == null || tank == null || slots.getSlots() < slot1 || slots.getSlots() < slot2 || slots.getStackInSlot(slot1) == null || slots.getStackInSlot(slot1).isEmpty()) {
			return false;
		}

		if(trySpecialFillFromFluidContainer(slots, tank, slot1, slot2))
			return true;

		ItemStack stack = slots.getStackInSlot(slot1);

		if(stack.getItem() == ModItems.fluid_barrel_infinite && tank.getFluid() != null) {
			return tank.fill(new FluidStack(tank.getFluid(), Integer.MAX_VALUE), true) > 0;
		}

		if(ArmorModHandler.hasMods(stack)) {
			ItemStack mod = ArmorModHandler.pryMod(stack, ArmorModHandler.plate_only);

			if(!mod.isEmpty() && mod.getItem() instanceof JetpackGlider) {
				JetpackGlider glider = (JetpackGlider) mod.getItem();
				FluidTank modTank = glider.getTank(mod);

				if(modTank.getFluid() == null || modTank.getFluidAmount() <= 0) {
					moveItems(slots, slot1, slot2, true);
					return false;
				}

				if(tank.getFluid() != null && modTank.getFluid().getFluid() != tank.getFluid().getFluid()) {
					return false;
				}

				int space = tank.getCapacity() - tank.getFluidAmount();
				if(space <= 0) {
					return false;
				}

				int toTransfer = Math.min(space, modTank.getFluidAmount());
				FluidStack drained = glider.drain(mod, toTransfer, true);

				if(drained != null && drained.amount > 0) {
					tank.fill(drained, true);
					ArmorModHandler.applyMod(stack, mod);

					modTank = glider.getTank(mod);
					if(modTank.getFluid() == null || modTank.getFluidAmount() <= 0) {
						moveItems(slots, slot1, slot2, true);
					}

					return true;
				}

				return false;
			}
		}

		if(stack.hasCapability(CapabilityFluidHandler.FLUID_HANDLER_ITEM_CAPABILITY, null)) {
			boolean returnValue = false;

			IFluidHandlerItem ifhi = stack.getCapability(CapabilityFluidHandler.FLUID_HANDLER_ITEM_CAPABILITY, null);
			if(ifhi != null) {
				FluidStack contained = ifhi.drain(Integer.MAX_VALUE, false);

				if(contained == null) {
					moveItems(slots, slot1, slot2, true);
					return false;
				}

				if(tank.getFluid() == null || contained.getFluid() == tank.getFluid().getFluid()) {
					FluidStack drained = ifhi.drain(Math.min(6000, tank.getCapacity() - tank.getFluidAmount()), true);
					if(drained != null && drained.amount > 0) {
						tank.fill(drained, true);
						returnValue = true;
					}
				}

				if(ifhi.drain(Integer.MAX_VALUE, false) == null) {
					moveItems(slots, slot1, slot2, true);
				}
			}

			return returnValue;
		}

		if(stack.getItem() instanceof IItemFluidHandler) {
			boolean returnValue = false;
			IItemFluidHandler handler = (IItemFluidHandler) stack.getItem();
			FluidStack contained = handler.drain(stack, Integer.MAX_VALUE, false);

			if(contained == null) {
				moveItems(slots, slot1, slot2, true);
				return false;
			}

			if(tank.getFluid() == null || contained.getFluid() == tank.getFluid().getFluid()) {
				FluidStack drained = handler.drain(stack, Math.min(6000, tank.getCapacity() - tank.getFluidAmount()), true);
				if(drained != null && drained.amount > 0) {
					tank.fill(drained, true);
					returnValue = true;
				}
			}

			if(handler.drain(stack, Integer.MAX_VALUE, false) == null) {
				moveItems(slots, slot1, slot2, true);
			}

			return returnValue;
		}

		if(FluidUtil.getFluidContained(stack) == null) {
			moveItems(slots, slot1, slot2, false);
			return false;
		}

		return false;
	}

	private static boolean trySpecialFillFromFluidContainer(IItemHandlerModifiable slots, FluidTank tank, int slot1, int slot2){
		ItemStack in = slots.getStackInSlot(slot1);
		ItemStack out = slots.getStackInSlot(slot2);

		if(in.getItem() == ModItems.fluid_tank_full && tank.fill(FluidUtil.getFluidContained(in), false) == 1000 && ((ItemFluidTank.isEmptyTank(out) && out.getCount() < 64) || out.isEmpty())) {
			tank.fill(FluidUtil.getFluidContained(in), true);
			in.shrink(1);
			if(out.isEmpty()) {
				slots.setStackInSlot(slot2, new ItemStack(ModItems.fluid_tank_full));
			} else {
				out.grow(1);
			}
			return true;
		}

		if(in.getItem() == ModItems.fluid_barrel_full && tank.fill(FluidUtil.getFluidContained(in), false) == 16000 && ((ItemFluidTank.isEmptyBarrel(out) && out.getCount() < 64) || out.isEmpty())) {
			tank.fill(FluidUtil.getFluidContained(in), true);
			in.shrink(1);
			if(out.isEmpty()) {
				slots.setStackInSlot(slot2, new ItemStack(ModItems.fluid_barrel_full));
			} else {
				out.grow(1);
			}
			return true;
		}

		if(in.getItem() == ModItems.canister_generic && tank.fill(FluidUtil.getFluidContained(in), false) == 1000 && ((ItemFluidCanister.isEmptyCanister(out) && out.getCount() < 64) || out.isEmpty())) {
			tank.fill(FluidUtil.getFluidContained(in), true);
			in.shrink(1);
			if(out.isEmpty()) {
				slots.setStackInSlot(slot2, new ItemStack(ModItems.canister_generic));
			} else {
				out.grow(1);
			}
			return true;
		}

		if(in.getItem() == ModItems.gas_canister && tank.fill(FluidUtil.getFluidContained(in), false) == 4000 && ((ItemGasCanister.isEmptyCanister(out) && out.getCount() < 64) || out.isEmpty())) {
			tank.fill(FluidUtil.getFluidContained(in), true);
			in.shrink(1);
			if(out.isEmpty()) {
				slots.setStackInSlot(slot2, new ItemStack(ModItems.gas_canister));
			} else {
				out.grow(1);
			}
			return true;
		}

		if(in.getItem() == ModItems.cell && tank.fill(FluidUtil.getFluidContained(in), false) == 1000 && ((ItemCell.isEmptyCell(out) && out.getCount() < 64) || out.isEmpty())) {
			tank.fill(FluidUtil.getFluidContained(in), true);
			in.shrink(1);
			if(out.isEmpty()) {
				slots.setStackInSlot(slot2, new ItemStack(ModItems.cell));
			} else {
				out.grow(1);
			}
			return true;
		}

		if(in.getItem() == ModItems.nugget_mercury && tank.fill(new FluidStack(ModForgeFluids.mercury, 125), false) == 125){
			tank.fill(new FluidStack(ModForgeFluids.mercury, 125), true);
			in.shrink(1);
			return true;
		}

		if(FluidContainerRegistry.hasFluid(in.getItem())) {
			FluidStack fluid = FluidContainerRegistry.getFluidFromItem(in.getItem());
			Item container = FluidContainerRegistry.getContainerItem(in.getItem());
			if(tank.fill(fluid, false) == fluid.amount && (out.isEmpty() || (out.getItem() == container && out.getCount() < out.getMaxStackSize()))) {
				tank.fill(fluid, true);
				in.shrink(1);
				if(out.isEmpty()) {
					slots.setStackInSlot(slot2, new ItemStack(container));
				} else {
					out.grow(1);
				}
				return true;
			}
		}

		return false;
	}

	public static boolean checkRestrictions(ItemStack stack, Predicate<FluidStack> fluidRestrictor){
		if(stack.getItem() == ModItems.fluid_barrel_infinite)
			return true;
		if(stack.getItem() == ModItems.nugget_mercury)
			return fluidRestrictor.apply(new FluidStack(ModForgeFluids.mercury, 125));
		FluidStack fluid = FluidUtil.getFluidContained(stack);
		if(fluid != null && fluidRestrictor.apply(fluid))
			return true;
		if(FluidContainerRegistry.hasFluid(stack.getItem())) {
			fluid = FluidContainerRegistry.getFluidFromItem(stack.getItem());
			if(fluid != null && fluidRestrictor.apply(fluid))
				return true;
		}
		return false;
	}

	public static boolean isEmtpyFluidTank(ItemStack stack){
		return stack.hasCapability(CapabilityFluidHandler.FLUID_HANDLER_ITEM_CAPABILITY, null) && FluidUtil.getFluidContained(stack) == null;
	}

	public static boolean canDrainIntoTank(ItemStack stack) {
		if(stack.isEmpty())
			return false;
		if(stack.getItem() == ModItems.fluid_barrel_infinite || stack.getItem() == ModItems.nugget_mercury)
			return true;
		if(FluidUtil.getFluidContained(stack) != null)
			return true;
		if(FluidContainerRegistry.hasFluid(stack.getItem()))
			return FluidContainerRegistry.getFluidFromItem(stack.getItem()) != null;
		if(stack.hasCapability(CapabilityFluidHandler.FLUID_HANDLER_ITEM_CAPABILITY, null)) {
			IFluidHandlerItem ifhi = stack.getCapability(CapabilityFluidHandler.FLUID_HANDLER_ITEM_CAPABILITY, null);
			return ifhi != null && ifhi.drain(Integer.MAX_VALUE, false) != null;
		}
		if(stack.getItem() instanceof IItemFluidHandler)
			return ((IItemFluidHandler) stack.getItem()).drain(stack, Integer.MAX_VALUE, false) != null;
		if(ArmorModHandler.hasMods(stack)) {
			ItemStack mod = ArmorModHandler.pryMod(stack, ArmorModHandler.plate_only);
			if(!mod.isEmpty() && mod.getItem() instanceof JetpackGlider) {
				FluidTank modTank = ((JetpackGlider) mod.getItem()).getTank(mod);
				return modTank.getFluid() != null && modTank.getFluidAmount() > 0;
			}
		}
		return false;
	}

	public static boolean canFillFromTank(ItemStack stack) {
		if(stack.isEmpty())
			return false;
		Item item = stack.getItem();
		if(item == Items.BUCKET)
			return true;
		if(item == ModItems.fluid_tank_full && ItemFluidTank.isEmptyTank(stack.copy()))
			return true;
		if(item == ModItems.fluid_barrel_full && ItemFluidTank.isEmptyBarrel(stack.copy()))
			return true;
		if(item == ModItems.canister_generic && ItemFluidCanister.isEmptyCanister(stack.copy()))
			return true;
		if(item == ModItems.gas_canister && ItemGasCanister.isEmptyCanister(stack.copy()))
			return true;
		if(item == ModItems.cell && ItemCell.isEmptyCell(stack.copy()))
			return true;
		if(item == ModItems.rod_empty || item == ModItems.rod_dual_empty || item == ModItems.rod_quad_empty)
			return true;
		if(stack.hasCapability(CapabilityFluidHandler.FLUID_HANDLER_ITEM_CAPABILITY, null))
			return FluidUtil.getFluidContained(stack) == null;
		if(item instanceof IItemFluidHandler) {
			FluidStack contained = ((IItemFluidHandler) item).drain(stack, Integer.MAX_VALUE, false);
			return contained == null;
		}
		if(ArmorModHandler.hasMods(stack)) {
			ItemStack mod = ArmorModHandler.pryMod(stack, ArmorModHandler.plate_only);
			if(!mod.isEmpty() && mod.getItem() instanceof JetpackGlider) {
				FluidTank modTank = ((JetpackGlider) mod.getItem()).getTank(mod);
				return modTank.getFluid() == null || modTank.getFluidAmount() < modTank.getCapacity();
			}
			if(!mod.isEmpty() && mod.getItem() instanceof JetpackBase)
				return true;
		}
		return false;
	}

	public static boolean fillFluidContainer(IItemHandlerModifiable slots, FluidTank tank, int slot1, int slot2) {
		if(slots == null || tank == null || tank.getFluid() == null || slots.getSlots() < slot1 || slots.getSlots() < slot2 || slots.getStackInSlot(slot1) == null || slots.getStackInSlot(slot1).isEmpty()) {
			return false;
		}

		if(trySpecialFillFluidContainer(slots, tank, slot1, slot2))
			return true;

		ItemStack stack = slots.getStackInSlot(slot1);

		if(stack.hasCapability(CapabilityFluidHandler.FLUID_HANDLER_ITEM_CAPABILITY, null)) {
			IFluidHandlerItem ifhi = FluidUtil.getFluidHandler(stack);
			FluidStack fStack = FluidUtil.getFluidContained(stack);
			return fillItemAndMove(slots, slot1, slot2, tank, ifhi, fStack, stack, true);
		}

		if(stack.getItem() instanceof IItemFluidHandler) {
			IItemFluidHandler handler = (IItemFluidHandler) stack.getItem();

			FluidStack contained = handler.drain(stack, Integer.MAX_VALUE, false);
			if(contained != null && contained.amount > 0 && contained.getFluid() != tank.getFluid().getFluid()) {
				moveItems(slots, slot1, slot2, false);
				return false;
			}

			int toTransfer = Math.min(16000, tank.getFluidAmount());
			if(toTransfer <= 0) {
				moveItems(slots, slot1, slot2, false);
				return false;
			}

			int filled = handler.fill(stack, new FluidStack(tank.getFluid().getFluid(), toTransfer), true);
			if(filled > 0) {
				tank.drain(filled, true);

				FluidStack remaining = handler.drain(stack, Integer.MAX_VALUE, false);
				int currentAmount = remaining != null ? remaining.amount : 0;
				int maxAmount = 0;

				if(stack.getItem() instanceof JetpackGlider) {
					maxAmount = ((JetpackGlider) stack.getItem()).capacity;
				}

				if(maxAmount > 0 && currentAmount >= maxAmount) {
					moveItems(slots, slot1, slot2, false);
				}

				return true;
			}

			moveItems(slots, slot1, slot2, false);
			return false;
		}

		if(ArmorModHandler.hasMods(stack)) {

			ItemStack mod = ArmorModHandler.pryMod(stack, ArmorModHandler.plate_only);

			if(!mod.isEmpty()) {

				if(mod.getItem() instanceof JetpackGlider) {
					JetpackGlider glider = (JetpackGlider) mod.getItem();
					FluidTank modTank = glider.getTank(mod);

					if(modTank.getFluid() != null && modTank.getFluidAmount() > 0 && modTank.getFluid().getFluid() != tank.getFluid().getFluid()) {
						moveItems(slots, slot1, slot2, false);
						return false;
					}

					int space = modTank.getCapacity() - modTank.getFluidAmount();
					if(space <= 0) {
						moveItems(slots, slot1, slot2, false);
						return false;
					}

					int toTransfer = Math.min(space, tank.getFluidAmount());
					if(toTransfer <= 0) {
						moveItems(slots, slot1, slot2, false);
						return false;
					}

					int filled = glider.fill(mod, new FluidStack(tank.getFluid().getFluid(), toTransfer), true);
					if(filled > 0) {
						tank.drain(filled, true);
						ArmorModHandler.applyMod(stack, mod);

						modTank = glider.getTank(mod);

						if(modTank.getFluidAmount() >= modTank.getCapacity()) {
							moveItems(slots, slot1, slot2, false);
						}

						return true;
					}

					moveItems(slots, slot1, slot2, false);
					return false;
				}

				if(mod.getItem() instanceof JetpackBase && ((JetpackBase) mod.getItem()).fuel == tank.getFluid().getFluid()) {

					boolean didFill = false;

					if(tank.getFluidAmount() > 0 && JetpackBase.getFuel(mod) < ((JetpackBase) mod.getItem()).maxFuel) {
						FluidStack st = tank.drain(25, false);
						int fill = st == null ? 0 : st.amount;
						fill = Math.min(((JetpackBase) mod.getItem()).maxFuel - JetpackBase.getFuel(mod), fill);
						if(fill > 0) {
							JetpackBase.setFuel(mod, JetpackBase.getFuel(mod) + fill);
							tank.drain(fill, true);
							if(JetpackBase.getFuel(mod) < ((JetpackBase) mod.getItem()).maxFuel) {
								didFill = true;
							}
							ArmorModHandler.applyMod(stack, mod);
						}
					}

					if(!didFill)
						moveItems(slots, slot1, slot2, false);
					else
						return true;
				}
			}

			moveItems(slots, slot1, slot2, false);
			return false;
		}

		return false;
	}

	private static boolean fillItemAndMove(IItemHandlerModifiable slots, int slot1, int slot2, FluidTank tank, IFluidHandlerItem fHandler, FluidStack fStack, ItemStack stack, boolean move){
		if(fStack != null && fHandler.fill(tank.getFluid(), false) <= 0) {
			if(move) moveItems(slots, slot1, slot2, false);
			return false;
		}
		boolean returnValue = false;
		if(fStack == null || fStack.getFluid() == tank.getFluid().getFluid()) {
			tank.drain(fHandler.fill(new FluidStack(tank.getFluid(), Math.min(16000, tank.getFluidAmount())), true), true);
			returnValue = true;
		}
		stack = fHandler.getContainer();
		fStack = FluidUtil.getFluidContained(stack);
		if(fStack != null && fHandler.fill(new FluidStack(fStack.getFluid(), Integer.MAX_VALUE), false) <= 0) {
			if(move) moveItems(slots, slot1, slot2, false);
		}
		return returnValue;
	}

	private static boolean fillItemAndMove(IItemHandlerModifiable slots, int slot1, int slot2, FluidTank tank, IItemFluidHandler fHandler, FluidStack fStack, ItemStack stack, boolean move){
		if(fStack != null && fHandler.fill(stack, tank.getFluid(), false) <= 0) {
			if(move) moveItems(slots, slot1, slot2, false);
			return false;
		}
		boolean returnValue = false;
		if(fStack == null || fStack.getFluid() == tank.getFluid().getFluid()) {
			tank.drain(fHandler.fill(stack, new FluidStack(tank.getFluid(), Math.min(16000, tank.getFluidAmount())), true), true);
			returnValue = true;
		}
		fStack = fHandler.drain(stack, Integer.MAX_VALUE, false);
		if(fStack != null && fHandler.fill(stack, new FluidStack(fStack.getFluid(), Integer.MAX_VALUE), false) <= 0) {
			if(move) moveItems(slots, slot1, slot2, false);
		}
		return returnValue;
	}

	private static boolean trySpecialFillFluidContainer(IItemHandlerModifiable slots, FluidTank tank, int slot1, int slot2){
		if(tank == null || tank.getFluid() == null) return false;
		ItemStack in = slots.getStackInSlot(slot1);
		ItemStack out = slots.getStackInSlot(slot2);

		ItemStack in1 = in.copy();
		Item item1 = in.getItem();
		in1.setCount(1);

		if(item1 == Items.BUCKET && tank.drain(1000, false) != null && tank.drain(1000, false).amount == 1000) {
			if(!out.isEmpty() && in.getCount() > 1)
				return false;
			FluidStack f = tank.drain(1000, true);
			if(f == null)
				return false;
			in.shrink(1);

			if(out.isEmpty()){
				slots.setStackInSlot(slot2, FluidUtil.getFilledBucket(f));
			} else {
				slots.setStackInSlot(slot1, FluidUtil.getFilledBucket(f));
			}
			return true;
		}

		if(tank.getFluid() != null && in.getItem() == ModItems.fluid_tank_full && tank.drain(1000, false) != null && tank.drain(1000, false).amount == 1000 && ItemFluidTank.isEmptyTank(in1) && ((ItemFluidTank.isFullTank(out, tank.getFluid().getFluid()) && out.getCount() < 64) || out.isEmpty())) {
			FluidStack f = tank.drain(1000, true);
			if(f == null)
				return false;
			in.shrink(1);

			if(out.isEmpty()) {
				slots.setStackInSlot(slot2, ItemFluidTank.getFullTank(f.getFluid()));
			} else {
				out.grow(1);
			}
			return true;
		}

		if(tank.getFluid() != null && in.getItem() == ModItems.fluid_barrel_full && tank.drain(16000, false) != null && tank.drain(16000, false).amount == 16000 && ItemFluidTank.isEmptyBarrel(in1) && ((ItemFluidTank.isFullBarrel(out, tank.getFluid().getFluid()) && out.getCount() < 64) || out.isEmpty())) {
			FluidStack f = tank.drain(16000, true);
			if(f == null)
				return false;
			in.shrink(1);

			if(out.isEmpty()) {
				slots.setStackInSlot(slot2, ItemFluidTank.getFullBarrel(f.getFluid()));
			} else {
				out.grow(1);
			}
			return true;
		}

		if(tank.getFluid() != null && in.getItem() == ModItems.canister_generic && SpecialContainerFillLists.EnumCanister.contains(tank.getFluid().getFluid()) && tank.drain(1000, false) != null && tank.drain(1000, false).amount == 1000 && ItemFluidCanister.isEmptyCanister(in1) && ((ItemFluidCanister.isFullCanister(out, tank.getFluid().getFluid()) && out.getCount() < 64) || out.isEmpty())) {
			FluidStack f = tank.drain(1000, true);
			if(f == null)
				return false;
			in.shrink(1);

			if(out.isEmpty()) {
				slots.setStackInSlot(slot2, ItemFluidCanister.getFullCanister(f.getFluid()));
			} else {
				out.grow(1);
			}
			return true;
		}

		if(tank.getFluid() != null && in.getItem() == ModItems.gas_canister && SpecialContainerFillLists.EnumGasCanister.contains(tank.getFluid().getFluid()) && tank.drain(4000, false) != null && tank.drain(4000, false).amount == 4000 && ItemGasCanister.isEmptyCanister(in1) && ((ItemGasCanister.isFullCanister(out, tank.getFluid().getFluid()) && out.getCount() < 64) || out.isEmpty())) {
			FluidStack f = tank.drain(4000, true);
			if(f == null)
				return false;
			in.shrink(1);

			if(out.isEmpty()) {
				slots.setStackInSlot(slot2, ItemGasCanister.getFullCanister(f.getFluid()));
			} else {
				out.grow(1);
			}
			return true;
		}

		if(tank.getFluid() != null && in.getItem() == ModItems.cell && SpecialContainerFillLists.EnumCell.contains(tank.getFluid().getFluid()) && tank.drain(1000, false) != null && tank.drain(1000, false).amount == 1000 && ItemCell.isEmptyCell(in1) && ((ItemCell.isFullCell(out, tank.getFluid().getFluid()) && out.getCount() < 64) || out.isEmpty())) {
			FluidStack f = tank.drain(1000, true);
			if(f == null)
				return false;
			in.shrink(1);

			if(out.isEmpty()) {
				slots.setStackInSlot(slot2, ItemCell.getFullCell(f.getFluid()));
			} else {
				out.grow(1);
			}
			return true;
		}

		if(in.getItem() == ModItems.rod_empty) {
			if(tank.getFluid() != null && tank.getFluid().getFluid() == ModForgeFluids.coolant && tank.getFluid().amount >= 1000 && out.isEmpty()) {
				tank.drain(1000, true);
				in.shrink(1);
				if(out.isEmpty()) {
					slots.setStackInSlot(slot2, new ItemStack(ModItems.rod_coolant));
				} else {
					slots.setStackInSlot(slot1, new ItemStack(ModItems.rod_coolant));
				}
				return true;
			}
			if(tank.getFluid() != null && tank.getFluid().getFluid() == ModForgeFluids.tritium && tank.getFluid().amount >= 1000 && out.isEmpty()) {
				tank.drain(1000, true);
				in.shrink(1);
				if(out.isEmpty()) {
					slots.setStackInSlot(slot2, new ItemStack(ModItems.rod_tritium));
				} else {
					slots.setStackInSlot(slot1, new ItemStack(ModItems.rod_tritium));
				}
				return true;
			}
			if(tank.getFluid() != null && tank.getFluid().getFluid() == FluidRegistry.WATER && tank.getFluid().amount >= 1000 && out.isEmpty()) {
				tank.drain(1000, true);
				in.shrink(1);
				if(out.isEmpty()) {
					slots.setStackInSlot(slot2, new ItemStack(ModItems.rod_water));
				} else {
					slots.setStackInSlot(slot1, new ItemStack(ModItems.rod_water));
				}
				return true;
			}
		}

		if(in.getItem() == ModItems.rod_dual_empty) {
			if(tank.getFluid() != null && tank.getFluid().getFluid() == ModForgeFluids.coolant && tank.getFluid().amount >= 2000 && out.isEmpty()) {
				tank.drain(2000, true);
				in.shrink(1);
				if(out.isEmpty()) {
					slots.setStackInSlot(slot2, new ItemStack(ModItems.rod_dual_coolant));
				} else {
					slots.setStackInSlot(slot1, new ItemStack(ModItems.rod_dual_coolant));
				}
				return true;
			}
			if(tank.getFluid() != null && tank.getFluid().getFluid() == ModForgeFluids.tritium && tank.getFluid().amount >= 2000 && out.isEmpty()) {
				tank.drain(2000, true);
				in.shrink(1);
				if(out.isEmpty()) {
					slots.setStackInSlot(slot2, new ItemStack(ModItems.rod_dual_tritium));
				} else {
					slots.setStackInSlot(slot1, new ItemStack(ModItems.rod_dual_tritium));
				}
				return true;
			}
			if(tank.getFluid() != null && tank.getFluid().getFluid() == FluidRegistry.WATER && tank.getFluid().amount >= 2000 && out.isEmpty()) {
				tank.drain(2000, true);
				in.shrink(1);
				if(out.isEmpty()) {
					slots.setStackInSlot(slot2, new ItemStack(ModItems.rod_dual_water));
				} else {
					slots.setStackInSlot(slot1, new ItemStack(ModItems.rod_dual_water));
				}
				return true;
			}
		}

		if(in.getItem() == ModItems.rod_quad_empty) {
			if(tank.getFluid() != null && tank.getFluid().getFluid() == ModForgeFluids.coolant && tank.getFluid().amount >= 4000 && out.isEmpty()) {
				tank.drain(4000, true);
				in.shrink(1);
				if(out.isEmpty()) {
					slots.setStackInSlot(slot2, new ItemStack(ModItems.rod_quad_coolant));
				} else {
					slots.setStackInSlot(slot1, new ItemStack(ModItems.rod_quad_coolant));
				}
				return true;
			}
			if(tank.getFluid() != null && tank.getFluid().getFluid() == ModForgeFluids.tritium && tank.getFluid().amount >= 4000 && out.isEmpty()) {
				tank.drain(4000, true);
				in.shrink(1);
				if(out.isEmpty()) {
					slots.setStackInSlot(slot2, new ItemStack(ModItems.rod_quad_tritium));
				} else {
					slots.setStackInSlot(slot1, new ItemStack(ModItems.rod_quad_tritium));
				}
				return true;
			}
			if(tank.getFluid() != null && tank.getFluid().getFluid() == FluidRegistry.WATER && tank.getFluid().amount >= 4000 && out.isEmpty()) {
				tank.drain(4000, true);
				in.shrink(1);
				if(out.isEmpty()) {
					slots.setStackInSlot(slot2, new ItemStack(ModItems.rod_quad_water));
				} else {
					slots.setStackInSlot(slot1, new ItemStack(ModItems.rod_quad_water));
				}
				return true;
			}
		}

		if(in.getItem() instanceof JetpackBase && ((JetpackBase)in.getItem()).fuel == tank.getFluid().getFluid()) {
			if(tank.getFluidAmount() > 0 && JetpackBase.getFuel(in) < ((JetpackBase)in.getItem()).maxFuel) {
				FluidStack st = tank.drain(25, false);
				int fill = st == null ? 0 : st.amount;
				JetpackBase.setFuel(in, Math.min(JetpackBase.getFuel(in) + fill, ((JetpackBase)in.getItem()).maxFuel));
				tank.drain(fill, true);
				if(JetpackBase.getFuel(in) >= ((JetpackBase)in.getItem()).maxFuel && out.isEmpty()) {
					slots.setStackInSlot(slot2, in);
					slots.setStackInSlot(slot1, ItemStack.EMPTY);
				}
				return true;
			}
		}

		Item container = FluidContainerRegistry.getFullContainer(in.getItem(), tank.getFluid().getFluid());
		if(container != null && container != Items.AIR) {
			FluidStack stack = FluidContainerRegistry.getFluidFromItem(container);
			if(tank.drain(stack, false).amount == stack.amount && (out.isEmpty() || (out.getItem() == container && out.getCount() < out.getMaxStackSize()))) {
				tank.drain(stack, true);
				in.shrink(1);
				if(out.isEmpty()) {
					slots.setStackInSlot(slot2, new ItemStack(container));
				} else {
					out.grow(1);
				}
				return true;
			}
		}

		return false;
	}

	public static boolean moveItems(IItemHandlerModifiable slots, int in, int out, boolean shouldUseContainerItem){
		if(slots.getStackInSlot(in) != null && !slots.getStackInSlot(in).isEmpty()) {
			if(shouldUseContainerItem && slots.getStackInSlot(in).getItem().hasContainerItem(slots.getStackInSlot(in))) {
				slots.setStackInSlot(in, slots.getStackInSlot(in).getItem().getContainerItem(slots.getStackInSlot(in)));
			}
			if(slots.getStackInSlot(out) == null || slots.getStackInSlot(out).isEmpty()) {
				slots.setStackInSlot(out, slots.getStackInSlot(in));
				slots.setStackInSlot(in, ItemStack.EMPTY);
				return true;
			} else if(Library.areItemStacksEqualIgnoreCount(slots.getStackInSlot(in), slots.getStackInSlot(out))) {
				int amountToTransfer = Math.min(slots.getStackInSlot(out).getMaxStackSize() - slots.getStackInSlot(out).getCount(), slots.getStackInSlot(in).getCount());
				slots.getStackInSlot(in).shrink(amountToTransfer);
				if(slots.getStackInSlot(in).getCount() <= 0)
					slots.setStackInSlot(in, ItemStack.EMPTY);
				slots.getStackInSlot(out).grow(amountToTransfer);
				return true;
			}
		}
		return false;
	}

	public static FluidTank changeTankSize(FluidTank fluidTank, int i){
		FluidTank newTank = new FluidTank(i);
		if(fluidTank.getFluid() == null) {
			return newTank;
		} else {
			newTank.fill(fluidTank.getFluid(), true);
			return newTank;
		}
	}

	public static TextureAtlasSprite getTextureFromFluid(Fluid f){
		if(f == null) {
			return null;
		}
		return Minecraft.getMinecraft().getTextureMapBlocks().getAtlasSprite(f.getStill().toString());
	}

	public static int getColorFromFluid(Fluid f){
		return Library.getColorFromResourceLocation(new ResourceLocation(f.getStill().getNamespace(), "textures/"+f.getStill().getPath()+".png"));
	}

	public static void setColorFromFluid(Fluid f){
		if(f == null)
			return;
		setRGBAFromHex(f.getColor());
	}

	public static void setRGBAFromHex(int color){
		float r = (color >> 16 & 0xFF) / 255F;
		float g = (color >> 8 & 0xFF) / 255F;
		float b = (color & 0xFF) / 255F;
		float a = (color >> 24 & 0xFF) / 255F;
		GlStateManager.color(r, g, b, a);
	}

	public static void setRGBFromHex(int color){
		float r = (color >> 16 & 0xFF) / 255F;
		float g = (color >> 8 & 0xFF) / 255F;
		float b = (color & 0xFF) / 255F;
		GlStateManager.color(r, g, b, 1);
	}

	public static boolean containsFluid(ItemStack stack, Fluid fluid){
		if(stack.getItem() == ModItems.fluid_barrel_infinite)
			return true;
		if(stack.getItem() == ModItems.nugget_mercury)
			return fluid == ModForgeFluids.mercury;
		FluidStack contained = FluidUtil.getFluidContained(stack);
		if(contained != null && contained.getFluid() == fluid)
			return true;
		if(FluidContainerRegistry.hasFluid(stack.getItem())) {
			contained = FluidContainerRegistry.getFluidFromItem(stack.getItem());
			if(contained != null && contained.getFluid() == fluid)
				return true;
		}
		return false;
	}

	public static NBTTagList serializeTankArray(FluidTank[] tanks){
		NBTTagList list = new NBTTagList();
		for(int i = 0; i < tanks.length; i++) {
			if(tanks[i] != null) {
				NBTTagCompound tag = new NBTTagCompound();
				tag.setByte("tank", (byte)i);
				tanks[i].writeToNBT(tag);
				list.appendTag(tag);
			}
		}
		return list;
	}

	public static void deserializeTankArray(NBTTagList tankList, FluidTank[] tanks){
		for(int i = 0; i < tankList.tagCount(); i++) {
			NBTTagCompound tag = tankList.getCompoundTagAt(i);
			byte b0 = tag.getByte("tank");
			if(b0 >= 0 && b0 < tanks.length) {
				tanks[b0].readFromNBT(tag);
			}
		}
	}

	public static boolean areTanksEqual(FluidTank tank1, FluidTank tank2){
		if(tank1 == null && tank2 == null) {
			return true;
		}
		if(tank1 == null ^ tank2 == null) {
			return false;
		}
		if(tank1.getFluid() == null && tank2.getFluid() == null) {
			return true;
		}
		if(tank1.getFluid() == null ^ tank2.getFluid() == null) {
			return false;
		}
		if(tank1.getFluid().amount == tank2.getFluid().amount && tank1.getFluid().getFluid() == tank2.getFluid().getFluid() && tank1.getCapacity() == tank2.getCapacity()) {
			return true;
		}
		return false;
	}

	public static FluidTank copyTank(FluidTank tank){
		if(tank == null)
			return null;
		return new FluidTank(tank.getFluid() != null ? tank.getFluid().copy() : null, tank.getCapacity());
	}

	public static boolean checkFluidConnectables(World world, BlockPos pos, FFPipeNetwork net, @Nullable EnumFacing facing){
		TileEntity tileentity = world.getTileEntity(pos);
		if(tileentity != null && tileentity instanceof IFluidPipe && ((IFluidPipe)tileentity).getNetworkTrue() == net)
			return true;
		if(tileentity != null && !(tileentity instanceof IFluidPipe) && tileentity.hasCapability(CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY, facing)) {
			return true;
		}
		return false;
	}

	public static boolean checkFluidConnectablesMk2(World world, BlockPos pos, Fluid type, @Nullable EnumFacing facing){
		TileEntity tileentity = world.getTileEntity(pos);
		if(tileentity instanceof IFluidPipeMk2 && ((IFluidPipeMk2)tileentity).getType() == type)
			return true;
		if(tileentity != null && !(tileentity instanceof IFluidPipeMk2) && tileentity.hasCapability(CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY, facing)) {
			return true;
		}
		Block block = world.getBlockState(pos).getBlock();
		if(block instanceof IFluidVisualConnectable)
			return ((IFluidVisualConnectable)block).shouldConnect(type);
		return false;
	}
}
