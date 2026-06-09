package com.hbm.tileentity.conductor.legacy;

import com.hbm.tileentity.conductor.TileEntityFFDuctBaseMk2;
import net.minecraft.nbt.NBTTagCompound;

public class TileEntityLegacySuccDummy extends TileEntityFFDuctBaseMk2 {

    @Override
    public void readFromNBT(NBTTagCompound compound) {
        super.readFromNBT(compound);

        this.extractionMode = true;
        this.throughput = -1;

        this.markDirty();
    }
}