package me.smokietrue.unikguild;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class Config {
    private Main plugin;
    private File file;
    private FileConfiguration config;
    
    public Config(Main plugin) {
        this.plugin = plugin;
        
        if (!plugin.getDataFolder().exists()) {
            plugin.getDataFolder().mkdirs();
        }
        
        file = new File(plugin.getDataFolder(), "guilds.yml");
        if (!file.exists()) {
            try {
                file.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        config = YamlConfiguration.loadConfiguration(file);
    }
    
    public void saveGuilds(Map<String, Guild> guilds, Map<UUID, String> playerGuilds, Map<String, String> abbreviations) {
        // Очищаем старые данные
        config.set("guilds", null);
        config.set("abbreviations", null);
        config.set("players", null);
        
        // Сохраняем гильдии
        for (Guild guild : guilds.values()) {
            String path = "guilds." + guild.getName();
            
            config.set(path + ".abbreviation", guild.getAbbreviation());
            config.set(path + ".founder", guild.getFounder().toString());
            config.set(path + ".color", guild.getColor());
            config.set(path + ".maxMembers", guild.getMaxMembers());
            
            // Сохраняем участников с ролями
            ConfigurationSection membersSection = config.createSection(path + ".members");
            for (Map.Entry<UUID, String> entry : guild.getRoles().entrySet()) {
                membersSection.set(entry.getKey().toString(), entry.getValue());
            }
        }
        
        // Сохраняем аббревиатуры
        ConfigurationSection abbrevSection = config.createSection("abbreviations");
        for (Map.Entry<String, String> entry : abbreviations.entrySet()) {
            abbrevSection.set(entry.getKey(), entry.getValue());
        }
        
        // Сохраняем привязку игроков к гильдиям
        ConfigurationSection playersSection = config.createSection("players");
        for (Map.Entry<UUID, String> entry : playerGuilds.entrySet()) {
            playersSection.set(entry.getKey().toString(), entry.getValue());
        }
        
        try {
            config.save(file);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    @SuppressWarnings("unchecked")
    public Map[] loadGuilds() {
        Map<String, Guild> guilds = new HashMap<>();
        Map<UUID, String> playerGuilds = new HashMap<>();
        Map<String, String> abbreviations = new HashMap<>();
        
        // Загружаем аббревиатуры (если есть)
        if (config.getConfigurationSection("abbreviations") != null) {
            ConfigurationSection abbrevSection = config.getConfigurationSection("abbreviations");
            for (String abbrev : abbrevSection.getKeys(false)) {
                abbreviations.put(abbrev, abbrevSection.getString(abbrev));
            }
        }
        
        // Загружаем гильдии
        if (config.getConfigurationSection("guilds") != null) {
            for (String guildName : config.getConfigurationSection("guilds").getKeys(false)) {
                String path = "guilds." + guildName;
                
                // Получаем данные гильдии
                String abbreviation = config.getString(path + ".abbreviation", "GUILD");
                UUID founder = UUID.fromString(config.getString(path + ".founder"));
                String color = config.getString(path + ".color");
                int maxMembers = config.getInt(path + ".maxMembers", 15);
                
                // Создаем гильдию
                Guild guild = new Guild(guildName, abbreviation, founder, color, maxMembers);
                
                // Загружаем участников
                ConfigurationSection membersSection = config.getConfigurationSection(path + ".members");
                if (membersSection != null) {
                    for (String uuidStr : membersSection.getKeys(false)) {
                        UUID memberId = UUID.fromString(uuidStr);
                        String role = membersSection.getString(uuidStr);
                        
                        if (!memberId.equals(founder)) {
                            guild.addMember(memberId);
                            guild.promoteMember(memberId, role);
                        }
                    }
                }
                
                guilds.put(guildName, guild);
            }
        }
        
        // Загружаем привязку игроков
        if (config.getConfigurationSection("players") != null) {
            ConfigurationSection playersSection = config.getConfigurationSection("players");
            for (String uuidStr : playersSection.getKeys(false)) {
                UUID playerId = UUID.fromString(uuidStr);
                String guildName = playersSection.getString(uuidStr);
                playerGuilds.put(playerId, guildName);
            }
        }
        
        return new Map[] {guilds, playerGuilds, abbreviations};
    }
}