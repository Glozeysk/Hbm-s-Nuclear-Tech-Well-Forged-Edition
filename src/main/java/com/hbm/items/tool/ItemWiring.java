package com.hbm.items.tool;

import java.util.List;

import com.hbm.util.I18nUtil;
import com.hbm.blocks.BlockDummyable;
import com.hbm.items.ModItems;
import com.hbm.tileentity.network.energy.TileEntityPylonBase;

import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.World;

public class ItemWiring extends Item {

	public ItemWiring(String s) {
		this.setTranslationKey(s);
		this.setRegistryName(s);

		this.setMaxDamage(100);
		this.setMaxStackSize(1);

		ModItems.ALL_ITEMS.add(this);
	}

	@Override
	public EnumActionResult onItemUse(EntityPlayer player, World world, BlockPos pos, EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ) {

		if (tryRepairWiring(player, world, hand)) {
			player.swingArm(hand);
			return EnumActionResult.SUCCESS;
		}

		Block b = world.getBlockState(pos).getBlock();
		BlockPos core = pos;
		if(b instanceof BlockDummyable) {
			int[] corePos = ((BlockDummyable)b).findCore(world, pos.getX(), pos.getY(), pos.getZ());

			if(corePos != null) {
				core = new BlockPos(corePos[0], corePos[1], corePos[2]);
			}
		}

		TileEntity te = world.getTileEntity(core);
		ItemStack stack = player.getHeldItem(hand);
		if (te != null && te instanceof TileEntityPylonBase) {
			if(player.isSneaking()) {
				if (!stack.hasTagCompound())
					stack.setTagCompound(new NBTTagCompound());

				stack.getTagCompound().setInteger("x", pos.getX());
				stack.getTagCompound().setInteger("y", pos.getY());
				stack.getTagCompound().setInteger("z", pos.getZ());

				if (world.isRemote)
					player.sendMessage(new TextComponentTranslation("chat.wiring.start", pos.getX(), pos.getY(), pos.getZ()));
			} else {
				if (stack.hasTagCompound()) {
					int x1 = stack.getTagCompound().getInteger("x");
					int y1 = stack.getTagCompound().getInteger("y");
					int z1 = stack.getTagCompound().getInteger("z");

					TileEntityPylonBase thisPylon = (TileEntityPylonBase)te;
					BlockPos newPos = new BlockPos(x1, y1, z1);
					if(!this.isLengthValid(pos.getX(), pos.getY(), pos.getZ(), x1, y1, z1, thisPylon.getMaxWireLength())){
						if (world.isRemote){
							BlockPos vector = newPos.subtract(pos);
							int distance = (int)MathHelper.sqrt(vector.getX() * vector.getX() + vector.getY() * vector.getY() + vector.getZ() * vector.getZ());
							player.sendMessage(new TextComponentTranslation("chat.wiring.tofar", distance, thisPylon.getMaxWireLength()));
						}
					} else if(pos.equals(newPos)){
						if (world.isRemote)
							player.sendMessage(new TextComponentTranslation("chat.wiring.noself"));
					} else{
						Block a = world.getBlockState(newPos).getBlock();
						BlockPos coreB = newPos;
						if(a instanceof BlockDummyable) {
							int[] corePosB = ((BlockDummyable)a).findCore(world, newPos.getX(), newPos.getY(), newPos.getZ());

							if(corePosB != null) {
								coreB = new BlockPos(corePosB[0], corePosB[1], corePosB[2]);
							}
						}
						TileEntity target = world.getTileEntity(coreB);
						if(target instanceof TileEntityPylonBase) {

							TileEntityPylonBase targetPylon = (TileEntityPylonBase) target;

							if(thisPylon.connected.contains(targetPylon.getPos()) || targetPylon.connected.contains(thisPylon.getPos())){
								if (world.isRemote)
									player.sendMessage(new TextComponentTranslation("chat.wiring.already"));
								return EnumActionResult.FAIL;
							}

							double dist = Math.sqrt(Math.pow(pos.getX() - x1, 2) + Math.pow(pos.getY() - y1, 2) + Math.pow(pos.getZ() - z1, 2));
							int requiredWire = (int) dist;
							int availableDurability = stack.getMaxDamage() - stack.getItemDamage();

							if (requiredWire > availableDurability) {
								if (world.isRemote)
									player.sendMessage(new TextComponentTranslation("chat.wiring.not_enough_wire"));
								return EnumActionResult.FAIL;
							}

							if(TileEntityPylonBase.canConnect(thisPylon, targetPylon)){
								thisPylon.addConnection(targetPylon.getPos());
								targetPylon.addConnection(thisPylon.getPos());

								if (!world.isRemote && !player.capabilities.isCreativeMode) {
									if (requiredWire > 0) {
										int newDamage = stack.getItemDamage() + requiredWire;
										if (newDamage >= stack.getMaxDamage()) {
											player.setHeldItem(hand, new ItemStack(ModItems.cable_drum));
										} else {
											stack.setItemDamage(newDamage);
										}
									}
								}

								if (world.isRemote)
									player.sendMessage(new TextComponentTranslation("chat.wiring.connected"));
							}else{
								if(thisPylon.getConnectionType() != targetPylon.getConnectionType()){
									if (world.isRemote)
										player.sendMessage(new TextComponentTranslation("chat.wiring.notcompatible"));
								}
							}
						}
					}
				}
			}
		} else {
			if(player.isSneaking()){
				if(stack.hasTagCompound()) {
					stack.setTagCompound(null);
					if (world.isRemote)
						player.sendMessage(new TextComponentTranslation("chat.wiring.cleared"));
				}
			} else {
				if(stack.hasTagCompound() && world.isRemote) {
					int x1 = stack.getTagCompound().getInteger("x");
					int y1 = stack.getTagCompound().getInteger("y");
					int z1 = stack.getTagCompound().getInteger("z");

					BlockPos vector = new BlockPos(x1, y1, z1).subtract(pos);
					int distance = (int)MathHelper.sqrt(vector.getX() * vector.getX() + vector.getY() * vector.getY() + vector.getZ() * vector.getZ());

					player.sendMessage(new TextComponentTranslation("chat.wiring.measure", distance));
				}
			}
		}
		player.swingArm(hand);
		return EnumActionResult.SUCCESS;
	}

	private boolean tryRepairWiring(EntityPlayer player, World world, EnumHand hand) {
		ItemStack mainStack = player.getHeldItemMainhand();
		ItemStack offStack = player.getHeldItemOffhand();
		ItemStack drum = null;
		ItemStack wire = null;
		EnumHand drumHand = null;

		if (mainStack.getItem() instanceof ItemWiring && offStack.getItem() == ModItems.wire_red_copper) {
			drum = mainStack; wire = offStack; drumHand = EnumHand.MAIN_HAND;
		} else if (offStack.getItem() instanceof ItemWiring && mainStack.getItem() == ModItems.wire_red_copper) {
			drum = offStack; wire = mainStack; drumHand = EnumHand.OFF_HAND;
		}

		if (drum != null && wire != null && drum.getItemDamage() > 0) {
			if (!world.isRemote) {
				wire.shrink(1);
				int newDamage = Math.max(0, drum.getItemDamage() - 5);
				ItemStack repaired = drum.copy();
				repaired.setItemDamage(newDamage);
				player.setHeldItem(drumHand, repaired);
			}
			return true;
		}
		return false;
	}

	@Override
	public ActionResult<ItemStack> onItemRightClick(World worldIn, EntityPlayer playerIn, EnumHand handIn) {
		if (tryRepairWiring(playerIn, worldIn, handIn)) {
			return new ActionResult<>(EnumActionResult.SUCCESS, playerIn.getHeldItem(handIn));
		}
		return new ActionResult<>(EnumActionResult.PASS, playerIn.getHeldItem(handIn));
	}

	@Override
	public void addInformation(ItemStack stack, World worldIn, List<String> tooltip, ITooltipFlag flagIn) {
		if (stack.getTagCompound() != null) {
			int x1 = stack.getTagCompound().getInteger("x");
			int y1 = stack.getTagCompound().getInteger("y");
			int z1 = stack.getTagCompound().getInteger("z");
			tooltip.add(I18nUtil.resolveKey("desc.wiring.start", x1, y1, z1));
		} else {
			tooltip.add(I18nUtil.resolveKey("desc.wiring.1"));
			tooltip.add(I18nUtil.resolveKey("desc.wiring.2"));
			tooltip.add(I18nUtil.resolveKey("desc.wiring.3"));
			tooltip.add(I18nUtil.resolveKey("desc.wiring.4"));
		}
	}

	public boolean isLengthValid(int x1, int y1, int z1, int x2, int y2, int z2, int length) {
		double l = Math.sqrt(Math.pow(x2 - x1, 2) + Math.pow(y2 - y1, 2) + Math.pow(z2 - z1, 2));
		return l <= length;
	}

	@Override
	public boolean isEnchantable(ItemStack stack) {
		return false;
	}

	@Override
	public int getItemEnchantability() {
		return 0;
	}
}