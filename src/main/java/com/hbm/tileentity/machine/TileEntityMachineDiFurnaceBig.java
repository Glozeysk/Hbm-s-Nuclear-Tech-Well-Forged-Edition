package com.hbm.tileentity.machine;

import api.hbm.energy.IEnergyUser;
import com.hbm.blocks.BlockDummyable;
import com.hbm.entity.particle.EntityGasFlameFX;
import com.hbm.forgefluid.FFUtils;
import com.hbm.forgefluid.ModForgeFluids;
import com.hbm.inventory.DiFurnaceRecipes;
import com.hbm.lib.ForgeDirection;
import com.hbm.lib.HBMSoundHandler;
import com.hbm.lib.Library;
import com.hbm.tileentity.IBufPacketReceiver;
import com.hbm.tileentity.TileEntityMachineBase;
import io.netty.buffer.ByteBuf;
import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.ITickable;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.MathHelper;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidTank;
import net.minecraftforge.fluids.FluidUtil;
import net.minecraftforge.fluids.capability.CapabilityFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.IFluidTankProperties;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public class TileEntityMachineDiFurnaceBig extends TileEntityMachineBase implements ITickable, IEnergyUser, IFluidHandler, IBufPacketReceiver {

	public long power = 0;
	public int process = 0;
	public static final long maxPower = 500000000;
	public FluidTank tank;
	public Fluid tankType = ModForgeFluids.nitan;
	public boolean needsUpdate = false;
	public boolean isRunning = false;

	public TileEntityMachineDiFurnaceBig() {
		super(11);
		tank = new FluidTank(16000);
	}

	@Override
	public String getName(){
		return "container.machinedifurnacebig";
	}

	public int getProcessSpeed() {
		if(tank.getFluid() != null) {
			if(tank.getFluid().getFluid() == ModForgeFluids.nitan) return 20;
			if(tank.getFluid().getFluid() == ModForgeFluids.balefire) return 4;
			if(tank.getFluid().getFluid() == ModForgeFluids.sparkfuel) return 2;
			if(tank.getFluid().getFluid() == ModForgeFluids.uu_matter) return 1;
		}
		return 20;
	}

	@Override
	public int[] getAccessibleSlotsFromSide(EnumFacing face) {
		return new int[] { 0, 1, 2, 3, 4, 5, 6, 7, 10 };
	}

	private void updateConnections() {
		ForgeDirection dir = ForgeDirection.getOrientation(this.getBlockMetadata() - BlockDummyable.offset);

		this.trySubscribe(world, pos.add(-1, 0, -2), ForgeDirection.NORTH);
		this.trySubscribe(world, pos.add(1, 0, -2), ForgeDirection.NORTH);
		this.trySubscribe(world, pos.add(-1, 0, 2), ForgeDirection.SOUTH);
		this.trySubscribe(world, pos.add(1, 0, 2), ForgeDirection.SOUTH);
		this.trySubscribe(world, pos.add(-2, 0, -1), ForgeDirection.WEST);
		this.trySubscribe(world, pos.add(-2, 0, 1), ForgeDirection.WEST);
		this.trySubscribe(world, pos.add(2, 0, -1), ForgeDirection.EAST);
		this.trySubscribe(world, pos.add(2, 0, 1), ForgeDirection.EAST);
	}

	@Override
	public boolean canInsertItem(int slot, ItemStack stack, int amount) {
		return isItemValidForSlot(slot, stack);
	}
	@Override
	public boolean canExtractItem(int i, ItemStack itemStack, int amount){
		return i == 4 || i == 5 || i == 6 || i == 7 || i == 10;
	}

	@Override
	public boolean isItemValidForSlot(int i, ItemStack stack){
		return i != 4 && i != 5 && i != 6 && i != 7 && i != 8 && i != 9 && i != 10;
	}

	public boolean isUseableByPlayer(EntityPlayer player) {
		if(world.getTileEntity(pos) != this) return false;
		return player.getDistanceSq(pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D) <= 64;
	}

	@Override
	public void readFromNBT(NBTTagCompound compound) {
		power = compound.getLong("power");
		tank.readFromNBT(compound);
		process = compound.getShort("process");
		isRunning = compound.getBoolean("isRunning");
		super.readFromNBT(compound);
	}

	@Override
	public NBTTagCompound writeToNBT(NBTTagCompound compound) {
		compound.setLong("power", power);
		tank.writeToNBT(compound);
		compound.setShort("process", (short) process);
		compound.setBoolean("isRunning", isRunning);
		return super.writeToNBT(compound);
	}

	@SideOnly(Side.CLIENT)
	private void spawnSmoke(double x, double y, double z) {
		Particle p = new Particle(world, x, y, z) {
			private float baseScale;
			{
				this.motionX = 0.0D;
				this.motionY = 0.05D;
				this.motionZ = 0.0D;
				this.particleRed = 0.9F;
				this.particleGreen = 0.9F;
				this.particleBlue = 0.9F;
				this.particleMaxAge = (int)(8.0F / (this.rand.nextFloat() * 0.9F + 0.1F));
				this.baseScale = this.particleScale * 1.1F;
				this.particleScale = 0;
				this.setParticleTextureIndex(0);
			}

			@Override
			public void onUpdate() {
				this.prevPosX = this.posX;
				this.prevPosY = this.posY;
				this.prevPosZ = this.posZ;
				if (this.particleAge++ >= this.particleMaxAge) this.setExpired();
				this.setParticleTextureIndex(7 - this.particleAge * 8 / this.particleMaxAge);
				this.posX += this.motionX;
				this.posY += this.motionY;
				this.posZ += this.motionZ;
				this.motionX *= 0.96D;
				this.motionY *= 0.96D;
				this.motionZ *= 0.96D;
			}

			@Override
			public void renderParticle(BufferBuilder buffer, Entity entityIn, float partialTicks, float rotationX, float rotationZ, float rotationYZ, float rotationXY, float rotationXZ) {
				float f = ((float)this.particleAge + partialTicks) / (float)this.particleMaxAge * 32.0F;
				f = MathHelper.clamp(f, 0.0F, 1.0F);
				this.particleScale = this.baseScale * f;
				super.renderParticle(buffer, entityIn, partialTicks, rotationX, rotationZ, rotationYZ, rotationXY, rotationXZ);
			}

			@Override
			public int getBrightnessForRender(float partialTick) {
				int i = super.getBrightnessForRender(partialTick);
				int k = i >> 16 & 0xFF;
				return 240 | (k << 16);
			}
		};
		Minecraft.getMinecraft().effectRenderer.addEffect(p);
	}

	public long getPowerScaled(long i) {
		return (power * i) / maxPower;
	}

	public int getProgressScaled(int i) {
		return (process * i) / getProcessSpeed();
	}

	private boolean hasValidPair(int a, int b) {
		if(inventory.getStackInSlot(a).isEmpty() || inventory.getStackInSlot(b).isEmpty()) return false;
		ItemStack result = DiFurnaceRecipes.getFurnaceProcessingResult(inventory.getStackInSlot(a), inventory.getStackInSlot(b));
		return result != null && !result.isEmpty();
	}

	public boolean canProcess() {
		if(tank.getFluidAmount() < 5) return false;
		if(power < 500000) return false;
		return hasValidPair(0, 2) || hasValidPair(1, 3);
	}

	private void processPair(int a, int b) {
		ItemStack result = DiFurnaceRecipes.getFurnaceProcessingResult(inventory.getStackInSlot(a), inventory.getStackInSlot(b));
		if(result == null || result.isEmpty()) return;

		int remaining = result.getCount();
		for(int s : new int[]{4, 5, 6, 7}) {
			if(remaining <= 0) break;
			ItemStack slot = inventory.getStackInSlot(s);
			if(slot.isEmpty()) {
				ItemStack out = result.copy();
				out.setCount(Math.min(remaining, result.getMaxStackSize()));
				inventory.setStackInSlot(s, out);
				remaining -= out.getCount();
			} else if(slot.isItemEqual(result)) {
				int add = Math.min(remaining, result.getMaxStackSize() - slot.getCount());
				slot.grow(add);
				remaining -= add;
			}
		}

		inventory.getStackInSlot(a).shrink(1);
		inventory.getStackInSlot(b).shrink(1);
		if(inventory.getStackInSlot(a).getCount() <= 0) inventory.setStackInSlot(a, ItemStack.EMPTY);
		if(inventory.getStackInSlot(b).getCount() <= 0) inventory.setStackInSlot(b, ItemStack.EMPTY);
	}

	public void process() {
		tank.drain(5, true);
		needsUpdate = true;
		power -= 500000;
		process++;

		if(process >= getProcessSpeed()) {
			if(hasValidPair(0, 2)) {
				processPair(0, 2);
			}
			if(hasValidPair(1, 3)) {
				processPair(1, 3);
			}
			process = 0;
		}
	}

	@Override
	public void update() {
		if (!world.isRemote) {
			this.updateConnections();

			power = Library.chargeTEFromItems(inventory, 8, power, maxPower);
			if(inputValidForTank(9) && tank.getFluidAmount() < tank.getCapacity()){
				FFUtils.fillFromFluidContainer(inventory, tank, 9, 10);
			}

			long prevPower = power;
			int prevAmount = tank.getFluidAmount();
			if (needsUpdate) {
				needsUpdate = false;
			}

			if(!inventory.getStackInSlot(1).isEmpty() && inventory.getStackInSlot(0).isEmpty()) {
				inventory.setStackInSlot(0, inventory.getStackInSlot(1).copy());
				inventory.setStackInSlot(1, ItemStack.EMPTY);
			}
			if(!inventory.getStackInSlot(3).isEmpty() && inventory.getStackInSlot(2).isEmpty()) {
				inventory.setStackInSlot(2, inventory.getStackInSlot(3).copy());
				inventory.setStackInSlot(3, ItemStack.EMPTY);
			}

			if (canProcess()) {
				isRunning = true;
				process();
				world.spawnEntity(new EntityGasFlameFX(world, pos.getX() + 1.671875F, pos.getY() + 3.2F, pos.getZ() + 1.671875F, 0.0, 0.0, 0.0, 0.15F));
				world.spawnEntity(new EntityGasFlameFX(world, pos.getX() - 0.671875F, pos.getY() + 3.2F, pos.getZ() + 1.671875F, 0.0, 0.0, 0.0, 0.15F));
				world.spawnEntity(new EntityGasFlameFX(world, pos.getX() + 1.671875F, pos.getY() + 3.2F, pos.getZ() - 0.671875F, 0.0, 0.0, 0.0, 0.15F));
				world.spawnEntity(new EntityGasFlameFX(world, pos.getX() - 0.671875F, pos.getY() + 3.2F, pos.getZ() - 0.671875F, 0.0, 0.0, 0.0, 0.15F));

				if(this.world.getTotalWorldTime() % 20 == 0)
					this.world.playSound(null, pos.getX() + 0.5F, pos.getY() + 0.5F, pos.getZ() + 0.5F, HBMSoundHandler.difurnace_loop, SoundCategory.BLOCKS, 1F, 1F);
			} else {
				isRunning = false;
				process = 0;
			}

			networkPackNT(15);

			if(prevPower != power || prevAmount != tank.getFluidAmount()){
				markDirty();
			}
		} else {
			if(isRunning) {
				process++;
				if(process >= getProcessSpeed()) {
					process = 0;
				}

				ForgeDirection dir = ForgeDirection.getOrientation(this.getBlockMetadata() - 10);
				ForgeDirection rot = dir.getRotation(ForgeDirection.UP);

				if(this.world.getTotalWorldTime() % 2 == 0) {
					switch (dir) {
						case WEST:
							world.spawnParticle(EnumParticleTypes.FLAME,
									pos.getX() - 0.5 + rot.offsetX * world.rand.nextDouble(),
									pos.getY() + 1.65 + world.rand.nextDouble() * 0.25,
									pos.getZ() + 0.9 * world.rand.nextDouble(), 0.0, 0.0, 0.0);
							break;
						case EAST:
							world.spawnParticle(EnumParticleTypes.FLAME,
									pos.getX() + 1.45 + rot.offsetX * world.rand.nextDouble(),
									pos.getY() + 1.65 + world.rand.nextDouble() * 0.25,
									pos.getZ() + rot.offsetZ * world.rand.nextDouble(), 0.0, 0.0, 0.0);
							break;
						case NORTH:
							world.spawnParticle(EnumParticleTypes.FLAME,
									pos.getX() + rot.offsetX * world.rand.nextDouble(),
									pos.getY() + 1.65 + world.rand.nextDouble() * 0.3,
									pos.getZ() - 0.58, 0.0, 0.0, 0.0);
							break;
						case SOUTH:
							world.spawnParticle(EnumParticleTypes.FLAME,
									pos.getX() + 0.9 + rot.offsetX * world.rand.nextDouble(),
									pos.getY() + 1.65 + world.rand.nextDouble() * 0.25,
									pos.getZ() + 1.45 - rot.offsetZ * world.rand.nextDouble(), 0.0, 0.0, 0.0);
						default:
							break;
					}
				}
				if(this.world.getTotalWorldTime() % 10 == 0) {
					spawnSmoke(pos.getX() + 1.671875F, pos.getY() + 1F, pos.getZ() + 1.671875F);
					spawnSmoke(pos.getX() - 0.671875F, pos.getY() + 1F, pos.getZ() + 1.671875F);
					spawnSmoke(pos.getX() + 1.671875F, pos.getY() + 1F, pos.getZ() - 0.671875F);
					spawnSmoke(pos.getX() - 0.671875F, pos.getY() + 1F, pos.getZ() - 0.671875F);
				}
			}
		}
	}

	@Override
	public void serialize(ByteBuf buf) {
		buf.writeBoolean(this.isRunning);
		buf.writeShort(this.process);
		buf.writeLong(this.power);
		ByteBufUtils.writeTag(buf, tank.writeToNBT(new NBTTagCompound()));
	}

	@Override
	public void deserialize(ByteBuf buf) {
		boolean wasRunning = this.isRunning;
		this.isRunning = buf.readBoolean();
		int serverProcess = buf.readShort();
		this.power = buf.readLong();
		tank.readFromNBT(ByteBufUtils.readTag(buf));
		if(tank.getFluid() != null)
			tankType = tank.getFluid().getFluid();

		if(!isRunning) {
			this.process = 0;
		} else if(!wasRunning) {
			this.process = serverProcess;
		}
	}

	protected boolean inputValidForTank(int slot){
		if(!inventory.getStackInSlot(slot).isEmpty()){
			FluidStack containerFluid = FluidUtil.getFluidContained(inventory.getStackInSlot(slot));
			if(containerFluid != null){
				if(isValidFluid(containerFluid)){
					setTankType(containerFluid.getFluid());
					return true;
				}
			}
		}
		return false;
	}

	public void setTankType(Fluid f){
		if(f != null && (tank.getFluid() == null || (tank.getFluid() != null && tank.getFluid().getFluid() != f))){
			tank.setFluid(new FluidStack(f, 0));
		}
	}

	private boolean isValidFluid(FluidStack stack) {
		if(stack == null) return false;
		return stack.getFluid() == ModForgeFluids.sparkfuel
				|| stack.getFluid() == ModForgeFluids.nitan
				|| stack.getFluid() == ModForgeFluids.uu_matter
				|| stack.getFluid() == ModForgeFluids.balefire;
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
	public void setPower(long i) {
		power = i;
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
		return new IFluidTankProperties[]{tank.getTankProperties()[0]};
	}

	@Override
	public int fill(FluidStack resource, boolean doFill) {
		if (isValidFluid(resource)) {
			if(tank.fill(resource, false) > 0) needsUpdate = true;
			return tank.fill(resource, doFill);
		}
		return 0;
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
	public <T> T getCapability(Capability<T> capability, EnumFacing facing) {
		if(capability == CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY){
			return CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY.cast(this);
		} else {
			return super.getCapability(capability, facing);
		}
	}

	@Override
	public boolean hasCapability(Capability<?> capability, EnumFacing facing) {
		if(capability == CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY){
			return true;
		} else {
			return super.hasCapability(capability, facing);
		}
	}
}