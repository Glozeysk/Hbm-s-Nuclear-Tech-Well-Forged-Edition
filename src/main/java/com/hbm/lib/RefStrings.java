package com.hbm.lib;

public class RefStrings {
	public static final String MODID = "hbm";
	public static final String NAME = "Hbm's Nuclear Tech - Well-forged Edition";
	public static final String VERSION = "NTM-Well-forged-Edition-1.12.2-1.3.1-hotfix1";
	public static final String BUILD_DATE = BuildInfo.BUILD_DATE;
	public static final String CHANGELOG = "Update 1.3.0 \"Fluid Update\"\n" +
			"- Ported ToolAbility system from CE\n" +
			"- Added ToolTypes for some blocks \n" +
			"- Added many localizations for items and gui titles\n" +
			"- Added opening animation for  Crates (Blocks and ItemBlocks)\n" +
			"- Added fill percentage in Crates GUI\n" +
			"- Added new FluidDuct system (with backward compatibility)\n" +
			"- Added PipeStub rendering in RenderFluidBarrel      \n" +
			"- Added BlockDecoSign (WIP)\n" +
			"- Added new Anvil update system\n" +
			"- Added particles for TileEntityExcavator\n" +
			"\n" +
			"\n" +
			"- Changed ItemToolAbility hands priority\n" +
			"- Changed onScrew logic for pipes\n" +
			"- Changed MeteoriteSwords weapon abilities\n" +
			"- Changed ModName\n" +
			"\n" +
			"- Fixed Crates with locks compatibility\n" +
			"- Fixed BogoSorter compatibility with crates\n" +
			"- Fixed Item filling in crates\n" +
			"- Fixed ConcurrentModificationException in fluid pipes\n" +
			"- Fixed MachineChemical insertItem logic\n" +
			"- Fixed MachineAutoCrafterOutput\n" +
			"- Fixed drop area for crates and batteries     \n" +
			"- Fixed DiFurnaceBig logic \n" +
			"- Fixed Array outOfBound crash in DiFurnaceBig, SteelFurnace and BlockRouter GUI\n" +
			"- Fixed BlockConveyor onScrew client crash       \n" +
			"- Fixed conveyor items dupe, when they're touching projectiles\n" +
			"- Fixed FluidIcons rendering    \n" +
			"- Fixed NPE in GUIScreenBobmazon\n" +
			"- Fixed WeaponThompson render\n" +
			"- Fixed BlockSafe structure  \n" +
			"- Fixed MachineMixer endless energy bug   \n" +
			"- Fixed MeteoriteSword rendering (client crash)\n" +
			"- Fixed MachineLadder NPE (Mobs AI issue)\n" +
			"\n" +
			"- Removed legacy FluidSucc system\n" +
			"- Removed coal hazard for lignite    \n" +
			"- Grenade Nuke spawn in HbmStructures";
	//HBM's Beta Naming Convention:
	//V T (X)
	//V -> next release version
	//T -> build type
	//X -> days since 10/10/10
	//Drillgon200: I completely ignored this to make my own even worse naming system. Sigh.
	public static final String CLIENTSIDE = "com.hbm.main.ClientProxy";
	public static final String SERVERSIDE = "com.hbm.main.ServerProxy";
}