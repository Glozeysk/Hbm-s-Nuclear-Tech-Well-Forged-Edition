package com.hbm.tileentity.machine;

import com.hbm.entity.effect.EntityBlackHole;
import com.hbm.interfaces.IControlReceiver;
import com.hbm.forgefluid.FFUtils;
import com.hbm.forgefluid.ModForgeFluids;
import com.hbm.inventory.SAFERecipes;
import com.hbm.items.ModItems;
import com.hbm.items.machine.ItemFWatzCore;
import com.hbm.lib.Library;
import com.hbm.lib.ModDamageSource;
import com.hbm.render.amlfrom1710.Vec3;
import com.hbm.tileentity.IBufPacketReceiver;
import com.hbm.tileentity.TileEntityMachineBase;
import com.hbm.world.FWatz;

import api.hbm.energy.IBatteryItem;
import api.hbm.energy.IEnergyGenerator;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ITickable;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.world.World;
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
import net.minecraftforge.items.ItemStackHandler;

import java.util.List;

public class TileEntityFWatzCore extends TileEntityMachineBase implements IControlReceiver, ITickable, IEnergyGenerator, IFluidHandler, IBufPacketReceiver {

	public long power;
	public final static long maxPower = 1000000000000L;
	public boolean cooldown = false;
    public int progress = 0;
	public final static int maxProgress = 4000;
    public boolean isProgressing;

	public FluidTank[] tanks;
	public Fluid[] tankTypes;
	public boolean needsUpdate = true;
	public boolean isOn = false;
    public boolean isOk = true;
    public boolean isDoingSomething = false;

	public TileEntityFWatzCore() {
		super(0);
        inventory = new ItemStackHandler(7){
			@Override
			protected void onContentsChanged(int slot) {
				super.onContentsChanged(slot);
				markDirty();
                if(slot == 2) progress = 0;
			}
		};
		tanks = new FluidTank[3];
		tankTypes = new Fluid[3];
		tanks[0] = new FluidTank(128000);
		tankTypes[0] = ModForgeFluids.coolant;
		tanks[1] = new FluidTank(64000);
		tankTypes[1] = ModForgeFluids.amat;
		tanks[2] = new FluidTank(64000);
		tankTypes[2] = ModForgeFluids.aschrab;
	}

	@Override
	public boolean hasPermission(EntityPlayer player){
		return true;
	}
	
	@Override
	public void receiveControl(NBTTagCompound data){
		this.isOn = !this.isOn;
		this.markDirty();
	}

	public String getName() {
		return "container.fusionaryWatzPlant";
	}

	public int getSingularityType(){
		Item item = inventory.getStackInSlot(2).getItem();
		if(item instanceof ItemFWatzCore core){
			return core.type;
		}
		return 0;
	}

    public int getType(){
        Item item = inventory.getStackInSlot(2).getItem();
        if(item instanceof ItemFWatzCore core){
            return core.type * (core.isBaby ? -1 : 1);
        }
        return 0;
    }

	@Override
	public void readFromNBT(NBTTagCompound compound) {
        progress = compound.getInteger("progress");
		power = compound.getLong("power");
		isOn = compound.getBoolean("isOn");
		tankTypes[0] = ModForgeFluids.coolant;
		tankTypes[1] = ModForgeFluids.amat;
		tankTypes[2] = ModForgeFluids.aschrab;
		if(compound.hasKey("tanks"))
			FFUtils.deserializeTankArray(compound.getTagList("tanks", 10), tanks);
		super.readFromNBT(compound);
	}

	@Override
	public NBTTagCompound writeToNBT(NBTTagCompound compound) {
        compound.setInteger("progress", progress);
		compound.setLong("power", power);
		compound.setBoolean("isOn", isOn);
		compound.setTag("tanks", FFUtils.serializeTankArray(tanks));
		return super.writeToNBT(compound);
	}

    public boolean canGrow() {
        if(!inventory.getStackInSlot(2).isEmpty() && inventory.getStackInSlot(2).getItem() instanceof ItemFWatzCore core && core.isBaby) {
            return true;
        }
        return false;
    }

    public boolean canGrowReversed() {
        if(!inventory.getStackInSlot(2).isEmpty() && inventory.getStackInSlot(2).getItem() instanceof ItemFWatzCore core && !core.isBaby) {
            return true;
        }
        return false;
    }

	@Override
	public void update() {
        if(inventory.getStackInSlot(2).getItem() instanceof ItemFWatzCore core) {
            doGravityStuff(world, isOk ? 6 : 30, 1, pos.getX()+0.5F, pos.getY()+2.5F, pos.getZ()+0.5F, (core.type / 2D + 2) * (core.isBaby ? 0.2 : 1));
        }
		if(!world.isRemote){
            if(this.isStructureValid(this.world)) {
                isOk = true;
                sendSAFEPower();

                if(isDoingSomething){
                    doElse();
                }

                if(inventory.getStackInSlot(2).getItem() instanceof ItemFWatzCore itemCore && this.isOn) {
                    if(cooldown) {

                        tanks[0].fill(new FluidStack(tankTypes[0], itemCore.coolantRefill), true);

                        if (tanks[0].getFluidAmount() >= tanks[0].getCapacity()) {
                            cooldown = false;
                        }
                        isDoingSomething = false;

                    } else {

                        if (tanks[1].getFluidAmount() > itemCore.amatDrain && tanks[2].getFluidAmount() > itemCore.aschrabDrain) {
                            tanks[0].drain(itemCore.coolantDrain, true);
                            tanks[1].drain(itemCore.amatDrain, true);
                            tanks[2].drain(itemCore.aschrabDrain, true);
                            needsUpdate = true;
                            power += itemCore.powerOutput;
                            isDoingSomething = true;
                            int processSpeed = 10;
                            if(!this.isOn){
                                progress = 0;
                            }
                            if(inventory.getStackInSlot(2).isEmpty() || !(inventory.getStackInSlot(2).getItem() instanceof ItemFWatzCore core)){
                                progress = 0;
                            }
                            if(inventory.getStackInSlot(2).getItem() instanceof ItemFWatzCore core && !core.isBaby){
                                progress = 0;
                            }
                            if(canGrow()){
				                isProgressing = true;
			                } else {
				                isProgressing = false;
			                }
                            if(isProgressing){
                                progress += processSpeed;
						        if(progress >= maxProgress) {
						            tryGrowCore();
						            progress = 0;
                                } 
                            } else {
                                progress = 0;
                            }
						    this.markDirty();
                        }

                        if (power > maxPower)
                            power = maxPower;

                        if (tanks[0].getFluidAmount() <= 0) {
                            cooldown = true;
                        }
                        }
                } else {
                    isDoingSomething = false;
                }

                if(power > maxPower)
                    power = maxPower;

                power = Library.chargeItemsFromTE(inventory, 0, power, maxPower);

                if(this.inputValidForTank(1, 3))
                    if(FFUtils.fillFromFluidContainer(inventory, tanks[1], 3, 5))
                        needsUpdate = true;
                if(this.inputValidForTank(2, 4))
                    if(FFUtils.fillFromFluidContainer(inventory, tanks[2], 4, 6))
                        needsUpdate = true;

            } else {
                isOk = false;
            }

            networkPackNT(50);

            if(needsUpdate) {
                needsUpdate = false;
                this.markDirty();
            }
        }
	}

    public void doElse(){
        ItemStack stack = inventory.getStackInSlot(2);
        if(stack.getItem() == ModItems.meteorite_sword_baleful){
            inventory.setStackInSlot(2, new ItemStack(ModItems.meteorite_sword_warped));
        } else if(stack.hasTagCompound()){
            NBTTagCompound nbt = stack.getTagCompound();
            if(nbt.getBoolean("ntmContagion")) nbt.removeTag("ntmContagion");
            if(nbt.isEmpty()) stack.setTagCompound(null);
        }
    }

	@Override
	public void serialize(ByteBuf buf) {
		buf.writeInt(this.progress);
		buf.writeLong(this.power);
		buf.writeBoolean(this.isOn);
		buf.writeBoolean(this.isOk);
		buf.writeBoolean(this.isDoingSomething);
		for(int i = 0; i < 3; i++) {
			buf.writeInt(this.tanks[i].getFluidAmount());
		}
		buf.writeBoolean(this.needsUpdate);
		if(this.needsUpdate) {
			ByteBufUtils.writeTag(buf, this.inventory.serializeNBT());
		}
	}

	@Override
	public void deserialize(ByteBuf buf) {
		this.progress = buf.readInt();
		this.power = buf.readLong();
		this.isOn = buf.readBoolean();
		this.isOk = buf.readBoolean();
		this.isDoingSomething = buf.readBoolean();
		for(int i = 0; i < 3; i++) {
			int amount = buf.readInt();
			this.tanks[i].setFluid(amount > 0 ? new FluidStack(tankTypes[i], amount) : null);
		}
		if(buf.readBoolean()) {
			this.inventory.deserializeNBT(ByteBufUtils.readTag(buf));
		}
	}

	private void sendSAFEPower(){
		this.sendPower(world, pos.add(7, 1, 0), Library.POS_X);
		this.sendPower(world, pos.add(-7, 1, 0), Library.NEG_X);
		this.sendPower(world, pos.add(0,  1, 7), Library.POS_Z);
		this.sendPower(world, pos.add(0, 1, -7), Library.NEG_Z);
        this.sendPower(world, pos.add(7, -3, 0), Library.POS_X);
        this.sendPower(world, pos.add(-7, -3, 0), Library.NEG_X);
        this.sendPower(world, pos.add(0, -3, 7), Library.POS_Z);
        this.sendPower(world, pos.add(0, -3, -7), Library.NEG_Z);
	}

	private void tryGrowCore(){
		ItemStack output = SAFERecipes.getOutput(inventory.getStackInSlot(2));
		if(output != null){
			inventory.setStackInSlot(2, output.copy());
		}
	}

	public boolean isStructureValid(World world) {
		return FWatz.checkHull(world, pos);
	}

	public long getPowerScaled(long i) {
		return (power / 100 * i) / (maxPower / 100);
	}

    public int getProgressScaled(int i) {
		return (progress * i) / maxProgress;
	}

	protected boolean inputValidForTank(int tank, int slot) {
		if(tanks[tank] != null) {
            return inventory.getStackInSlot(slot).getItem() == ModItems.fluid_barrel_infinite || isValidFluidForTank(tank, FluidUtil.getFluidContained(inventory.getStackInSlot(slot)));
		}
		return false;
	}

	private boolean isValidFluidForTank(int tank, FluidStack stack) {
		if(stack == null || tanks[tank] == null)
			return false;
		return stack.getFluid() == tankTypes[tank];
	}

	@Override
	public IFluidTankProperties[] getTankProperties() {
		return new IFluidTankProperties[]{tanks[0].getTankProperties()[0], tanks[1].getTankProperties()[0], tanks[2].getTankProperties()[0]};
	}

	@Override
	public int fill(FluidStack resource, boolean doFill) {
		if(resource == null) {
			return 0;
		} else if(resource.getFluid() == tankTypes[0]) {
			needsUpdate = true;
			return tanks[0].fill(resource, doFill);
		} else if(resource.getFluid() == tankTypes[1]) {
			needsUpdate = true;
			return tanks[1].fill(resource, doFill);
		} else if(resource.getFluid() == tankTypes[2]) {
			needsUpdate = true;
			return tanks[2].fill(resource, doFill);
		} else {
			return 0;
		}
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
        return new int[] {0, 2, 3, 4, 5, 6};
    }

    @Override
    public boolean isItemValidForSlot(int i, ItemStack stack) {
        if(stack.getItem() instanceof ItemFWatzCore){
            return i == 2;
        }
        if(stack.getItem() instanceof IBatteryItem){
			return i == 0;
		}
		if(FluidUtil.getFluidContained(stack) != null && FluidUtil.getFluidContained(stack).getFluid() == ModForgeFluids.amat){
        return i == 3;
		}
		if(FluidUtil.getFluidContained(stack) != null && FluidUtil.getFluidContained(stack).getFluid() == ModForgeFluids.aschrab){
        return i == 4;
		}
		return false;
    }

    @Override
    public boolean canExtractItem(int slot, ItemStack itemStack, int amount) {
        if(slot == 2 && itemStack.getItem() instanceof ItemFWatzCore core && !core.isBaby) return true;
        return slot == 5 || slot == 6;
    }
	
	@Override
	public boolean hasCapability(Capability<?> capability, EnumFacing facing) {
		return capability == CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY || super.hasCapability(capability, facing);
	}
	
	@Override
	public <T> T getCapability(Capability<T> capability, EnumFacing facing) {
		return capability == CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY ? CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY.cast(this) : super.getCapability(capability, facing);
	}
	
	public long getPower() {
		return power;
	}

	public void setPower(long i) {
		power = i;
	}

	public long getMaxPower() {
		return maxPower;
	}

    AxisAlignedBB bb = null;

    @Override
    public AxisAlignedBB getRenderBoundingBox() {
        if(bb == null) {
            bb = new AxisAlignedBB(pos.getX() + 0.5 - 8, pos.getY() + 0.5 - 3, pos.getZ() + 0.5 - 8, pos.getX() + 0.5 + 8, pos.getY() + 0.5 + 3, pos.getZ() + 0.5 + 8);
        }

        return bb;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public double getMaxRenderDistanceSquared() {
        return 65536.0D;
    }

    public static void doGravityStuff(World world, float range, float deathRadius, float posX, float posY, float posZ, double strength){
        List<Entity> entities = world.getEntitiesWithinAABBExcludingEntity(null, new AxisAlignedBB(posX - range, posY - range, posZ - range, posX + range, posY + range, posZ + range));

        for(Entity e : entities) {
            if(Library.isCreative(e))
                continue;

            Vec3 vec = Vec3.createVectorHelper(posX - e.posX, posY - e.posY, posZ - e.posZ);

            double dist = vec.length();

            if(dist > range)
                continue;

            vec = vec.normalize();

            if(!(e instanceof EntityItem))
                vec.rotateAroundY((float)Math.toRadians(15));
            double r2 = Math.max(dist * dist, 1);
            e.motionX += vec.xCoord * strength / r2;
            e.motionY += vec.yCoord * strength * 2 / r2;
            e.motionZ += vec.zCoord * strength / r2;

            if(e instanceof EntityBlackHole)
                continue;

            if(dist < deathRadius) {
                e.attackEntityFrom(ModDamageSource.blackhole, 1000);

                if(!(e instanceof EntityLivingBase))
                    e.setDead();
            }
        }
    }
}