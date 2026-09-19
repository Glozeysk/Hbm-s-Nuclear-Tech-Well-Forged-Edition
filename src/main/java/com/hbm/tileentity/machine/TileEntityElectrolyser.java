package com.hbm.tileentity.machine;

import com.hbm.blocks.BlockDummyable;
import com.hbm.blocks.ModBlocks;
import com.hbm.forgefluid.FFUtils;
import com.hbm.interfaces.IControlReceiver;
import com.hbm.inventory.ElectrolyserFluidRecipes;
import com.hbm.inventory.ElectrolyserFluidRecipes.ElectrolysisRecipe;
import com.hbm.inventory.ElectrolyserMetalRecipes;
import com.hbm.inventory.ElectrolyserMetalRecipes.ElectrolysisMetalRecipe;
import com.hbm.inventory.UpgradeManager;
import com.hbm.items.machine.ItemMachineUpgrade.UpgradeType;
import com.hbm.lib.ForgeDirection;
import com.hbm.lib.Library;
import com.hbm.main.MainRegistry;
import com.hbm.tileentity.TileEntityMachineBase;

import api.hbm.energy.IEnergyUser;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ITickable;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidTank;
import net.minecraftforge.fluids.FluidUtil;
import net.minecraftforge.fluids.capability.CapabilityFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.IFluidTankProperties;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

//Ported from NTM:CE without fluid identifiers, item byproducts of the fluid mode and molten material output.
//Only the process of the selected mode runs; the mode also picks the GUI page.
//Slots: 0 battery, 1-2 upgrades, 3-4 / 5-6 / 7-8 containers of tank 0 / 1 / 2, 9 crystal, 10-11 metal-fluid containers, 12-20 metal outputs.
public class TileEntityElectrolyser extends TileEntityMachineBase implements ITickable, IEnergyUser, IFluidHandler, IControlReceiver {

	public static final long maxPower = 20_000_000;
	public static final int usageOreBase = 10_000;
	public static final int usageFluidBase = 10_000;
	public static final int OUT_START = 12;
	public static final int OUT_END = 20;

	public long power;
	public int usageOre;
	public int usageFluid;
	public int progressFluid;
	public int durationFluid = 100;
	public int progressOre;
	public int durationMetal = 600;
	//0 = fluid page, 1 = metal page
	public int mode = 0;

	//0 = input (type picked from the first recipe fluid), 1-2 = outputs, 3 = metal-mode fluid (type picked from the first metal-recipe fluid)
	public FluidTank[] tanks;

	private final UpgradeManager upgradeManager = new UpgradeManager();

	public TileEntityElectrolyser() {
		super(21);
		tanks = new FluidTank[4];
		for(int i = 0; i < 4; i++)
			tanks[i] = new FluidTank(16_000);
	}

	@Override
	public String getName() {
		return "container.machineElectrolyser";
	}

	@Override
	public void update() {
		if(world.isRemote)
			return;

		power = Library.chargeTEFromItems(inventory, 0, power, maxPower);
		updateConnections();

		if(isFluidInput(inventory.getStackInSlot(3), tanks[0]))
			FFUtils.fillFromFluidContainer(inventory, tanks[0], 3, 4);
		FFUtils.fillFluidContainer(inventory, tanks[1], 5, 6);
		FFUtils.fillFluidContainer(inventory, tanks[2], 7, 8);
		if(isMetalFluidInput(inventory.getStackInSlot(10)))
			FFUtils.fillFromFluidContainer(inventory, tanks[3], 10, 11);

		upgradeManager.eval(inventory, 1, 2);
		int speed = Math.min(upgradeManager.getLevel(UpgradeType.SPEED), 3);
		int powerSaving = Math.min(upgradeManager.getLevel(UpgradeType.POWER), 3);
		int overdrive = Math.min(upgradeManager.getLevel(UpgradeType.OVERDRIVE), 3);

		usageOre = usageOreBase - usageOreBase * powerSaving / 4 + usageOreBase * speed;
		usageFluid = usageFluidBase - usageFluidBase * powerSaving / 4 + usageFluidBase * speed;

		ElectrolysisRecipe fluidRecipe = ElectrolyserFluidRecipes.getRecipe(tanks[0].getFluid() != null ? tanks[0].getFluid().getFluid() : null);
		ElectrolysisMetalRecipe metalRecipe = ElectrolyserMetalRecipes.getRecipe(inventory.getStackInSlot(9));
		durationFluid = getDuration(fluidRecipe != null ? fluidRecipe.duration : 100, speed, powerSaving);
		durationMetal = getDuration(metalRecipe != null ? metalRecipe.duration : 600, speed, powerSaving);

		boolean fluidRan = false;
		boolean metalRan = false;
		int cycles = Math.min(1 + overdrive * 2, 7);

		for(int i = 0; i < cycles; i++) {
			if(canProcessFluid(fluidRecipe)) {
				fluidRan = true;
				progressFluid++;
				power -= usageFluid;
				if(progressFluid >= durationFluid) {
					processFluid(fluidRecipe);
					progressFluid = 0;
					markDirty();
				}
			}

			metalRecipe = ElectrolyserMetalRecipes.getRecipe(inventory.getStackInSlot(9));
			if(canProcessMetal(metalRecipe)) {
				metalRan = true;
				progressOre++;
				power -= usageOre;
				if(progressOre >= durationMetal) {
					processMetal(metalRecipe);
					progressOre = 0;
					markDirty();
				}
			}
		}

		//a stalled or switched recipe starts over instead of finishing on leftover progress
		if(!fluidRan)
			progressFluid = 0;
		if(!metalRan)
			progressOre = 0;

		networkPackNT(50);
	}

	private static int getDuration(int base, int speed, int powerSaving) {
		int level = speed - Math.min(powerSaving, 1);
		return (int) Math.ceil(base * Math.max(1F - 0.25F * level, 0.2F));
	}

	private boolean canProcessFluid(ElectrolysisRecipe recipe) {
		if(mode != 0)
			return false;
		if(recipe == null || power < usageFluid)
			return false;
		if(tanks[0].getFluidAmount() < recipe.amount)
			return false;
		return outputFits(tanks[1], recipe.output1) && outputFits(tanks[2], recipe.output2);
	}

	//CE retyped the output tank and converted a leftover of another fluid in place
	private static boolean outputFits(FluidTank tank, FluidStack out) {
		if(out == null)
			return true;
		if(tank.getFluidAmount() > 0 && tank.getFluid().getFluid() != out.getFluid())
			return false;
		return tank.getFluidAmount() + out.amount <= tank.getCapacity();
	}

	private void processFluid(ElectrolysisRecipe recipe) {
		tanks[0].drain(recipe.amount, true);
		tanks[1].fill(recipe.output1.copy(), true);
		if(recipe.output2 != null)
			tanks[2].fill(recipe.output2.copy(), true);
	}

	private boolean canProcessMetal(ElectrolysisMetalRecipe recipe) {
		if(mode != 1)
			return false;
		if(recipe == null || power < usageOre)
			return false;
		if(inventory.getStackInSlot(9).getCount() < recipe.input.count())
			return false;
		if(recipe.fluid != null && (tanks[3].getFluid() == null || tanks[3].getFluid().getFluid() != recipe.fluid.getFluid() || tanks[3].getFluidAmount() < recipe.fluid.amount))
			return false;
		return placeOutputs(recipe.outputs, false);
	}

	private void processMetal(ElectrolysisMetalRecipe recipe) {
		placeOutputs(recipe.outputs, true);
		if(recipe.fluid != null)
			tanks[3].drain(recipe.fluid.amount, true);
		inventory.getStackInSlot(9).shrink(recipe.input.count());
	}

	//output slots are a shared buffer like the shredder's: fill matching stacks first, then empty slots
	private boolean placeOutputs(ItemStack[] outputs, boolean doPlace) {
		ItemStack[] slots = new ItemStack[OUT_END - OUT_START + 1];
		for(int i = 0; i < slots.length; i++)
			slots[i] = inventory.getStackInSlot(OUT_START + i).copy();

		for(ItemStack out : outputs) {
			int left = out.getCount();
			for(int i = 0; i < slots.length && left > 0; i++) {
				ItemStack slot = slots[i];
				if(!slot.isEmpty() && slot.isItemEqual(out) && ItemStack.areItemStackTagsEqual(slot, out)) {
					int move = Math.min(left, slot.getMaxStackSize() - slot.getCount());
					slot.grow(move);
					left -= move;
				}
			}
			for(int i = 0; i < slots.length && left > 0; i++) {
				if(slots[i].isEmpty()) {
					int move = Math.min(left, out.getMaxStackSize());
					slots[i] = out.copy();
					slots[i].setCount(move);
					left -= move;
				}
			}
			if(left > 0)
				return false;
		}

		if(doPlace)
			for(int i = 0; i < slots.length; i++)
				inventory.setStackInSlot(OUT_START + i, slots[i]);
		return true;
	}

	private boolean isFluidInput(ItemStack stack, FluidTank tank) {
		if(stack.isEmpty())
			return false;
		FluidStack contained = FluidUtil.getFluidContained(stack);
		if(contained == null || ElectrolyserFluidRecipes.getRecipe(contained.getFluid()) == null)
			return false;
		return tank.getFluidAmount() == 0 || tank.getFluid().getFluid() == contained.getFluid();
	}

	private boolean isMetalFluidInput(ItemStack stack) {
		if(stack.isEmpty())
			return false;
		FluidStack contained = FluidUtil.getFluidContained(stack);
		return contained != null && acceptsMetalFluid(contained.getFluid());
	}

	//the metal tank takes any metal-recipe fluid while empty, then only that fluid
	private boolean acceptsMetalFluid(Fluid fluid) {
		if(!ElectrolyserMetalRecipes.isRecipeFluid(fluid))
			return false;
		return tanks[3].getFluidAmount() == 0 || tanks[3].getFluid().getFluid() == fluid;
	}

	//cable spots behind the 6 ports of MachineElectrolyser.fillSpace
	private void updateConnections() {
		ForgeDirection dir = ForgeDirection.getOrientation(this.getBlockMetadata() - BlockDummyable.offset);
		ForgeDirection rot = dir.getRotation(ForgeDirection.UP);

		for(int side = -1; side <= 1; side += 2) {
			ForgeDirection face = side < 0 ? dir.getOpposite() : dir;
			for(int r = -1; r <= 1; r++)
				this.trySubscribe(world, new BlockPos(pos.getX() + dir.offsetX * 6 * side + rot.offsetX * r, pos.getY(), pos.getZ() + dir.offsetZ * 6 * side + rot.offsetZ * r), face);
		}
	}

	public int getGuiID() {
		return mode == 1 ? ModBlocks.guiID_machine_electrolyser_metal : ModBlocks.guiID_machine_electrolyser_fluid;
	}

	@Override
	public boolean hasPermission(EntityPlayer player) {
		return this.isUseableByPlayer(player);
	}

	@Override
	public void receiveControl(NBTTagCompound data) { }

	@Override
	public void receiveControl(EntityPlayerMP player, NBTTagCompound data) {
		if(data.hasKey("fluid"))
			mode = 0;
		if(data.hasKey("metal"))
			mode = 1;
		markDirty();
		player.openGui(MainRegistry.instance, getGuiID(), world, pos.getX(), pos.getY(), pos.getZ());
	}

	@Override
	public void serialize(ByteBuf buf) {
		buf.writeLong(power);
		buf.writeInt(progressFluid);
		buf.writeInt(progressOre);
		buf.writeInt(usageFluid);
		buf.writeInt(usageOre);
		buf.writeInt(durationFluid);
		buf.writeInt(durationMetal);
		for(FluidTank tank : tanks) {
			if(tank.getFluid() != null) {
				buf.writeBoolean(true);
				ByteBufUtils.writeUTF8String(buf, tank.getFluid().getFluid().getName());
				buf.writeInt(tank.getFluidAmount());
			} else {
				buf.writeBoolean(false);
			}
		}
	}

	@Override
	public void deserialize(ByteBuf buf) {
		power = buf.readLong();
		progressFluid = buf.readInt();
		progressOre = buf.readInt();
		usageFluid = buf.readInt();
		usageOre = buf.readInt();
		durationFluid = buf.readInt();
		durationMetal = buf.readInt();
		for(FluidTank tank : tanks) {
			if(buf.readBoolean()) {
				Fluid fluid = FluidRegistry.getFluid(ByteBufUtils.readUTF8String(buf));
				int amount = buf.readInt();
				tank.setFluid(fluid != null ? new FluidStack(fluid, amount) : null);
			} else {
				tank.setFluid(null);
			}
		}
	}

	@Override
	public void readFromNBT(NBTTagCompound nbt) {
		super.readFromNBT(nbt);
		power = nbt.getLong("power");
		progressFluid = nbt.getInteger("progressFluid");
		progressOre = nbt.getInteger("progressOre");
		mode = nbt.getInteger("mode");
		if(nbt.hasKey("tanks"))
			FFUtils.deserializeTankArray(nbt.getTagList("tanks", 10), tanks);
	}

	@Override
	public NBTTagCompound writeToNBT(NBTTagCompound nbt) {
		nbt.setLong("power", power);
		nbt.setInteger("progressFluid", progressFluid);
		nbt.setInteger("progressOre", progressOre);
		nbt.setInteger("mode", mode);
		nbt.setTag("tanks", FFUtils.serializeTankArray(tanks));
		return super.writeToNBT(nbt);
	}

	@Override
	public int[] getAccessibleSlotsFromSide(EnumFacing e) {
		return new int[] {9, 12, 13, 14, 15, 16, 17, 18, 19, 20};
	}

	@Override
	public boolean canInsertItem(int slot, ItemStack stack, int amount) {
		return slot == 9 && ElectrolyserMetalRecipes.getRecipe(stack) != null;
	}

	@Override
	public boolean canExtractItem(int slot, ItemStack stack, int amount) {
		return slot >= OUT_START && slot <= OUT_END;
	}

	private AxisAlignedBB bb = null;

	@Override
	public AxisAlignedBB getRenderBoundingBox() {
		if(bb == null)
			bb = new AxisAlignedBB(pos.getX() - 5, pos.getY(), pos.getZ() - 5, pos.getX() + 6, pos.getY() + 4, pos.getZ() + 6);
		return bb;
	}

	@Override
	@SideOnly(Side.CLIENT)
	public double getMaxRenderDistanceSquared() {
		return 65536.0D;
	}

	@Override
	public void setPower(long power) {
		this.power = power;
	}

	@Override
	public long getPower() {
		return power;
	}

	@Override
	public long getMaxPower() {
		return maxPower;
	}

	@Override
	public IFluidTankProperties[] getTankProperties() {
		return new IFluidTankProperties[] {tanks[0].getTankProperties()[0], tanks[1].getTankProperties()[0], tanks[2].getTankProperties()[0], tanks[3].getTankProperties()[0]};
	}

	@Override
	public int fill(FluidStack resource, boolean doFill) {
		if(resource == null || resource.getFluid() == null)
			return 0;
		if(acceptsMetalFluid(resource.getFluid()))
			return tanks[3].fill(resource, doFill);
		if(tanks[0].getFluidAmount() > 0)
			return resource.isFluidEqual(tanks[0].getFluid()) ? tanks[0].fill(resource, doFill) : 0;
		return ElectrolyserFluidRecipes.getRecipe(resource.getFluid()) != null ? tanks[0].fill(resource, doFill) : 0;
	}

	@Override
	public FluidStack drain(FluidStack resource, boolean doDrain) {
		if(resource == null)
			return null;
		for(int i = 1; i <= 2; i++)
			if(tanks[i].getFluidAmount() > 0 && resource.isFluidEqual(tanks[i].getFluid()))
				return tanks[i].drain(resource.amount, doDrain);
		return null;
	}

	@Override
	public FluidStack drain(int maxDrain, boolean doDrain) {
		for(int i = 1; i <= 2; i++)
			if(tanks[i].getFluidAmount() > 0)
				return tanks[i].drain(maxDrain, doDrain);
		return null;
	}

	@Override
	public boolean hasCapability(Capability<?> capability, EnumFacing facing) {
		return capability == CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY || super.hasCapability(capability, facing);
	}

	@Override
	public <T> T getCapability(Capability<T> capability, EnumFacing facing) {
		if(capability == CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY)
			return CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY.cast(this);
		return super.getCapability(capability, facing);
	}
}
