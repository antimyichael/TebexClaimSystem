package org.datathorn.claimstore.storage;

import org.datathorn.claimstore.TebexClaimSystemPlugin;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class PendingClaimsRepository {

    private final File file;
    private final TebexClaimSystemPlugin plugin;
    private final YamlConfiguration config;

    public PendingClaimsRepository(File file, TebexClaimSystemPlugin plugin) {
        this.file = file;
        this.plugin = plugin;
        ensureFile();
        this.config = YamlConfiguration.loadConfiguration(file);
    }

    public synchronized void addClaim(String player, String product, int quantity) {
        String playerKey = normalize(player);
        String productKey = normalize(product);
        int current = config.getInt(path(playerKey, productKey), 0);
        config.set(path(playerKey, productKey), current + quantity);
        save();
    }

    public synchronized void addClaims(Collection<String> players, String product, int quantity) {
        if (players == null || players.isEmpty() || quantity <= 0) {
            return;
        }
        String productKey = normalize(product);
        for (String player : players) {
            String playerKey = normalize(player);
            int current = config.getInt(path(playerKey, productKey), 0);
            config.set(path(playerKey, productKey), current + quantity);
        }
        save();
    }

    public synchronized Map<String, Integer> getClaims(String player) {
        String playerKey = normalize(player);
        ConfigurationSection section = config.getConfigurationSection("players." + playerKey);
        if (section == null) {
            return Collections.emptyMap();
        }

        Map<String, Integer> claims = new HashMap<String, Integer>();
        for (String product : section.getKeys(false)) {
            int quantity = section.getInt(product, 0);
            if (quantity > 0) {
                claims.put(product, quantity);
            }
        }
        return claims;
    }

    public synchronized void removeClaim(String player, String product) {
        String playerKey = normalize(player);
        String productKey = normalize(product);
        config.set(path(playerKey, productKey), null);

        ConfigurationSection section = config.getConfigurationSection("players." + playerKey);
        if (section != null && section.getKeys(false).isEmpty()) {
            config.set("players." + playerKey, null);
        }
        save();
    }

    public synchronized void decrementClaim(String player, String product, int quantityToRemove) {
        if (quantityToRemove <= 0) {
            return;
        }

        String playerKey = normalize(player);
        String productKey = normalize(product);
        String claimPath = path(playerKey, productKey);
        int current = config.getInt(claimPath, 0);
        int updated = current - quantityToRemove;

        if (updated <= 0) {
            removeClaim(player, product);
            return;
        }

        config.set(claimPath, updated);
        save();
    }

    public synchronized boolean hasAnyClaims(String player) {
        return !getClaims(player).isEmpty();
    }

    private void save() {
        try {
            config.save(file);
        } catch (IOException ex) {
            if (plugin != null) {
                plugin.getLogger().warning("Failed saving pending claims: " + ex.getMessage());
            }
        }
    }

    private String path(String player, String product) {
        return "players." + player + "." + product;
    }

    private String normalize(String value) {
        return value == null ? "" : value.toLowerCase();
    }

    private void ensureFile() {
        if (plugin != null && !plugin.getDataFolder().exists() && !plugin.getDataFolder().mkdirs()) {
            plugin.getLogger().warning("Could not create plugin data folder.");
            return;
        }
        if (file.exists()) {
            return;
        }

        try {
            if (file.getParentFile() != null && !file.getParentFile().exists()) {
                file.getParentFile().mkdirs();
            }
            if (!file.createNewFile()) {
                if (plugin != null) {
                    plugin.getLogger().warning("Could not create pending claims file.");
                }
            }
        } catch (IOException ex) {
            if (plugin != null) {
                plugin.getLogger().warning("Failed creating pending claims file: " + ex.getMessage());
            }
        }
    }
}

