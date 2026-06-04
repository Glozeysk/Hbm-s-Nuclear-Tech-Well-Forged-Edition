package com.hbm.handler;

import com.hbm.lib.RefStrings;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraftforge.event.world.ChunkDataEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.util.HashMap;
import java.util.Map;

public class LegacyPipeMigrationHandler {

    private static final String[] OLD_SUCC_IDS = {
            RefStrings.MODID + ":tileentity_ff_succ_mk2",
            RefStrings.MODID + ":tileentity_ff_succ_mk3",
            RefStrings.MODID + ":tileentity_ff_succ_mk4",
            RefStrings.MODID + ":tileentity_ff_succ_mk2_solid",
            RefStrings.MODID + ":tileentity_ff_succ_mk3_solid"
    };

    private static final String[] OLD_DUCT_IDS = {
            RefStrings.MODID + ":tileentity_ff_fluid_duct_mk2",
            RefStrings.MODID + ":tileentity_ff_fluid_duct_mk3",
            RefStrings.MODID + ":tileentity_ff_fluid_duct_mk4",
            RefStrings.MODID + ":tileentity_ff_fluid_duct_mk2_solid",
            RefStrings.MODID + ":tileentity_ff_fluid_duct_mk3_solid",
            RefStrings.MODID + ":tileentity_ff_fludi_duct_mk2",
            RefStrings.MODID + ":tileentity_ff_fludi_duct_mk3",
            RefStrings.MODID + ":tileentity_ff_fludi_duct_mk4",
            RefStrings.MODID + ":tileentity_ff_fludi_duct_mk2_solid",
            RefStrings.MODID + ":tileentity_ff_fludi_duct_mk3_solid"
    };

    private static final String NEW_DUCT_MK2_ID = RefStrings.MODID + ":tileentity_ff_fludi_duct_mk2";
    private static final String NEW_DUCT_MK3_ID = RefStrings.MODID + ":tileentity_ff_fludi_duct_mk3";
    private static final String NEW_DUCT_MK4_ID = RefStrings.MODID + ":tileentity_ff_fludi_duct_mk4";

    @SubscribeEvent
    public void onChunkLoad(ChunkDataEvent.Load event) {
        NBTTagCompound chunkNBT = event.getData();
        if (!chunkNBT.hasKey("TileEntities")) return;

        NBTTagList tileEntities = chunkNBT.getTagList("TileEntities", 10);
        boolean needsSave = false;

        for (int i = 0; i < tileEntities.tagCount(); i++) {
            NBTTagCompound teNBT = tileEntities.getCompoundTagAt(i);
            String id = teNBT.getString("id");

            if (matchesAny(id, OLD_SUCC_IDS)) {
                String newId = getNewIdForTier(id, "2", NEW_DUCT_MK2_ID, "3", NEW_DUCT_MK3_ID, "4", NEW_DUCT_MK4_ID);
                teNBT.setString("id", newId);
                teNBT.setBoolean("extractionMode", true);
                teNBT.setBoolean("migratedFromLegacy", true); // <-- ФЛАГ МИГРАЦИИ
                if (!teNBT.hasKey("throughput")) {
                    teNBT.setInteger("throughput", 50000);
                }
                needsSave = true;
            }
            else if (matchesAny(id, OLD_DUCT_IDS)) {
                String newId = getNewIdForTier(id, "2", NEW_DUCT_MK2_ID, "3", NEW_DUCT_MK3_ID, "4", NEW_DUCT_MK4_ID);
                teNBT.setString("id", newId);
                teNBT.setBoolean("extractionMode", false);
                teNBT.setBoolean("migratedFromLegacy", true); // <-- ФЛАГ МИГРАЦИИ
                if (!teNBT.hasKey("throughput")) {
                    teNBT.setInteger("throughput", 50000);
                }
                needsSave = true;
            }
        }

        if (needsSave) {
            event.getChunk().setModified(true);
        }
    }

    private boolean matchesAny(String id, String[] array) {
        for (String oldId : array) {
            if (oldId.equals(id)) return true;
        }
        return false;
    }

    private String getNewIdForTier(String oldId, String tier2, String newId2, String tier3, String newId3, String tier4, String newId4) {
        if (oldId.contains("_mk2") || oldId.contains("_mk2_")) return newId2;
        if (oldId.contains("_mk3") || oldId.contains("_mk3_")) return newId3;
        if (oldId.contains("_mk4") || oldId.contains("_mk4_")) return newId4;
        return newId2;
    }
}