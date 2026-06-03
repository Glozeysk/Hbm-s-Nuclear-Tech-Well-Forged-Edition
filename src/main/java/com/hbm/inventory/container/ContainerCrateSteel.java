package com.hbm.inventory.container;

import com.hbm.tileentity.machine.TileEntityCrateSteel;
import invtweaks.api.container.ChestContainer;
import net.minecraft.entity.player.InventoryPlayer;

@ChestContainer(rowSize = 9)
public class ContainerCrateSteel extends ContainerCrateBase<TileEntityCrateSteel> {

	protected final TileEntityCrateSteel diFurnace;

	public ContainerCrateSteel(InventoryPlayer invPlayer, TileEntityCrateSteel te) {
		super(invPlayer, te, 54, 9, 6, 8, 18, 8, 140, 198); this.diFurnace = te;
	}
}