package com.hbm.tileentity.machine;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import com.hbm.blocks.ModBlocks;
import com.hbm.blocks.machine.BlockHadronCoil;
import com.hbm.blocks.machine.BlockHadronPlating;
import com.hbm.handler.threading.PacketThreading;
import com.hbm.inventory.HadronRecipes;
import com.hbm.items.ModItems;
import com.hbm.lib.ForgeDirection;
import com.hbm.lib.Library;
import com.hbm.main.AdvancementManager;
import com.hbm.packet.AuxParticlePacketNT;
import com.hbm.packet.PacketDispatcher;
import com.hbm.tileentity.IBufPacketReceiver;
import com.hbm.tileentity.TileEntityMachineBase;
import com.hbm.tileentity.machine.TileEntityHadronDiode.DiodeConfig;

import com.hbm.items.machine.ItemFWatzCore;
import api.hbm.energy.IEnergyUser;
import io.netty.buffer.ByteBuf;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ITickable;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.common.network.NetworkRegistry.TargetPoint;

public class TileEntityHadron extends TileEntityMachineBase implements ITickable, IEnergyUser, IBufPacketReceiver {

	public long power;
	public static final long maxPower = 10000000;

	public boolean isOn = false;
	public boolean analysisOnly = false;
	public boolean hopperMode = false;
	
	private int delay;
	public EnumHadronState state = EnumHadronState.IDLE;
	private static final int delaySuccess = 20;
	private static final int delayNoResult = 60;
	private static final int delayError = 100;

	public boolean stat_success = false;
	public EnumHadronState stat_state = EnumHadronState.IDLE;
	public int stat_charge = 0;
	public int stat_x = 0;
	public int stat_y = 0;
	public int stat_z = 0;
	
	
	private static final int[] access = new int[] {0, 1, 2, 3};
	
	public TileEntityHadron() {
		super(5);
	}

	@Override
	public String getName() {
		return "container.hadron";
	}
	
	@Override
	public int[] getAccessibleSlotsFromSide(EnumFacing e) {
		return access;
	}
	
	@Override
	public boolean canExtractItem(int slot, ItemStack itemStack, int amount) {
		return slot == 2 || slot == 3;
	}
	
	@Override
	public boolean isItemValidForSlot(int i, ItemStack stack) {
		return i == 0 || i == 1;
	}
	
	@Override
	public void update() {
		if(!world.isRemote){

			power = Library.chargeTEFromItems(inventory, 4, power, maxPower);
			drawPower();

			if(delay <= 0 && this.isOn && particles.size() < maxParticles && !inventory.getStackInSlot(0).isEmpty() && !inventory.getStackInSlot(1).isEmpty() && power >= maxPower * 0.75 && (inventory.getStackInSlot(2).getCount() < inventory.getStackInSlot(2).getMaxStackSize()) && (inventory.getStackInSlot(3).getCount() < inventory.getStackInSlot(3).getMaxStackSize()) && !(inventory.getStackInSlot(2).getItem() instanceof ItemFWatzCore) && !(inventory.getStackInSlot(3).getItem() instanceof ItemFWatzCore)) {
				if(!hopperMode || (inventory.getStackInSlot(0).getCount() > 1 && inventory.getStackInSlot(1).getCount() > 1)) {
					ForgeDirection dir = ForgeDirection.getOrientation(this.getBlockMetadata());
					particles.add(new Particle(inventory.getStackInSlot(0), inventory.getStackInSlot(1), dir, pos.getX(), pos.getY(), pos.getZ()));
					inventory.getStackInSlot(0).shrink(1);
					inventory.getStackInSlot(1).shrink(1);
					power -= maxPower * 0.75;
					this.state = EnumHadronState.PROGRESS;
				}
			}
			
			if(delay > 0)
				delay--;
			else if(particles.isEmpty()) {
				this.state = EnumHadronState.IDLE;
			}

			if(!particles.isEmpty())
				updateParticles();

			for(Particle p : particlesToRemove) {
				particles.remove(p);
			}
			particlesToRemove.clear();
			
			networkPackNT(50);
		}
		
	}
	
	private void process(Particle p) {

		ItemStack[] result = HadronRecipes.getOutput(p.item1, p.item2, p.momentum, analysisOnly);

		if(result == null) {
			this.state = HadronRecipes.returnCode;
			this.setStats(this.state, p.momentum, false);
			this.delay = delayNoResult;
			world.playSound(null, p.posX, p.posY, p.posZ, SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, SoundCategory.BLOCKS, 2, 0.5F);
			return;
		}

		if((inventory.getStackInSlot(2).isEmpty() || (inventory.getStackInSlot(2).getItem() == result[0].getItem() && inventory.getStackInSlot(2).getCount() < inventory.getStackInSlot(2).getMaxStackSize())) &&
				(inventory.getStackInSlot(3).isEmpty() || (inventory.getStackInSlot(3).getItem() == result[1].getItem() && inventory.getStackInSlot(3).getCount() < inventory.getStackInSlot(3).getMaxStackSize()))) {

			for(int i = 2; i <= 3; i++ ) {

				if(inventory.getStackInSlot(i).isEmpty())
					inventory.setStackInSlot(i, result[i-2].copy());
				else
					inventory.getStackInSlot(i).grow(1);
			}
			
			if(result[0].getItem() == ModItems.particle_digamma) {
				List<EntityPlayer> players = world.getEntitiesWithinAABB(EntityPlayer.class,
						new AxisAlignedBB(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5)
						.grow(128, 50, 128));

				for(EntityPlayer player : players)
					AdvancementManager.grantAchievement(player, AdvancementManager.achOmega12);
			}
		}
		
		world.playSound(null, p.posX, p.posY, p.posZ, SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, SoundCategory.BLOCKS, 2, 1F);
		this.delay = delaySuccess;
		this.state = EnumHadronState.SUCCESS;
		this.setStats(this.state, p.momentum, true);
	}

	@Override
	public void serialize(ByteBuf buf) {
		buf.writeBoolean(isOn);
		buf.writeLong(power);
		buf.writeBoolean(analysisOnly);
		buf.writeBoolean(hopperMode);
		buf.writeByte((byte) state.ordinal());
		buf.writeBoolean(stat_success);
		buf.writeByte((byte) stat_state.ordinal());
		buf.writeInt(stat_charge);
		buf.writeInt(stat_x);
		buf.writeInt(stat_y);
		buf.writeInt(stat_z);
	}

	@Override
	public void deserialize(ByteBuf buf) {
		this.isOn = buf.readBoolean();
		this.power = buf.readLong();
		this.analysisOnly = buf.readBoolean();
		this.hopperMode = buf.readBoolean();
		this.state = EnumHadronState.values()[buf.readByte()];
		this.stat_success = buf.readBoolean();
		this.stat_state = EnumHadronState.values()[buf.readByte()];
		this.stat_charge = buf.readInt();
		this.stat_x = buf.readInt();
		this.stat_y = buf.readInt();
		this.stat_z = buf.readInt();
	}
	
	@Override
	public void handleButtonPacket(int value, int meta) {
		if(meta == 0)
			this.isOn = !this.isOn;
		if(meta == 1)
			this.analysisOnly = !this.analysisOnly;
		if(meta == 2)
			this.hopperMode = !this.hopperMode;
	}
	
	private void drawPower() {

		for(ForgeDirection dir : getRandomDirs()) {

			if(power == maxPower)
				return;

			int x = pos.getX() + dir.offsetX * 2;
			int y = pos.getY() + dir.offsetY * 2;
			int z = pos.getZ() + dir.offsetZ * 2;

			TileEntity te = world.getTileEntity(new BlockPos(x, y, z));

			if(te instanceof TileEntityHadronPower) {

				TileEntityHadronPower plug = (TileEntityHadronPower)te;

				long toDraw = Math.min(maxPower - power, plug.getPower());
				this.setPower(power + toDraw);
				plug.setPower(plug.getPower() - toDraw);
			}
		}
	}
	
	private void finishParticle(Particle p) {
		particlesToRemove.add(p);
		if(!p.isExpired())
			process(p);

		p.expired = true;
	}
	
	@Override
	public NBTTagCompound writeToNBT(NBTTagCompound compound) {
		compound.setBoolean("isOn", isOn);
		compound.setLong("power", power);
		compound.setBoolean("analysis", analysisOnly);
		compound.setBoolean("hopperMode", hopperMode);
		return super.writeToNBT(compound);
	}
	
	@Override
	public void readFromNBT(NBTTagCompound compound) {
		this.isOn = compound.getBoolean("isOn");
		this.power = compound.getLong("power");
		this.analysisOnly = compound.getBoolean("analysis");
		this.hopperMode = compound.getBoolean("hopperMode");
		super.readFromNBT(compound);
	}
	
	public int getPowerScaled(int i) {
		return (int)(power * i / maxPower);
	}

	static final int maxParticles = 1;
	List<Particle> particles = new ArrayList<>();
	List<Particle> particlesToRemove = new ArrayList<>();

	private void updateParticles() {

		for(Particle particle : particles) {
			particle.update();
		}
	}

	@Override
	public void setPower(long i) {
		power = i;
		markDirty();
	}

	@Override
	public long getPower() {
		return power;
	}

	@Override
	public long getMaxPower() {
		return maxPower;
	}
	
	private void setStats(EnumHadronState state, int count, boolean success) {
		this.stat_state = state;
		this.stat_charge = count;
		this.stat_success = success;
	}
	
	private void setExpireStats(EnumHadronState state, int count, int x, int y, int z) {
		this.stat_state = state;
		this.stat_charge = count;
		this.stat_x = x;
		this.stat_y = y;
		this.stat_z = z;
		this.stat_success = false;
	}

	public class Particle {

		ItemStack item1;
		ItemStack item2;
		ForgeDirection dir;
		int posX;
		int posY;
		int posZ;

		int momentum;
		int charge;
		int analysis;
		boolean isCheckExempt = false;

		boolean expired = false;

		public Particle(ItemStack item1, ItemStack item2, ForgeDirection dir, int posX, int posY, int posZ) {
			this.item1 = item1.copy();
			this.item2 = item2.copy();
			this.item1.setCount(1);
			this.item2.setCount(1);
			this.dir = dir;
			this.posX = posX;
			this.posY = posY;
			this.posZ = posZ;

			this.charge = 0;
			this.momentum = 0;
		}
		
		public void expire(EnumHadronState reason) {
			if(expired)
				return;

			this.expired = true;
			particlesToRemove.add(this);
			world.newExplosion(null, posX + 0.5, posY + 0.5, posZ + 0.5, 10, false, false);

			TileEntityHadron.this.state = reason;
			TileEntityHadron.this.delay = delayError;
			TileEntityHadron.this.setExpireStats(reason, this.momentum, posX, posY, posZ);
		}

		public boolean isExpired() {
			return this.expired;
		}

		public void update() {
			if(expired)
				return;

			changeDirection(this);
			makeSteppy(this);
			if(!this.isExpired())
				checkSegment(this);
			isCheckExempt = false;

			if(charge <= 0)
				this.expire(EnumHadronState.ERROR_NO_CHARGE);
		}
	}

	public void makeSteppy(Particle p) {

		ForgeDirection dir = p.dir;

		p.posX += dir.offsetX;
		p.posY += dir.offsetY;
		p.posZ += dir.offsetZ;

		int x = p.posX;
		int y = p.posY;
		int z = p.posZ;

		IBlockState blockState = world.getBlockState(new BlockPos(x, y, z));
		Block block = blockState.getBlock();
		TileEntity te = world.getTileEntity(new BlockPos(x, y, z));

		if(te instanceof TileEntityHadron) {

			if(p.analysis != 3)
				p.expire(EnumHadronState.ERROR_NO_ANALYSIS);
			else
				this.finishParticle(p);

			return;
		}

		if(blockState.getMaterial() != Material.AIR && block != ModBlocks.hadron_diode)
			p.expire(EnumHadronState.ERROR_OBSTRUCTED_CHANNEL);
		
		if(block == ModBlocks.hadron_diode)
			p.isCheckExempt = true;

		if(coilValue(world.getBlockState(new BlockPos(x + dir.offsetX, y + dir.offsetY, z + dir.offsetZ)).getBlock()) > 0)
			p.isCheckExempt = true;
	}

	public void checkSegment(Particle p) {

		ForgeDirection dir = p.dir;
		int x = p.posX;
		int y = p.posY;
		int z = p.posZ;

		int dX = 1 - Math.abs(dir.offsetX);
		int dY = 1 - Math.abs(dir.offsetY);
		int dZ = 1 - Math.abs(dir.offsetZ);

		boolean analysis = true;

		for(int a = x - dX * 2; a <= x + dX * 2; a++) {
			for(int b = y - dY * 2; b <= y + dY * 2; b++) {
				for(int c = z - dZ * 2; c <= z + dZ * 2;c++) {

					IBlockState blockState = world.getBlockState(new BlockPos(a, b, c));
					Block block = blockState.getBlock();

					if(a == x && b == y && c == z) {

						if(blockState.getMaterial() != Material.AIR)
							analysis = false;

						continue;
					}

					int ix = Math.abs(x - a);
					int iy = Math.abs(y - b);
					int iz = Math.abs(z - c);

					if(ix <= 1 && iy <= 1 && iz <= 1) {

						if(p.isCheckExempt && ix + iy + iz == 1) {
							continue;
						}

						if(blockState.getMaterial() == Material.AIR && analysis) {
							continue;
						}

						analysis = false;

						int coilVal = coilValue(block);

						if(coilVal == 0) {
							p.expire(EnumHadronState.ERROR_EXPECTED_COIL);
						} else {
							p.momentum += coilVal;
							p.charge -= coilVal;
						}

						continue;
					}

					if(ix + iy + iz <= 3) {

						if(isAnalysis(block))
							continue;

						analysis = false;

						if(isPlating(block))
							continue;

						TileEntity te = world.getTileEntity(new BlockPos(a, b, c));

						if(te instanceof TileEntityHadronPower) {

							TileEntityHadronPower plug = (TileEntityHadronPower)te;

							long bit = 10000;

							int times = (int) (plug.getPower() / bit);

							p.charge += times;

							plug.setPower(plug.getPower() - times * bit);

							continue;
						}

						if(p.isCheckExempt && ix + iy + iz == 2) {
							continue;
						}

						p.expire(EnumHadronState.ERROR_MALFORMED_SEGMENT);
					}
				}
			}
		}

		if(analysis) {

			p.analysis++;

			if(p.analysis > 3)
				p.expire(EnumHadronState.ERROR_ANALYSIS_TOO_LONG);

			if(p.analysis == 2) {
				this.world.playSound(null, p.posX + 0.5, p.posY + 0.5, p.posZ + 0.5, SoundEvents.ENTITY_FIREWORK_BLAST, SoundCategory.BLOCKS, 2.0F, 2F);
				this.state = EnumHadronState.ANALYSIS;
				NBTTagCompound data = new NBTTagCompound();
				data.setString("type", "hadron");
				PacketThreading.createAllAroundThreadedPacket(new AuxParticlePacketNT(data, p.posX + 0.5, p.posY + 0.5, p.posZ + 0.5), new TargetPoint(world.provider.getDimension(), p.posX + 0.5, p.posY + 0.5, p.posZ + 0.5, 25));
			}

			if(this.analysisOnly && p.analysis == 2) {
				this.finishParticle(p);
			}

		} else {

			if(p.analysis > 0 && p.analysis < 3)
				p.expire(EnumHadronState.ERROR_ANALYSIS_TOO_SHORT);
		}
	}

	public void changeDirection(Particle p) {

		ForgeDirection dir = p.dir;

		int x = p.posX;
		int y = p.posY;
		int z = p.posZ;

		int nx = x + dir.offsetX;
		int ny = y + dir.offsetY;
		int nz = z + dir.offsetZ;

		IBlockState nextState = world.getBlockState(new BlockPos(nx, ny, nz));
		Block next = nextState.getBlock();

		TileEntity te = world.getTileEntity(new BlockPos(nx, ny, nz));

		if(te instanceof TileEntityHadronDiode) {
			TileEntityHadronDiode diode = (TileEntityHadronDiode)te;

			if(diode.getConfig(p.dir.getOpposite().ordinal()) != DiodeConfig.IN) {
				p.expire(EnumHadronState.ERROR_DIODE_COLLISION);
			}

			p.isCheckExempt = true;

			return;
		}

		te = world.getTileEntity(new BlockPos(x, y, z));

		if(te instanceof TileEntityHadronDiode) {

			p.isCheckExempt = true;

			TileEntityHadronDiode diode = (TileEntityHadronDiode)te;

			if(diode.getConfig(dir.ordinal()) == DiodeConfig.OUT) {
				return;

			} else {

				List<ForgeDirection> dirs = getRandomDirs();

				for(ForgeDirection d : dirs) {

					if(d == dir || d == dir.getOpposite())
						continue;

					if(diode.getConfig(d.ordinal()) == DiodeConfig.OUT) {
						p.dir = d;
						return;
					}
				}
			}
		}

		if(nextState.getMaterial() == Material.AIR || next == ModBlocks.hadron_core)
			return;

		if(coilValue(next) > 0) {

			ForgeDirection validDir = ForgeDirection.UNKNOWN;

			List<ForgeDirection> dirs = getRandomDirs();

			for(ForgeDirection d : dirs) {

				if(d == dir || d == dir.getOpposite())
					continue;

				if(world.getBlockState(new BlockPos(x + d.offsetX, y + d.offsetY, z + d.offsetZ)).getMaterial() == Material.AIR) {

					if(validDir == ForgeDirection.UNKNOWN) {
						validDir = d;

					} else {
						p.expire(EnumHadronState.ERROR_BRANCHING_TURN);
						return;
					}
				}
			}

			p.dir = validDir;
			p.isCheckExempt = true;
			return;
		}

		p.expire(EnumHadronState.ERROR_OBSTRUCTED_CHANNEL);
	}

	private List<ForgeDirection> getRandomDirs() {

		List<Integer> rands = Arrays.asList(new Integer[] {0, 1, 2, 3, 4, 5} );
		Collections.shuffle(rands);
		List<ForgeDirection> dirs = new ArrayList<>();
		for(Integer i : rands) {
			dirs.add(ForgeDirection.getOrientation(i));
		}
		return dirs;
	}

	public int coilValue(Block b) {

		if(b instanceof BlockHadronCoil)
			return ((BlockHadronCoil)b).factor;

		return 0;
	}

	public boolean isPlating(Block b) {

		return b instanceof BlockHadronPlating ||
				b instanceof BlockHadronCoil ||
				b == ModBlocks.hadron_plating_glass ||
				b == ModBlocks.hadron_analysis_glass ||
				b == ModBlocks.hadron_access;
	}

	public boolean isAnalysis(Block b) {

		return b == ModBlocks.hadron_analysis ||
				b == ModBlocks.hadron_analysis_glass;
	}
	
	public static enum EnumHadronState {
		IDLE(0x8080ff),
		PROGRESS(0xffff00),
		ANALYSIS(0xffff00),
		NORESULT(0xff8000),
		NORESULT_TOO_SLOW(0xff8000),
		NORESULT_WRONG_INGREDIENT(0xff8000),
		NORESULT_WRONG_MODE(0xff8000),
		SUCCESS(0x00ff00),
		ERROR_NO_CHARGE(0xff0000, true),
		ERROR_NO_ANALYSIS(0xff0000, true),
		ERROR_OBSTRUCTED_CHANNEL(0xff0000, true),
		ERROR_EXPECTED_COIL(0xff0000, true),
		ERROR_MALFORMED_SEGMENT(0xff0000, true),
		ERROR_ANALYSIS_TOO_LONG(0xff0000, true),
		ERROR_ANALYSIS_TOO_SHORT(0xff0000, true),
		ERROR_DIODE_COLLISION(0xff0000, true),
		ERROR_BRANCHING_TURN(0xff0000, true),
		ERROR_GENERIC(0xff0000, true);

		public int color;
		public boolean showCoord;
		
		private EnumHadronState(int color) {
			this(color, false);
		}
		
		private EnumHadronState(int color, boolean showCoord) {
			this.color = color;
			this.showCoord = showCoord;
		}
	}

}