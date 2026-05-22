package com.hbm.blocks.machine;

import java.util.Random;

import javax.annotation.Nullable;

import com.hbm.blocks.BlockDummyable;

import net.minecraft.block.Block;
import net.minecraft.block.BlockLadder;
import net.minecraft.block.SoundType;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.util.EnumBlockRenderType;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.Explosion;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

public class MachineLadder extends BlockLadder {

    private final java.util.function.Supplier<Block> ownerSupplier;
    private Block ownerCache;

    public MachineLadder(String name, java.util.function.Supplier<Block> ownerSupplier) {
        this.setTranslationKey(name);
        this.setRegistryName(name);
        this.setHardness(-1.0F);
        this.setResistance(3600000.0F);
        this.setSoundType(SoundType.METAL);
        this.setTickRandomly(false);
        this.ownerSupplier = ownerSupplier;
    }

    public Block getOwnerBlock() {
        if(ownerCache == null) {
            ownerCache = ownerSupplier.get();
        }
        return ownerCache;
    }

    private boolean hasCoreNearby(World world, BlockPos pos) {
        Block owner = getOwnerBlock();
        int radius = 6;

        for(int dx = -radius; dx <= radius; dx++) {
            for(int dy = -radius; dy <= radius; dy++) {
                for(int dz = -radius; dz <= radius; dz++) {
                    BlockPos check = pos.add(dx, dy, dz);

                    if(!world.isBlockLoaded(check))
                        continue;

                    IBlockState state = world.getBlockState(check);

                    if(state.getBlock() == owner) {
                        try {
                            Integer meta = state.getValue(BlockDummyable.META);
                            if(meta != null && meta >= 12) {
                                return true;
                            }
                        } catch(Exception ignored) {
                            return true;
                        }
                    }
                }
            }
        }

        return false;
    }

    @Override
    public float getExplosionResistance(World world, BlockPos pos, @Nullable Entity exploder, Explosion explosion) {
        if(hasCoreNearby(world, pos)) {
            return 3600000.0F;
        }
        return super.getExplosionResistance(world, pos, exploder, explosion);
    }

    @Override
    public void onBlockExploded(World world, BlockPos pos, Explosion explosion) {
        if(hasCoreNearby(world, pos)) {
            return;
        }
        super.onBlockExploded(world, pos, explosion);
    }

    @Override
    public boolean canDropFromExplosion(Explosion explosion) {
        return false;
    }

    @Override
    public boolean isOpaqueCube(IBlockState state) {
        return false;
    }

    @Override
    public boolean isFullCube(IBlockState state) {
        return false;
    }

    @Override
    public EnumBlockRenderType getRenderType(IBlockState state) {
        return EnumBlockRenderType.INVISIBLE;
    }

    @Override
    public AxisAlignedBB getBoundingBox(IBlockState state, IBlockAccess world, BlockPos pos) {
        return super.getBoundingBox(state, world, pos);
    }

    @Nullable
    @Override
    public AxisAlignedBB getCollisionBoundingBox(IBlockState state, IBlockAccess world, BlockPos pos) {
        return NULL_AABB;
    }

    @Nullable
    @Override
    public RayTraceResult collisionRayTrace(IBlockState state, World world, BlockPos pos, Vec3d start, Vec3d end) {
        return null;
    }

    @Override
    public boolean canPlaceBlockOnSide(World world, BlockPos pos, EnumFacing side) {
        return true;
    }

    @Override
    public boolean canPlaceBlockAt(World world, BlockPos pos) {
        return true;
    }

    @Override
    public void neighborChanged(IBlockState state, World world, BlockPos pos, Block blockIn, BlockPos fromPos) {
    }

    public void onBlockDestroyedByPlayer(World world, BlockPos pos, IBlockState state) {
        if(!world.isRemote) {
            scheduleRestore(world, pos);
        }
    }

    public void onBlockDestroyedByExplosion(World world, BlockPos pos, Explosion explosion) {
        if(!world.isRemote) {
            scheduleRestore(world, pos);
        }
    }

    private void scheduleRestore(World world, BlockPos pos) {
        Block owner = getOwnerBlock();
        int radius = 6;

        for(int dx = -radius; dx <= radius; dx++) {
            for(int dy = -radius; dy <= radius; dy++) {
                for(int dz = -radius; dz <= radius; dz++) {
                    BlockPos neighbor = pos.add(dx, dy, dz);

                    if(!world.isBlockLoaded(neighbor))
                        continue;

                    IBlockState state = world.getBlockState(neighbor);
                    if(state.getBlock() == owner) {
                        world.scheduleUpdate(neighbor, owner, 10);
                        return;
                    }
                }
            }
        }
    }

    @Override
    public boolean isLadder(IBlockState state, IBlockAccess world, BlockPos pos, EntityLivingBase entity) {
        return true;
    }

    @Override
    public boolean isPassable(IBlockAccess world, BlockPos pos) {
        return true;
    }

    public float getPlayerRelativeBlockHardness(EntityPlayer player, World world, BlockPos pos) {
        return 0.0F;
    }

    @Override
    public boolean canEntityDestroy(IBlockState state, IBlockAccess world, BlockPos pos, Entity entity) {
        return false;
    }

    @Override
    public int quantityDropped(Random random) {
        return 0;
    }

    @Override
    public Item getItemDropped(IBlockState state, Random rand, int fortune) {
        return Items.AIR;
    }

    @Override
    public boolean isReplaceable(IBlockAccess world, BlockPos pos) {
        return true;
    }
}