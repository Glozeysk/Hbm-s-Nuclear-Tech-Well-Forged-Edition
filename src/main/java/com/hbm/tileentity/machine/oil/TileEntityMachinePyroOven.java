package com.hbm.tileentity.machine.oil;

import com.hbm.blocks.BlockDummyable;
import com.hbm.forgefluid.FFUtils;
import com.hbm.inventory.PyroOvenRecipes;
import com.hbm.inventory.PyroOvenRecipes.PyroOvenRecipe;
import com.hbm.inventory.UpgradeManager;
import com.hbm.items.machine.ItemMachineUpgrade.UpgradeType;
import com.hbm.lib.ForgeDirection;
import com.hbm.lib.HBMSoundHandler;
import com.hbm.lib.Library;
import com.hbm.main.MainRegistry;
import com.hbm.sound.AudioWrapper;
import com.hbm.tileentity.TileEntityMachineBase;

import api.hbm.energy.IEnergyUser;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.ITickable;
import net.minecraft.util.SoundCategory;
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
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

//Ported from NTM:CE without pollution and the fluid identifier; the input tank takes any recipe fluid while empty.
//Slots: 0 battery, 1 input item, 2 output item, 3-4 upgrades.
public class TileEntityMachinePyroOven extends TileEntityMachineBase implements ITickable, IEnergyUser, IFluidHandler {

	public static final long maxPower = 10_000_000;
	public static final int consumption = 10_000;
	public long power;
	public boolean isProgressing;
	public float progress;

	public int prevAnim;
	public int anim = 0;

	//0 = input, 1 = output
	public FluidTank[] tanks;

	private final UpgradeManager upgradeManager = new UpgradeManager();
	private PyroOvenRecipe lastValidRecipe;
	private AudioWrapper audio;

	public TileEntityMachinePyroOven() {
		super(5);
		tanks = new FluidTank[2];
		tanks[0] = new FluidTank(24_000);
		tanks[1] = new FluidTank(24_000);
	}

	@Override
	public String getName() {
		return "container.machinePyroOven";
	}

	@Override
	public void update() {

		if(!world.isRemote) {

			power = Library.chargeTEFromItems(inventory, 0, power, maxPower);
			updateConnections();

			upgradeManager.eval(inventory, 3, 4);
			int speed = Math.min(upgradeManager.getLevel(UpgradeType.SPEED), 3);
			int powerSaving = Math.min(upgradeManager.getLevel(UpgradeType.POWER), 3);
			int overdrive = Math.min(upgradeManager.getLevel(UpgradeType.OVERDRIVE), 3);

			isProgressing = false;
			PyroOvenRecipe recipe = getMatchingRecipe();
			int cost = getConsumption(speed + overdrive * 2, powerSaving);

			if(recipe != null && canProcess(recipe, cost)) {
				progress += 1F / Math.max((recipe.duration - speed * (recipe.duration / 4)) / (overdrive * 2 + 1), 1);
				isProgressing = true;
				power -= cost;

				if(progress >= 1F) {
					progress = 0F;
					finishRecipe(recipe);
					markDirty();
				}
			} else {
				progress = 0F;
			}

			networkPackNT(50);

		} else {

			prevAnim = anim;
			ForgeDirection dir = ForgeDirection.getOrientation(this.getBlockMetadata() - BlockDummyable.offset);
			ForgeDirection rot = dir.getRotation(ForgeDirection.DOWN);

			if(isProgressing) {
				anim++;

				float volume = getVolume(1F);
				if(audio == null) {
					audio = MainRegistry.proxy.getLoopedSound(HBMSoundHandler.pyroOperate, SoundCategory.BLOCKS, pos.getX(), pos.getY(), pos.getZ(), volume, 1.0F);
					audio.updateRange(15F);
					audio.startSound();
				}
				audio.updateVolume(volume);

				EntityPlayer player = MainRegistry.proxy.me();
				if(player != null && player.getDistance(pos.getX() + 0.5, pos.getY() + 3, pos.getZ() + 0.5) < 50) {
					double[] offsets = {-2.375, -0.875, 0.875, 2.375};
					for(double o : offsets)
						if(world.rand.nextInt(20) == 0)
							world.spawnParticle(EnumParticleTypes.CLOUD, pos.getX() + 0.5 - rot.offsetX + dir.offsetX * o, pos.getY() + 3, pos.getZ() + 0.5 - rot.offsetZ + dir.offsetZ * o, 0.0, 0.05, 0.0);
				}

			} else {
				stopAudio();
			}
		}
	}

	public static int getConsumption(int speed, int powerSaving) {
		return (int) (consumption * Math.pow(speed + 1, 2)) / (powerSaving + 1);
	}

	//first recipe whose inputs are fully present; recipes are not expected to overlap
	private PyroOvenRecipe getMatchingRecipe() {
		if(lastValidRecipe != null && doesRecipeMatch(lastValidRecipe))
			return lastValidRecipe;

		for(PyroOvenRecipe recipe : PyroOvenRecipes.recipes) {
			if(doesRecipeMatch(recipe)) {
				lastValidRecipe = recipe;
				return recipe;
			}
		}
		return null;
	}

	private boolean doesRecipeMatch(PyroOvenRecipe recipe) {
		if(recipe.inputFluid != null) {
			if(tanks[0].getFluid() == null || tanks[0].getFluid().getFluid() != recipe.inputFluid.getFluid())
				return false;
			if(tanks[0].getFluidAmount() < recipe.inputFluid.amount)
				return false;
		}

		ItemStack input = inventory.getStackInSlot(1);
		if(recipe.inputItem != null)
			return !input.isEmpty() && recipe.inputItem.matchesRecipe(input, true) && input.getCount() >= recipe.inputItem.count();

		return input.isEmpty();
	}

	private boolean canProcess(PyroOvenRecipe recipe, int cost) {
		//CE checked the cost without overdrive and could drain the buffer below zero
		if(power < cost)
			return false;

		if(recipe.outputFluid != null) {
			//CE retyped the tank and converted a leftover of another fluid in place
			if(tanks[1].getFluidAmount() > 0 && tanks[1].getFluid().getFluid() != recipe.outputFluid.getFluid())
				return false;
			if(tanks[1].getFluidAmount() + recipe.outputFluid.amount > tanks[1].getCapacity())
				return false;
		}

		if(recipe.outputItem != null) {
			ItemStack output = inventory.getStackInSlot(2);
			if(!output.isEmpty()) {
				if(output.getItem() != recipe.outputItem.getItem() || output.getItemDamage() != recipe.outputItem.getItemDamage() || !ItemStack.areItemStackTagsEqual(output, recipe.outputItem))
					return false;
				if(output.getCount() + recipe.outputItem.getCount() > output.getMaxStackSize())
					return false;
			}
		}

		return true;
	}

	private void finishRecipe(PyroOvenRecipe recipe) {
		if(recipe.outputItem != null) {
			if(inventory.getStackInSlot(2).isEmpty())
				inventory.setStackInSlot(2, recipe.outputItem.copy());
			else
				inventory.getStackInSlot(2).grow(recipe.outputItem.getCount());
		}
		if(recipe.outputFluid != null)
			tanks[1].fill(recipe.outputFluid.copy(), true);
		if(recipe.inputItem != null)
			inventory.getStackInSlot(1).shrink(recipe.inputItem.count());
		if(recipe.inputFluid != null)
			tanks[0].drain(recipe.inputFluid.amount, true);
	}

	//cable spots behind the 5 ports of MachinePyroOven.fillSpace
	private void updateConnections() {
		ForgeDirection dir = ForgeDirection.getOrientation(this.getBlockMetadata() - BlockDummyable.offset);
		ForgeDirection rot = dir.getRotation(ForgeDirection.DOWN);

		for(int i = -2; i <= 2; i++)
			this.trySubscribe(world, new BlockPos(pos.getX() + dir.offsetX * i + rot.offsetX * 3, pos.getY(), pos.getZ() + dir.offsetZ * i + rot.offsetZ * 3), rot);
	}

	private void stopAudio() {
		if(audio != null) {
			audio.stopSound();
			audio = null;
		}
	}

	@Override
	public void onChunkUnload() {
		super.onChunkUnload();
		stopAudio();
	}

	@Override
	public void invalidate() {
		super.invalidate();
		stopAudio();
	}

	@Override
	public void serialize(ByteBuf buf) {
		super.serialize(buf);
		buf.writeLong(power);
		buf.writeBoolean(isProgressing);
		buf.writeFloat(progress);
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
		super.deserialize(buf);
		power = buf.readLong();
		isProgressing = buf.readBoolean();
		progress = buf.readFloat();
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
		progress = nbt.getFloat("prog");
		if(nbt.hasKey("tanks"))
			FFUtils.deserializeTankArray(nbt.getTagList("tanks", 10), tanks);
	}

	@Override
	public NBTTagCompound writeToNBT(NBTTagCompound nbt) {
		nbt.setLong("power", power);
		nbt.setFloat("prog", progress);
		nbt.setTag("tanks", FFUtils.serializeTankArray(tanks));
		return super.writeToNBT(nbt);
	}

	@Override
	public int[] getAccessibleSlotsFromSide(EnumFacing e) {
		return new int[] {1, 2};
	}

	@Override
	public boolean canInsertItem(int slot, ItemStack stack, int amount) {
		return slot == 1 && PyroOvenRecipes.isInputItem(stack);
	}

	@Override
	public boolean canExtractItem(int slot, ItemStack stack, int amount) {
		return slot == 2;
	}

	private AxisAlignedBB bb = null;

	@Override
	public AxisAlignedBB getRenderBoundingBox() {
		if(bb == null)
			bb = new AxisAlignedBB(pos.getX() - 3, pos.getY(), pos.getZ() - 3, pos.getX() + 4, pos.getY() + 3.5, pos.getZ() + 4);
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
		return new IFluidTankProperties[] {tanks[0].getTankProperties()[0], tanks[1].getTankProperties()[0]};
	}

	@Override
	public int fill(FluidStack resource, boolean doFill) {
		if(resource == null)
			return 0;
		if(tanks[0].getFluidAmount() > 0)
			return resource.isFluidEqual(tanks[0].getFluid()) ? tanks[0].fill(resource, doFill) : 0;
		return PyroOvenRecipes.isInputFluid(resource.getFluid()) ? tanks[0].fill(resource, doFill) : 0;
	}

	@Override
	public FluidStack drain(FluidStack resource, boolean doDrain) {
		if(resource == null || tanks[1].getFluidAmount() <= 0 || !resource.isFluidEqual(tanks[1].getFluid()))
			return null;
		return tanks[1].drain(resource.amount, doDrain);
	}

	@Override
	public FluidStack drain(int maxDrain, boolean doDrain) {
		if(tanks[1].getFluidAmount() <= 0)
			return null;
		return tanks[1].drain(maxDrain, doDrain);
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
