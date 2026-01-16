package me.smokietrue.unikguild;

import org.bukkit.plugin.java.JavaPlugin;

public class Main extends JavaPlugin {
    private GuildManager guildManager;
    private ItemManager itemManager;
    private Config config;
    private EventListener eventListener;

    @Override
    public void onEnable() {
        // Инициализация компонентов
        this.config = new Config(this);
        this.guildManager = new GuildManager(this);
        this.itemManager = new ItemManager(this);
        this.eventListener = new EventListener(this);
        
        // Регистрация событий
        getServer().getPluginManager().registerEvents(eventListener, this);
        
        // Регистрируем команды С ПРОВЕРКОЙ на null
        GuildCommand guildCommand = new GuildCommand(this);
        
        // Команда "guilds" (должна быть в plugin.yml)
        if (getCommand("guilds") != null) {
            getCommand("guilds").setExecutor(guildCommand);
        } else {
            getLogger().severe("Команда 'guilds' не найдена в plugin.yml! Плагин будет отключен.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        
        // Команда "g" (должна быть в plugin.yml)
        if (getCommand("g") != null) {
            getCommand("g").setExecutor(guildCommand);
        } else {
            getLogger().warning("Команда 'g' не найдена в plugin.yml, но это не критично.");
        }
        
        // КОМАНДА "guildcolor" - УБЕРИТЕ ЭТУ СТРОКУ ИЛИ ДОБАВЬТЕ В plugin.yml!
        // getCommand("guildcolor").setExecutor(guildCommand); // ← ЭТУ СТРОКУ НУЖНО УДАЛИТЬ!
        
        // Загрузка данных
        guildManager.loadGuilds();
        
        getLogger().info("UnikGuild включен! Автор: smokietrue");
    }

    @Override
    public void onDisable() {
        if (guildManager != null) {
            guildManager.saveGuilds();
        }
        getLogger().info("UnikGuild выключен!");
    }

    public GuildManager getGuildManager() { return guildManager; }
    public ItemManager getItemManager() { return itemManager; }
    public Config getPluginConfig() { return config; }
    public EventListener getEventListener() { return eventListener; }
}