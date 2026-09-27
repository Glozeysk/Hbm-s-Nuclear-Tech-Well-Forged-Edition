package com.hbm.blocks.machine;

import java.util.ArrayList;
import java.util.List;

import com.hbm.blocks.BlockDummyable;
import com.hbm.blocks.ILookOverlay;
import com.hbm.forgefluid.ModForgeFluids;
import com.hbm.tileentity.TileEntityProxyCombo;
import com.hbm.tileentity.machine.TileEntityMachineIntake;
import com.hbm.util.BobMathUtil;
import com.hbm.util.I18nUtil;

import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumBlockRenderType;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.client.event.RenderGameOverlayEvent.Pre;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

//Ported from NTM:CE (MachineIntake)
public class MachineIntake extends BlockDummyable implements ILookOverlay {

	public MachineIntake(Material mat, String s) {
		super(mat, s);
	}

	@Override
	public TileEntity createNewTileEntity(World world, int meta) {
		if(meta >= 12)
			return new TileEntityMachineIntake();
		//every other part of the 2x2 is a port
		return new TileEntityProxyCombo(false, true, true);
	}

	@Override
	public int[] getDimensions() {
		return new int[] {0, 0, 1, 0, 1, 0};
	}

	@Override
	public int getOffset() {
		return 0;
	}

	@Override
	public EnumBlockRenderType getRenderType(IBlockState state) {
		return EnumBlockRenderType.ENTITYBLOCK_ANIMATED;
	}

	@Override
	@SideOnly(Side.CLIENT)
	public void printHook(Pre event, World world, int x, int y, int z) {

		int[] corePos = this.findCore(world, x, y, z);

		if(corePos == null)
			return;

		TileEntity te = world.getTileEntity(new BlockPos(corePos[0], corePos[1], corePos[2]));

		if(!(te instanceof TileEntityMachineIntake intake))
			return;

		List<String> text = new ArrayList<>();
		text.add((intake.power < TileEntityMachineIntake.consumption ? "§c" : "§a") + BobMathUtil.getShortNumber(intake.power) + "HE");
		text.add("§c<- §r" + ModForgeFluids.air.getLocalizedName(new FluidStack(ModForgeFluids.air, 1)) + ": " + intake.compair.getFluidAmount() + "/" + intake.compair.getCapacity() + "mB");

		ILookOverlay.printGeneric(event, I18nUtil.resolveKey(getTranslationKey() + ".name"), 0xffff00, 0x404000, text);
	}
}
