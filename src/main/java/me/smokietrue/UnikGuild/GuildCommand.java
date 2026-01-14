package me.smokietrue.unikguild;

import org.bukkit.*;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;

public class GuildCommand implements CommandExecutor {
    private Main plugin;
    
    public GuildCommand(Main plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (cmd.getName().equalsIgnoreCase("guilds")) {
            if (!(sender instanceof Player)) {
                sender.sendMessage(ChatColor.RED + "[⚡] Команды только для игроков!");
                return true;
            }
            
            Player player = (Player) sender;
            
            if (!player.hasPermission("unikguild.admin")) {
                player.sendMessage(ChatColor.RED + "[⚡] Недостаточно прав!");
                return true;
            }
            
            if (args.length == 0) {
                player.sendMessage(ChatColor.GOLD + "[⚡] Админ-команды гильдий:");
                player.sendMessage(ChatColor.YELLOW + "/guilds list - список всех гильдий");
                player.sendMessage(ChatColor.YELLOW + "/guilds delete <название> - удалить гильдию");
                player.sendMessage(ChatColor.YELLOW + "/guilds give <ник> - выдать меч гильдии");
                return true;
            }
            
            switch(args[0].toLowerCase()) {
                case "list":
                    player.sendMessage(ChatColor.GOLD + "Список гильдий:");
                    for (Guild guild : plugin.getGuildManager().getGuilds()) {
                        player.sendMessage(ChatColor.YELLOW + "- " + guild.getName() + 
                                         " (Участников: " + guild.getMemberCount() + "/" + guild.getMaxMembers() + ")");
                    }
                    break;
                case "delete":
                    if (args.length < 2) {
                        player.sendMessage(ChatColor.RED + "Использование: /guilds delete <название>");
                        return true;
                    }
                    if (plugin.getGuildManager().deleteGuild(args[1])) {
                        player.sendMessage(ChatColor.GREEN + "Гильдия удалена!");
                    } else {
                        player.sendMessage(ChatColor.RED + "Гильдия не найдена!");
                    }
                    break;
                case "give":
                    if (args.length < 2) {
                        player.sendMessage(ChatColor.RED + "Использование: /guilds give <ник>");
                        return true;
                    }
                    Player target = Bukkit.getPlayer(args[1]);
                    if (target == null) {
                        player.sendMessage(ChatColor.RED + "Игрок не найден!");
                        return true;
                    }
                    target.getInventory().addItem(plugin.getItemManager().createGuildSword("", "WHITE", 14));
                    player.sendMessage(ChatColor.GREEN + "Меч гильдии выдан!");
                    break;
            }
            return true;
        }
        
        if (cmd.getName().equalsIgnoreCase("g")) {
            if (!(sender instanceof Player)) {
                sender.sendMessage(ChatColor.RED + "[⚡] Команды только для игроков!");
                return true;
            }
            
            Player player = (Player) sender;
            
            if (args.length == 0) {
                player.sendMessage(ChatColor.GOLD + "[⚡] Команды гильдии:");
                player.sendMessage(ChatColor.YELLOW + "/g accept - принять приглашение");
                player.sendMessage(ChatColor.YELLOW + "/g deny - отказаться от приглашения");
                player.sendMessage(ChatColor.YELLOW + "/g members - участники гильдии");
                player.sendMessage(ChatColor.YELLOW + "/g kick <ник> - исключить участника");
                player.sendMessage(ChatColor.YELLOW + "/g leave - выйти из гильдии");
                player.sendMessage(ChatColor.YELLOW + "/g info - информация о гильдии");
                return true;
            }
            
            switch(args[0].toLowerCase()) {
                case "accept":
                    handleAccept(player);
                    break;
                case "deny":
                    handleDeny(player);
                    break;
                case "members":
                    handleMembers(player);
                    break;
                case "kick":
                    handleKick(player, args.length > 1 ? args[1] : null);
                    break;
                case "leave":
                    handleLeave(player);
                    break;
                case "info":
                    handleInfo(player);
                    break;
                default:
                    player.sendMessage(ChatColor.RED + "[⚡] Неизвестная команда!");
                    break;
            }
        }
        
        return true;
    }
    
    private void handleAccept(Player player) {
        Map<UUID, String> invitationTarget = plugin.getEventListener().invitationTarget;
        String guildName = invitationTarget.get(player.getUniqueId());
        if (guildName == null) {
            player.sendMessage(ChatColor.RED + "[⚡] У вас нет приглашений!");
            return;
        }
        
        if (plugin.getGuildManager().addToGuild(player.getUniqueId(), guildName)) {
            Guild guild = plugin.getGuildManager().getGuild(guildName);
            player.sendMessage(ChatColor.GREEN + "[⚡] Вы вступили в гильдию " + guild.getColoredName());
            player.spawnParticle(Particle.FIREWORK, player.getLocation(), 30);
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
        } else {
            player.sendMessage(ChatColor.RED + "[⚡] Не удалось вступить в гильдию!");
        }
        
        invitationTarget.remove(player.getUniqueId());
    }
    
    private void handleDeny(Player player) {
        Map<UUID, String> invitationTarget = plugin.getEventListener().invitationTarget;
        if (invitationTarget.remove(player.getUniqueId()) != null) {
            player.sendMessage(ChatColor.YELLOW + "[⚡] Вы отказались от приглашения.");
        } else {
            player.sendMessage(ChatColor.RED + "[⚡] У вас нет приглашений!");
        }
    }
    
    private void handleMembers(Player player) {
        Guild guild = plugin.getGuildManager().getGuildByPlayer(player.getUniqueId());
        if (guild == null) {
            player.sendMessage(ChatColor.RED + "[⚡] Вы не состоите в гильдии!");
            return;
        }
        
        player.sendMessage(ChatColor.GOLD + "══════════════════════════════════");
        player.sendMessage(ChatColor.YELLOW + "Участники гильдии " + guild.getColoredName() + ":");
        player.sendMessage("");
        
        for (Map.Entry<UUID, String> entry : guild.getRoles().entrySet()) {
            String playerName = Bukkit.getOfflinePlayer(entry.getKey()).getName();
            player.sendMessage(ChatColor.GREEN + "  " + playerName + " - " + entry.getValue());
        }
        
        player.sendMessage("");
        player.sendMessage(ChatColor.GRAY + "Всего: " + guild.getMemberCount() + "/" + guild.getMaxMembers());
        player.sendMessage(ChatColor.GOLD + "══════════════════════════════════");
    }
    
    private void handleKick(Player player, String targetName) {
        if (targetName == null) {
            player.sendMessage(ChatColor.RED + "[⚡] Использование: /g kick <ник>");
            return;
        }
        
        Guild guild = plugin.getGuildManager().getGuildByPlayer(player.getUniqueId());
        if (guild == null) {
            player.sendMessage(ChatColor.RED + "[⚡] Вы не состоите в гильдии!");
            return;
        }
        
        if (!guild.getFounder().equals(player.getUniqueId())) {
            player.sendMessage(ChatColor.RED + "[⚡] Только основатель может исключать!");
            return;
        }
        
        Player target = Bukkit.getPlayer(targetName);
        if (target == null) {
            player.sendMessage(ChatColor.RED + "[⚡] Игрок не найден!");
            return;
        }
        
        // Проверяем, что целевой игрок в той же гильдии
        Guild targetGuild = plugin.getGuildManager().getGuildByPlayer(target.getUniqueId());
        if (targetGuild == null || !targetGuild.getName().equals(guild.getName())) {
            player.sendMessage(ChatColor.RED + "[⚡] Этот игрок не состоит в вашей гильдии!");
            return;
        }
        
        // Нельзя исключить самого себя
        if (target.getUniqueId().equals(player.getUniqueId())) {
            player.sendMessage(ChatColor.RED + "[⚡] Вы не можете исключить самого себя!");
            return;
        }
        
        if (plugin.getGuildManager().removeFromGuild(target.getUniqueId())) {
            player.sendMessage(ChatColor.GREEN + "[⚡] Игрок " + targetName + " исключен!");
            target.sendMessage(ChatColor.RED + "[⚡] Вас исключили из гильдии!");
        } else {
            player.sendMessage(ChatColor.RED + "[⚡] Не удалось исключить игрока!");
        }
    }
    
    private void handleLeave(Player player) {
        if (plugin.getGuildManager().removeFromGuild(player.getUniqueId())) {
            player.sendMessage(ChatColor.YELLOW + "[⚡] Вы вышли из гильдии.");
        } else {
            player.sendMessage(ChatColor.RED + "[⚡] Вы не состоите в гильдии!");
        }
    }
    
    private void handleInfo(Player player) {
        Guild guild = plugin.getGuildManager().getGuildByPlayer(player.getUniqueId());
        if (guild == null) {
            player.sendMessage(ChatColor.RED + "[⚡] Вы не состоите в гильдии!");
            return;
        }
        
        String founderName = Bukkit.getOfflinePlayer(guild.getFounder()).getName();
        
        player.sendMessage(ChatColor.GOLD + "══════════════════════════════════");
        player.sendMessage(ChatColor.YELLOW + "Информация о гильдии:");
        player.sendMessage("");
        player.sendMessage(ChatColor.GREEN + "Название: " + guild.getColoredName());
        player.sendMessage(ChatColor.GREEN + "Аббревиатура: " + guild.getPrefix());
        player.sendMessage(ChatColor.GREEN + "Основатель: " + founderName);
        player.sendMessage(ChatColor.GREEN + "Участников: " + guild.getMemberCount() + "/" + guild.getMaxMembers());
        player.sendMessage(ChatColor.GREEN + "Цвет: " + guild.getColor());
        player.sendMessage("");
        player.sendMessage(ChatColor.GRAY + "Чат гильдии: - текст");
        player.sendMessage(ChatColor.GOLD + "══════════════════════════════════");
    }
}