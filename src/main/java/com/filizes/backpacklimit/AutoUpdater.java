package com.filizes.backpacklimit;

import org.bukkit.event.Listener;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import static org.bukkit.Bukkit.getLogger;

public class AutoUpdater implements Listener {

    private final String GIT_REPO = "https://api.github.com/repos/FILIZES/BackPackLimit/releases/latest";
    private final String DOWNLOAD_URL = "https://github.com/FILIZES/BackPackLimit/releases/download/";
    private final Plugin plugin;

    public AutoUpdater(Plugin plugin) {
        this.plugin = plugin;
    }

    public void checkForUpdates() {
        CompletableFuture.runAsync(() -> {
            try {
                HttpURLConnection connection = (HttpURLConnection) new URL(GIT_REPO).openConnection();
                connection.setRequestMethod("GET");
                connection.connect();

                if (connection.getResponseCode() == 200) {

                    String latestVersion = new BufferedReader(new InputStreamReader(connection.getInputStream()))
                            .lines().collect(Collectors.joining("\n"))
                            .split("\"tag_name\":\"")[1].split("\"")[0];

                    if (isNewVersionAvailable(latestVersion)) {
                        getLogger().info("New version available: " + latestVersion);
                        downloadAndUpdatePlugin(latestVersion);
                    } else {
                        getLogger().info("No updates available.");
                    }
                } else {
                    getLogger().warning("Failed to check for updates.");
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }

    private boolean isNewVersionAvailable(String latestVersion) {
        String currentVersion = plugin.getDescription().getVersion();
        return !currentVersion.equals(latestVersion);
    }

    private void downloadAndUpdatePlugin(String version) {
        CompletableFuture.runAsync(() -> {
            getLogger().info("Downloading update...");
            try {
                URL url = new URL(DOWNLOAD_URL + version + "/BackPackLimit.jar");
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.connect();

                File file = new File(plugin.getDataFolder().getParentFile(), "BackPackLimit.jar");
                try (BufferedInputStream in = new BufferedInputStream(connection.getInputStream());
                     FileOutputStream out = new FileOutputStream(file)) {

                    byte[] buffer = new byte[1024];
                    int bytesRead;
                    while ((bytesRead = in.read(buffer)) != -1) {
                        out.write(buffer, 0, bytesRead);
                    }
                }

                getLogger().info("Update downloaded. Restarting server...");
                Bukkit.getScheduler().runTask(plugin, Bukkit::reload);
            } catch (IOException e) {
                getLogger().warning("Failed to download update.");
                e.printStackTrace();
            }
        });
    }
}