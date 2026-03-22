package org.datathorn.claimstore.service;

import org.datathorn.claimstore.TebexClaimSystemPlugin;
import org.bukkit.ChatColor;

import java.util.Collections;
import java.util.Map;

public class MessageService {

    private final TebexClaimSystemPlugin plugin;

    public MessageService(TebexClaimSystemPlugin plugin) {
        this.plugin = plugin;
    }

    public String get(String key) {
        String raw = plugin.getConfig().getString("messages." + key, key);
        raw = applyPrefix(raw);
        return color(raw);
    }

    public String get(String key, Map<String, String> placeholders) {
        String value = plugin.getConfig().getString("messages." + key, key);
        value = applyPrefix(value);
        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            value = value.replace("{" + entry.getKey() + "}", entry.getValue());
        }
        return color(value);
    }

    public String get(String key, String placeholder, String replacement) {
        return get(key, Collections.singletonMap(placeholder, replacement));
    }

    public String color(String text) {
        return ChatColor.translateAlternateColorCodes('&', text);
    }

    private String applyPrefix(String value) {
        String prefix = plugin.getConfig().getString("messages.prefix", "");
        return value.replace("{prefix}", prefix == null ? "" : prefix);
    }
}

