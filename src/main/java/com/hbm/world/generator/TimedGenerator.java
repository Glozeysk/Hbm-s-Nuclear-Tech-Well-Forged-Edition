package com.hbm.world.generator;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Deque;

import net.minecraft.world.World;

public class TimedGenerator {

	private static final HashMap<Integer, Deque<ITimedJob>> operations = new HashMap<>();

	public static void automaton(World world, int amount) {

		Deque<ITimedJob> list = operations.get(world.provider.getDimension());

		if(list == null)
			return;

		long start = System.currentTimeMillis();
		int limit = Math.max(1, amount);
		int processed = 0;

		while(processed < limit && start + 10 > System.currentTimeMillis()) {

			if(list.isEmpty())
				return;

			ITimedJob entry = list.pollFirst();

			entry.work();
			processed++;
		}
	}

	public static void addOp(World world, ITimedJob job) {

		Deque<ITimedJob> list = operations.get(world.provider.getDimension());

		if(list == null) {
			list = new ArrayDeque<>();
			operations.put(world.provider.getDimension(), list);
		}

		list.add(job);
	}
	
	//should i be doing this? probably not, but watch me go
	//Drillgon200: I mean, a standard java Runnable probably would have worked exactly the same.
	public interface ITimedJob {

		public void work();

	}
}
