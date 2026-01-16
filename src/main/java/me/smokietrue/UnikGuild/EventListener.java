package me.smokietrue.unikguild;

import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import java.util.*;

public class EventListener implements Listener {
    private Main plugin;
    public Map<UUID, String> creatingGuild = new HashMap<>();
    public Map<UUID, String> awaitingColor = new HashMap<>();

    public EventListener(Main plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItemInMainHand();

        if (item.getType() == Material.GOLDEN_SWORD && item.hasItemMeta()) {
            event.setCancelled(true);
            
            if (creatingGuild.containsKey(player.getUniqueId())) {
                return;
            }
            
            ItemMeta meta = item.getItemMeta();
            if (meta != null && meta.hasDisplayName()) {
                String displayName = meta.getDisplayName();
                if (displayName.equals(ChatColor.GOLD + "Меч гильдии") || 
                    displayName.equals("Меч гильдии")) {
                    if (!creatingGuild.containsKey(player.getUniqueId())) {
                        startGuildCreation(player);
                        return;
                    }
                }
            }
            
            plugin.getGuildManager().sendGuildInfo(player);
        }
    }

    @EventHandler
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player)) return;
        
        Player damager = (Player) event.getDamager();
        ItemStack weapon = damager.getInventory().getItemInMainHand();

        if (weapon.getType() == Material.GOLDEN_SWORD && weapon.hasItemMeta()) {
            // Обработка меча гильдии
            event.setCancelled(true);
            
            if (event.getEntity() instanceof Player) {
                Player target = (Player) event.getEntity();
                plugin.getGuildManager().sendInvitation(damager, target);
            }
        } else if (weapon.getType() == Material.STICK && weapon.hasItemMeta()) {
            // Обработка палки изгнания
            event.setCancelled(true);
            
            if (event.getEntity() instanceof Player) {
                Player target = (Player) event.getEntity();
                
                // Проверяем, что это палка изгнания
                ItemMeta meta = weapon.getItemMeta();
                if (meta != null && meta.hasDisplayName() && meta.getDisplayName().contains("Палка изгнания")) {
                    
                    // Проверяем, что дамагер - основатель гильдии
                    Guild guild = plugin.getGuildManager().getGuildByPlayer(damager.getUniqueId());
                    if (guild == null || !guild.isFounder(damager.getUniqueId())) {
                        damager.sendMessage(ChatColor.RED + "Только основатель гильдии может использовать палку изгнания!");
                        return;
                    }
                    
                    // Проверяем, что цель - участник той же гильдии
                    if (!guild.getMembers().contains(target.getUniqueId())) {
                        damager.sendMessage(ChatColor.RED + "Этот игрок не состоит в вашей гильдии!");
                        return;
                    }
                    
                    // Нельзя исключить самого себя
                    if (target.getUniqueId().equals(damager.getUniqueId())) {
                        damager.sendMessage(ChatColor.RED + "Вы не можете исключить самого себя!");
                        return;
                    }
                    
                    // Нельзя исключить основателя
                    if (guild.isFounder(target.getUniqueId())) {
                        damager.sendMessage(ChatColor.RED + "Вы не можете исключить основателя гильдии!");
                        return;
                    }
                    
                    // Исключаем игрока
                    plugin.getGuildManager().kickMember(damager, target.getName());
                }
            }
        }
    }

    private void startGuildCreation(Player player) {
        creatingGuild.put(player.getUniqueId(), "awaiting_name");
        player.sendMessage(ChatColor.GOLD + "══════════════════════════════════");
        player.sendMessage(ChatColor.YELLOW + " Введите название гильдии:");
        player.sendMessage("");
        player.sendMessage(ChatColor.GREEN + " • От 2 до 16 символов");
        player.sendMessage(ChatColor.GREEN + " • Уникальное название");
        player.sendMessage("");
        player.sendMessage(ChatColor.GRAY + " Просто напишите в чат");
        player.sendMessage(ChatColor.GOLD + "══════════════════════════════════");
    }

    @EventHandler
    public void onPlayerChat(PlayerChatEvent event) {
        Player player = event.getPlayer();
        String message = event.getMessage().trim();

        if (creatingGuild.containsKey(player.getUniqueId()) && 
            creatingGuild.get(player.getUniqueId()).equals("awaiting_name")) {
            event.setCancelled(true);
            if (message.length() < 2 || message.length() > 16) {
                player.sendMessage(ChatColor.RED + "[⚡] Название гильдии должно быть от 2 до 16 символов!");
                creatingGuild.remove(player.getUniqueId());
                return;
            }
            if (plugin.getGuildManager().getGuild(message) != null) {
                player.sendMessage(ChatColor.RED + "[⚡] Гильдия с таким названием уже существует!");
                creatingGuild.remove(player.getUniqueId());
                return;
            }
            creatingGuild.put(player.getUniqueId(), message);
            awaitingColor.put(player.getUniqueId(), "waiting");
            
            sendColorSelection(player, message);
            return;
        }

        if (awaitingColor.containsKey(player.getUniqueId()) && 
            creatingGuild.containsKey(player.getUniqueId())) {
            event.setCancelled(true);
            String guildName = creatingGuild.get(player.getUniqueId());
            if (guildName.equals("awaiting_name")) {
                player.sendMessage(ChatColor.RED + "[⚡] Ошибка: сначала введите название гильдии!");
                creatingGuild.remove(player.getUniqueId());
                awaitingColor.remove(player.getUniqueId());
                return;
            }
            String colorCode = getColorByNumber(message);
            if (colorCode == null) {
                player.sendMessage(ChatColor.RED + "[⚡] Неверный выбор! Введите цифру от 1 до 8:");
                return;
            }
            completeGuildCreation(player, guildName, colorCode);
            return;
        }

        if (message.startsWith("- ") && message.length() > 2) {
            event.setCancelled(true);
            handleGuildChat(player, message.substring(2).trim());
        }
    }

    private void sendColorSelection(Player player, String guildName) {
        player.sendMessage("");
        player.sendMessage(ChatColor.GOLD + "══════════════════════════════════");
        player.sendMessage(ChatColor.YELLOW + " Выберите цвет гильдии:");
        player.sendMessage("");
        
        String[] colors = {"RED", "BLUE", "GREEN", "YELLOW", "LIGHT_PURPLE", "AQUA", "WHITE", "DARK_PURPLE"};
        String[] colorNames = {"Красный", "Синий", "Зеленый", "Желтый", "Фиолетовый", "Голубой", "Белый", "Темно-фиолетовый"};
        
        for (int i = 0; i < colors.length; i++) {
            net.md_5.bungee.api.chat.TextComponent colorButton = new net.md_5.bungee.api.chat.TextComponent("  " + (i+1) + ". " + colorNames[i]);
            colorButton.setColor(net.md_5.bungee.api.ChatColor.valueOf(colors[i]));
            colorButton.setClickEvent(new net.md_5.bungee.api.chat.ClickEvent(
                net.md_5.bungee.api.chat.ClickEvent.Action.RUN_COMMAND, "/guildcolor " + colors[i]
            ));
            colorButton.setHoverEvent(new net.md_5.bungee.api.chat.HoverEvent(
                net.md_5.bungee.api.chat.HoverEvent.Action.SHOW_TEXT,
                new net.md_5.bungee.api.chat.TextComponent[]{
                    new net.md_5.bungee.api.chat.TextComponent(ChatColor.GOLD + "Нажмите, чтобы выбрать " + colorNames[i])
                }
            ));
            
            player.spigot().sendMessage(colorButton);
        }
        
        player.sendMessage("");
        player.sendMessage(ChatColor.GRAY + " Нажмите на цвет или введите цифру 1-8");
        player.sendMessage(ChatColor.GOLD + "══════════════════════════════════");
        player.sendMessage("");
    }

    private void handleGuildChat(Player player, String message) {
        Guild guild = plugin.getGuildManager().getGuildByPlayer(player.getUniqueId());
        if (guild == null) {
            player.sendMessage(ChatColor.RED + "[⚡] Вы не состоите в гильдии!");
            return;
        }
        
        ChatColor color = ChatColor.valueOf(guild.getColor());
        String role = guild.getRole(player.getUniqueId());
        String formattedMessage = ChatColor.ITALIC + "" + color + "[" + guild.getName() + "] " + 
                                  ChatColor.RESET + player.getName() + " (" + role + ")" + 
                                  ChatColor.ITALIC + ": " + ChatColor.WHITE + message;
        
        for (UUID memberId : guild.getMembers()) {
            Player member = Bukkit.getPlayer(memberId);
            if (member != null && member.isOnline()) {
                member.sendMessage(formattedMessage);
            }
        }
        Bukkit.getConsoleSender().sendMessage(ChatColor.GRAY + "[GuildChat] " + guild.getName() + " | " +  
                                              player.getName() + ": " + message);
    }

    public void completeGuildCreation(Player player, String guildName, String colorCode) {
        try {
            if (!plugin.getGuildManager().createGuild(player, guildName, colorCode)) {
                player.sendMessage(ChatColor.RED + "[⚡] Не удалось создать гильдию!");
                return;
            }
            
            ItemStack sword = player.getInventory().getItemInMainHand();
            if (sword.getType() == Material.GOLDEN_SWORD) {
                ItemMeta meta = sword.getItemMeta();
                if (meta != null) {
                    meta.setDisplayName(ChatColor.valueOf(colorCode) + "Меч гильдии " + guildName);
                    meta.setLore(Arrays.asList(
                        ChatColor.valueOf(colorCode) + "Использований: 14/15",
                        "",
                        ChatColor.valueOf(colorCode) + "ПКМ: Информация о гильдии",
                        ChatColor.valueOf(colorCode) + "ЛКМ по игроку: Приглашить в гильдию"
                    ));
                    sword.setItemMeta(meta);
                }
            }
            
            player.getInventory().addItem(plugin.getItemManager().createKickStick());
            player.sendMessage(ChatColor.valueOf(colorCode) + "[⚡] Гильдия '" + guildName + "' создана!");
            player.sendMessage(ChatColor.GRAY + "Используйте '- текст' для чата гильдии");
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
            player.spawnParticle(Particle.FIREWORK, player.getLocation(), 50);
        } catch (Exception e) {
            player.sendMessage(ChatColor.RED + "[⚡] Ошибка при создании гильдии!");
            e.printStackTrace();
        } finally {
            creatingGuild.remove(player.getUniqueId());
            awaitingColor.remove(player.getUniqueId());
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        plugin.getGuildManager().updatePlayerDisplayOnJoin(player);
    }

    private String getColorByNumber(String number) {
        switch(number) {
            case "1": return "RED";
            case "2": return "BLUE";
            case "3": return "GREEN";
            case "4": return "YELLOW";
            case "5": return "LIGHT_PURPLE";
            case "6": return "AQUA";
            case "7": return "WHITE";
            case "8": return "DARK_PURPLE";
            default: return null;
        }
    }
}