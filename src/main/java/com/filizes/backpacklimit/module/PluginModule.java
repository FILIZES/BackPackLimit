package com.filizes.backpacklimit.module;

import com.filizes.backpacklimit.Main;
import com.filizes.backpacklimit.commands.BackpackLimitCommand;
import com.filizes.backpacklimit.commands.argument.OfflinePlayerArgumentResolver;
import com.filizes.backpacklimit.commands.handler.CustomInvalidUsageHandler;
import com.filizes.backpacklimit.config.service.MessageService;
import com.filizes.backpacklimit.config.settings.LimitSettings;
import com.filizes.backpacklimit.config.settings.MessageSettings;
import com.filizes.backpacklimit.config.settings.PerformanceSettings;
import com.filizes.backpacklimit.database.factory.DatabaseManagerFactory;
import com.filizes.backpacklimit.database.interfaces.DatabaseManager;
import com.filizes.backpacklimit.listener.PapiListener;
import com.filizes.backpacklimit.listener.PlayerListener;
import com.github.benmanes.caffeine.cache.AsyncLoadingCache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.UUID;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public final class PluginModule extends AbstractModule {

    private final Main plugin;

    public PluginModule(Main plugin) {
        this.plugin = plugin;
    }

    @Override
    protected void configure() {
        bind(Main.class).toInstance(this.plugin);
        bind(BackpackLimitCommand.class).in(Singleton.class);
        bind(OfflinePlayerArgumentResolver.class).in(Singleton.class);
        bind(CustomInvalidUsageHandler.class).in(Singleton.class);
        bind(PlayerListener.class).in(Singleton.class);

        if (plugin.getServer().getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            bind(PapiListener.class).in(Singleton.class);
        }
    }

    @Provides @Singleton public FileConfiguration provideConfig() {
        return plugin.getConfig();
    }

    @Provides @Singleton @Named("bukkitMainThreadExecutor") public Executor provideBukkitExecutor() {
        return runnable -> plugin.getServer().getScheduler().runTask(plugin, runnable);
    }

    @Provides @Singleton @Named("databaseExecutor") public Executor provideDatabaseExecutor() {
        int poolSize = Math.max(2, Runtime.getRuntime().availableProcessors() / 2);
        return Executors.newFixedThreadPool(poolSize);
    }

    @Provides @Singleton public LimitSettings provideLimitSettings(FileConfiguration config) {
        int defaultLimit = config.getInt("limit-settings.default-backpack-limit", 8);
        boolean ignore = config.getBoolean("limit-settings.ignore-armor-and-offhand", true);
        return new LimitSettings(defaultLimit, ignore);
    }

    @Provides
    @Singleton
    public MessageSettings provideMessageSettings(FileConfiguration config) {
        String prefix = config.getString("prefix", "&c[BPL] &r");
        long throttle = config.getLong("messages.throttle-cooldown-seconds", 3L);
        return new MessageSettings(prefix, throttle);
    }

    @Provides @Singleton public PerformanceSettings providePerformanceSettings(FileConfiguration config) {
        long checkInterval = config.getLong("performance.inventory-check-interval-seconds", 5);
        long interactionDelay = config.getLong("performance.inventory-interaction-check-delay-ticks", 2L);
        return new PerformanceSettings(checkInterval, interactionDelay);
    }

    @Provides
    @Singleton
    public AsyncLoadingCache<UUID, Integer> provideBackpackLimitCache(DatabaseManager dbManager) {
        return Caffeine.newBuilder()
                .expireAfterAccess(15, TimeUnit.MINUTES)
                .maximumSize(1000)
                .buildAsync((key, executor) -> dbManager.getPlayerBackpackLimit(key));
    }

    @Provides
    @Singleton
    public MessageService provideMessageService() {
        return new MessageService(plugin);
    }

    @Provides @Singleton public DatabaseManager provideDatabaseManager(
            Main plugin,
            LimitSettings limitSettings,
            @Named("databaseExecutor") Executor dbExecutor
    ) {
        return DatabaseManagerFactory.createDatabaseManager(plugin, limitSettings, dbExecutor);
    }
}