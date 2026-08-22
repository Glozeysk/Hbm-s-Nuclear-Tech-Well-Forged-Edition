package com.hbm.handler.threading;

import com.hbm.config.GeneralConfig;
import com.hbm.main.MainRegistry;
import com.hbm.main.NetworkHandler;
import com.hbm.packet.AuxElectricityPacket;
import com.hbm.packet.AuxGaugePacket;
import com.hbm.packet.AuxLongPacket;
import com.hbm.packet.AuxParticlePacketNT;
import com.hbm.packet.FluidTankPacket;
import com.hbm.packet.PacketDispatcher;
import com.hbm.packet.threading.ThreadedPacket;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.network.NetworkRegistry.TargetPoint;

import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;
import java.util.concurrent.locks.LockSupport;
import java.util.concurrent.locks.ReentrantLock;

import static com.hbm.lib.internal.UnsafeHolder.*;

public class PacketThreading {
    public static final ReentrantLock LOCK = new ReentrantLock();
    private static final Object IN_FLIGHT_BASE = staticFieldBase(PacketThreading.class, "inFlightDispatch");
    private static final long IN_FILGHT_OFF = staticfieldOffset(PacketThreading.class, "inFlightDispatch");
    private static final int QUEUE_CAPACITY = 8192;
    private static final int DROP_PARTICLE_QUEUE_THRESHOLD = 64;
    private static final int DROP_MAINTENANCE_QUEUE_THRESHOLD = 64;
    private static final int BATCH_SIZE = 256;
    private static final long TRANSIENT_SYNC_INTERVAL_NANOS = TimeUnit.MILLISECONDS.toNanos(500L);
    private static final long QUEUE_FULL_LOG_INTERVAL_NANOS = TimeUnit.SECONDS.toNanos(10L);
    private static final LongAdder totalCnt = new LongAdder();
    private static final LongAdder nanosWaited = new LongAdder();
    private static final Map<Long, Long> transientSyncTimes = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, LongAdder> recentPacketCounts = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, LongAdder> recentDroppedPacketCounts = new ConcurrentHashMap<>();
    private static final ConcurrentSkipListMap<Long, PacketTask> readyPackets = new ConcurrentSkipListMap<>();
    private static final AtomicLong sequenceCounter = new AtomicLong();
    private static volatile long nextSequenceToSend;
    private static volatile boolean running = true;
    private static volatile boolean enabled = false;
    private static volatile long lastQueueFullLogNanos;
    @SuppressWarnings("unused")
    private static volatile int inFlightDispatch;
    private static volatile LinkedBlockingQueue<PacketTask> dispatchQueue;
    private static volatile Thread[] packetWorkers;

    public static synchronized void init() {
        shutdown();
        transientSyncTimes.clear();
        recentPacketCounts.clear();
        recentDroppedPacketCounts.clear();
        readyPackets.clear();
        nextSequenceToSend = 0L;
        sequenceCounter.set(0L);

        if (!GeneralConfig.enablePacketThreading) {
            enabled = false;
            return;
        }

        enabled = true;
        running = true;
        LinkedBlockingQueue<PacketTask> q = new LinkedBlockingQueue<>(QUEUE_CAPACITY);
        dispatchQueue = q;
        Thread[] workers = new Thread[getWorkerCount()];
        for (int i = 0; i < workers.length; i++) {
            Thread t = new Thread(() -> processBatch(q), "NTM-Packet-Thread-" + i);
            t.setDaemon(true);
            workers[i] = t;
            t.start();
        }
        packetWorkers = workers;
        MainRegistry.logger.info("Initializing PacketThreading with {} workers and queue capacity {}.", workers.length, QUEUE_CAPACITY);
    }

    private static int getWorkerCount() {
        if (GeneralConfig.packetThreadingWorkers > 0) {
            return Math.max(1, Math.min(16, GeneralConfig.packetThreadingWorkers));
        }
        return Math.max(1, Math.min(4, Math.max(1, Runtime.getRuntime().availableProcessors() / 8)));
    }

    private static void shutdown() {
        enabled = false;
        running = false;
        int spin = 0;
        while (inFlightDispatch != 0) {
            spin++;
            if (spin < 1000) {
                Thread.yield();
            } else {
                LockSupport.parkNanos(500L);
            }
        }

        Thread[] workers = packetWorkers;
        packetWorkers = null;
        if (workers != null) {
            for (Thread t : workers) {
                if (t != null && t.isAlive()) {
                    t.interrupt();
                }
            }
        }

        LinkedBlockingQueue<PacketTask> q = dispatchQueue;
        dispatchQueue = null;
        if (q != null) {
            PacketTask task;
            while ((task = q.poll()) != null) {
                releaseTask(task);
            }
        }
        Map.Entry<Long, PacketTask> entry;
        while ((entry = readyPackets.pollFirstEntry()) != null) {
            releaseTask(entry.getValue());
        }
    }

    private static void processBatch(LinkedBlockingQueue<PacketTask> q) {
        ArrayList<PacketTask> batchBuffer = new ArrayList<>(BATCH_SIZE);

        while (running) {
            try {
                PacketTask first = q.take();
                batchBuffer.add(first);
                q.drainTo(batchBuffer, BATCH_SIZE - 1);

                for (PacketTask task : batchBuffer) {
                    if (task == null) {
                        continue;
                    }
                    try {
                        task.packet.getCompiledBuffer();
                    } catch (Throwable t) {
                        MainRegistry.logger.error("Failed to compile threaded packet", t);
                        failSequencedTask(task);
                    }
                }

                publishReadyPackets(batchBuffer);
                flushReadyPackets();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Throwable t) {
                MainRegistry.logger.error("Crash in packet worker loop", t);
                for (PacketTask task : batchBuffer) {
                    failSequencedTask(task);
                }
                flushReadyPackets();
                break;
            } finally {
                batchBuffer.clear();
            }
        }

        PacketTask task;
        while ((task = q.poll()) != null) {
            failSequencedTask(task);
        }
        flushReadyPackets();
    }

    private static void publishReadyPackets(ArrayList<PacketTask> batchBuffer) {
        for (PacketTask task : batchBuffer) {
            if (task != null) {
                readyPackets.put(task.sequence, task);
            }
        }
    }

    private static void flushReadyPackets() {
        LOCK.lock();
        try {
            boolean doFlushServer = false;
            boolean doFlushClient = false;
            while (true) {
                PacketTask task = readyPackets.remove(nextSequenceToSend);
                if (task == null) {
                    break;
                }
                nextSequenceToSend++;
                try {
                    if (!task.failed) {
                        send(task);
                        if (task.op == PacketOp.SERVER) {
                            doFlushClient = true;
                        } else {
                            doFlushServer = true;
                        }
                    }
                } catch (Throwable t) {
                    MainRegistry.logger.error("Failed to write packet to channel", t);
                } finally {
                    releaseTask(task);
                }
            }
            if (doFlushServer) {
                NetworkHandler.flushServerDirect();
            }
            if (doFlushClient) {
                NetworkHandler.flushClientDirect();
            }
        } finally {
            LOCK.unlock();
        }
    }

    private static void failSequencedTask(PacketTask task) {
        if (task == null) {
            return;
        }
        task.failed = true;
        readyPackets.putIfAbsent(task.sequence, task);
    }

    private static void dropSequencedTask(PacketTask task) {
        if (task == null) {
            return;
        }
        task.failed = true;
        readyPackets.put(task.sequence, task);
        flushReadyPackets();
    }

    private static void completeSequencedGap(PacketTask task) {
        if (task == null || task.sequence < 0L) {
            return;
        }
        task.failed = true;
        readyPackets.put(task.sequence, task);
        flushReadyPackets();
    }

    private static void releaseTask(PacketTask task) {
        if (task != null && task.packet != null) {
            task.packet.releaseBuffer();
        }
    }

    private static void send(PacketTask task) {
        switch (task.op) {
            case SERVER -> PacketDispatcher.wrapper.sendToServerDirect(task.packet);
            case PLAYER -> PacketDispatcher.wrapper.sendToDirect(task.packet, (EntityPlayerMP) task.target);
            case ALL -> PacketDispatcher.wrapper.sendToAllDirect(task.packet);
            case DIMENSION -> PacketDispatcher.wrapper.sendToDimensionDirect(task.packet, task.dimension);
            case ALL_AROUND -> PacketDispatcher.wrapper.sendToAllAroundDirect(task.packet, (TargetPoint) task.target);
            case TRACKING_POINT -> PacketDispatcher.wrapper.sendToAllTrackingDirect(task.packet, (TargetPoint) task.target);
            case TRACKING_ENTITY -> PacketDispatcher.wrapper.sendToAllTrackingDirect(task.packet, (Entity) task.target);
        }
    }

    private static void dispatch(ThreadedPacket packet, PacketOp op, Object target, int dimension) {
        totalCnt.increment();
        recordPacket(packet);
        U.getAndAddInt(IN_FLIGHT_BASE, IN_FILGHT_OFF, 1);
        try {
            if (!enabled || !GeneralConfig.enablePacketThreading) {
                try {
                    runSynchronously(packet, op, target, dimension);
                } finally {
                    completeSequencedGap(new PacketTask(packet, op, target, dimension, sequenceCounter.getAndIncrement()));
                }
                return;
            }

            if (isServerBroadcast(op) && !hasPlayers()) {
                packet.releaseBuffer();
                return;
            }

            long sequence = sequenceCounter.getAndIncrement();
            PacketTask task = new PacketTask(packet, op, target, dimension, sequence);

            if (shouldRateLimitTransient(packet)) {
                recordDroppedPacket(packet);
                dropSequencedTask(task);
                return;
            }

            LinkedBlockingQueue<PacketTask> q = dispatchQueue;
            if (q == null) {
                try {
                    runSynchronously(packet, op, target, dimension);
                } finally {
                    completeSequencedGap(task);
                }
                return;
            }

            if (shouldDropWhenCongested(packet, q.size())) {
                recordDroppedPacket(packet);
                dropSequencedTask(task);
                return;
            }

            if (!q.offer(task)) {
                if (isDroppable(packet)) {
                    recordDroppedPacket(packet);
                    dropSequencedTask(task);
                    maybeLogQueueFull(q.size());
                    return;
                }
                maybeLogQueueFull(q.size());
                try {
                    runSynchronously(packet, op, target, dimension);
                } finally {
                    completeSequencedGap(task);
                }
            }
        } finally {
            U.getAndAddInt(IN_FLIGHT_BASE, IN_FILGHT_OFF, -1);
        }
    }

    private static void runSynchronously(ThreadedPacket packet, PacketOp op, Object target, int dimension) {
        long start = System.nanoTime();
        try {
            packet.getCompiledBuffer();
            LOCK.lock();
            try {
                PacketTask task = new PacketTask(packet, op, target, dimension, -1L);
                send(task);
                if (op == PacketOp.SERVER) {
                    NetworkHandler.flushClientDirect();
                } else {
                    NetworkHandler.flushServerDirect();
                }
            } finally {
                LOCK.unlock();
            }
        } catch (Throwable t) {
            MainRegistry.logger.error("Error sending packet synchronously", t);
            throw t;
        } finally {
            nanosWaited.add(System.nanoTime() - start);
        }
    }

    private static boolean shouldRateLimitTransient(ThreadedPacket packet) {
        return false;
    }

    private static boolean shouldDropWhenCongested(ThreadedPacket packet, int size) {
        if (packet instanceof AuxParticlePacketNT) {
            return size >= QUEUE_CAPACITY - DROP_PARTICLE_QUEUE_THRESHOLD;
        }
        if (isMaintenancePacket(packet)) {
            return size >= QUEUE_CAPACITY - DROP_MAINTENANCE_QUEUE_THRESHOLD;
        }
        return false;
    }

    private static boolean isDroppable(ThreadedPacket packet) {
        return packet instanceof AuxParticlePacketNT || isMaintenancePacket(packet);
    }

    private static boolean isMaintenancePacket(ThreadedPacket packet) {
        return packet instanceof AuxGaugePacket
                || packet instanceof AuxLongPacket
                || packet instanceof AuxElectricityPacket
                || packet instanceof FluidTankPacket;
    }

    private static boolean isServerBroadcast(PacketOp op) {
        return op == PacketOp.ALL
                || op == PacketOp.DIMENSION
                || op == PacketOp.ALL_AROUND
                || op == PacketOp.TRACKING_POINT
                || op == PacketOp.TRACKING_ENTITY
                || op == PacketOp.PLAYER;
    }

    private static boolean hasPlayers() {
        MinecraftServer server = FMLCommonHandler.instance().getMinecraftServerInstance();
        return server != null && server.getPlayerList() != null && !server.getPlayerList().getPlayers().isEmpty();
    }

    private static void recordPacket(ThreadedPacket packet) {
        if (packet != null) {
            recentPacketCounts.computeIfAbsent(packet.getClass().getSimpleName(), key -> new LongAdder()).increment();
        }
    }

    private static void recordDroppedPacket(ThreadedPacket packet) {
        if (packet != null) {
            recentDroppedPacketCounts.computeIfAbsent(packet.getClass().getSimpleName(), key -> new LongAdder()).increment();
        }
    }

    private static void maybeLogQueueFull(int size) {
        long now = System.nanoTime();
        if (now - lastQueueFullLogNanos < QUEUE_FULL_LOG_INTERVAL_NANOS) {
            return;
        }
        lastQueueFullLogNanos = now;
        MainRegistry.logger.warn("Packet Queue full or congested (size {}). Recent packets: {}. Dropped: {}.", size, describeTop(recentPacketCounts), describeTop(recentDroppedPacketCounts));
        recentPacketCounts.clear();
        recentDroppedPacketCounts.clear();
    }

    private static String describeTop(ConcurrentHashMap<String, LongAdder> counts) {
        return counts.entrySet().stream()
                .map(entry -> Map.entry(entry.getKey(), entry.getValue().sum()))
                .filter(entry -> entry.getValue() > 0L)
                .sorted((left, right) -> Long.compare(right.getValue(), left.getValue()))
                .limit(6)
                .map(entry -> entry.getKey() + "=" + entry.getValue())
                .reduce((left, right) -> left + ", " + right)
                .orElse("none");
    }

    public static void createSendToDimensionThreadedPacket(ThreadedPacket message, int dimensionId) {
        dispatch(message, PacketOp.DIMENSION, null, dimensionId);
    }

    public static void createAllAroundThreadedPacket(ThreadedPacket message, TargetPoint target) {
        dispatch(message, PacketOp.ALL_AROUND, target, Integer.MIN_VALUE);
    }

    public static void createSendToAllTrackingThreadedPacket(ThreadedPacket message, TargetPoint point) {
        dispatch(message, PacketOp.TRACKING_POINT, point, Integer.MIN_VALUE);
    }

    public static void createSendToAllTrackingThreadedPacket(ThreadedPacket message, Entity entity) {
        dispatch(message, PacketOp.TRACKING_ENTITY, entity, Integer.MIN_VALUE);
    }

    public static void createSendToThreadedPacket(ThreadedPacket message, EntityPlayerMP player) {
        dispatch(message, PacketOp.PLAYER, player, Integer.MIN_VALUE);
    }

    public static void createSendToAllThreadedPacket(ThreadedPacket message) {
        dispatch(message, PacketOp.ALL, null, Integer.MIN_VALUE);
    }

    public static void createSendToServerThreadedPacket(ThreadedPacket message) {
        dispatch(message, PacketOp.SERVER, null, Integer.MIN_VALUE);
    }

    private enum PacketOp {
        PLAYER, ALL, DIMENSION, ALL_AROUND, TRACKING_POINT, TRACKING_ENTITY, SERVER
    }

    private static final class PacketTask {
        private final ThreadedPacket packet;
        private final PacketOp op;
        private final Object target;
        private final int dimension;
        private final long sequence;
        private boolean failed;

        private PacketTask(ThreadedPacket packet, PacketOp op, Object target, int dimension, long sequence) {
            this.packet = packet;
            this.op = op;
            this.target = target;
            this.dimension = dimension;
            this.sequence = sequence;
        }
    }
}