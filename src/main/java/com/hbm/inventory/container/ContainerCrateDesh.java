package com.hbm.inventory.container;

import com.hbm.tileentity.machine.TileEntityCrateDesh;
import net.minecraft.entity.player.InventoryPlayer;

public class ContainerCrateDesh extends ContainerCrateBase<TileEntityCrateDesh> {

	protected final TileEntityCrateDesh crate;

	public ContainerCrateDesh(InventoryPlayer invPlayer, TileEntityCrateDesh te) {
		super(invPlayer, te,
				104, 13, 8,
				44, 18,
				174, 232
		);
		this.crate = te;
	}
}