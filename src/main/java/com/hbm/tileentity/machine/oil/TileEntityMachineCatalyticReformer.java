package com.hbm.tileentity.machine.oil;

import com.hbm.blocks.BlockDummyable;
import com.hbm.forgefluid.FFUtils;
import com.hbm.interfaces.ITankPacketAcceptor;
import com.hbm.inventory.CatalyticReformerRecipes;
import com.hbm.items.ModItems;
import com.hbm.lib.ForgeDirection;
import com.hbm.lib.Library;
import com.hbm.packet.AuxElectricityPacket;
import com.hbm.packet.FluidTankPacket;
import com.hbm.packet.PacketDispatcher;
import com.hbm.tileentity.TileEntityMachineBase;
import com.hbm.util.Tuple.Quartet;

import api.hbm.energy.IEnergyUser;
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
import net.minecraftforge.fml.common.network.NetworkRegistry.TargetPoint;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

//Ported from NTM:CE, rewritten on this fork's Forge FluidTank / IEnergyUser primitives like the hydrotreater.
//Slots: 0 battery, 1-2 feedstock container in/out, 3-4 / 5-6 / 7-8 output tank 1/2/3 containers in/out, 9 catalyst.
public class TileEntityMachineCatalyticReformer extends TileEntityMachineBase implements ITickable, IEnergyUser, IFluidHandler, ITankPacketAcceptor {

	public static final long maxPower = 1_000_000;
	public static final int powerPerOperation = 20_000;
	public long power;

	//0 = feedstock (type picked from the first valid fluid), 1-3 = outputs (types follow the recipe)
	public FluidTank[] tanks;
	public Fluid[] tankTypes;

	public TileEntityMachineCatalyticReformer() {
		super(10);
		tanks = new FluidTank[4];
		tankTypes = new Fluid[4];

		tanks[0] = new FluidTank(64000);
		tanks[1] = new FluidTank(24000);
		tanks[2] = new FluidTank(24000);
		tanks[3] = new FluidTank(24000);
	}

	public String getName() {
		return "container.catalyticReformer";
	}

	@Override
	public void update() {
		if(!world.isRemote) {

			this.updateConnections();
			power = Library.chargeTEFromItems(inventory, 0, power, maxPower);

			if(this.inputValidForTank(1))
				FFUtils.fillFromFluidContainer(inventory, tanks[0], 1, 2);

			reform();

			FFUtils.fillFluidContainer(inventory, tanks[1], 3, 4);
			FFUtils.fillFluidContainer(inventory, tanks[2], 5, 6);
			FFUtils.fillFluidContainer(inventory, tanks[3], 7, 8);

			detectAndSendChanges();
		}
	}

	private void reform() {
		Quartet<FluidStack, FluidStack, FluidStack, FluidStack> recipe = CatalyticReformerRecipes.getRecipe(tankTypes[0]);

		if(recipe == null) {
			for(int i = 1; i < 4; i++)
				setTankType(i, null);
			return;
		}

		FluidStack[] outputs = new FluidStack[] { recipe.getX(), recipe.getY(), recipe.getZ() };

		for(int i = 0; i < 3; i++)
			setTankType(i + 1, outputs[i].getFluid());

		// don't run onto a leftover product of another recipe, it would be converted in place
		for(int i = 0; i < 3; i++)
			if(outputBlocked(i + 1, outputs[i]))
				return;

		if(power < powerPerOperation)
			return;
		if(tanks[0].getFluidAmount() < recipe.getW().amount)
			return;
		if(inventory.getStackInSlot(9).isEmpty() || inventory.getStackInSlot(9).getItem() != ModItems.catalyst_cobalt)
			return;
		for(int i = 0; i < 3; i++)
			if(tanks[i + 1].getFluidAmount() + outputs[i].amount > tanks[i + 1].getCapacity())
				return;

		tanks[0].drain(recipe.getW().amount, true);
		for(int i = 0; i < 3; i++)
			tanks[i + 1].fill(outputs[i].copy(), true);
		power -= powerPerOperation;
	}

	private boolean outputBlocked(int idx, FluidStack out) {
		return tanks[idx].getFluidAmount() > 0 && tanks[idx].getFluid().getFluid() != out.getFluid();
	}

	//Active HE subscription on the cable spots next to the 6 ports (see MachineCatalyticReformer.fillSpace),
	//passive capability lookups don't reach the HE net through TileEntityProxyCombo.
	private void updateConnections() {
		ForgeDirection dir = ForgeDirection.getOrientation(this.getBlockMetadata() - BlockDummyable.offset);
		ForgeDirection rot = dir.getRotation(ForgeDirection.UP);

		subscribe(dir.offsetX * 2 + rot.offsetX, dir.offsetZ * 2 + rot.offsetZ, dir);
		subscribe(dir.offsetX * 2 - rot.offsetX, dir.offsetZ * 2 - rot.offsetZ, dir);
		subscribe(-dir.offsetX * 2 + rot.offsetX, -dir.offsetZ * 2 + rot.offsetZ, dir.getOpposite());
		subscribe(-dir.offsetX * 2 - rot.offsetX, -dir.offsetZ * 2 - rot.offsetZ, dir.getOpposite());
		subscribe(rot.offsetX * 3, rot.offsetZ * 3, rot);
		subscribe(-rot.offsetX * 3, -rot.offsetZ * 3, rot.getOpposite());
	}

	private void subscribe(int dx, int dz, ForgeDirection dir) {
		this.trySubscribe(world, new BlockPos(pos.getX() + dx, pos.getY(), pos.getZ() + dz), dir);
	}

	private boolean inputValidForTank(int slot) {
		if(!inventory.getStackInSlot(slot).isEmpty()) {
			FluidStack containerFluid = FluidUtil.getFluidContained(inventory.getStackInSlot(slot));
			if(containerFluid != null && CatalyticReformerRecipes.getRecipe(containerFluid.getFluid()) != null) {
				if(tanks[0].getFluidAmount() > 0 && tanks[0].getFluid().getFluid() != containerFluid.getFluid())
					return false;
				tankTypes[0] = containerFluid.getFluid();
				return true;
			}
		}
		return false;
	}

	public void setTankType(int idx, Fluid type) {
		if(tanks[idx].getFluidAmount() > 0)
			return;
		if(tankTypes[idx] != type) {
			tankTypes[idx] = type;
			tanks[idx].setFluid(type != null ? new FluidStack(type, 0) : null);
		}
	}

	private long detectPower;
	private FluidTank[] detectTanks = new FluidTank[] {null, null, null, null};
	private int syncTick = 0;

	private void detectAndSendChanges() {
		boolean mark = false;
		if(detectPower != power) {
			mark = true;
			detectPower = power;
		}
		for(int i = 0; i < tanks.length; i++) {
			if(!FFUtils.areTanksEqual(tanks[i], detectTanks[i])) {
				mark = true;
				detectTanks[i] = FFUtils.copyTank(tanks[i]);
			}
		}
		syncTick++;
		boolean periodic = syncTick >= 5;
		if(periodic) {
			syncTick = 0;
			PacketDispatcher.wrapper.sendToAllAround(new FluidTankPacket(pos.getX(), pos.getY(), pos.getZ(), tanks), new TargetPoint(world.provider.getDimension(), pos.getX(), pos.getY(), pos.getZ(), 20));
		}
		//power also rides the periodic sync: sent only on change, a client that just (re)loaded the chunk kept an empty bar
		if(mark || periodic)
			PacketDispatcher.wrapper.sendToAllAround(new AuxElectricityPacket(pos.getX(), pos.getY(), pos.getZ(), power), new TargetPoint(world.provider.getDimension(), pos.getX(), pos.getY(), pos.getZ(), 20));
		if(mark)
			markDirty();
	}

	@Override
	public void recievePacket(NBTTagCompound[] tags) {
		if(tags.length != 4)
			return;
		for(int i = 0; i < tanks.length; i++)
			tanks[i].readFromNBT(tags[i]);
	}

	@Override
	public void readFromNBT(NBTTagCompound nbt) {
		if(nbt.hasKey("f0"))
			tankTypes[0] = FluidRegistry.getFluid(nbt.getString("f0"));
		power = nbt.getLong("power");
		if(nbt.hasKey("tanks"))
			FFUtils.deserializeTankArray(nbt.getTagList("tanks", 10), tanks);
		super.readFromNBT(nbt);
	}

	@Override
	public NBTTagCompound writeToNBT(NBTTagCompound nbt) {
		if(tankTypes[0] != null)
			nbt.setString("f0", tankTypes[0].getName());
		nbt.setLong("power", power);
		nbt.setTag("tanks", FFUtils.serializeTankArray(tanks));
		return super.writeToNBT(nbt);
	}

	public long getPowerScaled(long i) {
		return (power * i) / maxPower;
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
	public int[] getAccessibleSlotsFromSide(EnumFacing e) {
		return new int[] {0, 1, 2, 3, 4, 5, 6, 7, 8, 9};
	}

	@Override
	public boolean canExtractItem(int i, ItemStack stack, int amount) {
		return i == 2 || i == 4 || i == 6 || i == 8;
	}

	private AxisAlignedBB bb = null;

	@Override
	public AxisAlignedBB getRenderBoundingBox() {
		if(bb == null)
			bb = new AxisAlignedBB(pos.getX() - 2, pos.getY(), pos.getZ() - 2, pos.getX() + 3, pos.getY() + 7, pos.getZ() + 3);
		return bb;
	}

	@Override
	@SideOnly(Side.CLIENT)
	public double getMaxRenderDistanceSquared() {
		return 65536.0D;
	}

	@Override
	public IFluidTankProperties[] getTankProperties() {
		return new IFluidTankProperties[] {tanks[0].getTankProperties()[0], tanks[1].getTankProperties()[0], tanks[2].getTankProperties()[0], tanks[3].getTankProperties()[0]};
	}

	@Override
	public int fill(FluidStack resource, boolean doFill) {
		if(resource == null)
			return 0;
		if(tankTypes[0] != null && resource.getFluid() == tankTypes[0])
			return tanks[0].fill(resource, doFill);
		if(tanks[0].getFluidAmount() == 0 && CatalyticReformerRecipes.getRecipe(resource.getFluid()) != null) {
			//a simulated fill must not switch the feedstock type
			if(doFill) {
				tankTypes[0] = resource.getFluid();
				this.markDirty();
			}
			return tanks[0].fill(resource, doFill);
		}
		return 0;
	}

	@Override
	public FluidStack drain(FluidStack resource, boolean doDrain) {
		if(resource == null)
			return null;
		for(int i = 1; i < 4; i++)
			if(resource.isFluidEqual(tanks[i].getFluid()))
				return tanks[i].drain(resource.amount, doDrain);
		return null;
	}

	@Override
	public FluidStack drain(int maxDrain, boolean doDrain) {
		//check the amount, not the stack: a typed but empty output tank holds a 0 mB stack
		for(int i = 1; i < 4; i++)
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
