package com.hbm.tileentity.network;

import api.hbm.block.IConveyorBelt;
import com.hbm.blocks.network.BlockConveyor;
import com.hbm.blocks.network.conveyor.BeltItemData;
import com.hbm.blocks.network.conveyor.BeltLane;
import com.hbm.blocks.network.conveyor.BeltSegment;
import com.hbm.blocks.network.conveyor.BeltSegmentManager;
import com.hbm.entity.item.EntityMovingItem;
import com.hbm.interfaces.IControlReceiver;
import com.hbm.inventory.container.ContainerCraneEjectorCommon;
import com.hbm.inventory.gui.GUICraneEjectorCommon;
import com.hbm.items.ModItems;
import com.hbm.lib.Library;
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
    private int tickCounter = 0;
    public ModulePatternMatcher matcher;

    public static final int[] ALLOWED_SLOTS = {9, 10, 11, 12, 13, 14, 15, 16, 17};

    public TileEntityCraneEjectorBase() {
        super(20);
        this.matcher = new ModulePatternMatcher(9);
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

        tickCounter++;

        // Извлечение из входного инвентаря (с задержкой)
        if(tickCounter >= getDelay() && !this.world.isBlockPowered(pos)) {
            tickCounter = 0;

            int amount = getAmount();
            EnumFacing inputSide = getInputSide();
            EnumFacing accessFace = inputSide.getOpposite();

            TileEntity te = world.getTileEntity(pos.offset(inputSide));

            if(te != null && !(te instanceof TileEntityCraneEjectorBase)) {
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

                if(inv != null) {
                    int size = access == null ? inv.getSlots() : access.length;

                    for(int i = 0; i < size; i++) {
                        int actualSlot = access == null ? i : access[i];
                        int handlerSlot = access == null ? i : inv.getSlots() == access.length ? i : actualSlot;
                        ItemStack stack = inv.getStackInSlot(handlerSlot);

                        if(!stack.isEmpty() && (sided == null || canExtract(sided, actualSlot, stack, accessFace))) {
                            boolean match = this.matchesFilter(stack);

                            if(isWhitelist == match) {
                                int toSend = Math.min(amount, stack.getCount());
                                ItemStack extracted = inv.extractItem(handlerSlot, toSend, true);

                                if(!extracted.isEmpty()) {
                                    int fill = tryInsertItemCap(inventory, extracted.copy(), ALLOWED_SLOTS);

                                    if(fill > 0 && fill <= toSend) {
                                        inv.extractItem(handlerSlot, fill, false);
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Выталкивание в конвейер КАЖДЫЙ ТИК (без задержки и без проверки tickCounter)
        if(!this.world.isBlockPowered(pos)) {
            EnumFacing outputSide = getOutputSide();
            Block b = world.getBlockState(pos.offset(outputSide)).getBlock();

            if(b instanceof BlockConveyor) {
                pushToConveyorEveryTick(outputSide);
            } else {
                // Старая логика для других целей (работает только при tickCounter == 0)
                if(tickCounter == 0) {
                    TileEntity outputTe = world.getTileEntity(pos.offset(outputSide));
                    if(outputTe instanceof TileEntityCraneInserterBase) {
                        TileEntityCraneInserterBase inserter = (TileEntityCraneInserterBase) outputTe;
                        for(int index : ALLOWED_SLOTS) {
                            ItemStack stack = inventory.getStackInSlot(index);

                            if(!stack.isEmpty()) {
                                boolean match = this.matchesFilter(stack);

                                if(isWhitelist == match) {
                                    int toSend = Math.min(getAmount(), stack.getCount());
                                    ItemStack cStack = stack.copy();
                                    cStack.setCount(toSend);

                                    int accepted = inserter.tryInsertDirect(cStack.copy());
                                    if(accepted > 0) {
                                        stack.shrink(accepted);
                                        if(stack.getCount() == 0) {
                                            inventory.setStackInSlot(index, ItemStack.EMPTY);
                                        }
                                        break;
                                    }
                                }
                            }
                        }
                    } else if(b instanceof IConveyorBelt) {
                        IConveyorBelt belt = (IConveyorBelt) b;
                        int xCoord = pos.getX();
                        int yCoord = pos.getY();
                        int zCoord = pos.getZ();

                        for(int index : ALLOWED_SLOTS) {
                            ItemStack stack = inventory.getStackInSlot(index);

                            if(!stack.isEmpty()) {
                                boolean match = this.matchesFilter(stack);

                                if(isWhitelist == match) {
                                    int toSend = Math.min(getAmount(), stack.getCount());
                                    ItemStack cStack = stack.copy();
                                    stack.shrink(toSend);

                                    if(stack.getCount() == 0) {
                                        inventory.setStackInSlot(index, ItemStack.EMPTY);
                                    }

                                    cStack.setCount(toSend);

                                    EntityMovingItem moving = new EntityMovingItem(world);
                                    Vec3d itemPos = new Vec3d(xCoord + 0.5 + outputSide.getDirectionVec().getX() * 0.55, yCoord + 0.5 + outputSide.getDirectionVec().getY() * 0.55, zCoord + 0.5 + outputSide.getDirectionVec().getZ() * 0.55);
                                    Vec3d snap = belt.getClosestSnappingPosition(world, new BlockPos(xCoord + outputSide.getDirectionVec().getX(), yCoord + outputSide.getDirectionVec().getY(), zCoord + outputSide.getDirectionVec().getZ()), itemPos);
                                    moving.setPosition(snap.x, snap.y, snap.z);
                                    moving.setItemStack(cStack);
                                    world.spawnEntity(moving);
                                    break;
                                }
                            }
                        }
                    }
                }
            }
        }

        networkPackNT(15);
    }

    private void pushToConveyorEveryTick(EnumFacing outputSide) {
        BlockPos conveyorPos = pos.offset(outputSide);
        Block block = world.getBlockState(conveyorPos).getBlock();

        if(!(block instanceof BlockConveyor)) {
            return;
        }

        BeltSegment segment = BeltSegmentManager.getOrCreateSegment(world, conveyorPos);
        if(segment == null) {
            return;
        }

        int blockIndex = segment.getBlockIndex(conveyorPos);
        if(blockIndex < 0) {
            return;
        }

        BlockConveyor conveyor = (BlockConveyor) block;
        EnumFacing conveyorFacing = conveyor.getLaneFacing(world, conveyorPos);

        if(conveyorFacing != outputSide) {
            return;
        }

        int amount = getAmount();

        // Перебираем ВСЕ слоты и выталкиваем максимум
        for(int index : ALLOWED_SLOTS) {
            ItemStack stack = inventory.getStackInSlot(index);

            if(!stack.isEmpty()) {
                boolean match = this.matchesFilter(stack);

                if(isWhitelist == match) {
                    int toSend = Math.min(amount, stack.getCount());

                    // Пытаемся вставить в любую свободную полосу
                    boolean inserted = false;
                    for(int lane = 0; lane < segment.getLaneCount(); lane++) {
                        BeltLane beltLane = segment.getLane(lane);
                        double slotProgress = blockIndex + BeltLane.ITEM_LENGTH * 0.5D;

                        if(beltLane.isSlotFree(slotProgress)) {
                            ItemStack toInsert = stack.copy();
                            toInsert.setCount(toSend);

                            if(segment.insertStack(toInsert, lane, slotProgress)) {
                                stack.shrink(toSend);
                                if(stack.getCount() == 0) {
                                    inventory.setStackInSlot(index, ItemStack.EMPTY);
                                }
                                segment.markDirty();
                                inserted = true;
                                break; // Переходим к следующему слоту
                            }
                        }
                    }

                    // Если не удалось вставить - конвейер заполнен, создаем очередь
                    if(!inserted) {
                        break; // Останавливаем выталкивание
                    }
                }
            }
        }
    }

    public int tryInsertDirect(ItemStack stack) {
        return tryInsertItemCap(inventory, stack, ALLOWED_SLOTS);
    }

    protected int getDelay() {
        int delay = 20;

        if(!inventory.getStackInSlot(19).isEmpty()) {
            if(inventory.getStackInSlot(19).getItem() == ModItems.upgrade_ejector_1) {
                delay = 10;
            } else if(inventory.getStackInSlot(19).getItem() == ModItems.upgrade_ejector_2) {
                delay = 5;
            } else if(inventory.getStackInSlot(19).getItem() == ModItems.upgrade_ejector_3) {
                delay = 2;
            }
        }

        return delay;
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

    public int tryInsertItemCap(IItemHandler chest, ItemStack stack, int[] allowedSlots) {
        if(stack.isEmpty()) {
            return 0;
        }

        int filledAmount = 0;

        for(int i : allowedSlots) {
            if(stack.isEmpty() || stack.getCount() < 1) {
                return filledAmount;
            }

            ItemStack outputStack = stack.copy();
            ItemStack chestItem = chest.getStackInSlot(i).copy();

            if(chestItem.isEmpty() || (Library.areItemStacksCompatible(outputStack, chestItem, false) && chestItem.getCount() < chestItem.getMaxStackSize())) {
                int fillAmount = Math.min(chestItem.getMaxStackSize() - chestItem.getCount(), outputStack.getCount());
                outputStack.setCount(fillAmount);

                ItemStack rest = chest.insertItem(i, outputStack, true);

                if(rest.getCount() < outputStack.getCount()) {
                    stack.shrink(fillAmount - rest.getCount());
                    filledAmount += fillAmount - rest.getCount();
                    chest.insertItem(i, outputStack, false);
                }
            }
        }

        return filledAmount;
    }

    public static int[] masquerade(ISidedInventory sided, EnumFacing side) {
        if(sided instanceof TileEntityFurnace) {
            return new int[] {2};
        }

        return sided.getSlotsForFace(side);
    }

    @Override
    public void serialize(ByteBuf buf) {
        buf.writeBoolean(this.isWhitelist);

        for(int i = 0; i < matcher.modes.length; i++) {
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
        this.matcher.modes = new String[this.matcher.modes.length];

        for(int i = 0; i < matcher.modes.length; i++) {
            if(buf.readBoolean()) {
                matcher.modes[i] = ByteBufUtils.readUTF8String(buf);
            } else {
                matcher.modes[i] = null;
            }
        }
    }

    public boolean matchesFilter(ItemStack stack) {
        for(int i = 0; i < 9; i++) {
            ItemStack filter = inventory.getStackInSlot(i);

            if(!filter.isEmpty() && this.matcher.isValidForFilter(filter, i, stack)) {
                return true;
            }
        }

        return false;
    }

    public void nextMode(int i) {
        this.matcher.nextMode(world, inventory.getStackInSlot(i), i);
    }

    public void initPattern(ItemStack stack, int index) {
        this.matcher.initPatternSmart(world, stack, index);
    }

    @Override
    public boolean isItemValidForSlot(int i, ItemStack itemStack) {
        return i > 8 && i < 18;
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
        return ALLOWED_SLOTS;
    }

    @Override
    public boolean canExtractItem(int slot, ItemStack itemStack, int amount) {
        return false;
    }
}