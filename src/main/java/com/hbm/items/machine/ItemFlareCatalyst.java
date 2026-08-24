package com.hbm.items.machine;

import com.hbm.items.ItemBase;
import net.minecraft.item.ItemStack;

public class ItemFlareCatalyst extends ItemBase {

    public enum CatalystType {
        NONE,
        FILTER,
        BARRIER
    }

    private final CatalystType catalystType;

    public ItemFlareCatalyst(String name, CatalystType type) {
        super(name);
        this.catalystType = type;
        this.setMaxStackSize(1);
    }

    public CatalystType getCatalystType() {
        return this.catalystType;
    }

    public static CatalystType getType(ItemStack stack) {
        if (stack.isEmpty() || !(stack.getItem() instanceof ItemFlareCatalyst)) {
            return CatalystType.NONE;
        }
        return ((ItemFlareCatalyst) stack.getItem()).getCatalystType();
    }
}