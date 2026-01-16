package me.smokietrue.unikguild;

import org.bukkit.*;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import java.util.*;

public class GuildManager {
    private final Main plugin;
    private final Config config;
    private Map<String, Guild> guilds = new HashMap<>();
    private Map<UUID, String> playerGuilds = new HashMap<>();
    private Map<UUID, String> pendingInvitations = new HashMap<>();

    public GuildManager(Main plugin, Config config) {
        this.plugin = plugin;
        this.config = config;
        loadData();
    }

    @SuppressWarnings("unchecked")
    private void loadData() {
        Map[] loadedData = config.loadGuilds();
        this.guilds = (Map<String, Guild>) loadedData[0];
        this.playerGuilds = (Map<UUID, String>) loadedData[1];
    }

    private void saveData() {
        config.saveGuilds(guilds, playerGuilds, new HashMap<>());
    }

    public boolean createGuild(Player founder, String name, String colorCode) {
        if (guilds.containsKey(name)) return false;
        Guild guild = new Guild(name, founder.getUniqueId(), colorCode, 15);
        guilds.put(name, guild);
        playerGuilds.put(founder.getUniqueId(), name);
        saveData();
        updatePlayerDisplay(founder, guild);
        return true;
    }

    public void sendInvitation(Player inviter, Player target) {
        UUID inviterId = inviter.getUniqueId();
        String guildName = playerGuilds.get(inviterId);
        if (guildName == null) {
            inviter.sendMessage(ChatColor.RED + "Вы не состоите в гильдии.");
            return;
        }
        
        Guild guild = guilds.get(guildName);
        if (guild == null || !guild.isFounder(inviterId)) {
            inviter.sendMessage(ChatColor.RED + "Только основатель гильдии может приглашать.");
            return;
        }

        // ПРОВЕРКА: если target уже в гильдии
        if (playerGuilds.containsKey(target.getUniqueId())) {
            inviter.sendMessage(ChatColor.RED + "Игрок " + target.getName() + " уже состоит в гильдии!");
            return;
        }

        pendingInvitations.put(target.getUniqueId(), guildName);
        ChatColor guildColor = ChatColor.valueOf(guild.getColor());

        // Создаем кликабельные кнопки
        net.md_5.bungee.api.chat.TextComponent acceptButton = new net.md_5.bungee.api.chat.TextComponent("[✔ ПРИНЯТЬ]");
        acceptButton.setColor(net.md_5.bungee.api.ChatColor.GREEN);
        acceptButton.setBold(true);
        acceptButton.setClickEvent(new net.md_5.bungee.api.chat.ClickEvent(
            net.md_5.bungee.api.chat.ClickEvent.Action.RUN_COMMAND, "/g accept"
        ));
        acceptButton.setHoverEvent(new net.md_5.bungee.api.chat.HoverEvent(
            net.md_5.bungee.api.chat.HoverEvent.Action.SHOW_TEXT,
            new net.md_5.bungee.api.chat.TextComponent[]{
                new net.md_5.bungee.api.chat.TextComponent("Нажмите, чтобы принять приглашение")
            }
        ));

        net.md_5.bungee.api.chat.TextComponent denyButton = new net.md_5.bungee.api.chat.TextComponent("[✘ ОТКЛОНИТЬ]");
        denyButton.setColor(net.md_5.bungee.api.ChatColor.RED);
        denyButton.setBold(true);
        denyButton.setClickEvent(new net.md_5.bungee.api.chat.ClickEvent(
            net.md_5.bungee.api.chat.ClickEvent.Action.RUN_COMMAND, "/g deny"
        ));
        denyButton.setHoverEvent(new net.md_5.bungee.api.chat.HoverEvent(
            net.md_5.bungee.api.chat.HoverEvent.Action.SHOW_TEXT,
            new net.md_5.bungee.api.chat.TextComponent[]{
                new net.md_5.bungee.api.chat.TextComponent("Нажмите, чтобы отклонить приглашение")
            }
        ));

        net.md_5.bungee.api.chat.TextComponent separator = new net.md_5.bungee.api.chat.TextComponent(" - ");
        separator.setColor(net.md_5.bungee.api.ChatColor.GRAY);

        net.md_5.bungee.api.chat.TextComponent message = new net.md_5.bungee.api.chat.TextComponent();
        message.addExtra("\n");
        message.addExtra("══════════════════════════════════\n");
        message.addExtra("  ВАС ПРИГЛАШАЮТ В ГИЛЬДИЮ!\n");
        message.addExtra("  Название: ");
        net.md_5.bungee.api.chat.TextComponent guildNameText = new net.md_5.bungee.api.chat.TextComponent(guild.getName());
        guildNameText.setColor(net.md_5.bungee.api.ChatColor.WHITE);
        message.addExtra(guildNameText);
        message.addExtra("\n  Основатель: ");
        net.md_5.bungee.api.chat.TextComponent founderText = new net.md_5.bungee.api.chat.TextComponent(inviter.getName());
        founderText.setColor(net.md_5.bungee.api.ChatColor.WHITE);
        message.addExtra(founderText);
        message.addExtra("\n\n  Нажмите, чтобы ответить:\n  ");
        message.addExtra(acceptButton);
        message.addExtra(separator);
        message.addExtra(denyButton);
        message.addExtra("\n══════════════════════════════════\n");

        target.spigot().sendMessage(message);
        inviter.sendMessage(guildColor + "Приглашение отправлено игроку " + target.getName());
    }

    public void acceptInvitation(Player player) {
        UUID playerId = player.getUniqueId();
        String guildName = pendingInvitations.get(playerId);
        if (guildName == null) {
            player.sendMessage(ChatColor.RED + "У вас нет активных приглашений.");
            return;
        }
        
        // Дополнительная проверка: если игрок уже в гильдии
        if (playerGuilds.containsKey(playerId)) {
            player.sendMessage(ChatColor.RED + "Вы уже состоите в гильдии!");
            pendingInvitations.remove(playerId);
            return;
        }
        
        Guild guild = guilds.get(guildName);
        if (guild == null || guild.isFull()) {
            player.sendMessage(ChatColor.RED + "Приглашение устарело или гильдия заполнена.");
            pendingInvitations.remove(playerId);
            return;
        }
        
        // Снимаем прочность с меча основателя
        Player founder = Bukkit.getPlayer(guild.getFounder());
        if (founder != null && founder.isOnline()) {
            ItemStack sword = founder.getInventory().getItemInMainHand();
            if (sword.getType() == Material.GOLDEN_SWORD) {
                plugin.getItemManager().decrementUses(sword);
            }
        }

        guild.addMember(playerId);
        playerGuilds.put(playerId, guildName);
        saveData();
        updatePlayerDisplay(player, guild);
        pendingInvitations.remove(playerId);
        player.sendMessage(ChatColor.GREEN + "Вы вступили в гильдию " + guild.getName() + "!");
        guild.broadcast(ChatColor.GREEN + player.getName() + " вступил в гильдию.");
    }

    public void denyInvitation(Player player) {
        if (pendingInvitations.remove(player.getUniqueId()) != null) {
            player.sendMessage(ChatColor.YELLOW + "Вы отклонили приглашение в гильдию.");
        }
    }

    public void sendGuildInfo(Player player) {
        String guildName = playerGuilds.get(player.getUniqueId());
        if (guildName == null) {
            player.sendMessage(ChatColor.RED + "Вы не состоите в гильдии.");
            return;
        }
        Guild guild = guilds.get(guildName);
        if (guild == null) return;

        ChatColor guildColor = ChatColor.valueOf(guild.getColor());
        player.sendMessage(guildColor + "══════════════════════════════════");
        player.sendMessage(guildColor + "  ИНФОРМАЦИЯ О ГИЛЬДИИ");
        player.sendMessage(guildColor + "  Название: " + ChatColor.WHITE + guild.getName());
        player.sendMessage(guildColor + "  Основатель: " + ChatColor.WHITE +
                Bukkit.getOfflinePlayer(guild.getFounder()).getName());
        player.sendMessage(guildColor + "  Участников: " + ChatColor.WHITE +
                guild.getMembers().size() + "/" + guild.getMaxMembers());
        player.sendMessage("");
        
        // Кастомные роли гильдии
        player.sendMessage(guildColor + "  Доступные роли:");
        for (String role : guild.getCustomRoles()) {
            player.sendMessage(ChatColor.GRAY + "   • " + ChatColor.WHITE + role);
        }
        player.sendMessage("");
        
        player.sendMessage(guildColor + "  Участники:");
        for (UUID memberId : guild.getMembers()) {
            String memberName = Bukkit.getOfflinePlayer(memberId).getName();
            String role = guild.getRole(memberId);
            player.sendMessage(ChatColor.GRAY + "   • " +
                    (memberId.equals(guild.getFounder()) ? ChatColor.GOLD + memberName + " (Основатель)" :
                            ChatColor.WHITE + memberName + " (" + role + ")"));
        }
        player.sendMessage(guildColor + "══════════════════════════════════");
    }

    public void createRole(Player player, String roleName) {
        String guildName = playerGuilds.get(player.getUniqueId());
        if (guildName == null) {
            player.sendMessage(ChatColor.RED + "Вы не состоите в гильдии.");
            return;
        }
        
        Guild guild = guilds.get(guildName);
        if (guild == null || !guild.isFounder(player.getUniqueId())) {
            player.sendMessage(ChatColor.RED + "Только основатель гильдии может создавать роли.");
            return;
        }
        
        if (roleName.length() < 2 || roleName.length() > 16) {
            player.sendMessage(ChatColor.RED + "Название роли должно быть от 2 до 16 символов.");
            return;
        }
        
        if (guild.addCustomRole(roleName)) {
            saveData();
            player.sendMessage(ChatColor.GREEN + "Роль '" + roleName + "' успешно создана!");
        } else {
            player.sendMessage(ChatColor.RED + "Не удалось создать роль. Возможно, она уже существует.");
        }
    }

    public void assignRole(Player assigner, String targetName, String roleName) {
        String guildName = playerGuilds.get(assigner.getUniqueId());
        if (guildName == null) {
            assigner.sendMessage(ChatColor.RED + "Вы не состоите в гильдии.");
            return;
        }
        
        Guild guild = guilds.get(guildName);
        if (guild == null || !guild.isFounder(assigner.getUniqueId())) {
            assigner.sendMessage(ChatColor.RED + "Только основатель гильдии может назначать роли.");
            return;
        }
        
        Player target = Bukkit.getPlayer(targetName);
        if (target == null) {
            assigner.sendMessage(ChatColor.RED + "Игрок не найден или не в сети.");
            return;
        }
        
        if (!guild.getMembers().contains(target.getUniqueId())) {
            assigner.sendMessage(ChatColor.RED + "Этот игрок не состоит в вашей гильдии.");
            return;
        }
        
        if (target.getUniqueId().equals(guild.getFounder())) {
            assigner.sendMessage(ChatColor.RED + "Нельзя изменить роль основателя.");
            return;
        }
        
        if (!guild.getCustomRoles().contains(roleName) && !roleName.equals("Основатель")) {
            assigner.sendMessage(ChatColor.RED + "Роль '" + roleName + "' не существует в вашей гильдии.");
            return;
        }
        
        if (guild.assignRole(target.getUniqueId(), roleName)) {
            saveData();
            assigner.sendMessage(ChatColor.GREEN + "Игроку " + target.getName() + " назначена роль '" + roleName + "'!");
            target.sendMessage(ChatColor.GREEN + "Вам назначена роль '" + roleName + "' в гильдии " + guild.getName() + "!");
        } else {
            assigner.sendMessage(ChatColor.RED + "Не удалось назначить роль.");
        }
    }

    public void disbandGuild(Player player) {
        String guildName = playerGuilds.get(player.getUniqueId());
        if (guildName == null) {
            player.sendMessage(ChatColor.RED + "Вы не состоите в гильдии.");
            return;
        }
        Guild guild = guilds.get(guildName);
        if (guild == null || !guild.isFounder(player.getUniqueId())) {
            player.sendMessage(ChatColor.RED + "Только основатель гильдии может её распустить.");
            return;
        }
        
        // Удаляем предметы гильдии у всех участников
        for (UUID memberId : guild.getMembers()) {
            Player member = Bukkit.getPlayer(memberId);
            if (member != null && member.isOnline()) {
                member.sendMessage(ChatColor.RED + "Гильдия '" + guild.getName() + "' была распущена основателем.");
                clearPlayerDisplay(member);
                removeGuildItems(member);
            }
        }
        
        for (UUID memberId : guild.getMembers()) {
            playerGuilds.remove(memberId);
        }
        guilds.remove(guildName);
        saveData();
        player.sendMessage(ChatColor.RED + "Вы распустили гильдию '" + guildName + "'.");
    }
    
    private void removeGuildItems(Player player) {
        // Удаляем меч гильдии
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && item.getType() == Material.GOLDEN_SWORD && item.hasItemMeta()) {
                ItemMeta meta = item.getItemMeta();
                if (meta != null && meta.hasDisplayName() && meta.getDisplayName().contains("Меч гильдии")) {
                    player.getInventory().remove(item);
                }
            }
        }
        
        // Удаляем палку изгнания
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && item.getType() == Material.STICK && item.hasItemMeta()) {
                ItemMeta meta = item.getItemMeta();
                if (meta != null && meta.hasDisplayName() && meta.getDisplayName().contains("Палка изгнания")) {
                    player.getInventory().remove(item);
                }
            }
        }
    }

    public void updatePlayerDisplay(Player player, Guild guild) {
        ChatColor color = ChatColor.valueOf(guild.getColor());
        String tabName = color + player.getName();
        player.setPlayerListName(tabName);
    }

    public void updatePlayerDisplayOnJoin(Player player) {
        String guildName = playerGuilds.get(player.getUniqueId());
        if (guildName != null) {
            Guild guild = guilds.get(guildName);
            if (guild != null) {
                updatePlayerDisplay(player, guild);
            }
        }
    }

    private void clearPlayerDisplay(Player player) {
        player.setPlayerListName(null);
        player.setDisplayName(player.getName());
    }

    public Guild getGuild(String name) { return guilds.get(name); }
    public Guild getGuildByPlayer(UUID playerId) {
        String guildName = playerGuilds.get(playerId);
        return guildName != null ? guilds.get(guildName) : null;
    }
    
    public List<String> getGuildNames() {
        return new ArrayList<>(guilds.keySet());
    }
    
    public void kickMember(Player kicker, String targetName) {
        Guild guild = getGuildByPlayer(kicker.getUniqueId());
        if (guild == null || !guild.isFounder(kicker.getUniqueId())) {
            kicker.sendMessage(ChatColor.RED + "Только основатель может исключать участников.");
            return;
        }
        Player target = Bukkit.getPlayer(targetName);
        if (target == null) {
            kicker.sendMessage(ChatColor.RED + "Игрок не найден.");
            return;
        }
        if (!guild.getMembers().contains(target.getUniqueId())) {
            kicker.sendMessage(ChatColor.RED + "Этот игрок не в вашей гильдии.");
            return;
        }
        guild.removeMember(target.getUniqueId());
        playerGuilds.remove(target.getUniqueId());
        saveData();
        clearPlayerDisplay(target);
        removeGuildItems(target); // Удаляем предметы при кике
        kicker.sendMessage(ChatColor.GREEN + "Игрок " + targetName + " исключен из гильдии.");
        target.sendMessage(ChatColor.RED + "Вы были исключены из гильдии " + guild.getName() + ".");
    }
    
    public void leaveGuild(Player player) {
        Guild guild = getGuildByPlayer(player.getUniqueId());
        if (guild == null) {
            player.sendMessage(ChatColor.RED + "Вы не состоите в гильдии.");
            return;
        }
        if (guild.isFounder(player.getUniqueId())) {
            player.sendMessage(ChatColor.RED + "Основатель не может покинуть гильдию. Используйте /g disband.");
            return;
        }
        guild.removeMember(player.getUniqueId());
        playerGuilds.remove(player.getUniqueId());
        saveData();
        clearPlayerDisplay(player);
        removeGuildItems(player); // Удаляем предметы при выходе
        player.sendMessage(ChatColor.YELLOW + "Вы покинули гильдию " + guild.getName() + ".");
        guild.broadcast(ChatColor.YELLOW + player.getName() + " покинул гильдию.");
    }
    
    public void sendGuildsList(CommandSender sender) {
        if (guilds.isEmpty()) {
            sender.sendMessage(ChatColor.YELLOW + "На сервере нет гильдий.");
            return;
        }
        sender.sendMessage(ChatColor.GOLD + "══════════════════════════════════");
        sender.sendMessage(ChatColor.YELLOW + "Список гильдий:");
        for (Guild guild : guilds.values()) {
            ChatColor color = ChatColor.valueOf(guild.getColor());
            sender.sendMessage(color + " • " + guild.getName() + ChatColor.GRAY + 
                    " (" + guild.getMembers().size() + "/" + guild.getMaxMembers() + ")");
        }
        sender.sendMessage(ChatColor.GOLD + "══════════════════════════════════");
    }
    
    public void deleteGuild(CommandSender sender, String guildName) {
        if (!guilds.containsKey(guildName)) {
            sender.sendMessage(ChatColor.RED + "Гильдия не найдена.");
            return;
        }
        Guild guild = guilds.get(guildName);
        for (UUID memberId : guild.getMembers()) {
            playerGuilds.remove(memberId);
            Player member = Bukkit.getPlayer(memberId);
            if (member != null && member.isOnline()) {
                member.sendMessage(ChatColor.RED + "Ваша гильдия была удалена администратором.");
                clearPlayerDisplay(member);
                removeGuildItems(member);
            }
        }
        guilds.remove(guildName);
        saveData();
        sender.sendMessage(ChatColor.GREEN + "Гильдия '" + guildName + "' удалена.");
    }
}