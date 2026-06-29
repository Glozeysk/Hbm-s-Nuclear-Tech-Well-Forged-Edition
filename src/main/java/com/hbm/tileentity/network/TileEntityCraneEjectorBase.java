package com.hbm.tileentity.network;

import api.hbm.block.IConveyorBelt;
import com.hbm.blocks.network.BlockConveyor;
import com.hbm.blocks.network.conveyor.BeltItemData;
import com.hbm.blocks.network.conveyor.BeltLane;
import com.hbm.blocks.network.conveyor.BeltSegment;
import com.hbm.blocks.network.conveyor.BeltSegmentManager;
import com.hbm.blocks.network.conveyor.ConveyorEntryPoints;
import com.hbm.entity.item.EntityMovingItem;
import com.hbm.interfaces.IControlReceiver;
import com.hbm.inventory.container.ContainerCraneEjectorCommon;
import com.hbm.inventory.gui.GUICraneEjectorCommon;
import com.hbm.items.ModItems;
import com.hbm.modules.ModulePatternMatcher;
import com.hbm.tileentity.IBufPacketReceiver;
import com.hbm.tileentity.IGUIProvider;
import io.netty.buffer.ByteBuf;
import net.minecraft.block.Block;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.ISidedInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityFurnace;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.jetbrains.annotations.NotNull;

public abstract class TileEntityCraneEjectorBase extends TileEntityCraneBase implements IGUIProvider, IControlReceiver, IBufPacketReceiver {

    public boolean isWhitelist = false;
    private int inserterTickCounter = 0;
    private static final int INSERTER_DELAY = 3;
    public ModulePatternMatcher matcher;

    public static final int FILTER_COUNT = 18;

    public TileEntityCraneEjectorBase() {
        super(19);
        this.matcher = new ModulePatternMatcher(FILTER_COUNT);
    }

    @Override
    public EnumFacing getInputSide() {
        return getOutputSide().getOpposite();
    }

    @Override
    public void update() {
        super.update();

        if(world.isRemote) {
            return;
        }

        if(this.world.isBlockPowered(pos)) {
            return;
        }

        EnumFacing outputSide = getOutputSide();
        Block outputBlock = world.getBlockState(pos.offset(outputSide)).getBlock();

        if(outputBlock instanceof BlockConveyor) {
            pushToConveyorDirect(outputSide);
        } else {
            inserterTickCounter++;
            if(inserterTickCounter >= INSERTER_DELAY) {
                inserterTickCounter = 0;

                TileEntity outputTe = world.getTileEntity(pos.offset(outputSide));
                if(outputTe instanceof TileEntityCraneInserterBase) {
                    pushToInserterDirect((TileEntityCraneInserterBase) outputTe);
                } else if(outputBlock instanceof IConveyorBelt) {
                    pushToOldConveyorDirect(outputSide, (IConveyorBelt) outputBlock);
                }
            }
        }

        networkPackNT(15);
    }

    private void pushToConveyorDirect(EnumFacing outputSide) {
        BlockPos conveyorPos = pos.offset(outputSide);
        Block block = world.getBlockState(conveyorPos).getBlock();

        if(!(block instanceof BlockConveyor)) return;

        BeltSegment segment = BeltSegmentManager.getOrCreateSegment(world, conveyorPos);
        if(segment == null) return;

        int blockIndex = segment.getBlockIndex(conveyorPos);
        if(blockIndex < 0) return;

        BlockConveyor conveyor = (BlockConveyor) block;
        EnumFacing conveyorFacing = conveyor.getLaneFacing(world, conveyorPos);

        boolean isSideEntry = (conveyorFacing != outputSide);

        if(isSideEntry) {
            pushToConveyorSide(segment, conveyor, conveyorPos, blockIndex, outputSide, conveyorFacing);
        } else {
            pushToConveyorFront(segment, conveyorPos, blockIndex, outputSide, conveyorFacing);
        }
    }

    private void pushToConveyorFront(BeltSegment segment, BlockPos conveyorPos, int blockIndex, EnumFacing outputSide, EnumFacing conveyorFacing) {
        for(int lane = 0; lane < segment.getLaneCount(); lane++) {
            BeltLane beltLane = segment.getLane(lane);
            double slotProgress = blockIndex + BeltLane.ITEM_LENGTH * 0.5D;

            if(beltLane.isSlotFree(slotProgress)) {
                int extracted = extractAndInsertToConveyor(segment, lane, slotProgress, BeltItemData.ROUTE_FORWARD);
                if(extracted > 0) {
                    segment.markDirty();
                    return;
                }
            }
        }
    }

    private void pushToConveyorSide(BeltSegment segment, BlockConveyor conveyor, BlockPos conveyorPos, int blockIndex, EnumFacing outputSide, EnumFacing conveyorFacing) {
        ConveyorEntryPoints entryPoints = conveyor.getEntryPoints();
        if(entryPoints == null) return;

        EnumFacing left = conveyorFacing.rotateYCCW();
        EnumFacing right = conveyorFacing.rotateY();

        boolean fromLeft = (outputSide == left);
        boolean fromRight = (outputSide == right);

        if(!fromLeft && !fromRight) return;

        int routeType = fromLeft ? BeltItemData.ROUTE_LEFT_ENTRY : BeltItemData.ROUTE_RIGHT_ENTRY;

        int targetLane = findBestLaneForSideEntry(conveyor, fromLeft);

        BeltLane beltLane = segment.getLane(targetLane);
        double slotProgress = blockIndex + BeltLane.ITEM_LENGTH * 0.5D;

        if(beltLane.isSlotFree(slotProgress)) {
            int extracted = extractAndInsertToConveyor(segment, targetLane, slotProgress, routeType);
            if(extracted > 0) {
                segment.markDirty();
            }
        }
    }

    private int findBestLaneForSideEntry(BlockConveyor conveyor, boolean fromLeft) {
        double[] offsets = conveyor.getLaneOffsets();
        if(offsets.length <= 1) return 0;

        double targetX = fromLeft ? 2.0D : 14.0D;

        double bestDist = Double.MAX_VALUE;
        int bestLane = 0;

        for(int i = 0; i < offsets.length; i++) {
            double laneX = 8.0D + offsets[i] * 16.0D;
            double dist = Math.abs(laneX - targetX);
            if(dist < bestDist) {
                bestDist = dist;
                bestLane = i;
            }
        }

        return bestLane;
    }

    private int extractAndInsertToConveyor(BeltSegment segment, int lane, double slotProgress, int routeType) {
        EnumFacing inputSide = getInputSide();
        EnumFacing accessFace = inputSide.getOpposite();

        TileEntity te = world.getTileEntity(pos.offset(inputSide));
        if(te == null || te instanceof TileEntityCraneEjectorBase) return 0;

        int[] access = null;
        ISidedInventory sided = null;

        if(te instanceof ISidedInventory) {
            sided = (ISidedInventory) te;
            access = masquerade(sided, accessFace);
        }

        IItemHandler inv = null;
        if(te.hasCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, accessFace)) {
            inv = te.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, accessFace);
        } else if(te.hasCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, null)) {
            inv = te.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, null);
        }

        if(inv == null) return 0;

        int size = access == null ? inv.getSlots() : access.length;
        int amount = getAmount();

        for(int i = 0; i < size; i++) {
            int actualSlot = access == null ? i : access[i];
            int handlerSlot = access == null ? i : inv.getSlots() == access.length ? i : actualSlot;
            ItemStack stack = inv.getStackInSlot(handlerSlot);

            if(!stack.isEmpty() && (sided == null || canExtract(sided, actualSlot, stack, accessFace))) {
                boolean match = matchesFilter(stack);

                if(isWhitelist == match) {
                    int toSend = Math.min(amount, stack.getCount());
                    ItemStack extracted = inv.extractItem(handlerSlot, toSend, true);

                    if(!extracted.isEmpty()) {
                        ItemStack toInsert = extracted.copy();
                        BeltItemData item = new BeltItemData(toInsert, lane, slotProgress);
                        item.setRouteType(routeType);

                        if(segment.insertItem(item)) {
                            inv.extractItem(handlerSlot, extracted.getCount(), false);
                            return extracted.getCount();
                        }
                    }
                }
            }
        }

        return 0;
    }

    private void pushToInserterDirect(TileEntityCraneInserterBase inserter) {
        EnumFacing inputSide = getInputSide();
        EnumFacing accessFace = inputSide.getOpposite();

        TileEntity te = world.getTileEntity(pos.offset(inputSide));
        if(te == null || te instanceof TileEntityCraneEjectorBase) return;

        int[] access = null;
        ISidedInventory sided = null;

        if(te instanceof ISidedInventory) {
            sided = (ISidedInventory) te;
            access = masquerade(sided, accessFace);
        }

        IItemHandler inv = null;
        if(te.hasCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, accessFace)) {
            inv = te.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, accessFace);
        } else if(te.hasCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, null)) {
            inv = te.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, null);
        }

        if(inv == null) return;

        int size = access == null ? inv.getSlots() : access.length;
        int amount = getAmount();

        for(int i = 0; i < size; i++) {
            int actualSlot = access == null ? i : access[i];
            int handlerSlot = access == null ? i : inv.getSlots() == access.length ? i : actualSlot;
            ItemStack stack = inv.getStackInSlot(handlerSlot);

            if(!stack.isEmpty() && (sided == null || canExtract(sided, actualSlot, stack, accessFace))) {
                boolean match = matchesFilter(stack);

                if(isWhitelist == match) {
                    int toSend = Math.min(amount, stack.getCount());
                    ItemStack cStack = stack.copy();
                    cStack.setCount(toSend);

                    int accepted = inserter.tryInsertDirect(cStack.copy());
                    if(accepted > 0) {
                        inv.extractItem(handlerSlot, accepted, false);
                        return;
                    }
                }
            }
        }
    }

    private void pushToOldConveyorDirect(EnumFacing outputSide, IConveyorBelt belt) {
        EnumFacing inputSide = getInputSide();
        EnumFacing accessFace = inputSide.getOpposite();

        TileEntity te = world.getTileEntity(pos.offset(inputSide));
        if(te == null || te instanceof TileEntityCraneEjectorBase) return;

        int[] access = null;
        ISidedInventory sided = null;

        if(te instanceof ISidedInventory) {
            sided = (ISidedInventory) te;
            access = masquerade(sided, accessFace);
        }

        IItemHandler inv = null;
        if(te.hasCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, accessFace)) {
            inv = te.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, accessFace);
        } else if(te.hasCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, null)) {
            inv = te.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, null);
        }

        if(inv == null) return;

        int size = access == null ? inv.getSlots() : access.length;
        int amount = getAmount();

        for(int i = 0; i < size; i++) {
            int actualSlot = access == null ? i : access[i];
            int handlerSlot = access == null ? i : inv.getSlots() == access.length ? i : actualSlot;
            ItemStack stack = inv.getStackInSlot(handlerSlot);

            if(!stack.isEmpty() && (sided == null || canExtract(sided, actualSlot, stack, accessFace))) {
                boolean match = matchesFilter(stack);

                if(isWhitelist == match) {
                    int toSend = Math.min(amount, stack.getCount());
                    ItemStack extracted = inv.extractItem(handlerSlot, toSend, false);

                    if(!extracted.isEmpty()) {
                        int xCoord = pos.getX();
                        int yCoord = pos.getY();
                        int zCoord = pos.getZ();

                        EntityMovingItem moving = new EntityMovingItem(world);
                        Vec3d itemPos = new Vec3d(
                                xCoord + 0.5 + outputSide.getDirectionVec().getX() * 0.55,
                                yCoord + 0.5 + outputSide.getDirectionVec().getY() * 0.55,
                                zCoord + 0.5 + outputSide.getDirectionVec().getZ() * 0.55
                        );
                        Vec3d snap = belt.getClosestSnappingPosition(world,
                                new BlockPos(xCoord + outputSide.getDirectionVec().getX(),
                                        yCoord + outputSide.getDirectionVec().getY(),
                                        zCoord + outputSide.getDirectionVec().getZ()),
                                itemPos);
                        moving.setPosition(snap.x, snap.y, snap.z);
                        moving.setItemStack(extracted);
                        world.spawnEntity(moving);
                        return;
                    }
                }
            }
        }
    }

    protected int getAmount() {
        int amount = 1;

        if(!inventory.getStackInSlot(18).isEmpty()) {
            if(inventory.getStackInSlot(18).getItem() == ModItems.upgrade_stack_1) {
                amount = 4;
            } else if(inventory.getStackInSlot(18).getItem() == ModItems.upgrade_stack_2) {
                amount = 16;
            } else if(inventory.getStackInSlot(18).getItem() == ModItems.upgrade_stack_3) {
                amount = 64;
            }
        }

        return amount;
    }

    public static boolean canExtract(ISidedInventory sided, int index, ItemStack stack, EnumFacing dir) {
        boolean can = false;
        try {
            can = sided.canExtractItem(index, stack, dir);
        } catch(IndexOutOfBoundsException e) {
            return false;
        }
        return can;
    }

    public static int[] masquerade(ISidedInventory sided, EnumFacing side) {
        if(sided instanceof TileEntityFurnace) {
            return new int[] {2};
        }
        return sided.getSlotsForFace(side);
    }

    public boolean matchesFilter(ItemStack stack) {
        boolean hasAnyFilter = false;
        for(int i = 0; i < FILTER_COUNT; i++) {
            ItemStack filter = inventory.getStackInSlot(i);
            if(!filter.isEmpty()) {
                hasAnyFilter = true;
                if(this.matcher.isValidForFilter(filter, i, stack)) {
                    return true;
                }
            }
        }
        return !hasAnyFilter;
    }

    public void nextMode(int i) {
        this.matcher.nextMode(world, inventory.getStackInSlot(i), i);
    }

    public void initPattern(ItemStack stack, int index) {
        this.matcher.initPatternSmart(world, stack, index);
    }

    @Override
    public boolean isItemValidForSlot(int i, ItemStack itemStack) {
        return i < FILTER_COUNT || i == 18;
    }

    @Override
    public Container provideContainer(int ID, EntityPlayer player, World world, int x, int y, int z) {
        return new ContainerCraneEjectorCommon(player.inventory, this);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public GuiScreen provideGUI(int ID, EntityPlayer player, World world, int x, int y, int z) {
        return new GUICraneEjectorCommon(player.inventory, this);
    }

    @Override
    public void readFromNBT(NBTTagCompound nbt) {
        super.readFromNBT(nbt);
        this.isWhitelist = nbt.getBoolean("isWhitelist");
        this.matcher.readFromNBT(nbt);
    }

    @Override
    public @NotNull NBTTagCompound writeToNBT(NBTTagCompound nbt) {
        super.writeToNBT(nbt);
        nbt.setBoolean("isWhitelist", this.isWhitelist);
        this.matcher.writeToNBT(nbt);
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
        if(data.hasKey("isWhitelist")) {
            this.isWhitelist = !this.isWhitelist;
        }
    }

    @Override
    public int[] getAccessibleSlotsFromSide(EnumFacing e) {
        return new int[0];
    }

    @Override
    public boolean canExtractItem(int slot, ItemStack itemStack, int amount) {
        return false;
    }

    @Override
    public void serialize(ByteBuf buf) {
        buf.writeBoolean(this.isWhitelist);
        for(int i = 0; i < FILTER_COUNT; i++) {
            if(matcher.modes[i] != null) {
                buf.writeBoolean(true);
                ByteBufUtils.writeUTF8String(buf, matcher.modes[i]);
            } else {
                buf.writeBoolean(false);
            }
        }
    }

    @Override
    public void deserialize(ByteBuf buf) {
        this.isWhitelist = buf.readBoolean();
        this.matcher.modes = new String[FILTER_COUNT];
        for(int i = 0; i < FILTER_COUNT; i++) {
            if(buf.readBoolean()) {
                matcher.modes[i] = ByteBufUtils.readUTF8String(buf);
            } else {
                matcher.modes[i] = null;
            }
        }
    }
}