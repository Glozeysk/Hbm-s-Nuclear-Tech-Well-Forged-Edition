package com.hbm.explosion;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Iterator;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import com.hbm.config.CompatibilityConfig;
import com.hbm.render.amlfrom1710.Vec3;

import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.BlockPos.MutableBlockPos;

import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.world.World;

public class ExplosionNukeRayBatched {

	public HashMap<ChunkPos, HashSet<IntTriplet>> perChunk = new HashMap(); //for future: optimize blockmap further by using sub-chunks instead of chunks
	public List<ChunkPos> orderedChunks = new ArrayList();
	private CoordComparator comparator = new CoordComparator();
	public boolean isContained = true;
	int posX;
	int posY;
	int posZ;
	int chunkCenterX;
	int chunkCenterZ;
	World world;

	int strength;
	int radius;

	int gspNumMax;
	int gspNum;
	double gspX;
	double gspY;

	private static final int maxY = 255;
	private static final int minY = 0;
	private static final int LARGE_BLAST_WORKERS = 4;
	private static final int LARGE_BLAST_RADIUS = 256;
	private static final ExecutorService WORKERS = Executors.newFixedThreadPool(LARGE_BLAST_WORKERS, new NukeThreadFactory());

	public boolean isAusf3Complete = false;

	public ExplosionNukeRayBatched(World world, int x, int y, int z, int strength, int radius) {
		this.world = world;
		this.posX = x;
		this.posY = y;
		this.posZ = z;
		this.chunkCenterX = x >> 4;
		this.chunkCenterZ = z >> 4;
		this.strength = strength;
		this.radius = radius;

		// Total number of points
		this.gspNumMax = (int)(2.5 * Math.PI * Math.pow(this.strength, 2));
		this.gspNum = 1;

		// The beginning of the generalized spiral points
		this.gspX = Math.PI;
		this.gspY = 0.0;
	}

	private void generateGspUp(){
		if (this.gspNum < this.gspNumMax) {
			int k = this.gspNum + 1;
			double hk = -1.0 + 2.0 * (k - 1.0) / (this.gspNumMax - 1.0);
			this.gspX = Math.acos(hk);

			double prev_lon = this.gspY;
			double lon = prev_lon + 3.6 / Math.sqrt(this.gspNumMax) / Math.sqrt(1.0 - hk * hk);
			this.gspY = lon % (Math.PI * 2);
		} else {
			this.gspX = 0.0;
			this.gspY = 0.0;
		}
		this.gspNum++;
	}

	// Get Cartesian coordinates for spherical coordinates
	private Vec3 getSpherical2cartesian(){
		double dx = Math.sin(this.gspX) * Math.cos(this.gspY);
		double dz = Math.sin(this.gspX) * Math.sin(this.gspY);
		double dy = Math.cos(this.gspX);
		return Vec3.createVectorHelper(dx, dy, dz);
	}

	public void addPos(int x, int y, int z){
		ChunkPos chunkPos = new ChunkPos(x >> 4, z >> 4);
		HashSet<IntTriplet> triplets = perChunk.get(chunkPos);
				
		if(triplets == null) {
			triplets = new HashSet();
			perChunk.put(chunkPos, triplets);
		}
				
		triplets.add(new IntTriplet(x, y, z));
	}

	public void collectTip(int time) {
		if(!CompatibilityConfig.isWarDim(world)){
			isAusf3Complete = true;
			return;
		}
		if(this.radius >= LARGE_BLAST_RADIUS) {
			collectTipParallel(time);
			return;
		}
		MutableBlockPos pos = new BlockPos.MutableBlockPos();
		long raysProcessed = 0;
		long start = System.currentTimeMillis();


		IBlockState blockState;
		Block b;
		int iX, iY, iZ, radius;
		float rayStrength;
		Vec3 vec;

		while (this.gspNumMax >= this.gspNum){
			// Get Cartesian coordinates for spherical coordinates
			vec = this.getSpherical2cartesian();

			radius = (int)Math.ceil(this.radius);
			rayStrength = strength * 0.3F;

			//Finding the end of the ray
			for(int r = 0; r < radius+1; r ++) {

				iY = (int) Math.floor(posY + (vec.yCoord * r));
				
				if(iY < minY || iY > maxY){
					isContained = false;
					break;
				}

				iX = (int) Math.floor(posX + (vec.xCoord * r));
				iZ = (int) Math.floor(posZ + (vec.zCoord * r));


				pos.setPos(iX, iY, iZ);
				if(!world.isBlockLoaded(pos, false)) {
					isContained = false;
					break;
				}
				blockState = world.getBlockState(pos);
				b = blockState.getBlock();
				if(b.getExplosionResistance(null) >= 2_000_000)
					break;

				rayStrength -= Math.pow(getNukeResistance(blockState, b)+1, 3 * ((double) r) / ((double) radius))-1;

				//save block positions in to-destroy-hashset until rayStrength is 0 
				if(rayStrength > 0){
					if(b != Blocks.AIR) {
						//all-air chunks don't need to be buffered at all
						addPos(iX, iY, iZ);
					}
					if(r >= radius) {
						isContained = false;
					}
				} else {
					break;
				}
			}
			
			// Raise one generalized spiral points
			this.generateGspUp();
			raysProcessed++;
			if(raysProcessed % 50 == 0 && System.currentTimeMillis()+1 > start + time) {
				// System.out.println("NTM C "+raysProcessed+" "+Math.round(1000D * 100D*gspNum/(double)gspNumMax)/1000D+"% "+gspNum+"/"+gspNumMax+" "+(System.currentTimeMillis()-start)+"ms");
				return;
			}
		} 
		
		orderedChunks.addAll(perChunk.keySet());
		orderedChunks.sort(comparator);
		
		isAusf3Complete = true;
	}

	private void collectTipParallel(int time) {
		long start = System.currentTimeMillis();
		int batches = Math.max(1, LARGE_BLAST_WORKERS);

		while(this.gspNumMax >= this.gspNum && System.currentTimeMillis() < start + time) {
			List<RayTask> tasks = new ArrayList<RayTask>(batches);
			for(int i = 0; i < batches && this.gspNumMax >= this.gspNum; i++) {
				tasks.add(new RayTask(this.getSpherical2cartesian()));
				this.generateGspUp();
			}

			try {
				List<Future<RayResult>> results = WORKERS.invokeAll(tasks, Math.max(1, time), TimeUnit.MILLISECONDS);
				for(Future<RayResult> future : results) {
					if(!future.isDone() || future.isCancelled()) {
						continue;
					}
					RayResult result = future.get();
					if(!result.contained) {
						isContained = false;
					}
					for(IntTriplet triplet : result.positions) {
						addPos(triplet.xCoord, triplet.yCoord, triplet.zCoord);
					}
				}
			} catch(Exception ignored) {
				break;
			}
		}

		if(this.gspNumMax < this.gspNum) {
			orderedChunks.addAll(perChunk.keySet());
			orderedChunks.sort(comparator);
			isAusf3Complete = true;
		}
	}
	
	public static float getNukeResistance(IBlockState blockState, Block b) {
		if(blockState.getMaterial().isLiquid()){
			return 0.1F;
		} else {
			if(b == Blocks.SANDSTONE) return 4F;
			if(b == Blocks.OBSIDIAN) return 18F;
			return b.getExplosionResistance(null);
		}
	}
	
	/** little comparator for roughly sorting chunks by distance to the center */
	public class CoordComparator implements Comparator<ChunkPos> {

		@Override
		public int compare(ChunkPos o1, ChunkPos o2) {

			int diff1 = Math.abs(chunkCenterX - o1.x) + Math.abs(chunkCenterZ - o1.z);
			int diff2 = Math.abs(chunkCenterX - o2.x) + Math.abs(chunkCenterZ - o2.z);
			
			return diff1 > diff2 ? 1 : diff1 < diff2 ? -1 : 0;
		}
	}

	public void processChunk(int time){
		long start = System.currentTimeMillis();
		while(System.currentTimeMillis() < start + time){
			processChunkBlocks(start, time);
		}
	}

	HashSet<IntTriplet> positions = new HashSet();
	ChunkPos chunk;

	public void processChunkBlocks(long start, int time){
		if(!CompatibilityConfig.isWarDim(world)){
			this.perChunk.clear();
		}
		if(this.perChunk.isEmpty()) return;
		int i = 0;

		if(positions.size() == 0) {
			chunk = orderedChunks.get(0);
			positions = perChunk.get(chunk);
		}
		
		List<IntTriplet> done = new ArrayList<IntTriplet>();
		MutableBlockPos pos = new BlockPos.MutableBlockPos();
		for(IntTriplet coord : positions) {
			pos.setPos(coord.xCoord, coord.yCoord, coord.zCoord);
			if(!world.isBlockLoaded(pos, false)) {
				done.add(coord);
				continue;
			}
			world.setBlockToAir(pos);
			done.add(coord);
			i++;
			if(i % Math.max(256, time * 16) == 0 && System.currentTimeMillis()+1 > start + time){
				// System.out.println("NTM B "+Math.round(1000D * 100D*i/(double)positions.size())/1000D+"% "+i+"/"+positions.size()+" "+(System.currentTimeMillis()-start)+"ms");
				break;
			}
		}
		positions.removeAll(done);

		if(positions.size() == 0){
			perChunk.remove(chunk);
			orderedChunks.remove(0);
		}
	}
	
	public class IntTriplet {
		public final int xCoord;
		public final int yCoord;
		public final int zCoord;
		private final int hashCode;

		
		public IntTriplet(int x, int y, int z) {
			this.xCoord = x;
			this.yCoord = y;
			this.zCoord = z;
			this.hashCode = Objects.hash(x, y, z);
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (!(obj instanceof IntTriplet))
				return false;
			IntTriplet other = (IntTriplet) obj;
			if (xCoord != other.xCoord)
				return false;
			if (yCoord != other.yCoord)
				return false;
			if (zCoord != other.zCoord)
				return false;
			return true;
		}

		@Override
	    public int hashCode() {
	        return this.hashCode;
	    }
	}

	private class RayTask implements Callable<RayResult> {
		private final Vec3 vec;

		private RayTask(Vec3 vec) {
			this.vec = vec;
		}

		@Override
		public RayResult call() {
			RayResult result = new RayResult();
			MutableBlockPos pos = new BlockPos.MutableBlockPos();
			int rayRadius = (int)Math.ceil(ExplosionNukeRayBatched.this.radius);
			float rayStrength = ExplosionNukeRayBatched.this.strength * 0.3F;

			for(int r = 0; r < rayRadius + 1; r++) {
				int iY = (int)Math.floor(posY + (vec.yCoord * r));

				if(iY < minY || iY > maxY) {
					result.contained = false;
					break;
				}

				int iX = (int)Math.floor(posX + (vec.xCoord * r));
				int iZ = (int)Math.floor(posZ + (vec.zCoord * r));

				pos.setPos(iX, iY, iZ);
				if(!world.isBlockLoaded(pos, false)) {
					result.contained = false;
					break;
				}

				IBlockState blockState = world.getBlockState(pos);
				Block b = blockState.getBlock();
				if(b.getExplosionResistance(null) >= 2_000_000) {
					break;
				}

				rayStrength -= Math.pow(getNukeResistance(blockState, b) + 1, 3 * ((double)r) / ((double)rayRadius)) - 1;

				if(rayStrength > 0) {
					if(b != Blocks.AIR) {
						result.positions.add(new IntTriplet(iX, iY, iZ));
					}
					if(r >= rayRadius) {
						result.contained = false;
					}
				} else {
					break;
				}
			}

			return result;
		}
	}

	private class RayResult {
		private boolean contained = true;
		private final List<IntTriplet> positions = new ArrayList<IntTriplet>();
	}

	private static class NukeThreadFactory implements ThreadFactory {
		private int index;

		@Override
		public Thread newThread(Runnable runnable) {
			Thread thread = new Thread(runnable, "NTM-Nuke-Ray-" + index++);
			thread.setDaemon(true);
			return thread;
		}
	}
}
