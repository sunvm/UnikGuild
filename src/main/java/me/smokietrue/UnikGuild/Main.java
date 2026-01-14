package me.smokietrue.unikguild;

import org.bukkit.plugin.java.JavaPlugin;

public class Main extends JavaPlugin {
    private GuildManager guildManager;
    private ItemManager itemManager;
    private Config config;
    private EventListener eventListener;

    @Override
    public void onEnable() {
        this.config = new Config(this);
        this.guildManager = new GuildManager(this);
        this.itemManager = new ItemManager(this);
        this.eventListener = new EventListener(this);
        
        getServer().getPluginManager().registerEvents(eventListener, this);
        
        // Регистрируем все команды
        GuildCommand guildCommand = new GuildCommand(this);
        getCommand("guilds").setExecutor(guildCommand);
        getCommand("g").setExecutor(guildCommand);
        getCommand("guildcolor").setExecutor(guildCommand);
        
        guildManager.loadGuilds();
        
        getLogger().info("UnikGuild включен! Автор: smokietrue");
    }

    @Override
    public void onDisable() {
        guildManager.saveGuilds();
        getLogger().info("UnikGuild выключен!");
    }

    public GuildManager getGuildManager() { return guildManager; }
    public ItemManager getItemManager() { return itemManager; }
    public Config getPluginConfig() { return config; }
    public EventListener getEventListener() { return eventListener; }
}