package com.hbm.tileentity.network;

import com.hbm.interfaces.IControlReceiver;
import com.hbm.inventory.container.ContainerCraneRouter;
import com.hbm.inventory.gui.GUICraneRouter;
import com.hbm.modules.ModulePatternMatcher;
import com.hbm.tileentity.IBufPacketReceiver;
import com.hbm.tileentity.IGUIProvider;
import com.hbm.tileentity.TileEntityMachineBase;

import io.netty.buffer.ByteBuf;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ITickable;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.jetbrains.annotations.NotNull;

public class TileEntityCraneRouter extends TileEntityMachineBase implements IGUIProvider, IControlReceiver, ITickable, IBufPacketReceiver {
    public ModulePatternMatcher[] patterns = new ModulePatternMatcher[6];
    public int[] modes = new int[6];
    public static final int MODE_NONE = 0;
    public static final int MODE_WHITELIST = 1;
    public static final int MODE_BLACKLIST = 2;
    public static final int MODE_WILDCARD = 3;

    public TileEntityCraneRouter() {
        super(5 * 6);

        for(int i = 0; i < patterns.length; i++) {
            patterns[i] = new ModulePatternMatcher(5);
        }
    }

    @Override
    public String getName() {
        return "container.craneRouter";
    }

    @Override
    public void update() {
        if(!world.isRemote) {
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
        modes[i]++;
        if(modes[i] > 3)
            modes[i] = 0;
    }
}