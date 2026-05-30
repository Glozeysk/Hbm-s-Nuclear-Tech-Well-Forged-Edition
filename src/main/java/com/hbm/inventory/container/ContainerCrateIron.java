package com.hbm.inventory.container;

import com.hbm.tileentity.machine.TileEntityCrateIron;
import net.minecraft.entity.player.InventoryPlayer;

public class ContainerCrateIron extends ContainerCrateBase<TileEntityCrateIron> {

	protected final TileEntityCrateIron diFurnace;

	public ContainerCrateIron(InventoryPlayer invPlayer, TileEntityCrateIron te) {
		super(invPlayer, te,
				36, 9, 4,
				8, 18,
				104, 162
		);
		this.diFurnace = te;
	}
}