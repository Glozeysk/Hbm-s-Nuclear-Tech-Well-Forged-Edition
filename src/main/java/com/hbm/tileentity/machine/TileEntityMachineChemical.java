package com.hbm.tileentity.machine;

import api.hbm.energy.IEnergyUser;
import com.hbm.blocks.BlockDummyable;
import com.hbm.forgefluid.FFUtils;
import com.hbm.interfaces.ITankPacketAcceptor;
import com.hbm.inventory.ChemplantRecipes;
import com.hbm.inventory.RecipesCommon.AStack;
import com.hbm.items.ModItems;
import com.hbm.items.machine.ItemChemistryTemplate;
import com.hbm.lib.ForgeDirection;
import com.hbm.lib.Library;
import com.hbm.packet.*;
import com.hbm.tileentity.TileEntityMachineBase;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.SPacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ITickable;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidTank;
import net.minecraftforge.fluids.capability.CapabilityFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.IFluidTankProperties;
import net.minecraftforge.fml.common.network.NetworkRegistry.TargetPoint;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandlerModifiable;
import net.minecraftforge.items.ItemStackHandler;

import java.util.ArrayList;
import java.util.List;

public class TileEntityMachineChemical extends TileEntityMachineBase implements IEnergyUser, ITankPacketAcceptor, ITickable {

	public static final long maxPower = 2000000;
	public long power;
	public int progress;
	public boolean needsProcess = true;
	public int maxProgress = 100;
	public boolean isProgressing;
	public boolean needsUpdate = false;
	public boolean needsTankTypeUpdate = false;
	public FluidTank[] tanks;
	public Fluid[] tankTypes;
	public ItemStack previousTemplate = ItemStack.EMPTY;
	public ItemStack previousTemplate2 = ItemStack.EMPTY;
	int consumption = 100;
	int speed = 100;
	private long detectPower;
	private boolean detectIsProgressing;
	private FluidTank[] detectTanks = new FluidTank[]{null, null, null, null};
	public boolean frame = false;
	public int anim;
	public int prevAnim;

	public TileEntityMachineChemical() {
		super(21);
		inventory = new ItemStackHandler(21) {
			@Override
			protected void onContentsChanged(int slot) {
				markDirty();
				OnContentsChanged(slot);
				super.onContentsChanged(slot);
			}
			@Override
			public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
				if(slot != 4)
					return super.insertItem(slot, stack, simulate);
				return stack;
			}

			@Override
			public ItemStack extractItem(int slot, int amount, boolean simulate) {
				if(slot != 4)
					return super.extractItem(slot, amount, simulate);
				return ItemStack.EMPTY;
			}
		};
		tanks = new FluidTank[4];
		tanks[0] = new FluidTank(24000);
		tanks[1] = new FluidTank(24000);
		tanks[2] = new FluidTank(24000);
		tanks[3] = new FluidTank(24000);
		tankTypes = new Fluid[]{null, null, null, null};
	}

	public void OnContentsChanged(int slot) {
		this.needsProcess = true;
	}

	@Override
	public boolean canInsertItem(int slot, ItemStack itemStack, int amount) {
		if(slot < 13 || slot > 16) {
			return false;
		}

		if(itemStack.isEmpty()) {
			return false;
		}

		ItemStack templateStack = inventory.getStackInSlot(4);
		if(templateStack.isEmpty()) {
			return false;
		}

		List<AStack> recipe = ChemplantRecipes.getChemInputFromTempate(templateStack);
		if(recipe == null) {
			return false;
		}

		ItemStack[] outputs = ChemplantRecipes.getChemOutputFromTempate(templateStack);
		if(outputs == null || Library.isArrayEmpty(outputs)) {
			return false;
		}

		ItemStack compareStack = itemStack.copy();
		compareStack.setCount(1);

		int ingredientIndex = -1;
		for(int i = 0; i < recipe.size(); i++) {
			AStack sing = recipe.get(i).copy();
			sing.singulize();
			if(sing.isApplicable(compareStack)) {
				ingredientIndex = i;
				break;
			}
		}

		if(ingredientIndex == -1) {
			return false;
		}

		List<Integer> validSlots = getValidSlotsForIngredient(recipe, ingredientIndex);
		return validSlots.contains(slot);
	}

	private List<Integer> getValidSlotsForIngredient(List<AStack> recipe, int ingredientIndex) {
		List<Integer> result = new ArrayList<>();

		int[] slotOwner = new int[21];
		for(int k = 0; k < 21; k++) {
			slotOwner[k] = -1;
		}

		for(int k = 13; k < 17; k++) {
			ItemStack slotStack = inventory.getStackInSlot(k);
			if(!slotStack.isEmpty()) {
				ItemStack compare = slotStack.copy();
				compare.setCount(1);
				for(int i = 0; i < recipe.size(); i++) {
					AStack sing = recipe.get(i).copy();
					sing.singulize();
					if(sing.isApplicable(compare)) {
						slotOwner[k] = i;
						break;
					}
				}
			}
		}

		AStack ingredient = recipe.get(ingredientIndex);
		AStack singularized = ingredient.copy();
		singularized.singulize();

		float maxStackSize = ingredient.getStack().getMaxStackSize();
		int stackCount = (int) Math.ceil(ingredient.count() / maxStackSize);
		int stacksFound = 0;

		for(int k = 13; k < 17; k++) {
			if(slotOwner[k] == ingredientIndex) {
				stacksFound++;
				ItemStack slotStack = inventory.getStackInSlot(k);
				if(slotStack.getCount() < slotStack.getMaxStackSize()) {
					result.add(k);
				}
			}
		}

		int reservedFreeSlots = 0;
		for(int i = 0; i < ingredientIndex; i++) {
			AStack prevIngredient = recipe.get(i);
			float prevMaxStack = prevIngredient.getStack().getMaxStackSize();
			int prevStackCount = (int) Math.ceil(prevIngredient.count() / prevMaxStack);

			int prevStacksFound = 0;
			for(int k = 13; k < 17; k++) {
				if(slotOwner[k] == i) {
					prevStacksFound++;
				}
			}

			reservedFreeSlots += Math.max(0, prevStackCount - prevStacksFound);
		}

		int freeSlotsNeeded = stackCount - stacksFound;
		if(freeSlotsNeeded > 0) {
			int skipped = 0;
			for(int k = 13; k < 17; k++) {
				if(slotOwner[k] == -1 && inventory.getStackInSlot(k).isEmpty()) {
					if(skipped < reservedFreeSlots) {
						skipped++;
						continue;
					}
					result.add(k);
					freeSlotsNeeded--;
					if(freeSlotsNeeded <= 0) {
						break;
					}
				}
			}
		}

		return result;
	}

	@Override
	public boolean canExtractItem(int slot, ItemStack itemStack, int amount){
		if(slot == 5 || slot == 6 || slot == 7 || slot == 8) {
			return true;
		}
		return false;
	}

	@Override
	public int[] getAccessibleSlotsFromSide(EnumFacing face) {
		return new int[] { 5, 6, 7, 8, 13, 14, 15, 16 };
	}

	public boolean isUseableByPlayer(EntityPlayer player) {
		if(world.getTileEntity(pos) != this) {
			return false;
		} else {
			return player.getDistanceSqToCenter(pos) <= 128;
		}
	}

	@Override
	public void readFromNBT(NBTTagCompound nbt) {
		super.readFromNBT(nbt);
		String[] types;

		this.power = nbt.getLong("powerTime");
		detectPower = power + 1;
		isProgressing = nbt.getBoolean("progressing");
		detectIsProgressing = !isProgressing;

		tanks[0].readFromNBT(nbt.getCompoundTag("input1"));
		tanks[1].readFromNBT(nbt.getCompoundTag("input2"));
		tanks[2].readFromNBT(nbt.getCompoundTag("output1"));
		tanks[3].readFromNBT(nbt.getCompoundTag("output2"));

		types = new String[]{nbt.getString("tankType0"), nbt.getString("tankType1"), nbt.getString("tankType2"), nbt.getString("tankType3")};

		if(!types[0].equals("empty")) {
			tankTypes[0] = FluidRegistry.getFluid(types[0]);
		} else {
			tankTypes[0] = null;
		}
		if(!types[1].equals("empty")) {
			tankTypes[1] = FluidRegistry.getFluid(types[1]);
		} else {
			tankTypes[1] = null;
		}
		if(!types[2].equals("empty")) {
			tankTypes[2] = FluidRegistry.getFluid(types[2]);
		} else {
			tankTypes[2] = null;
		}
		if(!types[3].equals("empty")) {
			tankTypes[3] = FluidRegistry.getFluid(types[3]);
		} else {
			tankTypes[3] = null;
		}
		if(nbt.hasKey("inventory"))
			inventory.deserializeNBT((NBTTagCompound) nbt.getTag("inventory"));
	}

	@Override
	public NBTTagCompound writeToNBT(NBTTagCompound nbt) {
		super.writeToNBT(nbt);
		nbt.setLong("powerTime", power);
		String[] types = new String[]{tankTypes[0] != null ? tankTypes[0].getName() : "empty", tankTypes[1] != null ? tankTypes[1].getName() : "empty", tankTypes[2] != null ? tankTypes[2].getName() : "empty", tankTypes[3] != null ? tankTypes[3].getName() : "empty"};

		nbt.setBoolean("progressing", isProgressing);

		NBTTagCompound input1 = new NBTTagCompound();
		NBTTagCompound input2 = new NBTTagCompound();
		NBTTagCompound output1 = new NBTTagCompound();
		NBTTagCompound output2 = new NBTTagCompound();

		tanks[0].writeToNBT(input1);
		tanks[1].writeToNBT(input2);
		tanks[2].writeToNBT(output1);
		tanks[3].writeToNBT(output2);

		nbt.setTag("input1", input1);
		nbt.setTag("input2", input2);
		nbt.setTag("output1", output1);
		nbt.setTag("output2", output2);

		nbt.setString("tankType0", types[0] != null ? types[0] : "empty");
		nbt.setString("tankType1", types[1] != null ? types[1] : "empty");
		nbt.setString("tankType2", types[2] != null ? types[2] : "empty");
		nbt.setString("tankType3", types[3] != null ? types[3] : "empty");

		NBTTagCompound inv = inventory.serializeNBT();
		nbt.setTag("inventory", inv);
		return nbt;
	}

	public long getPowerScaled(long i) {
		return (power * i) / maxPower;
	}

	public int getProgressScaled(int i) {
		return (progress * i) / Math.max(10, maxProgress);
	}

	@Override
	public void update() {
		needsTankTypeUpdate = previousTemplate2 != inventory.getStackInSlot(4);
		previousTemplate2 = inventory.getStackInSlot(4);
		this.consumption = 100;
		this.speed = 100;

		double c = 100;
		double s = 100;

		for(int i = 1; i < 4; i++) {
			ItemStack stack = inventory.getStackInSlot(i);

			if(!stack.isEmpty()) {
				if(stack.getItem() == ModItems.upgrade_speed_1) {
					s *= 0.75;
					c *= 3;
				}
				if(stack.getItem() == ModItems.upgrade_speed_2) {
					s *= 0.65;
					c *= 6;
				}
				if(stack.getItem() == ModItems.upgrade_speed_3) {
					s *= 0.5;
					c *= 9;
				}
				if(stack.getItem() == ModItems.upgrade_power_1) {
					c *= 0.8;
					s *= 1.25;
				}
				if(stack.getItem() == ModItems.upgrade_power_2) {
					c *= 0.4;
					s *= 1.5;
				}
				if(stack.getItem() == ModItems.upgrade_power_3) {
					c *= 0.2;
					s *= 2;
				}
			}
		}
		this.speed = (int) s;
		this.consumption = (int) c;

		if(speed < 2)
			speed = 2;
		if(consumption < 1)
			consumption = 1;
		if(this.needsTankTypeUpdate)
			setContainers();

		if(!world.isRemote) {
			if(needsUpdate) {
				needsUpdate = false;
			}

			isProgressing = false;

			this.updateConnections();

			power = Library.chargeTEFromItems(inventory, 0, power, maxPower);

			if(inputTankEmpty(0, 17) && inventory.getStackInSlot(19).isEmpty()){
				FFUtils.fillFluidContainer(inventory, tanks[0], 17, 19);
				FFUtils.moveItems(inventory, 17, 19, false);
			} if(inputValidForTank(0, 17)) {
				FFUtils.fillFromFluidContainer(inventory, tanks[0], 17, 19);
			}

			if(inputTankEmpty(1, 18) && inventory.getStackInSlot(20).isEmpty()){
				FFUtils.fillFluidContainer(inventory, tanks[1], 18, 20);
				FFUtils.moveItems(inventory, 18, 20, false);
			} else if(inputValidForTank(1, 18)){
				FFUtils.fillFromFluidContainer(inventory, tanks[1], 18, 20);
			}

			if((tankTypes[0] == FluidRegistry.WATER && inventory.getStackInSlot(17).getItem() == ModItems.inf_water) || inventory.getStackInSlot(17).getItem() == ModItems.fluid_barrel_infinite)
				FFUtils.fillFromFluidContainer(inventory, tanks[0], 17, 19);
			if((tankTypes[1] == FluidRegistry.WATER && inventory.getStackInSlot(18).getItem() == ModItems.inf_water) || inventory.getStackInSlot(18).getItem() == ModItems.fluid_barrel_infinite)
				FFUtils.fillFromFluidContainer(inventory, tanks[1], 18, 20);

			FFUtils.fillFluidContainer(inventory, tanks[2], 9, 11);
			FFUtils.fillFluidContainer(inventory, tanks[3], 10, 12);

			if(tanks[2].getFluidAmount() > 0 || tanks[3].getFluidAmount() > 0) {
				BlockPos[] ports = new BlockPos[] {
						pos.add(-1, 0, -2),
						pos.add(0, 0, -2),
						pos.add(1, 0, -2),
						pos.add(-1, 0, 2),
						pos.add(0, 0, 2),
						pos.add(1, 0, 2),
						pos.add(-2, 0, -1),
						pos.add(-2, 0, 0),
						pos.add(-2, 0, 1),
						pos.add(2, 0, -1),
						pos.add(2, 0, 0),
						pos.add(2, 0, 1)
				};
				for(BlockPos port : ports) {
					if(tanks[2].getFluidAmount() > 0)
						FFUtils.fillFluid(this, tanks[2], world, port, tanks[2].getCapacity() >> 1);
					if(tanks[3].getFluidAmount() > 0)
						FFUtils.fillFluid(this, tanks[3], world, port, tanks[3].getCapacity() >> 1);
				}
			}

			ItemStack[] itemOutputs = ChemplantRecipes.getChemOutputFromTempate(inventory.getStackInSlot(4));
			FluidStack[] fluidOutputs = ChemplantRecipes.getFluidOutputFromTempate(inventory.getStackInSlot(4));

			if(needsProcess && (itemOutputs != null || !Library.isArrayEmpty(fluidOutputs))) {

				List<AStack> itemInputs = ChemplantRecipes.getChemInputFromTempate(inventory.getStackInSlot(4));
				FluidStack[] fluidInputs = ChemplantRecipes.getFluidInputFromTempate(inventory.getStackInSlot(4));
				int duration = ChemplantRecipes.getProcessTime(inventory.getStackInSlot(4));

				this.maxProgress = (duration * speed) / 100;
				if(removeItems(itemInputs, cloneItemStackProper(inventory)) && hasFluidsStored(fluidInputs)) {
					if(power >= consumption) {
						if(hasSpaceForItems(itemOutputs) && hasSpaceForFluids(fluidOutputs)) {
							progress++;
							isProgressing = true;

							if(progress >= maxProgress) {
								progress = 0;
								if(itemOutputs != null)
									addItems(itemOutputs);
								if(fluidOutputs != null)
									addFluids(fluidOutputs);

								removeItems(itemInputs, inventory);
								removeFluids(fluidInputs);
								if(inventory.getStackInSlot(0).getItem() == ModItems.meteorite_sword_machined)
									inventory.setStackInSlot(0, new ItemStack(ModItems.meteorite_sword_treated));
							}

							power -= consumption;
						}
					}
				} else {
					progress = 0;
					needsProcess = true;
				}
			} else {
				progress = 0;
			}

			detectAndSendChanges();
		} else {
			this.prevAnim = this.anim;
			if(this.isProgressing) this.anim++;

			if(world.getTotalWorldTime() % 20 == 0) {
				frame = world.getBlockState(pos.up(3)).getBlock() != Blocks.AIR;
			}
		}
	}

	private void updateConnections() {
		ForgeDirection dir = ForgeDirection.getOrientation(this.getBlockMetadata() - BlockDummyable.offset);

		this.trySubscribe(world, pos.add(-1, 0, -2), ForgeDirection.NORTH);
		this.trySubscribe(world, pos.add(0, 0, -2), ForgeDirection.NORTH);
		this.trySubscribe(world, pos.add(1, 0, -2), ForgeDirection.NORTH);
		this.trySubscribe(world, pos.add(-1, 0, 2), ForgeDirection.SOUTH);
		this.trySubscribe(world, pos.add(0, 0, 2), ForgeDirection.SOUTH);
		this.trySubscribe(world, pos.add(1, 0, 2), ForgeDirection.SOUTH);
		this.trySubscribe(world, pos.add(-2, 0, -1), ForgeDirection.WEST);
		this.trySubscribe(world, pos.add(-2, 0, 0), ForgeDirection.WEST);
		this.trySubscribe(world, pos.add(-2, 0, 1), ForgeDirection.WEST);
		this.trySubscribe(world, pos.add(2, 0, -1), ForgeDirection.EAST);
		this.trySubscribe(world, pos.add(2, 0, 0), ForgeDirection.EAST);
		this.trySubscribe(world, pos.add(2, 0, 1), ForgeDirection.EAST);
	}

	private boolean validateTe(TileEntity te) {
		return te != null && te.hasCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, null);
	}

	private void syncRenderData() {
		if(world != null && !world.isRemote) {
			IBlockState state = world.getBlockState(pos);
			world.notifyBlockUpdate(pos, state, state, 3);
			markDirty();
		}
	}

	private void setContainers() {
		if(inventory.getStackInSlot(4) == ItemStack.EMPTY || (inventory.getStackInSlot(4) != ItemStack.EMPTY && !(inventory.getStackInSlot(4).getItem() instanceof ItemChemistryTemplate))) {
			tankTypes[0] = null;
			tankTypes[1] = null;
			tankTypes[2] = null;
			tankTypes[3] = null;
			tanks[0].setFluid(null);
			tanks[1].setFluid(null);
			tanks[2].setFluid(null);
			tanks[3].setFluid(null);
		} else {
			needsTankTypeUpdate = true;
			if(previousTemplate != ItemStack.EMPTY && ItemStack.areItemStacksEqual(previousTemplate, inventory.getStackInSlot(4))) {
				needsTankTypeUpdate = false;
			}
			previousTemplate = inventory.getStackInSlot(4).copy();

			FluidStack[] fluidInputs = ChemplantRecipes.getFluidInputFromTempate(inventory.getStackInSlot(4));
			FluidStack[] fluidOutputs = ChemplantRecipes.getFluidOutputFromTempate(inventory.getStackInSlot(4));

			if(fluidInputs == null) {
				tankTypes[0] = null;
				tankTypes[1] = null;
				tanks[0].setFluid(null);
				tanks[1].setFluid(null);
			} else {
				tankTypes[0] = fluidInputs[0] == null ? null : fluidInputs[0].getFluid();
				if(fluidInputs.length >= 2) {
					tankTypes[1] = fluidInputs[1] == null ? null : fluidInputs[1].getFluid();
				} else {
					tankTypes[1] = null;
					tanks[1].setFluid(null);
				}
			}

			if(fluidOutputs == null) {
				tankTypes[2] = null;
				tankTypes[3] = null;
				tanks[2].setFluid(null);
				tanks[3].setFluid(null);
			} else {
				tankTypes[2] = fluidOutputs[0] == null ? null : fluidOutputs[0].getFluid();
				if(fluidOutputs.length >= 2) {
					tankTypes[3] = fluidOutputs[1] == null ? null : fluidOutputs[1].getFluid();
				} else {
					tankTypes[3] = null;
					tanks[3].setFluid(null);
				}
			}

			if(tanks[0].getFluid() != null && tanks[0].getFluid().getFluid() != tankTypes[0]) {
				tanks[0].setFluid(null);
				if(needsTankTypeUpdate) {
					needsTankTypeUpdate = false;
				}
			}
			if(tanks[1].getFluid() != null && tanks[1].getFluid().getFluid() != tankTypes[1]) {
				tanks[1].setFluid(null);
				if(needsTankTypeUpdate) {
					needsTankTypeUpdate = false;
				}
			}
			if(tanks[2].getFluid() != null && tanks[2].getFluid().getFluid() != tankTypes[2]) {
				tanks[2].setFluid(null);
			}
			if(tanks[3].getFluid() != null && tanks[3].getFluid().getFluid() != tankTypes[3]) {
				tanks[3].setFluid(null);
				if(needsTankTypeUpdate) {
					needsTankTypeUpdate = false;
				}
			}
		}
		syncRenderData();
	}

	protected boolean inputValidForTank(int tank, int slot) {
		if(!inventory.getStackInSlot(slot).isEmpty() && tankTypes[tank] != null) {
			return FFUtils.checkRestrictions(inventory.getStackInSlot(slot), f -> f.getFluid() == tankTypes[tank]);
		}
		return false;
	}

	protected boolean inputTankEmpty(int tank, int slot) {
		if(!inventory.getStackInSlot(slot).isEmpty() && tankTypes[tank] != null) {
			ItemStack c = inventory.getStackInSlot(slot).copy();
			c.setCount(1);
			return FFUtils.isEmtpyFluidTank(c);
		}
		return false;
	}

	public boolean hasFluidsStored(FluidStack[] fluids) {
		if(Library.isArrayEmpty(fluids))
			return true;
		if(fluids.length == 2){
			if((fluids[0] == null || fluids[0].amount <= tanks[0].getFluidAmount()) && (fluids[1] == null || fluids[1].amount <= tanks[1].getFluidAmount()))
				return true;
		}else{
			if(fluids[0] == null || fluids[0].amount <= tanks[0].getFluidAmount())
				return true;
		}
		return false;
	}

	public boolean hasSpaceForFluids(FluidStack[] fluids) {
		if(Library.isArrayEmpty(fluids))
			return true;
		if(fluids.length == 2){
			if((fluids[0] == null || tanks[2].fill(fluids[0], false) == fluids[0].amount) && (fluids[1] == null || fluids[1] != null && tanks[3].fill(fluids[1], false) == fluids[1].amount))
				return true;
		}else{
			if(fluids[0] == null || tanks[2].fill(fluids[0], false) == fluids[0].amount)
				return true;
		}
		return false;
	}

	public void removeFluids(FluidStack[] fluids) {
		if(Library.isArrayEmpty(fluids))
			return;
		tanks[0].drain(fluids[0].amount, true);
		if(fluids.length == 2) {
			tanks[1].drain(fluids[1].amount, true);
		}
	}

	public boolean hasSpaceForItems(ItemStack[] stacks) {
		if(stacks == null)
			return true;
		if(stacks != null && Library.isArrayEmpty(stacks))
			return true;

		for(int i = 0; i < stacks.length; i++){
			if(inventory.getStackInSlot(5+i) == ItemStack.EMPTY) {
				continue;
			} else {
				if(Library.areItemStacksCompatible(stacks[i].copy(), inventory.getStackInSlot(5+i).copy(), false)){
					if(inventory.getStackInSlot(5+i).getCount() + stacks[i].getCount() <= inventory.getStackInSlot(5+i).getMaxStackSize()){
						continue;
					}
				}
				return false;
			}
		}
		return true;
	}

	public void addItems(ItemStack[] stacks) {
		if(stacks == null)
			return;
		if(stacks != null && Library.isArrayEmpty(stacks))
			return;

		for(int i = 0; i<stacks.length; i++){
			if(inventory.getStackInSlot(5+i) == ItemStack.EMPTY){
				inventory.setStackInSlot(5+i, stacks[i].copy());
			} else {
				inventory.getStackInSlot(5+i).setCount(inventory.getStackInSlot(5+i).getCount() + stacks[i].getCount());
			}
		}
	}

	public void addFluids(FluidStack[] stacks) {
		if(stacks != null){
			tanks[2].fill(stacks[0], true);
			if(stacks.length == 2){
				if(stacks[1] != null) {
					tanks[3].fill(stacks[1], true);
				}
			}
		}
	}

	public IItemHandlerModifiable cloneItemStackProper(IItemHandlerModifiable array) {
		IItemHandlerModifiable stack = new ItemStackHandler(array.getSlots());

		for(int i = 0; i < array.getSlots(); i++)
			if(array.getStackInSlot(i) != null)
				stack.setStackInSlot(i, array.getStackInSlot(i).copy());
			else
				stack.setStackInSlot(i, ItemStack.EMPTY);

		return stack;
	}

	private int getValidSlot(AStack nextIngredient) {
		int firstFreeSlot = -1;
		int stackCount = (int) Math.ceil(nextIngredient.count() / 64F);
		int stacksFound = 0;

		nextIngredient = nextIngredient.singulize();

		for(int k = 13; k < 17; k++) {
			if(stacksFound < stackCount) {
				ItemStack assStack = inventory.getStackInSlot(k).copy();
				if(assStack.isEmpty()) {
					if(firstFreeSlot < 13)
						firstFreeSlot = k;
					continue;
				} else {
					assStack.setCount(1);
					if(nextIngredient.isApplicable(assStack)) {
						if(inventory.getStackInSlot(k).getCount() < assStack.getMaxStackSize())
							return k;
						else
							stacksFound++;
					}
				}
			} else {
				return -1;
			}
		}
		if(firstFreeSlot < 13)
			return -2;
		return firstFreeSlot;
	}

	public boolean removeItems(List<AStack> stack, IItemHandlerModifiable array) {
		if(stack == null)
			return true;
		for(int i = 0; i < stack.size(); i++) {
			for(int j = 0; j < stack.get(i).count(); j++) {
				AStack sta = stack.get(i).copy();
				sta.setCount(1);
				if(!canRemoveItemFromArray(sta, array))
					return false;
			}
		}
		return true;
	}

	public boolean canRemoveItemFromArray(AStack stack, IItemHandlerModifiable array) {
		AStack st = stack.copy();

		for(int i = 6; i < 18; i++) {
			if(array.getStackInSlot(i).getItem() != Items.AIR) {
				ItemStack sta = array.getStackInSlot(i).copy();
				sta.setCount(1);

				if(st.isApplicable(sta) && array.getStackInSlot(i).getCount() > 0) {
					array.getStackInSlot(i).shrink(1);

					if(array.getStackInSlot(i).isEmpty())
						array.setStackInSlot(i, ItemStack.EMPTY);

					return true;
				}
			}
		}
		return false;
	}

	@Override
	public long getPower() {
		return power;
	}

	@Override
	public void setPower(long i) {
		power = i;
	}

	@Override
	public long getMaxPower() {
		return maxPower;
	}

	@Override
	public AxisAlignedBB getRenderBoundingBox() {
		return TileEntity.INFINITE_EXTENT_AABB;
	}

	@Override
	@SideOnly(Side.CLIENT)
	public double getMaxRenderDistanceSquared() {
		return 65536.0D;
	}

	@Override
	public SPacketUpdateTileEntity getUpdatePacket() {
		NBTTagCompound tag = new NBTTagCompound();
		writeToNBT(tag);
		return new SPacketUpdateTileEntity(pos, 0, tag);
	}

	@Override
	public void onDataPacket(NetworkManager net, SPacketUpdateTileEntity pkt) {
		readFromNBT(pkt.getNbtCompound());
	}

	@Override
	public void recievePacket(NBTTagCompound[] tags) {
		if(tags.length != 4) {
			return;
		} else {
			tanks[0].readFromNBT(tags[0]);
			tanks[1].readFromNBT(tags[1]);
			tanks[2].readFromNBT(tags[2]);
			tanks[3].readFromNBT(tags[3]);
		}
	}

	public void haveNeedProess() {
		this.needsProcess = true;
	}

	public ItemStack getStackInSlot(int i) {
		return inventory.getStackInSlot(i);
	}

	@Override
	public String getName() {
		return "container.chemical";
	}

	private void detectAndSendChanges() {
		PacketDispatcher.wrapper.sendToAll(new LoopedSoundPacket(pos.getX(), pos.getY(), pos.getZ()));

		boolean mark = false;

		if(detectIsProgressing != isProgressing) {
			mark = true;
			detectIsProgressing = isProgressing;
		}
		PacketDispatcher.wrapper.sendToAllAround(new TEChemicalPacket(pos.getX(), pos.getY(), pos.getZ(), isProgressing), new TargetPoint(world.provider.getDimension(), pos.getX(), pos.getY(), pos.getZ(), 100));
		if(detectPower != power) {
			mark = true;
			detectPower = power;
		}
		PacketDispatcher.wrapper.sendToAllAround(new AuxElectricityPacket(pos.getX(), pos.getY(), pos.getZ(), power), new TargetPoint(world.provider.getDimension(), pos.getX(), pos.getY(), pos.getZ(), 100));
		if(!FFUtils.areTanksEqual(detectTanks[0], tanks[0])) {
			detectTanks[0] = FFUtils.copyTank(tanks[0]);
			mark = true;
			needsUpdate = true;
		}
		if(!FFUtils.areTanksEqual(detectTanks[1], tanks[1])) {
			detectTanks[1] = FFUtils.copyTank(tanks[1]);
			mark = true;
			needsUpdate = true;
		}
		if(!FFUtils.areTanksEqual(detectTanks[2], tanks[2])) {
			detectTanks[2] = FFUtils.copyTank(tanks[2]);
			mark = true;
			needsUpdate = true;
		}
		if(!FFUtils.areTanksEqual(detectTanks[3], tanks[3])) {
			detectTanks[3] = FFUtils.copyTank(tanks[3]);
			mark = true;
			needsUpdate = true;
		}
		PacketDispatcher.wrapper.sendToAllAround(new FluidTankPacket(pos.getX(), pos.getY(), pos.getZ(), new FluidTank[]{tanks[0], tanks[1], tanks[2], tanks[3]}), new TargetPoint(world.provider.getDimension(), pos.getX(), pos.getY(), pos.getZ(), 100));

		if(mark)
			markDirty();
	}

	@Override
	public boolean hasCapability(Capability<?> capability, EnumFacing facing) {
		if(capability == CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY) {
			return true;
		}
		return super.hasCapability(capability, facing);
	}

	@Override
	public <T> T getCapability(Capability<T> capability, EnumFacing facing) {
		if(capability == CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY) {
			return CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY.cast(new ChemplantFluidHandler(tanks, tankTypes));
		}
		return super.getCapability(capability, facing);
	}

	private class ChemplantFluidHandler implements IFluidHandler {

		private FluidTank[] tanks;
		private Fluid[] tankTypes;

		public ChemplantFluidHandler(FluidTank[] tanks, Fluid[] tankTypes) {
			this.tanks = tanks;
			this.tankTypes = tankTypes;
		}

		@Override
		public IFluidTankProperties[] getTankProperties() {
			return new IFluidTankProperties[]{tanks[0].getTankProperties()[0], tanks[1].getTankProperties()[0], tanks[2].getTankProperties()[0], tanks[3].getTankProperties()[0]};
		}

		@Override
		public int fill(FluidStack resource, boolean doFill) {
			needsProcess = true;
			if(resource == null)
				return 0;
			if(tankTypes[0] != null && resource.getFluid() == tankTypes[0]) {
				return tanks[0].fill(resource, doFill);
			}
			if(tankTypes[1] != null && resource.getFluid() == tankTypes[1]) {
				return tanks[1].fill(resource, doFill);
			}
			return 0;
		}

		@Override
		public FluidStack drain(FluidStack resource, boolean doDrain) {
			if(resource == null)
				return null;
			if(tanks[2].getFluid() != null && resource.isFluidEqual(tanks[2].getFluid())) {
				return tanks[2].drain(resource.amount, doDrain);
			}
			if(tanks[3].getFluid() != null && resource.isFluidEqual(tanks[3].getFluid())) {
				return tanks[3].drain(resource.amount, doDrain);
			}
			return null;
		}

		@Override
		public FluidStack drain(int maxDrain, boolean doDrain) {
			if(tanks[2].getFluid() != null) {
				return tanks[2].drain(maxDrain, doDrain);
			}
			if(tanks[3].getFluid() != null) {
				return tanks[3].drain(maxDrain, doDrain);
			}
			return null;
		}
	}
}