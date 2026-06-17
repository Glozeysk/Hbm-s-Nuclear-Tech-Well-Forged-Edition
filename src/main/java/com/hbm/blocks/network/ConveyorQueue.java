package com.hbm.blocks.network;

import com.hbm.entity.item.EntityMovingItem;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.*;

public final class ConveyorQueue {

    public static final double ITEM_LENGTH = 0.25D;
    public static final double HALF_ITEM = ITEM_LENGTH * 0.5D;
    public static final double ENTRY_PROGRESS = HALF_ITEM;
    public static final double EXIT_PROGRESS = 1.0D - HALF_ITEM;
    public static final int MAX_ITEMS_PER_LANE = 4;
    private static final double EPS = 1.0E-5D;

    private static final Object LOCK = new Object();
    private static final Map<LaneKey, LinkedHashSet<EntityMovingItem>> LANES = new HashMap<>();
    private static final Map<ReservationKey, EntityMovingItem> RESERVATIONS = new HashMap<>();
    private static final Map<ReservationKey, EntityMovingItem> HOLD_RESERVATIONS = new HashMap<>();

    private ConveyorQueue() {
    }

    private static boolean isServerWorld(World world) {
        return world != null && !world.isRemote;
    }

    public static void sync(World world, BlockPos pos, int lane, EntityMovingItem item) {
        if (!isServerWorld(world) || item == null || item.isDead || lane < 0) return;

        synchronized (LOCK) {
            if (item.hasQueueRegistration()) {
                if (item.getQueueDim() == world.provider.getDimension()
                        && pos.equals(item.getQueuePos())
                        && item.getQueueLane() == lane) {
                    LaneKey sameKey = new LaneKey(world.provider.getDimension(), pos, lane);
                    LANES.computeIfAbsent(sameKey, k -> new LinkedHashSet<>()).add(item);
                    return;
                }
                unregisterQueueInternal(item);
            }

            LaneKey key = new LaneKey(world.provider.getDimension(), pos, lane);
            LANES.computeIfAbsent(key, k -> new LinkedHashSet<>()).add(item);
            item.setQueueRegistration(world.provider.getDimension(), pos, lane);
        }
    }

    public static void unregister(EntityMovingItem item) {
        if (item == null) return;

        synchronized (LOCK) {
            if (item.hasQueueRegistration()) {
                unregisterQueueInternal(item);
            }
            if (item.hasPointReservation()) {
                releasePointReservationInternal(item);
            }
            if (item.hasHoldReservation()) {
                releaseHoldReservationInternal(item);
            }
        }
    }

    public static void unregisterQueueOnly(EntityMovingItem item) {
        if (item == null || !item.hasQueueRegistration()) return;

        synchronized (LOCK) {
            unregisterQueueInternal(item);
        }
    }

    private static void unregisterQueueInternal(EntityMovingItem item) {
        LaneKey key = new LaneKey(item.getQueueDim(), item.getQueuePos(), item.getQueueLane());
        LinkedHashSet<EntityMovingItem> set = LANES.get(key);

        if (set != null) {
            set.remove(item);
            if (set.isEmpty()) {
                LANES.remove(key);
            }
        }

        item.clearQueueRegistration();
    }

    public static List<EntityMovingItem> getOrderedItems(World world, BlockPos pos, int lane, BlockConveyor conveyor) {
        if (!isServerWorld(world)) {
            return Collections.emptyList();
        }

        LaneKey key = new LaneKey(world.provider.getDimension(), pos, lane);
        List<EntityMovingItem> items = new ArrayList<>();

        synchronized (LOCK) {
            LinkedHashSet<EntityMovingItem> raw = LANES.get(key);

            if (raw == null || raw.isEmpty()) {
                return Collections.emptyList();
            }

            List<EntityMovingItem> invalid = null;

            for (EntityMovingItem item : raw) {
                if (isValidMember(world, pos, lane, item)) {
                    items.add(item);
                } else {
                    if (invalid == null) invalid = new ArrayList<>();
                    invalid.add(item);
                }
            }

            if (invalid != null && !invalid.isEmpty()) {
                raw.removeAll(invalid);
                if (raw.isEmpty()) {
                    LANES.remove(key);
                }
            }
        }

        EnumFacing facing = conveyor.getLaneFacing(world, pos);
        items.sort((a, b) -> {
            double pa = conveyor.getLaneProgress(pos, facing, new Vec3d(a.posX, a.posY, a.posZ));
            double pb = conveyor.getLaneProgress(pos, facing, new Vec3d(b.posX, b.posY, b.posZ));
            return Double.compare(pb, pa);
        });

        return items;
    }

    public static boolean canInsertAtEntry(World world, BlockPos pos, int lane, BlockConveyor conveyor) {
        if (!isServerWorld(world)) return true;

        List<EntityMovingItem> items = getOrderedItems(world, pos, lane, conveyor);
        if (items.isEmpty()) return true;
        if (items.size() >= MAX_ITEMS_PER_LANE) return false;

        EntityMovingItem tail = items.get(items.size() - 1);
        EnumFacing facing = conveyor.getLaneFacing(world, pos);
        double tailProgress = conveyor.getLaneProgress(pos, facing, new Vec3d(tail.posX, tail.posY, tail.posZ));

        return tailProgress >= ENTRY_PROGRESS + ITEM_LENGTH - EPS;
    }

    public static boolean reservePoint(World world, BlockPos pos, int lane, double progress, EntityMovingItem item) {
        if (!isServerWorld(world) || item == null || item.isDead || lane < 0) return false;

        int slot = getSlotIndex(progress);

        synchronized (LOCK) {
            if (item.hasPointReservation()) {
                if (item.getReservationDim() == world.provider.getDimension()
                        && pos.equals(item.getReservationPos())
                        && item.getReservationLane() == lane
                        && getSlotIndex(item.getReservationProgress()) == slot) {
                    ReservationKey sameKey = new ReservationKey(world.provider.getDimension(), pos, lane, slot);
                    EntityMovingItem owner = RESERVATIONS.get(sameKey);
                    if (owner == null || owner == item || owner.isDead) {
                        RESERVATIONS.put(sameKey, item);
                        return true;
                    }
                    return false;
                }
                releasePointReservationInternal(item);
            }

            ReservationKey key = new ReservationKey(world.provider.getDimension(), pos, lane, slot);
            EntityMovingItem owner = RESERVATIONS.get(key);

            if (owner != null && owner != item && !owner.isDead) {
                return false;
            }

            RESERVATIONS.put(key, item);
            item.setPointReservation(world.provider.getDimension(), pos, lane, getSlotProgress(slot));
            return true;
        }
    }

    public static void releasePointReservation(EntityMovingItem item) {
        if (item == null || !item.hasPointReservation()) return;

        synchronized (LOCK) {
            releasePointReservationInternal(item);
        }
    }

    private static void releasePointReservationInternal(EntityMovingItem item) {
        ReservationKey key = new ReservationKey(
                item.getReservationDim(),
                item.getReservationPos(),
                item.getReservationLane(),
                getSlotIndex(item.getReservationProgress())
        );

        EntityMovingItem owner = RESERVATIONS.get(key);
        if (owner == item || owner == null || owner.isDead) {
            RESERVATIONS.remove(key);
        }

        item.clearPointReservation();
    }

    public static boolean reserveHoldPoint(World world, BlockPos pos, int lane, double progress, EntityMovingItem item) {
        if (!isServerWorld(world) || item == null || item.isDead || lane < 0) return false;

        int slot = getSlotIndex(progress);

        synchronized (LOCK) {
            if (item.hasHoldReservation()) {
                if (item.getHoldReservationDim() == world.provider.getDimension()
                        && pos.equals(item.getHoldReservationPos())
                        && item.getHoldReservationLane() == lane
                        && getSlotIndex(item.getHoldReservationProgress()) == slot) {
                    ReservationKey sameKey = new ReservationKey(world.provider.getDimension(), pos, lane, slot);
                    EntityMovingItem owner = HOLD_RESERVATIONS.get(sameKey);
                    if (owner == null || owner == item || owner.isDead) {
                        HOLD_RESERVATIONS.put(sameKey, item);
                        return true;
                    }
                    return false;
                }
                releaseHoldReservationInternal(item);
            }

            ReservationKey key = new ReservationKey(world.provider.getDimension(), pos, lane, slot);
            EntityMovingItem owner = HOLD_RESERVATIONS.get(key);

            if (owner != null && owner != item && !owner.isDead) {
                return false;
            }

            HOLD_RESERVATIONS.put(key, item);
            item.setHoldReservation(world.provider.getDimension(), pos, lane, getSlotProgress(slot));
            return true;
        }
    }

    public static void releaseHoldReservation(EntityMovingItem item) {
        if (item == null || !item.hasHoldReservation()) return;

        synchronized (LOCK) {
            releaseHoldReservationInternal(item);
        }
    }

    private static void releaseHoldReservationInternal(EntityMovingItem item) {
        ReservationKey key = new ReservationKey(
                item.getHoldReservationDim(),
                item.getHoldReservationPos(),
                item.getHoldReservationLane(),
                getSlotIndex(item.getHoldReservationProgress())
        );

        EntityMovingItem owner = HOLD_RESERVATIONS.get(key);
        if (owner == item || owner == null || owner.isDead) {
            HOLD_RESERVATIONS.remove(key);
        }

        item.clearHoldReservation();
    }

    public static boolean isPointBlocked(World world, BlockPos pos, int lane, double progress, EntityMovingItem requester) {
        if (!isServerWorld(world) || lane < 0) return false;

        int slot = getSlotIndex(progress);

        synchronized (LOCK) {
            ReservationKey key = new ReservationKey(world.provider.getDimension(), pos, lane, slot);

            EntityMovingItem ownerA = RESERVATIONS.get(key);
            if (ownerA != null) {
                if (ownerA.isDead) {
                    RESERVATIONS.remove(key);
                } else if (ownerA != requester) {
                    return true;
                }
            }

            EntityMovingItem ownerB = HOLD_RESERVATIONS.get(key);
            if (ownerB != null) {
                if (ownerB.isDead) {
                    HOLD_RESERVATIONS.remove(key);
                } else if (ownerB != requester) {
                    return true;
                }
            }
        }

        return false;
    }

    public static Double getNearestBlockedProgressAhead(World world, BlockPos pos, int lane, double minProgress, EntityMovingItem requester) {
        if (!isServerWorld(world) || lane < 0) return null;

        Double best = null;

        synchronized (LOCK) {
            for (int slot = 0; slot < MAX_ITEMS_PER_LANE; slot++) {
                double progress = getSlotProgress(slot);
                if (progress + EPS < minProgress) continue;

                ReservationKey key = new ReservationKey(world.provider.getDimension(), pos, lane, slot);

                EntityMovingItem ownerA = RESERVATIONS.get(key);
                if (ownerA != null) {
                    if (ownerA.isDead) {
                        RESERVATIONS.remove(key);
                    } else if (ownerA != requester) {
                        if (best == null || progress < best) best = progress;
                    }
                }

                EntityMovingItem ownerB = HOLD_RESERVATIONS.get(key);
                if (ownerB != null) {
                    if (ownerB.isDead) {
                        HOLD_RESERVATIONS.remove(key);
                    } else if (ownerB != requester) {
                        if (best == null || progress < best) best = progress;
                    }
                }
            }
        }

        return best;
    }

    public static int getSlotIndex(double progress) {
        double clamped = MathHelper.clamp(progress, ENTRY_PROGRESS, EXIT_PROGRESS);
        int idx = Math.round((float) ((clamped - ENTRY_PROGRESS) / ITEM_LENGTH));
        return MathHelper.clamp(idx, 0, MAX_ITEMS_PER_LANE - 1);
    }

    public static double getSlotProgress(int slot) {
        int clamped = MathHelper.clamp(slot, 0, MAX_ITEMS_PER_LANE - 1);
        return ENTRY_PROGRESS + clamped * ITEM_LENGTH;
    }

    public static void clearLane(World world, BlockPos pos, int lane) {
        if (!isServerWorld(world)) return;

        synchronized (LOCK) {
            LaneKey key = new LaneKey(world.provider.getDimension(), pos, lane);
            LinkedHashSet<EntityMovingItem> set = LANES.remove(key);
            if (set != null) {
                for (EntityMovingItem item : set) {
                    item.clearQueueRegistration();
                }
            }

            for (int slot = 0; slot < MAX_ITEMS_PER_LANE; slot++) {
                ReservationKey rKey = new ReservationKey(world.provider.getDimension(), pos, lane, slot);

                EntityMovingItem ownerA = RESERVATIONS.remove(rKey);
                if (ownerA != null) ownerA.clearPointReservation();

                EntityMovingItem ownerB = HOLD_RESERVATIONS.remove(rKey);
                if (ownerB != null) ownerB.clearHoldReservation();
            }
        }
    }

    public static void clearBlock(World world, BlockPos pos, int laneCount) {
        if (!isServerWorld(world)) return;

        synchronized (LOCK) {
            for (int i = 0; i < laneCount; i++) {
                clearLane(world, pos, i);
            }
        }
    }

    private static boolean isValidMember(World world, BlockPos pos, int lane, EntityMovingItem item) {
        if (item == null || item.isDead || item.world != world) return false;
        if (item.getConveyorLane() != lane) return false;

        BlockPos currentPos = new BlockPos(Math.floor(item.posX), Math.floor(item.posY), Math.floor(item.posZ));
        return pos.equals(currentPos);
    }

    private static final class LaneKey {
        private final int dim;
        private final BlockPos pos;
        private final int lane;

        private LaneKey(int dim, BlockPos pos, int lane) {
            this.dim = dim;
            this.pos = pos.toImmutable();
            this.lane = lane;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (!(obj instanceof LaneKey)) return false;
            LaneKey other = (LaneKey) obj;
            return dim == other.dim && lane == other.lane && Objects.equals(pos, other.pos);
        }

        @Override
        public int hashCode() {
            return Objects.hash(dim, pos, lane);
        }
    }

    private static final class ReservationKey {
        private final int dim;
        private final BlockPos pos;
        private final int lane;
        private final int slot;

        private ReservationKey(int dim, BlockPos pos, int lane, int slot) {
            this.dim = dim;
            this.pos = pos.toImmutable();
            this.lane = lane;
            this.slot = slot;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (!(obj instanceof ReservationKey)) return false;
            ReservationKey other = (ReservationKey) obj;
            return dim == other.dim && lane == other.lane && slot == other.slot && Objects.equals(pos, other.pos);
        }

        @Override
        public int hashCode() {
            return Objects.hash(dim, pos, lane, slot);
        }
    }
}