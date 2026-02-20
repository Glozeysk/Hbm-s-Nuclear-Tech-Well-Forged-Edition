package com.hbm.blocks.bomb;

import java.util.Random;

import com.hbm.entity.effect.EntityCloudFleijaRainbow;
import com.hbm.entity.logic.EntityNukeExplosionMK3;
import com.hbm.blocks.ModBlocks;
import com.hbm.explosion.ExplosionLarge;
import com.hbm.explosion.ExplosionNT;
import com.hbm.explosion.ExplosionNT.ExAttrib;
import com.hbm.interfaces.IBomb;
import net.minecraft.init.SoundEvents;
import net.minecraft.util.SoundCategory;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.Explosion;
import net.minecraft.world.World;

public class DetMiner4 extends Block implements IBomb {

	public DetMiner4(Material m, String s) {
		super(m);
		this.setTranslationKey(s);
		this.setRegistryName(s);
		
		ModBlocks.ALL_BLOCKS.add(this);
	}
	
	@Override
	public Item getItemDropped(IBlockState state, Random rand, int fortune) {
		return Items.AIR;
	}
	
	@Override
	public void explode(World world, BlockPos pos) {
		if (!world.isRemote) {

			world.playSound(null, pos.getX(), pos.getY(), pos.getZ(), SoundEvents.ENTITY_GENERIC_EXPLODE, SoundCategory.AMBIENT, 100.0f, world.rand.nextFloat() * 0.1F + 0.9F);
			EntityNukeExplosionMK3 exp = new EntityNukeExplosionMK3(world);
				exp.posX = pos.getX();
				exp.posY = pos.getY();
				exp.posZ = pos.getZ();
				exp.destructionRange = 100;
				exp.speed = 25;
				exp.coefficient = 1.0F;
				exp.waste = false;
				exp.dropblocks = true;
					if(!EntityNukeExplosionMK3.isJammed(world, exp)){
						world.spawnEntity(exp);
			    		
			    		EntityCloudFleijaRainbow cloud = new EntityCloudFleijaRainbow(world, 100);
			    		cloud.posX = pos.getX();
			    		cloud.posY = pos.getY();
			    		cloud.posZ = pos.getZ();
			    		world.spawnEntity(cloud);
			    		world.setBlockToAir(pos);
			    	}
		}
	}
	
	// @Override
	// public void onExplosionDestroy(World worldIn, BlockPos pos, Explosion explosionIn) {
	// 	this.explode(worldIn, pos);
	// }
	
	@Override
	public void neighborChanged(IBlockState state, World world, BlockPos pos, Block blockIn, BlockPos fromPos) {
		if (world.getRedstonePowerFromNeighbors(pos) > 0)
        {
        	this.explode(world, pos);
        }
	}

}
