package me.smokietrue.unikguild;

import org.bukkit.entity.Player;

public class UnikGuildAPI {
    private final Main plugin;

    public UnikGuildAPI(Main plugin) {
        this.plugin = plugin;
    }

    public boolean isPlayerInGuild(Player player) {
        return plugin.getGuildManager().getGuildByPlayer(player.getUniqueId()) != null;
    }
    
    public String getPlayerGuildName(Player player) {
        Guild guild = plugin.getGuildManager().getGuildByPlayer(player.getUniqueId());
        return guild != null ? guild.getName() : null;
    }
    
    public String getPlayerGuildColor(Player player) {
        Guild guild = plugin.getGuildManager().getGuildByPlayer(player.getUniqueId());
        return guild != null ? guild.getColor() : null;
    }
}