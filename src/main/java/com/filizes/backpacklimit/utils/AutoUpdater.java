package com.filizes.backpacklimit.utils;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.io.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@RequiredArgsConstructor
public class AutoUpdater {

    private static final String GITHUB_API_URL = "https://api.github.com/repos/FILIZES/BackPackLimit/releases/latest";
    private static final String DOWNLOAD_URL_FORMAT = "https://github.com/FILIZES/BackPackLimit/releases/download/%s/BackPackLimit.jar";

    private final JavaPlugin plugin;
    private final File pluginFile;
    private final OkHttpClient httpClient = new OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build();
    private final Gson gson = new Gson();

    public void checkForUpdates() {
        String currentVersionStr = plugin.getDescription().getVersion();
        fetchLatestVersionTag()
                .thenAccept(latestVersionTag -> {
                    if (isNewer(currentVersionStr, latestVersionTag)) {
                        downloadUpdate(latestVersionTag);
                    }
                })
                .exceptionally(ex -> {
                    log.error("Не удалось проверить обновления.", ex.getCause() != null ? ex.getCause() : ex);
                    return null;
                });
    }

    private CompletableFuture<String> fetchLatestVersionTag() {
        return CompletableFuture.supplyAsync(() -> {
            var request = new Request.Builder().url(GITHUB_API_URL).build();
            try (var response = httpClient.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    throw new IOException("Не удалось получить ответ от GitHub API: " + response.code());
                }
                var json = gson.fromJson(response.body().string(), JsonObject.class);
                return json.get("tag_name").getAsString();
            } catch (Exception e) {
                throw new RuntimeException("Не удалось получить информацию о последнем релизе.", e);
            }
        });
    }

    private boolean isNewer(String currentVersionStr, String latestVersionStr) {
        try {
            var current = Version.fromString(currentVersionStr);
            var latest = Version.fromString(latestVersionStr);
            return latest.compareTo(current) > 0;
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
            return false;
        }
    }

    private void downloadUpdate(String releaseTag) {
        File updateFolder = plugin.getServer().getUpdateFolderFile();
        if (!updateFolder.exists() && !updateFolder.mkdirs()) {
            return;
        }

        var destinationFile = new File(updateFolder, this.pluginFile.getName());
        var downloadUrl = DOWNLOAD_URL_FORMAT.formatted(releaseTag);
        var request = new Request.Builder().url(downloadUrl).build();

        httpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
                if (!response.isSuccessful()) {
                    return;
                }
                try (var in = response.body().byteStream(); var out = new FileOutputStream(destinationFile)) {
                    in.transferTo(out);
                }
            }

            @Override
            public void onFailure(@NotNull Call call, @NotNull IOException e) {
                e.printStackTrace();
            }
        });
    }

    private record Version(int major, int minor, int patch) implements Comparable<Version> {
        private static final Pattern VERSION_PATTERN = Pattern.compile(".*?(\\d+)(?:\\.(\\d+))?(?:\\.(\\d+))?.*");

        static Version fromString(String versionString) {
            if (versionString == null || versionString.trim().isEmpty()) {
                throw new IllegalArgumentException("Строка версии не может быть пустой.");
            }
            Matcher matcher = VERSION_PATTERN.matcher(versionString.trim());

            if (matcher.matches()) {
                int major = Integer.parseInt(matcher.group(1));
                String minorStr = matcher.group(2);
                int minor = (minorStr != null) ? Integer.parseInt(minorStr) : 0;

                String patchStr = matcher.group(3);
                int patch = (patchStr != null) ? Integer.parseInt(patchStr) : 0;

                return new Version(major, minor, patch);
            } else {
                throw new IllegalArgumentException("Неверный формат строки версии: '" + versionString + "'");
            }
        }

        @Override
        public int compareTo(@NotNull Version other) {
            int majorCompare = Integer.compare(this.major, other.major);
            if (majorCompare != 0) return majorCompare;

            int minorCompare = Integer.compare(this.minor, other.minor);
            if (minorCompare != 0) return minorCompare;

            return Integer.compare(this.patch, other.patch);
        }
    }
}