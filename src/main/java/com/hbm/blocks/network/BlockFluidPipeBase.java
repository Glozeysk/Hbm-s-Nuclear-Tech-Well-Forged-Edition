package com.hbm.blocks.network;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

import com.hbm.blocks.ILookOverlay;
import com.hbm.blocks.ModBlocks;
import com.hbm.forgefluid.ModForgeFluids;
import com.hbm.tileentity.conductor.TileEntityFFDuctBaseMk2;
import com.hbm.util.I18nUtil;

import api.hbm.block.IToolable;
import api.hbm.block.IToolable.ToolType;
import net.minecraft.block.Block;
import net.minecraft.block.BlockContainer;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.state.BlockFaceShape;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.client.event.RenderGameOverlayEvent.Pre;
import net.minecraftforge.fluids.Fluid;

public abstract class BlockFluidPipeBase extends BlockContainer implements ILookOverlay, IToolable {

    public static final PropertyBool EXTRACTS = PropertyBool.create("extracts");

    private static final double PIPE_MIN = 0.3125D;
    private static final double PIPE_MAX = 0.6875D;

    /*
     * Насколько сильно помогать игроку продолжать прямую трубу.
     * Если все еще иногда ставится вверх - можно поднять до 0.4D.
     * Если слишком агрессивно тянет в торец - уменьшить до 0.3D.
     */
    private static final double CONTINUE_ZONE = 0.375D;

    private static final AxisAlignedBB CENTER_BB =
            new AxisAlignedBB(PIPE_MIN, PIPE_MIN, PIPE_MIN, PIPE_MAX, PIPE_MAX, PIPE_MAX);

    protected static final int[] THROUGHPUT_TIERS = {-1, 50, 100, 500, 1000, 10000, 50000, 100000};

    public BlockFluidPipeBase(Material materialIn, String s) {
        super(materialIn);
        this.setTranslationKey(s);
        this.setRegistryName(s);
        this.setDefaultState(this.blockState.getBaseState().withProperty(EXTRACTS, true));
        ModBlocks.ALL_BLOCKS.add(this);
    }

    @Override
    public void onNeighborChange(IBlockAccess world, BlockPos pos, BlockPos neighbor) {
        TileEntity te = world.getTileEntity(pos);
        if (te instanceof TileEntityFFDuctBaseMk2) {
            ((TileEntityFFDuctBaseMk2) te).onNeighborChange();
        }
    }

    @Override
    public void neighborChanged(IBlockState state, World worldIn, BlockPos pos, Block blockIn, BlockPos fromPos) {
        TileEntity te = worldIn.getTileEntity(pos);
        if (te instanceof TileEntityFFDuctBaseMk2) {
            ((TileEntityFFDuctBaseMk2) te).onNeighborChange();
        }
    }

    @Override
    public IBlockState getActualState(IBlockState state, IBlockAccess worldIn, BlockPos pos) {
        TileEntity te = worldIn.getTileEntity(pos);
        if (te instanceof TileEntityFFDuctBaseMk2) {
            ((TileEntityFFDuctBaseMk2) te).onNeighborChange();
        }
        return state;
    }

    @Override
    public void breakBlock(World worldIn, BlockPos pos, IBlockState state) {
        TileEntity te = worldIn.getTileEntity(pos);
        if (te instanceof TileEntityFFDuctBaseMk2) {
            TileEntityFFDuctBaseMk2.breakBlock(worldIn, pos);
        }
        super.breakBlock(worldIn, pos, state);
    }

    private static int getConnectionMask(TileEntityFFDuctBaseMk2 pipe) {
        boolean up = pipe.connections[0] != null;
        boolean down = pipe.connections[1] != null;
        boolean north = pipe.connections[2] != null;
        boolean east = pipe.connections[3] != null;
        boolean south = pipe.connections[4] != null;
        boolean west = pipe.connections[5] != null;

        return (east ? 32 : 0)
                | (west ? 16 : 0)
                | (up ? 8 : 0)
                | (down ? 4 : 0)
                | (south ? 2 : 0)
                | (north ? 1 : 0);
    }

    private static boolean isStraightX(int mask) {
        return mask == 32 || mask == 16 || mask == 48;
    }

    private static boolean isStraightY(int mask) {
        return mask == 8 || mask == 4 || mask == 12;
    }

    private static boolean isStraightZ(int mask) {
        return mask == 2 || mask == 1 || mask == 3;
    }

    private List<AxisAlignedBB> getPipeBoxes(IBlockAccess world, BlockPos pos) {
        List<AxisAlignedBB> boxes = new ArrayList<>();
        TileEntity te = world.getTileEntity(pos);

        if (!(te instanceof TileEntityFFDuctBaseMk2)) {
            boxes.add(CENTER_BB);
            return boxes;
        }

        TileEntityFFDuctBaseMk2 pipe = (TileEntityFFDuctBaseMk2) te;
        int mask = getConnectionMask(pipe);

        if (isStraightX(mask)) {
            boxes.add(new AxisAlignedBB(0.0D, PIPE_MIN, PIPE_MIN, 1.0D, PIPE_MAX, PIPE_MAX));
            return boxes;
        }
        if (isStraightY(mask)) {
            boxes.add(new AxisAlignedBB(PIPE_MIN, 0.0D, PIPE_MIN, PIPE_MAX, 1.0D, PIPE_MAX));
            return boxes;
        }
        if (isStraightZ(mask)) {
            boxes.add(new AxisAlignedBB(PIPE_MIN, PIPE_MIN, 0.0D, PIPE_MAX, PIPE_MAX, 1.0D));
            return boxes;
        }

        boxes.add(CENTER_BB);

        if (pipe.connections[0] != null) {
            boxes.add(new AxisAlignedBB(PIPE_MIN, 0.5D, PIPE_MIN, PIPE_MAX, 1.0D, PIPE_MAX));
        }
        if (pipe.connections[1] != null) {
            boxes.add(new AxisAlignedBB(PIPE_MIN, 0.0D, PIPE_MIN, PIPE_MAX, 0.5D, PIPE_MAX));
        }
        if (pipe.connections[2] != null) {
            boxes.add(new AxisAlignedBB(PIPE_MIN, PIPE_MIN, 0.0D, PIPE_MAX, PIPE_MAX, 0.5D));
        }
        if (pipe.connections[3] != null) {
            boxes.add(new AxisAlignedBB(0.5D, PIPE_MIN, PIPE_MIN, 1.0D, PIPE_MAX, PIPE_MAX));
        }
        if (pipe.connections[4] != null) {
            boxes.add(new AxisAlignedBB(PIPE_MIN, PIPE_MIN, 0.5D, PIPE_MAX, PIPE_MAX, 1.0D));
        }
        if (pipe.connections[5] != null) {
            boxes.add(new AxisAlignedBB(0.0D, PIPE_MIN, PIPE_MIN, 0.5D, PIPE_MAX, PIPE_MAX));
        }

        return boxes;
    }

    @Override
    public void addCollisionBoxToList(IBlockState state, World worldIn, BlockPos pos, AxisAlignedBB entityBox,
                                      List<AxisAlignedBB> collidingBoxes, @Nullable Entity entityIn, boolean isActualState) {
        for (AxisAlignedBB aabb : getPipeBoxes(worldIn, pos)) {
            Block.addCollisionBoxToList(pos, entityBox, collidingBoxes, aabb);
        }
    }

    @Override
    @Nullable
    public RayTraceResult collisionRayTrace(IBlockState state, World worldIn, BlockPos pos, Vec3d start, Vec3d end) {
        List<AxisAlignedBB> boxes = getPipeBoxes(worldIn, pos);

        RayTraceResult closestHit = null;
        double minDistance = Double.MAX_VALUE;

        for (AxisAlignedBB aabb : boxes) {
            RayTraceResult hit = this.rayTrace(pos, start, end, aabb);
            if (hit != null) {
                double dist = hit.hitVec.squareDistanceTo(start);
                if (dist < minDistance) {
                    minDistance = dist;
                    closestHit = hit;
                }
            }
        }

        if (closestHit == null) {
            return null;
        }

        TileEntity te = worldIn.getTileEntity(pos);
        if (!(te instanceof TileEntityFFDuctBaseMk2)) {
            return closestHit;
        }

        TileEntityFFDuctBaseMk2 pipe = (TileEntityFFDuctBaseMk2) te;
        int mask = getConnectionMask(pipe);

        double localX = closestHit.hitVec.x - pos.getX();
        double localY = closestHit.hitVec.y - pos.getY();
        double localZ = closestHit.hitVec.z - pos.getZ();

        /*
         * Ключевой фикс:
         * если труба прямая/тупиковая, и луч попал в верх/низ/бок около торца,
         * то считаем, что игрок целился именно в торец, а не в верхнюю грань.
         * Иначе Minecraft ставит следующий блок вверх.
         */
        if (isStraightX(mask) && closestHit.sideHit != EnumFacing.EAST && closestHit.sideHit != EnumFacing.WEST) {
            if (localX >= 1.0D - CONTINUE_ZONE) {
                return new RayTraceResult(closestHit.hitVec, EnumFacing.EAST, pos);
            }
            if (localX <= CONTINUE_ZONE) {
                return new RayTraceResult(closestHit.hitVec, EnumFacing.WEST, pos);
            }
        }

        if (isStraightZ(mask) && closestHit.sideHit != EnumFacing.SOUTH && closestHit.sideHit != EnumFacing.NORTH) {
            if (localZ >= 1.0D - CONTINUE_ZONE) {
                return new RayTraceResult(closestHit.hitVec, EnumFacing.SOUTH, pos);
            }
            if (localZ <= CONTINUE_ZONE) {
                return new RayTraceResult(closestHit.hitVec, EnumFacing.NORTH, pos);
            }
        }

        if (isStraightY(mask) && closestHit.sideHit != EnumFacing.UP && closestHit.sideHit != EnumFacing.DOWN) {
            if (localY >= 1.0D - CONTINUE_ZONE) {
                return new RayTraceResult(closestHit.hitVec, EnumFacing.UP, pos);
            }
            if (localY <= CONTINUE_ZONE) {
                return new RayTraceResult(closestHit.hitVec, EnumFacing.DOWN, pos);
            }
        }

        return closestHit;
    }

    @Override
    public AxisAlignedBB getBoundingBox(IBlockState state, IBlockAccess world, BlockPos pos) {
        TileEntity te = world.getTileEntity(pos);

        if (te instanceof TileEntityFFDuctBaseMk2) {
            TileEntityFFDuctBaseMk2 pipe = (TileEntityFFDuctBaseMk2) te;
            int mask = getConnectionMask(pipe);

            if (mask == 0) {
                return CENTER_BB;
            } else if (isStraightX(mask)) {
                return new AxisAlignedBB(0.0D, PIPE_MIN, PIPE_MIN, 1.0D, PIPE_MAX, PIPE_MAX);
            } else if (isStraightY(mask)) {
                return new AxisAlignedBB(PIPE_MIN, 0.0D, PIPE_MIN, PIPE_MAX, 1.0D, PIPE_MAX);
            } else if (isStraightZ(mask)) {
                return new AxisAlignedBB(PIPE_MIN, PIPE_MIN, 0.0D, PIPE_MAX, PIPE_MAX, 1.0D);
            } else {
                boolean east = pipe.connections[3] != null;
                boolean west = pipe.connections[5] != null;
                boolean up = pipe.connections[0] != null;
                boolean down = pipe.connections[1] != null;
                boolean south = pipe.connections[4] != null;
                boolean north = pipe.connections[2] != null;

                return new AxisAlignedBB(
                        west ? 0.0D : PIPE_MIN,
                        down ? 0.0D : PIPE_MIN,
                        north ? 0.0D : PIPE_MIN,
                        east ? 1.0D : PIPE_MAX,
                        up ? 1.0D : PIPE_MAX,
                        south ? 1.0D : PIPE_MAX
                );
            }
        }

        return CENTER_BB;
    }

    @Override public boolean isFullBlock(IBlockState state) { return false; }
    @Override public boolean isFullCube(IBlockState state) { return false; }
    @Override public boolean isBlockNormalCube(IBlockState state) { return false; }
    @Override public boolean isNormalCube(IBlockState state) { return false; }
    @Override public boolean isNormalCube(IBlockState state, IBlockAccess world, BlockPos pos) { return false; }
    @Override public boolean isOpaqueCube(IBlockState state) { return false; }

    @Override
    public BlockFaceShape getBlockFaceShape(IBlockAccess worldIn, IBlockState state, BlockPos pos, EnumFacing face) {
        return BlockFaceShape.CENTER;
    }

    @Override
    protected BlockStateContainer createBlockState() {
        return new BlockStateContainer(this, EXTRACTS);
    }

    @Override
    public int getMetaFromState(IBlockState state) {
        return state.getValue(EXTRACTS) ? 1 : 0;
    }

    @Override
    public IBlockState getStateFromMeta(int meta) {
        return meta > 0
                ? getDefaultState().withProperty(EXTRACTS, true)
                : getDefaultState().withProperty(EXTRACTS, false);
    }

    @Override
    public boolean onScrew(World world, EntityPlayer player, int x, int y, int z, EnumFacing side,
                           float fX, float fY, float fZ, EnumHand hand, ToolType tool) {
        if (tool == ToolType.SCREWDRIVER) {
            BlockPos pos = new BlockPos(x, y, z);
            TileEntity te = world.getTileEntity(pos);

            if (te instanceof TileEntityFFDuctBaseMk2) {
                TileEntityFFDuctBaseMk2 pipe = (TileEntityFFDuctBaseMk2) te;

                if (!pipe.hasExternalConnections()) {
                    return false;
                }

                int current = pipe.getThroughput();
                int currentIndex = -1;

                for (int i = 0; i < THROUGHPUT_TIERS.length; i++) {
                    if (THROUGHPUT_TIERS[i] == current) {
                        currentIndex = i;
                        break;
                    }
                }

                int nextIndex = currentIndex == -1 ? 0 : (currentIndex + 1) % THROUGHPUT_TIERS.length;
                int newThroughput = THROUGHPUT_TIERS[nextIndex];

                pipe.setThroughput(newThroughput);

                player.swingArm(hand);
                world.playSound(null, pos, SoundEvents.BLOCK_METAL_PLACE, SoundCategory.BLOCKS, 0.5F, 1.2F);
                return true;
            }
        }

        return false;
    }

    @Override
    public void printHook(Pre event, World world, int x, int y, int z) {
        TileEntity te = world.getTileEntity(new BlockPos(x, y, z));
        if (!(te instanceof TileEntityFFDuctBaseMk2)) return;

        TileEntityFFDuctBaseMk2 pipe = (TileEntityFFDuctBaseMk2) te;
        Fluid ductFluid = pipe.getType();

        List<String> text = new ArrayList<>();

        if (ductFluid == null) {
            text.add("§7" + I18nUtil.resolveKey("desc.none"));
        } else {
            int color = ModForgeFluids.getFluidColor(ductFluid);
            text.add("&[" + color + "&]" + I18nUtil.resolveKey(ductFluid.getUnlocalizedName()));
        }

        if (pipe.hasExternalConnections()) {
            text.add(TextFormatting.AQUA + I18nUtil.resolveKey("desc.throughput") + ": " + getThroughputText(pipe.getThroughput()));
        }

        ILookOverlay.printGeneric(
                event,
                I18nUtil.resolveKey(getTranslationKey() + ".name"),
                0xffff00,
                0x404000,
                text
        );
    }

    protected String getThroughputText(int throughput) {
        if (throughput == -1) {
            return "∞ B/s";
        }
        return (throughput / 50) + " B/s";
    }

    public abstract void addInformation(ItemStack stack, World player, List<String> tooltip, ITooltipFlag advanced);
}