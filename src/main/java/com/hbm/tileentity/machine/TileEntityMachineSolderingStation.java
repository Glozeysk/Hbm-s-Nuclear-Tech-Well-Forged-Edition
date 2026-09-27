package com.hbm.tileentity.machine;

import com.hbm.blocks.BlockDummyable;
import com.hbm.handler.threading.PacketThreading;
import com.hbm.inventory.RecipesCommon.AStack;
import com.hbm.inventory.SolderingRecipes;
import com.hbm.inventory.SolderingRecipes.SolderingRecipe;
import com.hbm.inventory.UpgradeManager;
import com.hbm.items.machine.ItemMachineUpgrade.UpgradeType;
import com.hbm.lib.ForgeDirection;
import com.hbm.lib.Library;
import com.hbm.packet.AuxParticlePacketNT;
import com.hbm.tileentity.TileEntityMachineBase;

import api.hbm.energy.IEnergyUser;
import io.netty.buffer.ByteBuf;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ITickable;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidTank;
import net.minecraftforge.fluids.capability.CapabilityFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.IFluidTankProperties;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.NetworkRegistry.TargetPoint;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

//Ported from NTM:CE (TileEntityMachineSolderingStation); no fluid identifier and no collision prevention toggle.
//Slots: 0-4 inputs, 5 solder, 6 output, 7 battery, 8-9 upgrades.
public class TileEntityMachineSolderingStation extends TileEntityMachineBase implements ITickable, IEnergyUser, IFluidHandler {

	public static final int tankSize = 8_000;

	public long power;
	public long maxPower = 2_000;
	public long consumption;
	public int progress;
	public int processTime = 1;
	public FluidTank tank;
	public ItemStack display = ItemStack.EMPTY;

	private final UpgradeManager upgradeManager = new UpgradeManager();
	private AxisAlignedBB bb = null;

	public TileEntityMachineSolderingStation() {
		super(10);
		tank = new FluidTank(tankSize);
	}

	@Override
	public String getName() {
		return "container.machineSolderingStation";
	}

	@Override
	public void update() {

		if(!world.isRemote) {

			this.power = Library.chargeTEFromItems(inventory, 7, power, maxPower);

			if(world.getTotalWorldTime() % 20 == 0)
				this.updateConnections();

			SolderingRecipe recipe = SolderingRecipes.getRecipe(new ItemStack[] {
					inventory.getStackInSlot(0), inventory.getStackInSlot(1), inventory.getStackInSlot(2), inventory.getStackInSlot(3), inventory.getStackInSlot(4)
			});

			upgradeManager.eval(inventory, 8, 9);
			int speed = Math.min(upgradeManager.getLevel(UpgradeType.SPEED), 3);
			int powerSaving = Math.min(upgradeManager.getLevel(UpgradeType.POWER), 3);
			int overdrive = Math.min(upgradeManager.getLevel(UpgradeType.OVERDRIVE), 3);

			long intendedMaxPower;

			if(recipe != null) {
				this.processTime = Math.max(recipe.duration - (recipe.duration * speed / 6) + (recipe.duration * powerSaving / 3), 1);
				this.consumption = recipe.consumption + (recipe.consumption * speed) - (recipe.consumption * powerSaving / 6);
				this.consumption *= (long) Math.pow(2, overdrive);
				this.display = recipe.output;
				intendedMaxPower = consumption * 20;

				if(canProcess(recipe)) {
					this.progress += 1 + overdrive;
					this.power -= this.consumption;

					if(progress >= processTime) {
						this.progress = 0;
						this.consumeItems(recipe);

						if(inventory.getStackInSlot(6).isEmpty())
							inventory.setStackInSlot(6, recipe.output.copy());
						else
							inventory.getStackInSlot(6).grow(recipe.output.getCount());

						this.markDirty();
					}

					if(world.getTotalWorldTime() % 20 == 0)
						this.spawnSparks();

				} else {
					this.progress = 0;
				}

			} else {
				this.progress = 0;
				this.consumption = 100;
				this.display = ItemStack.EMPTY;
				intendedMaxPower = 2_000;
			}

			//the buffer follows the recipe, but never deletes charge that is already stored
			this.maxPower = Math.max(intendedMaxPower, power);

			this.networkPackNT(25);
		}
	}

	private void spawnSparks() {

		ForgeDirection dir = ForgeDirection.getOrientation(this.getBlockMetadata() - BlockDummyable.offset);
		ForgeDirection rot = dir.getRotation(ForgeDirection.UP);
		NBTTagCompound data = new NBTTagCompound();
		data.setString("type", "tau");
		data.setByte("count", (byte) 3);
		PacketThreading.createAllAroundThreadedPacket(
				new AuxParticlePacketNT(data,
						pos.getX() + 0.5 - dir.offsetX * 0.5 + rot.offsetX * 0.5,
						pos.getY() + 1.125,
						pos.getZ() + 0.5 - dir.offsetZ * 0.5 + rot.offsetZ * 0.5),
				new TargetPoint(world.provider.getDimension(), pos.getX(), pos.getY(), pos.getZ(), 25));
	}

	public boolean canProcess(SolderingRecipe recipe) {

		if(this.power < this.consumption)
			return false;

		ItemStack solder = inventory.getStackInSlot(5);
		if(!SolderingRecipes.isSolder(solder) || solder.getCount() < SolderingRecipes.SOLDER_PER_RECIPE)
			return false;

		if(recipe.fluid != null) {
			FluidStack stored = tank.getFluid();
			if(stored == null || stored.getFluid() != recipe.fluid.getFluid() || stored.amount < recipe.fluid.amount)
				return false;
		}

		ItemStack out = inventory.getStackInSlot(6);

		if(out.isEmpty())
			return true;

		return out.getItem() == recipe.output.getItem() && out.getItemDamage() == recipe.output.getItemDamage()
				&& out.getCount() + recipe.output.getCount() <= out.getMaxStackSize();
	}

	private void consumeItems(SolderingRecipe recipe) {

		for(AStack aStack : recipe.inputs) {
			for(int i = 0; i < 5; i++) {
				ItemStack stack = inventory.getStackInSlot(i);
				if(aStack.matchesRecipe(stack, true) && stack.getCount() >= aStack.count()) {
					stack.shrink(aStack.count());
					break;
				}
			}
		}

		inventory.getStackInSlot(5).shrink(SolderingRecipes.SOLDER_PER_RECIPE);

		if(recipe.fluid != null)
			tank.drain(recipe.fluid.amount, true);
	}

	//the machine is 2x2x1; cables and pipes attach one block out from each side
	private void updateConnections() {

		ForgeDirection dir = ForgeDirection.getOrientation(this.getBlockMetadata() - BlockDummyable.offset);
		ForgeDirection rot = dir.getRotation(ForgeDirection.UP);

		this.trySubscribe(world, pos.add(dir.offsetX, 0, dir.offsetZ), dir);
		this.trySubscribe(world, pos.add(dir.offsetX + rot.offsetX, 0, dir.offsetZ + rot.offsetZ), dir);
		this.trySubscribe(world, pos.add(-dir.offsetX * 2, 0, -dir.offsetZ * 2), dir.getOpposite());
		this.trySubscribe(world, pos.add(-dir.offsetX * 2 + rot.offsetX, 0, -dir.offsetZ * 2 + rot.offsetZ), dir.getOpposite());
		this.trySubscribe(world, pos.add(-rot.offsetX, 0, -rot.offsetZ), rot.getOpposite());
		this.trySubscribe(world, pos.add(-dir.offsetX - rot.offsetX, 0, -dir.offsetZ - rot.offsetZ), rot.getOpposite());
		this.trySubscribe(world, pos.add(rot.offsetX * 2, 0, rot.offsetZ * 2), rot);
		this.trySubscribe(world, pos.add(-dir.offsetX + rot.offsetX * 2, 0, -dir.offsetZ + rot.offsetZ * 2), rot);
	}

	@Override
	public void serialize(ByteBuf buf) {
		super.serialize(buf);
		buf.writeLong(power);
		buf.writeLong(maxPower);
		buf.writeLong(consumption);
		buf.writeInt(progress);
		buf.writeInt(processTime);

		FluidStack stored = tank.getFluid();
		if(stored != null && stored.amount > 0) {
			buf.writeBoolean(true);
			ByteBufUtils.writeUTF8String(buf, stored.getFluid().getName());
			buf.writeInt(stored.amount);
		} else {
			buf.writeBoolean(false);
		}

		if(!display.isEmpty()) {
			buf.writeBoolean(true);
			buf.writeInt(Item.getIdFromItem(display.getItem()));
			buf.writeInt(display.getItemDamage());
		} else {
			buf.writeBoolean(false);
		}
	}

	@Override
	public void deserialize(ByteBuf buf) {
		super.deserialize(buf);
		this.power = buf.readLong();
		this.maxPower = buf.readLong();
		this.consumption = buf.readLong();
		this.progress = buf.readInt();
		this.processTime = buf.readInt();

		if(buf.readBoolean()) {
			Fluid fluid = FluidRegistry.getFluid(ByteBufUtils.readUTF8String(buf));
			int amount = buf.readInt();
			tank.setFluid(fluid != null ? new FluidStack(fluid, amount) : null);
		} else {
			tank.setFluid(null);
		}

		this.display = buf.readBoolean() ? new ItemStack(Item.getItemById(buf.readInt()), 1, buf.readInt()) : ItemStack.EMPTY;
	}

	@Override
	public void readFromNBT(NBTTagCompound nbt) {
		super.readFromNBT(nbt);
		this.power = nbt.getLong("power");
		this.maxPower = Math.max(nbt.getLong("maxPower"), 2_000);
		this.progress = nbt.getInteger("progress");
		this.processTime = nbt.getInteger("processTime");
		if(nbt.hasKey("tank"))
			tank.readFromNBT(nbt.getCompoundTag("tank"));
	}

	@Override
	public NBTTagCompound writeToNBT(NBTTagCompound nbt) {
		nbt.setLong("power", power);
		nbt.setLong("maxPower", maxPower);
		nbt.setInteger("progress", progress);
		nbt.setInteger("processTime", processTime);
		nbt.setTag("tank", tank.writeToNBT(new NBTTagCompound()));
		return super.writeToNBT(nbt);
	}

	public int getProgressScaled(int i) {
		return processTime > 0 ? (progress * i) / processTime : 0;
	}

	@Override
	public long getPower() {
		return Math.max(Math.min(power, maxPower), 0);
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
	public IFluidTankProperties[] getTankProperties() {
		return tank.getTankProperties();
	}

	@Override
	public int fill(FluidStack resource, boolean doFill) {

		//an empty tank takes any fluid a recipe asks for, after that it only tops up what it already holds
		if(resource == null || !SolderingRecipes.isValidFluid(resource.getFluid()))
			return 0;

		return tank.fill(resource, doFill);
	}

	@Override
	public FluidStack drain(FluidStack resource, boolean doDrain) {
		return null;
	}

	@Override
	public FluidStack drain(int maxDrain, boolean doDrain) {
		return null;
	}

	@Override
	public int[] getAccessibleSlotsFromSide(EnumFacing e) {
		return new int[] { 0, 1, 2, 3, 4, 5, 6 };
	}

	@Override
	public boolean isItemValidForSlot(int slot, ItemStack stack) {

		if(slot == 5)
			return SolderingRecipes.isSolder(stack);

		if(slot > 4)
			return false;

		//one kind of item per slot, so the others stay free for the rest of the recipe
		for(int i = 0; i < 5; i++) {
			if(i == slot)
				continue;

			ItemStack other = inventory.getStackInSlot(i);

			if(!other.isEmpty() && other.getItem() == stack.getItem() && other.getItemDamage() == stack.getItemDamage())
				return false;
		}

		return SolderingRecipes.isInput(stack);
	}

	@Override
	public boolean canExtractItem(int i, ItemStack stack, int amount) {
		return i == 6;
	}

	@Override
	public boolean hasCapability(Capability<?> capability, EnumFacing facing) {
		if(capability == CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY)
			return true;
		return super.hasCapability(capability, facing);
	}

	@Override
	public <T> T getCapability(Capability<T> capability, EnumFacing facing) {
		if(capability == CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY)
			return CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY.cast(this);
		return super.getCapability(capability, facing);
	}

	@Override
	public AxisAlignedBB getRenderBoundingBox() {

		if(bb == null)
			bb = new AxisAlignedBB(pos.getX() - 1, pos.getY(), pos.getZ() - 1, pos.getX() + 2, pos.getY() + 3, pos.getZ() + 2);

		return bb;
	}

	@Override
	@SideOnly(Side.CLIENT)
	public double getMaxRenderDistanceSquared() {
		return 65536.0D;
	}
}
