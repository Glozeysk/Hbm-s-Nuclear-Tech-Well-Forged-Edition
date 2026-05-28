package com.hbm.items.tool;

import api.hbm.item.IDepthRockTool;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import com.hbm.blocks.ModBlocks;
import com.hbm.blocks.generic.BlockBedrockOre;
import com.hbm.blocks.generic.BlockBedrockOreTE;
import com.hbm.blocks.generic.ItemBlockStorageCrate;
import com.hbm.handler.HbmKeybinds;
import com.hbm.handler.ToolAbility;
import com.hbm.handler.ToolPreset;
import com.hbm.handler.WeaponAbility;
import com.hbm.handler.ability.AvailableAbilities;
import com.hbm.handler.ability.IBaseAbility;
import com.hbm.handler.ability.IToolAreaAbility;
import com.hbm.handler.ability.IToolHarvestAbility;
import com.hbm.inventory.gui.GUIScreenToolAbility;
import com.hbm.items.ModItems;
import com.hbm.main.MainRegistry;
import com.hbm.packet.NBTItemControlPacket;
import com.hbm.packet.PacketDispatcher;
import com.hbm.util.I18nUtil;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiIngame;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.init.SoundEvents;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.*;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.network.play.server.SPacketBlockChange;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.*;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IInteractionObject;
import net.minecraft.world.World;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.common.ForgeHooks;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.opengl.GL11;

import java.util.*;

public class ItemToolAbility extends ItemTool implements IItemAbility, IDepthRockTool {

	private EnumToolType toolType;
	private EnumRarity rarity = EnumRarity.COMMON;
	protected float damage;
	protected double movement;
	private List<ToolAbility> breakAbility = new ArrayList<>();
	private List<WeaponAbility> hitAbility = new ArrayList<>();
	public static int dropX = 0, dropY = 0, dropZ = 0;
	private boolean rockBreaker = false;
	protected AvailableAbilities availableAbilities = new AvailableAbilities().addToolAbilities();

	private static final Map<IBaseAbility, Map.Entry<Integer, Integer>> abilityGui = new LinkedHashMap<>();

	@SideOnly(Side.CLIENT)
	private static boolean wasAltDown = false;

	static {
		abilityGui.put(IToolAreaAbility.RECURSION, new AbstractMap.SimpleImmutableEntry<>(0, 138));
		abilityGui.put(IToolAreaAbility.HAMMER, new AbstractMap.SimpleImmutableEntry<>(16, 138));
		abilityGui.put(IToolAreaAbility.HAMMER_FLAT, new AbstractMap.SimpleImmutableEntry<>(32, 138));
		abilityGui.put(IToolAreaAbility.EXPLOSION, new AbstractMap.SimpleImmutableEntry<>(48, 138));
	}

	public enum EnumToolType {
		PICKAXE(Sets.newHashSet(Material.IRON, Material.ANVIL, Material.ROCK), Sets.newHashSet(Blocks.ACTIVATOR_RAIL, Blocks.COAL_ORE, Blocks.COBBLESTONE, Blocks.DETECTOR_RAIL, Blocks.DIAMOND_BLOCK, Blocks.DIAMOND_ORE, Blocks.DOUBLE_STONE_SLAB, Blocks.GOLDEN_RAIL, Blocks.GOLD_BLOCK, Blocks.GOLD_ORE, Blocks.ICE, Blocks.IRON_BLOCK, Blocks.IRON_ORE, Blocks.LAPIS_BLOCK, Blocks.LAPIS_ORE, Blocks.LIT_REDSTONE_ORE, Blocks.MOSSY_COBBLESTONE, Blocks.NETHERRACK, Blocks.PACKED_ICE, Blocks.RAIL, Blocks.REDSTONE_ORE, Blocks.SANDSTONE, Blocks.RED_SANDSTONE, Blocks.STONE, Blocks.STONE_SLAB, Blocks.STONE_BUTTON, Blocks.STONE_PRESSURE_PLATE)),
		AXE(Sets.newHashSet(Material.WOOD, Material.PLANTS, Material.VINE), Sets.newHashSet(Blocks.PLANKS, Blocks.BOOKSHELF, Blocks.LOG, Blocks.LOG2, Blocks.CHEST, Blocks.PUMPKIN, Blocks.LIT_PUMPKIN, Blocks.MELON_BLOCK, Blocks.LADDER, Blocks.WOODEN_BUTTON, Blocks.WOODEN_PRESSURE_PLATE)),
		SHOVEL(Sets.newHashSet(Material.CLAY, Material.SAND, Material.GROUND, Material.SNOW, Material.CRAFTED_SNOW), Sets.newHashSet(Blocks.CLAY, Blocks.DIRT, Blocks.FARMLAND, Blocks.GRASS, Blocks.GRAVEL, Blocks.MYCELIUM, Blocks.SAND, Blocks.SNOW, Blocks.SNOW_LAYER, Blocks.SOUL_SAND, Blocks.GRASS_PATH, Blocks.CONCRETE_POWDER)),
		MINER(Sets.newHashSet(Material.GRASS, Material.IRON, Material.ANVIL, Material.ROCK, Material.CLAY, Material.SAND, Material.GROUND, Material.SNOW, Material.CRAFTED_SNOW));

		EnumToolType(Set<Material> materials) { this.materials = materials; }
		EnumToolType(Set<Material> materials, Set<Block> blocks) { this.materials = materials; this.blocks = blocks; }
		public Set<Material> materials = new HashSet<>();
		public Set<Block> blocks = new HashSet<>();
	}

	public ItemToolAbility(float damage, float attackSpeedIn, double movement, ToolMaterial material, EnumToolType type, String s) {
		super(0, attackSpeedIn, material, type.blocks);
		this.setTranslationKey(s);
		this.setRegistryName(s);
		this.damage = damage;
		this.movement = movement;
		this.toolType = type;
		if (type == EnumToolType.MINER) {
			this.setHarvestLevel("shovel", material.getHarvestLevel());
			this.setHarvestLevel("pickaxe", material.getHarvestLevel());
			this.setHarvestLevel("axe", material.getHarvestLevel());
		} else {
			this.setHarvestLevel(type.toString().toLowerCase(), material.getHarvestLevel());
		}
		ModItems.ALL_ITEMS.add(this);
	}

	public ItemToolAbility addAbility(IBaseAbility ability, int level) {
		this.availableAbilities.addAbility(ability, level);
		return this;
	}

	public ItemToolAbility addBreakAbility(ToolAbility ability) {
		this.breakAbility.add(ability);
		if (ability instanceof ToolAbility.RecursionAbility) {
			availableAbilities.addAbility(IToolAreaAbility.RECURSION, ((ToolAbility.RecursionAbility) ability).getRadius());
		} else if (ability instanceof ToolAbility.HammerAbility) {
			int range = ((ToolAbility.HammerAbility) ability).getRange();
			availableAbilities.addAbility(IToolAreaAbility.HAMMER, range);
			availableAbilities.addAbility(IToolAreaAbility.HAMMER_FLAT, range);
		} else if (ability instanceof ToolAbility.LuckAbility) {
			availableAbilities.addAbility(IToolHarvestAbility.LUCK, ((ToolAbility.LuckAbility) ability).getLuck());
		} else if (ability instanceof ToolAbility.ExplosionAbility) {
			float[] explosionLevels = {0F, 2.5F, 5F, 10F, 15F};
			float target = ((ToolAbility.ExplosionAbility) ability).getStrength();
			int lvl = findLevelIndexFloat(explosionLevels, target);
			availableAbilities.addAbility(IToolAreaAbility.EXPLOSION, lvl);
		} else if (ability instanceof ToolAbility.SilkAbility) {
			availableAbilities.addAbility(IToolHarvestAbility.SILK, 0);
		} else if (ability instanceof ToolAbility.SmelterAbility) {
			availableAbilities.addAbility(IToolHarvestAbility.SMELTER, 0);
		} else if (ability instanceof ToolAbility.ShredderAbility) {
			availableAbilities.addAbility(IToolHarvestAbility.SHREDDER, 0);
		} else if (ability instanceof ToolAbility.CentrifugeAbility) {
			availableAbilities.addAbility(IToolHarvestAbility.CENTRIFUGE, 0);
		} else if (ability instanceof ToolAbility.CrystallizerAbility) {
			availableAbilities.addAbility(IToolHarvestAbility.CRYSTALLIZER, 0);
		} else if (ability instanceof ToolAbility.MercuryAbility) {
			availableAbilities.addAbility(IToolHarvestAbility.MERCURY, 0);
		}
		return this;
	}

	public ItemToolAbility addHitAbility(WeaponAbility ability) {
		this.hitAbility.add(ability);
		return this;
	}

	public ItemToolAbility setRarity(EnumRarity rarity) {
		this.rarity = rarity;
		return this;
	}

	public ItemToolAbility setDepthRockBreaker() {
		this.rockBreaker = true;
		return this;
	}

	public AvailableAbilities getAvailableAbilities() {
		return availableAbilities;
	}

	public static class Configuration {
		public List<ToolPreset> presets;
		public int currentPreset;

		public Configuration() { this.presets = null; this.currentPreset = 0; }
		public Configuration(List<ToolPreset> presets, int currentPreset) { this.presets = presets; this.currentPreset = currentPreset; }

		public void writeToNBT(NBTTagCompound nbt) {
			nbt.setInteger("ability", currentPreset);
			NBTTagList nbtPresets = new NBTTagList();
			for (ToolPreset preset : presets) {
				NBTTagCompound nbtPreset = new NBTTagCompound();
				preset.writeToNBT(nbtPreset);
				nbtPresets.appendTag(nbtPreset);
			}
			nbt.setTag("abilityPresets", nbtPresets);
		}

		public void readFromNBT(NBTTagCompound nbt) {
			currentPreset = nbt.getInteger("ability");
			NBTTagList nbtPresets = nbt.getTagList("abilityPresets", 10);
			int numPresets = Math.min(nbtPresets.tagCount(), 99);
			presets = new ArrayList<>(numPresets);
			for (int i = 0; i < numPresets; i++) {
				ToolPreset preset = new ToolPreset();
				preset.readFromNBT(nbtPresets.getCompoundTagAt(i));
				presets.add(preset);
			}
			currentPreset = Math.max(0, Math.min(currentPreset, presets.size() - 1));
		}

		public void reset(AvailableAbilities availableAbilities) {
			currentPreset = 0;
			presets = new ArrayList<>(availableAbilities.size());
			presets.add(new ToolPreset());
			availableAbilities.getToolAreaAbilities().forEach((ability, level) -> {
				if (ability == IToolAreaAbility.NONE) return;
				presets.add(new ToolPreset(ability, level, IToolHarvestAbility.NONE, 0));
			});
			availableAbilities.getToolHarvestAbilities().forEach((ability, level) -> {
				if (ability == IToolHarvestAbility.NONE) return;
				presets.add(new ToolPreset(IToolAreaAbility.NONE, 0, ability, level));
			});
			presets.sort(Comparator.comparing((ToolPreset p) -> p.harvestAbility)
					.thenComparingInt(p -> p.harvestAbilityLevel)
					.thenComparing(p -> p.areaAbility)
					.thenComparingInt(p -> p.areaAbilityLevel));
		}

		public void restrictTo(AvailableAbilities availableAbilities) {
			for (ToolPreset preset : presets) preset.restrictTo(availableAbilities);
		}

		public ToolPreset getActivePreset() { return presets.get(currentPreset); }
	}

	public Configuration getConfiguration(ItemStack stack) {
		Configuration config = new Configuration();
		if (stack == null || !stack.hasTagCompound() || !stack.getTagCompound().hasKey("ability") || !stack.getTagCompound().hasKey("abilityPresets")) {
			config.reset(availableAbilities);
			return config;
		}
		config.readFromNBT(stack.getTagCompound());
		config.restrictTo(availableAbilities);
		return config;
	}

	public void setConfiguration(ItemStack stack, Configuration config) {
		if (stack == null) return;
		if (!stack.hasTagCompound()) stack.setTagCompound(new NBTTagCompound());
		config.writeToNBT(stack.getTagCompound());
	}

	@Override
	public void onUpdate(ItemStack stack, World world, Entity entity, int slot, boolean isSelected) {
		super.onUpdate(stack, world, entity, slot, isSelected);

		if (!world.isRemote || !isSelected || !(entity instanceof EntityPlayer)) return;

		handleAltKeyClient((EntityPlayer) entity);
	}

	@SideOnly(Side.CLIENT)
	private void handleAltKeyClient(EntityPlayer player) {
		Minecraft mc = Minecraft.getMinecraft();

		if (mc.currentScreen != null) {
			wasAltDown = false;
			return;
		}

		boolean altDown = HbmKeybinds.abilityAltKey.isKeyDown();

		if (altDown && !wasAltDown && this.availableAbilities.hasAnyRealAbility()) {
			player.openGui(MainRegistry.instance, ModItems.guiID_item_tool_ability, player.world, 0, 0, 0);
		}

		wasAltDown = altDown;
	}

	@Override
	public EnumActionResult onItemUse(EntityPlayer player, World worldIn, BlockPos pos, EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ) {
		if (hand == EnumHand.OFF_HAND && player.getHeldItemMainhand().getItem() instanceof ItemToolAbility) {
			return EnumActionResult.PASS;
		}

		EnumHand otherHand = hand == EnumHand.MAIN_HAND ? EnumHand.OFF_HAND : EnumHand.MAIN_HAND;
		if (isOffHandInteractive(player.getHeldItem(otherHand))) {
			return EnumActionResult.PASS;
		}

		ItemStack stack = player.getHeldItem(hand);
		if (stack.isEmpty() || !canOperate(stack)) return EnumActionResult.PASS;

		if (player.isSneaking()) {
			switchMode(player, stack);
			return EnumActionResult.SUCCESS;
		}

		if (worldIn.isRemote) return EnumActionResult.PASS;

		TileEntity te = worldIn.getTileEntity(pos);
		Block block = worldIn.getBlockState(pos).getBlock();
		if (te != null || block instanceof IInteractionObject) {
			return EnumActionResult.PASS;
		}

		switchMode(player, stack);
		return EnumActionResult.SUCCESS;
	}

	@Override
	public ActionResult<ItemStack> onItemRightClick(World world, EntityPlayer player, EnumHand hand) {
		if (hand == EnumHand.OFF_HAND && player.getHeldItemMainhand().getItem() instanceof ItemToolAbility) {
			return super.onItemRightClick(world, player, hand);
		}

		EnumHand otherHand = hand == EnumHand.MAIN_HAND ? EnumHand.OFF_HAND : EnumHand.MAIN_HAND;
		if (isOffHandInteractive(player.getHeldItem(otherHand))) {
			return super.onItemRightClick(world, player, hand);
		}

		ItemStack stack = player.getHeldItem(hand);
		if (stack.isEmpty() || !canOperate(stack)) return super.onItemRightClick(world, player, hand);

		if (player.isSneaking()) {
			switchMode(player, stack);
			return ActionResult.newResult(EnumActionResult.SUCCESS, stack);
		}

		if (ItemBlockStorageCrate.isContainer(player.getHeldItemMainhand()) || ItemBlockStorageCrate.isContainer(player.getHeldItemOffhand())) {
			return super.onItemRightClick(world, player, hand);
		}

		if (world.isRemote) return super.onItemRightClick(world, player, hand);

		switchMode(player, stack);
		return ActionResult.newResult(EnumActionResult.SUCCESS, stack);
	}

	private void switchMode(EntityPlayer player, ItemStack stack) {
		Configuration config = getConfiguration(stack);

		if (player.isSneaking()) {
			config.currentPreset = 0;
		} else {
			if (config.presets.size() < 2) return;
			config.currentPreset = (config.currentPreset + 1) % config.presets.size();
		}

		setConfiguration(stack, config);
		PacketDispatcher.wrapper.sendToServer(new NBTItemControlPacket(stack.getTagCompound()));

		ToolPreset preset = config.getActivePreset();
		String msg = preset.isNone() ?
				"[§6" + I18nUtil.resolveKey("chat.abildisabled") + "§r]" :
				"[§e" + I18nUtil.resolveKey("chat.abilenabled") + "§r] " + preset.getMessage().getFormattedText();

		MainRegistry.proxy.displayTooltipLegacy(msg, 11);
		player.world.playSound(null, player.posX, player.posY, player.posZ,
				SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, SoundCategory.PLAYERS,
				0.25F, preset.isNone() ? 0.75F : 1.25F);
	}

	private boolean isOffHandInteractive(ItemStack offhand) {
		if (offhand.isEmpty()) return false;
		if (ItemBlockStorageCrate.isContainer(offhand)) return true;
		return offhand.getItemUseAction() != EnumAction.NONE || offhand.getItem() instanceof ItemBlock;
	}

	@Override
	public boolean onBlockStartBreak(ItemStack stack, BlockPos pos, EntityPlayer player) {
		World world = player.world;
		if (world.isRemote || !canOperate(stack)) return false;
		if (isForbiddenBlock(world.getBlockState(pos).getBlock())) return false;

		Configuration config = getConfiguration(stack);
		ToolPreset preset = config.getActivePreset();

		dropX = pos.getX();
		dropY = pos.getY();
		dropZ = pos.getZ();

		preset.harvestAbility.preHarvestAll(preset.harvestAbilityLevel, world, player);
		boolean skipRef = preset.areaAbility.onDig(preset.areaAbilityLevel, world, pos, player, this);
		if (!skipRef) breakExtraBlock(world, pos.getX(), pos.getY(), pos.getZ(), player, pos.getX(), pos.getY(), pos.getZ());
		preset.harvestAbility.postHarvestAll(preset.harvestAbilityLevel, world, player);
		return true;
	}

	@Override
	public float getDestroySpeed(ItemStack stack, IBlockState state) {
		if (!canOperate(stack)) return 1;
		if (toolType == null) return super.getDestroySpeed(stack, state);
		if (toolType.blocks.contains(state.getBlock()) || toolType.materials.contains(state.getMaterial())) return this.efficiency;
		return super.getDestroySpeed(stack, state);
	}

	@Override
	public boolean canHarvestBlock(IBlockState state, ItemStack stack) {
		if (!canOperate(stack)) return false;
		if (isForbiddenBlock(state.getBlock())) return false;
		Configuration config = getConfiguration(stack);
		if (config != null && config.getActivePreset().harvestAbility == IToolHarvestAbility.SILK) return true;
		return getDestroySpeed(stack, state) > 1;
	}

	public boolean canShearBlock(Block block, ItemStack stack, World world, int x, int y, int z) { return false; }

	public static boolean isForbiddenBlock(Block b) {
		return b == Blocks.BARRIER || b == Blocks.BEDROCK || b == Blocks.COMMAND_BLOCK
				|| b == Blocks.CHAIN_COMMAND_BLOCK || b == Blocks.REPEATING_COMMAND_BLOCK
				|| b == ModBlocks.ore_bedrock_oil || b instanceof BlockBedrockOre || b instanceof BlockBedrockOreTE;
	}

	@Override
	public boolean hitEntity(ItemStack stack, EntityLivingBase target, EntityLivingBase attacker) {
		if (!attacker.world.isRemote && !this.hitAbility.isEmpty() && attacker instanceof EntityPlayer && canOperate(stack)) {
			for (WeaponAbility ability : this.hitAbility) {
				ability.onHit(attacker.world, (EntityPlayer) attacker, target, this);
			}
		}
		stack.damageItem(2, attacker);
		return true;
	}

	@Override
	public Multimap<String, AttributeModifier> getItemAttributeModifiers(EntityEquipmentSlot slot) {
		Multimap<String, AttributeModifier> map = HashMultimap.create();
		if (slot == EntityEquipmentSlot.MAINHAND) {
			map.put(SharedMonsterAttributes.MOVEMENT_SPEED.getName(), new AttributeModifier(UUID.fromString("91AEAA56-376B-4498-935B-2F7F68070635"), "Tool modifier", movement, 1));
			map.put(SharedMonsterAttributes.ATTACK_DAMAGE.getName(), new AttributeModifier(ATTACK_DAMAGE_MODIFIER, "Tool modifier", (double) this.damage, 0));
		}
		return map;
	}

	public void breakExtraBlock(World world, int x, int y, int z, EntityPlayer player, int refX, int refY, int refZ) {
		breakExtraBlock(world, x, y, z, player, refX, refY, refZ, EnumHand.MAIN_HAND);
	}

	public void breakExtraBlock(World world, int x, int y, int z, EntityPlayer playerEntity, int refX, int refY, int refZ, EnumHand hand) {
		BlockPos pos = new BlockPos(x, y, z);
		if (world.isAirBlock(pos)) return;
		if (!(playerEntity instanceof EntityPlayerMP)) return;

		EntityPlayerMP player = (EntityPlayerMP) playerEntity;
		ItemStack stack = player.getHeldItem(hand);
		if (stack.isEmpty()) return;

		IBlockState state = world.getBlockState(pos);
		Block block = state.getBlock();
		int meta = block.getMetaFromState(state);

		if (!(canHarvestBlock(state, stack) || canShearBlock(block, stack, world, x, y, z))
				|| (state.getBlockHardness(world, pos) == -1.0F && state.getPlayerRelativeBlockHardness(player, world, pos) == 0.0F))
			return;

		BlockPos refPos = new BlockPos(refX, refY, refZ);
		IBlockState refState = world.getBlockState(refPos);

		float refStrength = refState.getPlayerRelativeBlockHardness(player, world, refPos);
		float strength = state.getPlayerRelativeBlockHardness(player, world, pos);

		if (!ForgeHooks.canHarvestBlock(state.getBlock(), player, world, pos) || strength <= 0.0F
				|| refStrength / strength > 10f || refState.getPlayerRelativeBlockHardness(player, world, refPos) < 0)
			return;

		int exp = ForgeHooks.onBlockBreakEvent(world, player.interactionManager.getGameType(), player, pos);
		if (exp == -1) return;

		Configuration config = getConfiguration(stack);
		ToolPreset preset = config.getActivePreset();

		preset.harvestAbility.onHarvestBlock(preset.harvestAbilityLevel, world, x, y, z, player, block, meta);
	}

	public static void standardDigPost(World world, int x, int y, int z, EntityPlayerMP player) {
		BlockPos pos = new BlockPos(x, y, z);
		IBlockState state = world.getBlockState(pos);
		Block block = state.getBlock();
		world.playEvent(player, 2001, pos, Block.getStateId(state));
		if (player.capabilities.isCreativeMode) {
			removeBlock(world, x, y, z, false, player);
			player.connection.sendPacket(new SPacketBlockChange(world, pos));
		} else {
			ItemStack itemstack = player.getHeldItemMainhand();
			boolean canHarvest = ForgeHooks.canHarvestBlock(block, player, world, pos);
			boolean removedByPlayer = removeBlock(world, x, y, z, canHarvest, player);
			if (!itemstack.isEmpty()) {
				itemstack.onBlockDestroyed(world, state, pos, player);
				if (itemstack.getCount() == 0) player.setHeldItem(EnumHand.MAIN_HAND, ItemStack.EMPTY);
			}
			if (removedByPlayer && canHarvest) {
				block.harvestBlock(world, player, pos, state, world.getTileEntity(pos), itemstack);
			}
		}
	}

	public static boolean removeBlock(World world, int x, int y, int z, boolean canHarvest, EntityPlayerMP player) {
		BlockPos pos = new BlockPos(x, y, z);
		IBlockState state = world.getBlockState(pos);
		Block block = state.getBlock();
		block.onBlockHarvested(world, pos, state, player);
		boolean flag = block.removedByPlayer(state, world, pos, player, canHarvest);
		if (flag) block.onPlayerDestroy(world, pos, state);
		return flag;
	}

	@SideOnly(Side.CLIENT)
	@Override
	public boolean hasEffect(ItemStack stack) {
		Configuration config = getConfiguration(stack);
		return config != null && !config.getActivePreset().isNone() || super.hasEffect(stack);
	}

	@Override
	@SideOnly(Side.CLIENT)
	public void addInformation(ItemStack stack, World worldIn, List<String> list, ITooltipFlag flagIn) {
		Configuration config = getConfiguration(stack);
		ToolPreset activePreset = config.getActivePreset();
		availableAbilities.addInformation(list, activePreset);
		if (this.rockBreaker)
			list.add("§5[" + I18nUtil.resolveKey("trait.unmineable") + "]§d " + I18nUtil.resolveKey("tool.ability.canmine"));
	}

	protected boolean canOperate(ItemStack stack) { return true; }

	@Override
	public boolean canBreakRock(World world, EntityPlayer player, ItemStack tool, IBlockState block, BlockPos pos) {
		return canOperate(tool) && this.rockBreaker;
	}

	public void renderHUD(RenderGameOverlayEvent.Pre event, RenderGameOverlayEvent.ElementType type, EntityPlayer player, ItemStack stack, EnumHand hand) {
		if (type != RenderGameOverlayEvent.ElementType.CROSSHAIRS) return;

		Configuration config = getConfiguration(stack);
		ToolPreset preset = config.getActivePreset();
		Map.Entry<Integer, Integer> uv = abilityGui.get(preset.areaAbility);

		if (uv == null) return;

		GuiIngame gui = Minecraft.getMinecraft().ingameGUI;
		int size = 16;

		GlStateManager.pushMatrix();
		Minecraft.getMinecraft().renderEngine.bindTexture(GUIScreenToolAbility.texture);
		GlStateManager.enableBlend();
		GlStateManager.disableLighting();
		GlStateManager.disableDepth();
		GlStateManager.depthMask(false);
		GlStateManager.color(1F, 1F, 1F, 1F);

		OpenGlHelper.glBlendFunc(GL11.GL_ONE_MINUS_DST_COLOR, GL11.GL_ONE_MINUS_SRC_COLOR, 1, 0);
		gui.drawTexturedModalRect(
				event.getResolution().getScaledWidth() / 2 - size - 8,
				event.getResolution().getScaledHeight() / 2 + 8,
				uv.getKey(), uv.getValue(), size, size);
		OpenGlHelper.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, 1, 0);
		GlStateManager.disableBlend();
		GlStateManager.enableDepth();
		GlStateManager.depthMask(true);
		GlStateManager.color(1F, 1F, 1F, 1F);
		GlStateManager.popMatrix();
		Minecraft.getMinecraft().renderEngine.bindTexture(Gui.ICONS);
	}

	private int findLevelIndexFloat(float[] values, float target) {
		for (int i = 0; i < values.length; i++) {
			if (Math.abs(values[i] - target) < 0.01F) return i;
		}
		return values.length - 1;
	}
}