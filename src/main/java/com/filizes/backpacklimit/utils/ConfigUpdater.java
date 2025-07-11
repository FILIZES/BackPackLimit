package com.filizes.backpacklimit.utils;

import com.filizes.backpacklimit.Main;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Set;

@Slf4j
@RequiredArgsConstructor
public final class ConfigUpdater {

    private final Main plugin;

    public void update(String fileName) {
        File configFile = new File(plugin.getDataFolder(), fileName);
        if (!configFile.exists()) {
            plugin.saveResource(fileName, false);
            return;
        }
        try (InputStream defaultStream = plugin.getResource(fileName)) {
            if (defaultStream == null) {
                return;
            }

            YamlConfiguration userConfig = YamlConfiguration.loadConfiguration(configFile);
            YamlConfiguration defaultConfig = YamlConfiguration.loadConfiguration(new InputStreamReader(defaultStream, StandardCharsets.UTF_8));
            Set<String> missingKeys = defaultConfig.getKeys(true);
            missingKeys.removeAll(userConfig.getKeys(true));

            if (missingKeys.isEmpty()) {
                return;
            }

            Path backupPath = new File(plugin.getDataFolder(), fileName + ".old").toPath();
            Files.copy(configFile.toPath(), backupPath, StandardCopyOption.REPLACE_EXISTING);

            for (String key : missingKeys) {
                userConfig.set(key, defaultConfig.get(key));
            }

            userConfig.save(configFile);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}