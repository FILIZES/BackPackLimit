package com.filizes.backpacklimit;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.Call;
import okhttp3.Callback;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;

import java.io.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AutoUpdater implements Listener {

    private static final String GIT_REPO = "https://api.github.com/repos/FILIZES/BackPackLimit/releases/latest";
    private static final String DOWNLOAD_URL = "https://github.com/FILIZES/BackPackLimit/releases/download/";

    private final Plugin plugin;
    private final OkHttpClient httpClient;

    public AutoUpdater(Plugin plugin) {
        this.plugin = plugin;
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(15, TimeUnit.SECONDS)
                .build();
    }

    public void checkForUpdates() {
        CompletableFuture.supplyAsync(this::fetchLatestVersion)
                .thenAccept(latestVersion -> {
                    if (latestVersion == null) return;

                    if (isNewVersionAvailable(latestVersion)) {
                        plugin.getLogger().info("Доступна новая версия: " + latestVersion);
                        downloadAndUpdatePlugin(latestVersion);
                    } else {
                        plugin.getLogger().info("Обновления нет. Используется последняя версия.");
                    }
                })
                .exceptionally(e -> {
                    plugin.getLogger().severe("Ошибка при проверке обновлений: " + e.getMessage());
                    return null;
                });
    }

    private String fetchLatestVersion() {
        Request request = new Request.Builder()
                .url(GIT_REPO)
                .header("User-Agent", "BackPackLimit-Updater")
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful() || response.body() == null) {
                plugin.getLogger().warning("Не удалось проверить обновления. Код ответа: " + response.code());
                return null;
            }

            return extractLatestVersion(response.body().string());
        } catch (IOException e) {
            plugin.getLogger().severe("Ошибка при получении данных об обновлении: " + e.getMessage());
            return null;
        }
    }

    private String extractLatestVersion(String json) {
        Pattern pattern = Pattern.compile("\"tag_name\":\"([^\"]+)\"");
        Matcher matcher = pattern.matcher(json);
        if (matcher.find()) {
            return matcher.group(1);
        }
        plugin.getLogger().warning("Не удалось извлечь номер версии из JSON.");
        return null;
    }

    private boolean isNewVersionAvailable(String latestVersion) {
        String currentVersion = plugin.getDescription().getVersion();
        return compareVersions(currentVersion, latestVersion) < 0;
    }

    private int compareVersions(String v1, String v2) {
        String[] parts1 = v1.split("\\.");
        String[] parts2 = v2.split("\\.");
        int length = Math.max(parts1.length, parts2.length);

        for (int i = 0; i < length; i++) {
            int num1 = (i < parts1.length) ? parseIntSafe(parts1[i]) : 0;
            int num2 = (i < parts2.length) ? parseIntSafe(parts2[i]) : 0;

            if (num1 != num2) return Integer.compare(num1, num2);
        }
        return 0;
    }

    private int parseIntSafe(String str) {
        try {
            return Integer.parseInt(str);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private void downloadAndUpdatePlugin(String version) {
        plugin.getLogger().info("Загрузка обновления " + version + "...");

        CompletableFuture.runAsync(() -> {
            File pluginsFolder = plugin.getDataFolder().getParentFile();
            File newFile = new File(pluginsFolder, "BackPackLimit.jar");
            String fileUrl = DOWNLOAD_URL + version + "/BackPackLimit.jar";

            Request request = new Request.Builder().url(fileUrl).build();

            httpClient.newCall(request).enqueue(new Callback() {
                @Override
                public void onResponse(Call call, Response response) {
                    if (!response.isSuccessful() || response.body() == null) {
                        plugin.getLogger().severe("Ошибка при скачивании: Код " + response.code());
                        return;
                    }

                    try (InputStream in = response.body().byteStream();
                         FileOutputStream out = new FileOutputStream(newFile)) {
                        byte[] buffer = new byte[4096];
                        int bytesRead;
                        while ((bytesRead = in.read(buffer)) != -1) {
                            out.write(buffer, 0, bytesRead);
                        }
                        plugin.getLogger().info("Обновление загружено! Перезагрузите сервер.");
                    } catch (IOException e) {
                        plugin.getLogger().severe("Ошибка при записи файла: " + e.getMessage());
                    }
                }

                @Override
                public void onFailure(Call call, IOException e) {
                    plugin.getLogger().severe("Ошибка скачивания обновления: " + e.getMessage());
                }
            });
        });
    }
}
