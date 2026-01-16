package me.smokietrue.unikguild;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import java.util.*;

public class Guild {
    private final String name;
    private final UUID founder;
    private final Set<UUID> members = new HashSet<>();
    private final Map<UUID, String> roles = new HashMap<>();
    private final String color;
    private final int maxMembers;
    private final Set<String> customRoles = new HashSet<>();

    public Guild(String name, UUID founder, String color, int maxMembers) {
        this.name = name;
        this.founder = founder;
        this.color = color;
        this.maxMembers = maxMembers;
        this.members.add(founder);
        this.roles.put(founder, "Основатель");
        // Добавляем стандартные роли
        this.customRoles.add("Заместитель");
        this.customRoles.add("Старшина");
        this.customRoles.add("Рядовой");
    }

    public void broadcast(String message) {
        String formattedMessage = ChatColor.ITALIC + message;
        for (UUID memberId : members) {
            Player member = Bukkit.getPlayer(memberId);
            if (member != null && member.isOnline()) {
                member.sendMessage(formattedMessage);
            }
        }
    }

    public boolean addCustomRole(String roleName) {
        if (roleName == null || roleName.trim().isEmpty() || roleName.length() > 16) {
            return false;
        }
        return customRoles.add(roleName.trim());
    }

    public boolean removeCustomRole(String roleName) {
        // Нельзя удалить роль, если она назначена кому-то
        for (String assignedRole : roles.values()) {
            if (assignedRole.equalsIgnoreCase(roleName)) {
                return false;
            }
        }
        return customRoles.remove(roleName);
    }

    public boolean assignRole(UUID playerId, String roleName) {
        if (!members.contains(playerId) || playerId.equals(founder)) {
            return false; // Нельзя изменить роль основателя
        }
        
        // Проверяем, что роль существует (стандартная или кастомная)
        if (customRoles.contains(roleName) || roleName.equals("Основатель")) {
            roles.put(playerId, roleName);
            return true;
        }
        return false;
    }

    public String getRole(UUID playerId) {
        return roles.get(playerId);
    }

    public String getName() { return name; }
    public UUID getFounder() { return founder; }
    public String getColor() { return color; }
    public int getMaxMembers() { return maxMembers; }
    public Set<UUID> getMembers() { return new HashSet<>(members); }
    public Map<UUID, String> getRoles() { return new HashMap<>(roles); }
    public Set<String> getCustomRoles() { return new HashSet<>(customRoles); }
    public boolean isFounder(UUID playerId) { return founder.equals(playerId); }
    public boolean isFull() { return members.size() >= maxMembers; }
    public void addMember(UUID playerId) { 
        members.add(playerId); 
        roles.put(playerId, "Рядовой"); // По умолчанию новая роль
    }
    public void removeMember(UUID playerId) { 
        members.remove(playerId); 
        roles.remove(playerId); 
    }
}