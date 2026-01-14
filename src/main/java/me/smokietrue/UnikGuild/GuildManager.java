package me.smokietrue.unikguild;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

public class GuildManager {
    private Map<String, Guild> guilds = new HashMap<>();
    private Map<String, String> abbreviations = new HashMap<>();
    private Map<UUID, String> playerGuilds = new HashMap<>();
    private Main plugin;

    public GuildManager(Main plugin) {
        this.plugin = plugin;
    }

    public boolean createGuild(Player founder, String name, String abbreviation, String color) {
        if (guilds.containsKey(name)) return false;
        if (playerGuilds.containsKey(founder.getUniqueId())) return false;
        if (abbreviations.containsKey(abbreviation.toUpperCase())) return false;
        
        Guild guild = new Guild(name, abbreviation, founder.getUniqueId(), color, 15);
        guilds.put(name, guild);
        abbreviations.put(abbreviation.toUpperCase(), name);
        playerGuilds.put(founder.getUniqueId(), name);
        
        Bukkit.getScheduler().runTask(plugin, () -> {
            founder.setPlayerListName(guild.getPrefix() + founder.getName());
            founder.setDisplayName(guild.getPrefix() + founder.getName());
        });
        
        return true;
    }

    public Guild getGuild(String name) {
        return guilds.get(name);
    }

    public Guild getGuildByPlayer(UUID player) {
        String guildName = playerGuilds.get(player);
        return guildName != null ? guilds.get(guildName) : null;
    }
    
    public Guild getGuildByAbbreviation(String abbreviation) {
        String guildName = abbreviations.get(abbreviation.toUpperCase());
        return guildName != null ? guilds.get(guildName) : null;
    }
    
    public boolean isAbbreviationUsed(String abbreviation) {
        return abbreviations.containsKey(abbreviation.toUpperCase());
    }

    public boolean deleteGuild(String name) {
        Guild guild = guilds.get(name);
        if (guild == null) return false;
        
        // Удаляем предметы гильдии у всех участников
        for (UUID member : guild.getMembers()) {
            Player player = Bukkit.getPlayer(member);
            if (player != null) {
                removeGuildItems(player);
            }
            
            playerGuilds.remove(member);
            
            // Восстанавливаем стандартное отображение
            if (player != null) {
                player.setPlayerListName(player.getName());
                player.setDisplayName(player.getName());
            }
        }
        
        // Удаляем аббревиатуру
        abbreviations.remove(guild.getAbbreviation());
        guilds.remove(name);
        return true;
    }
    
    // Метод для удаления предметов гильдии у игрока
    public void removeGuildItems(Player player) {
        // Удаляем меч гильдии
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && item.getType() == Material.GOLDEN_SWORD) {
                ItemMeta meta = item.getItemMeta();
                if (meta != null && meta.hasDisplayName() && meta.getDisplayName().contains("Меч гильдии")) {
                    player.getInventory().remove(item);
                }
            }
        }
        
        // Удаляем палку изгнания
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && item.getType() == Material.STICK) {
                ItemMeta meta = item.getItemMeta();
                if (meta != null && meta.hasDisplayName() && 
                    meta.getDisplayName().equals(ChatColor.RED + "Палка изгнания")) {
                    player.getInventory().remove(item);
                }
            }
        }
        
        // Удаляем жезл назначения (если есть)
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && item.getType() == Material.BLAZE_ROD) {
                ItemMeta meta = item.getItemMeta();
                if (meta != null && meta.hasDisplayName() && 
                    meta.getDisplayName().equals(ChatColor.GOLD + "Жезл назначения")) {
                    player.getInventory().remove(item);
                }
            }
        }
    }

    public boolean addToGuild(UUID player, String guildName) {
        Guild guild = guilds.get(guildName);
        if (guild == null || guild.isFull()) return false;
        
        if (guild.addMember(player)) {
            playerGuilds.put(player, guildName);
            
            Player bukkitPlayer = Bukkit.getPlayer(player);
            if (bukkitPlayer != null && bukkitPlayer.isOnline()) {
                bukkitPlayer.setPlayerListName(guild.getPrefix() + bukkitPlayer.getName());
                bukkitPlayer.setDisplayName(guild.getPrefix() + bukkitPlayer.getName());
            }
            
            return true;
        }
        return false;
    }

    public boolean removeFromGuild(UUID player) {
        String guildName = playerGuilds.get(player);
        if (guildName == null) return false;
        
        Guild guild = guilds.get(guildName);
        if (guild == null) return false;
        
        if (guild.removeMember(player)) {
            playerGuilds.remove(player);
            
            Player bukkitPlayer = Bukkit.getPlayer(player);
            if (bukkitPlayer != null && bukkitPlayer.isOnline()) {
                bukkitPlayer.setPlayerListName(bukkitPlayer.getName());
                bukkitPlayer.setDisplayName(bukkitPlayer.getName());
            }
            
            return true;
        }
        return false;
    }

    public List<Guild> getGuilds() {
        return new ArrayList<>(guilds.values());
    }

    @SuppressWarnings("unchecked")
    public void loadGuilds() {
        Map[] data = plugin.getPluginConfig().loadGuilds();
        if (data != null && data.length >= 3) {
            this.guilds = (Map<String, Guild>) data[0];
            this.playerGuilds = (Map<UUID, String>) data[1];
            this.abbreviations = (Map<String, String>) data[2];
        }
    }

    public void saveGuilds() {
        plugin.getPluginConfig().saveGuilds(guilds, playerGuilds, abbreviations);
    }
}