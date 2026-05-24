package com.hbm.handler.ability;

import com.hbm.handler.ToolAbility;
import com.hbm.items.tool.IItemAbility;
import com.hbm.items.tool.ItemToolAbility;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.Arrays;
import java.util.Objects;

public class ToolAbilityAdapter implements IToolAreaAbility {
    private final ToolAbility legacy;
    private final int[] params;

    public ToolAbilityAdapter(ToolAbility legacy, int... params) {
        this.legacy = legacy;
        this.params = params.length > 0 ? params : new int[]{0};
    }

    @Override public String getName() { return legacy.getName(); }
    @Override public String getExtension(int level) {
        int param = level < params.length ? params[level] : params[params.length - 1];
        return " (" + param + ")";
    }
    @Override public int levels() { return params.length; }
    @Override public boolean isAllowed() { return legacy.isAllowed(); }

    @Override
    public boolean onDig(int level, World world, BlockPos pos, EntityPlayer player, ItemToolAbility tool) {
        int param = level < params.length ? params[level] : params[params.length - 1];
        ToolAbility temp = createLegacyWithParam(legacy.getClass(), param);
        if (temp != null) {
            temp.onDig(world, pos.getX(), pos.getY(), pos.getZ(), player,
                    world.getBlockState(pos), (IItemAbility) tool, EnumHand.MAIN_HAND);
        }
        return legacy instanceof ToolAbility.ExplosionAbility;
    }

    private ToolAbility createLegacyWithParam(Class<? extends ToolAbility> clazz, int param) {
        try {
            if (clazz == ToolAbility.RecursionAbility.class) return new ToolAbility.RecursionAbility(param);
            if (clazz == ToolAbility.HammerAbility.class) return new ToolAbility.HammerAbility(param);
            if (clazz == ToolAbility.LuckAbility.class) return new ToolAbility.LuckAbility(param);
            if (clazz == ToolAbility.ExplosionAbility.class) return new ToolAbility.ExplosionAbility((float) param);
        } catch (Exception e) { e.printStackTrace(); }
        return null;
    }

    @Override public int sortOrder() { return legacy.hashCode(); }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        ToolAbilityAdapter that = (ToolAbilityAdapter) o;
        return Objects.equals(legacy, that.legacy) && Objects.deepEquals(params, that.params);
    }

    @Override
    public int hashCode() {
        return Objects.hash(legacy, Arrays.hashCode(params));
    }
}