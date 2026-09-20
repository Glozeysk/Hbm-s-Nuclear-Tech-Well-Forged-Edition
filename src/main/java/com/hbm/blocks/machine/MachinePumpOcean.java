package com.hbm.blocks.machine;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import com.hbm.blocks.BlockDummyable;
import com.hbm.blocks.ILookOverlay;
import com.hbm.blocks.ITooltipProvider;
import com.hbm.lib.ForgeDirection;
import com.hbm.tileentity.TileEntityProxyCombo;
import com.hbm.tileentity.machine.TileEntityMachinePumpOcean;
import com.hbm.util.I18nUtil;

import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumBlockRenderType;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.client.event.RenderGameOverlayEvent.Pre;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

//Ported from NTM:CE (MachinePump), reworked into the ocean brine pump; the steam variant is not ported.
public class MachinePumpOcean extends BlockDummyable implements ILookOverlay, ITooltipProvider {

	public MachinePumpOcean(Material mat, String s) {
		super(mat, s);
	}

	@Override
	public TileEntity createNewTileEntity(World world, int meta) {

		if(meta >= 12)
			return new TileEntityMachinePumpOcean();

		if(hasExtra(meta))
			return new TileEntityProxyCombo(false, true, true);

		return null;
	}

	@Override
	public int[] getDimensions() {
		return new int[] {3, 0, 1, 1, 1, 1};
	}

	@Override
	public int getOffset() {
		return 1;
	}

	@Override
	public void fillSpace(World world, int x, int y, int z, ForgeDirection dir, int o) {
		super.fillSpace(world, x, y, z, dir, o);

		//one port on each side of the core; must match TileEntityMachinePumpOcean.getConPos
		this.makeExtra(world, x + dir.offsetX * o + 1, y, z + dir.offsetZ * o);
		this.makeExtra(world, x + dir.offsetX * o - 1, y, z + dir.offsetZ * o);
		this.makeExtra(world, x + dir.offsetX * o, y, z + dir.offsetZ * o + 1);
		this.makeExtra(world, x + dir.offsetX * o, y, z + dir.offsetZ * o - 1);
	}

	@Override
	public EnumBlockRenderType getRenderType(IBlockState state) {
		return EnumBlockRenderType.ENTITYBLOCK_ANIMATED;
	}

	@Override
	@SideOnly(Side.CLIENT)
	public void addInformation(ItemStack stack, World player, List<String> tooltip, ITooltipFlag advanced) {
		this.addStandardInfo(tooltip);
	}

	@Override
	@SideOnly(Side.CLIENT)
	public void printHook(Pre event, World world, int x, int y, int z) {

		int[] corePos = this.findCore(world, x, y, z);

		if(corePos == null)
			return;

		TileEntity te = world.getTileEntity(new BlockPos(corePos[0], corePos[1], corePos[2]));

		if(!(te instanceof TileEntityMachinePumpOcean pump))
			return;

		List<String> text = new ArrayList();
		text.add("§a-> §r" + String.format(Locale.US, "%,d", pump.power) + " / " + String.format(Locale.US, "%,d", TileEntityMachinePumpOcean.maxPower) + "HE");
		text.add("§c<- §r" + (pump.tank.getFluid() != null ? pump.tank.getFluid().getLocalizedName() : "") + ": " + String.format(Locale.US, "%,d", pump.tank.getFluidAmount()) + " / " + String.format(Locale.US, "%,d", pump.tank.getCapacity()) + "mB");

		int warnColor = System.currentTimeMillis() % 1000 < 500 ? 0xff0000 : 0xffff00;

		if(!pump.validBiome)
			text.add("&[" + warnColor + "&]! ! ! NO_VALID_BIOME ! ! !");
		if(!pump.validHeight)
			text.add("&[" + warnColor + "&]! ! ! NO_VALID_HEIGHT ! ! !");
		if(!pump.validWater)
			text.add("&[" + warnColor + "&]! ! ! NO_VALID_WATER_COUNT ! ! !");

		ILookOverlay.printGeneric(event, I18nUtil.resolveKey(getTranslationKey() + ".name"), 0xffff00, 0x404000, text);
	}
}
