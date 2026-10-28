package com.hbm.blocks.network;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

import com.hbm.blocks.ModBlocks;
import com.hbm.lib.DirPos;
import com.hbm.tileentity.TileEntityProxyBase;
import com.hbm.tileentity.machine.TileEntityMachineArcWelder;
import com.hbm.tileentity.machine.TileEntityMachineCompressorCompact;

import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;

//Air ducts carry nothing, they only tie compressor ports and arc welder ports into one vacuum network.
public class AirDuctNetwork {

	/** a runaway network is cut off here so a mistake never walks half the world */
	public static final int MAX_DUCTS = 4096;

	public static TileEntityMachineArcWelder getWelder(IBlockAccess world, BlockPos pos) {
		TileEntity te = world.getTileEntity(pos);

		if(te instanceof TileEntityMachineArcWelder)
			return (TileEntityMachineArcWelder) te;

		if(te instanceof TileEntityProxyBase) {
			TileEntity core = ((TileEntityProxyBase) te).getTE();
			if(core instanceof TileEntityMachineArcWelder)
				return (TileEntityMachineArcWelder) core;
		}

		return null;
	}

	public static TileEntityMachineCompressorCompact getCompressor(IBlockAccess world, BlockPos pos) {
		TileEntity te = world.getTileEntity(pos);

		if(te instanceof TileEntityMachineCompressorCompact)
			return (TileEntityMachineCompressorCompact) te;

		if(te instanceof TileEntityProxyBase) {
			TileEntity core = ((TileEntityProxyBase) te).getTE();
			if(core instanceof TileEntityMachineCompressorCompact)
				return (TileEntityMachineCompressorCompact) core;
		}

		return null;
	}

	/** whether a duct at pos joins whatever sits on that side */
	public static boolean connects(IBlockAccess world, BlockPos pos, EnumFacing side) {
		BlockPos other = pos.offset(side);

		if(world.getBlockState(other).getBlock() == ModBlocks.air_duct)
			return true;

		//only welder blocks with a tile entity are ports, the plain dummies above them are not
		if(getWelder(world, other) != null)
			return true;

		//a compressor only takes a duct at one of its connection spots
		TileEntityMachineCompressorCompact compressor = getCompressor(world, other);
		if(compressor != null)
			for(DirPos con : compressor.getConPos())
				if(con.getPos().equals(pos))
					return true;

		return false;
	}

	/** every welder reachable from the compressor, standing directly at a port or at the end of connected ducts */
	public static Set<TileEntityMachineArcWelder> findWelders(TileEntityMachineCompressorCompact compressor) {

		Set<TileEntityMachineArcWelder> welders = new LinkedHashSet<TileEntityMachineArcWelder>();
		Set<BlockPos> visited = new HashSet<BlockPos>();
		ArrayDeque<BlockPos> queue = new ArrayDeque<BlockPos>();
		IBlockAccess world = compressor.getWorld();

		for(DirPos con : compressor.getConPos()) {
			BlockPos start = con.getPos();
			TileEntityMachineArcWelder direct = getWelder(world, start);

			if(direct != null)
				welders.add(direct);
			else if(world.getBlockState(start).getBlock() == ModBlocks.air_duct && visited.add(start))
				queue.add(start);
		}

		while(!queue.isEmpty() && visited.size() <= MAX_DUCTS) {
			BlockPos duct = queue.poll();

			for(EnumFacing side : EnumFacing.VALUES) {
				BlockPos next = duct.offset(side);
				TileEntityMachineArcWelder welder = getWelder(world, next);

				if(welder != null)
					welders.add(welder);
				else if(world.getBlockState(next).getBlock() == ModBlocks.air_duct && visited.add(next))
					queue.add(next);
			}
		}

		return welders;
	}
}
