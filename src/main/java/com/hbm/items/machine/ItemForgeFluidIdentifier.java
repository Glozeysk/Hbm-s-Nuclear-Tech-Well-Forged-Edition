package com.hbm.items.machine;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Map.Entry;

import com.hbm.interfaces.IHasCustomModel;
import com.hbm.items.ModItems;
import com.hbm.lib.RefStrings;
import com.hbm.main.MainRegistry;
import com.hbm.blocks.ModBlocks;
import com.hbm.config.GeneralConfig;
import com.hbm.packet.PacketDispatcher;
import com.hbm.packet.PipeUpdatePacket;
import com.hbm.tileentity.conductor.TileEntityFFDuctBaseMk2;
import com.hbm.tileentity.conductor.TileEntityFFFluidDuctMk4;
import com.hbm.util.I18nUtil;
import com.hbm.forgefluid.FluidTypeHandler;

import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.NonNullList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fml.common.network.NetworkRegistry.TargetPoint;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public class ItemForgeFluidIdentifier extends Item implements IHasCustomModel {

	public static final ModelResourceLocation identifierModel = new ModelResourceLocation(RefStrings.MODID + ":forge_fluid_identifier", "inventory");

	public ItemForgeFluidIdentifier(String s) {
		this.setTranslationKey(s);
		this.setRegistryName(s);
		this.setCreativeTab(MainRegistry.partsTab);

		ModItems.ALL_ITEMS.add(this);
	}

	// @Override
	// public ItemStack getContainerItem(ItemStack itemStack) {
	// 	return itemStack.copy();
	// }

	// @Override
	// public boolean hasContainerItem() {
	// 	return true;
	// }

	@Override
	public void getSubItems(CreativeTabs tab, NonNullList<ItemStack> items) {
		if(GeneralConfig.registerTanks){
			if (tab == this.getCreativeTab() || tab == CreativeTabs.SEARCH) {
				for (Entry<String, Fluid> set : FluidRegistry.getRegisteredFluids().entrySet()) {
					if(FluidTypeHandler.noID(set.getValue())) continue;
					ItemStack stack = new ItemStack(this, 1, 0);
					NBTTagCompound tag = new NBTTagCompound();
					tag.setString("fluidtype", set.getKey());
					stack.setTagCompound(tag);
					items.add(stack);
				}
			}
		}
	}

	@Override
	public void addInformation(ItemStack stack, World worldIn, List<String> list, ITooltipFlag flagIn) {
		if (!(stack.getItem() instanceof ItemForgeFluidIdentifier))
			return;
		Fluid f = null;
		if (stack.hasTagCompound()) {
			f = FluidRegistry.getFluid(stack.getTagCompound().getString("fluidtype"));
		}
		list.add("");
		list.add(I18nUtil.resolveKey("desc.unfluidid"));
		if (f != null)
			list.add("   " + f.getLocalizedName(new FluidStack(f, 1000)));
		else
			list.add("   " + I18nUtil.resolveKey("fluid.none"));
	}

	@Override
	@SideOnly(Side.CLIENT)
	public String getItemStackDisplayName(ItemStack stack) {
		String s = ("" + I18n.format(this.getTranslationKey() + ".name")).trim();
		Fluid s1 = null;
		if (stack.hasTagCompound()) {
			s1 = FluidRegistry.getFluid(stack.getTagCompound().getString("fluidtype"));
		}

		if (s1 != null) {
			s = s + ": " + s1.getLocalizedName(new FluidStack(s1, 1000));
		}

		return s;
	}

	public static Fluid getType(ItemStack stack) {
		if (stack != null && stack.getItem() instanceof ItemForgeFluidIdentifier && stack.hasTagCompound())
			return FluidRegistry.getFluid(stack.getTagCompound().getString("fluidtype"));
		else
			return null;
	}
	
	public static ItemStack getStackFromFluid(Fluid f){
		ItemStack stack = new ItemStack(ModItems.forge_fluid_identifier, 1, 0);
		NBTTagCompound tag = new NBTTagCompound();
		tag.setString("fluidtype", f.getName());
		stack.setTagCompound(tag);
		return stack;
	}

	public static void spreadType(World worldIn, BlockPos origin, Fluid hand, Fluid pipe, int maxDepth){
		if(hand == pipe) return;
		TileEntity originTe = worldIn.getTileEntity(origin);
		if(!(originTe instanceof TileEntityFFDuctBaseMk2) || ((TileEntityFFDuctBaseMk2) originTe).getType() != pipe) return;

		// Iterative flood-fill: change every matching pipe's type silently (no per-pipe
		// notify/rebuild/packet), then do exactly one network rebuild and one sync pass for the
		// whole region. setType() per pipe used to fan out into ~8 packets each (own sync +
		// neighbor ripple via notifyNeighborsOfStateChange) and a full network rebuild each,
		// which stormed the outbound packet buffer on large networks (O(N) packets -> O(N^2) rebuilds).
		Set<BlockPos> visited = new HashSet<>();
		List<TileEntityFFDuctBaseMk2> changed = new ArrayList<>();
		Deque<BlockPos> posStack = new ArrayDeque<>();
		Deque<Integer> budgetStack = new ArrayDeque<>();
		posStack.push(origin);
		budgetStack.push(maxDepth);
		visited.add(origin);

		while(!posStack.isEmpty()){
			BlockPos cur = posStack.pop();
			int budget = budgetStack.pop();
			if(budget <= 0) continue;

			TileEntity te = worldIn.getTileEntity(cur);
			if(!(te instanceof TileEntityFFDuctBaseMk2)) continue;
			TileEntityFFDuctBaseMk2 duct = (TileEntityFFDuctBaseMk2) te;
			if(duct.getType() != pipe) continue;

			duct.setTypeSilent(hand);
			changed.add(duct);

			for(EnumFacing e : EnumFacing.VALUES){
				BlockPos n = cur.offset(e);
				if(visited.add(n)){
					posStack.push(n);
					budgetStack.push(budget - 1);
				}
			}
		}

		if(changed.isEmpty()) return;

		// buildNetwork flood-fills the whole connected component from one starting pipe, so a
		// single rebuild from the origin reconstructs the network for the entire changed region.
		TileEntityFFDuctBaseMk2.rebuildNetworks(worldIn, origin);
		//setTypeSilent skips the magnetic pipes' setType override, so their energy nets are rebuilt here in one batch
		TileEntityFFFluidDuctMk4.rebuildEnergyNetworks(worldIn, changed);

		for(TileEntityFFDuctBaseMk2 duct : changed){
			duct.refreshLocalState();
			duct.syncToWatchers();
		}

		if(!worldIn.isRemote)
			PacketDispatcher.wrapper.sendToAllTracking(new PipeUpdatePacket(origin, 1), new TargetPoint(worldIn.provider.getDimension(), origin.getX(), origin.getY(), origin.getZ(), 10));
	}

	@Override
	public EnumActionResult onItemUse(EntityPlayer player, World worldIn, BlockPos pos, EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ) {
		TileEntity te = worldIn.getTileEntity(pos);
		TileEntityFFDuctBaseMk2 duct = null;
		if(te != null && te instanceof TileEntityFFDuctBaseMk2){
			duct = (TileEntityFFDuctBaseMk2) te;
		}
		if(duct != null){
			if(getType(player.getHeldItem(hand)) != duct.getType()){
				spreadType(worldIn, pos, getType(player.getHeldItem(hand)), duct.getType(), 256);
			}
		}
		return super.onItemUse(player, worldIn, pos, hand, facing, hitX, hitY, hitZ);
	}

	@Override
	public ActionResult<ItemStack> onItemRightClick(World world, EntityPlayer player, EnumHand hand) {
		ItemStack stack = player.getHeldItem(hand);

		RayTraceResult ray = this.rayTrace(world, player, false);
		if (ray != null && ray.typeOfHit == RayTraceResult.Type.BLOCK) {
			BlockPos pos = ray.getBlockPos();
			TileEntity te = world.getTileEntity(pos);

			if (te instanceof TileEntityFFDuctBaseMk2) { 
				return new ActionResult<>(EnumActionResult.FAIL, stack);
			}
		}

		if (world.isRemote) {
			player.openGui(MainRegistry.instance, ModItems.guiID_fluid_id, world, 0, 0, 0);
		}
		return new ActionResult<>(EnumActionResult.SUCCESS, stack);
	}

	@Override
	public ModelResourceLocation getResourceLocation() {
		return identifierModel;
	}
}
