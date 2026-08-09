package com.hbm.blocks.network.conveyor.block;

import api.hbm.block.IConveyorBelt;
import api.hbm.block.IConveyorVectorProvider;
import api.hbm.block.IEnterableBlock;
import com.hbm.entity.item.EntityMovingItem;
import com.hbm.tileentity.network.TileEntityConveyor;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.properties.PropertyInteger;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

public class BlockConveyorChute extends BlockConveyor {

    public static final PropertyInteger TYPE = PropertyInteger.create("type", 0, 2);

    public BlockConveyorChute(Material materialIn, String s) {
        super(materialIn, s, () -> new double[]{0.0D}, IConveyorVectorProvider.linear(), BlockConveyor::selectNearestLane);
    }

    @Override
    public boolean usesLaneQueues() {
        return false;
    }

    private boolean shouldGoVertical(World world, BlockPos pos, Vec3d itemPos) {
        Block belowBlock = world.getBlockState(pos.down()).getBlock();
        if (belowBlock instanceof IConveyorBelt || belowBlock instanceof IEnterableBlock) {
            return true;
        }
        return itemPos.y > pos.getY() + 0.25D;
    }

    @Override
    public void onEntityCollision(World world, BlockPos pos, IBlockState state, Entity entity) {
        if (world.isRemote) return;

        if (entity instanceof EntityItem && entity.ticksExisted > 10 && !entity.isDead) {
            EntityMovingItem item = new EntityMovingItem(world);
            item.setItemStack(((EntityItem) entity).getItem());
            Vec3d entityPos = new Vec3d(entity.posX, entity.posY, entity.posZ);
            Vec3d snap = this.getClosestSnappingPosition(world, pos, entityPos);
            item.setPositionAndRotation(snap.x, snap.y, snap.z, 0, 0);
            world.spawnEntity(item);
            entity.setDead();
            return;
        }

        if (shouldGoVertical(world, pos, new Vec3d(entity.posX, entity.posY, entity.posZ))) {
            entity.motionX *= 4.0D;
            entity.motionY *= 4.0D;
            entity.motionZ *= 4.0D;
        } else if (entity.posY > pos.getY() + 0.25D) {
            entity.motionX *= 3.0D;
            entity.motionY *= 3.0D;
            entity.motionZ *= 3.0D;
        }
    }

    @Override
    public Vec3d getTravelLocation(World world, int x, int y, int z, Vec3d itemPos, double speed) {
        BlockPos pos = new BlockPos(x, y, z);

        if (shouldGoVertical(world, pos, itemPos)) {
            double targetY = itemPos.y + speed * 4.0D;
            return new Vec3d(pos.getX() + 0.5D, targetY, pos.getZ() + 0.5D);
        }

        return super.getTravelLocation(world, x, y, z, itemPos, speed);
    }

    public EnumFacing getTravelDirection(World world, BlockPos pos, Vec3d itemPos) {
        if (shouldGoVertical(world, pos, itemPos)) {
            return EnumFacing.UP;
        }
        return world.getBlockState(pos).getValue(FACING);
    }

    @Override
    public void onBlockPlacedBy(World worldIn, BlockPos pos, IBlockState state, EntityLivingBase placer, ItemStack stack) {
        EnumFacing facing = placer.getHorizontalFacing().getOpposite();
        worldIn.setBlockState(pos, state.withProperty(FACING, facing).withProperty(TYPE, getUpdatedType(worldIn, pos, facing)));
    }

    @Override
    public void neighborChanged(IBlockState state, World world, BlockPos pos, Block blockIn, BlockPos fromPos) {
        world.setBlockState(pos, state.withProperty(TYPE, getUpdatedType(world, pos)));
    }

    public int getUpdatedType(World world, BlockPos pos) {
        return getUpdatedType(world, pos, world.getBlockState(pos).getValue(FACING));
    }

    public int getUpdatedType(World world, BlockPos pos, EnumFacing side) {
        boolean hasChuteBelow = world.getBlockState(pos.down()).getBlock() instanceof BlockConveyorChute;
        boolean hasInputBelt = false;
        Block inputBlock = world.getBlockState(pos.offset(side, 1)).getBlock();
        if (inputBlock instanceof IConveyorBelt || inputBlock instanceof IEnterableBlock) {
            hasInputBelt = true;
        }
        if (hasChuteBelow) {
            return hasInputBelt ? 2 : 1;
        }
        return 0;
    }

    @Override
    public void breakBlock(World world, BlockPos pos, IBlockState state) {
        TileEntity te = world.getTileEntity(pos);
        if (te instanceof TileEntityConveyor) {
            ((TileEntityConveyor) te).invalidate();
        }
        super.breakBlock(world, pos, state);
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
    public boolean doesSideBlockRendering(IBlockState state, IBlockAccess world, BlockPos pos, EnumFacing face) {
        return false;
    }

    @Override
    public AxisAlignedBB getBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
        return FULL_BLOCK_AABB;
    }

    @Override
    public BlockRenderLayer getRenderLayer() {
        return BlockRenderLayer.CUTOUT;
    }

    @Override
    public BlockStateContainer createBlockState() {
        return new BlockStateContainer(this, new IProperty[]{FACING, TYPE});
    }

    @Override
    public int getMetaFromState(IBlockState state) {
        return (state.getValue(FACING).getIndex() - 2) + (state.getValue(TYPE) << 2);
    }

    @Override
    public IBlockState getStateFromMeta(int meta) {
        EnumFacing enumfacing = EnumFacing.values()[(meta % 4) + 2];
        return this.getDefaultState().withProperty(FACING, enumfacing).withProperty(TYPE, meta >> 2);
    }
}