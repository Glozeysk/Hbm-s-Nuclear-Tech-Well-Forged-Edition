package com.hbm.lib;

public class RefStrings {
	public static final String MODID = "hbm";
	public static final String NAME = "Hbm's Nuclear Tech - Waldemar Edition";
	public static final String VERSION = "NTM-Waldemar-Edition-1.12.2-1.2.4";
	public static final String BUILD_DATE;
	public static final String CHANGELOG = "Update 1.2.3\n" +
			"- Added grow progress bar in ITER GUI;\n" +
			"- Added speed change in ITER breeder which depends on required heat and actual heat;\n" +
			"- Added flux summary in RBMK outgasser;\n" +
			"- Added bedrock ore to neutrino lens;\n" +
			"- Added tooltip to Big Furnace;\n" +
			"- Added void buttons for crystallizer and plasma heater fluid tanks;\n" +
			"- Added compatibility armor with Quark emotes.\n" +
			"\n" +
			"- Injected and tested new BufPacket serialization/deserialization system;\n" +
			"- Fully reworked magnetic ducts;\n" +
			"- Changed magnetic ducts craft;\n" +
			"- Changed arsenic nugget craft;\n" +
			"- Ported Machine Assembly model, sounds, animations;\n" +
			"- Ported Machine Chemplant model and animations;\n" +
			"- Added wire recoloring and changed wiring_red_copper texture and craft.\n" +
			"\n" +
			"- Fixed Oil Spill particles in MachineOilWell;\n" +
			"- Fixed armor helmet rotation issues on armor stands;\n" +
			"- Fixed neutrino lens visual bugs;\n" +
			"- Fixed lid jumping on heatproof columns;\n" +
			"- Fixed shift+click interactions on conveyor extractors/inserters;\n" +
			"- Fixed jetpacks model sync;\n" +
			"- Fixed cyclotron recipes bug;\n" +
			"- Fixed night goggles effect bug.";
	//HBM's Beta Naming Convention:
	//V T (X)
	//V -> next release version
	//T -> build type
	//X -> days since 10/10/10
	//Drillgon200: I completely ignored this to make my own even worse naming system. Sigh.
	public static final String CLIENTSIDE = "com.hbm.main.ClientProxy";
	public static final String SERVERSIDE = "com.hbm.main.ServerProxy";



	static {
		String date = "";
		try {
			java.util.Properties props = new java.util.Properties();
			props.load(RefStrings.class.getClassLoader().getResourceAsStream("build_date.properties"));
			date = props.getProperty("build.date", "");
		} catch(Exception e) {
			date = "";
		}
		BUILD_DATE = date;
	}
}