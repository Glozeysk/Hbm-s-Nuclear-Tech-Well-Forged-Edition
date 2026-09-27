package com.hbm.tileentity.machine.oil;

import com.hbm.forgefluid.FFUtils;
import com.hbm.inventory.LiquefactionRecipes;
import com.hbm.inventory.UpgradeManager;
import com.hbm.items.machine.ItemMachineUpgrade.UpgradeType;
import com.hbm.lib.ForgeDirection;
import com.hbm.lib.Library;
import com.hbm.tileentity.TileEntityMachineBase;

import api.hbm.energy.IEnergyUser;
import io.netty.buffer.ByteBuf;
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
import net.minecraftforge.fluids.capability.CapabilityFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.IFluidTankProperties;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

//Ported from NTM:CE (TileEntityMachineLiquefactor): one item in, fluid out to every port.
//Slots: 0 input, 1 battery, 2-3 upgrades.
public class TileEntityMachineLiquefactor extends TileEntityMachineBase implements ITickable, IEnergyUser, IFluidHandler {

	public static final long maxPower = 100_000;
	public static final int usageBase = 250;
	public static final int processTimeBase = 60;
	public static final int tankSize = 24_000;

	public long power;
	public int usage;
	public int progress;
	public int processTime;
	public FluidTank tank;

	private final UpgradeManager upgradeManager = new UpgradeManager();
	private AxisAlignedBB bb = null;

	public TileEntityMachineLiquefactor() {
		super(4);
		tank = new FluidTank(tankSize);
	}

	@Override
	public String getName() {
		return "container.machineLiquefactor";
	}

	@Override
	public void update() {

		if(!world.isRemote) {
			this.power = Library.chargeTEFromItems(inventory, 1, power, maxPower);

			if(world.getTotalWorldTime() % 20 == 0)
				this.updateConnections();

			upgradeManager.eval(inventory, 2, 3);
			int speed = Math.min(upgradeManager.getLevel(UpgradeType.SPEED), 3);
			int powerSaving = Math.min(upgradeManager.getLevel(UpgradeType.POWER), 3);

			this.processTime = processTimeBase - (processTimeBase / 4) * speed;
			this.usage = (usageBase + (usageBase * speed)) / (powerSaving + 1);

			if(this.canProcess())
				this.process();
			else
				this.progress = 0;

			this.sendFluid();
			this.networkPackNT(50);
		}
	}

	//ports on top, bottom and the four sides of the second layer, the same spots as in CE; must match MachineLiquefactor.fillSpace
	private BlockPos[] getConPos() {
		return new BlockPos[] {
				pos.add(0, 4, 0), pos.add(0, -1, 0),
				pos.add(2, 1, 0), pos.add(-2, 1, 0),
				pos.add(0, 1, 2), pos.add(0, 1, -2)
		};
	}

	private static final ForgeDirection[] CON_DIRS = { ForgeDirection.UP, ForgeDirection.DOWN, ForgeDirection.EAST, ForgeDirection.WEST, ForgeDirection.SOUTH, ForgeDirection.NORTH };

	private void updateConnections() {
		BlockPos[] con = getConPos();
		for(int i = 0; i < con.length; i++)
			this.trySubscribe(world, con[i], CON_DIRS[i]);
	}

	private void sendFluid() {
		for(BlockPos con : getConPos()) {
			if(tank.getFluidAmount() > 0)
				FFUtils.fillFluid(this, tank, world, con, tank.getFluidAmount());
		}
	}

	public boolean canProcess() {

		if(this.power < usage)
			return false;

		FluidStack out = LiquefactionRecipes.getOutput(inventory.getStackInSlot(0));

		if(out == null)
			return false;

		FluidStack stored = tank.getFluid();

		if(stored != null && stored.amount > 0 && stored.getFluid() != out.getFluid())
			return false;

		return out.amount + tank.getFluidAmount() <= tank.getCapacity();
	}

	public void process() {

		this.power -= usage;
		progress++;

		if(progress >= processTime) {
			FluidStack out = LiquefactionRecipes.getOutput(inventory.getStackInSlot(0));
			tank.fill(out.copy(), true);
			inventory.getStackInSlot(0).shrink(1);
			progress = 0;
			this.markDirty();
		}
	}

	@Override
	public void serialize(ByteBuf buf) {
		super.serialize(buf);
		buf.writeLong(power);
		buf.writeInt(progress);
		buf.writeInt(usage);
		buf.writeInt(processTime);

		FluidStack stored = tank.getFluid();
		if(stored != null && stored.amount > 0) {
			buf.writeBoolean(true);
			ByteBufUtils.writeUTF8String(buf, stored.getFluid().getName());
			buf.writeInt(stored.amount);
		} else {
			buf.writeBoolean(false);
		}
	}

	@Override
	public void deserialize(ByteBuf buf) {
		super.deserialize(buf);
		this.power = buf.readLong();
		this.progress = buf.readInt();
		this.usage = buf.readInt();
		this.processTime = buf.readInt();

		if(buf.readBoolean()) {
			Fluid fluid = FluidRegistry.getFluid(ByteBufUtils.readUTF8String(buf));
			int amount = buf.readInt();
			tank.setFluid(fluid != null ? new FluidStack(fluid, amount) : null);
		} else {
			tank.setFluid(null);
		}
	}

	@Override
	public void readFromNBT(NBTTagCompound nbt) {
		super.readFromNBT(nbt);
		this.power = nbt.getLong("power");
		this.progress = nbt.getInteger("progress");
		if(nbt.hasKey("tank"))
			tank.readFromNBT(nbt.getCompoundTag("tank"));
	}

	@Override
	public NBTTagCompound writeToNBT(NBTTagCompound nbt) {
		nbt.setLong("power", power);
		nbt.setInteger("progress", progress);
		nbt.setTag("tank", tank.writeToNBT(new NBTTagCompound()));
		return super.writeToNBT(nbt);
	}

	public int getProgressScaled(int i) {
		return processTime > 0 ? (progress * i) / processTime : 0;
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
	public IFluidTankProperties[] getTankProperties() {
		return tank.getTankProperties();
	}

	@Override
	public int fill(FluidStack resource, boolean doFill) {
		return 0;
	}

	@Override
	public FluidStack drain(FluidStack resource, boolean doDrain) {
		if(resource == null || tank.getFluid() == null || !resource.isFluidEqual(tank.getFluid()))
			return null;
		return tank.drain(resource.amount, doDrain);
	}

	@Override
	public FluidStack drain(int maxDrain, boolean doDrain) {
		return tank.drain(maxDrain, doDrain);
	}

	@Override
	public int[] getAccessibleSlotsFromSide(EnumFacing e) {
		return new int[] { 0 };
	}

	@Override
	public boolean isItemValidForSlot(int slot, ItemStack stack) {
		return slot == 0 && LiquefactionRecipes.getOutput(stack) != null;
	}

	@Override
	public boolean canExtractItem(int slot, ItemStack stack, int amount) {
		return false;
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
			bb = new AxisAlignedBB(pos.getX() - 1, pos.getY(), pos.getZ() - 1, pos.getX() + 2, pos.getY() + 4, pos.getZ() + 2);

		return bb;
	}

	@Override
	@SideOnly(Side.CLIENT)
	public double getMaxRenderDistanceSquared() {
		return 65536.0D;
	}
}
