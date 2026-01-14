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
    public Map<UUID, String> awaitingAbbreviation = new HashMap<>();
    public Map<UUID, String> invitationTarget = new HashMap<>();
    
    public EventListener(Main plugin) {
        this.plugin = plugin;
    }
    
    @EventHandler
    public void onPlayerChat(PlayerChatEvent event) {
        Player player = event.getPlayer();
        String message = event.getMessage().trim();
        
        // Обработка названия гильдии
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
            
            player.sendMessage(ChatColor.GOLD + "══════════════════════════════════");
            player.sendMessage(ChatColor.YELLOW + "   Выберите цвет гильдии:");
            player.sendMessage("");
            player.sendMessage(ChatColor.RED + "   1. " + ChatColor.RED + "Красный");
            player.sendMessage(ChatColor.BLUE + "   2. " + ChatColor.BLUE + "Синий");
            player.sendMessage(ChatColor.GREEN + "   3. " + ChatColor.GREEN + "Зеленый");
            player.sendMessage(ChatColor.YELLOW + "   4. " + ChatColor.YELLOW + "Желтый");
            player.sendMessage(ChatColor.LIGHT_PURPLE + "   5. " + ChatColor.LIGHT_PURPLE + "Фиолетовый");
            player.sendMessage(ChatColor.AQUA + "   6. " + ChatColor.AQUA + "Голубой");
            player.sendMessage(ChatColor.WHITE + "   7. " + ChatColor.WHITE + "Белый");
            player.sendMessage(ChatColor.DARK_PURPLE + "   8. " + ChatColor.DARK_PURPLE + "Темно-фиолетовый");
            player.sendMessage("");
            player.sendMessage(ChatColor.GRAY + "   Введите цифру 1-8 в чат");
            player.sendMessage(ChatColor.GOLD + "══════════════════════════════════");
            
            return;
        }
        
        // Обработка выбора цвета через цифру
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
            
            creatingGuild.put(player.getUniqueId(), guildName + "|" + colorCode);
            awaitingColor.remove(player.getUniqueId());
            awaitingAbbreviation.put(player.getUniqueId(), "waiting");
            
            player.sendMessage(ChatColor.GOLD + "══════════════════════════════════");
            player.sendMessage(ChatColor.YELLOW + "   Введите аббревиатуру гильдии:");
            player.sendMessage("");
            player.sendMessage(ChatColor.GREEN + "   • 3 символа (буквы или цифры)");
            player.sendMessage(ChatColor.GREEN + "   • Пример: ABC, 123, A1B");
            player.sendMessage("");
            player.sendMessage(ChatColor.GRAY + "   Аббревиатура будет отображаться в чате и TAB");
            player.sendMessage(ChatColor.GOLD + "══════════════════════════════════");
            
            return;
        }
        
        // Обработка выбора аббревиатуры
        if (awaitingAbbreviation.containsKey(player.getUniqueId()) && 
            creatingGuild.containsKey(player.getUniqueId())) {
            
            event.setCancelled(true);
            
            String[] guildData = creatingGuild.get(player.getUniqueId()).split("\\|");
            String guildName = guildData[0];
            String colorCode = guildData[1];
            
            if (message.length() != 3) {
                player.sendMessage(ChatColor.RED + "[⚡] Аббревиатура должна быть ровно 3 символа!");
                player.sendMessage(ChatColor.GRAY + "Пример: ABC, 123, A1B");
                return;
            }
            
            if (!message.matches("[A-Za-z0-9]{3}")) {
                player.sendMessage(ChatColor.RED + "[⚡] Аббревиатура должна содержать только буквы и цифры!");
                return;
            }
            
            if (plugin.getGuildManager().isAbbreviationUsed(message.toUpperCase())) {
                player.sendMessage(ChatColor.RED + "[⚡] Эта аббревиатура уже используется!");
                return;
            }
            
            completeGuildCreation(player, guildName, message.toUpperCase(), colorCode);
        }
        
        // Обработка чата гильдии
        if (message.startsWith("- ") && message.length() > 2) {
            handleGuildChat(player, message.substring(2).trim());
            event.setCancelled(true);
        }
    }
    
    private void handleGuildChat(Player player, String message) {
        Guild guild = plugin.getGuildManager().getGuildByPlayer(player.getUniqueId());
        
        if (guild == null) {
            player.sendMessage(ChatColor.RED + "[⚡] Вы не состоите в гильдии!");
            return;
        }
        
        String formattedMessage = guild.getPrefix() + player.getName() + ": " + ChatColor.WHITE + message;
        
        for (UUID memberId : guild.getMembers()) {
            Player member = Bukkit.getPlayer(memberId);
            if (member != null && member.isOnline()) {
                member.sendMessage(formattedMessage);
            }
        }
        
        Bukkit.getConsoleSender().sendMessage(ChatColor.GRAY + "[GuildChat] " + guild.getName() + " | " + 
                                               player.getName() + ": " + message);
    }
    
    public void completeGuildCreation(Player player, String guildName, String abbreviation, String colorCode) {
        try {
            if (!plugin.getGuildManager().createGuild(player, guildName, abbreviation, colorCode)) {
                player.sendMessage(ChatColor.RED + "[⚡] Не удалось создать гильдию!");
                return;
            }
            
            ItemStack sword = player.getInventory().getItemInMainHand();
            if (sword.getType() == Material.GOLDEN_SWORD) {
                ItemMeta meta = sword.getItemMeta();
                meta.setDisplayName(ChatColor.valueOf(colorCode) + "Меч гильдии " + guildName);
                meta.setLore(Arrays.asList(
                    ChatColor.valueOf(colorCode) + "Использований: 14/15",
                    "",
                    ChatColor.valueOf(colorCode) + "ПКМ: Информация о гильдии",
                    ChatColor.valueOf(colorCode) + "ЛКМ по игроку: Пригласить в гильдию"
                ));
                meta.setUnbreakable(true);
                sword.setItemMeta(meta);
            }
            
            player.getInventory().addItem(plugin.getItemManager().createKickStick());
            updatePlayerDisplay(player, guildName, abbreviation, colorCode);
            
            player.sendMessage(ChatColor.valueOf(colorCode) + "[⚡] Гильдия '" + guildName + "' создана!");
            player.sendMessage(ChatColor.GREEN + "Аббревиатура: " + ChatColor.valueOf(colorCode) + abbreviation);
            player.sendMessage(ChatColor.GRAY + "Используйте '- текст' для чата гильдии");
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
            player.spawnParticle(Particle.FIREWORK, player.getLocation(), 50);
            
        } catch (Exception e) {
            player.sendMessage(ChatColor.RED + "[⚡] Ошибка при создании гильдии!");
            e.printStackTrace();
        } finally {
            creatingGuild.remove(player.getUniqueId());
            awaitingColor.remove(player.getUniqueId());
            awaitingAbbreviation.remove(player.getUniqueId());
        }
    }
    
    private void updatePlayerDisplay(Player player, String guildName, String abbreviation, String colorCode) {
        String tabName = ChatColor.valueOf(colorCode) + "[" + abbreviation + "] " + 
                        ChatColor.RESET + player.getName();
        player.setPlayerListName(tabName);
        player.setDisplayName(ChatColor.valueOf(colorCode) + "[" + abbreviation + "] " + 
                             ChatColor.RESET + player.getName());
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
    
    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItemInMainHand();
        
        if (item.getType() == Material.GOLDEN_SWORD && 
            (event.getAction() == Action.RIGHT_CLICK_AIR || 
             event.getAction() == Action.RIGHT_CLICK_BLOCK)) {
            
            event.setCancelled(true);
            
            Guild guild = plugin.getGuildManager().getGuildByPlayer(player.getUniqueId());
            
            if (guild == null) {
                if (creatingGuild.containsKey(player.getUniqueId())) {
                    String stage = creatingGuild.get(player.getUniqueId());
                    if (stage.equals("awaiting_name")) {
                        player.sendMessage(ChatColor.GOLD + "[⚡] Введите название гильдии в чат:");
                    } else if (awaitingColor.containsKey(player.getUniqueId())) {
                        player.sendMessage(ChatColor.GOLD + "[⚡] Выберите цвет гильдии (цифра 1-8):");
                    } else if (awaitingAbbreviation.containsKey(player.getUniqueId())) {
                        player.sendMessage(ChatColor.GOLD + "[⚡] Введите аббревиатуру (3 символа):");
                    }
                } else {
                    player.sendMessage(ChatColor.GOLD + "[⚡] Введите название гильдии в чат:");
                    creatingGuild.put(player.getUniqueId(), "awaiting_name");
                }
            } else if (guild.getFounder().equals(player.getUniqueId())) {
                player.sendMessage(ChatColor.GREEN + "[⚡] Меч готов для приглашений. Ударьте игрока.");
            }
        }
    }
    
    @EventHandler
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof Player) {
            Player attacker = (Player) event.getDamager();
            ItemStack item = attacker.getInventory().getItemInMainHand();
            
            // Палка изгнания - работает при ударе (ЛКМ)
            if (item.getType() == Material.STICK && item.hasItemMeta()) {
                ItemMeta meta = item.getItemMeta();
                if (meta.hasDisplayName() && meta.getDisplayName().equals(ChatColor.RED + "Палка изгнания")) {
                    event.setCancelled(true);
                    
                    if (event.getEntity() instanceof Player) {
                        Player target = (Player) event.getEntity();
                        Guild attackerGuild = plugin.getGuildManager().getGuildByPlayer(attacker.getUniqueId());
                        Guild targetGuild = plugin.getGuildManager().getGuildByPlayer(target.getUniqueId());
                        
                        // Проверяем, что оба игрока в одной гильдии
                        if (attackerGuild != null && targetGuild != null && 
                            attackerGuild.getName().equals(targetGuild.getName())) {
                            
                            // Проверяем, что изгоняющий - основатель
                            if (attackerGuild.getFounder().equals(attacker.getUniqueId())) {
                                if (plugin.getGuildManager().removeFromGuild(target.getUniqueId())) {
                                    attacker.sendMessage(ChatColor.GREEN + "[⚡] Игрок " + target.getName() + " исключен!");
                                    target.sendMessage(ChatColor.RED + "[⚡] Вас исключили из гильдии!");
                                    target.spawnParticle(Particle.CAMPFIRE_COSY_SMOKE, target.getLocation(), 50);
                                }
                            } else {
                                attacker.sendMessage(ChatColor.RED + "[⚡] Только основатель может исключать!");
                            }
                        } else {
                            attacker.sendMessage(ChatColor.RED + "[⚡] Вы не можете исключить игрока из другой гильдии!");
                        }
                    }
                }
            }
            
            // Меч гильдии - приглашение
            if (item.getType() == Material.GOLDEN_SWORD && item.hasItemMeta()) {
                ItemMeta meta = item.getItemMeta();
                if (meta.hasDisplayName() && meta.getDisplayName().contains("Меч гильдии")) {
                    event.setCancelled(true);
                    
                    if (event.getEntity() instanceof Player) {
                        Player target = (Player) event.getEntity();
                        Guild guild = plugin.getGuildManager().getGuildByPlayer(attacker.getUniqueId());
                        
                        if (guild != null && guild.getFounder().equals(attacker.getUniqueId())) {
                            processInvitation(attacker, target, guild, item);
                        }
                    }
                }
            }
        }
    }
    
    private void processInvitation(Player player, Player target, Guild guild, ItemStack sword) {
        if (guild.isFull()) {
            player.sendMessage(ChatColor.RED + "[⚡] Лимит участников исчерпан!");
            return;
        }
        
        int usesLeft = plugin.getItemManager().getRemainingUses(sword);
        if (usesLeft <= 0) {
            player.sendMessage(ChatColor.RED + "[⚡] Меч сломан! Купите новый.");
            return;
        }
        
        if (!plugin.getItemManager().decrementUses(sword)) {
            player.sendMessage(ChatColor.RED + "[⚡] Ошибка при использовании меча!");
            return;
        }
        
        invitationTarget.put(target.getUniqueId(), guild.getName());
        
        target.sendMessage(ChatColor.GOLD + "══════════════════════════════════");
        target.sendMessage("");
        target.sendMessage(ChatColor.YELLOW + "   ВСТУПИТЬ В ГИЛЬДИЮ?");
        target.sendMessage("");
        target.sendMessage(ChatColor.GREEN + "   Гильдия: " + guild.getColoredName());
        target.sendMessage(ChatColor.GREEN + "   Аббревиатура: " + guild.getPrefix());
        target.sendMessage(ChatColor.GREEN + "   Основатель: " + player.getName());
        target.sendMessage("");
        target.sendMessage(ChatColor.GREEN + "   /g accept - Принять");
        target.sendMessage(ChatColor.RED + "   /g deny - Отказать");
        target.sendMessage("");
        target.sendMessage(ChatColor.GOLD + "══════════════════════════════════");
        
        target.spawnParticle(Particle.HEART, target.getLocation().add(0, 2, 0), 10);
        target.playSound(target.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f);
    }
    
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        Guild guild = plugin.getGuildManager().getGuildByPlayer(player.getUniqueId());
        
        if (guild != null) {
            updatePlayerDisplay(player, guild.getName(), guild.getAbbreviation(), guild.getColor());
        }
    }
    
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        UUID playerId = event.getPlayer().getUniqueId();
        creatingGuild.remove(playerId);
        awaitingColor.remove(playerId);
        awaitingAbbreviation.remove(playerId);
        invitationTarget.remove(playerId);
    }
}