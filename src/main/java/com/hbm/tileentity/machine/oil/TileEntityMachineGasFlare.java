package com.hbm.tileentity.machine.oil;

import com.hbm.entity.particle.EntityGasFlameFX;
import com.hbm.explosion.ExplosionThermo;
import com.hbm.forgefluid.FFUtils;
import com.hbm.interfaces.IControlReceiver;
import com.hbm.inventory.FluidCombustionRecipes;
import com.hbm.inventory.GasFlareRecipes;
import com.hbm.inventory.container.ContainerMachineGasFlare;
import com.hbm.inventory.gui.GUIMachineGasFlare;
import com.hbm.items.machine.ItemFlareCatalyst;
import com.hbm.lib.HBMSoundHandler;
import com.hbm.main.MainRegistry;
import com.hbm.particle.ParticleRBMKFlame;
import com.hbm.sound.AudioWrapper;
import com.hbm.tileentity.IBufPacketReceiver;
import com.hbm.tileentity.IGUIProvider;
import com.hbm.tileentity.TileEntityMachineBase;
import io.netty.buffer.ByteBuf;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.SoundEvents;
import net.minecraft.inventory.Container;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ITickable;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.world.World;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidTank;
import net.minecraftforge.fluids.capability.CapabilityFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.IFluidTankProperties;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public class TileEntityMachineGasFlare extends TileEntityMachineBase implements ITickable, IFluidHandler, IGUIProvider, IControlReceiver, IBufPacketReceiver {

	public FluidTank tank;
	public FluidTank outputTank;
	public boolean isOn = false;
	public boolean doesBurn = false;
	public boolean needsUpdate;
	public boolean isProcessing;

	public float byproductCoefficient = 1.0F;

	private boolean prevDoesBurn = false;
	private AudioWrapper audio;

	@SideOnly(Side.CLIENT)
	private ParticleRBMKFlame flameParticle;

	public TileEntityMachineGasFlare() {
		super(5); // 0=Fuel In, 1=Fuel Out, 2=Catalyst, 3=Fluid Empty Canister In, 4=Fluid Canister Out
		tank = new FluidTank(64000);
		outputTank = new FluidTank(64000);
		needsUpdate = false;
	}

	@Override
	public String getName() {
		return "container.gasFlare";
	}

	public boolean isUseableByPlayer(EntityPlayer player) {
		if(world.getTileEntity(pos) != this) return false;
		return player.getDistanceSq(pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D) <= 128;
	}

	@Override
	public void readFromNBT(NBTTagCompound compound) {
		tank.readFromNBT(compound);
		outputTank.readFromNBT(compound.getCompoundTag("outputTank"));
		isOn = compound.getBoolean("isOn");
		doesBurn = compound.getBoolean("doesBurn");
		byproductCoefficient = compound.getFloat("byproductCoefficient");
		prevDoesBurn = doesBurn;
		super.readFromNBT(compound);
	}

	@Override
	public NBTTagCompound writeToNBT(NBTTagCompound compound) {
		tank.writeToNBT(compound);
		NBTTagCompound outTankNBT = new NBTTagCompound();
		outputTank.writeToNBT(outTankNBT);
		compound.setTag("outputTank", outTankNBT);
		compound.setBoolean("isOn", isOn);
		compound.setBoolean("doesBurn", doesBurn);
		compound.setFloat("byproductCoefficient", byproductCoefficient);
		return super.writeToNBT(compound);
	}

	@Override
	public void update() {
		if(!world.isRemote) {
			int prevAmountIn = tank.getFluidAmount();
			int prevAmountOut = outputTank.getFluidAmount();

			if(needsUpdate) {
				needsUpdate = false;
			}

			if(this.inputValidForTank(0)) {
				if(FFUtils.fillFromFluidContainer(inventory, tank, 0, 1)) needsUpdate = true;
			}

			if(FFUtils.fillFluidContainer(inventory, outputTank, 3, 4)) {
				needsUpdate = true;
			}

			if(doesBurn && !prevDoesBurn) {
				world.playSound(null, pos.getX(), pos.getY() + 12, pos.getZ(), SoundEvents.ITEM_FLINTANDSTEEL_USE, SoundCategory.BLOCKS, 0.6F, 1.0F);
			}
			if(!doesBurn && prevDoesBurn) {
				world.playSound(null, pos.getX(), pos.getY() + 12, pos.getZ(), SoundEvents.BLOCK_FIRE_EXTINGUISH, SoundCategory.BLOCKS, 0.09F, 1.0F);
			}
			prevDoesBurn = doesBurn;

			if(doesBurn && this.world.getTotalWorldTime() % 90 == 0) {
				this.world.playSound(null, pos.getX(), pos.getY() + 12, pos.getZ(), SoundEvents.BLOCK_FIRE_AMBIENT, SoundCategory.BLOCKS, 0.5F, 1F);
			}

			if(isOn && doesBurn && tank.getFluidAmount() > 0 && tank.getFluid() != null) {
				Fluid currentFluid = tank.getFluid().getFluid();

				if (currentFluid != null) {
					ItemStack catalystStack = inventory.getStackInSlot(2);
					ItemFlareCatalyst.CatalystType currentCatalyst = ItemFlareCatalyst.getType(catalystStack);

					GasFlareRecipes.Recipe recipe = GasFlareRecipes.getRecipe(currentFluid, tank.getFluidAmount(), currentCatalyst);

					if (recipe != null) {
						tank.drain(recipe.inputAmount, true);

						if (recipe.outputFluid != null && recipe.outputAmount > 0) {
							int outAmount = (int) (recipe.outputAmount * byproductCoefficient);
							outputTank.fill(new FluidStack(recipe.outputFluid, outAmount), true);
						}

						needsUpdate = true;
						isProcessing = true;
						spawnFireEffects();

					} else if (FluidCombustionRecipes.hasFuelRecipe(currentFluid)) {
						int burnAmount = Math.max(10, FluidCombustionRecipes.getFlameEnergy(currentFluid) / 50);
						burnAmount = Math.min(burnAmount, tank.getFluidAmount());

						if (burnAmount > 0) {
							tank.drain(burnAmount, true);
							needsUpdate = true;
							isProcessing = true;
							spawnFireEffects();
						} else {
							isProcessing = false;
						}
					} else {
						isProcessing = false;
					}
				}
			} else {
				isProcessing = false;
			}

			networkPackNT(25);

			if(prevAmountIn != tank.getFluidAmount() || prevAmountOut != outputTank.getFluidAmount() || needsUpdate){
				markDirty();
			}
		} else {
			float volume = this.getVolume(2);
			boolean isVenting = isOn && tank.getFluidAmount() >= 10;

			if(isVenting && volume > 0) {
				if(audio == null) {
					audio = MainRegistry.proxy.getLoopedSound(HBMSoundHandler.flare_operate, SoundCategory.BLOCKS, pos.getX(), pos.getY(), pos.getZ(), volume, 1.0F);
					audio.startSound();
				}
				if(doesBurn && isProcessing) {
					audio.updateVolume(volume);
					audio.updatePitch(1.0F);
					audio.updateRange(37.0F);
				} else {
					audio.updateVolume(0.8F);
					audio.updatePitch(0.6F);
					audio.updateRange(15.0F);
				}
			} else {
				if(audio != null) {
					audio.stopSound();
					audio = null;
				}
			}

			if(doesBurn && isProcessing) {
				if(flameParticle == null || flameParticle.isDead()) {
					flameParticle = new ParticleRBMKFlame(world, pos.getX() + 1.5, pos.getY() + 10.8, pos.getZ() + 1.5, 200);
					flameParticle.setParticleScale(0.4F);
					Minecraft.getMinecraft().effectRenderer.addEffect(flameParticle);
				}
			} else {
				if(flameParticle != null && !flameParticle.isDead()) {
					flameParticle.kill();
					flameParticle = null;
				}
			}
		}
	}

	private void spawnFireEffects() {
		world.spawnEntity(new EntityGasFlameFX(world, pos.getX() + 0.5F, pos.getY() + 11F, pos.getZ() + 0.5F, 0.0, 0.0, 0.0));
		ExplosionThermo.setEntitiesOnFire(world, pos.getX(), pos.getY() + 11, pos.getZ(), 5);
	}

	@Override
	public void serialize(ByteBuf buf) {
		buf.writeBoolean(isOn);
		buf.writeBoolean(doesBurn);
		buf.writeBoolean(isProcessing);
		buf.writeFloat(byproductCoefficient);
		ByteBufUtils.writeTag(buf, tank.writeToNBT(new NBTTagCompound()));
		ByteBufUtils.writeTag(buf, outputTank.writeToNBT(new NBTTagCompound()));
	}

	@Override
	public void deserialize(ByteBuf buf) {
		this.isOn = buf.readBoolean();
		this.doesBurn = buf.readBoolean();
		this.isProcessing = buf.readBoolean();
		this.byproductCoefficient = buf.readFloat();
		tank.readFromNBT(ByteBufUtils.readTag(buf));
		outputTank.readFromNBT(ByteBufUtils.readTag(buf));
	}

	@Override
	public int[] getAccessibleSlotsFromSide(EnumFacing e) {
		return new int[] {0, 1, 2, 3, 4};
	}

	@Override
	public boolean canInsertItem(int slot, ItemStack itemStack, int amount) {
		return this.isItemValidForSlot(slot, itemStack);
	}

	@Override
	public boolean isItemValidForSlot(int i, ItemStack stack) {
		if(stack.getItem() instanceof ItemFlareCatalyst) {
			return i == 2; // Только катализатор в слот 2
		}
		if(i == 4) {
			return false;
		}
		return i == 0 || i == 1 || i == 3;
	}

	protected boolean inputValidForTank(int slot){
		return !inventory.getStackInSlot(slot).isEmpty();
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
		return new IFluidTankProperties[]{tank.getTankProperties()[0], outputTank.getTankProperties()[0]};
	}

	@Override
	public int fill(FluidStack resource, boolean doFill) {
		return tank.fill(resource, doFill);
	}

	@Override
	public FluidStack drain(FluidStack resource, boolean doDrain) {
		if (resource == null || resource.getFluid() == null) return null;
		// Разрешаем забирать только из выходного бака (outputTank) и только если жидкость совпадает
		if (outputTank.getFluid() != null && outputTank.getFluid().isFluidEqual(resource)) {
			return outputTank.drain(resource, doDrain);
		}
		return null;
	}

	@Override
	public FluidStack drain(int maxDrain, boolean doDrain) {
		return outputTank.drain(maxDrain, doDrain);
	}

	@Override
	public boolean hasCapability(Capability<?> capability, EnumFacing facing) {
		if(capability == CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY) {
			return true;
		} else {
			return super.hasCapability(capability, facing);
		}
	}

	@Override
	public <T> T getCapability(Capability<T> capability, EnumFacing facing) {
		if(capability == CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY) {
			return CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY.cast(this);
		} else {
			return super.getCapability(capability, facing);
		}
	}

	@Override
	public Container provideContainer(int ID, EntityPlayer player, World world, int x, int y, int z) {
		return new ContainerMachineGasFlare(player.inventory, this);
	}

	@Override
	@SideOnly(Side.CLIENT)
	public GuiScreen provideGUI(int ID, EntityPlayer player, World world, int x, int y, int z) {
		return new GUIMachineGasFlare(player.inventory, this);
	}

	@Override
	public boolean hasPermission(EntityPlayer player) {
		return player.getDistanceSq(pos) <= 256D;
	}

	@Override
	public void receiveControl(NBTTagCompound data) {
		if(data.hasKey("valve")) this.isOn = !this.isOn;
		if(data.hasKey("dial")) this.doesBurn = !this.doesBurn;
		markDirty();
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
}