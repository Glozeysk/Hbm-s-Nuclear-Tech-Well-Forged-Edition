package com.hbm.tileentity.network;

import api.hbm.block.IConveyorInput;
import api.hbm.block.IConveyorOutput;
import com.hbm.blocks.network.conveyor.block.BlockConveyor;
import com.hbm.blocks.network.conveyor.BeltItemData;
import com.hbm.blocks.network.conveyor.BeltLane;
import com.hbm.blocks.network.conveyor.BeltSegment;
import com.hbm.blocks.network.conveyor.BeltSegmentManager;
import com.hbm.interfaces.IControlReceiver;
import com.hbm.inventory.container.ContainerCraneRouter;
import com.hbm.inventory.gui.GUICraneRouter;
import com.hbm.tileentity.IBufPacketReceiver;
import com.hbm.tileentity.IGUIProvider;
import com.hbm.tileentity.TileEntityMachineBase;
import io.netty.buffer.ByteBuf;
import net.minecraft.block.Block;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ITickable;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class TileEntityCraneRouter extends TileEntityMachineBase implements IGUIProvider, IControlReceiver, ITickable, IBufPacketReceiver, IConveyorInput, IConveyorOutput {
    public static final int MODE_NONE = 0;
    public static final int MODE_INPUT = 1;
    public static final int MODE_OUTPUT = 2;
    private static final int MAX_LANES = 2;

    private static final ThreadLocal<Set<BlockPos>> INSERTING_VISITED = ThreadLocal.withInitial(HashSet::new);
    private static final ThreadLocal<Set<BlockPos>> CAN_ACCEPT_VISITED = ThreadLocal.withInitial(HashSet::new);

    public int[] modes = new int[6];
    private boolean[] manualOverride = new boolean[6];
    private int[] currentOutputDir = new int[MAX_LANES];

    public TileEntityCraneRouter() {
        super(0);
    }

    @Override
    public String getName() {
        return "container.craneRouter";
    }

    @Override
    public int tryInsertDirect(ItemStack stack) {
        return tryInsertDirect(stack, 0);
    }

    @Override
    public int tryInsertDirect(ItemStack stack, int sourceLane) {
        if (stack.isEmpty()) return 0;
        if (sourceLane < 0 || sourceLane >= MAX_LANES) sourceLane = 0;

        Set<BlockPos> visited = INSERTING_VISITED.get();
        if (visited.contains(pos)) {
            return 0;
        }
        visited.add(pos);

        try {
            List<EnumFacing> outputDirs = getOutputDirections();
            if (outputDirs.isEmpty()) return 0;

            int startIdx = currentOutputDir[sourceLane];

            for (int attempt = 0; attempt < outputDirs.size(); attempt++) {
                int idx = (startIdx + attempt) % outputDirs.size();
                EnumFacing dir = outputDirs.get(idx);

                int accepted = tryInsertToDirection(dir, stack, sourceLane);
                if (accepted > 0) {
                    currentOutputDir[sourceLane] = (idx + 1) % outputDirs.size();
                    return accepted;
                }
            }

            return 0;
        } finally {
            visited.remove(pos);
        }
    }

    private int tryInsertToDirection(EnumFacing dir, ItemStack stack, int sourceLane) {
        if (isInputDirection(dir)) {
            return 0;
        }

        TileEntity targetTe = world.getTileEntity(pos.offset(dir));
        Block targetBlock = world.getBlockState(pos.offset(dir)).getBlock();

        if (targetTe instanceof IConveyorInput) {
            return ((IConveyorInput) targetTe).tryInsertDirect(stack.copy(), sourceLane);
        } else if (targetBlock instanceof BlockConveyor) {
            BeltSegment segment = BeltSegmentManager.getOrCreateSegment(world, pos.offset(dir));
            if (segment != null) {
                int blockIndex = segment.getBlockIndex(pos.offset(dir));
                if (blockIndex >= 0) {
                    BlockConveyor conveyor = (BlockConveyor) targetBlock;
                    EnumFacing conveyorFacing = conveyor.getLaneFacing(world, pos.offset(dir));
                    net.minecraft.block.state.IBlockState state = world.getBlockState(pos.offset(dir));
                    net.minecraft.block.state.IBlockState actualState = conveyor.getActualState(state, world, pos.offset(dir));
                    BlockConveyor.CurveType curve = actualState.getValue(BlockConveyor.CURVE);
                    EnumFacing fromConveyorToRouter = dir.getOpposite();

                    int targetLane = sourceLane < segment.getLaneCount() ? sourceLane : 0;
                    int routeType = BeltItemData.ROUTE_FORWARD;

                    if (curve == BlockConveyor.CurveType.NONE) {
                        EnumFacing left = conveyorFacing.rotateYCCW();
                        EnumFacing right = conveyorFacing.rotateY();

                        if (fromConveyorToRouter == left) {
                            routeType = BeltItemData.ROUTE_RIGHT_ENTRY;
                        } else if (fromConveyorToRouter == right) {
                            routeType = BeltItemData.ROUTE_LEFT_ENTRY;
                        }
                    }

                    BeltLane beltLane = segment.getLane(targetLane);
                    double slotProgress = blockIndex + BeltLane.ITEM_LENGTH * 0.5D;
                    if (beltLane.isSlotFree(slotProgress)) {
                        BeltItemData item = new BeltItemData(stack.copy(), targetLane, slotProgress);
                        item.setRouteType(routeType);
                        if (segment.insertItem(item)) {
                            segment.markDirty();
                            return stack.getCount();
                        }
                    }
                }
            }
            return 0;
        } else if (targetTe != null && targetTe.hasCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, dir.getOpposite())) {
            IItemHandler handler = targetTe.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, dir.getOpposite());
            if (handler != null) {
                ItemStack toInsert = stack.copy();
                ItemStack rest = insertIntoHandler(handler, toInsert);
                return stack.getCount() - rest.getCount();
            }
        }

        return 0;
    }

    private boolean isInputDirection(EnumFacing dir) {
        BlockPos neighbor = pos.offset(dir);
        Block block = world.getBlockState(neighbor).getBlock();

        if (block instanceof BlockConveyor) {
            BlockConveyor conv = (BlockConveyor) block;
            EnumFacing convFacing = conv.getLaneFacing(world, neighbor);
            return convFacing == dir.getOpposite();
        }

        return false;
    }

    private boolean canAcceptFromDirection(EnumFacing dir) {
        if (isInputDirection(dir)) {
            return false;
        }

        TileEntity targetTe = world.getTileEntity(pos.offset(dir));
        Block targetBlock = world.getBlockState(pos.offset(dir)).getBlock();

        if (targetTe instanceof IConveyorInput) {
            return ((IConveyorInput) targetTe).canAcceptAny();
        } else if (targetBlock instanceof BlockConveyor) {
            BeltSegment segment = BeltSegmentManager.getOrCreateSegment(world, pos.offset(dir));
            if (segment != null) {
                int blockIndex = segment.getBlockIndex(pos.offset(dir));
                if (blockIndex >= 0) {
                    for (int lane = 0; lane < segment.getLaneCount(); lane++) {
                        BeltLane beltLane = segment.getLane(lane);
                        double slotProgress = blockIndex + BeltLane.ITEM_LENGTH * 0.5D;
                        if (beltLane.isSlotFree(slotProgress)) {
                            return true;
                        }
                    }
                }
            }
            return false;
        } else if (targetTe != null && targetTe.hasCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, dir.getOpposite())) {
            IItemHandler handler = targetTe.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, dir.getOpposite());
            if (handler != null) {
                for (int i = 0; i < handler.getSlots(); i++) {
                    ItemStack slotStack = handler.getStackInSlot(i);
                    if (slotStack.isEmpty() || slotStack.getCount() < slotStack.getMaxStackSize()) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    private ItemStack insertIntoHandler(IItemHandler handler, ItemStack stack) {
        ItemStack remaining = stack.copy();
        for (int i = 0; i < handler.getSlots() && !remaining.isEmpty(); i++) {
            remaining = handler.insertItem(i, remaining, false);
        }
        return remaining;
    }

    private int determineTargetLane(BeltSegment segment, EnumFacing fromDirection, EnumFacing conveyorFacing, int sourceLane) {
        if (segment.getLaneCount() <= 1) return 0;

        EnumFacing left = conveyorFacing.rotateYCCW();
        EnumFacing right = conveyorFacing.rotateY();

        if (fromDirection == left) {
            return 0;
        } else if (fromDirection == right) {
            return segment.getLaneCount() - 1;
        }

        return sourceLane < segment.getLaneCount() ? sourceLane : 0;
    }

    @Override
    public boolean canAcceptAny() {
        Set<BlockPos> visited = CAN_ACCEPT_VISITED.get();
        if (visited.contains(pos)) {
            return false;
        }
        visited.add(pos);

        try {
            List<EnumFacing> outputDirs = getOutputDirections();
            if (outputDirs.isEmpty()) return false;

            for (EnumFacing dir : outputDirs) {
                if (canAcceptFromDirection(dir)) {
                    return true;
                }
            }
            return false;
        } finally {
            visited.remove(pos);
        }
    }

    @Override
    public boolean canExtractFrom(EnumFacing side) {
        int index = getIndexFromDirection(side);
        return index >= 0 && modes[index] == MODE_INPUT;
    }

    @Override
    public ItemStack extractItem(EnumFacing side, int maxAmount) {
        int index = getIndexFromDirection(side);
        if (index < 0 || modes[index] != MODE_INPUT) return ItemStack.EMPTY;

        TileEntity sourceTe = world.getTileEntity(pos.offset(side));
        if (sourceTe instanceof IConveyorOutput) {
            return ((IConveyorOutput) sourceTe).extractItem(side.getOpposite(), maxAmount);
        }
        return ItemStack.EMPTY;
    }

    private List<EnumFacing> getOutputDirections() {
        List<EnumFacing> dirs = new ArrayList<>();
        for (int i = 0; i < 6; i++) {
            if (modes[i] == MODE_OUTPUT) {
                EnumFacing dir = getDirectionFromIndex(i);
                if (dir != null) {
                    dirs.add(dir);
                }
            }
        }
        return dirs;
    }

    private EnumFacing getDirectionFromIndex(int index) {
        EnumFacing[] customEnumOrder = new EnumFacing[]{
                EnumFacing.NORTH, EnumFacing.UP, EnumFacing.EAST,
                EnumFacing.SOUTH, EnumFacing.DOWN, EnumFacing.WEST
        };
        return index < customEnumOrder.length ? customEnumOrder[index] : null;
    }

    private int getIndexFromDirection(EnumFacing dir) {
        EnumFacing[] customEnumOrder = new EnumFacing[]{
                EnumFacing.NORTH, EnumFacing.UP, EnumFacing.EAST,
                EnumFacing.SOUTH, EnumFacing.DOWN, EnumFacing.WEST
        };
        for (int i = 0; i < customEnumOrder.length; i++) {
            if (customEnumOrder[i] == dir) return i;
        }
        return -1;
    }

    @Override
    public void update() {
        if (!world.isRemote) {
            boolean changed = false;
            for (int i = 0; i < 6; i++) {
                if (modes[i] == MODE_NONE && !manualOverride[i]) {
                    EnumFacing dir = getDirectionFromIndex(i);

                    if (dir == EnumFacing.DOWN) {
                        continue;
                    }

                    BlockPos neighbor = pos.offset(dir);
                    Block block = world.getBlockState(neighbor).getBlock();

                    if (block instanceof BlockConveyor) {
                        BlockConveyor conv = (BlockConveyor) block;
                        EnumFacing convFacing = conv.getLaneFacing(world, neighbor);
                        if (convFacing == dir.getOpposite()) {
                            modes[i] = MODE_INPUT;
                            changed = true;
                        } else {
                            modes[i] = MODE_OUTPUT;
                            changed = true;
                        }
                    } else if (world.getTileEntity(neighbor) != null) {
                        modes[i] = MODE_OUTPUT;
                        changed = true;
                    }
                }
            }

            if (changed) {
                markDirty();
            }
            networkPackNT(15);
        }
    }

    @Override
    public void serialize(ByteBuf buf) {
        for (int i = 0; i < 6; i++) {
            buf.writeInt(i < modes.length ? modes[i] : 0);
        }
        for (int i = 0; i < 6; i++) {
            buf.writeBoolean(manualOverride[i]);
        }
        for (int i = 0; i < MAX_LANES; i++) {
            buf.writeInt(currentOutputDir[i]);
        }
    }

    @Override
    public void deserialize(ByteBuf buf) {
        this.modes = new int[6];
        for (int i = 0; i < 6; i++) {
            this.modes[i] = buf.readInt();
        }
        this.manualOverride = new boolean[6];
        for (int i = 0; i < 6; i++) {
            this.manualOverride[i] = buf.readBoolean();
        }
        this.currentOutputDir = new int[MAX_LANES];
        for (int i = 0; i < MAX_LANES; i++) {
            this.currentOutputDir[i] = buf.readInt();
        }
    }

    @Override
    public void readFromNBT(NBTTagCompound nbt) {
        super.readFromNBT(nbt);

        if (nbt.hasKey("modes")) {
            int[] loaded = nbt.getIntArray("modes");
            if (loaded != null && loaded.length == 6) {
                this.modes = loaded;
            } else {
                this.modes = new int[6];
            }
        } else {
            this.modes = new int[6];
        }

        this.manualOverride = new boolean[6];
        for (int i = 0; i < 6; i++) {
            this.manualOverride[i] = nbt.getBoolean("manualOverride_" + i);
        }

        this.currentOutputDir = new int[MAX_LANES];
        for (int i = 0; i < MAX_LANES; i++) {
            this.currentOutputDir[i] = nbt.getInteger("outputDir_" + i);
        }
    }

    @Override
    public @NotNull NBTTagCompound writeToNBT(NBTTagCompound nbt) {
        super.writeToNBT(nbt);
        nbt.setIntArray("modes", this.modes);
        for (int i = 0; i < 6; i++) {
            nbt.setBoolean("manualOverride_" + i, this.manualOverride[i]);
        }
        for (int i = 0; i < MAX_LANES; i++) {
            nbt.setInteger("outputDir_" + i, this.currentOutputDir[i]);
        }
        return nbt;
    }

    @Override
    public Container provideContainer(int ID, EntityPlayer player, World world, int x, int y, int z) {
        return new ContainerCraneRouter(player.inventory, this);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public GuiScreen provideGUI(int ID, EntityPlayer player, World world, int x, int y, int z) {
        return new GUICraneRouter(player.inventory, this);
    }

    @Override
    public boolean hasPermission(EntityPlayer player) {
        int xCoord = pos.getX();
        int yCoord = pos.getY();
        int zCoord = pos.getZ();
        return new Vec3d(xCoord - player.posX, yCoord - player.posY, zCoord - player.posZ).length() < 20;
    }

    @Override
    public void receiveControl(NBTTagCompound data) {
        int i = data.getInteger("toggle");
        if (i < 0 || i >= 6) return;

        if (modes[i] == MODE_INPUT) return;

        manualOverride[i] = true;

        if (modes[i] == MODE_NONE) {
            modes[i] = MODE_OUTPUT;
        } else {
            modes[i] = MODE_NONE;
        }

        markDirty();
    }

    @Override
    public boolean isItemValidForSlot(int i, ItemStack itemStack) {
        return false;
    }
}