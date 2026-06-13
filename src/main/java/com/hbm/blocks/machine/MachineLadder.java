package com.hbm.blocks.machine;

import java.util.Random;
import java.util.function.Supplier;

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

    private final Supplier<Block> ownerSupplier;
    private Block ownerCache;
    private static final int SEARCH_RADIUS = 6;

    public MachineLadder(String name, Supplier<Block> ownerSupplier) {
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

    @Nullable
    private BlockPos findCore(World world, BlockPos pos) {
        Block owner = getOwnerBlock();

        for(int dx = -SEARCH_RADIUS; dx <= SEARCH_RADIUS; dx++) {
            for(int dy = -SEARCH_RADIUS; dy <= SEARCH_RADIUS; dy++) {
                for(int dz = -SEARCH_RADIUS; dz <= SEARCH_RADIUS; dz++) {
                    BlockPos check = pos.add(dx, dy, dz);

                    if(!world.isBlockLoaded(check))
                        continue;

                    IBlockState state = world.getBlockState(check);

                    if(state.getBlock() != owner)
                        continue;

                    try {
                        Integer meta = state.getValue(BlockDummyable.META);
                        if(meta != null && meta >= 12) {
                            return check;
                        }
                    } catch(Exception ignored) {
                        return check;
                    }
                }
            }
        }

        return null;
    }

    @Override
    public void breakBlock(World world, BlockPos pos, IBlockState state) {
        if(!world.isRemote) {
            BlockPos corePos = findCore(world, pos);
            if(corePos != null) {
                Block coreBlock = world.getBlockState(corePos).getBlock();
                world.scheduleUpdate(corePos, coreBlock, 5);
            }
        }
        super.breakBlock(world, pos, state);
    }

    @Override
    public float getExplosionResistance(World world, BlockPos pos,
                                        @Nullable Entity exploder, Explosion explosion) {
        if(findCore(world, pos) != null) {
            return 3600000.0F;
        }
        return super.getExplosionResistance(world, pos, exploder, explosion);
    }

    @Override
    public void onBlockExploded(World world, BlockPos pos, Explosion explosion) {
        if(findCore(world, pos) != null) {
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

    @Nullable
    @Override
    public AxisAlignedBB getCollisionBoundingBox(IBlockState state,
                                                 IBlockAccess world, BlockPos pos) {
        return NULL_AABB;
    }

    @Nullable
    @Override
    public RayTraceResult collisionRayTrace(IBlockState state, World world,
                                            BlockPos pos, Vec3d start, Vec3d end) {
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
    public void neighborChanged(IBlockState state, World world, BlockPos pos,
                                Block blockIn, BlockPos fromPos) {
    }

    @Override
    public boolean isLadder(IBlockState state, IBlockAccess world,
                            BlockPos pos, EntityLivingBase entity) {
        return true;
    }

    @Override
    public boolean isPassable(IBlockAccess world, BlockPos pos) {
        return true;
    }

    public float getPlayerRelativeBlockHardness(EntityPlayer player,
                                                World world, BlockPos pos) {
        return 0.0F;
    }

    @Override
    public boolean canEntityDestroy(IBlockState state, IBlockAccess world,
                                    BlockPos pos, Entity entity) {
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