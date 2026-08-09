package com.hbm.inventory.gui;

import com.hbm.util.I18nUtil;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.lwjgl.opengl.GL11;

import com.hbm.inventory.AssemblerRecipes;
import com.hbm.inventory.RecipesCommon.AStack;
import com.hbm.inventory.RecipesCommon.ComparableStack;
import com.hbm.inventory.RecipesCommon.OreDictStack;
import com.hbm.inventory.container.ContainerMachineAssembler;
import com.hbm.items.ModItems;
import com.hbm.items.machine.ItemAssemblyTemplate;
import com.hbm.lib.RefStrings;
import com.hbm.tileentity.machine.TileEntityMachineAssembler;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.util.NonNullList;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.oredict.OreDictionary;

public class GUIMachineAssembler extends GuiInfoContainer {

	private static ResourceLocation texture = new ResourceLocation(RefStrings.MODID + ":textures/gui/gui_assembler.png");
	private TileEntityMachineAssembler assembler;

	public GUIMachineAssembler(InventoryPlayer invPlayer, TileEntityMachineAssembler tedf) {
		super(new ContainerMachineAssembler(invPlayer, tedf));
		assembler = tedf;

		this.xSize = 176;
		this.ySize = 222;
	}

	@Override
	public void drawScreen(int mouseX, int mouseY, float f) {
		super.drawScreen(mouseX, mouseY, f);
		this.renderHoveredToolTip(mouseX, mouseY);
		this.drawElectricityInfo(this, mouseX, mouseY, guiLeft + 116, guiTop + 70 - 52, 16, 52, assembler.power, TileEntityMachineAssembler.maxPower);

		if(assembler.inventory.getStackInSlot(4).getItem() == Items.AIR || assembler.inventory.getStackInSlot(4).getItem()!= ModItems.assembly_template) {
			String[] text1 = I18nUtil.resolveKeyArray("desc.guimachassembler");
			this.drawCustomInfoStat(mouseX, mouseY, guiLeft - 16, guiTop + 36, 16, 16, guiLeft - 8, guiTop + 36 + 16, text1);
		}

		String[] text = I18nUtil.resolveKeyArray("desc.guiacceptupgrades1");
		this.drawCustomInfoStat(mouseX, mouseY, guiLeft + 141, guiTop + 40, 8, 8, guiLeft + 141, guiTop + 40 + 16, text);
	}

	@Override
	protected void mouseClicked(int x, int y, int button) throws IOException {
		super.mouseClicked(x, y, button);
		if(this.checkClick(x, y, 79, 52, 18, 18)) GUIScreenAssemblerTemplate.openSelector(assembler, this, 4);
	}

	@Override
	protected void drawGuiContainerForegroundLayer( int i, int j) {
		String name = this.assembler.hasCustomInventoryName() ? this.assembler.getInventoryName() : I18n.format(this.assembler.getInventoryName());
		this.fontRenderer.drawString(name, this.xSize / 2 - this.fontRenderer.getStringWidth(name) / 2, 6, 4210752);
		this.fontRenderer.drawString(I18n.format("container.inventory"), 8, this.ySize - 96 + 2, 4210752);
	}

	@Override
	protected void drawGuiContainerBackgroundLayer(float p_146976_1_, int p_146976_2_, int p_146976_3_) {
		this.drawDefaultBackground();
		GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
		Minecraft.getMinecraft().getTextureManager().bindTexture(texture);
		drawTexturedModalRect(guiLeft, guiTop, 0, 0, xSize, ySize);

		int i = (int)assembler.getPowerScaled(52);
		drawTexturedModalRect(guiLeft + 116, guiTop + 70 - i, 176, 52 - i, 16, i);
		if(assembler.isProgressing){
			int j = assembler.getProgressScaled(83);
			drawTexturedModalRect(guiLeft + 45, guiTop + 82, 2, 222, j, 32);
		} else {
			drawTexturedModalRect(guiLeft + 45, guiTop + 82, 2, 222, 0, 32);
		}

		if(assembler.inventory.getStackInSlot(4).getItem() == Items.AIR || assembler.inventory.getStackInSlot(4).getItem()!= ModItems.assembly_template) {
			this.drawInfoPanel(guiLeft - 16, guiTop + 36, 16, 16, 6);
		}

		this.drawInfoPanel(guiLeft + 141, guiTop + 40, 8, 8, 8);

		ItemStack templateStack = assembler.inventory.getStackInSlot(4);
		if (!templateStack.isEmpty() && templateStack.getItem() instanceof ItemAssemblyTemplate) {
			int recipeIdx = ItemAssemblyTemplate.getRecipeIndex(templateStack);
			if (recipeIdx >= 0 && recipeIdx < AssemblerRecipes.recipeList.size()) {
				ComparableStack out = AssemblerRecipes.recipeList.get(recipeIdx);
				Object[] in = AssemblerRecipes.recipes.get(out);

				if (in != null) {
					List<IngredientStatus> statuses = new ArrayList<IngredientStatus>();
					for (Object o : in) {
						if (o instanceof AStack) {
							AStack req = (AStack) o;
							int needed = req.count();
							int have = 0;
							AStack sing = req.copy();
							sing.singulize();
							for (int k = 6; k <= 17; k++) {
								ItemStack slotStack = assembler.inventory.getStackInSlot(k);
								if (!slotStack.isEmpty()) {
									ItemStack compare = slotStack.copy();
									compare.setCount(1);
									if (sing.isApplicable(compare)) {
										have += slotStack.getCount();
									}
								}
							}
							statuses.add(new IngredientStatus(req, needed, have));
						}
					}

					for (int k = 6; k <= 17; k++) {
						int col = (k - 6) % 2;
						int row = (k - 6) / 2;
						int slotX = 8 + col * 18;
						int slotY = 18 + row * 18;

						ItemStack slotStack = assembler.inventory.getStackInSlot(k);

						if (!slotStack.isEmpty()) {
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
					for (int k = 6; k <= 17; k++) {
						if (assembler.inventory.getStackInSlot(k).isEmpty()) {
							emptySlots.add(k);
						}
					}

					int emptySlotIndex = 0;
					for (IngredientStatus status : statuses) {
						if (status.have == 0 && emptySlotIndex < emptySlots.size()) {
							int k = emptySlots.get(emptySlotIndex);
							emptySlotIndex++;

							int col = (k - 6) % 2;
							int row = (k - 6) / 2;
							int slotX = 8 + col * 18;
							int slotY = 18 + row * 18;

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