package com.hbm.tileentity.machine;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;

public class TileEntityCrateDesh extends TileEntityCrateBase {
	public TileEntityCrateDesh() { super(104); }
	@Override protected String getDefaultInventoryName() { return "container.crateDesh"; }
	public static TileEntityCrateDesh fromItemStack(ItemStack stack, EntityPlayer player) {
		TileEntityCrateDesh te = new TileEntityCrateDesh();
		te.initFromStack(stack, player);
		return te;
	}
}