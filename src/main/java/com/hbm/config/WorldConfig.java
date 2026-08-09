package com.hbm.config;

import com.hbm.config.CompatibilityConfig;

public class WorldConfig {

	public static int uraniumSpawn = 6;
	public static int thoriumSpawn = 7;
	public static int titaniumSpawn = 8;
	public static int sulfurSpawn = 5;
	public static int aluminiumSpawn = 7;
	public static int copperSpawn = 12;
	public static int fluoriteSpawn = 6;
	public static int niterSpawn = 6;
	public static int tungstenSpawn = 10;
	public static int leadSpawn = 6;
	public static int berylliumSpawn = 6;
	public static int ligniteSpawn = 2;
	public static int asbestosSpawn = 4;
	public static int rareSpawn = 6;
	public static int lithiumSpawn = 6;
	public static int cinnebarSpawn = 1;
	public static int oilcoalSpawn = 128;
	public static int gassshaleSpawn = 5;
	public static int gasbubbleSpawn = 4;
	public static int explosivebubbleSpawn = 8;
	public static int cobaltSpawn = 2;
	
	public static int ironClusterSpawn = 4;
	public static int titaniumClusterSpawn = 2;
	public static int aluminiumClusterSpawn = 3;
	
	public static int netherUraniumuSpawn = 8;
	public static int netherTungstenSpawn = 10;
	public static int netherSulfurSpawn = 26;
	public static int netherPhosphorusSpawn = 24;
	public static int netherCoalSpawn = 24;
	public static int netherPlutoniumSpawn = 8;
	public static int netherCobaltSpawn = 2;

	public static int endTikiteSpawn = 8;
	
	public static int radioStructure = 500;
	public static int antennaStructure = 750;
	public static int atomStructure = 500;
	public static int vertibirdStructure = 500;
	public static int dungeonStructure = 64;
	public static int relayStructure = 500;
	public static int satelliteStructure = 500;
	public static int bunkerStructure = 1000;
	public static int siloStructure = 1000;
	public static int factoryStructure = 1000;
	public static int dudStructure = 500;
	public static int spaceshipStructure = 1000;
	public static int barrelStructure = 5000;
	public static int geyserWater = 3000;
	public static int geyserChlorine = 3000;
	public static int geyserVapor = 500;
	public static int meteorStructure = 15000;
	public static int capsuleStructure = 100;
	public static int broadcaster = 5000;
	public static int minefreq = 64;
	public static int radfreq = 5000;
	public static int vaultfreq = 2500;
	public static int arcticStructure = 500;
	public static int jungleStructure = 2000;
	public static int pyramidStructure = 4000;
	
	public static int meteorStrikeChance = 20 * 60 * 180;
	public static int meteorShowerChance = 20 * 60 * 5;
	public static int meteorShowerDuration = 6000;

	public static int convertToInt(Object e){
		if(e == null)
			return 0;
		return (int)e;
	}

	private static int structureValue(Object value, int fallback) {
		int result = convertToInt(value);
		return result > 0 ? result : fallback;
	}
	
	public static void loadFromCompatibilityConfig() {

		uraniumSpawn = convertToInt(CompatibilityConfig.uraniumSpawn.get(0));
		titaniumSpawn = convertToInt(CompatibilityConfig.titaniumSpawn.get(0));
		sulfurSpawn = convertToInt(CompatibilityConfig.sulfurSpawn.get(0));
		aluminiumSpawn = convertToInt(CompatibilityConfig.aluminiumSpawn.get(0));
		copperSpawn = convertToInt(CompatibilityConfig.copperSpawn.get(0));
		fluoriteSpawn = convertToInt(CompatibilityConfig.fluoriteSpawn.get(0));
		niterSpawn = convertToInt(CompatibilityConfig.niterSpawn.get(0));
		tungstenSpawn = convertToInt(CompatibilityConfig.tungstenSpawn.get(0));
		leadSpawn = convertToInt(CompatibilityConfig.leadSpawn.get(0));
		berylliumSpawn = convertToInt(CompatibilityConfig.berylliumSpawn.get(0));
		thoriumSpawn = convertToInt(CompatibilityConfig.thoriumSpawn.get(0));
		ligniteSpawn = convertToInt(CompatibilityConfig.ligniteSpawn.get(0));
		asbestosSpawn = convertToInt(CompatibilityConfig.asbestosSpawn.get(0));
		lithiumSpawn = convertToInt(CompatibilityConfig.lithiumSpawn.get(0));
		rareSpawn = convertToInt(CompatibilityConfig.rareSpawn.get(0));
		oilcoalSpawn = convertToInt(CompatibilityConfig.oilcoalSpawn.get(0));
		gassshaleSpawn = convertToInt(CompatibilityConfig.gassshaleSpawn.get(0));
		explosivebubbleSpawn = convertToInt(CompatibilityConfig.explosivebubbleSpawn.get(0));
		gasbubbleSpawn = convertToInt(CompatibilityConfig.gasbubbleSpawn.get(0));
		cinnebarSpawn = convertToInt(CompatibilityConfig.cinnebarSpawn.get(0));
		cobaltSpawn = convertToInt(CompatibilityConfig.cobaltSpawn.get(0));
		
		ironClusterSpawn = convertToInt(CompatibilityConfig.ironClusterSpawn.get(0));
		titaniumClusterSpawn = convertToInt(CompatibilityConfig.titaniumClusterSpawn.get(0));
		aluminiumClusterSpawn = convertToInt(CompatibilityConfig.aluminiumClusterSpawn.get(0));
		
		netherUraniumuSpawn = convertToInt(CompatibilityConfig.netherUraniumSpawn.get(-1));
		netherTungstenSpawn = convertToInt(CompatibilityConfig.netherTungstenSpawn.get(-1));
		netherSulfurSpawn = convertToInt(CompatibilityConfig.netherSulfurSpawn.get(-1));
		netherPhosphorusSpawn = convertToInt(CompatibilityConfig.netherPhosphorusSpawn.get(-1));
		netherCoalSpawn = convertToInt(CompatibilityConfig.netherCoalSpawn.get(-1));
		netherPlutoniumSpawn = convertToInt(CompatibilityConfig.netherPlutoniumSpawn.get(-1));
		netherCobaltSpawn = convertToInt(CompatibilityConfig.netherCobaltSpawn.get(-1));
		
		endTikiteSpawn = convertToInt(CompatibilityConfig.endTixiteSpawn.get(1));
		
        
		radioStructure = structureValue(CompatibilityConfig.radioStructure.get(0), radioStructure);
		antennaStructure = structureValue(CompatibilityConfig.antennaStructure.get(0), antennaStructure);
		atomStructure = structureValue(CompatibilityConfig.atomStructure.get(0), atomStructure);
		vertibirdStructure = structureValue(CompatibilityConfig.vertibirdStructure.get(0), vertibirdStructure);
		dungeonStructure = structureValue(CompatibilityConfig.dungeonStructure.get(0), dungeonStructure);
		relayStructure = structureValue(CompatibilityConfig.relayStructure.get(0), relayStructure);
		satelliteStructure = structureValue(CompatibilityConfig.satelliteStructure.get(0), satelliteStructure);
		bunkerStructure = structureValue(CompatibilityConfig.bunkerStructure.get(0), bunkerStructure);
		siloStructure = structureValue(CompatibilityConfig.siloStructure.get(0), siloStructure);
		factoryStructure = structureValue(CompatibilityConfig.factoryStructure.get(0), factoryStructure);
		dudStructure = structureValue(CompatibilityConfig.dudStructure.get(0), dudStructure);
		spaceshipStructure = structureValue(CompatibilityConfig.spaceshipStructure.get(0), spaceshipStructure);
		barrelStructure = structureValue(CompatibilityConfig.barrelStructure.get(0), barrelStructure);
		broadcaster = structureValue(CompatibilityConfig.broadcaster.get(0), broadcaster);
		minefreq = structureValue(CompatibilityConfig.minefreq.get(0), minefreq);
		radfreq = structureValue(CompatibilityConfig.radfreq.get(0), radfreq);
		vaultfreq = structureValue(CompatibilityConfig.vaultfreq.get(0), vaultfreq);
		geyserWater = structureValue(CompatibilityConfig.geyserWater.get(0), geyserWater);
		geyserChlorine = structureValue(CompatibilityConfig.geyserChlorine.get(0), geyserChlorine);
		

		geyserVapor = structureValue(CompatibilityConfig.geyserVapor.get(0), geyserVapor);
		meteorStructure = structureValue(CompatibilityConfig.meteorStructure.get(0), meteorStructure);
		capsuleStructure = structureValue(CompatibilityConfig.capsuleStructure.get(0), capsuleStructure);
		arcticStructure = structureValue(CompatibilityConfig.arcticStructure.get(0), arcticStructure);
		jungleStructure = structureValue(CompatibilityConfig.jungleStructure.get(0), jungleStructure);
		pyramidStructure = structureValue(CompatibilityConfig.pyramidStructure.get(0), pyramidStructure);
		

		meteorStrikeChance = convertToInt(CompatibilityConfig.meteorStrikeChance.get(0));
		meteorShowerChance = convertToInt(CompatibilityConfig.meteorShowerChance.get(0));
		meteorShowerDuration = convertToInt(CompatibilityConfig.meteorShowerDuration.get(0));
		
	}

}
