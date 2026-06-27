package com.hbm.tileentity.network;

import com.hbm.blocks.network.conveyor.BeltItemData;
import com.hbm.blocks.network.conveyor.BeltLane;
import com.hbm.blocks.network.conveyor.BeltSegment;
import com.hbm.blocks.network.conveyor.BeltSegmentManager;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ITickable;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.common.util.Constants;

public class TileEntityConveyor extends TileEntity implements ITickable {

    private NBTTagCompound pendingItems = null;
    private boolean restored = false;
    private int restoreAttempts = 0;
    private static final int MAX_RESTORE_ATTEMPTS = 20;

    @Override
    public void update() {
        if (world.isRemote) return;

        if (pendingItems != null && !restored) {
            if (restorePendingItems()) {
                restored = true;
                pendingItems = null;
            } else {
                restoreAttempts++;
                if (restoreAttempts >= MAX_RESTORE_ATTEMPTS) {
                    System.out.println("Failed to restore items at " + pos + " after " + MAX_RESTORE_ATTEMPTS + " attempts");
                    pendingItems = null;
                }
            }
        }
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound nbt) {
        nbt = super.writeToNBT(nbt);

        BeltSegment segment = BeltSegmentManager.getSegmentAt(world, pos);
        if (segment != null) {
            int blockIndex = segment.getBlockIndex(pos);
            if (blockIndex >= 0) {
                NBTTagList itemList = new NBTTagList();
                double minProg = blockIndex;
                double maxProg = blockIndex + 1.0D;

                for (int l = 0; l < segment.getLaneCount(); l++) {
                    BeltLane lane = segment.getLane(l);
                    for (BeltItemData item : lane.getItems()) {
                        if (item.getProgress() >= minProg && item.getProgress() < maxProg) {
                            NBTTagCompound itemNbt = item.writeToNBT();
                            itemNbt.setDouble("LocalProgress", item.getProgress() - blockIndex);
                            itemList.appendTag(itemNbt);
                        }
                    }
                }

                if (itemList.tagCount() > 0) {
                    nbt.setTag("BeltItems", itemList);
                }
            }
        }

        return nbt;
    }

    @Override
    public void readFromNBT(NBTTagCompound nbt) {
        super.readFromNBT(nbt);

        if (nbt.hasKey("BeltItems")) {
            pendingItems = nbt.copy();
            restored = false;
            restoreAttempts = 0;
        }
    }

    private boolean restorePendingItems() {
        if (pendingItems == null) return true;
        if (!pendingItems.hasKey("BeltItems")) return true;

        BeltSegment segment = BeltSegmentManager.getOrCreateSegment(world, pos);
        if (segment == null) {
            return false;
        }

        int blockIndex = segment.getBlockIndex(pos);
        if (blockIndex < 0) {
            return false;
        }

        NBTTagList itemList = pendingItems.getTagList("BeltItems", Constants.NBT.TAG_COMPOUND);
        int restoredCount = 0;

        for (int i = 0; i < itemList.tagCount(); i++) {
            NBTTagCompound itemNbt = itemList.getCompoundTagAt(i);
            BeltItemData item = BeltItemData.readFromNBT(itemNbt);

            double localProgress = itemNbt.getDouble("LocalProgress");
            double globalProgress = blockIndex + localProgress;

            item.setProgress(globalProgress);

            if (!item.getStack().isEmpty()) {
                BeltLane lane = segment.getLane(item.getLane());
                if (lane.findByUid(item.getUniqueId()) == null) {
                    if (segment.insertItemDirect(item)) {
                        restoredCount++;
                    }
                }
            }
        }

        if (restoredCount > 0) {
            segment.markDirty();
        }

        return true;
    }

    @Override
    public void invalidate() {
        super.invalidate();
    }
}