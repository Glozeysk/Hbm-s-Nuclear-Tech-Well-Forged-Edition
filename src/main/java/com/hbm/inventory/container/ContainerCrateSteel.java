package com.hbm.inventory.container;

import com.hbm.tileentity.machine.TileEntityCrateSteel;
import net.minecraft.entity.player.InventoryPlayer;

public class ContainerCrateSteel extends ContainerCrateBase<TileEntityCrateSteel> {

	protected final TileEntityCrateSteel diFurnace;

	public ContainerCrateSteel(InventoryPlayer invPlayer, TileEntityCrateSteel te) {
		super(invPlayer, te,
				54, 9, 6,
				8, 18,
				140, 198
		);
		this.diFurnace = te;
	}
}