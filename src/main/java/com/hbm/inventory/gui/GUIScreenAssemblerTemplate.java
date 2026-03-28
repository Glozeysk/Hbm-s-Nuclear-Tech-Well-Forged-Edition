package com.hbm.inventory.gui;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.hbm.inventory.AssemblerRecipes;
import com.hbm.inventory.ChemplantRecipes;
import com.hbm.items.machine.ItemChemistryTemplate;
import com.hbm.items.ModItems;
import com.hbm.lib.RefStrings;
import com.hbm.packet.ItemAssTemplatePacket;
import com.hbm.packet.PacketDispatcher;
import com.hbm.tileentity.machine.TileEntityMachineAssembler;
import com.hbm.tileentity.machine.TileEntityMachineAssembly;
import com.hbm.tileentity.machine.TileEntityMachineChemplant;
import com.hbm.tileentity.machine.TileEntityMachineChemfac;
import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.PositionedSoundRecord;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.network.NetworkRegistry.TargetPoint;
import org.lwjgl.input.Keyboard;

public class GUIScreenAssemblerTemplate extends GuiScreen {
	
    protected static final ResourceLocation texture = new ResourceLocation(RefStrings.MODID + ":textures/gui/gui_planner_template.png");
    protected int xSize = 176;
    protected int ySize = 229;
    protected int guiLeft;
    protected int guiTop;
	protected TileEntity tile;
	EntityLivingBase player;
    int currentPage = 0;
    List<ItemStack> stacks = new ArrayList<ItemStack>();
    List<FolderButton> buttons = new ArrayList<FolderButton>();
    // private final EntityPlayer player;
	private final List<ItemStack> allStacks;
	private GuiTextField search;
	protected GuiScreen previousScreen;
	protected int slot;
	private void search(String sub) {

		stacks.clear();

		this.currentPage = 0;

		if(sub == null || sub.isEmpty()) {
			stacks.addAll(allStacks);
			updateButtons();
			return;
		}

		sub = sub.toLowerCase();

		for(ItemStack stack : allStacks) {
			if(stack.getDisplayName().toLowerCase().contains(sub)) {
				stacks.add(stack);
			}
		}

		updateButtons();
	}

	public static void openSelector(TileEntity tile, GuiScreen previousScreen, int slot) {
        FMLCommonHandler.instance().showGuiScreen(new GUIScreenAssemblerTemplate(tile, previousScreen, slot));
    }

    public GUIScreenAssemblerTemplate(TileEntity tile, GuiScreen previousScreen, int slot) {
    	
    	this.tile = tile;
		this.previousScreen = previousScreen;
		this.slot = slot;
		this.allStacks = new ArrayList<>();

    	//Assembly Templates
		if(tile instanceof TileEntityMachineAssembler){
			for (int i = 0; i < AssemblerRecipes.recipeList.size(); ++i) {
				NBTTagCompound tag = new NBTTagCompound();
				tag.setInteger("type", i);
				ItemStack stack = new ItemStack(ModItems.assembly_template, 1, 0);
				stack.setTagCompound(tag);
				allStacks.add(stack);
			}
		}
		if(tile instanceof TileEntityMachineAssembly){
			for (int i = 0; i < AssemblerRecipes.recipeList.size(); ++i) {
				NBTTagCompound tag = new NBTTagCompound();
				tag.setInteger("type", i);
				ItemStack stack = new ItemStack(ModItems.assembly_template, 1, 0);
				stack.setTagCompound(tag);
				allStacks.add(stack);
			}
		}
		//Chemistry Templates
		if(tile instanceof TileEntityMachineChemplant){
			for (int i: ChemplantRecipes.recipeNames.keySet()){
				allStacks.add(new ItemStack(ModItems.chemistry_template, 1, i));
			}
		}
		if(tile instanceof TileEntityMachineChemfac){
			for (int i: ChemplantRecipes.recipeNames.keySet()){
				allStacks.add(new ItemStack(ModItems.chemistry_template, 1, i));
			}
		}
		search(null);
    }
    
    int getPageCount() {
    	return (int)Math.ceil((stacks.size() - 1) / (5 * 7));
    }
    
    public void updateScreen() {
    	if(currentPage < 0)
    		currentPage = 0;
    	if(currentPage > getPageCount())
    		currentPage = getPageCount();
    }
    
    public void drawScreen(int mouseX, int mouseY, float f)
    {
        this.drawDefaultBackground();
        this.drawGuiContainerBackgroundLayer(f, mouseX, mouseY);
        GlStateManager.disableLighting();
        this.drawGuiContainerForegroundLayer(mouseX, mouseY);
        GlStateManager.enableLighting();
    }
    
    public void initGui()
    {
        super.initGui();
        this.guiLeft = (this.width - this.xSize) / 2;
        this.guiTop = (this.height - this.ySize) / 2;

        updateButtons();

		Keyboard.enableRepeatEvents(true);
		this.search = new GuiTextField(0, this.fontRenderer, guiLeft + 61, guiTop + 213, 48, 12);
		this.search.setTextColor(0xffffff);
		this.search.setDisabledTextColour(0xffffff);
		this.search.setEnableBackgroundDrawing(false);
		this.search.setMaxStringLength(100);
    }
	
	@Override
	public boolean doesGuiPauseGame() {
		return false;
	}
    
    protected void updateButtons() {
        
        if(!buttons.isEmpty())
        	buttons.clear();
        
        for(int i = currentPage * 35; i < Math.min(currentPage * 35 + 35, stacks.size()); i++) {
    		buttons.add(new FolderButton(guiLeft + 25 + (27 * (i % 5)), guiTop + 26 + (27 * (int)Math.floor((i / 5D))) - currentPage * 27 * 7, stacks.get(i)));
        }

        if(currentPage != 0)
        	buttons.add(new FolderButton(guiLeft + 25 - 18, guiTop + 26 + (27 * 3), 1, "Previous"));
        if(currentPage != getPageCount())
        	buttons.add(new FolderButton(guiLeft + 25 + (27 * 4) + 18, guiTop + 26 + (27 * 3), 2, "Next"));
    }

    protected void mouseClicked(int i, int j, int k) {
		if(i >= guiLeft + 45 && i < guiLeft + 117 && j >= guiTop + 211 && j < guiTop + 223) {
			this.search.setFocused(true);
		} else  {
			this.search.setFocused(false);
		}

    	try {
    		for(FolderButton b : buttons)
    			if(b.isMouseOnButton(i, j))
    				b.executeAction();
    	} catch (Exception ex) {
    		updateButtons();
    	}

		if(guiLeft + 3 <= i && guiLeft + 3 + 11 > i && guiTop + 3 < j && guiTop + 3 + 11 >= j) {
            FMLCommonHandler.instance().showGuiScreen(previousScreen);
        }
    }
	
	protected void drawGuiContainerForegroundLayer(int i, int j) {

		this.fontRenderer.drawString(I18n.format((currentPage + 1) + "/" + (getPageCount() + 1)), 
				guiLeft + this.xSize / 2 - this.fontRenderer.getStringWidth(I18n.format((currentPage + 1) + "/" + (getPageCount() + 1))) / 2, guiTop + 10, 4210752);
		
		for(FolderButton b : buttons)
			if(b.isMouseOnButton(i, j))
				b.drawString(i, j);
	}

	protected void drawGuiContainerBackgroundLayer(float f, int i, int j) {
		GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
		Minecraft.getMinecraft().getTextureManager().bindTexture(texture);
		drawTexturedModalRect(guiLeft, guiTop, 0, 0, xSize, ySize);

		if(search.isFocused())
			drawTexturedModalRect(guiLeft + 45, guiTop + 211, 176, 54, 72, 12);

		for(FolderButton b : buttons)
			b.drawButton(b.isMouseOnButton(i, j));
		for(FolderButton b : buttons)
			b.drawIcon(b.isMouseOnButton(i, j));

		search.drawTextBox();
	}

	@Override
    protected void keyTyped(char p_73869_1_, int p_73869_2_)
    {
		if (this.search.textboxKeyTyped(p_73869_1_, p_73869_2_)) {
			this.search(this.search.getText());
			return;
		}

		if(p_73869_2_ == 1 || p_73869_2_ == this.mc.gameSettings.keyBindInventory.getKeyCode()) {
			this.mc.player.closeScreen();
		}
        
    }
	
	class FolderButton {
		
		int xPos;
		int yPos;
		//0: regular, 1: prev, 2: next
		int type;
		String info;
		ItemStack stack;
		
		public FolderButton(int x, int y, int t, String i) {
			xPos = x;
			yPos = y;
			type = t;
			info = i;
		}
		
		public FolderButton(int x, int y, ItemStack stack) {
			xPos = x;
			yPos = y;
			type = 0;
			info = stack.getDisplayName();
			this.stack = stack.copy();
		}
		
		public void updateButton(int mouseX, int mouseY) {
		}
		
		public boolean isMouseOnButton(int mouseX, int mouseY) {
			return xPos <= mouseX && xPos + 18 > mouseX && yPos < mouseY && yPos + 18 >= mouseY;
		}
		
		public void drawButton(boolean b) {
			Minecraft.getMinecraft().getTextureManager().bindTexture(texture);
			drawTexturedModalRect(xPos, yPos, b ? 176 + 18 : 176, type == 1 ? 18 : (type == 2 ? 36 : 0), 18, 18);
		}
		
		public void drawIcon(boolean b) {
			try {
		        RenderHelper.enableGUIStandardItemLighting();
				if(stack != null) {
					if(tile instanceof TileEntityMachineAssembler && stack.getItem() == ModItems.assembly_template)
						itemRender.renderItemAndEffectIntoGUI(player, AssemblerRecipes.getOutputFromTempate(stack), xPos + 1, yPos + 1);
					else if((tile instanceof TileEntityMachineChemplant) && stack.getItem() == ModItems.chemistry_template)
						itemRender.renderItemAndEffectIntoGUI(player, new ItemStack(ModItems.chemistry_icon, 1, stack.getItemDamage()), xPos + 1, yPos + 1);
					else if((tile instanceof TileEntityMachineChemfac) && stack.getItem() == ModItems.chemistry_template)
						itemRender.renderItemAndEffectIntoGUI(player, new ItemStack(ModItems.chemistry_icon, 1, stack.getItemDamage()), xPos + 1, yPos + 1);
					else if(tile instanceof TileEntityMachineAssembly && stack.getItem() == ModItems.assembly_template)
						itemRender.renderItemAndEffectIntoGUI(player, AssemblerRecipes.getOutputFromTempate(stack), xPos + 1, yPos + 1);
				}
				RenderHelper.disableStandardItemLighting();
			} catch(Exception x) { }
		}
		
		public void drawString(int x, int y) {
			if(info == null || info.isEmpty())
				return;
			
			String s = info;

			drawHoveringText(Arrays.asList(new String[] { s }), x, y);
		}
		
		public void executeAction() {
			mc.getSoundHandler().playSound(PositionedSoundRecord.getMasterRecord(SoundEvents.UI_BUTTON_CLICK, 1.0F));
			World world = Minecraft.getMinecraft().world;
			if(type == 0) {
				PacketDispatcher.wrapper.sendToServer(new ItemAssTemplatePacket(tile.getPos(), stack.copy(), slot));
				FMLCommonHandler.instance().showGuiScreen(previousScreen);
			} else if(type == 1) {
				if(currentPage > 0)
					currentPage--;
				updateButtons();
			} else if(type == 2) {
				if(currentPage < getPageCount())
					currentPage++;
				updateButtons();
			}
		}
		
	}

}
