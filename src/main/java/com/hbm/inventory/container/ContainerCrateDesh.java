package com.hbm.inventory.container;

import com.hbm.tileentity.machine.TileEntityCrateDesh;
import invtweaks.api.container.ChestContainer;
import net.minecraft.entity.player.InventoryPlayer;

@ChestContainer(isLargeChest = true)
public class ContainerCrateDesh extends ContainerCrateBase<TileEntityCrateDesh> {

	protected final TileEntityCrateDesh crate;

	public ContainerCrateDesh(InventoryPlayer invPlayer, TileEntityCrateDesh te) {
		super(invPlayer, te,
				104,
				13,
				8,
				8,
				18,
				44,
				174,
				232
		);
		this.crate = te;
	}
}