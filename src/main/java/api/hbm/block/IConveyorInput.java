package api.hbm.block;

import net.minecraft.item.ItemStack;

public interface IConveyorInput {
    int tryInsertDirect(ItemStack stack);

    default int tryInsertDirect(ItemStack stack, int sourceLane) {
        return tryInsertDirect(stack);
    }

    boolean canAcceptAny();
}