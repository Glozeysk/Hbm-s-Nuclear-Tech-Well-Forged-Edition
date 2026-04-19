package com.hbm.tileentity.machine;

import java.util.Random;

import com.hbm.blocks.BlockDummyable;
import com.hbm.forgefluid.FFUtils;
import com.hbm.forgefluid.ModForgeFluids;
import com.hbm.interfaces.Untested;
import com.hbm.inventory.MachineRecipes;
import com.hbm.items.ModItems;
import com.hbm.items.machine.ItemForgeFluidIdentifier;
import com.hbm.lib.ForgeDirection;
import com.hbm.lib.HBMSoundHandler;
import com.hbm.lib.Library;
import com.hbm.main.MainRegistry;
import com.hbm.sound.AudioWrapper;
import com.hbm.tileentity.IBufPacketReceiver;
import com.hbm.tileentity.TileEntityMachineBase;

import api.hbm.energy.IEnergyGenerator;
import io.netty.buffer.ByteBuf;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ITickable;
import net.minecraft.util.SoundCategory;
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

public class TileEntityMachineLargeTurbine extends TileEntityMachineBase implements ITickable, IEnergyGenerator, IFluidHandler, IBufPacketReceiver {

	public long power;
	public static final long maxPower = 100000000;
	public int age = 0;
	public FluidTank[] tanks;
	public Fluid[] types = new Fluid[2];

	private boolean shouldTurn;
	public float rotor;
	public float lastRotor;
	public float fanAcceleration = 0F;

	@SideOnly(Side.CLIENT)
	private AudioWrapper audio;

	private final float audioDesync;

	public TileEntityMachineLargeTurbine() {
		super(7);
		tanks = new FluidTank[2];
		tanks[0] = new FluidTank(512000);
		tanks[1] = new FluidTank(10240000);
		types[0] = ModForgeFluids.steam;
		types[1] = ModForgeFluids.spentsteam;

		Random rand = new Random();
		audioDesync = rand.nextFloat() * 0.05F;
	}

	@Untested
	@Override
	public void update() {
		if(!world.isRemote) {

			age++;
			if(age >= 2) {
				age = 0;
			}

			fillFluidInit(tanks[1]);
			ForgeDirection dir = ForgeDirection.getOrientation(this.getBlockMetadata() - BlockDummyable.offset);
			this.sendPower(world, pos.add(dir.offsetX * -4, 0, dir.offsetZ * -4), dir.getOpposite());

			if(inventory.getStackInSlot(0).getItem() == ModItems.forge_fluid_identifier && inventory.getStackInSlot(1).isEmpty()) {
				Fluid f = ItemForgeFluidIdentifier.getType(inventory.getStackInSlot(0));
				if(isValidFluidForTank(0, new FluidStack(f, 1000))) {
					types[0] = f;
					if(tanks[0].getFluid() != null && tanks[0].getFluid().getFluid() != types[0])
						tanks[0].setFluid(null);
					inventory.setStackInSlot(1, inventory.getStackInSlot(0));
					inventory.setStackInSlot(0, ItemStack.EMPTY);
				}
			}

			if(inputValidForTank(0, 2))
				FFUtils.fillFromFluidContainer(inventory, tanks[0], 2, 3);
			power = Library.chargeItemsFromTE(inventory, 4, power, maxPower);

			boolean operational = false;

			Object[] outs = MachineRecipes.getTurbineOutput(types[0]);

			if(outs == null) {
				types[1] = null;
				tanks[1].setFluid(null);
			} else {
				types[1] = (Fluid) outs[0];
				if(tanks[1].getFluid() != null && tanks[1].getFluid().getFluid() != types[1])
					tanks[1].setFluid(null);

				int processMax = (int) Math.ceil(Math.ceil(tanks[0].getFluidAmount() / 10F) / (Integer) outs[2]);
				int processSteam = tanks[0].getFluidAmount() / (Integer) outs[2];
				int processWater = (tanks[1].getCapacity() - tanks[1].getFluidAmount()) / (Integer) outs[1];

				int cycles = Math.min(processMax, Math.min(processSteam, processWater));

				tanks[0].drain((Integer) outs[2] * cycles, true);
				tanks[1].fill(new FluidStack(types[1], (Integer) outs[1] * cycles), true);

				power += (Integer) outs[3] * cycles;

				if(power > maxPower)
					power = maxPower;
				if(cycles > 0)
					operational = true;
			}

			FFUtils.fillFluidContainer(inventory, tanks[1], 5, 6);

			this.shouldTurn = operational;
			networkPackNT(50);
		} else {

			this.lastRotor = this.rotor;

			if(shouldTurn) {
				this.fanAcceleration = Math.max(0F, Math.min(15F, this.fanAcceleration + 0.075F + audioDesync));

				if(audio == null) {
					audio = MainRegistry.proxy.getLoopedSound(HBMSoundHandler.turbofanOperate, SoundCategory.BLOCKS, pos.getX() + 0.5F, pos.getY() + 0.5F, pos.getZ() + 0.5F, 1.0F, 1.0F);
					if(audio != null) {
						audio.updateRange(10F);
						audio.startSound();
					}
				}

				if(audio != null) {
					float turbineSpeed = this.fanAcceleration / 15F;
					audio.updateVolume(0.4F * turbineSpeed);
					audio.updatePitch(0.25F + 0.75F * turbineSpeed);
				}
			} else {
				this.fanAcceleration = Math.max(0F, Math.min(15F, this.fanAcceleration - 0.1F));

				if(audio != null) {
					if(this.fanAcceleration > 0) {
						float turbineSpeed = this.fanAcceleration / 15F;
						audio.updateVolume(0.4F * turbineSpeed);
						audio.updatePitch(0.25F + 0.75F * turbineSpeed);
					} else {
						audio.stopSound();
						audio = null;
					}
				}
			}

			this.rotor += this.fanAcceleration;

			if(this.rotor >= 360) {
				this.rotor -= 360;
				this.lastRotor -= 360;
			}
		}
	}

	@Override
	public void onChunkUnload() {
		super.onChunkUnload();

		if(audio != null) {
			audio.stopSound();
			audio = null;
		}
	}

	@Override
	public void invalidate() {
		super.invalidate();

		if(audio != null) {
			audio.stopSound();
			audio = null;
		}
	}

	@Override
	public void serialize(ByteBuf buf) {
		buf.writeLong(this.power);
		buf.writeBoolean(this.shouldTurn);
		buf.writeInt(tanks[0].getFluidAmount());
		buf.writeInt(tanks[1].getFluidAmount());
		ByteBufUtils.writeUTF8String(buf, types[0] != null ? types[0].getName() : "");
		ByteBufUtils.writeUTF8String(buf, types[1] != null ? types[1].getName() : "");
	}

	@Override
	public void deserialize(ByteBuf buf) {
		this.power = buf.readLong();
		this.shouldTurn = buf.readBoolean();

		int amount0 = buf.readInt();
		int amount1 = buf.readInt();
		String typeName0 = ByteBufUtils.readUTF8String(buf);
		String typeName1 = ByteBufUtils.readUTF8String(buf);

		if(!typeName0.isEmpty()) {
			types[0] = FluidRegistry.getFluid(typeName0);
			if(types[0] != null && amount0 > 0) {
				tanks[0].setFluid(new FluidStack(types[0], amount0));
			} else {
				tanks[0].setFluid(null);
			}
		} else {
			types[0] = null;
			tanks[0].setFluid(null);
		}

		if(!typeName1.isEmpty()) {
			types[1] = FluidRegistry.getFluid(typeName1);
			if(types[1] != null && amount1 > 0) {
				tanks[1].setFluid(new FluidStack(types[1], amount1));
			} else {
				tanks[1].setFluid(null);
			}
		} else {
			types[1] = null;
			tanks[1].setFluid(null);
		}
	}

	protected boolean inputValidForTank(int tank, int slot) {
		if(inventory.getStackInSlot(slot) != ItemStack.EMPTY && tanks[tank] != null) {
			FluidStack f = FluidUtil.getFluidContained(inventory.getStackInSlot(slot));
			if(f != null && f.getFluid() == types[tank])
				return true;
		}
		return false;
	}

	private boolean isValidFluidForTank(int tank, FluidStack stack) {
		if(stack == null || tanks[tank] == null)
			return false;
		return stack.getFluid() == ModForgeFluids.steam || stack.getFluid() == ModForgeFluids.hotsteam || stack.getFluid() == ModForgeFluids.superhotsteam || stack.getFluid() == ModForgeFluids.ultrahotsteam;
	}

	public long getPowerScaled(int i) {
		return (power * i) / maxPower;
	}

	@Override
	public void readFromNBT(NBTTagCompound compound) {
		super.readFromNBT(compound);
		if(compound.hasKey("tankType0"))
			types[0] = FluidRegistry.getFluid(compound.getString("tankType0"));
		else
			types[0] = null;
		if(compound.hasKey("tankType1"))
			types[1] = FluidRegistry.getFluid(compound.getString("tankType1"));
		else
			types[1] = null;

		FFUtils.deserializeTankArray(compound.getTagList("tanks", 10), tanks);
		power = compound.getLong("power");
	}

	@Override
	public NBTTagCompound writeToNBT(NBTTagCompound compound) {
		compound.setTag("tanks", FFUtils.serializeTankArray(tanks));
		compound.setLong("power", power);
		if(types[0] != null)
			compound.setString("tankType0", types[0].getName());
		if(types[1] != null)
			compound.setString("tankType1", types[1].getName());
		return super.writeToNBT(compound);
	}

	@Override
	public String getName() {
		return "container.machineLargeTurbine";
	}

	public void fillFluidInit(FluidTank type) {
		ForgeDirection dir = ForgeDirection.getOrientation(this.getBlockMetadata() - BlockDummyable.offset);
		dir = dir.getRotation(ForgeDirection.UP);

		fillFluid(pos.getX() + dir.offsetX * 2, pos.getY(), pos.getZ() + dir.offsetZ * 2, type);
		fillFluid(pos.getX() + dir.offsetX * -2, pos.getY(), pos.getZ() + dir.offsetZ * -2, type);
	}

	public void fillFluid(int x, int y, int z, FluidTank type) {
		FFUtils.fillFluid(this, type, world, new BlockPos(x, y, z), 10239000);
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
	public IFluidTankProperties[] getTankProperties() {
		return new IFluidTankProperties[]{tanks[0].getTankProperties()[0], tanks[1].getTankProperties()[0]};
	}

	@Override
	public int fill(FluidStack resource, boolean doFill) {
		if(resource != null && resource.getFluid() == types[0]) {
			return tanks[0].fill(resource, doFill);
		}
		return 0;
	}

	@Override
	public FluidStack drain(FluidStack resource, boolean doDrain) {
		if(resource != null && resource.getFluid() == types[1]) {
			return tanks[1].drain(resource, doDrain);
		}
		return null;
	}

	@Override
	public FluidStack drain(int maxDrain, boolean doDrain) {
		return tanks[1].drain(maxDrain, doDrain);
	}

	@Override
	public <T> T getCapability(Capability<T> capability, EnumFacing facing) {
		if(capability == CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY) {
			return CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY.cast(this);
		}
		return super.getCapability(capability, facing);
	}

	@Override
	public boolean hasCapability(Capability<?> capability, EnumFacing facing) {
		if(capability == CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY) {
			return true;
		}
		return super.hasCapability(capability, facing);
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
}