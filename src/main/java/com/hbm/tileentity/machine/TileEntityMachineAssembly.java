package com.hbm.tileentity.machine;

import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import com.hbm.blocks.BlockDummyable;
import com.hbm.blocks.ModBlocks;
import com.hbm.handler.MultiblockHandler;
import com.hbm.inventory.AssemblerRecipes;
import com.hbm.inventory.RecipesCommon.AStack;
import com.hbm.inventory.RecipesCommon.ComparableStack;
import com.hbm.items.ModItems;
import com.hbm.items.machine.ItemAssemblyTemplate;
import com.hbm.lib.ForgeDirection;
import com.hbm.lib.HBMSoundHandler;
import com.hbm.lib.Library;
import com.hbm.main.MainRegistry;
import com.hbm.sound.AudioWrapper;
import com.hbm.tileentity.IBufPacketReceiver;
import com.hbm.tileentity.TileEntityMachineBase;
import com.hbm.util.BobMathUtil;

import api.hbm.energy.IEnergyUser;
import io.netty.buffer.ByteBuf;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ITickable;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.IItemHandlerModifiable;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.oredict.OreDictionary;

public class TileEntityMachineAssembly extends TileEntityMachineBase implements ITickable, IEnergyUser, IBufPacketReceiver {

	public long power;
	public static final long maxPower = 2000000;
	public int progress;
	public boolean needsProcess = true;
	public int maxProgress = 100;
	public boolean isProgressing;
	int age = 0;
	int consumption = 100;
	int speed = 100;
	public boolean frame = false;
	public AssemblerArm[] arms = new AssemblerArm[] { new AssemblerArm(), new AssemblerArm() };
    public double prevRing;
    public double ring;
    public double ringSpeed;
    public double ringTarget;
    public int ringDelay;

	@SideOnly(Side.CLIENT)
	public int recipe;

	private AudioWrapper audio;
	
	public TileEntityMachineAssembly() {

		super(18);
		inventory = new ItemStackHandler(18){
			@Override
			protected void onContentsChanged(int slot){
				markDirty();
				OnContentsChanged(slot);
				super.onContentsChanged(slot);
			}
			@Override
			public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
				if(slot != 4)
					return super.insertItem(slot, stack, simulate);
				return stack;
			}

			@Override
			public ItemStack extractItem(int slot, int amount, boolean simulate) {
				if(slot != 4)
					return super.extractItem(slot, amount, simulate);
				return ItemStack.EMPTY;
			}
		};
	}

	public void OnContentsChanged(int slot){
		this.needsProcess = true;
	}

	@Override
	public boolean canInsertItem(int slot, ItemStack itemStack, int amount) {
		if(slot < 6 || slot > 17) {
			return false;
		}

		if(itemStack.isEmpty()) {
			return false;
		}

		ItemStack templateStack = inventory.getStackInSlot(4);
		if(templateStack.isEmpty()) {
			return false;
		}

		List<AStack> recipe = AssemblerRecipes.getRecipeFromTempate(templateStack);
		if(recipe == null) {
			return false;
		}

		ItemStack output = AssemblerRecipes.getOutputFromTempate(templateStack);
		if(output == null || output.isEmpty()) {
			return false;
		}

		ItemStack compareStack = itemStack.copy();
		compareStack.setCount(1);

		for(AStack ingredient : recipe) {
			AStack sing = ingredient.copy();
			sing.singulize();

			if(sing.isApplicable(compareStack)) {
				int validSlot = getValidSlot(ingredient.copy());
				if(validSlot == slot) {
					return true;
				}
			}
		}

		return false;
	}
	
	@Override
	public boolean canExtractItem(int slot, ItemStack itemStack, int amount){
		if(slot == 5) {
			return true;
		}
		return false;
	}

	@Override
	public int[] getAccessibleSlotsFromSide(EnumFacing face) {
		return new int[] { 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17 };
	}

	@Override
	public String getName() {
		return "container.assembly";
	}

	@Override
	public void readFromNBT(NBTTagCompound nbt) {
		super.readFromNBT(nbt);
		this.power = nbt.getLong("powerTime");
		this.isProgressing = nbt.getBoolean("progressing");
		this.progress = nbt.getInteger("progress");
	}

	@Override
	public NBTTagCompound writeToNBT(NBTTagCompound nbt) {
		super.writeToNBT(nbt);
		nbt.setBoolean("progressing", this.isProgressing);
		nbt.setLong("powerTime", power);
		nbt.setInteger("progress", progress);
		return nbt;
	}

	public long getPowerScaled(long i) {
		return (power * i) / maxPower;
	}

	public int getProgressScaled(int i) {
		return (progress * i) / Math.max(10, maxProgress);
	}

	@Override
	public void update() {
		if(!world.isRemote) {

			this.updateConnections();

			this.consumption = 100;
			this.speed = 100;

			double c = 100;
			double s = 100;

			for(int i = 1; i < 4; i++) {
				ItemStack stack = inventory.getStackInSlot(i);

				if(!stack.isEmpty()) {
					if(stack.getItem() == ModItems.upgrade_speed_1) {
						s *= 0.75;
						c *= 3;
					}
					if(stack.getItem() == ModItems.upgrade_speed_2) {
						s *= 0.65;
						c *= 6;
					}
					if(stack.getItem() == ModItems.upgrade_speed_3) {
						s *= 0.5;
						c *= 9;
					}
					if(stack.getItem() == ModItems.upgrade_power_1) {
						c *= 0.8;
						s *= 1.25;
					}
					if(stack.getItem() == ModItems.upgrade_power_2) {
						c *= 0.4;
						s *= 1.5;
					}
					if(stack.getItem() == ModItems.upgrade_power_3) {
						c *= 0.2;
						s *= 2;
					}
				}
			}
		this.speed = (int) s;
		this.consumption = (int) c;

		if(speed < 2)
			speed = 2;
		if(consumption < 2)
			consumption = 2;
			isProgressing = false;
			power = Library.chargeTEFromItems(inventory, 0, power, maxPower);
			if(needsProcess && (AssemblerRecipes.getOutputFromTempate(inventory.getStackInSlot(4)) != ItemStack.EMPTY && AssemblerRecipes.getRecipeFromTempate(inventory.getStackInSlot(4)) != null)) {
				this.maxProgress = (ItemAssemblyTemplate.getProcessTime(inventory.getStackInSlot(4)) * speed) / 100;
				if(removeItems(AssemblerRecipes.getRecipeFromTempate(inventory.getStackInSlot(4)), cloneItemStackProper(inventory))) {
					if(power >= consumption ){
					if(inventory.getStackInSlot(5).isEmpty() || (!inventory.getStackInSlot(5).isEmpty() && inventory.getStackInSlot(5).getItem() == AssemblerRecipes.getOutputFromTempate(inventory.getStackInSlot(4)).copy().getItem()) && inventory.getStackInSlot(5).getCount() + AssemblerRecipes.getOutputFromTempate(inventory.getStackInSlot(4)).copy().getCount() <= inventory.getStackInSlot(5).getMaxStackSize()) {
						progress++;
						isProgressing = true;

						if(progress >= maxProgress) {
							progress = 0;
							if(inventory.getStackInSlot(5).isEmpty()) {
								inventory.setStackInSlot(5, AssemblerRecipes.getOutputFromTempate(inventory.getStackInSlot(4)).copy());
							} else {
								inventory.getStackInSlot(5).grow(AssemblerRecipes.getOutputFromTempate(inventory.getStackInSlot(4)).copy().getCount());
							}

							removeItems(AssemblerRecipes.getRecipeFromTempate(inventory.getStackInSlot(4)), inventory);
							if(inventory.getStackInSlot(0).getItem() == ModItems.meteorite_sword_alloyed)
								inventory.setStackInSlot(0, new ItemStack(ModItems.meteorite_sword_machined));
						}

						power -= consumption;
					}}
				} else{
					progress = 0;
					needsProcess = false;
				}
			} else{
				progress = 0;
			}

			networkPackNT(150);
		} else {

			if(world.getTotalWorldTime() % 20 == 0) {
				frame = world.getBlockState(pos.up(3)).getBlock() != Blocks.AIR;
			}

			for(AssemblerArm arm : arms) {
				arm.updateInterp();
				if(isProgressing) {
					arm.updateArm();
				} else {
					arm.returnToNullPos();
				}

				if(arm.prevAngles[3] != arm.angles[3] && arm.angles[3] == -0.75) {
					world.playSound(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, HBMSoundHandler.assemblerStrike, SoundCategory.BLOCKS, this.getVolume(0.5F), 1F, false);
				}
			}

			this.prevRing = this.ring;

			if(isProgressing) {
				if(this.ring != this.ringTarget) {
					double ringDelta = Math.abs(this.ringTarget - this.ring);
					if(ringDelta <= this.ringSpeed) this.ring = this.ringTarget;
					if(this.ringTarget > this.ring) this.ring += this.ringSpeed;
					if(this.ringTarget < this.ring) this.ring -= this.ringSpeed;
					if(this.ringTarget == this.ring) {
						if(ringTarget >= 360) {
							this.ringTarget -= 360D;
							this.ring -= 360D;
							this.prevRing -= 360D;
						}
						if(ringTarget <= -360) {
							this.ringTarget += 360D;
							this.ring += 360D;
							this.prevRing += 360D;
						}
						this.ringDelay = 20 + world.rand.nextInt(21);
					}
				} else {
					if(this.ringDelay > 0) this.ringDelay--;
					if(this.ringDelay <= 0) {
						this.ringTarget += (world.rand.nextDouble() * 2 - 1) * 135;
						this.ringSpeed = 10D + world.rand.nextDouble() * 5D;
					}
				}
			}

			float volume = this.getVolume(2);

			if(isProgressing && volume > 0) {
				if(audio == null) {
					audio = MainRegistry.proxy.getLoopedSoundStartStop(world, HBMSoundHandler.motor, HBMSoundHandler.assemblerStart, HBMSoundHandler.assemblerStop, SoundCategory.BLOCKS, pos.getX(), pos.getY(), pos.getZ(), volume, 1.0F);
					if(audio != null) {
						audio.startSound();
					}
				} else {
					audio.keepAlive();
					audio.updateVolume(volume);
				}
			} else {
				if(audio != null) {
					audio.stopSound();
					audio = null;
				}
			}

		}
	}

	private void updateConnections() {

		ForgeDirection dir = ForgeDirection.getOrientation(this.getBlockMetadata() - BlockDummyable.offset);

		this.trySubscribe(world, pos.add(-1, 0, -2), ForgeDirection.NORTH);
		this.trySubscribe(world, pos.add(0, 0, -2), ForgeDirection.NORTH);
		this.trySubscribe(world, pos.add(1, 0, -2), ForgeDirection.NORTH);
		this.trySubscribe(world, pos.add(-1, 0, 2), ForgeDirection.SOUTH);
		this.trySubscribe(world, pos.add(0, 0, 2), ForgeDirection.SOUTH);
		this.trySubscribe(world, pos.add(1, 0, 2), ForgeDirection.SOUTH);
		this.trySubscribe(world, pos.add(-2, 0, -1), ForgeDirection.WEST);
		this.trySubscribe(world, pos.add(-2, 0, 0), ForgeDirection.WEST);
		this.trySubscribe(world, pos.add(-2, 0, 1), ForgeDirection.WEST);
		this.trySubscribe(world, pos.add(2, 0, -1), ForgeDirection.EAST);
		this.trySubscribe(world, pos.add(2, 0, 0), ForgeDirection.EAST);
		this.trySubscribe(world, pos.add(2, 0, 1), ForgeDirection.EAST);
		
	}

	@Override
	public void onChunkUnload() {
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
		buf.writeInt(this.progress);
		buf.writeInt(this.maxProgress);
		buf.writeBoolean(this.isProgressing);
		buf.writeInt(!inventory.getStackInSlot(4).isEmpty() ? ItemAssemblyTemplate.getRecipeIndex(inventory.getStackInSlot(4)) : -1);
	}

	@Override
	public void deserialize(ByteBuf buf) {
		this.power = buf.readLong();
		this.progress = buf.readInt();
		this.maxProgress = buf.readInt();
		this.isProgressing = buf.readBoolean();
		this.recipe = buf.readInt();
	}

	public ItemStackHandler cloneItemStackProper(IItemHandlerModifiable array) {
		ItemStackHandler stack = new ItemStackHandler(array.getSlots());

		for(int i = 0; i < array.getSlots(); i++)
			if(array.getStackInSlot(i).getItem() != Items.AIR)
				stack.setStackInSlot(i, array.getStackInSlot(i).copy());
			else
				stack.setStackInSlot(i, ItemStack.EMPTY);
		;

		return stack;
	}

	private int getValidSlot(AStack nextIngredient){
		int firstFreeSlot = -1;
		float maxStackSize = nextIngredient.getStack().getMaxStackSize();
		int stackCount = (int)Math.ceil(nextIngredient.count() / maxStackSize);
		int stacksFound = 0;

		nextIngredient = nextIngredient.singulize();
		
		for(int k = 6; k < 18; k++) {
			if(stacksFound < stackCount){
				ItemStack assStack = inventory.getStackInSlot(k).copy();
				if(assStack.isEmpty()){
					if(firstFreeSlot < 6){
						firstFreeSlot = k;
					}
				} else {
				
					assStack.setCount(1);
					if(nextIngredient.isApplicable(assStack)){
						if(inventory.getStackInSlot(k).getCount() < assStack.getMaxStackSize()) {
							return k;
						}
						else
							stacksFound++;
					}
				}
			}else {
				return -1;
			}
		}
		if(firstFreeSlot < 6)
			return -2;
		return firstFreeSlot;
	}

	public boolean removeItems(List<AStack> stack, IItemHandlerModifiable array) {
		if(stack == null)
			return false;

		for(int i = 0; i < stack.size(); i++) {
			for(int j = 0; j < stack.get(i).count(); j++) {
				AStack sta = stack.get(i).copy();
				sta.singulize();
				if(!canRemoveItemFromArray(sta, array)){
					return false;
				}
			}
		}

		return true;

	}

	public boolean canRemoveItemFromArray(AStack stack, IItemHandlerModifiable array) {
		AStack st = stack.copy();

		if(st == null)
			return true;

		for(int i = 6; i < 18; i++) {

			if(!array.getStackInSlot(i).isEmpty()) {

				ItemStack sta = array.getStackInSlot(i).copy();
				sta.setCount(1);

				if(st.isApplicable(sta) && array.getStackInSlot(i).getCount() > 0) {
					array.getStackInSlot(i).shrink(1);

					if(array.getStackInSlot(i).isEmpty())
						array.setStackInSlot(i, ItemStack.EMPTY);

					return true;
				}
			}
		}

		return false;
	}

	public boolean isItemAcceptable(ItemStack stack1, ItemStack stack2) {

		if(stack1 != null && stack2 != null && !stack1.isEmpty() && !stack2.isEmpty()) {
			if(Library.areItemStacksCompatible(stack1, stack2))
				return true;

			int[] ids1 = OreDictionary.getOreIDs(stack1);
			int[] ids2 = OreDictionary.getOreIDs(stack2);

			if(ids1.length > 0 && ids2.length > 0) {
				for(int i = 0; i < ids1.length; i++)
					for(int j = 0; j < ids2.length; j++)
						if(ids1[i] == ids2[j])
							return true;
			}
		}

		return false;
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
	public AxisAlignedBB getRenderBoundingBox() {
		return new AxisAlignedBB(pos.getX(), pos.getY(), pos.getZ(), pos.getX() + 1, pos.getY() + 1, pos.getZ() + 1).grow(2, 1, 2).grow(10);
	}

	@Override
	@SideOnly(Side.CLIENT)
	public double getMaxRenderDistanceSquared() {
		return 65536.0D;
	}
	
	@Override
	public int countMufflers() {

		int count = 0;

		for(int x = pos.getX() - 1; x <= pos.getX() + 1; x++)
			for(int z = pos.getZ() - 1; z <= pos.getZ() + 1; z++)
				if(world.getBlockState(new BlockPos(x, pos.getY() - 1, z)).getBlock() == ModBlocks.muffler)
					count++;

		return count;
	}

	public static class AssemblerArm {

        public double[] angles = new double[4];
        public double[] prevAngles = new double[4];
        public double[] targetAngles = new double[4];
        public double[] speed = new double[4];

        Random rand = new Random();
        ArmActionState state = ArmActionState.ASSUME_POSITION;
        int actionDelay = 0;

        public static enum ArmActionState {
            ASSUME_POSITION,
            EXTEND_STRIKER,
            RETRACT_STRIKER
        }

        public AssemblerArm() {
            this.resetSpeed();
        }

        private void updateInterp() {
            for(int i = 0; i < angles.length; i++) {
                prevAngles[i] = angles[i];
            }
        }

        private void returnToNullPos() {
            for(int i = 0; i < 4; i++) this.targetAngles[i] = 0;
            for(int i = 0; i < 3; i++) this.speed[i] = 3;
            this.speed[3] = 0.25;
            this.state = ArmActionState.RETRACT_STRIKER;

            this.move();
        }

        private void resetSpeed() {
            speed[0] = 15;	//Pivot
            speed[1] = 15;	//Arm
            speed[2] = 15;	//Piston
            speed[3] = 0.5;	//Striker
        }

        public void updateArm() {
            resetSpeed();

            if(actionDelay > 0) {
                actionDelay--;
                return;
            }

            switch(state) {
                // Move. If done moving, set a delay and progress to EXTEND
                case ASSUME_POSITION:
                    if(move()) {
                        actionDelay = 2;
                        state = ArmActionState.EXTEND_STRIKER;
                        targetAngles[3] = -0.75D;
                    }
                    break;
                case EXTEND_STRIKER:
                    if(move()) {
                        state = ArmActionState.RETRACT_STRIKER;
                        targetAngles[3] = 0D;
                    }
                    break;
                case RETRACT_STRIKER:
                    if(move()) {
                        actionDelay = 2 + rand.nextInt(5);
                        chooseNewArmPoistion();
                        state = ArmActionState.ASSUME_POSITION;
                    }
                    break;

            }
        }

        private double[][] pos = new double[][] { // possible positions for the arms
                {45, -15, -5},
                {15, 15, -15},
                {25, 10, -15},
                {30, 0, -10},
                {70, -10, -25},
        }; // sure it's not truly random like with the old assemfac, but at least now the striker always hits the center and doesn't clip through the board

        public void chooseNewArmPoistion() {
            int chosen = rand.nextInt(pos.length);
            this.targetAngles[0] = pos[chosen][0];
            this.targetAngles[1] = pos[chosen][1];
            this.targetAngles[2] = pos[chosen][2];
        }

        private boolean move() {
            boolean didMove = false;

            for(int i = 0; i < angles.length; i++) {
                if(angles[i] == targetAngles[i])
                    continue;

                didMove = true;

                double angle = angles[i];
                double target = targetAngles[i];
                double turn = speed[i];
                double delta = Math.abs(angle - target);

                if(delta <= turn) {
                    angles[i] = targetAngles[i];
                    continue;
                }

                if(angle < target) {
                    angles[i] += turn;
                } else {
                    angles[i] -= turn;
                }
            }

            return !didMove;
        }

        public double[] getPositions(float interp) {
            return new double[] {
                    BobMathUtil.interp(this.prevAngles[0], this.angles[0], interp),
                    BobMathUtil.interp(this.prevAngles[1], this.angles[1], interp),
                    BobMathUtil.interp(this.prevAngles[2], this.angles[2], interp),
                    BobMathUtil.interp(this.prevAngles[3], this.angles[3], interp)
            };
        }
    }
}