package com.hbm.handler.ability;

import com.hbm.handler.ToolPreset;
import com.hbm.main.MainRegistry;
import com.hbm.util.I18nUtil;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.*;
import java.util.stream.Collectors;

public class AvailableAbilities {
    // Insertion order matters
    private HashMap<IBaseAbility, Integer> abilities = new HashMap<IBaseAbility, Integer>();

    public AvailableAbilities() { }

    public AvailableAbilities addAbility(IBaseAbility ability, int level) {
        if(level < 0) level = 0;
        abilities.put(ability, level);
        return this;
    }

    public AvailableAbilities addToolAbilities() {
        addAbility(IToolAreaAbility.NONE, 0);
        addAbility(IToolHarvestAbility.NONE, 0);
        return this;
    }

    public AvailableAbilities removeAbility(IBaseAbility ability) {
        abilities.remove(ability);
        return this;
    }

    public boolean supportsAbility(IBaseAbility ability) {
        return abilities.containsKey(ability);
    }

    public int maxLevel(IBaseAbility ability) {
        return abilities.getOrDefault(ability, -1);
    }

    public Map<IBaseAbility, Integer> get() {
        return Collections.unmodifiableMap(abilities);
    }

    public Map<IWeaponAbility, Integer> getWeaponAbilities() {
        return abilities.keySet().stream().filter(a -> a instanceof IWeaponAbility).collect(Collectors.toMap(a -> (IWeaponAbility) a, a -> abilities.get(a)));
    }

    public Map<IBaseAbility, Integer> getToolAbilities() {
        return abilities.keySet().stream().filter(a -> a instanceof IToolAreaAbility || a instanceof IToolHarvestAbility).collect(Collectors.toMap(a -> a, a -> abilities.get(a)));
    }

    public Map<IToolAreaAbility, Integer> getToolAreaAbilities() {
        return abilities.keySet().stream().filter(a -> a instanceof IToolAreaAbility).collect(Collectors.toMap(a -> (IToolAreaAbility) a, a -> abilities.get(a)));
    }

    public Map<IToolHarvestAbility, Integer> getToolHarvestAbilities() {
        return abilities.keySet().stream().filter(a -> a instanceof IToolHarvestAbility).collect(Collectors.toMap(a -> (IToolHarvestAbility) a, a -> abilities.get(a)));
    }

    public HashMap<IBaseAbility, Integer> getAbilities() {
        return abilities;
    }

    public boolean hasAnyRealAbility() {
        return abilities.keySet().stream()
                .anyMatch(a -> a != IToolAreaAbility.NONE && a != IToolHarvestAbility.NONE);
    }

    public int size() {
        return abilities.size();
    }

    public boolean isEmpty() {
        return abilities.isEmpty();
    }

    @SideOnly(Side.CLIENT)
    public void addInformation(List<String> list, ToolPreset activePreset) {
        Comparator<Map.Entry<IBaseAbility, Integer>> abilityComparator =
                Comparator.comparing(Map.Entry<IBaseAbility, Integer>::getKey)
                        .thenComparing(Map.Entry::getValue);

        List<Map.Entry<IBaseAbility, Integer>> toolAbilities = abilities.entrySet().stream()
                .filter(entry -> (entry.getKey() instanceof IToolAreaAbility && entry.getKey() != IToolAreaAbility.NONE)
                        || (entry.getKey() instanceof IToolHarvestAbility && entry.getKey() != IToolHarvestAbility.NONE))
                .sorted(abilityComparator)
                .collect(Collectors.toList());

        if (!toolAbilities.isEmpty()) {
            list.add(I18nUtil.resolveKey("tool.ability.title"));

            for (Map.Entry<IBaseAbility, Integer> entry : toolAbilities) {
                IBaseAbility ability = entry.getKey();
                int maxLevel = entry.getValue();
                boolean isActive = false;
                int activeLevel = 0;

                if (activePreset != null && !activePreset.isNone()) {
                    if (ability instanceof IToolAreaAbility) {
                        if (ability == activePreset.areaAbility) {
                            isActive = true;
                            activeLevel = activePreset.areaAbilityLevel;
                        }
                    } else if (ability instanceof IToolHarvestAbility) {
                        if (ability == activePreset.harvestAbility) {
                            isActive = true;
                            activeLevel = activePreset.harvestAbilityLevel;
                        }
                    }
                }

                String line;
                if (isActive) {
                    line = " " + TextFormatting.YELLOW + TextFormatting.BOLD + ">" + ability.getFullName(activeLevel);
                } else {
                    line = "  " + TextFormatting.GOLD + ability.getFullName(maxLevel);
                }
                list.add(line);
            }

            list.add(I18nUtil.resolveKey("tooltip.ability.cycle"));
            list.add(I18nUtil.resolveKey("tooltip.ability.sneak"));
            list.add(I18nUtil.resolveKey("tooltip.ability.alt"));
        }

        List<Map.Entry<IBaseAbility, Integer>> weaponAbilities = abilities.entrySet().stream()
                .filter(entry -> entry.getKey() instanceof IWeaponAbility && entry.getKey() != IWeaponAbility.NONE)
                .sorted(abilityComparator)
                .collect(Collectors.toList());

        if (!weaponAbilities.isEmpty()) {
            list.add("Weapon modifiers: ");
            for (Map.Entry<IBaseAbility, Integer> entry : weaponAbilities) {
                IBaseAbility ability = entry.getKey();
                int level = entry.getValue();
                list.add("  " + TextFormatting.RED + ability.getFullName(level));
            }
        }
    }
}
