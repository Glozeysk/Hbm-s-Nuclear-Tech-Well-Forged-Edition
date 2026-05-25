package com.hbm.tileentity.machine;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;

public class TileEntityCrateIron extends TileEntityCrateBase {
	public TileEntityCrateIron() { super(36); }
	@Override protected String getDefaultInventoryName() { return "container.crateIron"; }
	public static TileEntityCrateIron fromItemStack(ItemStack stack, EntityPlayer player) {
		TileEntityCrateIron te = new TileEntityCrateIron();
		te.initFromStack(stack, player);
		return te;
	}
}