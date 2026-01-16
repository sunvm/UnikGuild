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
        config.set("guilds", null);
        config.set("players", null);

        for (Guild guild : guilds.values()) {
            String path = "guilds." + guild.getName();
            config.set(path + ".founder", guild.getFounder().toString());
            config.set(path + ".color", guild.getColor());
            config.set(path + ".maxMembers", guild.getMaxMembers());

            // Сохраняем кастомные роли
            List<String> customRoles = new ArrayList<>(guild.getCustomRoles());
            config.set(path + ".customRoles", customRoles);

            // Сохраняем участников с ролями
            ConfigurationSection membersSection = config.createSection(path + ".members");
            for (Map.Entry<UUID, String> entry : guild.getRoles().entrySet()) {
                membersSection.set(entry.getKey().toString(), entry.getValue());
            }
        }

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

        if (config.getConfigurationSection("guilds") != null) {
            for (String guildName : config.getConfigurationSection("guilds").getKeys(false)) {
                String path = "guilds." + guildName;
                UUID founder = UUID.fromString(config.getString(path + ".founder"));
                String color = config.getString(path + ".color");
                int maxMembers = config.getInt(path + ".maxMembers", 15);

                Guild guild = new Guild(guildName, founder, color, maxMembers);

                // Загружаем кастомные роли
                List<String> customRoles = config.getStringList(path + ".customRoles");
                if (customRoles != null) {
                    for (String role : customRoles) {
                        guild.addCustomRole(role);
                    }
                }

                ConfigurationSection membersSection = config.getConfigurationSection(path + ".members");
                if (membersSection != null) {
                    for (String uuidStr : membersSection.getKeys(false)) {
                        UUID memberId = UUID.fromString(uuidStr);
                        String role = membersSection.getString(uuidStr);
                        if (!memberId.equals(founder)) {
                            guild.addMember(memberId);
                            guild.assignRole(memberId, role != null ? role : "Рядовой");
                        }
                    }
                }
                guilds.put(guildName, guild);
            }
        }

        if (config.getConfigurationSection("players") != null) {
            ConfigurationSection playersSection = config.getConfigurationSection("players");
            for (String uuidStr : playersSection.getKeys(false)) {
                playerGuilds.put(UUID.fromString(uuidStr), playersSection.getString(uuidStr));
            }
        }

        return new Map[] {guilds, playerGuilds};
    }
}