package com.hbm.inventory.container;

import com.hbm.tileentity.machine.TileEntityCrateTungsten;
import net.minecraft.entity.player.InventoryPlayer;

public class ContainerCrateTungsten extends ContainerCrateBase<TileEntityCrateTungsten> {

	protected final TileEntityCrateTungsten crate;

	public ContainerCrateTungsten(InventoryPlayer invPlayer, TileEntityCrateTungsten te) {
		super(invPlayer, te,
				27, 9, 3,
				8, 18,
				86, 144
		);
		this.crate = te;
	}
}