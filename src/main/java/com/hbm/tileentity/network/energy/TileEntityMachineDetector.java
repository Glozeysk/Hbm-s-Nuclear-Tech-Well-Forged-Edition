package com.hbm.tileentity.network.energy;

import com.hbm.blocks.network.energy.PowerDetector;
import com.hbm.tileentity.TileEntityLoadedBase;

import api.hbm.energy.IEnergyUser;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ITickable;

public class TileEntityMachineDetector extends TileEntityLoadedBase implements ITickable, IEnergyUser {

	//keep this TE when only the IS_ON state changes: a recreated client TE loses the synced values
	@Override
	public boolean shouldRefresh(net.minecraft.world.World world, net.minecraft.util.math.BlockPos pos, net.minecraft.block.state.IBlockState oldState, net.minecraft.block.state.IBlockState newState) {
		return oldState.getBlock().getClass() != newState.getBlock().getClass();
	}

	private long power;

	public TileEntityMachineDetector(){
		super();
		this.power = 0;
	}
	
	@Override
	public void update() {
		if(!world.isRemote) {

			this.updateStandardConnections(world, pos);

			int meta = this.getBlockMetadata();
			int state = 0;

			if(this.power > 0) {
				state = 1;
				this.power -= 1;
			}

			if(meta != state) {
				PowerDetector.updateBlockState(state==1 ? true: false, world, pos);
				this.markDirty();
			}
		}
	}

	@Override
	public void setPower(long i) {
		this.power = i;
	}

	@Override
	public long getPower() {
		return this.power;
	}

	@Override
	public long getMaxPower() {
		return 30;
	}

}
