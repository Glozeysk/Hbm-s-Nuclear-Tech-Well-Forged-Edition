package com.hbm.tileentity.machine;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import com.hbm.blocks.BlockDummyable;
import com.hbm.blocks.network.AirDuctNetwork;
import com.hbm.forgefluid.FFUtils;
import com.hbm.forgefluid.ModForgeFluids;
import com.hbm.forgefluid.PressurizedFluids;
import com.hbm.interfaces.IControlReceiver;
import com.hbm.inventory.CompressorRecipes;
import com.hbm.inventory.CompressorRecipes.CompressorRecipe;
import com.hbm.inventory.UpgradeManager;
import com.hbm.items.machine.ItemMachineUpgrade.UpgradeType;
import com.hbm.lib.DirPos;
import com.hbm.lib.ForgeDirection;
import com.hbm.lib.Library;
import com.hbm.tileentity.TileEntityMachineBase;

import api.hbm.energy.IEnergyUser;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ITickable;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.MathHelper;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidTank;
import net.minecraftforge.fluids.capability.CapabilityFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.IFluidTankProperties;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

//Ported from NTM:CE (TileEntityMachineCompressorCompact + TileEntityMachineCompressorBase). No fluid identifier; instead of
//five pressure levels there are three modes: compression sets the pressure flag (or runs a recipe), the two vacuum modes
//pump arc welders standing port to port or linked by air ducts, like the fore pump and the cryo pump do.
//Slots: 0 battery, 1-2 upgrades.
public class TileEntityMachineCompressorCompact extends TileEntityMachineBase implements ITickable, IEnergyUser, IFluidHandler, IControlReceiver {

	public static final int MODE_HIGH_VACUUM = 0;
	public static final int MODE_VACUUM = 1;
	public static final int MODE_COMPRESSION = 2;

	public static final long maxPower = 100_000;
	public static final int processTimeBase = 100;
	public static final int powerRequirementBase = 2_500;
	public static final int nitrogenPerTick = 2;

	public long power;
	public boolean isOn;
	public int progress;
	public int processTime = processTimeBase;
	public int powerRequirement = powerRequirementBase;
	public int mode = MODE_COMPRESSION;
	public FluidTank[] tanks;

	public float fanSpin;
	public float prevFanSpin;

	private final UpgradeManager upgradeManager = new UpgradeManager();
	private AxisAlignedBB bb = null;

	public TileEntityMachineCompressorCompact() {
		super(3);
		tanks = new FluidTank[] { new FluidTank(16_000), new FluidTank(16_000) };
	}

	@Override
	public String getName() {
		return "container.machineCompressor";
	}

	@Override
	public void update() {

		if(!world.isRemote) {

			if(world.getTotalWorldTime() % 20 == 0)
				for(DirPos con : getConPos())
					this.trySubscribe(world, con.getPos(), con.getDir());

			this.power = Library.chargeTEFromItems(inventory, 0, power, maxPower);

			upgradeManager.eval(inventory, 1, 2);
			int speedLevel = Math.min(upgradeManager.getLevel(UpgradeType.SPEED), 3);
			int powerLevel = Math.min(upgradeManager.getLevel(UpgradeType.POWER), 3);
			int overLevel = Math.min(upgradeManager.getLevel(UpgradeType.OVERDRIVE), 3);

			CompressorRecipe recipe = CompressorRecipes.getRecipe(tanks[0].getFluid());

			//CE's timing: plain compression has its own speed steps, recipes are divided by the speed level
			if(recipe == null)
				this.processTime = speedLevel == 3 ? 10 : speedLevel == 2 ? 20 : speedLevel == 1 ? 60 : processTimeBase;
			else
				this.processTime = recipe.duration / (speedLevel + 1);

			this.powerRequirement = powerRequirementBase / (powerLevel + 1);
			this.processTime = Math.max(this.processTime / (overLevel + 1), 1);
			this.powerRequirement = this.powerRequirement * (overLevel * 2 + 1);

			//the vacuum modes pay on top of the upgraded base
			if(mode == MODE_VACUUM)
				this.powerRequirement = this.powerRequirement * 3 / 2;
			else if(mode == MODE_HIGH_VACUUM)
				this.powerRequirement = this.powerRequirement * 2;

			this.isOn = false;

			if(mode == MODE_COMPRESSION) {
				if(canCompress(recipe)) {
					this.progress++;
					this.isOn = true;
					this.power -= powerRequirement;

					if(progress >= processTime) {
						progress = 0;
						this.compress(recipe);
						this.markDirty();
					}
				} else {
					this.progress = 0;
				}
			} else {
				this.progress = 0;
				this.pumpVacuum();
			}

			for(DirPos con : getConPos())
				if(tanks[1].getFluidAmount() > 0)
					FFUtils.fillFluid(this, tanks[1], world, con.getPos(), tanks[1].getFluidAmount());

			this.networkPackNT(100);

		} else {
			this.prevFanSpin = this.fanSpin;

			if(this.isOn) {
				this.fanSpin += 45;

				if(this.fanSpin >= 360) {
					this.prevFanSpin -= 360;
					this.fanSpin -= 360;
				}
			}
		}
	}

	/** what one compression cycle turns the input into, null when the input cannot be compressed */
	private FluidStack getOutput(CompressorRecipe recipe) {
		FluidStack in = tanks[0].getFluid();

		if(in == null)
			return null;

		if(recipe != null)
			return recipe.output.copy();

		//there is only one pressure step, a pressurized fluid without a recipe goes nowhere
		if(PressurizedFluids.isPressurized(in))
			return null;

		return PressurizedFluids.pressurize(new FluidStack(in, CompressorRecipes.DEFAULT_AMOUNT));
	}

	private boolean canCompress(CompressorRecipe recipe) {

		if(this.power < powerRequirement)
			return false;

		FluidStack out = getOutput(recipe);

		if(out == null)
			return false;

		int inputAmount = recipe != null ? recipe.inputAmount : CompressorRecipes.DEFAULT_AMOUNT;

		return tanks[0].getFluidAmount() >= inputAmount && tanks[1].fill(out, false) == out.amount;
	}

	private void compress(CompressorRecipe recipe) {
		FluidStack out = getOutput(recipe);
		tanks[0].drain(recipe != null ? recipe.inputAmount : CompressorRecipes.DEFAULT_AMOUNT, true);
		tanks[1].fill(out, true);
	}

	private void pumpVacuum() {

		boolean deep = mode == MODE_HIGH_VACUUM;

		if(this.power < powerRequirement)
			return;

		if(deep) {
			FluidStack stored = tanks[0].getFluid();
			if(stored == null || stored.getFluid() != ModForgeFluids.nitrogen || PressurizedFluids.isPressurized(stored) || stored.amount < nitrogenPerTick)
				return;
		}

		//the same numbers as the fore pump and the cryo pump
		int rate = deep ? 100 : 50;
		int floor = deep ? 45_000 : 0;
		int ceiling = deep ? 100_000 : 50_000;

		//every welder this compressor can serve right now shares its rate; several compressors on one network simply add up
		List<TileEntityMachineArcWelder> targets = new ArrayList<TileEntityMachineArcWelder>();
		for(TileEntityMachineArcWelder welder : getWelders())
			if(!welder.isInvalid() && welder.usesVacuum() && welder.vacuum >= floor && welder.vacuum < ceiling)
				targets.add(welder);

		if(targets.isEmpty())
			return;

		int share = rate / targets.size();
		int added = 0;

		for(TileEntityMachineArcWelder welder : targets)
			added += welder.addVacuum(share, ceiling);

		//power and nitrogen are only spent while a chamber actually takes the vacuum
		if(added > 0) {
			this.power -= powerRequirement;
			if(deep)
				tanks[0].drain(nitrogenPerTick, true);
			this.isOn = true;
		}
	}

	private Set<TileEntityMachineArcWelder> welderCache = null;

	/** welders standing port to port or at the end of connected air ducts; the walk is redone once a second */
	public Set<TileEntityMachineArcWelder> getWelders() {
		if(welderCache == null || world.getTotalWorldTime() % 20 == 0)
			welderCache = AirDuctNetwork.findWelders(this);
		return welderCache;
	}

	//must match MachineCompressorCompact.fillSpace
	public DirPos[] getConPos() {

		ForgeDirection dir = ForgeDirection.getOrientation(this.getBlockMetadata() - BlockDummyable.offset);
		ForgeDirection rot = dir.getRotation(ForgeDirection.UP);
		int x = pos.getX(), y = pos.getY(), z = pos.getZ();

		return new DirPos[] {
				new DirPos(x + rot.offsetX * 4, y + 1, z + rot.offsetZ * 4, rot),
				new DirPos(x - rot.offsetX * 4, y + 1, z - rot.offsetZ * 4, rot.getOpposite()),
				new DirPos(x + dir.offsetX * 2 - rot.offsetX, y + 1, z + dir.offsetZ * 2 - rot.offsetZ, dir),
				new DirPos(x + dir.offsetX * 2 + rot.offsetX, y + 1, z + dir.offsetZ * 2 + rot.offsetZ, dir),
				new DirPos(x - dir.offsetX * 2 - rot.offsetX, y + 1, z - dir.offsetZ * 2 - rot.offsetZ, dir.getOpposite()),
				new DirPos(x - dir.offsetX * 2 + rot.offsetX, y + 1, z - dir.offsetZ * 2 + rot.offsetZ, dir.getOpposite())
		};
	}

	@Override
	public boolean hasPermission(EntityPlayer player) {
		return this.isUseableByPlayer(player);
	}

	@Override
	public void receiveControl(NBTTagCompound data) {
		if(data.hasKey("mode")) {
			int next = MathHelper.clamp(data.getInteger("mode"), MODE_HIGH_VACUUM, MODE_COMPRESSION);
			if(next != mode) {
				this.mode = next;
				this.progress = 0;
				this.markDirty();
			}
		}
	}

	@Override
	public void serialize(ByteBuf buf) {
		super.serialize(buf);
		buf.writeInt(progress);
		buf.writeInt(processTime);
		buf.writeInt(powerRequirement);
		buf.writeLong(power);
		buf.writeBoolean(isOn);
		buf.writeByte(mode);
		//written as NBT so the pressure flag reaches the client
		ByteBufUtils.writeTag(buf, tanks[0].writeToNBT(new NBTTagCompound()));
		ByteBufUtils.writeTag(buf, tanks[1].writeToNBT(new NBTTagCompound()));
	}

	@Override
	public void deserialize(ByteBuf buf) {
		super.deserialize(buf);
		this.progress = buf.readInt();
		this.processTime = buf.readInt();
		this.powerRequirement = buf.readInt();
		this.power = buf.readLong();
		this.isOn = buf.readBoolean();
		this.mode = buf.readByte();
		readTank(tanks[0], ByteBufUtils.readTag(buf));
		readTank(tanks[1], ByteBufUtils.readTag(buf));
	}

	private static void readTank(FluidTank tank, NBTTagCompound tag) {
		tank.setFluid(null);
		if(tag != null)
			tank.readFromNBT(tag);
	}

	@Override
	public void readFromNBT(NBTTagCompound nbt) {
		super.readFromNBT(nbt);
		this.power = nbt.getLong("power");
		this.progress = nbt.getInteger("progress");
		this.mode = nbt.hasKey("mode") ? MathHelper.clamp(nbt.getInteger("mode"), MODE_HIGH_VACUUM, MODE_COMPRESSION) : MODE_COMPRESSION;
		readTank(tanks[0], nbt.getCompoundTag("tank0"));
		readTank(tanks[1], nbt.getCompoundTag("tank1"));
	}

	@Override
	public NBTTagCompound writeToNBT(NBTTagCompound nbt) {
		nbt.setLong("power", power);
		nbt.setInteger("progress", progress);
		nbt.setInteger("mode", mode);
		nbt.setTag("tank0", tanks[0].writeToNBT(new NBTTagCompound()));
		nbt.setTag("tank1", tanks[1].writeToNBT(new NBTTagCompound()));
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
		return new IFluidTankProperties[] { tanks[0].getTankProperties()[0], tanks[1].getTankProperties()[0] };
	}

	@Override
	public int fill(FluidStack resource, boolean doFill) {
		//the tank itself keeps a pressurized fluid and its plain form apart
		return resource == null ? 0 : tanks[0].fill(resource, doFill);
	}

	@Override
	public FluidStack drain(FluidStack resource, boolean doDrain) {
		if(resource == null || !resource.isFluidEqual(tanks[1].getFluid()))
			return null;
		return tanks[1].drain(resource.amount, doDrain);
	}

	@Override
	public FluidStack drain(int maxDrain, boolean doDrain) {
		return tanks[1].drain(maxDrain, doDrain);
	}

	@Override
	public int[] getAccessibleSlotsFromSide(EnumFacing e) {
		return new int[] {};
	}

	@Override
	public boolean isItemValidForSlot(int slot, ItemStack stack) {
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
			bb = new AxisAlignedBB(pos.getX() - 3, pos.getY(), pos.getZ() - 3, pos.getX() + 4, pos.getY() + 3, pos.getZ() + 4);

		return bb;
	}

	@Override
	@SideOnly(Side.CLIENT)
	public double getMaxRenderDistanceSquared() {
		return 65536.0D;
	}
}
