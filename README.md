<div align="center">

<img src="https://capsule-render.vercel.app/api?type=waving&height=180&color=0:F59E0B,50:F97316,100:DC2626&text=Well-forged%20Edition&fontSize=52&fontColor=ffffff&fontAlignY=38&desc=HBM%27s%20Nuclear%20Tech%20%C2%B7%20Minecraft%201.12.2&descAlignY=62&descSize=17&animation=fadeIn" alt="HBM's Nuclear Tech — Well-forged Edition" width="100%" />

![Minecraft](https://img.shields.io/badge/Minecraft-1.12.2-3C8527?style=for-the-badge)
![Forge](https://img.shields.io/badge/Forge-1.12.2-E04E14?style=for-the-badge)
![Java](https://img.shields.io/badge/Java-8_target-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)
![Release](https://img.shields.io/badge/Release-1.3.2-6C63FF?style=for-the-badge)
![License](https://img.shields.io/badge/License-GPLv3-B22222?style=for-the-badge)
[![CurseForge](https://img.shields.io/badge/CurseForge-Download-F16436?style=for-the-badge&logo=curseforge&logoColor=white)](https://www.curseforge.com/minecraft/mc-mods/ntm-well-forged-edition)

</div>

## Overview

**HBM's Nuclear Tech — Well-forged Edition** is a community-maintained continuation of the HBM's Nuclear Tech lineage for Minecraft 1.12.2.
It builds on the Extended Edition / Custom 1.12.2 branch and keeps the original nuclear-industrial sandbox while expanding progression, tightening balance, and hardening the engine for long modpack playthroughs.

This README describes the edition as a whole — its gameplay direction, balance philosophy, and engineering work relative to the upstream 1.12.2 lineage — rather than an individual contributor's changelog.

The project focuses on four areas:

- **Gameplay:** deeper petrochemistry, new machines, fluids, items, and processing chains.
- **Balance:** reworked recipes, power and heat costs, ore distribution, and progression gates.
- **Performance and stability:** fluid-network reworks, packet fixes, dedicated-server hardening, and regression tests.
- **Quality of life:** better JEI integration, GUIs, tooltips, localization, sounds, and visual feedback.

## At a glance

| Parameter | Value |
|:---|:---|
| Minecraft | 1.12.2 |
| Loader | Forge |
| Current version | 1.3.2 |
| Runtime target | Java 8 |
| Development toolchain | Java 25 via JvmDowngrader |
| Active branch | `Custom-1.12.2` |
| License | GPLv3; see `LICENSE` and `LICENSE.LESSER` |

## What makes this edition different

### Gameplay and progression

The edition expands the industrial chain rather than adding isolated machines. New processing steps feed existing ones, and several late-game recipes now depend on the new intermediate products.

Major additions include:

- **Petrochemical chain.** Hydrotreater, Catalytic Reformer, Pyrolysis Oven, Fraction Tower heat, refinery heat, high-grade petroil, reformate, high-grade diesel, and high-grade kerosene.
- **Electrolysis.** A dedicated Electrolyser with fluid and metal modes, speed/power/overdrive upgrades, water and heavy-water electrolysis, crystal recipes, and perfect bedrock ore processing.
- **Gas handling.** Gas Flare soot byproducts, fuel-gas support for the Turbofan, sour gas, and expanded JEI gas listings.
- **Ocean resources.** Ocean Pump that produces brine in ocean biomes under valid sea-level conditions.
- **Solids and plastics.** PVC, hard plastic, fullerene, fullerite, soot, and new intermediate items.
- **Compressed fluids.** Compressor and compressed-fluid recipes.
- **Machine content.** New machine implementations, recipes, GUI work, sounds, and particles across the processing chain.

Existing HBM systems are still the backbone: reactors, multiblocks, nuclear explosions, radiation, missile silos, and the industrial progression players expect from the 1.12.2 branch.

### Balance

Balance changes are aimed at making progression more interconnected and less dependent on a single dominant path.

Examples:

- **Refinery and Fraction Tower** received heat mechanics, rebalanced power/heat costs, and shared feedstock behavior across the tower stack.
- **Gas Flare** soot yield and fuel-gas behavior were tuned.
- **Breeder** recipes were reworked: uranium and U238 rods now breed into reactor-grade plutonium, while several legacy recipes were removed.
- **Assembly recipes** for advanced machines and components now require hard plastic and fullerite, tying late-game construction to the new production chains.
- **Bedrock ore distribution** was restricted by dimension, and impossible bedrock ores are hidden from JEI together with their processing chains.
- **Engines, solar boiler, Defabricator, loot pools, and several legacy recipes** received targeted balance passes.

Where possible, values remain configurable through HBM's configuration system.

### Performance and stability

A large part of the edition is engineering work that is not visible in a feature list but matters in long sessions and on servers.

- **Fluid network rework.** Iterative pipe traversal replaced recursive flood-fill, fixing `StackOverflowError` on long pipelines; pipe migration preserves backward compatibility; dead-consumer and network-join bugs were fixed.
- **Packet pressure.** Fluid tank sync is gated, looped sounds and tank packets use tracking/ranged dispatch, large Netty packets flush correctly, and fluid packet spam was reduced.
- **Dedicated server hardening.** Fixed the `@SideOnly` crash in `AudioWrapper`, added server-side regression tests, and added a dedicated-server smoke test to CI.
- **Multiblock placement checks.** Space checks now respect replaceable blocks and cover the same volumes that are actually filled, preventing machines from overwriting existing multiblocks.
- **Dummy blocks.** Reworked and optimized the dummy-block system and fixed several server crashes involving orphaned or missing core positions.
- **Nuclear fallout.** Fixed fallout moving machines, multiblock parts, chests, and pipes into gaps as empty copies, and fixed falling fallout blocks overwriting non-falling blocks.
- **Memory and state bugs.** Fixed the `RadiationSystemNT` buffer leak, shared missile acceleration state, and several NBT/identifier issues.
- **Observability.** Added the `/hbm tps` command and an in-game performance settings section.

### Quality of life

- **JEI:** fluid icons, generator/fluid search tags, recipe clues for assembly and chemistry machines, and hiding of unobtainable bedrock ores.
- **GUIs:** new assembly, chemical, chemplant, and fluid-tank interfaces; output-slot recoloring; crate fill percentage and opening animations.
- **Tooltips and localization:** more item/GUI descriptions, localized tool abilities, and clearer machine information.
- **Crates:** dynamic top textures, lock handling, inventory saving fixes, and container fixes.
- **Tools and equipment:** ToolAbility system ported from CE, wire durability and repair, cable drum stack size, and anvil upgrade rework.
- **Audio and visuals:** machine sounds for arc welders, pyro-oven, electrolyser, turbofan, selenium engine, crystallizer, excavator, and diesel generators; improved gas-flare particles and sounds.
- **World migration:** migration support for legacy fluid tank and crystallizer multiblocks.

## Content highlights

| System | Highlights |
|:---|:---|
| Petrochemistry | Hydrotreater, Catalytic Reformer, Pyrolysis Oven, Fraction Tower heat, reformate, high-grade fuels |
| Electrolysis | Fluid/metal modes, heavy water, crystal processing, perfect bedrock ore, fullerene |
| Gas processing | Gas Flare, soot, sour gas, fuel gas, Turbofan integration |
| Fluids | Brine, sodium hydroxide, sodium base, hot sodium, electrosolvent, compressed fluids |
| Materials | PVC, hard plastic, fullerene, fullerite, soot powder |
| Fluid logistics | Fluid Update pipe system, migration, onScrew logic, extraction-speed fixes |
| UI and integration | JEI categories, recipe clues, fluid icons, crates, localized tooltips |
| Stability | Dedicated-server smoke test, AudioWrapper regression tests, multiblock and dummy-block fixes |

## Installation

1. Install **Minecraft Forge 1.12.2**.
2. Place the release jar into the `mods` folder.
3. Launch the game with the Forge profile.

The mod declares Minecraft Forge as its loader dependency. Optional integrations with CraftTweaker, JEI, OpenComputers, CoFH, AE2, and other 1.12.2 mods are detected when those mods are present; check the release page for the recommended environment.

> Back up existing worlds before switching between forks or major versions. The edition includes migration logic for some legacy multiblocks, but registry and fluid changes can still affect old saves.

## Building from source

Requirements:

- JDK 25 for development (the build uses modern Java syntax through JvmDowngrader and targets JVM 8);
- Gradle Wrapper from the repository;
- Forge 1.12.2 development environment.

```bash
./gradlew build
./gradlew runClient
./gradlew runServer
./gradlew check
```

The release jar appears in `build/libs/`. On Windows, `buildFast.bat` builds only the release jar.

### Tests and CI

The `ReleaseFixes` branch runs `./gradlew check` in GitHub Actions. The check includes the AudioWrapper server-side regression tests and a dedicated-server smoke test.

## Project layout

| Package | Responsibility |
|:---|:---|
| `com.hbm.blocks` | Machines, multiblocks, reactors, processing blocks |
| `com.hbm.tileentity` | Machine logic, inventories, fluid and energy handling |
| `com.hbm.items` | Tools, components, fuel, armor, and equipment |
| `com.hbm.crafting` | Recipes and recipe registration |
| `com.hbm.explosion` | Nuclear explosions, fallout, and destruction logic |
| `com.hbm.hazard` | Radiation, hazard, and contamination systems |
| `com.hbm.entity` | Missiles, projectiles, and nuclear entities |
| `com.hbm.packet` | Network packets and synchronization |
| `com.hbm.world` | World generation and structure logic |
| `com.hbm.render`, `com.hbm.particle`, `com.hbm.sound` | Client rendering, particles, and audio |
| `com.hbm.config`, `com.hbm.command` | Configuration and commands |
| `com.hbm.core`, `com.hbm.main` | Coremod bootstrap and mod entry points |

## Branches and releases

- `Custom-1.12.2` — active development branch.
- `ReleaseFixes` — stabilization branch used for CI checks and releases.
- `Test-1.12.2` — experimental and test work.

The current release line is **1.3.2**. Published tags start at `1.0.5` and include `1.1.0`, `1.2.x`, `1.3.0`, `1.3.1`, and `1.3.2`.

## Fork lineage

Well-forged Edition would not exist without the upstream work:

- **HBMTheBobcat / HBMMods** — original HBM's Nuclear Tech Mod.
- **Drillgon200** — original 1.12.2 port.
- **TheOriginalGolem** — 1.12.2 bug fixing.
- **Alcater** — Custom 1.12.2 version, updates, and textures.
- **Glozeysk** — Well-forged Edition fork author.
- **StefanParmezan** — fork developer.
- **Yar1k** — texture painting and ideas.
- **Community contributors** — bug reports, pull requests, ports, and feedback.

## Contributing

Pull requests are welcome. For larger changes, open an issue first so the direction can be discussed before implementation.

When submitting a change:

- keep the scope focused;
- test both client and dedicated server behavior;
- include reproduction steps for bug fixes;
- follow the existing code style and localization conventions.

## Links

- [CurseForge](https://www.curseforge.com/minecraft/mc-mods/ntm-well-forged-edition)
- [GitHub](https://github.com/Glozeysk/Hbm-s-Nuclear-Tech-Well-Forged-Edition)
- [Issues](https://github.com/Glozeysk/Hbm-s-Nuclear-Tech-Well-Forged-Edition/issues)

## License

The repository includes the **GNU General Public License v3** (`LICENSE`) and the **GNU Lesser General Public License v3** (`LICENSE.LESSER`). See those files for the terms that apply to the corresponding parts of the project.
