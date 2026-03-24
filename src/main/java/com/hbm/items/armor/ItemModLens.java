package com.hbm.items.armor;

import com.hbm.blocks.ModBlocks;
import com.hbm.blocks.generic.BlockBedrockOreTE;
import com.hbm.handler.ArmorModHandler;
import com.hbm.handler.threading.PacketThreading;
import com.hbm.inventory.BedrockOreRegistry;
import com.hbm.items.ISatChip;
import com.hbm.packet.AuxParticlePacketNT;
import com.hbm.render.util.RenderOverhead;
import com.hbm.saveddata.satellites.Satellite;
import com.hbm.saveddata.satellites.SatelliteSavedData;
import com.hbm.saveddata.satellites.SatelliteScanner;
import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.HashMap;
import java.util.List;

public class ItemModLens extends ItemArmorMod implements ISatChip {

    public static int countLimit = 100;
    public static HashMap<Block, Object[]> blockList = new HashMap<>();

    public ItemModLens(String s) {
        super(ArmorModHandler.extra, true, false, false, false, s);
    }

    @Override
    public void addInformation(ItemStack itemstack, World world, List<String> list, ITooltipFlag flag) {
        list.add(TextFormatting.AQUA + "Satellite Frequency: " + this.getFreq(itemstack));
        list.add("");
        super.addInformation(itemstack, world, list, flag);
    }

    @Override
    public void addDesc(List<String> list, ItemStack stack, ItemStack armor) {
        list.add(TextFormatting.AQUA + "  " + stack.getDisplayName() + " (Freq: " + getFreq(stack) + ")");
    }

    public static void initBlockList() {
        blockList.put(ModBlocks.ore_coltan, new Object[]{"tile.ore_coltan.name", 0xffbd54});
        blockList.put(ModBlocks.ore_gneiss_lithium, new Object[]{"tile.ore_gneiss_lithium.name", 0xcef2ee});
        blockList.put(ModBlocks.ore_reiium, new Object[]{"tile.ore_reiium.name", 0xbe0000});
        blockList.put(ModBlocks.ore_weidanium, new Object[]{"tile.ore_weidanium.name", 0xff3f00});
        blockList.put(ModBlocks.ore_australium, new Object[]{"tile.ore_australium.name", 0xffff00});
        blockList.put(ModBlocks.ore_verticium, new Object[]{"tile.ore_verticium.name", 0x00ff00});
        blockList.put(ModBlocks.ore_unobtainium, new Object[]{"tile.ore_unobtainium.name", 0x0059ff});
        blockList.put(ModBlocks.ore_daffergon, new Object[]{"tile.ore_daffergon.name", 0xa500ff});
        blockList.put(Blocks.END_PORTAL_FRAME, new Object[]{"neutrino.end_portal.name", 0x40b080});
        blockList.put(ModBlocks.basalt_gem, new Object[]{"tile.basalt_gem.name", 0xff5000});
        blockList.put(ModBlocks.volcano_core, new Object[]{"tile.volcano_core.name", 0xff4000});
        blockList.put(ModBlocks.pink_log, new Object[]{"tile.pink_log.name", 0xff00ff});
        blockList.put(ModBlocks.crate_red, new Object[]{"tile.crate_red.name", 0xff0000});
        blockList.put(ModBlocks.brick_jungle_circle, new Object[]{"tile.brick_jungle_circle.name", 0xff0000});
        blockList.put(ModBlocks.safe, new Object[]{"tile.safe.name", 0xa0a0a0});
        blockList.put(ModBlocks.statue_elb_f, new Object[]{"ELB", 0x909090});
        blockList.put(ModBlocks.block_euphemium_cluster, new Object[]{"tile.block_euphemium_cluster.name", 0xd2398c});
        blockList.put(ModBlocks.ore_schrabidium, new Object[]{"tile.ore_schrabidium.name", 0x00d0ff});
        blockList.put(ModBlocks.ore_gneiss_schrabidium, new Object[]{"tile.ore_schrabidium.name", 0x00d0ff});
        blockList.put(ModBlocks.ore_nether_schrabidium, new Object[]{"tile.ore_schrabidium.name", 0x00d0ff});
        blockList.put(ModBlocks.taint, new Object[]{"tile.taint.name", 0x00ff74});
        blockList.put(ModBlocks.ore_bedrock_block, new Object[]{"tile.ore_bedrock_block.name", 0xff5900});
        blockList.put(ModBlocks.crashed_balefire, new Object[]{"tile.crashed_bomb.name", 0x00ff00});
    }

    @Override
    public void modUpdate(EntityLivingBase entity, ItemStack armor) {
        World world = entity.world;
        if(world.isRemote) return;
        if(!(entity instanceof EntityPlayerMP player)) return;

        ItemStack lens = ArmorModHandler.pryMods(armor)[ArmorModHandler.extra];
        if(lens == null) return;

        int freq = this.getFreq(lens);
        Satellite sat = SatelliteSavedData.getData(world).getSatFromFreq(freq);
        if(!(sat instanceof SatelliteScanner)) return;

        int x = (int) Math.floor(player.posX);
        int y = (int) Math.floor(player.posY);
        int z = (int) Math.floor(player.posZ);
        int range = 3;

        int cX = x >> 4;
        int cZ = z >> 4;

        int height = Math.max(Math.min(y + 10, 255), 64);
        int layersPerTick = Math.max(height / 20, 1);
        long time = world.getTotalWorldTime();
        int baseSeg = (int) ((time * layersPerTick) % height);

        int hits = 0;

        for(int layer = 0; layer < layersPerTick; layer++) {
            int seg = (baseSeg + layer) % height;

            for(int chunkX = cX - range; chunkX <= cX + range; chunkX++) {
                for(int chunkZ = cZ - range; chunkZ <= cZ + range; chunkZ++) {
                    Chunk c = world.getChunk(chunkX, chunkZ);

                    for(int ix = 0; ix < 16; ix++) {
                        for(int iz = 0; iz < 16; iz++) {

                            Block b = c.getBlockState(ix, seg, iz).getBlock();
                            int aX = (chunkX << 4) + ix;
                            int aZ = (chunkZ << 4) + iz;

                            Object[] highlightData = blockList.get(b);
                            if(highlightData != null && isClusterRepresentative(world, aX, seg, aZ, b)) {
                                String label = (String) highlightData[0];
                                int color = (Integer) highlightData[1];

                                if(b == ModBlocks.ore_bedrock_block) {
                                    TileEntity te = world.getTileEntity(new BlockPos(aX, seg, aZ));
                                    if(te instanceof BlockBedrockOreTE.TileEntityBedrockOre) {
                                        BlockBedrockOreTE.TileEntityBedrockOre oreTE = (BlockBedrockOreTE.TileEntityBedrockOre) te;
                                        if(oreTE.oreName != null) {
                                            String baseName = net.minecraft.util.text.translation.I18n.translateToLocal(label);
                                            label = baseName + ": " + BedrockOreRegistry.getOreName(oreTE.oreName);
                                            color = oreTE.color;
                                        }
                                    }
                                }

                                if(sendMarker(aX, seg, aZ, label, color, player))
                                    hits++;
                            }

                            if(hits > countLimit) return;
                        }
                    }
                }
            }
        }
    }

    private boolean isClusterRepresentative(World world, int x, int y, int z, Block blockType) {
        String oreType = null;
        if(blockType == ModBlocks.ore_bedrock_block) {
            TileEntity te = world.getTileEntity(new BlockPos(x, y, z));
            if(te instanceof BlockBedrockOreTE.TileEntityBedrockOre) {
                oreType = ((BlockBedrockOreTE.TileEntityBedrockOre) te).oreName;
            }
        }

        for(int dx = -1; dx <= 1; dx++) {
            for(int dy = -1; dy <= 1; dy++) {
                for(int dz = -1; dz <= 1; dz++) {
                    if(dx == 0 && dy == 0 && dz == 0) continue;

                    int nx = x + dx, ny = y + dy, nz = z + dz;
                    if(ny < 0 || ny > 255) continue;
                    BlockPos npos = new BlockPos(nx, ny, nz);
                    if(!world.isBlockLoaded(npos)) continue;

                    if(world.getBlockState(npos).getBlock() != blockType) continue;

                    if(oreType != null) {
                        TileEntity nte = world.getTileEntity(npos);
                        if(nte instanceof BlockBedrockOreTE.TileEntityBedrockOre) {
                            String neighborOre = ((BlockBedrockOreTE.TileEntityBedrockOre) nte).oreName;
                            if(!oreType.equals(neighborOre)) continue;
                        } else {
                            continue;
                        }
                    }

                    if(nx < x || (nx == x && ny < y) || (nx == x && ny == y && nz < z)) {
                        return false;
                    }
                }
            }
        }

        return true;
    }

    private boolean sendMarker(int x, int y, int z, String label, int color, EntityPlayerMP player) {
        NBTTagCompound data = new NBTTagCompound();
        data.setString("type", "marker");
        data.setInteger("color", color);
        data.setInteger("expires", 3_000);
        data.setDouble("dist", 300D);
        if(label != null) {
            String translated = net.minecraft.util.text.translation.I18n.translateToLocal(label);
            data.setString("label", translated);
        }
        PacketThreading.createSendToThreadedPacket(new AuxParticlePacketNT(data, x, y, z), player);
        return true;
    }

    @SideOnly(Side.CLIENT)
    public static class ClientMarkerHandler {

        @SubscribeEvent
        public void onClientTick(TickEvent.ClientTickEvent event) {
            if(event.phase != TickEvent.Phase.END) return;
            Minecraft mc = Minecraft.getMinecraft();
            if(mc.world == null || mc.player == null) return;
            if(mc.world.getTotalWorldTime() % 5 != 0) return;

            RenderOverhead.markers.keySet().removeIf(bpos -> {
                if(!mc.world.isBlockLoaded(bpos)) return false;
                Block block = mc.world.getBlockState(bpos).getBlock();
                return !blockList.containsKey(block);
            });
        }
    }
}