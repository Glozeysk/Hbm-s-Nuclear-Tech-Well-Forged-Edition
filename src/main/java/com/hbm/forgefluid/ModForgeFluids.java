package com.hbm.forgefluid;

import java.awt.Color;
import java.util.HashMap;

import com.hbm.blocks.ModBlocks;
import com.hbm.blocks.fluid.CoriumBlock;
import com.hbm.blocks.fluid.CoriumFluid;
import com.hbm.blocks.fluid.MudBlock;
import com.hbm.blocks.fluid.MudFluid;
import com.hbm.blocks.fluid.SchrabidicBlock;
import com.hbm.blocks.fluid.SchrabidicFluid;
import com.hbm.blocks.fluid.ToxicBlock;
import com.hbm.blocks.fluid.ToxicFluid;
import com.hbm.blocks.fluid.RadWaterBlock;
import com.hbm.blocks.fluid.RadWaterFluid;
import com.hbm.blocks.fluid.VolcanicBlock;
import com.hbm.blocks.fluid.VolcanicFluid;
import com.hbm.forgefluid.FluidTypeHandler.FluidTrait;
import com.hbm.inventory.EngineRecipes.FuelGrade;
import com.hbm.lib.ModDamageSource;
import com.hbm.lib.RefStrings;
import com.hbm.render.misc.EnumSymbol;

import net.minecraft.util.ResourceLocation;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

@Mod.EventBusSubscriber(modid = RefStrings.MODID)
public class ModForgeFluids {

	public static HashMap<Fluid, Integer> fluidColors = new HashMap<Fluid, Integer>();

	public static Fluid spentsteam = HbmFluid.builder("spentsteam")
			.temperature(40)
			.props(0, 0, 0, EnumSymbol.NONE)
			.build();
	public static Fluid steam = HbmFluid.builder("steam")
			.temperature(100)
			.props(0, 0, 1, EnumSymbol.NONE)
			.build();
	public static Fluid hotsteam = HbmFluid.builder("hotsteam")
			.temperature(300)
			.props(0, 0, 2, EnumSymbol.NONE)
			.build();
	public static Fluid superhotsteam = HbmFluid.builder("superhotsteam")
			.temperature(450)
			.props(0, 0, 3, EnumSymbol.NONE)
			.build();
	public static Fluid ultrahotsteam = HbmFluid.builder("ultrahotsteam")
			.temperature(600)
			.props(0, 0, 4, EnumSymbol.NONE)
			.build();
	public static Fluid coolant = HbmFluid.builder("coolant")
			.temperatureKelvin(173)
			.props(1, 0, 0, EnumSymbol.CROYGENIC)
			.build();
	public static Fluid hotcoolant = HbmFluid.builder("hotcoolant")
			.temperature(200)
			.props(4, 1, 1, EnumSymbol.NONE)
			.build();

	public static Fluid brine = HbmFluid.builder("brine")
			.props(1, 0, 0, EnumSymbol.NONE)
			.build();
	public static Fluid sodiumhydroxide = HbmFluid.builder("sodiumhydroxide")
			.props(3, 0, 1, EnumSymbol.NONE)
			.build();
	public static Fluid sodiumbase = HbmFluid.builder("sodiumbase")
			.temperature(400)
			.props(3, 3, 2, EnumSymbol.NOWATER)
			.build();
	public static Fluid sodiumhot = HbmFluid.builder("sodiumhot")
			.temperature(1200)
			.props(4, 4, 3, EnumSymbol.NOWATER)
			.build();
	public static Fluid electrosolvent = HbmFluid.builder("electrosolvent")
			.props(4, 3, 2, EnumSymbol.ACID).dfc(1.75F).trait(FluidTrait.CORROSIVE_2)
			.build();
	public static Fluid heavywater = HbmFluid.builder("heavywater")
			.props(1, 0, 0, EnumSymbol.NONE)
			.build();
	public static Fluid deuterium = HbmFluid.builder("deuterium")
			.props(2, 4, 0, EnumSymbol.CROYGENIC).dfc(1.2F)
			.trait(FluidTrait.COMBUSTION_TU, 5)
			.fuel(FuelGrade.HIGH, 10_000)
			.cell("cell_deuterium").gasCanister("gas_deuterium")
			.build();
	public static Fluid tritium = HbmFluid.builder("tritium")
			.props(3, 4, 0, EnumSymbol.RADIATION).dfc(1.3F)
			.trait(FluidTrait.COMBUSTION_TU, 5)
			.fuel(FuelGrade.HIGH, 10_000)
			.cell("cell_tritium").gasCanister("gas_tritium")
			.build();

	public static Fluid oil = new Fluid("oil", new ResourceLocation(RefStrings.MODID, "blocks/forgefluid/oil_still"), new ResourceLocation(RefStrings.MODID, "blocks/forgefluid/oil_flowing"), null, Color.WHITE);
	public static Fluid hotoil = HbmFluid.builder("hotoil")
			.temperature(350)
			.props(2, 3, 0, EnumSymbol.NONE)
			.build();
	public static Fluid crackoil = HbmFluid.builder("crackoil")
			.props(2, 1, 0, EnumSymbol.NONE)
			.trait(FluidTrait.COMBUSTION_TU, 31.2)
			.build();
	public static Fluid crackoil_pure = HbmFluid.builder("crackoil_pure")
			.props(2, 1, 0, EnumSymbol.NONE)
			.trait(FluidTrait.COMBUSTION_TU, 62.5)
			.build();
	public static Fluid hotcrackoil = HbmFluid.builder("hotcrackoil")
			.temperature(350)
			.props(2, 3, 0, EnumSymbol.NONE)
			.build();
	public static Fluid hotcrackoil_pure = HbmFluid.builder("hotcrackoil_pure")
			.temperature(350)
			.props(2, 3, 0, EnumSymbol.NONE)
			.build();
	public static Fluid heavyoil = HbmFluid.builder("heavyoil")
			.props(2, 1, 0, EnumSymbol.NONE)
			.trait(FluidTrait.COMBUSTION_TU, 55)
			.fuel(FuelGrade.LOW, 68_700)
			.canister("canister_heavyoil")
			.build();
	public static Fluid bitumen = HbmFluid.builder("bitumen")
			.props(2, 0, 0, EnumSymbol.NONE)
			.canister("canister_bitumen")
			.build();
	public static Fluid smear = HbmFluid.builder("smear")
			.props(2, 1, 0, EnumSymbol.NONE)
			.trait(FluidTrait.COMBUSTION_TU, 82.5)
			.canister("canister_smear")
			.build();
	public static Fluid heatingoil = HbmFluid.builder("heatingoil")
			.props(2, 2, 0, EnumSymbol.NONE)
			.trait(FluidTrait.COMBUSTION_TU, 391)
			.fuel(FuelGrade.LOW, 489_000)
			.canister("canister_heatingoil")
			.build();

	public static Fluid reclaimed = HbmFluid.builder("reclaimed")
			.props(2, 2, 0, EnumSymbol.NONE)
			.trait(FluidTrait.COMBUSTION_TU, 113)
			.fuel(FuelGrade.LOW, 141_000)
			.canister("canister_reoil")
			.build();
	public static Fluid petroil = HbmFluid.builder("petroil")
			.props(1, 3, 0, EnumSymbol.NONE)
			.trait(FluidTrait.COMBUSTION_TU, 130)
			.fuel(FuelGrade.MEDIUM, 195_000)
			.canister("canister_petroil")
			.build();
	public static Fluid petroil_hq = HbmFluid.builder("petroil_hq")
			.props(1, 2, 0, EnumSymbol.NONE)
			.trait(FluidTrait.COMBUSTION_TU, 605)
			.fuel(FuelGrade.HIGH, 1_510_000)
			.build();

	public static Fluid fracksol = HbmFluid.builder("fracksol")
			.props(1, 3, 3, EnumSymbol.ACID).trait(FluidTrait.CORROSIVE)
			.canister("canister_fracksol")
			.build();
	//Drillgon200: Bruh I spelled this wrong, too.
	public static Fluid lubricant = HbmFluid.builder("lubricant")
			.props(2, 1, 0, EnumSymbol.NONE)
			.canister("canister_canola")
			.build();

	public static Fluid naphtha = HbmFluid.builder("naphtha")
			.props(2, 1, 0, EnumSymbol.NONE)
			.trait(FluidTrait.COMBUSTION_TU, 110)
			.fuel(FuelGrade.MEDIUM, 165_000)
			.canister("canister_naphtha")
			.build();
	public static Fluid naphtha_pure = HbmFluid.builder("naphtha_pure")
			.props(2, 1, 0, EnumSymbol.NONE)
			.trait(FluidTrait.COMBUSTION_TU, 220)
			.fuel(FuelGrade.MEDIUM, 330_000)
			.build();
	public static Fluid reformate = HbmFluid.builder("reformate")
			.props(2, 2, 0, EnumSymbol.NONE)
			.trait(FluidTrait.COMBUSTION_TU, 2_400)
			.fuel(FuelGrade.HIGH, 6_000_000)
			.build();
	public static Fluid diesel = HbmFluid.builder("diesel")
			.props(1, 2, 0, EnumSymbol.NONE)
			.trait(FluidTrait.COMBUSTION_TU, 550)
			.fuel(FuelGrade.HIGH, 1_370_000)
			.canister("canister_fuel")
			.build();
	public static Fluid diesel_hq = HbmFluid.builder("diesel_hq")
			.props(1, 2, 0, EnumSymbol.NONE)
			.trait(FluidTrait.COMBUSTION_TU, 1_370)
			.fuel(FuelGrade.HIGH, 3_430_000)
			.build();

	public static Fluid lightoil = HbmFluid.builder("lightoil")
			.props(1, 2, 0, EnumSymbol.NONE)
			.trait(FluidTrait.COMBUSTION_TU, 1_460)
			.fuel(FuelGrade.MEDIUM, 2_200_000)
			.canister("canister_lightoil")
			.build();
	public static Fluid lightoil_pure = HbmFluid.builder("lightoil_pure")
			.props(1, 2, 0, EnumSymbol.NONE)
			.trait(FluidTrait.COMBUSTION_TU, 2_930)
			.fuel(FuelGrade.MEDIUM, 4_400_000)
			.build();
	public static Fluid kerosene = HbmFluid.builder("kerosene")
			.props(1, 2, 0, EnumSymbol.NONE)
			.trait(FluidTrait.COMBUSTION_TU, 2_560)
			.fuel(FuelGrade.AERO, 3_850_000)
			.canister("canister_kerosene")
			.build();
	public static Fluid kerosene_hq = HbmFluid.builder("kerosene_hq")
			.props(1, 2, 0, EnumSymbol.NONE)
			.trait(FluidTrait.COMBUSTION_TU, 6_400)
			.fuel(FuelGrade.AERO, 9_600_000)
			.build();

	public static Fluid gas = HbmFluid.builder("gas")
			.temperatureKelvin(111)
			.props(1, 4, 1, EnumSymbol.NONE)
			.trait(FluidTrait.COMBUSTION_TU, 2_000)
			.fuel(FuelGrade.GAS, 3_000_000)
			.gasCanister("gas_full")
			.build();
	public static Fluid petroleum = HbmFluid.builder("petroleum")
			.props(1, 4, 1, EnumSymbol.NONE)
			.trait(FluidTrait.COMBUSTION_TU, 1_650)
			.fuel(FuelGrade.GAS, 2_470_000)
			.gasCanister("gas_petroleum")
			.build();
	public static Fluid petroleum_raw = HbmFluid.builder("petroleum_raw")
			.props(1, 4, 1, EnumSymbol.NONE)
			.trait(FluidTrait.COMBUSTION_TU, 650)
			.fuel(FuelGrade.GAS, 1_000_000)
			.build();
	public static Fluid methane = HbmFluid.builder("methane")
			.props(1, 4, 1, EnumSymbol.NONE)
			.trait(FluidTrait.COMBUSTION_TU, 3_000)
			.fuel(FuelGrade.GAS, 4_500_000)
			.build();

	public static Fluid aromatics = HbmFluid.builder("aromatics")
			.props(1, 4, 1, EnumSymbol.NONE)
			.trait(FluidTrait.COMBUSTION_TU, 5_450)
			.build();
	public static Fluid unsaturateds = HbmFluid.builder("unsaturateds")
			.props(1, 4, 1, EnumSymbol.NONE)
			.trait(FluidTrait.COMBUSTION_TU, 3_660)
			.build();
	
	public static Fluid biogas = HbmFluid.builder("biogas")
			.props(1, 4, 1, EnumSymbol.NONE)
			.trait(FluidTrait.COMBUSTION_TU, 62.5)
			.fuel(FuelGrade.GAS, 78_100)
			.gasCanister("gas_biogas")
			.build();
	public static Fluid biofuel = HbmFluid.builder("biofuel")
			.props(1, 2, 0, EnumSymbol.NONE)
			.trait(FluidTrait.COMBUSTION_TU, 500)
			.fuel(FuelGrade.AERO, 1_250_000)
			.canister("canister_biofuel")
			.build();
	public static Fluid xylene = HbmFluid.builder("xylene")
			.props(2, 3, 0, EnumSymbol.NONE)
			.trait(FluidTrait.COMBUSTION_TU, 3_150)
			.fuel(FuelGrade.HIGH, 7_870_000)
			.build();

	public static Fluid ethanol = new Fluid("ethanol", new ResourceLocation(RefStrings.MODID, "blocks/forgefluid/ethanol_still"), new ResourceLocation(RefStrings.MODID, "blocks/forgefluid/ethanol_flowing"), null, Color.WHITE);
	public static Fluid fishoil = HbmFluid.builder("fishoil")
			.props(0, 1, 0, EnumSymbol.NONE)
			.trait(FluidTrait.COMBUSTION_TU, 75)
			.build();
	public static Fluid sunfloweroil = HbmFluid.builder("sunfloweroil")
			.props(0, 1, 0, EnumSymbol.NONE)
			.build();
	public static Fluid colloid = HbmFluid.builder("colloid")
			.props(0, 0, 0, EnumSymbol.NONE)
			.build();

	public static Fluid nitan = HbmFluid.builder("nitan")
			.props(2, 4, 1, EnumSymbol.NONE).dfc(1.6F)
			.trait(FluidTrait.COMBUSTION_TU, 32_000)
			.fuel(FuelGrade.HIGH, 80_000_000)
			.canister("canister_superfuel")
			.build();
	public static Fluid sparkfuel = HbmFluid.builder("sparkfuel")
			.temperature(20000)
			.props(5, 5, 5, EnumSymbol.RADIATION).dfc(2.5F).trait(FluidTrait.CORROSIVE_2)
			.trait(FluidTrait.COMBUSTION_TU, 256_000)
			.fuel(FuelGrade.HIGH, 640_000_000)
			.build();
	public static Fluid chlorine = HbmFluid.builder("chlorine")
			.props(3, 0, 0, EnumSymbol.OXIDIZER).trait(FluidTrait.CORROSIVE_2)
			.build();
	public static Fluid chloromethane = HbmFluid.builder("chloromethane")
			.props(2, 4, 0, EnumSymbol.NONE)
			.build();
	public static Fluid dichloromethane = HbmFluid.builder("dichloromethane")
			.props(2, 1, 0, EnumSymbol.NONE).trait(FluidTrait.CORROSIVE)
			.build();
	public static Fluid chloroform = HbmFluid.builder("chloroform")
			.props(2, 0, 0, EnumSymbol.NONE).trait(FluidTrait.CORROSIVE)
			.build();
	public static Fluid tetrachromethane = HbmFluid.builder("tetrachromethane")
			.props(3, 0, 0, EnumSymbol.NONE).trait(FluidTrait.CORROSIVE_2)
			.build();
	public static Fluid phosgene = HbmFluid.builder("phosgene")
			.props(4, 0, 1, EnumSymbol.OXIDIZER).trait(FluidTrait.CORROSIVE)
			.build();

	public static Fluid uf6 = HbmFluid.builder("uf6")
			.props(4, 0, 2, EnumSymbol.RADIATION).dfc(1.3F).trait(FluidTrait.CORROSIVE)
			.cell("cell_uf6")
			.build();
	public static Fluid puf6 = HbmFluid.builder("puf6")
			.props(4, 0, 4, EnumSymbol.RADIATION).dfc(1.4F).trait(FluidTrait.CORROSIVE)
			.cell("cell_puf6")
			.build();
	public static Fluid sas3 = HbmFluid.builder("sas3")
			.props(5, 0, 4, EnumSymbol.RADIATION).dfc(1.5F).trait(FluidTrait.CORROSIVE)
			.cell("cell_sas3")
			.build();

	public static Fluid amat = HbmFluid.builder("amat")
			.props(6, 0, 6, EnumSymbol.ANTIMATTER).dfc(2.2F).trait(FluidTrait.AMAT)
			.cell("cell_antimatter")
			.build();
	public static Fluid aschrab = HbmFluid.builder("aschrab")
			.props(6, 1, 6, EnumSymbol.ANTIMATTER).dfc(2.5F).trait(FluidTrait.AMAT)
			.cell("cell_anti_schrabidium")
			.build();

	public static Fluid acid = HbmFluid.builder("acid")
			.props(3, 0, 1, EnumSymbol.OXIDIZER).dfc(1.05F).trait(FluidTrait.CORROSIVE)
			.build();
	public static Fluid sulfuric_acid = HbmFluid.builder("sulfuric_acid")
			.props(3, 0, 2, EnumSymbol.ACID).dfc(1.3F).trait(FluidTrait.CORROSIVE)
			.build();
	public static Fluid nitric_acid = HbmFluid.builder("nitric_acid")
			.props(3, 0, 3, EnumSymbol.ACID).dfc(1.4F).trait(FluidTrait.CORROSIVE_2)
			.build();
	public static Fluid solvent = HbmFluid.builder("solvent")
			.props(2, 3, 0, EnumSymbol.ACID).dfc(1.45F).trait(FluidTrait.CORROSIVE)
			.build();
	public static Fluid radiosolvent = HbmFluid.builder("radiosolvent")
			.props(3, 3, 0, EnumSymbol.ACID).dfc(1.6F).trait(FluidTrait.CORROSIVE_2)
			.build();
	public static Fluid nitroglycerin = HbmFluid.builder("nitroglycerin")
			.props(0, 4, 4, EnumSymbol.NONE).dfc(1.5F)
			.build();
	public static Fluid iongel = HbmFluid.builder("iongel")
			.props(1, 0, 4, EnumSymbol.NONE)
			.build();

	public static Fluid sourgas = HbmFluid.builder("sourgas")
			.props(4, 4, 0, EnumSymbol.ACID).trait(FluidTrait.CORROSIVE)
			.trait(FluidTrait.COMBUSTION_TU, 250)
			.build();

	public static Fluid liquid_osmiridium = HbmFluid.builder("liquid_osmiridium")
			.temperatureKelvin(573)
			.props(5, 0, 5, EnumSymbol.OXIDIZER).dfc(1.8F).trait(FluidTrait.CORROSIVE_2)
			.build();
	public static Fluid watz = HbmFluid.builder("watz")
			.temperatureKelvin(2773).density(2500).viscosity(3000).luminosity(5)
			.props(4, 0, 3, EnumSymbol.OXIDIZER).dfc(1.5F).trait(FluidTrait.CORROSIVE_2)
			.build();
	public static Fluid cryogel = HbmFluid.builder("cryogel")
			.temperatureKelvin(113)
			.props(2, 0, 0, EnumSymbol.CROYGENIC)
			.build();

	public static Fluid hydrogen = HbmFluid.builder("hydrogen")
			.temperatureKelvin(21)
			.props(1, 4, 0, EnumSymbol.CROYGENIC).dfc(1F)
			.trait(FluidTrait.COMBUSTION_TU, 5)
			.fuel(FuelGrade.HIGH, 10_000)
			.gasCanister("gas_hydrogen")
			.build();
	public static Fluid oxygen = HbmFluid.builder("oxygen")
			.temperatureKelvin(90)
			.props(3, 0, 0, EnumSymbol.CROYGENIC).dfc(1.1F)
			.gasCanister("gas_oxygen")
			.build();
	public static Fluid xenon = HbmFluid.builder("xenon")
			.temperatureKelvin(163)
			.props(0, 0, 0, EnumSymbol.ASPHYXIANT).dfc(1.25F)
			.build();
	public static Fluid balefire = HbmFluid.builder("balefire")
			.temperature(15000)
			.props(4, 4, 5, EnumSymbol.RADIATION).dfc(2.4F).trait(FluidTrait.CORROSIVE)
			.trait(FluidTrait.COMBUSTION_TU, 128_000)
			.fuel(FuelGrade.HIGH, 320_000_000)
			.cell("cell_balefire")
			.build();

	public static Fluid mercury = HbmFluid.builder("mercury")
			.props(2, 0, 0, EnumSymbol.NONE)
			.build();

	public static Fluid plasma_hd = HbmFluid.builder("plasma_hd")
			.temperature(25000)
			.props(0, 4, 0, EnumSymbol.RADIATION)
			.trait(FluidTrait.NO_CONTAINER).trait(FluidTrait.NO_ID)
			.build();
	public static Fluid plasma_ht = HbmFluid.builder("plasma_ht")
			.temperature(30000)
			.props(0, 4, 0, EnumSymbol.RADIATION)
			.trait(FluidTrait.NO_CONTAINER).trait(FluidTrait.NO_ID)
			.build();
	public static Fluid plasma_dt = HbmFluid.builder("plasma_dt")
			.temperature(32500)
			.props(0, 4, 0, EnumSymbol.RADIATION)
			.trait(FluidTrait.NO_CONTAINER).trait(FluidTrait.NO_ID)
			.build();
	public static Fluid plasma_xm = HbmFluid.builder("plasma_xm")
			.temperature(45000)
			.props(0, 4, 1, EnumSymbol.RADIATION)
			.trait(FluidTrait.NO_CONTAINER).trait(FluidTrait.NO_ID)
			.build();
	public static Fluid plasma_put = HbmFluid.builder("plasma_put")
			.temperature(50000)
			.props(2, 3, 1, EnumSymbol.RADIATION)
			.trait(FluidTrait.NO_CONTAINER).trait(FluidTrait.NO_ID)
			.build();
	public static Fluid plasma_bf = HbmFluid.builder("plasma_bf")
			.temperature(85000)
			.props(4, 5, 4, EnumSymbol.RADIATION)
			.trait(FluidTrait.NO_CONTAINER).trait(FluidTrait.NO_ID)
			.build();
	
	public static Fluid uu_matter = HbmFluid.builder("ic2uu_matter")
			.textures("blocks/forgefluid/uu_still", "blocks/forgefluid/uu_flowing")
			.temperature(1000000)
			.props(6, 2, 6, EnumSymbol.ACID).dfc(2.0F).trait(FluidTrait.CORROSIVE)
			.trait(FluidTrait.COMBUSTION_TU, 5_000_000)
			.build();

	public static Fluid pain = HbmFluid.builder("pain")
			.props(2, 0, 1, EnumSymbol.ACID).trait(FluidTrait.CORROSIVE)
			.build();
	public static Fluid wastefluid = HbmFluid.builder("wastefluid")
			.props(2, 0, 1, EnumSymbol.RADIATION)
			.build();
	public static Fluid wastegas = HbmFluid.builder("wastegas")
			.props(2, 0, 1, EnumSymbol.RADIATION)
			.build();
	public static Fluid gasoline = HbmFluid.builder("gasoline")
			.props(1, 3, 0, EnumSymbol.NONE)
			.trait(FluidTrait.COMBUSTION_TU, 195)
			.fuel(FuelGrade.MEDIUM, 293_000)
			.canister("canister_gasoline")
			.build();
	public static Fluid gasoline_hq = HbmFluid.builder("gasoline_hq")
			.props(1, 2, 0, EnumSymbol.NONE)
			.trait(FluidTrait.COMBUSTION_TU, 907)
			.fuel(FuelGrade.HIGH, 2_260_000)
			.build();
	public static Fluid experience = HbmFluid.builder("experience")
			.props(0, 0, 0, EnumSymbol.NONE).dfc(1.1F)
			.build();
	
	//Block fluids
	public static Fluid toxic_fluid = new ToxicFluid("toxic_fluid").setDensity(2500).setViscosity(2000).setTemperature(70+273);
	public static Fluid radwater_fluid = new RadWaterFluid("radwater_fluid").setDensity(1000);
	public static Fluid mud_fluid = new MudFluid().setDensity(2500).setViscosity(3000).setLuminosity(5).setTemperature(1773);
	public static Fluid schrabidic = new SchrabidicFluid("schrabidic").setDensity(31200).setViscosity(500);
	public static Fluid corium_fluid = new CoriumFluid().setDensity(31200).setViscosity(2000).setTemperature(3000);
	public static Fluid volcanic_lava_fluid = new VolcanicFluid().setLuminosity(15).setDensity(3000).setViscosity(3000).setTemperature(1300);
	
	public static void init() {
		//oil and ethanol keep the manual guard: common names other mods may register first
		if(!FluidRegistry.registerFluid(oil))
			oil = FluidRegistry.getFluid("oil");
		HbmFluidContainer.CANISTER.register(oil, "canister_oil"); //oil is a manual fluid, register its canister here


		if(!FluidRegistry.registerFluid(ethanol))
			ethanol = FluidRegistry.getFluid("ethanol");

		if(!FluidRegistry.registerFluid(toxic_fluid))
			toxic_fluid = FluidRegistry.getFluid("toxic_fluid");
		if(!FluidRegistry.registerFluid(radwater_fluid))
			radwater_fluid = FluidRegistry.getFluid("radwater_fluid");
		if(!FluidRegistry.registerFluid(mud_fluid))
			mud_fluid = FluidRegistry.getFluid("mud_fluid");
		if(!FluidRegistry.registerFluid(schrabidic))
			schrabidic = FluidRegistry.getFluid("schrabidic");
		if(!FluidRegistry.registerFluid(corium_fluid))
			corium_fluid = FluidRegistry.getFluid("corium_fluid");
		if(!FluidRegistry.registerFluid(volcanic_lava_fluid))
			volcanic_lava_fluid = FluidRegistry.getFluid("volcanic_lava_fluid");

		ModBlocks.toxic_block = new ToxicBlock(ModForgeFluids.toxic_fluid, ModBlocks.fluidtoxic, ModDamageSource.radiation, "toxic_block").setResistance(500F);
		ModBlocks.radwater_block = new RadWaterBlock(ModForgeFluids.radwater_fluid, ModBlocks.fluidradwater, ModDamageSource.radiation, "radwater_block").setResistance(500F);
		ModBlocks.mud_block = new MudBlock(ModForgeFluids.mud_fluid, ModBlocks.fluidmud, ModDamageSource.mudPoisoning, "mud_block").setResistance(500F);
		ModBlocks.schrabidic_block = new SchrabidicBlock(schrabidic, ModBlocks.fluidschrabidic.setReplaceable(), ModDamageSource.radiation, "schrabidic_block").setResistance(500F);
		ModBlocks.corium_block = new CoriumBlock(corium_fluid, ModBlocks.fluidcorium, "corium_block").setResistance(500F);
		ModBlocks.volcanic_lava_block = new VolcanicBlock(volcanic_lava_fluid, ModBlocks.fluidvolcanic, "volcanic_lava_block").setResistance(500F);
		toxic_fluid.setBlock(ModBlocks.toxic_block);
		radwater_fluid.setBlock(ModBlocks.radwater_block);
		mud_fluid.setBlock(ModBlocks.mud_block);
		schrabidic.setBlock(ModBlocks.schrabidic_block);
		corium_fluid.setBlock(ModBlocks.corium_block);
		volcanic_lava_fluid.setBlock(ModBlocks.volcanic_lava_block);
		FluidRegistry.addBucketForFluid(toxic_fluid);
		FluidRegistry.addBucketForFluid(radwater_fluid);
		FluidRegistry.addBucketForFluid(mud_fluid);
		FluidRegistry.addBucketForFluid(schrabidic);
		FluidRegistry.addBucketForFluid(corium_fluid);
		FluidRegistry.addBucketForFluid(volcanic_lava_fluid);

		HbmFluid.registerAllToForge();
	}

	//Stupid forge reads a bunch of default fluids from NBT when the world loads, which screws up my logic for replacing my fluids with fluids from other mods.
	//Forge does this in a place with apparently no events surrounding it. It calls a method in the mod container, but I've
	//been searching for an hour now and I have found no way to make your own custom mod container.
	//Would it have killed them to add a simple event there?!?
	public static void setFromRegistry() {
		spentsteam = FluidRegistry.getFluid("spentsteam");
		steam = FluidRegistry.getFluid("steam");
		hotsteam = FluidRegistry.getFluid("hotsteam");
		superhotsteam = FluidRegistry.getFluid("superhotsteam");
		ultrahotsteam = FluidRegistry.getFluid("ultrahotsteam");
		coolant = FluidRegistry.getFluid("coolant");
		hotcoolant = FluidRegistry.getFluid("hotcoolant");

		heavywater = FluidRegistry.getFluid("heavywater");
		deuterium = FluidRegistry.getFluid("deuterium");
		tritium = FluidRegistry.getFluid("tritium");

		oil = FluidRegistry.getFluid("oil");
		hotoil = FluidRegistry.getFluid("hotoil");
		crackoil = FluidRegistry.getFluid("crackoil");
		hotcrackoil = FluidRegistry.getFluid("hotcrackoil");

		heavyoil = FluidRegistry.getFluid("heavyoil");
		bitumen = FluidRegistry.getFluid("bitumen");
		smear = FluidRegistry.getFluid("smear");
		heatingoil = FluidRegistry.getFluid("heatingoil");

		reclaimed = FluidRegistry.getFluid("reclaimed");
		petroil = FluidRegistry.getFluid("petroil");

		fracksol = FluidRegistry.getFluid("fracksol");
		lubricant = FluidRegistry.getFluid("lubricant");

		naphtha = FluidRegistry.getFluid("naphtha");
		diesel = FluidRegistry.getFluid("diesel");

		lightoil = FluidRegistry.getFluid("lightoil");
		kerosene = FluidRegistry.getFluid("kerosene");

		gas = FluidRegistry.getFluid("gas");
		petroleum = FluidRegistry.getFluid("petroleum");

		aromatics = FluidRegistry.getFluid("aromatics");
		unsaturateds = FluidRegistry.getFluid("unsaturateds");

		biogas = FluidRegistry.getFluid("biogas");
		biofuel = FluidRegistry.getFluid("biofuel");

		ethanol = FluidRegistry.getFluid("ethanol");
		fishoil = FluidRegistry.getFluid("fishoil");
		sunfloweroil = FluidRegistry.getFluid("sunfloweroil");
		colloid = FluidRegistry.getFluid("colloid");

		nitan = FluidRegistry.getFluid("nitan");
		sparkfuel = FluidRegistry.getFluid("sparkfuel");

		uf6 = FluidRegistry.getFluid("uf6");
		puf6 = FluidRegistry.getFluid("puf6");
		sas3 = FluidRegistry.getFluid("sas3");

		amat = FluidRegistry.getFluid("amat");
		aschrab = FluidRegistry.getFluid("aschrab");

		acid = FluidRegistry.getFluid("acid");
		sourgas = FluidRegistry.getFluid("sourgas");
		sulfuric_acid = FluidRegistry.getFluid("sulfuric_acid");
		nitric_acid = FluidRegistry.getFluid("nitric_acid");
		solvent = FluidRegistry.getFluid("solvent");
		radiosolvent = FluidRegistry.getFluid("radiosolvent");
		nitroglycerin = FluidRegistry.getFluid("nitroglycerin");
		liquid_osmiridium = FluidRegistry.getFluid("liquid_osmiridium");
		watz = FluidRegistry.getFluid("watz");
		cryogel = FluidRegistry.getFluid("cryogel");

		hydrogen = FluidRegistry.getFluid("hydrogen");
		oxygen = FluidRegistry.getFluid("oxygen");
		xenon = FluidRegistry.getFluid("xenon");
		balefire = FluidRegistry.getFluid("balefire");

		mercury = FluidRegistry.getFluid("mercury");

		plasma_dt = FluidRegistry.getFluid("plasma_dt");
		plasma_hd = FluidRegistry.getFluid("plasma_hd");
		plasma_ht = FluidRegistry.getFluid("plasma_ht");
		plasma_put = FluidRegistry.getFluid("plasma_put");
		plasma_xm = FluidRegistry.getFluid("plasma_xm");
		plasma_bf = FluidRegistry.getFluid("plasma_bf");
		uu_matter = FluidRegistry.getFluid("ic2uu_matter");
		
		pain = FluidRegistry.getFluid("pain");
		wastefluid = FluidRegistry.getFluid("wastefluid");
		wastegas = FluidRegistry.getFluid("wastegas");
		gasoline = FluidRegistry.getFluid("gasoline");
		experience = FluidRegistry.getFluid("experience");

		toxic_fluid = FluidRegistry.getFluid("toxic_fluid");
		radwater_fluid = FluidRegistry.getFluid("radwater_fluid");
		mud_fluid = FluidRegistry.getFluid("mud_fluid");
		schrabidic = FluidRegistry.getFluid("schrabidic");
		corium_fluid = FluidRegistry.getFluid("corium_fluid");
	}

	@SubscribeEvent
	public static void worldLoad(WorldEvent.Load evt) {
		setFromRegistry();
	}

	public static void registerFluidColors(){
		for(Fluid f : FluidRegistry.getRegisteredFluids().values()){
			fluidColors.put(f, FFUtils.getColorFromFluid(f));
		}
	}

	public static int getFluidColor(Fluid f){
		if(f == null)
			return 0;
		Integer color = fluidColors.get(f);
		if(color == null)
			return 0xFFFFFF;
		return color;
	}
}
