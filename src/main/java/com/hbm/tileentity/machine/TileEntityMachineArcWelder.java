package com.hbm.tileentity.machine;

import com.hbm.blocks.BlockDummyable;
import com.hbm.blocks.ModBlocks;
import com.hbm.forgefluid.ModForgeFluids;
import com.hbm.handler.threading.PacketThreading;
import com.hbm.interfaces.IControlReceiver;
import com.hbm.inventory.ArcWelderRecipes;
import com.hbm.inventory.ArcWelderRecipes.ArcWelderRecipe;
import com.hbm.inventory.ArcWelderRecipes.WeldType;
import com.hbm.inventory.RecipesCommon.AStack;
import com.hbm.inventory.UpgradeManager;
import com.hbm.items.machine.ItemMachineUpgrade.UpgradeType;
import com.hbm.lib.ForgeDirection;
import com.hbm.lib.Library;
import com.hbm.main.MainRegistry;
import com.hbm.packet.AuxParticlePacketNT;
import com.hbm.tileentity.TileEntityMachineBase;

import api.hbm.energy.IEnergyUser;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.Item;
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
import net.minecraftforge.fml.common.network.NetworkRegistry.TargetPoint;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

//Ported from NTM:CE (TileEntityMachineArcWelder); the CE fluid identifier slot is dropped, the four processes are ours.
//Slots: 0-2 inputs, 3 output, 4 battery, 5-6 upgrades, 7 DNT transformer (EBW only).
public class TileEntityMachineArcWelder extends TileEntityMachineBase implements ITickable, IEnergyUser, IFluidHandler, IControlReceiver {

	public static final int MODE_MMA = 0;
	public static final int MODE_TIG = 1;
	public static final int MODE_VAW = 2;
	public static final int MODE_EBW = 3;

	public static final int tankSize = 2_000;
	/** below this the tank cannot shield an arc, so TIG waits for a refill */
	public static final int argonRequired = 1_800;

	//vacuum is kept in thousandths of a percent so the 1%/s draw stays exact; it only ever comes from pumps
	public static final int vacuumMax = 100_000;
	public static final int vacuumCost = 50;
	public static final int vacuumRequired = 90_000;

	/** [recipe type][mode] = speed bonus in percent, -1 where that process cannot run the recipe */
	private static final int[][] MODE_BONUS = {
			{  0, 25, 50, -1 },
			{ -1,  0, 25, -1 },
			{ -1, -1,  0, 25 },
			{ -1, -1, -1,  0 }
	};

	public long power;
	public long consumption;
	public int progress;
	public int processTime = 1;
	public FluidTank tank;
	public int mode = MODE_MMA;
	public int vacuum;
	public ItemStack display = ItemStack.EMPTY;

	private int argonRemainder;
	private final UpgradeManager upgradeManager = new UpgradeManager();
	private AxisAlignedBB bb = null;

	public TileEntityMachineArcWelder() {
		super(8);
		tank = new FluidTank(tankSize);
	}

	@Override
	public String getName() {
		return "container.machineArcWelder";
	}

	@Override
	public void update() {

		if(!world.isRemote) {

			this.power = Library.chargeTEFromItems(inventory, 4, power, getMaxPower());
			this.updateConnections();

			ArcWelderRecipe recipe = ArcWelderRecipes.getRecipe(inventory.getStackInSlot(0), inventory.getStackInSlot(1), inventory.getStackInSlot(2));

			upgradeManager.eval(inventory, 5, 6);
			int speed = Math.min(upgradeManager.getLevel(UpgradeType.SPEED), 3);
			int powerSaving = Math.min(upgradeManager.getLevel(UpgradeType.POWER), 3);
			int overdrive = Math.min(upgradeManager.getLevel(UpgradeType.OVERDRIVE), 3);

			if(recipe != null) {
				int upgradeTime = Math.max(recipe.duration - (recipe.duration * speed / 6) + (recipe.duration * powerSaving / 3), 1);
				this.consumption = recipe.consumption + (recipe.consumption * speed) - (recipe.consumption * powerSaving / 6);
				this.consumption *= (long) Math.pow(2, overdrive);
				this.display = recipe.output;

				int step = 1 + overdrive;
				//consumables follow the upgrades only, the bonus for welding in a better process is free
				int speedFactor = (int) (1000L * recipe.duration * step / upgradeTime);
				int bonus = getBonus(recipe.type);
				this.processTime = Math.max(upgradeTime * 100 / (100 + Math.max(bonus, 0)), 1);

				if(usesVacuum() && vacuum < vacuumRequired) {
					//the chamber lost its vacuum, the weld is ruined
					this.progress = 0;
				} else if(canProcess(recipe)) {
					this.progress += step;
					this.power -= this.consumption;
					this.drawConsumables(speedFactor);

					if(progress >= processTime) {
						this.progress = 0;
						this.consumeItems(recipe);

						if(inventory.getStackInSlot(3).isEmpty())
							inventory.setStackInSlot(3, recipe.output.copy());
						else
							inventory.getStackInSlot(3).grow(recipe.output.getCount());

						this.markDirty();
					}

					this.spawnArc();
				}
				//a recipe this process cannot weld just keeps its progress until the machine is set up for it

			} else {
				this.progress = 0;
				this.consumption = 100;
				this.display = ItemStack.EMPTY;
			}

			this.networkPackNT(25);
		}
	}

	public boolean usesVacuum() {
		return mode >= MODE_VAW;
	}

	/** pumps call this; returns what was actually taken so several of them can share the work */
	public int addVacuum(int amount, int ceiling) {

		if(!usesVacuum() || vacuum >= ceiling)
			return 0;

		int added = Math.min(amount, Math.min(ceiling, vacuumMax) - vacuum);
		vacuum += added;
		return added;
	}

	/** speed bonus for running this recipe in the currently selected process, -1 when it cannot run at all */
	private int getBonus(WeldType type) {

		int bonus = MODE_BONUS[type.ordinal()][mode];

		//the beam needs its emitter no matter what is being welded
		if(bonus >= 0 && mode == MODE_EBW && !hasTransformer())
			return -1;

		return bonus;
	}

	/** the electron gun draws its beam current from a DNT transformer in the special slot */
	public static boolean isBeamTransformer(ItemStack stack) {
		return !stack.isEmpty() && stack.getItem() == Item.getItemFromBlock(ModBlocks.machine_transformer_dnt);
	}

	private boolean hasTransformer() {
		return isBeamTransformer(inventory.getStackInSlot(7));
	}

	/** the running process eats its shielding gas or its vacuum, whatever the recipe itself is */
	private void drawConsumables(int speedFactor) {

		if(usesVacuum()) {
			vacuum = Math.max(vacuum - vacuumCost * speedFactor / 1000, 0);
			return;
		}

		if(mode == MODE_TIG) {
			argonRemainder += speedFactor;
			int drain = argonRemainder / 1000;
			argonRemainder -= drain * 1000;
			if(drain > 0)
				tank.drain(drain, true);
		}
	}

	private void spawnArc() {

		if(world.getTotalWorldTime() % 2 != 0)
			return;

		ForgeDirection dir = ForgeDirection.getOrientation(this.getBlockMetadata() - BlockDummyable.offset);
		NBTTagCompound data = new NBTTagCompound();
		data.setString("type", world.getTotalWorldTime() % 20 == 0 ? "tau" : "hadron");
		data.setByte("count", (byte) 5);
		PacketThreading.createAllAroundThreadedPacket(
				new AuxParticlePacketNT(data, pos.getX() + 0.5 - dir.offsetX * 0.5, pos.getY() + 1.25, pos.getZ() + 0.5 - dir.offsetZ * 0.5),
				new TargetPoint(world.provider.getDimension(), pos.getX(), pos.getY(), pos.getZ(), 25));
	}

	public boolean canProcess(ArcWelderRecipe recipe) {

		if(this.power < this.consumption)
			return false;

		if(getBonus(recipe.type) < 0)
			return false;

		if(mode == MODE_TIG && tank.getFluidAmount() < argonRequired)
			return false;

		ItemStack out = inventory.getStackInSlot(3);

		if(out.isEmpty())
			return true;

		return out.getItem() == recipe.output.getItem() && out.getItemDamage() == recipe.output.getItemDamage()
				&& out.getCount() + recipe.output.getCount() <= out.getMaxStackSize();
	}

	private void consumeItems(ArcWelderRecipe recipe) {

		for(AStack aStack : recipe.inputs) {
			for(int i = 0; i < 3; i++) {
				ItemStack stack = inventory.getStackInSlot(i);
				if(aStack.matchesRecipe(stack, true) && stack.getCount() >= aStack.count()) {
					stack.shrink(aStack.count());
					break;
				}
			}
		}
	}

	//the machine is 3x2x2, the ports sit on the bottom layer only; must match MachineArcWelder.fillSpace
	private void updateConnections() {

		ForgeDirection dir = ForgeDirection.getOrientation(this.getBlockMetadata() - BlockDummyable.offset);
		ForgeDirection rot = dir.getRotation(ForgeDirection.UP);

		//footprint is core + a*rot + b*dir with a in -1..1 and b in -1..0, so the cables sit one block further out
		for(int a = -1; a <= 1; a++) {
			this.trySubscribe(world, pos.add(rot.offsetX * a + dir.offsetX, 0, rot.offsetZ * a + dir.offsetZ), dir);
			this.trySubscribe(world, pos.add(rot.offsetX * a - dir.offsetX * 2, 0, rot.offsetZ * a - dir.offsetZ * 2), dir.getOpposite());
		}

		for(int b = -1; b <= 0; b++) {
			this.trySubscribe(world, pos.add(rot.offsetX * 2 + dir.offsetX * b, 0, rot.offsetZ * 2 + dir.offsetZ * b), rot);
			this.trySubscribe(world, pos.add(-rot.offsetX * 2 + dir.offsetX * b, 0, -rot.offsetZ * 2 + dir.offsetZ * b), rot.getOpposite());
		}
	}

	@Override
	public boolean hasPermission(EntityPlayer player) {
		return this.isUseableByPlayer(player);
	}

	@Override
	public void receiveControl(NBTTagCompound data) { }

	@Override
	public void receiveControl(EntityPlayerMP player, NBTTagCompound data) {

		if(data.hasKey("mode")) {
			int next = MathHelper.clamp(data.getInteger("mode"), MODE_MMA, MODE_EBW);

			if(next != mode) {
				//the chamber has to be set up again: gas is always vented, the vacuum only survives between VAW and EBW
				if(!(usesVacuum() && next >= MODE_VAW))
					vacuum = 0;

				tank.setFluid(null);
				argonRemainder = 0;
				progress = 0;
				mode = next;
				power = Math.min(power, getMaxPower());
				this.markDirty();
			}
		}

		//the slot layout differs per process, so the container has to be rebuilt
		player.openGui(MainRegistry.instance, ModBlocks.guiID_machine_arc_welder, world, pos.getX(), pos.getY(), pos.getZ());
	}

	@Override
	public void serialize(ByteBuf buf) {
		super.serialize(buf);
		buf.writeLong(power);
		buf.writeLong(consumption);
		buf.writeInt(progress);
		buf.writeInt(processTime);
		buf.writeInt(tank.getFluidAmount());
		buf.writeByte(mode);
		buf.writeInt(vacuum);

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
		this.consumption = buf.readLong();
		this.progress = buf.readInt();
		this.processTime = buf.readInt();
		int amount = buf.readInt();
		tank.setFluid(amount > 0 ? new FluidStack(ModForgeFluids.argon, amount) : null);
		this.mode = buf.readByte();
		this.vacuum = buf.readInt();

		this.display = buf.readBoolean() ? new ItemStack(Item.getItemById(buf.readInt()), 1, buf.readInt()) : ItemStack.EMPTY;
	}

	@Override
	public void readFromNBT(NBTTagCompound nbt) {
		super.readFromNBT(nbt);
		this.power = nbt.getLong("power");
		this.progress = nbt.getInteger("progress");
		this.mode = MathHelper.clamp(nbt.getInteger("mode"), MODE_MMA, MODE_EBW);
		this.vacuum = nbt.getInteger("vacuum");
		if(nbt.hasKey("tank"))
			tank.readFromNBT(nbt.getCompoundTag("tank"));
	}

	@Override
	public NBTTagCompound writeToNBT(NBTTagCompound nbt) {
		nbt.setLong("power", power);
		nbt.setInteger("progress", progress);
		nbt.setInteger("mode", mode);
		nbt.setInteger("vacuum", vacuum);
		nbt.setTag("tank", tank.writeToNBT(new NBTTagCompound()));
		return super.writeToNBT(nbt);
	}

	public int getProgressScaled(int i) {
		return processTime > 0 ? (progress * i) / processTime : 0;
	}

	public int getVacuumScaled(int i) {
		return (vacuum * i) / vacuumMax;
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
		return WeldType.values()[MathHelper.clamp(mode, MODE_MMA, MODE_EBW)].maxPower;
	}

	@Override
	public IFluidTankProperties[] getTankProperties() {
		return tank.getTankProperties();
	}

	@Override
	public int fill(FluidStack resource, boolean doFill) {

		//only the shielding gas goes in, and only while the machine is set up to use it
		if(resource == null || resource.getFluid() != ModForgeFluids.argon || mode != MODE_TIG)
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
		return new int[] { 0, 1, 2, 3 };
	}

	@Override
	public boolean isItemValidForSlot(int slot, ItemStack stack) {

		if(slot > 2)
			return false;

		//one kind of feedstock belongs in one slot, so the other two stay free for the rest of the recipe
		for(int i = 0; i < 3; i++) {
			if(i == slot)
				continue;

			ItemStack other = inventory.getStackInSlot(i);

			if(!other.isEmpty() && other.getItem() == stack.getItem() && other.getItemDamage() == stack.getItemDamage())
				return false;
		}

		return true;
	}

	@Override
	public boolean canExtractItem(int i, ItemStack stack, int amount) {
		return i == 3;
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
			bb = new AxisAlignedBB(pos.getX() - 2, pos.getY(), pos.getZ() - 2, pos.getX() + 3, pos.getY() + 3, pos.getZ() + 3);

		return bb;
	}

	@Override
	@SideOnly(Side.CLIENT)
	public double getMaxRenderDistanceSquared() {
		return 65536.0D;
	}
}
