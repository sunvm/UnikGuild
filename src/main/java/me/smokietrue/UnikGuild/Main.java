package me.smokietrue.unikguild;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import java.util.*;

public class Main extends JavaPlugin implements TabCompleter {
    private GuildManager guildManager;
    private ItemManager itemManager;
    private Config config;
    private EventListener eventListener;
    private static Main instance;
    private UnikGuildAPI api;

    @Override
    public void onEnable() {
        instance = this;
        this.config = new Config(this);
        this.guildManager = new GuildManager(this, config);
        this.itemManager = new ItemManager(this);
        this.eventListener = new EventListener(this);
        this.api = new UnikGuildAPI(this);

        getServer().getPluginManager().registerEvents(eventListener, this);
        getServer().getServicesManager().register(UnikGuildAPI.class, api, this, org.bukkit.plugin.ServicePriority.Normal);

        getCommand("g").setExecutor(this);
        getCommand("guilds").setExecutor(this);
        getCommand("guildcolor").setExecutor(this);
        
        getCommand("g").setTabCompleter(this);
        getCommand("guilds").setTabCompleter(this);

        Bukkit.getScheduler().runTask(this, () -> {
            for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
                guildManager.updatePlayerDisplayOnJoin(onlinePlayer);
            }
        });

        getLogger().info(ChatColor.GREEN + "UnikGuild v1.0.1 успешно загружен!");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (command.getName().equalsIgnoreCase("g")) {
            if (args.length == 0) {
                sendHelp(sender);
                return true;
            }
            return handlePlayerCommand(sender, args);
        } else if (command.getName().equalsIgnoreCase("guilds")) {
            return handleAdminCommand(sender, args);
        } else if (command.getName().equalsIgnoreCase("guildcolor")) {
            return handleColorCommand(sender, args);
        }
        return false;
    }

    private boolean handleColorCommand(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Эту команду могут выполнять только игроки.");
            return true;
        }
        Player player = (Player) sender;
        
        if (!eventListener.awaitingColor.containsKey(player.getUniqueId()) || 
            !eventListener.creatingGuild.containsKey(player.getUniqueId())) {
            player.sendMessage(ChatColor.RED + "Вы не выбираете цвет для гильдии.");
            return true;
        }
        
        if (args.length < 1) {
            player.sendMessage(ChatColor.RED + "Использование: /guildcolor <цвет>");
            return true;
        }
        
        String colorCode = args[0].toUpperCase();
        String guildName = eventListener.creatingGuild.get(player.getUniqueId());
        
        if (guildName.equals("awaiting_name")) {
            player.sendMessage(ChatColor.RED + "Сначала введите название гильдии!");
            return true;
        }
        
        String[] validColors = {"RED", "BLUE", "GREEN", "YELLOW", "LIGHT_PURPLE", "AQUA", "WHITE", "DARK_PURPLE"};
        boolean isValid = false;
        for (String validColor : validColors) {
            if (validColor.equals(colorCode)) {
                isValid = true;
                break;
            }
        }
        
        if (!isValid) {
            player.sendMessage(ChatColor.RED + "Неверный цвет! Допустимые цвета: RED, BLUE, GREEN, YELLOW, LIGHT_PURPLE, AQUA, WHITE, DARK_PURPLE");
            return true;
        }
        
        eventListener.completeGuildCreation(player, guildName, colorCode);
        return true;
    }

    private boolean handlePlayerCommand(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Эту команду могут выполнять только игроки.");
            return true;
        }
        Player player = (Player) sender;
        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "accept":
                guildManager.acceptInvitation(player);
                break;
            case "deny":
                guildManager.denyInvitation(player);
                break;
            case "members":
            case "info":
                guildManager.sendGuildInfo(player);
                break;
            case "kick":
                if (args.length < 2) {
                    player.sendMessage(ChatColor.RED + "Использование: /g kick <ник>");
                    return true;
                }
                guildManager.kickMember(player, args[1]);
                break;
            case "leave":
                guildManager.leaveGuild(player);
                break;
            case "disband":
                guildManager.disbandGuild(player);
                break;
            case "chat":
                if (args.length < 2) {
                    player.sendMessage(ChatColor.RED + "Использование: /g chat <сообщение>");
                    return true;
                }
                String message = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
                Bukkit.getScheduler().runTask(this, () -> {
                    player.chat("- " + message);
                });
                break;
            case "role":
                if (args.length < 2) {
                    player.sendMessage(ChatColor.RED + "Использование: /g role <название роли>");
                    return true;
                }
                guildManager.createRole(player, args[1]);
                break;
            case "assign":
                if (args.length < 4 || !args[2].equalsIgnoreCase("to")) {
                    player.sendMessage(ChatColor.RED + "Использование: /g assign <ник> to <роль>");
                    player.sendMessage(ChatColor.GRAY + "Пример: /g assign PlayerName to Заместитель");
                    return true;
                }
                guildManager.assignRole(player, args[1], args[3]);
                break;
            case "nametag":
                if (args.length < 2) {
                    player.sendMessage(ChatColor.RED + "Использование: /g nametag <имя питомца>");
                    return true;
                }
                guildManager.createNameTag(player, args[1]);
                break;
            default:
                sendHelp(sender);
        }
        return true;
    }

    private boolean handleAdminCommand(CommandSender sender, String[] args) {
        if (!sender.hasPermission("unikguild.admin")) {
            sender.sendMessage(ChatColor.RED + "У вас нет прав на использование этой команды.");
            return true;
        }
        if (args.length == 0) {
            sendAdminHelp(sender);
            return true;
        }
        String subCommand = args[0].toLowerCase();
        switch (subCommand) {
            case "list":
                guildManager.sendGuildsList(sender);
                break;
            case "delete":
                if (args.length < 3) {
                    sender.sendMessage(ChatColor.RED + "Использование: /guilds delete <name|user> <значение>");
                    sender.sendMessage(ChatColor.GRAY + "Примеры:");
                    sender.sendMessage(ChatColor.GRAY + "  /guilds delete name НазваниеГильдии");
                    sender.sendMessage(ChatColor.GRAY + "  /guilds delete user ИгрокОснователь");
                    return true;
                }
                String deleteType = args[1].toLowerCase();
                String deleteValue = args[2];
                
                if (deleteType.equals("name")) {
                    guildManager.deleteGuildByName(sender, deleteValue);
                } else if (deleteType.equals("user")) {
                    guildManager.deleteGuildByFounder(sender, deleteValue);
                } else {
                    sender.sendMessage(ChatColor.RED + "Неверный тип удаления. Используйте 'name' или 'user'.");
                }
                break;
            case "give":
                if (args.length < 2) {
                    sender.sendMessage(ChatColor.RED + "Использование: /guilds give <ник>");
                    return true;
                }
                Player target = Bukkit.getPlayer(args[1]);
                if (target == null) {
                    sender.sendMessage(ChatColor.RED + "Игрок не найден или не в сети.");
                    return true;
                }
                target.getInventory().addItem(itemManager.createEmptyGuildSword());
                sender.sendMessage(ChatColor.GREEN + "Меч гильдии выдан игроку " + target.getName());
                break;
            default:
                sendAdminHelp(sender);
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        
        if (command.getName().equalsIgnoreCase("g")) {
            if (args.length == 1) {
                String[] subCommands = {"accept", "deny", "info", "members", "kick", "leave", "disband", "chat", "role", "assign", "nametag"};
                for (String sub : subCommands) {
                    if (sub.startsWith(args[0].toLowerCase())) {
                        completions.add(sub);
                    }
                }
            } else if (args.length == 2) {
                if (args[0].equalsIgnoreCase("kick")) {
                    if (sender instanceof Player) {
                        Player player = (Player) sender;
                        Guild guild = guildManager.getGuildByPlayer(player.getUniqueId());
                        if (guild != null) {
                            for (UUID memberId : guild.getMembers()) {
                                Player member = Bukkit.getPlayer(memberId);
                                if (member != null && !member.getUniqueId().equals(player.getUniqueId())) {
                                    String name = member.getName();
                                    if (name.toLowerCase().startsWith(args[1].toLowerCase())) {
                                        completions.add(name);
                                    }
                                }
                            }
                        }
                    }
                } else if (args[0].equalsIgnoreCase("assign")) {
                    if (sender instanceof Player) {
                        Player player = (Player) sender;
                        Guild guild = guildManager.getGuildByPlayer(player.getUniqueId());
                        if (guild != null) {
                            for (UUID memberId : guild.getMembers()) {
                                Player member = Bukkit.getPlayer(memberId);
                                if (member != null && !member.getUniqueId().equals(guild.getFounder())) {
                                    String name = member.getName();
                                    if (name.toLowerCase().startsWith(args[1].toLowerCase())) {
                                        completions.add(name);
                                    }
                                }
                            }
                        }
                    }
                }
            } else if (args.length == 4 && args[0].equalsIgnoreCase("assign") && args[2].equalsIgnoreCase("to")) {
                if (sender instanceof Player) {
                    Player player = (Player) sender;
                    Guild guild = guildManager.getGuildByPlayer(player.getUniqueId());
                    if (guild != null) {
                        for (String role : guild.getCustomRoles()) {
                            if (role.toLowerCase().startsWith(args[3].toLowerCase())) {
                                completions.add(role);
                            }
                        }
                    }
                }
            }
        } else if (command.getName().equalsIgnoreCase("guilds")) {
            if (args.length == 1) {
                String[] subCommands = {"list", "delete", "give"};
                for (String sub : subCommands) {
                    if (sub.startsWith(args[0].toLowerCase())) {
                        completions.add(sub);
                    }
                }
            } else if (args.length == 2) {
                if (args[0].equalsIgnoreCase("delete")) {
                    String[] deleteTypes = {"name", "user"};
                    for (String type : deleteTypes) {
                        if (type.startsWith(args[1].toLowerCase())) {
                            completions.add(type);
                        }
                    }
                } else if (args[0].equalsIgnoreCase("give")) {
                    for (Player player : Bukkit.getOnlinePlayers()) {
                        if (player.getName().toLowerCase().startsWith(args[1].toLowerCase())) {
                            completions.add(player.getName());
                        }
                    }
                }
            } else if (args.length == 3) {
                if (args[0].equalsIgnoreCase("delete")) {
                    if (args[1].equalsIgnoreCase("name")) {
                        for (String guildName : guildManager.getGuildNames()) {
                            if (guildName.toLowerCase().startsWith(args[2].toLowerCase())) {
                                completions.add(guildName);
                            }
                        }
                    } else if (args[1].equalsIgnoreCase("user")) {
                        // Предлагаем имена основателей гильдий
                        Set<String> founders = new HashSet<>();
                        Map<String, Guild> guildsMap = guildManager.getGuilds();
                        for (Guild guild : guildsMap.values()) {
                            String founderName = Bukkit.getOfflinePlayer(guild.getFounder()).getName();
                            if (founderName != null && founderName.toLowerCase().startsWith(args[2].toLowerCase())) {
                                founders.add(founderName);
                            }
                        }
                        completions.addAll(founders);
                    }
                }
            }
        }
        
        return completions;
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(ChatColor.GOLD + "══════════════════════════════════");
        sender.sendMessage(ChatColor.YELLOW + "Команды гильдии:");
        sender.sendMessage(ChatColor.GREEN + "/g info - Информация о гильдии");
        sender.sendMessage(ChatColor.GREEN + "/g kick <ник> - Исключить участника");
        sender.sendMessage(ChatColor.GREEN + "/g leave - Покинуть гильдию");
        sender.sendMessage(ChatColor.GREEN + "/g disband - Распустить гильдию");
        sender.sendMessage(ChatColor.GREEN + "/g chat <сообщение> - Чат гильдии");
        sender.sendMessage(ChatColor.GREEN + "/g role <роль> - Создать роль");
        sender.sendMessage(ChatColor.GREEN + "/g assign <ник> to <роль> - Назначить роль");
        sender.sendMessage(ChatColor.GREEN + "/g nametag <имя> - Создать именную бирку");
        sender.sendMessage(ChatColor.GRAY + "Или используйте префикс '- ' в чате");
        sender.sendMessage(ChatColor.GOLD + "══════════════════════════════════");
    }

    private void sendAdminHelp(CommandSender sender) {
        sender.sendMessage(ChatColor.GOLD + "══════════════════════════════════");
        sender.sendMessage(ChatColor.YELLOW + "Админ-команды гильдий:");
        sender.sendMessage(ChatColor.GREEN + "/guilds list - Список всех гильдий");
        sender.sendMessage(ChatColor.GREEN + "/guilds delete name <название> - Удалить гильдию по названию");
        sender.sendMessage(ChatColor.GREEN + "/guilds delete user <ник> - Удалить гильдию по основателю");
        sender.sendMessage(ChatColor.GREEN + "/guilds give <ник> - Выдать меч гильдии");
        sender.sendMessage(ChatColor.GOLD + "══════════════════════════════════");
    }

    public GuildManager getGuildManager() { return guildManager; }
    public ItemManager getItemManager() { return itemManager; }
    public EventListener getEventListener() { return eventListener; }
    public static Main getInstance() { return instance; }
    public UnikGuildAPI getAPI() { return api; }
}