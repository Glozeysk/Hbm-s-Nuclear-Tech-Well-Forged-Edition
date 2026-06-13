package com.hbm.handler.ability;

import com.hbm.config.ToolConfig;
import com.hbm.inventory.CentrifugeRecipes;
import com.hbm.inventory.ShredderRecipes;
import com.hbm.items.ModItems;
import com.hbm.items.tool.ItemToolAbility;
import com.hbm.util.EnchantmentUtil;
import net.minecraft.block.Block;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.init.Enchantments;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.FurnaceRecipes;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.oredict.OreDictionary;

import java.util.List;

public interface IToolHarvestAbility extends IBaseAbility {

    default void preHarvestAll(int level, World world, EntityPlayer player, EnumHand hand) { }
    default void postHarvestAll(int level, World world, EntityPlayer player, EnumHand hand) { }

    default void onHarvestBlock(int level, World world, int x, int y, int z, EntityPlayer player, Block block, int meta, EnumHand hand) {
        harvestBlock(false, world, x, y, z, player, hand);
    }

    static void harvestBlock(boolean skipDefaultDrops, World world, int x, int y, int z, EntityPlayer player, EnumHand hand) {
        if (skipDefaultDrops) {
            world.setBlockToAir(new BlockPos(x, y, z));
            ItemStack stack = player.getHeldItem(hand);
            if (!stack.isEmpty()) stack.damageItem(1, player);
        } else if (player instanceof EntityPlayerMP) {
            ItemToolAbility.standardDigPost(world, x, y, z, (EntityPlayerMP) player, hand);
        }
    }

    int SORT_ORDER_BASE = 100;

    IToolHarvestAbility NONE = new IToolHarvestAbility() {
        @Override
        public String getName() {
            return "";
        }

        @Override
        public int sortOrder() {
            return SORT_ORDER_BASE;
        }
    };

    IToolHarvestAbility SILK = new IToolHarvestAbility() {
        @Override
        public String getName() {
            return "tool.ability.silktouch";
        }

        @Override
        public boolean isAllowed() {
            return ToolConfig.abilitySilk;
        }

        @Override
        public int sortOrder() {
            return SORT_ORDER_BASE + 1;
        }

        @Override
        public void preHarvestAll(int level, World world, EntityPlayer player, EnumHand hand) {
            ItemStack stack = player.getHeldItem(hand);
            if (!stack.isEmpty()) {
                EnchantmentUtil.applyTemporaryEnchantment(stack, Enchantments.SILK_TOUCH, 1, "prevSilk");
            }
        }

        @Override
        public void postHarvestAll(int level, World world, EntityPlayer player, EnumHand hand) {
            ItemStack stack = player.getHeldItem(hand);
            if (!stack.isEmpty()) {
                EnchantmentUtil.restoreTemporaryEnchantment(stack, Enchantments.SILK_TOUCH, "prevSilk");
            }
        }
    };

    IToolHarvestAbility LUCK = new IToolHarvestAbility() {
        @Override
        public String getName() {
            return "tool.ability.luck";
        }

        @Override
        public boolean isAllowed() {
            return ToolConfig.abilityLuck;
        }

        @Override
        public int levels() {
            return Integer.MAX_VALUE;
        }

        @Override
        public String getExtension(int level) {
            return level == 0 ? "" : " (" + level + ")";
        }

        @Override
        public int sortOrder() {
            return SORT_ORDER_BASE + 2;
        }

        @Override
        public void preHarvestAll(int level, World world, EntityPlayer player, EnumHand hand) {
            ItemStack stack = player.getHeldItem(hand);
            if (!stack.isEmpty()) {
                EnchantmentUtil.applyTemporaryEnchantment(stack, Enchantments.FORTUNE, Math.max(1, level), "prevFortune");
            }
        }

        @Override
        public void postHarvestAll(int level, World world, EntityPlayer player, EnumHand hand) {
            ItemStack stack = player.getHeldItem(hand);
            if (!stack.isEmpty()) {
                EnchantmentUtil.restoreTemporaryEnchantment(stack, Enchantments.FORTUNE, "prevFortune");
            }
        }
    };

    IToolHarvestAbility SMELTER = new IToolHarvestAbility() {
        @Override
        public String getName() {
            return "tool.ability.smelter";
        }

        @Override
        public boolean isAllowed() {
            return ToolConfig.abilityFurnace;
        }

        @Override
        public int sortOrder() {
            return SORT_ORDER_BASE + 3;
        }

        @Override
        public void onHarvestBlock(int level, World world, int x, int y, int z, EntityPlayer player, Block block, int meta, EnumHand hand) {
            List<ItemStack> drops = block.getDrops(world, new BlockPos(x, y, z), world.getBlockState(new BlockPos(x, y, z)), 0);

            boolean doesSmelt = false;

            for (int i = 0; i < drops.size(); i++) {
                ItemStack stack = drops.get(i).copy();
                ItemStack result = FurnaceRecipes.instance().getSmeltingResult(stack);

                if (!result.isEmpty()) {
                    result = result.copy();
                    result.setCount(result.getCount() * stack.getCount());
                    drops.set(i, result);
                    doesSmelt = true;
                }
            }

            harvestBlock(doesSmelt, world, x, y, z, player, hand);

            if (doesSmelt) {
                for (ItemStack stack : drops) {
                    world.spawnEntity(new EntityItem(world, ItemToolAbility.dropX + 0.5, ItemToolAbility.dropY + 0.5, ItemToolAbility.dropZ + 0.5, stack.copy()));
                }
            }
        }
    };

    IToolHarvestAbility SHREDDER = new IToolHarvestAbility() {
        @Override
        public String getName() {
            return "tool.ability.shredder";
        }

        @Override
        public boolean isAllowed() {
            return ToolConfig.abilityShredder;
        }

        @Override
        public int sortOrder() {
            return SORT_ORDER_BASE + 4;
        }

        @Override
        public void onHarvestBlock(int level, World world, int x, int y, int z, EntityPlayer player, Block block, int meta, EnumHand hand) {
            if (block == Blocks.LIT_REDSTONE_ORE) block = Blocks.REDSTONE_ORE;

            ItemStack stack = new ItemStack(block, 1, meta);
            ItemStack result = ShredderRecipes.getShredderResult(stack);

            boolean doesShred = !result.isEmpty() && result.getItem() != ModItems.scrap;

            harvestBlock(doesShred, world, x, y, z, player, hand);

            if (doesShred) {
                world.spawnEntity(new EntityItem(world, ItemToolAbility.dropX + 0.5, ItemToolAbility.dropY + 0.5, ItemToolAbility.dropZ + 0.5, result.copy()));
            }
        }
    };

    IToolHarvestAbility CENTRIFUGE = new IToolHarvestAbility() {
        @Override
        public String getName() {
            return "tool.ability.centrifuge";
        }

        @Override
        public boolean isAllowed() {
            return ToolConfig.abilityCentrifuge;
        }

        @Override
        public int sortOrder() {
            return SORT_ORDER_BASE + 5;
        }

        @Override
        public void onHarvestBlock(int level, World world, int x, int y, int z, EntityPlayer player, Block block, int meta, EnumHand hand) {
            if (block == Blocks.LIT_REDSTONE_ORE) block = Blocks.REDSTONE_ORE;

            ItemStack stack = new ItemStack(block, 1, meta);
            ItemStack[] result = CentrifugeRecipes.getOutput(stack);

            boolean doesCentrifuge = result != null;

            harvestBlock(doesCentrifuge, world, x, y, z, player, hand);

            if (doesCentrifuge) {
                for (ItemStack st : result) {
                    if (st != null) {
                        world.spawnEntity(new EntityItem(world, ItemToolAbility.dropX + 0.5, ItemToolAbility.dropY + 0.5, ItemToolAbility.dropZ + 0.5, st.copy()));
                    }
                }
            }
        }
    };

    IToolHarvestAbility CRYSTALLIZER = new IToolHarvestAbility() {
        @Override
        public String getName() {
            return "tool.ability.crystallizer";
        }

        @Override
        public boolean isAllowed() {
            return ToolConfig.abilityCrystallizer;
        }

        @Override
        public int sortOrder() {
            return SORT_ORDER_BASE + 6;
        }

        @Override
        public void onHarvestBlock(int level, World world, int x, int y, int z, EntityPlayer player, Block block, int meta, EnumHand hand) {
            if (block == Blocks.LIT_REDSTONE_ORE) block = Blocks.REDSTONE_ORE;

            ItemStack stack = new ItemStack(block, 1, meta);
            ItemStack crystal = ItemStack.EMPTY;
            for (int id : OreDictionary.getOreIDs(stack)) {
                String name = OreDictionary.getOreName(id);
                if (name != null && name.startsWith("ore") && name.length() > 3) {
                    String crystalName = "crystal" + name.substring(3);
                    List<ItemStack> list = OreDictionary.getOres(crystalName);
                    for (ItemStack is : list) {
                        if (is.getItem().getRegistryName().getNamespace().equals("hbm")) {
                            crystal = is.copy();
                            crystal.setCount(1);
                            break;
                        }
                    }
                }
                if (!crystal.isEmpty()) break;
            }

            boolean doesCrystallize = !crystal.isEmpty();

            harvestBlock(doesCrystallize, world, x, y, z, player, hand);

            if (doesCrystallize) {
                world.spawnEntity(new EntityItem(world, ItemToolAbility.dropX + 0.5, ItemToolAbility.dropY + 0.5, ItemToolAbility.dropZ + 0.5, crystal.copy()));
            }
        }
    };

    IToolHarvestAbility MERCURY = new IToolHarvestAbility() {
        @Override
        public String getName() {
            return "tool.ability.mercury";
        }

        @Override
        public boolean isAllowed() {
            return ToolConfig.abilityMercury;
        }

        @Override
        public int sortOrder() {
            return SORT_ORDER_BASE + 7;
        }

        @Override
        public void onHarvestBlock(int level, World world, int x, int y, int z, EntityPlayer player, Block block, int meta, EnumHand hand) {
            if (block == Blocks.LIT_REDSTONE_ORE) block = Blocks.REDSTONE_ORE;

            int mercury = 0;

            if (block == Blocks.REDSTONE_ORE)
                mercury = player.getRNG().nextInt(5) + 4;
            if (block == Blocks.REDSTONE_BLOCK)
                mercury = player.getRNG().nextInt(7) + 8;

            boolean doesConvert = mercury > 0;

            harvestBlock(doesConvert, world, x, y, z, player, hand);

            if (doesConvert) {
                world.spawnEntity(new EntityItem(world, ItemToolAbility.dropX + 0.5, ItemToolAbility.dropY + 0.5, ItemToolAbility.dropZ + 0.5, new ItemStack(ModItems.nugget_mercury, mercury)));
            }
        }
    };

    IToolHarvestAbility[] abilities = { NONE, SILK, LUCK, SMELTER, SHREDDER, CENTRIFUGE, CRYSTALLIZER, MERCURY };

    static IToolHarvestAbility getByName(String name) {
        for (IToolHarvestAbility ability : abilities) {
            if (ability.getName().equals(name))
                return ability;
        }
        return NONE;
    }
}