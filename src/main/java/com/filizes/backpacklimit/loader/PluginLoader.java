package com.filizes.backpacklimit.loader;

import com.filizes.backpacklimit.Main;
import com.filizes.backpacklimit.config.settings.PerformanceSettings;
import com.filizes.backpacklimit.database.interfaces.DatabaseManager;
import com.filizes.backpacklimit.listener.PapiListener;
import com.filizes.backpacklimit.listener.PlayerListener;
import com.filizes.backpacklimit.listener.service.InventoryLimitService;
import com.filizes.backpacklimit.module.PluginModule;
import com.filizes.backpacklimit.utils.AutoUpdater;
import com.filizes.backpacklimit.utils.ConfigUpdater;
import com.google.inject.Guice;
import com.google.inject.Injector;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.bukkit.Bukkit;
import org.bukkit.plugin.PluginManager;

import java.io.File;

@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public final class PluginLoader {

    @Getter
    private final Injector injector;
    private final Main plugin;
    private final File pluginFile;

    private final CommandsLoader commandsLoader;

    public PluginLoader(Main plugin, File pluginFile) {
        this.plugin = plugin;
        this.pluginFile = pluginFile;
        setupConfigs();
        this.injector = Guice.createInjector(new PluginModule(plugin));
        this.commandsLoader = new CommandsLoader(plugin, injector);
    }

    public void load() {
        commandsLoader.load();
        registerListeners();
        registerTasks();
        Bukkit.getScheduler().runTaskAsynchronously(plugin, this::checkForUpdates);
    }

    public void unload() {
        commandsLoader.unload();
        injector.getInstance(DatabaseManager.class).disconnect();
    }

    private void setupConfigs() {
        plugin.saveDefaultConfig();

        var updater = new ConfigUpdater(plugin);
        updater.update("config.yml");
        updater.update("messages.yml");

        plugin.reloadConfig();
    }

    private void registerTasks() {
        InventoryLimitService limitService = injector.getInstance(InventoryLimitService.class);
        PerformanceSettings settings = injector.getInstance(PerformanceSettings.class);

        long intervalTicks = settings.inventoryCheckIntervalSeconds() * 20L;

        plugin.getServer().getScheduler().runTaskTimerAsynchronously(
                plugin,
                limitService::performScheduledCheck,
                intervalTicks,
                intervalTicks
        );
    }


    private void registerListeners() {
        PluginManager pluginManager = plugin.getServer().getPluginManager();

        pluginManager.registerEvents(injector.getInstance(PlayerListener.class), plugin);

        if (pluginManager.isPluginEnabled("PlaceholderAPI")) {
            injector.getInstance(PapiListener.class).register();
        }
    }

    private void checkForUpdates() {
        new AutoUpdater(plugin, this.pluginFile).checkForUpdates();
    }
}