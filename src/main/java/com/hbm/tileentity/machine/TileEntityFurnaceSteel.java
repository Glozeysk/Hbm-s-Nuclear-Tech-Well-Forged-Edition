package com.hbm.tileentity.machine;

import java.util.List;

import com.hbm.inventory.container.ContainerFurnaceSteel;
import com.hbm.inventory.gui.GUIFurnaceSteel;
import com.hbm.lib.ForgeDirection;
import com.hbm.tileentity.IBufPacketReceiver;
import com.hbm.tileentity.IGUIProvider;
import com.hbm.tileentity.TileEntityMachineBase;
import com.hbm.util.ItemStackUtil;

import api.hbm.tile.IHeatSource;
import io.netty.buffer.ByteBuf;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.FurnaceRecipes;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.ITickable;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public class TileEntityFurnaceSteel extends TileEntityMachineBase implements IGUIProvider, ITickable, IBufPacketReceiver {

	public int[] progress = new int[3];
	public int[] bonus = new int[3];
	public static final int processTime = 40_000;

	public int heat;
	public static final int maxHeat = 100_000;
	public static final double diffusion = 0.05D;
	private ItemStack[] lastItems = new ItemStack[] { ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY };

	public boolean wasOn = false;

	public TileEntityFurnaceSteel() {
		super(6);
	}

	@Override
	public String getName() {
		return "container.furnaceSteel";
	}

	@Override
	public void update() {

		if(!world.isRemote) {
			tryPullHeat();

			this.wasOn = false;

			int burn = (heat - TileEntityFurnaceSteel.maxHeat / 3) / 10;

			for(int i = 0; i < 3; i++) {
				ItemStack input = inventory.getStackInSlot(i);
				ItemStack last = lastItems[i];

				if(input.isEmpty() || last.isEmpty() || !input.isItemEqual(last)) {
					progress[i] = 0;
					bonus[i] = 0;
				}

				if(canSmelt(i)) {
					progress[i] += burn;
					this.heat -= burn;
					this.wasOn = true;
				}


				lastItems[i] = input.isEmpty() ? ItemStack.EMPTY : input.copy();

				if(progress[i] >= processTime) {
					ItemStack outputs = inventory.getStackInSlot(i + 3);
					ItemStack result = FurnaceRecipes.instance().getSmeltingResult(inventory.getStackInSlot(i));

					if(result == null || result.isEmpty())
						continue;

					if(outputs.isEmpty()) {
						inventory.setStackInSlot(i + 3, result.copy());
					} else {
						outputs.setCount(Math.min(outputs.getMaxStackSize(), outputs.getCount() + result.getCount()));
					}

					this.addBonus(inventory.getStackInSlot(i), i);

					outputs = inventory.getStackInSlot(i + 3);
					while(bonus[i] >= 100) {
						outputs.setCount(Math.min(outputs.getMaxStackSize(), outputs.getCount() + result.getCount()));
						bonus[i] -= 100;
					}

					input.shrink(1);

					progress[i] = 0;
				}
			}

			networkPackNT(50);
		} else {

			if(this.wasOn) {
				ForgeDirection dir = ForgeDirection.getOrientation(this.getBlockMetadata() - 10);
				ForgeDirection rot = dir.getRotation(ForgeDirection.UP);

				world.spawnParticle(EnumParticleTypes.SMOKE_NORMAL, pos.getX() + 0.5 - dir.offsetX * 1.125 - rot.offsetX * 0.75, pos.getY() + 2.625, pos.getZ() + 0.5 - dir.offsetZ * 1.125 - rot.offsetZ * 0.75, 0.0, 0.05, 0.0);

				if(world.rand.nextInt(20) == 0)
					world.spawnParticle(EnumParticleTypes.CLOUD, pos.getX() + 0.5 + dir.offsetX * 0.75, pos.getY() + 2, pos.getZ() + 0.5 + dir.offsetZ * 0.75, 0.0, 0.05, 0.0);

				if(world.rand.nextInt(15) == 0)
					world.spawnParticle(EnumParticleTypes.LAVA, pos.getX() + 0.5 + dir.offsetX * 1.5 + rot.offsetX * (world.rand.nextDouble() - 0.5), pos.getY() + 0.75, pos.getZ() + 0.5 + dir.offsetZ * 1.5 + rot.offsetZ * (world.rand.nextDouble() - 0.5), dir.offsetX * 0.5D, 0.05, dir.offsetZ * 0.5D);
			}
		}
	}

	@Override
	public void serialize(ByteBuf buf) {
		for(int i = 0; i < 3; i++) buf.writeInt(this.progress[i]);
		for(int i = 0; i < 3; i++) buf.writeInt(this.bonus[i]);
		buf.writeInt(this.heat);
		buf.writeBoolean(this.wasOn);
	}

	@Override
	public void deserialize(ByteBuf buf) {
		if(this.progress == null || this.progress.length < 3) this.progress = new int[3];
		if(this.bonus == null || this.bonus.length < 3) this.bonus = new int[3];

		for(int i = 0; i < 3; i++) this.progress[i] = buf.readInt();
		for(int i = 0; i < 3; i++) this.bonus[i] = buf.readInt();
		this.heat = buf.readInt();
		this.wasOn = buf.readBoolean();
	}

	@Override
	public void readFromNBT(NBTTagCompound nbt) {
		super.readFromNBT(nbt);

		int[] savedProgress = nbt.getIntArray("progress");
		int[] savedBonus = nbt.getIntArray("bonus");

		this.progress = new int[3];
		this.bonus = new int[3];

		for(int i = 0; i < Math.min(savedProgress.length, 3); i++) {
			this.progress[i] = savedProgress[i];
		}
		for(int i = 0; i < Math.min(savedBonus.length, 3); i++) {
			this.bonus[i] = savedBonus[i];
		}

		this.heat = nbt.getInteger("heat");

		this.lastItems = new ItemStack[] { ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY };

		NBTTagList list = nbt.getTagList("lastItems", 10);
		for(int i = 0; i < list.tagCount(); i++) {
			NBTTagCompound nbt1 = list.getCompoundTagAt(i);
			byte b0 = nbt1.getByte("lastItem");
			if(b0 >= 0 && b0 < lastItems.length) {
				ItemStack stack = new ItemStack(nbt1);
				lastItems[b0] = stack.isEmpty() ? ItemStack.EMPTY : stack;
			}
		}
	}

	@Override
	public NBTTagCompound writeToNBT(NBTTagCompound nbt) {
		super.writeToNBT(nbt);

		nbt.setIntArray("progress", progress);
		nbt.setIntArray("bonus", bonus);
		nbt.setInteger("heat", heat);

		NBTTagList list = new NBTTagList();
		for(int i = 0; i < lastItems.length; i++) {
			ItemStack stack = lastItems[i];
			if(stack != null && !stack.isEmpty()) {
				NBTTagCompound nbt1 = new NBTTagCompound();
				nbt1.setByte("lastItem", (byte) i);
				stack.writeToNBT(nbt1);
				list.appendTag(nbt1);
			}
		}
		nbt.setTag("lastItems", list);

		return nbt;
	}

	protected void addBonus(ItemStack stack, int index) {

		List<String> names = ItemStackUtil.getOreDictNames(stack);

		for(String name : names) {
			if(name.startsWith("ore")) { this.bonus[index] += 25; return; }
			if(name.startsWith("log")) { this.bonus[index] += 50; return; }
			if(name.equals("anyTar")) { this.bonus[index] += 50; return; }
		}
	}

	protected void tryPullHeat() {

		if(this.heat >= TileEntityFurnaceSteel.maxHeat) return;
		BlockPos blockBelow = pos.down();
		TileEntity con = world.getTileEntity(blockBelow);

		if(con instanceof IHeatSource) {
			IHeatSource source = (IHeatSource) con;
			int diff = source.getHeatStored() - this.heat;

			if(diff == 0) {
				return;
			}

			if(diff > 0) {
				diff = (int) Math.ceil(diff * diffusion);
				source.useUpHeat(diff);
				this.heat += diff;
				if(this.heat > TileEntityFurnaceSteel.maxHeat)
					this.heat = TileEntityFurnaceSteel.maxHeat;
				return;
			}
		}

		this.heat = Math.max(this.heat - Math.max(this.heat / 1000, 1), 0);
	}

	public boolean canSmelt(int index) {

		if(this.heat < TileEntityFurnaceSteel.maxHeat / 3) return false;
		if(inventory.getStackInSlot(index).isEmpty()) {
			return false;
		}
		ItemStack itemStack = FurnaceRecipes.instance().getSmeltingResult(inventory.getStackInSlot(index));

		if(itemStack == null || itemStack.isEmpty()) {
			return false;
		}

		if(inventory.getStackInSlot(index + 3).isEmpty()) {
			return true;
		}

		if(!inventory.getStackInSlot(index + 3).isItemEqual(itemStack)) {
			return false;
		}

		if(inventory.getStackInSlot(index + 3).getCount() < inventory.getSlotLimit(index + 3) && inventory.getStackInSlot(index + 3).getCount() < inventory.getStackInSlot(index + 3).getMaxStackSize()) {
			return true;
		} else {
			return inventory.getStackInSlot(index + 3).getCount() < itemStack.getMaxStackSize();
		}
	}

	@Override
	public int[] getAccessibleSlotsFromSide(EnumFacing e) {
		return new int[]{ 0, 1, 2, 3, 4, 5 };
	}

	@Override
	public boolean isItemValidForSlot(int i, ItemStack itemStack) {

		if(i < 3)
			return FurnaceRecipes.instance().getSmeltingResult(itemStack) != null;

		return false;
	}

	@Override
	public boolean canExtractItem(int i, ItemStack itemStack, int j) {
		return i > 2;
	}

	@Override
	public Container provideContainer(int ID, EntityPlayer player, World world, int x, int y, int z) {
		return new ContainerFurnaceSteel(player.inventory, this);
	}

	@Override
	@SideOnly(Side.CLIENT)
	public GuiScreen provideGUI(int ID, EntityPlayer player, World world, int x, int y, int z) {
		return new GUIFurnaceSteel(player.inventory, this);
	}

	@Override
	public AxisAlignedBB getRenderBoundingBox() {
		return INFINITE_EXTENT_AABB;
	}

	@Override
	@SideOnly(Side.CLIENT)
	public double getMaxRenderDistanceSquared() {
		return 65536.0D;
	}
}