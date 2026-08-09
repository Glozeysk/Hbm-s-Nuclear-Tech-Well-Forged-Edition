package com.hbm.tileentity.network;

import api.hbm.block.IConveyorInput;
import api.hbm.block.IConveyorOutput;
import com.hbm.blocks.network.conveyor.block.BlockConveyor;
import com.hbm.blocks.network.conveyor.BeltItemData;
import com.hbm.blocks.network.conveyor.BeltLane;
import com.hbm.blocks.network.conveyor.BeltSegment;
import com.hbm.blocks.network.conveyor.BeltSegmentManager;
import com.hbm.interfaces.IControlReceiver;
import com.hbm.inventory.container.ContainerCraneSorter;
import com.hbm.inventory.gui.GUICraneSorter;
import com.hbm.lib.Library;
import com.hbm.modules.ModulePatternMatcher;
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
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class TileEntityCraneSorter extends TileEntityMachineBase implements IGUIProvider, IControlReceiver, ITickable, IBufPacketReceiver, IConveyorInput, IConveyorOutput {
    public ModulePatternMatcher[] patterns = new ModulePatternMatcher[6];
    public int[] modes = new int[6];
    public static final int MODE_NONE = 0;
    public static final int MODE_WHITELIST = 1;
    public static final int MODE_BLACKLIST = 2;
    public static final int MODE_WILDCARD = 3;

    private boolean[] blockedDirections = new boolean[6];
    private boolean[] manualOverride = new boolean[6];
    private int[] currentOutputIndex = new int[2];

    public TileEntityCraneSorter() {
        super(30);
        for(int i = 0; i < patterns.length; i++) {
            patterns[i] = new ModulePatternMatcher(5);
        }
    }

    @Override
    public String getName() {
        return "container.craneSorter";
    }

    @Override
    public int tryInsertDirect(ItemStack stack) {
        return tryInsertDirect(stack, 0);
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

    @Override
    public int tryInsertDirect(ItemStack stack, int sourceLane) {
        if (stack.isEmpty()) return 0;
        if (sourceLane < 0 || sourceLane >= 2) sourceLane = 0;

        List<EnumFacing> validDirs = new ArrayList<>();

        for(int i = 0; i < 6; i++) {
            if (modes[i] == MODE_NONE) continue;

            EnumFacing dir = getDirectionFromIndex(i);
            if (dir == null) continue;

            if (isInputDirection(dir)) continue;

            ModulePatternMatcher matcher = patterns[i];
            int mode = modes[i];

            boolean matchesFilter = false;

            if(mode == MODE_WILDCARD) {
                matchesFilter = true;
            } else {
                for(int slot = 0; slot < 5; slot++) {
                    ItemStack filter = inventory.getStackInSlot(i * 5 + slot);
                    if(filter.isEmpty()) continue;
                    if(matcher.isValidForFilter(filter, slot, stack)) {
                        matchesFilter = true;
                        break;
                    }
                }
            }

            if((mode == MODE_WHITELIST && matchesFilter) || (mode == MODE_BLACKLIST && !matchesFilter) || mode == MODE_WILDCARD) {
                validDirs.add(dir);
            }
        }

        if(validDirs.isEmpty()) {
            return 0;
        }

        int idx = currentOutputIndex[sourceLane] % validDirs.size();
        EnumFacing dir = validDirs.get(idx);
        currentOutputIndex[sourceLane]++;

        TileEntity targetTe = world.getTileEntity(pos.offset(dir));
        Block targetBlock = world.getBlockState(pos.offset(dir)).getBlock();

        if (targetTe instanceof IConveyorInput) {
            int accepted = ((IConveyorInput) targetTe).tryInsertDirect(stack.copy(), sourceLane);
            if (accepted > 0) {
                return accepted;
            }
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
                    EnumFacing fromConveyorToSorter = dir.getOpposite();

                    int targetLane = sourceLane < segment.getLaneCount() ? sourceLane : 0;
                    int routeType = BeltItemData.ROUTE_FORWARD;

                    if (curve == BlockConveyor.CurveType.NONE) {
                        EnumFacing left = conveyorFacing.rotateYCCW();
                        EnumFacing right = conveyorFacing.rotateY();

                        if (fromConveyorToSorter == left) {
                            routeType = BeltItemData.ROUTE_RIGHT_ENTRY;
                        } else if (fromConveyorToSorter == right) {
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
            int dirIndex = getIndexFromDirection(dir);
            if (dirIndex >= 0) blockedDirections[dirIndex] = true;
            return 0;
        }

        else if (targetTe != null && targetTe.hasCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, dir.getOpposite())) {
            IItemHandler handler = targetTe.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, dir.getOpposite());
            if (handler != null) {
                ItemStack toInsert = stack.copy();
                ItemStack rest = insertIntoHandler(handler, toInsert);
                int accepted = stack.getCount() - rest.getCount();
                if (accepted > 0) {
                    return accepted;
                }
            }
        }

        return 0;
    }

    private boolean canSendToDirection(EnumFacing dir, ItemStack stack) {
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
                    if (slotStack.isEmpty() || (Library.areItemStacksCompatible(stack, slotStack, false) && slotStack.getCount() < slotStack.getMaxStackSize())) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    @Override
    public boolean canAcceptAny() {
        for(int i = 0; i < 6; i++) {
            if (modes[i] == MODE_NONE) continue;

            EnumFacing dir = getDirectionFromIndex(i);
            if (dir == null) continue;

            if (isInputDirection(dir)) continue;

            TileEntity targetTe = world.getTileEntity(pos.offset(dir));
            Block targetBlock = world.getBlockState(pos.offset(dir)).getBlock();

            if (targetTe instanceof IConveyorInput) {
                if (((IConveyorInput) targetTe).canAcceptAny()) {
                    return true;
                }
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
            } else if (targetTe != null && targetTe.hasCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, dir.getOpposite())) {
                IItemHandler handler = targetTe.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, dir.getOpposite());
                if (handler != null) {
                    for (int j = 0; j < handler.getSlots(); j++) {
                        ItemStack slotStack = handler.getStackInSlot(j);
                        if (slotStack.isEmpty() || slotStack.getCount() < slotStack.getMaxStackSize()) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
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
    public boolean canExtractFrom(EnumFacing side) {
        return true;
    }

    @Override
    public ItemStack extractItem(EnumFacing side, int maxAmount) {
        return ItemStack.EMPTY;
    }

    private ItemStack insertIntoHandler(IItemHandler handler, ItemStack stack) {
        ItemStack remaining = stack.copy();
        for (int i = 0; i < handler.getSlots() && !remaining.isEmpty(); i++) {
            remaining = handler.insertItem(i, remaining, false);
        }
        return remaining;
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

    public void resetBlockedDirections() {
        for (int i = 0; i < blockedDirections.length; i++) {
            blockedDirections[i] = false;
        }
    }

    @Override
    public void update() {
        if(!world.isRemote) {
            boolean changed = false;

            if (world.getTotalWorldTime() % 20 == 0) {
                resetBlockedDirections();
            }

            for (int i = 0; i < 6; i++) {
                if (modes[i] == MODE_NONE && !manualOverride[i]) {
                    EnumFacing dir = getDirectionFromIndex(i);
                    if (dir == EnumFacing.UP || dir == EnumFacing.DOWN) {
                        continue;
                    }

                    BlockPos neighbor = pos.offset(dir);
                    Block block = world.getBlockState(neighbor).getBlock();

                    if (block instanceof BlockConveyor) {
                        BlockConveyor conv = (BlockConveyor) block;
                        EnumFacing convFacing = conv.getLaneFacing(world, neighbor);
                        if (convFacing == dir.getOpposite()) {
                            modes[i] = MODE_WHITELIST;
                            changed = true;
                        }
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
        for(int i = 0; i < patterns.length; i++) {
            NBTTagCompound compound = new NBTTagCompound();
            patterns[i].writeToNBT(compound);
            ByteBufUtils.writeTag(buf, compound != null ? compound : new NBTTagCompound());
        }
        for(int i = 0; i < 6; i++) {
            buf.writeInt(i < modes.length ? modes[i] : 0);
        }
        for (int i = 0; i < 6; i++) {
            buf.writeBoolean(manualOverride[i]);
        }
        for (int i = 0; i < 2; i++) {
            buf.writeInt(currentOutputIndex[i]);
        }
    }

    @Override
    public void deserialize(ByteBuf buf) {
        for(int i = 0; i < patterns.length; i++) {
            NBTTagCompound compound = ByteBufUtils.readTag(buf);
            if(compound != null) {
                patterns[i].readFromNBT(compound);
            } else {
                patterns[i] = new ModulePatternMatcher(5);
            }
        }
        this.modes = new int[6];
        for(int i = 0; i < 6; i++) {
            this.modes[i] = buf.readInt();
        }
        this.manualOverride = new boolean[6];
        for (int i = 0; i < 6; i++) {
            this.manualOverride[i] = buf.readBoolean();
        }
        this.currentOutputIndex = new int[2];
        for (int i = 0; i < 2; i++) {
            this.currentOutputIndex[i] = buf.readInt();
        }
    }

    @Override
    public void readFromNBT(NBTTagCompound nbt) {
        super.readFromNBT(nbt);

        for(int i = 0; i < patterns.length; i++) {
            if(nbt.hasKey("pattern" + i)) {
                NBTTagCompound compound = nbt.getCompoundTag("pattern" + i);
                patterns[i].readFromNBT(compound);
            } else {
                patterns[i] = new ModulePatternMatcher(5);
            }
        }

        if(nbt.hasKey("modes")) {
            int[] loaded = nbt.getIntArray("modes");
            if(loaded != null && loaded.length == 6) {
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

        this.currentOutputIndex = new int[2];
        for (int i = 0; i < 2; i++) {
            this.currentOutputIndex[i] = nbt.getInteger("outputIndex_" + i);
        }
    }

    @Override
    public Container provideContainer(int ID, EntityPlayer player, World world, int x, int y, int z) {
        return new ContainerCraneSorter(player.inventory, this);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public GuiScreen provideGUI(int ID, EntityPlayer player, World world, int x, int y, int z) {
        return new GUICraneSorter(player.inventory, this);
    }

    public void nextMode(int index) {
        int matcher = index / 5;
        int mIndex = index % 5;
        this.patterns[matcher].nextMode(world, inventory.getStackInSlot(index), mIndex);
    }

    public void initPattern(ItemStack stack, int index) {
        int matcher = index / 5;
        int mIndex = index % 5;
        this.patterns[matcher].initPatternSmart(world, stack, mIndex);
    }

    @Override
    public @NotNull NBTTagCompound writeToNBT(NBTTagCompound nbt) {
        super.writeToNBT(nbt);

        for(int i = 0; i < patterns.length; i++) {
            NBTTagCompound compound = new NBTTagCompound();
            patterns[i].writeToNBT(compound);
            nbt.setTag("pattern" + i, compound);
        }
        nbt.setIntArray("modes", this.modes);
        for (int i = 0; i < 6; i++) {
            nbt.setBoolean("manualOverride_" + i, this.manualOverride[i]);
        }
        for (int i = 0; i < 2; i++) {
            nbt.setInteger("outputIndex_" + i, this.currentOutputIndex[i]);
        }
        return nbt;
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

        manualOverride[i] = true;

        modes[i]++;
        if(modes[i] > 3) {
            modes[i] = 0;
        }

        markDirty();
    }

    @Override
    public boolean isItemValidForSlot(int i, ItemStack itemStack) {
        return i < 30;
    }
}