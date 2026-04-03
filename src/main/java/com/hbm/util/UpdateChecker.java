package com.hbm.util;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.hbm.lib.RefStrings;
import com.hbm.main.MainRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.Style;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.event.ClickEvent;
import net.minecraft.util.text.event.HoverEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class UpdateChecker {

    private static final String CURSEFORGE_API_URL = "https://api.curseforge.com/v1/mods/1467521/files?pageSize=1";
    private static final String CURSEFORGE_PAGE_URL = "https://www.curseforge.com/minecraft/mc-mods/ntm-waldemar-edition";
    private static final String API_KEY = "$2a$10$bL4bIL5pUWqfcO7KQtnMReakwtfHbNKh6v1uTpKlzhwoueEJQnPnm";

    private static boolean checkedThisSession = false;
    private static String latestVersion = null;
    private static boolean updateAvailable = false;

    @SubscribeEvent
    @SideOnly(Side.CLIENT)
    public void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if(checkedThisSession) {
            showUpdateMessage(event.player);
            return;
        }

        new Thread(() -> {
            checkForUpdates();
            checkedThisSession = true;
            Minecraft.getMinecraft().addScheduledTask(() -> showUpdateMessage(event.player));
        }, "NTM-UpdateChecker").start();
    }

    private void checkForUpdates() {
        try {
            URL url = new URL(CURSEFORGE_API_URL);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setRequestProperty("Accept", "application/json");
            connection.setRequestProperty("x-api-key", API_KEY);
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);

            if(connection.getResponseCode() == 200) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                StringBuilder response = new StringBuilder();
                String line;
                while((line = reader.readLine()) != null) {
                    response.append(line);
                }
                reader.close();

                JsonObject json = new JsonParser().parse(response.toString()).getAsJsonObject();
                JsonObject latestFile = json.getAsJsonArray("data").get(0).getAsJsonObject();
                latestVersion = latestFile.get("displayName").getAsString();

                String currentVersion = RefStrings.VERSION;
                updateAvailable = !latestVersion.contains(currentVersion) && !currentVersion.contains(latestVersion);
            }
            connection.disconnect();
        } catch(Exception e) {
            MainRegistry.logger.warn("Failed to check for updates: " + e.getMessage());
        }
    }

    @SideOnly(Side.CLIENT)
    private void showUpdateMessage(EntityPlayer player) {
        if(latestVersion == null) {
            return;
        }

        ITextComponent prefix = new TextComponentString("[NTM] ")
                .setStyle(new Style().setColor(TextFormatting.GOLD));

        if(updateAvailable) {
            ITextComponent newVersionText = new TextComponentTranslation("ntm.update.newversion")
                    .setStyle(new Style().setColor(TextFormatting.YELLOW));

            ITextComponent versionNumber = new TextComponentString(latestVersion)
                    .setStyle(new Style().setColor(TextFormatting.GREEN).setBold(true));

            ITextComponent message = prefix.createCopy();
            message.appendSibling(newVersionText);
            message.appendSibling(new TextComponentString(" "));
            message.appendSibling(versionNumber);

            player.sendMessage(message);

            ITextComponent downloadHover = new TextComponentTranslation("ntm.update.download.hover");

            ITextComponent downloadButton = new TextComponentTranslation("ntm.update.download")
                    .setStyle(new Style()
                            .setColor(TextFormatting.AQUA)
                            .setBold(true)
                            .setClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, CURSEFORGE_PAGE_URL))
                            .setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, downloadHover)));

            ITextComponent downloadMessage = prefix.createCopy();
            downloadMessage.appendSibling(downloadButton);

            player.sendMessage(downloadMessage);
        } else {
            ITextComponent upToDateText = new TextComponentTranslation("ntm.update.uptodate")
                    .setStyle(new Style().setColor(TextFormatting.GREEN));

            ITextComponent message = prefix.createCopy();
            message.appendSibling(upToDateText);

            player.sendMessage(message);
        }
    }
}