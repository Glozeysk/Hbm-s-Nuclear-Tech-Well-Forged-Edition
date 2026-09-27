package com.hbm.blocks.machine;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import com.hbm.blocks.ILookOverlay;
import com.hbm.blocks.ModBlocks;
import com.hbm.forgefluid.ModForgeFluids;
import com.hbm.tileentity.machine.TileEntityMachineCryoPump;
import com.hbm.tileentity.machine.TileEntityMachineVacuumPump;
import com.hbm.util.I18nUtil;

import net.minecraft.block.BlockContainer;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumBlockRenderType;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.client.event.RenderGameOverlayEvent.Pre;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.fluids.FluidStack;

/** Both vacuum pumps for the arc welder; they only differ in the tile they carry. */
public class MachineVacuumPump extends BlockContainer implements ILookOverlay {

	private final boolean cryo;

	public MachineVacuumPump(Material mat, String s, boolean cryo) {
		super(mat);
		this.cryo = cryo;
		this.setTranslationKey(s);
		this.setRegistryName(s);

		ModBlocks.ALL_BLOCKS.add(this);
	}

	@Override
	public TileEntity createNewTileEntity(World world, int meta) {
		return cryo ? new TileEntityMachineCryoPump() : new TileEntityMachineVacuumPump();
	}

	@Override
	public EnumBlockRenderType getRenderType(IBlockState state) {
		return EnumBlockRenderType.MODEL;
	}

	@Override
	@SideOnly(Side.CLIENT)
	public void printHook(Pre event, World world, int x, int y, int z) {

		TileEntity te = world.getTileEntity(new BlockPos(x, y, z));

		if(!(te instanceof TileEntityMachineVacuumPump pump))
			return;

		List<String> text = new ArrayList<>();
		text.add(pump.isOn ? "§aRunning" : "§cIdle");
		text.add(String.format(Locale.US, "%,d", pump.power) + " / " + String.format(Locale.US, "%,d", pump.getMaxPower()) + " HE");

		if(te instanceof TileEntityMachineCryoPump cryoPump)
			text.add(ModForgeFluids.nitrogen.getLocalizedName(new FluidStack(ModForgeFluids.nitrogen, 1)) + ": "
					+ String.format(Locale.US, "%,d", cryoPump.tank.getFluidAmount()) + " / " + String.format(Locale.US, "%,d", cryoPump.tank.getCapacity()) + " mB");

		ILookOverlay.printGeneric(event, I18nUtil.resolveKey(getTranslationKey() + ".name"), 0xffff00, 0x404000, text);
	}
}
