package com.hbm.inventory.gui;

import com.hbm.inventory.container.ContainerMachineAssembly;
import com.hbm.tileentity.machine.TileEntityMachineAssembly;
import com.hbm.util.I18nUtil;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.lwjgl.opengl.GL11;

import com.hbm.items.ModItems;
import com.hbm.lib.RefStrings;
import com.hbm.inventory.AssemblerRecipes;
import com.hbm.inventory.RecipesCommon.AStack;
import com.hbm.inventory.RecipesCommon.ComparableStack;
import com.hbm.inventory.RecipesCommon.OreDictStack;
import com.hbm.items.machine.ItemAssemblyTemplate;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.init.Items;
import net.minecraft.inventory.InventoryBasic;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.util.NonNullList;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.oredict.OreDictionary;

public class GUIMachineAssembly extends GuiInfoContainer {

    private static ResourceLocation texture = new ResourceLocation(RefStrings.MODID + ":textures/gui/gui_assembler.png");
    private TileEntityMachineAssembly assembly;
    private List<GhostItem> currentGhosts = new ArrayList<>();
    private List<GhostSlot> ghostSlots = new ArrayList<>();

    public GUIMachineAssembly(InventoryPlayer invPlayer, TileEntityMachineAssembly tedf) {
        super(new ContainerMachineAssembly(invPlayer, tedf));
        assembly = tedf;

        this.xSize = 176;
        this.ySize = 222;

        for (int i = 0; i < 12; i++) {
            int col = i % 2;
            int row = i / 2;
            int slotX = 8 + col * 18;
            int slotY = 18 + row * 18;
            GhostSlot ghostSlot = new GhostSlot(slotX, slotY);
            ghostSlot.slotNumber = this.inventorySlots.inventorySlots.size();
            this.inventorySlots.inventorySlots.add(ghostSlot);
            this.inventorySlots.inventoryItemStacks.add(ItemStack.EMPTY);
            ghostSlots.add(ghostSlot);
        }

        GhostSlot outputGhost = new GhostSlot(130, 86);
        outputGhost.slotNumber = this.inventorySlots.inventorySlots.size();
        this.inventorySlots.inventorySlots.add(outputGhost);
        this.inventorySlots.inventoryItemStacks.add(ItemStack.EMPTY);
        ghostSlots.add(outputGhost);
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float f) {
        super.drawScreen(mouseX, mouseY, f);
        this.renderHoveredToolTip(mouseX, mouseY);
        this.drawElectricityInfo(this, mouseX, mouseY, guiLeft + 116, guiTop + 70 - 52, 16, 52, assembly.power, TileEntityMachineAssembly.maxPower);

        if(assembly.inventory.getStackInSlot(4).getItem() == Items.AIR || assembly.inventory.getStackInSlot(4).getItem()!= ModItems.assembly_template) {
            String[] text1 = I18nUtil.resolveKeyArray("desc.guimachassembler");
            this.drawCustomInfoStat(mouseX, mouseY, guiLeft - 16, guiTop + 36, 16, 16, guiLeft - 8, guiTop + 36 + 16, text1);
        }

        String[] text = I18nUtil.resolveKeyArray("desc.guiacceptupgrades1");
        this.drawCustomInfoStat(mouseX, mouseY, guiLeft + 141, guiTop + 40, 8, 8, guiLeft + 141, guiTop + 40 + 16, text);

        for (int i = 0; i < currentGhosts.size(); i++) {
            GhostItem ghost = currentGhosts.get(i);
            if (mouseX >= ghost.x && mouseX < ghost.x + ghost.width && mouseY >= ghost.y && mouseY < ghost.y + ghost.height) {
                if (i < ghostSlots.size()) {
                    this.hoveredSlot = ghostSlots.get(i);
                }
                this.renderToolTip(ghost.stack, mouseX, mouseY);
                break;
            }
        }
    }

    @Override
    protected void mouseClicked(int x, int y, int button) throws IOException {
        if (button == 0) {
            for (GhostItem ghost : currentGhosts) {
                if (x >= ghost.x && x < ghost.x + ghost.width && y >= ghost.y && y < ghost.y + ghost.height) {
                    openJeiRecipe(ghost.stack);
                    return;
                }
            }
        }
        super.mouseClicked(x, y, button);
        if(this.checkClick(x, y, 79, 52, 18, 18)) GUIScreenAssemblerTemplate.openSelector(assembly, this, 4);
    }

    private void openJeiRecipe(ItemStack stack) {
        if (stack.isEmpty()) return;
        try {
            Class<?> focusModeClass = Class.forName("mezz.jei.api.recipe.IFocus$Mode");
            Object outputMode = focusModeClass.getField("OUTPUT").get(null);
            Class<?> focusClass = Class.forName("mezz.jei.api.recipe.Focus");
            Object focus = focusClass.getConstructor(focusModeClass, Object.class).newInstance(outputMode, stack);

            Class<?> jeiRuntimeClass = Class.forName("mezz.jei.startup.JeiRuntime");
            java.lang.reflect.Method getRuntimeMethod = jeiRuntimeClass.getMethod("getRuntime");
            Object runtime = getRuntimeMethod.invoke(null);

            if (runtime != null) {
                java.lang.reflect.Method getRecipesGuiMethod = runtime.getClass().getMethod("getRecipesGui");
                Object recipesGui = getRecipesGuiMethod.invoke(runtime);
                java.lang.reflect.Method showMethod = recipesGui.getClass().getMethod("show", Class.forName("mezz.jei.api.recipe.IFocus"));
                showMethod.invoke(recipesGui, focus);
            }
        } catch (Exception e) {}
    }

    @Override
    protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY) {
        String name = this.assembly.hasCustomInventoryName() ? this.assembly.getInventoryName() : I18n.format(this.assembly.getInventoryName());
        this.fontRenderer.drawString(name, this.xSize / 2 - this.fontRenderer.getStringWidth(name) / 2, 6, 4210752);
        this.fontRenderer.drawString(I18n.format("container.inventory"), 8, this.ySize - 96 + 2, 4210752);
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(float p_146976_1_, int p_146976_2_, int p_146976_3_) {
        this.drawDefaultBackground();
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        Minecraft.getMinecraft().getTextureManager().bindTexture(texture);
        drawTexturedModalRect(guiLeft, guiTop, 0, 0, xSize, ySize);

        int i = (int) assembly.getPowerScaled(52);
        drawTexturedModalRect(guiLeft + 116, guiTop + 70 - i, 176, 52 - i, 16, i);
        if(assembly.isProgressing){
            int j = assembly.getProgressScaled(83);
            drawTexturedModalRect(guiLeft + 45, guiTop + 82, 2, 222, j, 32);
        } else {
            drawTexturedModalRect(guiLeft + 45, guiTop + 82, 2, 222, 0, 32);
        }

        if(assembly.inventory.getStackInSlot(4).getItem() == Items.AIR || assembly.inventory.getStackInSlot(4).getItem()!= ModItems.assembly_template) {
            this.drawInfoPanel(guiLeft - 16, guiTop + 36, 16, 16, 6);
        }

        this.drawInfoPanel(guiLeft + 141, guiTop + 40, 8, 8, 8);

        for (GhostSlot gs : ghostSlots) {
            gs.setGhostStack(ItemStack.EMPTY);
        }
        currentGhosts.clear();

        ItemStack templateStack = assembly.inventory.getStackInSlot(4);
        if (!templateStack.isEmpty() && templateStack.getItem() instanceof ItemAssemblyTemplate) {
            int recipeIdx = ItemAssemblyTemplate.getRecipeIndex(templateStack);
            if (recipeIdx >= 0 && recipeIdx < AssemblerRecipes.recipeList.size()) {
                ComparableStack out = AssemblerRecipes.recipeList.get(recipeIdx);
                Object[] in = AssemblerRecipes.recipes.get(out);

                if (in != null) {
                    List<IngredientStatus> statuses = new ArrayList<IngredientStatus>();
                    boolean allIngredientsMet = true;

                    for (Object o : in) {
                        if (o instanceof AStack) {
                            AStack req = (AStack) o;
                            int needed = req.count();
                            int have = 0;
                            AStack sing = req.copy();
                            sing.singulize();
                            for (int k = 6; k <= 17; k++) {
                                ItemStack slotStack = assembly.inventory.getStackInSlot(k);
                                if (!slotStack.isEmpty()) {
                                    ItemStack compare = slotStack.copy();
                                    compare.setCount(1);
                                    if (sing.isApplicable(compare)) {
                                        have += slotStack.getCount();
                                    }
                                }
                            }
                            if (have < needed) allIngredientsMet = false;
                            statuses.add(new IngredientStatus(req, needed, have));
                        }
                    }

                    for (int k = 6; k <= 17; k++) {
                        int col = (k - 6) % 2;
                        int row = (k - 6) / 2;
                        int slotX = 8 + col * 18;
                        int slotY = 18 + row * 18;

                        ItemStack slotStack = assembly.inventory.getStackInSlot(k);

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
                        if (assembly.inventory.getStackInSlot(k).isEmpty()) {
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
                                currentGhosts.add(new GhostItem(guiLeft + slotX, guiTop + slotY, 16, 16, ghostStack.copy()));

                                int ghostSlotIndex = k - 6;
                                if (ghostSlotIndex >= 0 && ghostSlotIndex < ghostSlots.size()) {
                                    ghostSlots.get(ghostSlotIndex).setGhostStack(ghostStack.copy());
                                }

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

                    ItemStack outputStack = out.toStack();
                    ItemStack outSlotStack = assembly.inventory.getStackInSlot(5);
                    if (!outputStack.isEmpty() && outSlotStack.isEmpty()) {
                        int outX = 130;
                        int outY = 86;
                        int outW = 24;
                        int outH = 24;

                        ItemStack displayOut = outputStack.copy();
                        displayOut.setCount(1);
                        currentGhosts.add(new GhostItem(guiLeft + outX, guiTop + outY, outW, outH, displayOut.copy()));

                        int outputGhostIndex = 12;
                        if (outputGhostIndex < ghostSlots.size()) {
                            ghostSlots.get(outputGhostIndex).setGhostStack(displayOut.copy());
                        }

                        RenderHelper.enableGUIStandardItemLighting();
                        GlStateManager.enableDepth();
                        GlStateManager.enableBlend();
                        GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
                        GlStateManager.color(1.0F, 1.0F, 1.0F, 0.5F);
                        Minecraft.getMinecraft().getRenderItem().renderItemAndEffectIntoGUI(displayOut, guiLeft + outX + 4, guiTop + outY + 4);
                        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
                        GlStateManager.disableBlend();
                        RenderHelper.disableStandardItemLighting();

                        GlStateManager.disableLighting();
                        GlStateManager.disableDepth();
                        int outputOverlayColor = allIngredientsMet ? 0x7F00FF00 : 0x7FFF0000;
                        drawRect(guiLeft + outX, guiTop + outY, guiLeft + outX + outW, guiTop + outY + outH, outputOverlayColor);

                        if (outputStack.getCount() > 1) {
                            String countStr = String.valueOf(outputStack.getCount());
                            fontRenderer.drawStringWithShadow(countStr, guiLeft + outX + 20 - fontRenderer.getStringWidth(countStr), guiTop + outY + 13, 0xFFFFFF);
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
                    int index = (int) (Math.abs(System.currentTimeMillis() / 1000) % ores.size());
                    for (int i = 0; i < ores.size(); i++) {
                        int checkIndex = (index + i) % ores.size();
                        ItemStack stack = ores.get(checkIndex);
                        if (!stack.isEmpty() && stack.getItem() != Items.AIR) {
                            return stack.copy();
                        }
                    }
                }
                return ItemStack.EMPTY;
            } else if (req instanceof ComparableStack) {
                ItemStack stack = ((ComparableStack) req).toStack();
                return stack != null ? stack : ItemStack.EMPTY;
            }
            return ItemStack.EMPTY;
        }
    }

    private static class GhostItem {
        int x, y, width, height;
        ItemStack stack;
        public GhostItem(int x, int y, int w, int h, ItemStack stack) {
            this.x = x; this.y = y; this.width = w; this.height = h; this.stack = stack;
        }
    }

    private static class GhostSlot extends Slot {
        private ItemStack ghostStack = ItemStack.EMPTY;

        public GhostSlot(int xPosition, int yPosition) {
            super(new InventoryBasic("ghost", false, 1), 0, xPosition, yPosition);
        }

        @Override
        public ItemStack getStack() {
            return ghostStack;
        }

        @Override
        public boolean getHasStack() {
            return !ghostStack.isEmpty();
        }

        public void setGhostStack(ItemStack stack) {
            this.ghostStack = stack;
        }

        @Override
        public void putStack(ItemStack stack) {}

        @Override
        public ItemStack decrStackSize(int amount) {
            return ItemStack.EMPTY;
        }

        @Override
        public boolean isItemValid(ItemStack stack) {
            return false;
        }

        @Override
        public boolean canTakeStack(EntityPlayer playerIn) {
            return false;
        }

        @Override
        public boolean isEnabled() {
            return false;
        }
    }
}