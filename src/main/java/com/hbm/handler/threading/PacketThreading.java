package com.hbm.handler.threading;

import com.hbm.config.GeneralConfig;
import com.hbm.main.MainRegistry;
import com.hbm.main.NetworkHandler;
import com.hbm.packet.PacketDispatcher;
import com.hbm.packet.threading.ThreadedPacket;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraftforge.fml.common.network.NetworkRegistry.TargetPoint;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import org.jctools.queues.MpscBlockingConsumerArrayQueue;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.LongAdder;
import java.util.concurrent.locks.LockSupport;
import java.util.concurrent.locks.ReentrantLock;

import static com.hbm.lib.internal.UnsafeHolder.*;

/**
 * Methods here are safe to call off-thread
 */
public class PacketThreading {
    /**
     * Global lock guarding the FML channel state for outbound packets.
     */
    public static final ReentrantLock LOCK = new ReentrantLock();
    private static final Object IN_FLIGHT_BASE = staticFieldBase(PacketThreading.class, "inFlightDispatch");
    private static final long IN_FILGHT_OFF = staticfieldOffset(PacketThreading.class, "inFlightDispatch");
    private static final int QUEUE_CAPACITY = 4096;
    private static final int BATCH_SIZE = 128;
    private static final LongAdder totalCnt = new LongAdder();
    private static final LongAdder nanosWaited = new LongAdder();
    @SuppressWarnings("FieldMayBeFinal")
    private static volatile boolean running = true;
    private static volatile boolean enabled = false;
    @SuppressWarnings("unused")
    private static volatile int inFlightDispatch;
    private static volatile MpscBlockingConsumerArrayQueue<PacketTask> singleThreadQueue;
    private static volatile Thread singleWorkerThread;

    /**
     * Sets up thread pool settings during mod initialization.
     */
    public static synchronized void init() {
        shutdown();

        if (!GeneralConfig.enablePacketThreading) {
            enabled = false;
            return;
        }

        enabled = true;

        MainRegistry.logger.info("Initializing PacketThreading in Optimized Single-Threaded mode.");
        running = true;
        MpscBlockingConsumerArrayQueue<PacketTask> q = new MpscBlockingConsumerArrayQueue<>(QUEUE_CAPACITY);
        singleThreadQueue = q;
        Thread t = new Thread(() -> processBatch(q), "NTM-Packet-Thread-0");
        t.setDaemon(true);
        singleWorkerThread = t;
        t.start();

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

        Thread t = singleWorkerThread;
        singleWorkerThread = null;
        if (t != null && t.isAlive()) t.interrupt();
        MpscBlockingConsumerArrayQueue<PacketTask> q = singleThreadQueue;
        singleThreadQueue = null;
        if (q != null) {
            PacketTask task;
            while ((task = q.relaxedPoll()) != null) {
                task.packet.releaseBuffer();
            }
        }
    }

    private static void processBatch(MpscBlockingConsumerArrayQueue<PacketTask> q) {
        List<PacketTask> batchBuffer = new ArrayList<>(BATCH_SIZE);

        while (running) {
            try {
                PacketTask first = q.take();
                batchBuffer.add(first);
                for (int i = 0; i < BATCH_SIZE - 1; i++) {
                    PacketTask next = q.relaxedPoll();
                    if (next == null) break;
                    batchBuffer.add(next);
                }

                for (int i = 0; i < batchBuffer.size(); i++) {
                    PacketTask task = batchBuffer.get(i);
                    try {
                        task.packet.getCompiledBuffer();
                    } catch (Throwable t) {
                        MainRegistry.logger.error("Failed to compile threaded packet", t);
                        task.packet.releaseBuffer();
                        batchBuffer.set(i, null);
                    }
                }

                LOCK.lock();
                try {
                    boolean doFlushServer = false;
                    boolean doFlushClient = false;

                    for (PacketTask task : batchBuffer) {
                        if (task == null) continue;
                        try {
                            send(task);
                            if (task.op == PacketOp.SERVER) doFlushClient = true;
                            else doFlushServer = true;
                        } catch (Throwable t) {
                            MainRegistry.logger.error("Failed to write packet to channel", t);
                        }
                    }

                    // Early flush to reduce latency (tick-end flush stays as a backstop).
                    if (doFlushServer) NetworkHandler.flushServerDirect();
                    if (doFlushClient) NetworkHandler.flushClientDirect();

                } finally {
                    LOCK.unlock();
                }

                for (PacketTask task : batchBuffer) {
                    if (task != null) task.packet.releaseBuffer();
                }
            } catch (InterruptedException e) {
                MainRegistry.logger.warn("Packet worker interrupted");
                Thread.currentThread().interrupt();
                break;
            } catch (Throwable t) {
                MainRegistry.logger.error("Crash in packet worker loop", t);
                for (PacketTask task : batchBuffer) {
                    if (task != null) task.packet.releaseBuffer();
                }
            } finally {
                batchBuffer.clear();
            }
        }

        PacketTask task;
        while ((task = q.relaxedPoll()) != null) {
            task.packet.releaseBuffer();
        }
    }

    // caller shall release() the buffer sent once after send()
    // FML fan-out retains pkt.payload() for each dispatcher, so the underlying memory
    // stays alive until all downstream retains are released.
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
        U.getAndAddInt(IN_FLIGHT_BASE, IN_FILGHT_OFF, 1);
        try {
            if (!enabled || !GeneralConfig.enablePacketThreading) {
                runSynchronously(packet, op, target, dimension);
                return;
            }

            PacketTask task = new PacketTask(packet, op, target, dimension);


            MpscBlockingConsumerArrayQueue<PacketTask> q = singleThreadQueue;
            if (q == null || !q.offer(task)) {
                MainRegistry.logger.warn("Packet Queue full (size > {}). Running synchronously.", QUEUE_CAPACITY);
                runSynchronously(packet, op, target, dimension);
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
                send(new PacketTask(packet, op, target, dimension));
            } finally {
                LOCK.unlock();
            }
        } catch (Throwable t) {
            MainRegistry.logger.error("Error sending packet synchronously", t);
            throw t;
        } finally {
            packet.releaseBuffer();
            nanosWaited.add(System.nanoTime() - start);
        }
    }

    /**
     * Mirrors {@link com.hbm.main.NetworkHandler#sendToDimension(IMessage, int)}.
     */
    public static void createSendToDimensionThreadedPacket(@NotNull ThreadedPacket message, int dimensionId) {
        dispatch(message, PacketOp.DIMENSION, null, dimensionId);
    }

    /**
     * Mirrors {@link com.hbm.main.NetworkHandler#sendToAllAround(IMessage, TargetPoint)}.
     */
    public static void createAllAroundThreadedPacket(@NotNull ThreadedPacket message, @NotNull TargetPoint target) {
        dispatch(message, PacketOp.ALL_AROUND, target, Integer.MIN_VALUE);
    }

    /**
     * Mirrors {@link com.hbm.main.NetworkHandler#sendToAllTracking(IMessage, TargetPoint)}.
     */
    public static void createSendToAllTrackingThreadedPacket(@NotNull ThreadedPacket message, @NotNull TargetPoint point) {
        dispatch(message, PacketOp.TRACKING_POINT, point, Integer.MIN_VALUE);
    }

    /**
     * Mirrors {@link com.hbm.main.NetworkHandler#sendToAllTracking(IMessage, Entity)}.
     */
    public static void createSendToAllTrackingThreadedPacket(@NotNull ThreadedPacket message, @NotNull Entity entity) {
        dispatch(message, PacketOp.TRACKING_ENTITY, entity, Integer.MIN_VALUE);
    }

    /**
     * Mirrors {@link com.hbm.main.NetworkHandler#sendTo(IMessage, EntityPlayerMP)}.
     */
    public static void createSendToThreadedPacket(@NotNull ThreadedPacket message, @NotNull EntityPlayerMP player) {
        dispatch(message, PacketOp.PLAYER, player, Integer.MIN_VALUE);
    }

    /**
     * Mirrors {@link com.hbm.main.NetworkHandler#sendToAll(IMessage)}.
     */
    public static void createSendToAllThreadedPacket(@NotNull ThreadedPacket message) {
        dispatch(message, PacketOp.ALL, null, Integer.MIN_VALUE);
    }

    /**
     * Mirrors {@link com.hbm.main.NetworkHandler#sendToServer(IMessage)}.
     */
    public static void createSendToServerThreadedPacket(@NotNull ThreadedPacket message) {
        dispatch(message, PacketOp.SERVER, null, Integer.MIN_VALUE);
    }

    private enum PacketOp {
        PLAYER, ALL, DIMENSION, ALL_AROUND, TRACKING_POINT, TRACKING_ENTITY, SERVER
    }

    record PacketTask(ThreadedPacket packet, PacketOp op, Object target, int dimension) {
    }
}
