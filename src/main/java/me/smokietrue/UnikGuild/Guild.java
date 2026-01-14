package me.smokietrue.unikguild;

import org.bukkit.ChatColor;
import java.util.*;

public class Guild {
    private String name;
    private String abbreviation; // Аббревиатура 3 символа
    private UUID founder;
    private Set<UUID> members = new HashSet<>();
    private Map<UUID, String> roles = new HashMap<>();
    private String color;
    private int maxMembers;
    private Date creationDate;

    public Guild(String name, String abbreviation, UUID founder, String color, int maxMembers) {
        this.name = name;
        this.abbreviation = abbreviation.toUpperCase(); // Приводим к верхнему регистру
        this.founder = founder;
        this.color = color;
        this.maxMembers = maxMembers;
        this.creationDate = new Date();
        
        members.add(founder);
        roles.put(founder, "Основатель");
    }

    public boolean addMember(UUID player) {
        if (members.size() >= maxMembers) return false;
        members.add(player);
        roles.put(player, "Участник");
        return true;
    }

    public boolean removeMember(UUID player) {
        if (founder.equals(player)) return false; // Основатель не может выйти
        members.remove(player);
        roles.remove(player);
        return true;
    }

    public boolean promoteMember(UUID player, String role) {
        if (!members.contains(player)) return false;
        roles.put(player, role);
        return true;
    }

    public String getColoredName() {
        return ChatColor.valueOf(color) + name;
    }

    public String getPrefix() {
        return ChatColor.valueOf(color) + "[" + abbreviation + "] ";
    }

    // Геттеры
    public String getName() { return name; }
    public String getAbbreviation() { return abbreviation; }
    public UUID getFounder() { return founder; }
    public Set<UUID> getMembers() { return Collections.unmodifiableSet(members); }
    public Map<UUID, String> getRoles() { return Collections.unmodifiableMap(roles); }
    public String getColor() { return color; }
    public int getMaxMembers() { return maxMembers; }
    public int getMemberCount() { return members.size(); }
    public boolean isFull() { return members.size() >= maxMembers; }
}