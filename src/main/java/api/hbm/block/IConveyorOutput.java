package api.hbm.block;

import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;

public interface IConveyorOutput {
    boolean canExtractFrom(EnumFacing side);
    ItemStack extractItem(EnumFacing side, int maxAmount);
}