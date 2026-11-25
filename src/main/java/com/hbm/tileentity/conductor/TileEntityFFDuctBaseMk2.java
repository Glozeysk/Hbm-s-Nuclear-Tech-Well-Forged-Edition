package com.hbm.tileentity.conductor;

import java.util.ArrayList;
import java.util.List;

import com.hbm.forgefluid.FFPipeNetworkMk2;
import com.hbm.forgefluid.FFUtils;
import com.hbm.interfaces.IFluidPipeMk2;
import com.hbm.packet.PacketDispatcher;
import com.hbm.packet.PipeUpdatePacket;

import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.SPacketUpdateTileEntity;
import net.minecraft.server.management.PlayerChunkMapEntry;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ITickable;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.CapabilityFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.IFluidTankProperties;
import net.minecraftforge.fml.common.network.NetworkRegistry.TargetPoint;

public class TileEntityFFDuctBaseMk2 extends TileEntity implements IFluidPipeMk2, IFluidHandler, ITickable {

	public EnumFacing[] connections = new EnumFacing[6];
	protected Fluid type;
	protected FFPipeNetworkMk2 network = null;
	public TileEntity[] tileentityCache = new TileEntity[6];
	public boolean isBeingDestroyed = false;
	private long lastFillWorldTime = -1;

	protected int throughput = -1;
	protected boolean extractionMode = true;
	private boolean needsInitialization = true;

	private long lastTickTransferTime = -1;
	private int transferredThisTick = 0;

	public TileEntityFFDuctBaseMk2() {
	}

	public int getPipeTier() { return 2; }
	public int getThroughput() { return throughput; }

	public void setThroughput(int value) {
		if (value == -1) {
			this.throughput = -1;
		} else {
			this.throughput = Math.max(1, value);
		}
		markDirty();
	}

	protected boolean canConnectTo(BlockPos neighborPos, EnumFacing facing) {
		if (!FFUtils.checkFluidConnectablesMk2(this.world, neighborPos, getType(), facing)) return false;
		TileEntity te = world.getTileEntity(neighborPos);
		if (te instanceof TileEntityFFDuctBaseMk2) {
			return ((TileEntityFFDuctBaseMk2) te).getPipeTier() == this.getPipeTier();
		}
		return true;
	}

	public void setType(Fluid f) {
		if (f != type) {
			type = f;
			world.notifyNeighborsOfStateChange(pos, getBlockType(), true);
			world.neighborChanged(pos, getBlockType(), pos);
			IBlockState state = world.getBlockState(pos);
			world.markAndNotifyBlock(pos, world.getChunk(pos), state, state, 2);
			rebuildNetworks(world, pos);
			if (world instanceof WorldServer) {
				PlayerChunkMapEntry entry = ((WorldServer) world).getPlayerChunkMap().getEntry(MathHelper.floor(pos.getX()) >> 4, MathHelper.floor(pos.getZ()) >> 4);
				if (entry != null) {
					for (EntityPlayerMP player : entry.getWatchingPlayers()) {
						player.connection.sendPacket(new SPacketUpdateTileEntity(pos, 0, writeToNBT(new NBTTagCompound())));
					}
				}
			}
			if (!world.isRemote)
				PacketDispatcher.wrapper.sendToAllTracking(new PipeUpdatePacket(pos, 1), new TargetPoint(world.provider.getDimension(), pos.getX(), pos.getY(), pos.getZ(), 10));
		}
	}

	public Fluid getType() { return type; }

	@Override
	public NBTTagCompound writeToNBT(NBTTagCompound compound) {
		super.writeToNBT(compound);
		if(type != null) compound.setString("fluidType", type.getName());
		compound.setInteger("throughput", throughput);
		compound.setBoolean("extractionMode", extractionMode);
		return compound;
	}

	@Override
	public void readFromNBT(NBTTagCompound compound) {
		super.readFromNBT(compound);

		if(compound.hasKey("fluidType")) {
			this.type = FluidRegistry.getFluid(compound.getString("fluidType"));
		}

		this.throughput = compound.hasKey("throughput") ? compound.getInteger("throughput") : -1;
		this.extractionMode = compound.hasKey("extractionMode") ? compound.getBoolean("extractionMode") : true;
	}

	@Override
	public SPacketUpdateTileEntity getUpdatePacket(){
		return new SPacketUpdateTileEntity(this.getPos(), 0, this.writeToNBT(new NBTTagCompound()));
	}

	@Override
	public NBTTagCompound getUpdateTag() { return this.writeToNBT(new NBTTagCompound()); }

	@Override
	public void onDataPacket(NetworkManager net, SPacketUpdateTileEntity pkt) {
		this.readFromNBT(pkt.getNbtCompound());
	}

	@Override
	public void handleUpdateTag(NBTTagCompound tag) {
		Fluid f = this.type;
		this.readFromNBT(tag);
		if(f != type) {
			for(EnumFacing e : EnumFacing.VALUES) {
				TileEntity te = world.getTileEntity(pos.offset(e));
				if(te instanceof TileEntityFFDuctBaseMk2)
					((TileEntityFFDuctBaseMk2) te).onNeighborChange();
			}
			this.onNeighborChange();
		}
	}

	@Override
	public void onLoad() {
		if (!world.isRemote) {
			joinOrMakeNetwork();
			onNeighborChange();
		}
		needsInitialization = false;
	}

	public void onNeighborChange() {
		rebuildCache();
		updateConnections();
		if(!world.isRemote)
			PacketDispatcher.wrapper.sendToAllTracking(new PipeUpdatePacket(pos), new TargetPoint(world.provider.getDimension(), pos.getX(), pos.getY(), pos.getZ(), 10));
	}

	@Override
	public void onChunkUnload() {
		if(network == null) return;
		for(TileEntity te : tileentityCache) {
			if(te != null) {
				if(te instanceof IFluidPipeMk2) continue;
				if(!world.isBlockLoaded(te.getPos())) { network.checkForRemoval(te); continue; }
				boolean flag = true;
				for(EnumFacing e : EnumFacing.VALUES) {
					BlockPos p = te.getPos().offset(e);
					if(world.isBlockLoaded(p)) {
						TileEntity ent = world.getTileEntity(p);
						if(ent instanceof IFluidPipeMk2 && ((IFluidPipeMk2) ent).getNetwork() == network) { flag = false; break; }
					}
				}
				if(flag) network.checkForRemoval(te);
			}
		}
		network.checkForRemoval(this);
		for(int i = 0; i < tileentityCache.length; i++) tileentityCache[i] = null;
		this.network = null;
	}

	@Override public void invalidate() { super.invalidate(); }

	public static void breakBlock(World world, BlockPos pos) {
		TileEntity te = world.getTileEntity(pos);
		if(te instanceof TileEntityFFDuctBaseMk2) ((TileEntityFFDuctBaseMk2) te).isBeingDestroyed = true;
		rebuildNetworks(world, pos);
	}

	public static void rebuildNetworks(World world, BlockPos pos) {
		TileEntity center = world.getTileEntity(pos);
		for(EnumFacing e : EnumFacing.VALUES) {
			TileEntity te = world.getTileEntity(pos.offset(e));
			if(te instanceof IFluidPipeMk2) {
				IFluidPipeMk2 pipe = (IFluidPipeMk2) te;
				if(pipe.getNetwork() != null) pipe.getNetwork().destroy();
			}
		}
		if(center instanceof IFluidPipeMk2 && ((IFluidPipeMk2) center).getNetwork() != null)
			((IFluidPipeMk2) center).getNetwork().destroy();
		for(EnumFacing e : EnumFacing.VALUES) FFPipeNetworkMk2.buildNetwork(world.getTileEntity(pos.offset(e)));
		FFPipeNetworkMk2.buildNetwork(center);
	}

	@Override
	public void joinOrMakeNetwork() {
		List<FFPipeNetworkMk2> otherNetworks = new ArrayList<>();
		for(EnumFacing e : EnumFacing.VALUES) {
			BlockPos offset = pos.offset(e);
			TileEntity te = world.getTileEntity(offset);
			if(te instanceof IFluidPipeMk2) {
				if(te instanceof TileEntityFFDuctBaseMk2 && ((TileEntityFFDuctBaseMk2) te).getPipeTier() != this.getPipeTier()) continue;
				IFluidPipeMk2 pipe = (IFluidPipeMk2) te;
				if(pipe.getNetwork() != null && pipe.getNetwork().getType() == this.getType() && !otherNetworks.contains(pipe.getNetwork()))
					otherNetworks.add(pipe.getNetwork());
			}
		}
		if(otherNetworks.isEmpty()) { network = new FFPipeNetworkMk2(this); network.tryAdd(this); return; }
		else {
			FFPipeNetworkMk2 net = otherNetworks.remove(0);
			while(otherNetworks.size() > 0) net = FFPipeNetworkMk2.mergeNetworks(net, otherNetworks.remove(0));
			network = net; net.tryAdd(this);
		}
	}

	protected boolean rebuildCache() {
		boolean changed = false;
		for(EnumFacing e : EnumFacing.VALUES) {
			TileEntity te = world.getTileEntity(pos.offset(e));
			if(tileentityCache[e.getIndex()] == null) {
				if(te != null) { if(network != null) network.tryAdd(te); tileentityCache[e.getIndex()] = te; changed = true; }
			} else {
				if(te == null) { if(network != null) network.checkForRemoval(tileentityCache[e.getIndex()]); tileentityCache[e.getIndex()] = null; changed = true; }
				else if(te != tileentityCache[e.getIndex()]) {
					if(network != null) { network.checkForRemoval(tileentityCache[e.getIndex()]); network.tryAdd(te); }
					tileentityCache[e.getIndex()] = te; changed = true;
				}
			}
		}
		return changed;
	}

	public void updateConnections() {
		connections[0] = canConnectTo(pos.up(), EnumFacing.DOWN) ? EnumFacing.UP : null;
		connections[1] = canConnectTo(pos.down(), EnumFacing.UP) ? EnumFacing.DOWN : null;
		connections[2] = canConnectTo(pos.north(), EnumFacing.SOUTH) ? EnumFacing.NORTH : null;
		connections[3] = canConnectTo(pos.east(), EnumFacing.WEST) ? EnumFacing.EAST : null;
		connections[4] = canConnectTo(pos.south(), EnumFacing.NORTH) ? EnumFacing.SOUTH : null;
		connections[5] = canConnectTo(pos.west(), EnumFacing.EAST) ? EnumFacing.WEST : null;
	}

	@Override public FFPipeNetworkMk2 getNetwork() { return network; }
	@Override public void setNetwork(FFPipeNetworkMk2 net) { network = net; }
	@Override public boolean isValidForBuilding() { return !isBeingDestroyed; }
	@Override public IFluidTankProperties[] getTankProperties() { return network != null ? network.getTankProperties() : new IFluidTankProperties[] {}; }

	protected boolean checkFluidCorrosion(FluidStack resource) {
		return false;
	}

	@Override
	public int fill(FluidStack resource, boolean doFill) {
		if (resource == null || resource.amount <= 0) return 0;
		if (this.type == null) return 0;
		if (this.type != resource.getFluid()) return 0;

		if (!world.isRemote && checkFluidCorrosion(resource)) {
			if (doFill) {
				destroyPipe();
			}
			return 0;
		}

		int remaining = getRemainingThroughput();
		if (remaining <= 0) return 0;

		FluidStack toFill = resource.copy();
		if (toFill.amount > remaining) {
			toFill.amount = remaining;
		}

		int filled = network != null ? network.fill(toFill, doFill, null) : 0;

		if (filled > 0 && doFill) {
			if (throughput != -1) {
				transferredThisTick += filled;
			}
		}
		return filled;
	}

	protected void destroyPipe() {
	}

	@Override
	public void update() {
		if(world.isRemote || network == null || !extractionMode) return;

		if (network.getType() == null) return;

		int remaining = getRemainingThroughput();
		if (remaining <= 0) return;

		int maxNetFill = network.fill(new FluidStack(network.getType(), Integer.MAX_VALUE), false);
		if (maxNetFill <= 0) return;

		for(EnumFacing e : EnumFacing.VALUES) {
			TileEntity te = world.getTileEntity(pos.offset(e));
			if(te == null || te instanceof IFluidPipeMk2) continue;
			if(!te.hasCapability(CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY, e.getOpposite())) continue;

			IFluidHandler neighbor = te.getCapability(CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY, e.getOpposite());

			int canExtract = (throughput == -1) ? maxNetFill : Math.min(remaining, maxNetFill);
			if (canExtract <= 0) break;

			FluidStack simulatedDrain = neighbor.drain(new FluidStack(network.getType(), canExtract), false);
			if (simulatedDrain == null || simulatedDrain.amount <= 0) continue;

			if (!world.isRemote && checkFluidCorrosion(simulatedDrain)) {
				destroyPipe();
				return;
			}

			int accepted = network.fill(simulatedDrain, false, te.getPos());
			if (accepted <= 0) continue;

			FluidStack drained = neighbor.drain(new FluidStack(network.getType(), accepted), true);
			if (drained != null && drained.amount > 0) {
				int actuallyAccepted = network.fill(drained, true, te.getPos());

				if (actuallyAccepted > 0) {
					if (throughput != -1) {
						transferredThisTick += actuallyAccepted;
					}
					remaining -= actuallyAccepted;
					maxNetFill -= actuallyAccepted;
				}
			}
		}
	}

	@Override public FluidStack drain(FluidStack resource, boolean doDrain) { return network != null ? network.drain(resource, doDrain) : null; }
	@Override public FluidStack drain(int maxDrain, boolean doDrain) { return network != null ? network.drain(maxDrain, doDrain) : null; }

	public boolean isTransportingFluid() { return lastFillWorldTime >= 0 && (world.getTotalWorldTime() - lastFillWorldTime) < 20; }

	@Override
	public boolean hasCapability(Capability<?> capability, EnumFacing facing) {
		return capability == CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY || super.hasCapability(capability, facing);
	}

	@Override
	public <T> T getCapability(Capability<T> capability, EnumFacing facing) {
		return capability == CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY ? CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY.cast(this) : super.getCapability(capability, facing);
	}

	private int getRemainingThroughput() {
		if (throughput == -1) {
			return Integer.MAX_VALUE;
		}
		if (world == null) return throughput;
		long currentTime = world.getTotalWorldTime();
		if (currentTime != lastTickTransferTime) {
			lastTickTransferTime = currentTime;
			transferredThisTick = 0;
		}
		return Math.max(0, throughput - transferredThisTick);
	}

	public boolean hasExternalConnections() {
		if (this.world == null) return false;
		for (EnumFacing e : EnumFacing.VALUES) {
			TileEntity te = world.getTileEntity(pos.offset(e));
			if (te != null && !(te instanceof IFluidPipeMk2)) {
				if (te.hasCapability(CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY, e.getOpposite())) {
					return true;
				}
			}
		}
		return false;
	}
}