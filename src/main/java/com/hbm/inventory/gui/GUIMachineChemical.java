package com.hbm.inventory.gui;

import com.hbm.util.I18nUtil;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.lwjgl.opengl.GL11;

import com.hbm.forgefluid.FFUtils;
import com.hbm.inventory.ChemplantRecipes;
import com.hbm.inventory.RecipesCommon.AStack;
import com.hbm.inventory.RecipesCommon.ComparableStack;
import com.hbm.inventory.RecipesCommon.OreDictStack;
import com.hbm.inventory.container.ContainerMachineChemical;
import com.hbm.items.ModItems;
import com.hbm.items.machine.ItemChemistryTemplate;
import com.hbm.lib.RefStrings;
import com.hbm.tileentity.machine.TileEntityMachineChemical;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.NonNullList;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.oredict.OreDictionary;

public class GUIMachineChemical extends GuiInfoContainer {

	private static ResourceLocation texture = new ResourceLocation(RefStrings.MODID + ":textures/gui/gui_chemplant.png");
	private TileEntityMachineChemical chemplant;

	public GUIMachineChemical(InventoryPlayer invPlayer, TileEntityMachineChemical tedf) {
		super(new ContainerMachineChemical(invPlayer, tedf));
		chemplant = tedf;

		this.xSize = 176;
		this.ySize = 222;
	}

	@Override
	public void drawScreen(int mouseX, int mouseY, float f) {
		super.drawScreen(mouseX, mouseY, f);
		this.renderHoveredToolTip(mouseX, mouseY);

		FFUtils.renderTankInfo(this, mouseX, mouseY, guiLeft + 8, guiTop + 52 - 34, 16, 34, chemplant.tanks[0], chemplant.tankTypes[0]);
		FFUtils.renderTankInfo(this, mouseX, mouseY, guiLeft + 26, guiTop + 52 - 34, 16, 34, chemplant.tanks[1], chemplant.tankTypes[1]);
		FFUtils.renderTankInfo(this, mouseX, mouseY, guiLeft + 134, guiTop + 52 - 34, 16, 34, chemplant.tanks[2], chemplant.tankTypes[2]);
		FFUtils.renderTankInfo(this, mouseX, mouseY, guiLeft + 152, guiTop + 52 - 34, 16, 34, chemplant.tanks[3], chemplant.tankTypes[3]);

		this.drawElectricityInfo(this, mouseX, mouseY, guiLeft + 44, guiTop + 70 - 52, 16, 52, chemplant.power, TileEntityMachineChemical.maxPower);

		if(chemplant.getStackInSlot(4) == null || chemplant.getStackInSlot(4).isEmpty() || chemplant.getStackInSlot(4).getItem()!= ModItems.chemistry_template) {
			String[] text = new String[] { "Error: This machine requires a chemistry template!" };
			this.drawCustomInfoStat(mouseX, mouseY, guiLeft - 16, guiTop + 36, 16, 16, guiLeft - 8, guiTop + 36 + 16, text);
		}

		String[] text = I18nUtil.resolveKeyArray("desc.guiacceptupgrades1");
		this.drawCustomInfoStat(mouseX, mouseY, guiLeft + 105, guiTop + 40, 8, 8, guiLeft + 105, guiTop + 40 + 16, text);
	}

	@Override
	protected void mouseClicked(int x, int y, int button) throws IOException {
		super.mouseClicked(x, y, button);
		if(this.checkClick(x, y, 79, 53, 18, 18)) GUIScreenAssemblerTemplate.openSelector(chemplant, this, 4);
	}

	@Override
	protected void drawGuiContainerForegroundLayer( int i, int j) {
		String name = this.chemplant.hasCustomInventoryName() ? this.chemplant.getInventoryName() : I18n.format(this.chemplant.getInventoryName());
		this.fontRenderer.drawString(name, this.xSize / 2 - this.fontRenderer.getStringWidth(name) / 2, 6, 4210752);
		this.fontRenderer.drawString(I18n.format("container.inventory"), 8, this.ySize - 96 + 2, 4210752);
	}

	@Override
	protected void drawGuiContainerBackgroundLayer(float p_146976_1_, int p_146976_2_, int p_146976_3_) {
		this.drawDefaultBackground();
		GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
		Minecraft.getMinecraft().getTextureManager().bindTexture(texture);
		drawTexturedModalRect(guiLeft, guiTop, 0, 0, xSize, ySize);

		int i = (int)chemplant.getPowerScaled(52);
		drawTexturedModalRect(guiLeft + 44, guiTop + 70 - i, 176, 52 - i, 16, i);

		if(chemplant.isProgressing){
			int j = chemplant.getProgressScaled(90);
			drawTexturedModalRect(guiLeft + 43, guiTop + 89, 0, 222, j, 18);
		} else {
			drawTexturedModalRect(guiLeft + 43, guiTop + 89, 0, 222, 0, 18);
		}

		this.drawInfoPanel(guiLeft + 105, guiTop + 40, 8, 8, 8);

		if(chemplant.getStackInSlot(4) == null || chemplant.getStackInSlot(4).getItem()!= ModItems.chemistry_template) {
			this.drawInfoPanel(guiLeft - 16, guiTop + 36, 16, 16, 6);
		}

		Minecraft.getMinecraft().getTextureManager().bindTexture(TextureMap.LOCATION_BLOCKS_TEXTURE);

		FFUtils.drawLiquid(chemplant.tanks[0], guiLeft, guiTop, zLevel, 16, 34, 8, 80);
		FFUtils.drawLiquid(chemplant.tanks[1], guiLeft, guiTop, zLevel, 16, 34, 26, 80);
		FFUtils.drawLiquid(chemplant.tanks[2], guiLeft, guiTop, zLevel, 16, 34, 134, 80);
		FFUtils.drawLiquid(chemplant.tanks[3], guiLeft, guiTop, zLevel, 16, 34, 152, 80);

		ItemStack templateStack = chemplant.getStackInSlot(4);
		if (templateStack != null && !templateStack.isEmpty() && templateStack.getItem() instanceof ItemChemistryTemplate) {
			List<AStack> itemInputs = ChemplantRecipes.getChemInputFromTempate(templateStack);

			if (itemInputs != null) {
				List<IngredientStatus> statuses = new ArrayList<>();
				for (AStack req : itemInputs) {
					int needed = req.count();
					int have = 0;
					AStack sing = req.copy();
					sing.singulize();
					for (int k = 13; k <= 16; k++) {
						ItemStack slotStack = chemplant.getStackInSlot(k);
						if (slotStack != null && !slotStack.isEmpty()) {
							ItemStack compare = slotStack.copy();
							compare.setCount(1);
							if (sing.isApplicable(compare)) {
								have += slotStack.getCount();
							}
						}
					}
					statuses.add(new IngredientStatus(req, needed, have));
				}

				for (int k = 13; k <= 16; k++) {
					int col = (k - 13) % 2;
					int row = (k - 13) / 2;
					int slotX = 8 + col * 18;
					int slotY = 90 + row * 18;

					ItemStack slotStack = chemplant.getStackInSlot(k);

					if (slotStack != null && !slotStack.isEmpty()) {
						IngredientStatus matchedStatus = null;
						for (IngredientStatus status : statuses) {
							AStack s = status.req.copy();
							s.singulize();
							ItemStack compare = slotStack.copy();
							compare.setCount(1);
							if (s.isApplicable(compare)) {
								matchedStatus = status;
								break;
							}
						}

						if (matchedStatus != null) {
							GlStateManager.disableLighting();
							GlStateManager.disableDepth();
							drawRect(guiLeft + slotX, guiTop + slotY, guiLeft + slotX + 16, guiTop + slotY + 16, matchedStatus.have >= matchedStatus.needed ? 0x7F00FF00 : 0x7FFF0000);
							GlStateManager.enableDepth();
							GlStateManager.enableLighting();
						}
					}
				}

				List<Integer> emptySlots = new ArrayList<>();
				for (int k = 13; k <= 16; k++) {
					ItemStack s = chemplant.getStackInSlot(k);
					if (s == null || s.isEmpty()) emptySlots.add(k);
				}

				int emptySlotIndex = 0;
				for (IngredientStatus status : statuses) {
					if (status.have == 0 && emptySlotIndex < emptySlots.size()) {
						int k = emptySlots.get(emptySlotIndex);
						emptySlotIndex++;

						int col = (k - 13) % 2;
						int row = (k - 13) / 2;
						int slotX = 8 + col * 18;
						int slotY = 90 + row * 18;

						ItemStack ghostStack = status.getDisplayStack();
						if (!ghostStack.isEmpty()) {
							ghostStack.setCount(1);

							RenderHelper.enableGUIStandardItemLighting();
							GlStateManager.enableDepth();
							GlStateManager.enableBlend();
							GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
							GlStateManager.color(1.0F, 1.0F, 1.0F, 0.5F);
							Minecraft.getMinecraft().getRenderItem().renderItemAndEffectIntoGUI(ghostStack, guiLeft + slotX, guiTop + slotY);
							GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
							GlStateManager.disableBlend();
							RenderHelper.disableStandardItemLighting();
						}

						GlStateManager.disableLighting();
						GlStateManager.disableDepth();
						drawRect(guiLeft + slotX, guiTop + slotY, guiLeft + slotX + 16, guiTop + slotY + 16, 0x7FFF0000);

						if (status.needed > 1) {
							String countStr = String.valueOf(status.needed);
							fontRenderer.drawStringWithShadow(countStr, guiLeft + slotX + 17 - fontRenderer.getStringWidth(countStr), guiTop + slotY + 9, 0xFFFFFF);
						}
						GlStateManager.enableDepth();
						GlStateManager.enableLighting();
					}
				}
			}
		}
	}

	private static class IngredientStatus {
		AStack req;
		int needed;
		int have;

		public IngredientStatus(AStack req, int needed, int have) {
			this.req = req;
			this.needed = needed;
			this.have = have;
		}

		public ItemStack getDisplayStack() {
			if (req instanceof OreDictStack) {
				OreDictStack ods = (OreDictStack) req;
				NonNullList<ItemStack> ores = OreDictionary.getOres(ods.name);
				if (!ores.isEmpty()) {
					return ores.get((int) (Math.abs(System.currentTimeMillis() / 1000) % ores.size())).copy();
				}
				return ItemStack.EMPTY;
			} else if (req instanceof ComparableStack) {
				ItemStack stack = ((ComparableStack) req).toStack();
				return stack != null ? stack : ItemStack.EMPTY;
			}
			return ItemStack.EMPTY;
		}
	}
}