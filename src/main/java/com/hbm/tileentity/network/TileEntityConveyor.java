package com.hbm.tileentity.network;

import com.hbm.blocks.network.BlockConveyor;
import com.hbm.blocks.network.conveyor.BeltItemData;
import com.hbm.blocks.network.conveyor.BeltLane;
import com.hbm.blocks.network.conveyor.BeltSegment;
import com.hbm.blocks.network.conveyor.BeltSegmentManager;
import net.minecraft.block.Block;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ITickable;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.common.util.Constants;

import java.util.ArrayList;
import java.util.List;

public class TileEntityConveyor extends TileEntity implements ITickable {

    private NBTTagCompound pendingItems = null;

    @Override
    public void update() {
        if (world.isRemote) return;

        if (pendingItems != null) {
            restorePendingItems();
            pendingItems = null;
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
        }
    }

    private void restorePendingItems() {
        if (pendingItems == null) return;
        if (!pendingItems.hasKey("BeltItems")) return;

        BeltSegment segment = BeltSegmentManager.getOrCreateSegment(world, pos);
        if (segment == null) return;

        int blockIndex = segment.getBlockIndex(pos);
        if (blockIndex < 0) return;

        NBTTagList itemList = pendingItems.getTagList("BeltItems", Constants.NBT.TAG_COMPOUND);
        for (int i = 0; i < itemList.tagCount(); i++) {
            NBTTagCompound itemNbt = itemList.getCompoundTagAt(i);
            BeltItemData item = BeltItemData.readFromNBT(itemNbt);

            double localProgress = itemNbt.getDouble("LocalProgress");
            double globalProgress = blockIndex + localProgress;

            item.setProgress(globalProgress);

            if (!item.getStack().isEmpty()) {
                segment.insertItemDirect(item);
            }
        }

        segment.markDirty();
    }

    @Override
    public void invalidate() {
        super.invalidate();
    }
}