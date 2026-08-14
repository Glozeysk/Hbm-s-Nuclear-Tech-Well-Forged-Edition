package com.hbm.blocks.machine.dummy;

import api.hbm.energy.IEnergyConnectorBlock;
import com.hbm.blocks.ModBlocks;
import com.hbm.handler.RadiationSystemNT;
import com.hbm.interfaces.*;
import com.hbm.lib.ForgeDirection;
import com.hbm.main.MainRegistry;
import com.hbm.tileentity.machine.*;
import micdoodle8.mods.galacticraft.api.block.IPartialSealableBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockContainer;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumBlockRenderType;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.Optional;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.Random;

@Optional.InterfaceList({@Optional.Interface(iface = "micdoodle8.mods.galacticraft.api.block.IPartialSealableBlock", modid = "galacticraftcore")})
public class DummyBlockBase extends BlockContainer
        implements IDummy, IEnergyConnectorBlock, IBomb, IRadResistantBlock, IPartialSealableBlock {

    public static boolean safeBreak = false;

    public static boolean batchRemovalMode = false;

    protected final DummyProperties props;
    protected final boolean isPort;

    public DummyBlockBase(DummyProperties props, boolean isPort) {
        super(Material.IRON);
        this.props = props;
        this.isPort = isPort;

        String regName = isPort ? props.portRegistryName : props.blockRegistryName;
        this.setTranslationKey(regName);
        this.setRegistryName(regName);
        this.setCreativeTab(null);
        this.setHardness(props.hardness).setResistance(props.resistance);

        ModBlocks.ALL_BLOCKS.add(this);
    }
    public static void destroyQuietly(World world, BlockPos pos, boolean dropItems) {
        if (world.isRemote) return;

        safeBreak = true;
        batchRemovalMode = true;

        try {
            IBlockState state = world.getBlockState(pos);
            Block block = state.getBlock();

            TileEntity te = world.getTileEntity(pos);

            if (te != null) {
                world.removeTileEntity(pos);
            }

            if (block instanceof DummyBlockBase && ((DummyBlockBase) block).props.isRadResistant) {
                RadiationSystemNT.markChunkForRebuild(world, pos);
            }

            world.setBlockState(pos, Blocks.AIR.getDefaultState(), 3);

        } finally {
            safeBreak = false;
            batchRemovalMode = false;
        }
    }

    @Override
    @SideOnly(Side.CLIENT)
    public boolean addDestroyEffects(World world, BlockPos pos, net.minecraft.client.particle.ParticleManager manager) {
        return true;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public boolean addHitEffects(IBlockState state, World world, RayTraceResult target, net.minecraft.client.particle.ParticleManager manager) {
        return true;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void randomDisplayTick(IBlockState stateIn, World worldIn, BlockPos pos, Random rand) {
    }

    protected TileEntity createTileEntity(World world, int meta) {
        if (isPort) {
            switch (props.portType) {
                case FLUID: return new TileEntityDummyFluidPort();
                case PORT_NEW: return new TileEntityDummyPortNew();
                case ITEM_ENERGY: return new TileEntityDummyPort();
                default: return new TileEntityDummy();
            }
        }
        return new TileEntityDummy();
    }

    protected boolean onActivated(World world, BlockPos pos, EntityPlayer player, EnumHand hand,
                                  BlockPos targetPos, TileEntity targetTE) {
        if (props.isDoor) return handleDoorInteraction(world, targetTE, player, hand);
        if (!player.isSneaking() && props.guiId != -1 && targetTE != null) {
            player.openGui(MainRegistry.instance, props.guiId, world,
                    targetPos.getX(), targetPos.getY(), targetPos.getZ());
            return true;
        }
        return false;
    }

    protected void onBreakBlock(World world, BlockPos pos, TileEntity te) {
    }

    /** Дополнительная логика при установке. */
    protected void onBlockPlaced(World world, BlockPos pos) {
    }

    @Override
    public final TileEntity createNewTileEntity(World worldIn, int meta) {
        return createTileEntity(worldIn, meta);
    }

    @Override
    public final void breakBlock(World world, BlockPos pos, IBlockState state) {
        if (batchRemovalMode) {
            world.removeTileEntity(pos);
            return;
        }

        TileEntity te = world.getTileEntity(pos);

        if (te != null) onBreakBlock(world, pos, te);

        if (!safeBreak && te instanceof TileEntityDummy && ((TileEntityDummy) te).target != null) {
            BlockPos corePos = ((TileEntityDummy) te).target;
            if (!world.isRemote) {
                net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(
                        new com.hbm.event.DummyBlockEvent.DummyBroken(world, pos, corePos));
            }
        }
        world.removeTileEntity(pos);

        if (props.isRadResistant) {
            RadiationSystemNT.markChunkForRebuild(world, pos);
        }
    }

    @Override
    public final void onBlockAdded(World worldIn, BlockPos pos, IBlockState state) {
        if (props.isRadResistant) RadiationSystemNT.markChunkForRebuild(worldIn, pos);
        onBlockPlaced(worldIn, pos);
        super.onBlockAdded(worldIn, pos, state);
    }

    @Override
    public final boolean onBlockActivated(World world, BlockPos pos, IBlockState state,
                                          EntityPlayer player, EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ) {
        if (world.isRemote) return true;

        TileEntity te = world.getTileEntity(pos);
        if (!(te instanceof TileEntityDummy) || ((TileEntityDummy) te).target == null) return false;

        BlockPos targetPos = ((TileEntityDummy) te).target;
        TileEntity targetTE = world.getTileEntity(targetPos);

        return onActivated(world, pos, player, hand, targetPos, targetTE);
    }

    protected boolean handleDoorInteraction(World world, TileEntity targetTE, EntityPlayer player, EnumHand hand) {
        if (targetTE == null) return false;

        Item held = player.getHeldItemMainhand().getItem();
        if (held instanceof com.hbm.items.tool.ItemLock || held == com.hbm.items.ModItems.key_kit) return false;

        if (targetTE instanceof TileEntityVaultDoor) {
            TileEntityVaultDoor door = (TileEntityVaultDoor) targetTE;
            if (!player.isSneaking()) {
                if (door.canAccess(player)) { door.tryToggle(); return true; }
            } else if (props.hasVariantToggle) {
                door.type = (door.type + 1) % TileEntityVaultDoor.maxTypes;
                return true;
            }
        } else if (targetTE instanceof TileEntitySiloHatch) {
            TileEntitySiloHatch h = (TileEntitySiloHatch) targetTE;
            if (!player.isSneaking() && h.canAccess(player)) { h.tryToggle(); return true; }
        } else if (targetTE instanceof TileEntityBlastDoor) {
            TileEntityBlastDoor d = (TileEntityBlastDoor) targetTE;
            if (!player.isSneaking() && d.canAccess(player)) { d.tryToggle(); return true; }
        }
        return false;
    }

    @Override
    public final void explode(World world, BlockPos pos) {
        if (!props.isDoor) return;
        TileEntity te = world.getTileEntity(pos);
        if (!(te instanceof TileEntityDummy)) return;
        TileEntity target = world.getTileEntity(((TileEntityDummy) te).target);
        if (target == null) return;

        if (target instanceof TileEntityVaultDoor && !((TileEntityVaultDoor)target).isLocked()) ((TileEntityVaultDoor)target).tryToggle();
        if (target instanceof TileEntitySiloHatch && !((TileEntitySiloHatch)target).isLocked()) ((TileEntitySiloHatch)target).tryToggle();
        if (target instanceof TileEntityBlastDoor && !((TileEntityBlastDoor)target).isLocked()) ((TileEntityBlastDoor)target).tryToggle();
    }

    @Override
    public final boolean isSealed(World worldIn, BlockPos blockPos, EnumFacing direction) {
        if (!props.isDoor) return false;
        return getDoorState(worldIn, blockPos) == IDoor.DoorState.CLOSED;
    }

    @Override
    public final boolean isRadResistant(World worldIn, BlockPos blockPos) {
        if (!props.isRadResistant) return false;
        if (props.isDoor) return getDoorState(worldIn, blockPos) == IDoor.DoorState.CLOSED;
        return true;
    }

    private IDoor.DoorState getDoorState(World worldIn, BlockPos pos) {
        TileEntity te = worldIn.getTileEntity(pos);
        if (te instanceof TileEntityDummy && ((TileEntityDummy) te).target != null) {
            TileEntity t = worldIn.getTileEntity(((TileEntityDummy) te).target);
            if (t instanceof IDoor) return ((IDoor) t).getState();
        }
        return IDoor.DoorState.OPEN;
    }

    @Override
    public boolean canConnect(IBlockAccess world, BlockPos pos, ForgeDirection dir) {
        return isPort;
    }

    @Override
    public AxisAlignedBB getBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
        return props.boundingBox;
    }

    @Override
    public ItemStack getPickBlock(IBlockState state, RayTraceResult target, World world, BlockPos pos, EntityPlayer player) {
        return new ItemStack(Item.getItemFromBlock(props.dropBlock));
    }

    @Override
    public Item getItemDropped(IBlockState state, Random rand, int fortune) { return Items.AIR; }

    @Override
    public EnumBlockRenderType getRenderType(IBlockState state) { return EnumBlockRenderType.INVISIBLE; }

    @Override
    public boolean isOpaqueCube(IBlockState state) { return false; }
    @Override
    public boolean isBlockNormalCube(IBlockState state) { return false; }
    @Override
    public boolean isNormalCube(IBlockState state) { return false; }
    @Override
    public boolean isNormalCube(IBlockState state, IBlockAccess world, BlockPos pos) { return false; }
    @Override
    public boolean shouldSideBeRendered(IBlockState s, IBlockAccess w, BlockPos pos, EnumFacing side) { return false; }
}