package com.hbm.tileentity.machine.rbmk;

import java.util.List;
import java.util.Map;

import com.hbm.config.MobConfig;
import com.hbm.blocks.ModBlocks;
import com.hbm.blocks.machine.rbmk.RBMKBase;
import com.hbm.blocks.machine.rbmk.RBMKRod;
import com.hbm.entity.projectile.EntityRBMKDebris.DebrisType;
import com.hbm.interfaces.IControlReceiver;
import com.hbm.interfaces.ICopiable;
import com.hbm.items.ModItems;
import com.hbm.items.machine.ItemRBMKRod;
import com.hbm.lib.ForgeDirection;
import com.hbm.saveddata.RadiationSavedData;
import com.hbm.inventory.control_panel.DataValue;
import com.hbm.inventory.control_panel.DataValueFloat;
import com.hbm.inventory.control_panel.DataValueString;
import com.hbm.render.amlfrom1710.Vec3;
import com.hbm.tileentity.IBufPacketReceiver;
import com.hbm.tileentity.machine.rbmk.TileEntityRBMKConsole.ColumnType;

import io.netty.buffer.ByteBuf;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;

public class TileEntityRBMKRod extends TileEntityRBMKSlottedBase implements IRBMKFluxReceiver, IRBMKLoadable, ICopiable, IControlReceiver, IBufPacketReceiver {
	
	public double fluxFast;
	public double fluxSlow;
	public boolean hasRod;

	public double fluxOut = 0;

	public float fuelR;
	public float fuelG;
	public float fuelB;
	public float cherenkovR;
	public float cherenkovG;
	public float cherenkovB;
	public volatile int DepletionToExtract = 50;


	public TileEntityRBMKRod() {
		super(1);
	}

	@Override
	public String getName() {
		return "container.rbmkRod";
	}

	public boolean getDepletion(ItemStack stack) {
		ItemRBMKRod rod = ((ItemRBMKRod)inventory.getStackInSlot(0).getItem());
		if(((((rod.yield - ItemRBMKRod.getYield(stack)) / rod.yield) * 100000) / 1000) >= DepletionToExtract){
			return true;
		}
		return false;
	}
	
	@Override
	public boolean isModerated() {
		return ((RBMKRod)this.getBlockType()).moderated;
	}

	@Override
	public boolean isHeatproof() {
		return ((RBMKRod)this.getBlockType()).heatproof;
	}

	@SuppressWarnings("incomplete-switch")
	@Override
	public void receiveFlux(NType type, double flux) {
		
		switch(type) {
		case FAST: this.fluxFast += flux; break;
		case SLOW: this.fluxSlow += flux; break;
		}
	}
	
	@Override
	public void update() {

		if(!world.isRemote) {
			
			if(inventory.getStackInSlot(0).getItem() instanceof ItemRBMKRod) {
				ItemRBMKRod rod = ((ItemRBMKRod)inventory.getStackInSlot(0).getItem());
				this.fuelR = rod.fuelR;
				this.fuelG = rod.fuelG;
				this.fuelB = rod.fuelB;
				this.cherenkovR = rod.cherenkovR;
				this.cherenkovG = rod.cherenkovG;
				this.cherenkovB = rod.cherenkovB;
				
				double fluxIn = fluxFromType(rod.nType);
				fluxOut = rod.burn(world, inventory.getStackInSlot(0), fluxIn);
				NType rType = rod.rType;
				
				rod.updateHeat(world, inventory.getStackInSlot(0), 1.0D);
				this.heat += rod.provideHeat(world, inventory.getStackInSlot(0), heat, 1.0D);
				
				
				if(!this.hasLid()) {
					RadiationSavedData.incrementRad(world, pos, (float) ((this.fluxFast + this.fluxSlow) * 0.05F), (float) ((this.fluxFast + this.fluxSlow) * 10F));
				} else{
					double meltdownPercent = rod.getMeltdownPercent(inventory.getStackInSlot(0));
					if(meltdownPercent > 0){
						RadiationSavedData.incrementRad(world, pos, (float) ((this.fluxFast + this.fluxSlow) * 0.05F * meltdownPercent * 0.01D), (float) ((this.fluxFast + this.fluxSlow) * meltdownPercent * 0.1D));
					}
				}
				
				super.update();
				this.fluxFast = 0;
				this.fluxSlow = 0;

				if(this.heat > this.maxHeat()) {
					this.meltdown();
					return;
				}
				
				if(fluxOut > 0){
					spreadFlux(this.isModerated() ? NType.SLOW : rType, fluxOut);
				}
				
				hasRod = true;
			} else {

				this.fluxFast = 0;
				this.fluxSlow = 0;
				
				hasRod = false;
				
				super.update();
			}
		}
	}
	
	private double fluxFromType(NType type) {
		
		switch(type) {
		case SLOW: return this.fluxFast * 0.5D + this.fluxSlow;
		case FAST: return this.fluxFast + this.fluxSlow * 0.3D;
		case ANY: return this.fluxFast + this.fluxSlow;
		}
		
		return 0.0D;
	}
	
	public static final ForgeDirection[] fluxDirs = new ForgeDirection[] {
			ForgeDirection.NORTH,
			ForgeDirection.EAST,
			ForgeDirection.SOUTH,
			ForgeDirection.WEST
	};
	
	protected static NType stream;
	
	protected void spreadFlux(NType type, double fluxOut) {
		
		int range = RBMKDials.getFluxRange(world);
		
		for(ForgeDirection dir : fluxDirs) {
			
			stream = type;
			double flux = fluxOut;
			
			for(int i = 1; i <= range; i++) {
				
				flux = runInteraction(pos.getX() + dir.offsetX * i, pos.getY(), pos.getZ() + dir.offsetZ * i, flux);
				
				if(flux <= 0)
					break;
			}
		}
	}
	
	protected double runInteraction(int x, int y, int z, double flux) {
		
		TileEntity te = world.getTileEntity(new BlockPos(x, y, z));
		
		if(te instanceof TileEntityRBMKBase) {
			TileEntityRBMKBase base = (TileEntityRBMKBase) te;
			
			if(!base.hasLid())
				RadiationSavedData.incrementRad(world, pos, (float) (flux * 0.05F), Float.MAX_VALUE);
			
			if(base.isModerated()) {
				TileEntityRBMKRod.stream = NType.SLOW;
			}
		}

		if(te instanceof TileEntityRBMKRod) {
			TileEntityRBMKRod rod = (TileEntityRBMKRod)te;
			
			if(rod.inventory.getStackInSlot(0).getItem() instanceof ItemRBMKRod) {
				rod.receiveFlux(stream, flux);
				return 0;
			}
		}
		if(te instanceof IRBMKFluxReceiver) {
			IRBMKFluxReceiver rod = (IRBMKFluxReceiver)te;
			rod.receiveFlux(stream, flux);
			return 0;
		}
		
		if(te instanceof TileEntityRBMKControl) {
			TileEntityRBMKControl control = (TileEntityRBMKControl)te;
			
			if(control.getMult() == 0.0D)
				return 0;
			
			flux *= control.getMult();
			
			return flux;
		}
		
		if(te instanceof TileEntityRBMKModerator) {
			stream = NType.SLOW;
			return flux;
		}
		
		if(te instanceof TileEntityRBMKReflector) {
			this.receiveFlux(this.isModerated() ? NType.SLOW : stream, flux);
			return 0;
		}
		
		if(te instanceof TileEntityRBMKAbsorber) {
			return 0;
		}
		
		if(te instanceof TileEntityRBMKBase) {
			return flux;
		}
		
		int limit = RBMKDials.getColumnHeight(world);
		int hits = 0;
		for(int h = 0; h <= limit; h++) {
			
			if(!world.getBlockState(new BlockPos(x, y, z)).isOpaqueCube())
				hits++;
		}
		
		if(hits > 0)
			RadiationSavedData.incrementRad(world, pos, (float) (flux * 0.05F * hits / (float)limit), (float) (flux * 0.05F * hits / (float)limit) * 10F);
		
		return 0;
	}
	
	@Override
	public void readFromNBT(NBTTagCompound nbt) {
		super.readFromNBT(nbt);

		this.fluxFast = nbt.getDouble("fluxFast");
		this.fluxSlow = nbt.getDouble("fluxSlow");
		DepletionToExtract = nbt.getInteger("DepletionToExtract");
		this.hasRod = nbt.getBoolean("hasRod");
		this.fuelR = nbt.getFloat("fuelR");
		this.fuelG = nbt.getFloat("fuelG");
		this.fuelB = nbt.getFloat("fuelB");
		this.cherenkovR = nbt.getFloat("cherenkovR");
		this.cherenkovG = nbt.getFloat("cherenkovG");
		this.cherenkovB = nbt.getFloat("cherenkovB");
	}
	
	@Override
	public NBTTagCompound writeToNBT(NBTTagCompound nbt) {
		super.writeToNBT(nbt);

		nbt.setDouble("fluxFast", this.fluxFast);
		nbt.setDouble("fluxSlow", this.fluxSlow);
		nbt.setInteger("DepletionToExtract", DepletionToExtract);
		nbt.setBoolean("hasRod", this.hasRod);
		nbt.setFloat("fuelR", this.fuelR);
		nbt.setFloat("fuelG", this.fuelG);
		nbt.setFloat("fuelB", this.fuelB);
		nbt.setFloat("cherenkovR", this.cherenkovR);
		nbt.setFloat("cherenkovG", this.cherenkovG);
		nbt.setFloat("cherenkovB", this.cherenkovB);
		return nbt;
	}

	@Override
	public void serialize(ByteBuf buf) {
		super.serialize(buf);
		buf.writeDouble(this.fluxFast);
		buf.writeDouble(this.fluxSlow);
		buf.writeBoolean(this.hasRod);
		buf.writeDouble(this.fluxOut);
		buf.writeFloat(this.fuelR);
		buf.writeFloat(this.fuelG);
		buf.writeFloat(this.fuelB);
		buf.writeFloat(this.cherenkovR);
		buf.writeFloat(this.cherenkovG);
		buf.writeFloat(this.cherenkovB);
		buf.writeInt(this.DepletionToExtract);
	}

	@Override
	public void deserialize(ByteBuf buf) {
		super.deserialize(buf);
		this.fluxFast = buf.readDouble();
		this.fluxSlow = buf.readDouble();
		this.hasRod = buf.readBoolean();
		this.fluxOut = buf.readDouble();
		this.fuelR = buf.readFloat();
		this.fuelG = buf.readFloat();
		this.fuelB = buf.readFloat();
		this.cherenkovR = buf.readFloat();
		this.cherenkovG = buf.readFloat();
		this.cherenkovB = buf.readFloat();
		this.DepletionToExtract = buf.readInt();
	}

	@Override
	public boolean hasPermission(EntityPlayer player) {
		return Vec3.createVectorHelper(pos.getX() - player.posX, pos.getY() - player.posY, pos.getZ() - player.posZ).length() < 20;
	}
	
	public void getDiagData(NBTTagCompound nbt) {
		this.writeToNBT(nbt);
		
		if(inventory.getStackInSlot(0).getItem() instanceof ItemRBMKRod) {
			
			ItemRBMKRod rod = ((ItemRBMKRod)inventory.getStackInSlot(0).getItem());

			nbt.setString("f_yield", ItemRBMKRod.getYield(inventory.getStackInSlot(0)) + " / " + rod.yield + " (" + (ItemRBMKRod.getEnrichment(inventory.getStackInSlot(0)) * 100) + "%)");
			nbt.setString("f_xenon", ItemRBMKRod.getPoison(inventory.getStackInSlot(0)) + "%");
			nbt.setString("f_heat", ItemRBMKRod.getCoreHeat(inventory.getStackInSlot(0)) + " / " + ItemRBMKRod.getHullHeat(inventory.getStackInSlot(0))  + " / " + rod.meltingPoint);
		}
		nbt.removeTag("fuelR");
		nbt.removeTag("fuelG");
		nbt.removeTag("fuelB");
		nbt.removeTag("cherenkovR");
		nbt.removeTag("cherenkovG");
		nbt.removeTag("cherenkovB");
		nbt.removeTag("jumpheight");
		nbt.removeTag("steam");
		nbt.removeTag("water");
	}
	
	@Override
	public void onMelt(int reduce) {
		int h = RBMKDials.getColumnHeight(world);
		reduce = MathHelper.clamp(reduce, 1, h);
		
		if(world.rand.nextInt(3) == 0)
			reduce++;
		
		boolean corium = inventory.getStackInSlot(0).getItem() instanceof ItemRBMKRod;
		
		if(corium && (inventory.getStackInSlot(0).getItem() == ModItems.rbmk_fuel_drx || inventory.getStackInSlot(0).getItem() == ModItems.rbmk_fuel_spk)) 
			RBMKBase.digamma = true;
		
		inventory.setStackInSlot(0, ItemStack.EMPTY);

		if(corium) {
			
			for(int i = h; i >= 0; i--) {
				
				if(i <= h + 1 - reduce) {
					world.setBlockState(new BlockPos(pos.getX(), pos.getY() + i, pos.getZ()), ModBlocks.corium_block.getDefaultState());
				} else {
					world.setBlockState(new BlockPos(pos.getX(), pos.getY() + i, pos.getZ()), Blocks.AIR.getDefaultState());
				}
				IBlockState state = world.getBlockState(pos.up(i));
				world.notifyBlockUpdate(pos.up(i), state, state, 3);
			}
			
			int count = 1 + world.rand.nextInt(RBMKDials.getColumnHeight(world));
			
			for(int i = 0; i < count; i++) {
				spawnDebris(DebrisType.FUEL);
			}
		} else {
			this.standardMelt(reduce);
		}
		
		spawnDebris(DebrisType.ELEMENT);
		
		if(this.getBlockMetadata() == RBMKBase.DIR_NORMAL_LID.ordinal() + RBMKBase.offset)
			spawnDebris(DebrisType.LID);

		if(MobConfig.enableElementals) {
			List<EntityPlayer> players = world.getEntitiesWithinAABB(EntityPlayer.class, new AxisAlignedBB(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5).grow(100, 100, 100));

			for(EntityPlayer player : players) {
				player.getEntityData().getCompoundTag(EntityPlayer.PERSISTED_NBT_TAG).setBoolean("radMark", true);
			}
		}
	}

	@Override
	public ColumnType getConsoleType() {
		return ColumnType.FUEL;
	}

	@Override
	public NBTTagCompound getNBTForConsole() {
		NBTTagCompound data = new NBTTagCompound();
		
		if(inventory.getStackInSlot(0).getItem() instanceof ItemRBMKRod) {
			
			ItemRBMKRod rod = ((ItemRBMKRod)inventory.getStackInSlot(0).getItem());
			data.setString("rod_name", rod.getTranslationKey());
			data.setDouble("enrichment", ItemRBMKRod.getEnrichment(inventory.getStackInSlot(0)));
			data.setDouble("xenon", ItemRBMKRod.getPoison(inventory.getStackInSlot(0)));
			data.setDouble("c_heat", ItemRBMKRod.getHullHeat(inventory.getStackInSlot(0)));
			data.setDouble("c_coreHeat", ItemRBMKRod.getCoreHeat(inventory.getStackInSlot(0)));
			data.setDouble("c_maxHeat", rod.meltingPoint);
			data.setDouble("meltdown", ItemRBMKRod.getMeltdownPercent(inventory.getStackInSlot(0)));
		}
		data.setDouble("flux", this.fluxOut);
		return data;
	}

	@Override
	public boolean canLoad(ItemStack toLoad) {
		return toLoad != null && inventory.getStackInSlot(0).isEmpty();
	}

	@Override
	public void load(ItemStack toLoad) {
		inventory.setStackInSlot(0, toLoad.copy());
		this.markDirty();
	}

	@Override
	public boolean canUnload() {
		return !inventory.getStackInSlot(0).isEmpty();
	}

	@Override
	public ItemStack provideNext() {
		return inventory.getStackInSlot(0);
	}

	@Override
	public void unload() {
		inventory.setStackInSlot(0, ItemStack.EMPTY);
		this.markDirty();
	}

	@Override
	public Map<String, DataValue> getQueryData() {
		Map<String, DataValue> data = super.getQueryData();

		if (inventory.getStackInSlot(0).getItem() instanceof ItemRBMKRod) {
			ItemRBMKRod rod = ((ItemRBMKRod)inventory.getStackInSlot(0).getItem());
			data.put("rod_name", new DataValueString(rod.getTranslationKey()));
			data.put("enrichment", new DataValueFloat((float) ItemRBMKRod.getEnrichment(inventory.getStackInSlot(0))));
			data.put("xenon", new DataValueFloat((float) ItemRBMKRod.getPoison(inventory.getStackInSlot(0))));
			data.put("c_heat", new DataValueFloat((float) ItemRBMKRod.getHullHeat(inventory.getStackInSlot(0))));
			data.put("c_coreHeat", new DataValueFloat((float) ItemRBMKRod.getCoreHeat(inventory.getStackInSlot(0))));
			data.put("c_maxHeat", new DataValueFloat((float) rod.meltingPoint));
			data.put("meltdown", new DataValueFloat((float) ItemRBMKRod.getMeltdownPercent(inventory.getStackInSlot(0))));
		}
		data.put("flux", new DataValueFloat((float) this.fluxOut));

		return data;
	}

	@Override
    public void receiveControl(NBTTagCompound data) {
        if (data.hasKey("minus")) {
            if (this.DepletionToExtract > 5) {
                this.DepletionToExtract -= 5;
            } else if (this.DepletionToExtract > 1) {
                this.DepletionToExtract -= 4;
            }
        }
        
        if (data.hasKey("plus")) {
            if (this.DepletionToExtract == 1) {
                this.DepletionToExtract += 4;
            } else if (this.DepletionToExtract >= 5 && this.DepletionToExtract < 95) {
                this.DepletionToExtract += 5;
            }
        }
        
        this.DepletionToExtract = MathHelper.clamp(this.DepletionToExtract, 1, 95);
        this.markChanged();
    }

	@Override
    public NBTTagCompound getSettings(World world, int x, int y, int z) {
        NBTTagCompound data = new NBTTagCompound();
        data.setInteger("DepletionToExtract", DepletionToExtract);
        return data;
    }

    @Override
    public void pasteSettings(NBTTagCompound nbt, int index, World world, EntityPlayer player, int x, int y, int z) {
        if(nbt.hasKey("DepletionToExtract")) {
            this.DepletionToExtract = MathHelper.clamp(nbt.getInteger("DepletionToExtract"), 1, 95);
        }
    }

	@Override
	public int[] getAccessibleSlotsFromSide(EnumFacing e) {
		return new int[] { 0 };
	}

	@Override
	public boolean canExtractItem(int slot, ItemStack itemStack, int amount) {
		if(itemStack.getItem() instanceof ItemRBMKRod && this.getDepletion(itemStack)) {
			return true;
		}
		return false;
	}
}