package com.filizes.backpacklimit;

import com.filizes.backpacklimit.config.service.MessageService;
import com.filizes.backpacklimit.listener.service.InventoryLimitService;
import com.filizes.backpacklimit.loader.PluginLoader;
import lombok.extern.slf4j.Slf4j;
import org.bukkit.plugin.java.JavaPlugin;

@Slf4j
public final class Main extends JavaPlugin {

    private PluginLoader loader;

    @Override
    public void onEnable() {
        try {
            this.loader = new PluginLoader(this, getFile());
            this.loader.load();
        } catch (Exception e) {
            e.printStackTrace();
            getServer().getPluginManager().disablePlugin(this);
        }
    }

    @Override
    public void onDisable() {
        if (this.loader != null) {
            this.loader.getInjector().getInstance(MessageService.class);
            this.loader.getInjector().getInstance(InventoryLimitService.class);
            this.loader.unload();
        }
    }
}