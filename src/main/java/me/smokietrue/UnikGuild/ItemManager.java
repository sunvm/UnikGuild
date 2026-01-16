package me.smokietrue.unikguild;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Arrays;
import java.util.List;

public class ItemManager {
    private Main plugin;
    
    public ItemManager(Main plugin) {
        this.plugin = plugin;
    }
    
    // Метод для создания меча при выдаче командой /guilds give
    public ItemStack createEmptyGuildSword() {
        ItemStack sword = new ItemStack(Material.GOLDEN_SWORD);
        ItemMeta meta = sword.getItemMeta();
        
        if (meta == null) return sword;
        
        meta.setDisplayName(ChatColor.GOLD + "Меч гильдии");
        meta.setLore(Arrays.asList(
            ChatColor.YELLOW + "Использований: 15/15",
            "",
            ChatColor.GRAY + "ПКМ: Начать создание гильдии",
            ChatColor.GRAY + "ЛКМ по игроку: Пригласить в гильдию"
        ));
        
        meta.setUnbreakable(true);
        meta.addItemFlags(ItemFlag.HIDE_UNBREAKABLE);
        
        Enchantment unbreaking = Enchantment.getByKey(NamespacedKey.minecraft("unbreaking"));
        if (unbreaking != null) {
            meta.addEnchant(unbreaking, 3, true);
        }
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        sword.setItemMeta(meta);
        
        return sword;
    }
    
    // Метод для обновления меча после создания гильдии
    public ItemStack updateSwordForGuild(ItemStack sword, String guildName, String color, int uses) {
        if (sword.getType() != Material.GOLDEN_SWORD) return sword;
        
        ItemMeta meta = sword.getItemMeta();
        if (meta == null) return sword;
        
        ChatColor chatColor = ChatColor.valueOf(color);
        meta.setDisplayName(chatColor + "Меч гильдии " + guildName);
        meta.setLore(Arrays.asList(
            chatColor + "Использований: " + uses + "/15",
            "",
            chatColor + "ПКМ: Информация о гильдии",
            chatColor + "ЛКМ по игроку: Приглашить в гильдию"
        ));
        
        sword.setItemMeta(meta);
        return sword;
    }
    
    public ItemStack createGuildSword(String guildName, String color, int uses) {
        ItemStack sword = new ItemStack(Material.GOLDEN_SWORD);
        ItemMeta meta = sword.getItemMeta();
        
        if (meta == null) return sword;
        
        ChatColor chatColor = ChatColor.valueOf(color);
        meta.setDisplayName(chatColor + "Меч гильдии " + guildName);
        meta.setLore(Arrays.asList(
            chatColor + "Использований: " + uses + "/15",
            "",
            chatColor + "ПКМ: Информация о гильдии",
            chatColor + "ЛКМ по игроку: Приглашить в гильдию"
        ));
        
        meta.setUnbreakable(true);
        meta.addItemFlags(ItemFlag.HIDE_UNBREAKABLE);
        
        Enchantment unbreaking = Enchantment.getByKey(NamespacedKey.minecraft("unbreaking"));
        if (unbreaking != null) {
            meta.addEnchant(unbreaking, 3, true);
        }
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        sword.setItemMeta(meta);
        
        return sword;
    }
    
    public ItemStack createKickStick() {
        ItemStack stick = new ItemStack(Material.STICK);
        ItemMeta meta = stick.getItemMeta();
        meta.setDisplayName(ChatColor.RED + "Палка изгнания");
        meta.setLore(Arrays.asList(
            ChatColor.GRAY + "ЛКМ по игроку:",
            ChatColor.GRAY + "Исключить из гильдии"
        ));
        stick.setItemMeta(meta);
        return stick;
    }
    
    public ItemStack createPromotionRod() {
        ItemStack rod = new ItemStack(Material.BLAZE_ROD);
        ItemMeta meta = rod.getItemMeta();
        meta.setDisplayName(ChatColor.GOLD + "Жезл назначения");
        meta.setLore(Arrays.asList(
            ChatColor.GRAY + "ПКМ по игроку:",
            ChatColor.GRAY + "Назначить должность"
        ));
        rod.setItemMeta(meta);
        return rod;
    }
    
    public int getRemainingUses(ItemStack sword) {
        if (sword == null || sword.getType() != Material.GOLDEN_SWORD) return 0;
        
        ItemMeta meta = sword.getItemMeta();
        if (meta == null) return 0;
        
        List<String> lore = meta.getLore();
        if (lore == null || lore.isEmpty()) return 0;
        
        for (String line : lore) {
            if (line.contains("Использований:")) {
                try {
                    String[] parts = line.split(":");
                    if (parts.length >= 2) {
                        String numbers = parts[1].trim();
                        String[] useParts = numbers.split("/");
                        if (useParts.length >= 1) {
                            String current = useParts[0].replaceAll("[^0-9]", "");
                            return Integer.parseInt(current);
                        }
                    }
                } catch (NumberFormatException e) {
                    e.printStackTrace();
                }
            }
        }
        return 0;
    }
    
    public boolean decrementUses(ItemStack sword) {
        if (sword == null || sword.getType() != Material.GOLDEN_SWORD) return false;
        
        ItemMeta meta = sword.getItemMeta();
        if (meta == null) return false;
        
        List<String> lore = meta.getLore();
        if (lore == null || lore.isEmpty()) return false;
        
        for (int i = 0; i < lore.size(); i++) {
            String line = lore.get(i);
            if (line.contains("Использований:")) {
                try {
                    String[] parts = line.split(":");
                    if (parts.length >= 2) {
                        String numbers = parts[1].trim();
                        String[] useParts = numbers.split("/");
                        if (useParts.length >= 1) {
                            String currentStr = useParts[0].replaceAll("[^0-9]", "");
                            int currentUses = Integer.parseInt(currentStr);
                            
                            if (currentUses <= 0) return false;
                            
                            currentUses--;
                            String colorCode = line.substring(0, 2);
                            lore.set(i, colorCode + "Использований: " + currentUses + "/15");
                            meta.setLore(lore);
                            sword.setItemMeta(meta);
                            return true;
                        }
                    }
                } catch (NumberFormatException e) {
                    e.printStackTrace();
                }
            }
        }
        return false;
    }
}