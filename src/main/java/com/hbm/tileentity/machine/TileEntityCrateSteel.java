package com.hbm.tileentity.machine;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;

public class TileEntityCrateSteel extends TileEntityCrateBase {
	public TileEntityCrateSteel() { super(54); }
	@Override protected String getDefaultInventoryName() { return "container.crateSteel"; }
	public static TileEntityCrateSteel fromItemStack(ItemStack stack, EntityPlayer player) {
		TileEntityCrateSteel te = new TileEntityCrateSteel();
		te.initFromStack(stack, player);
		return te;
	}
}