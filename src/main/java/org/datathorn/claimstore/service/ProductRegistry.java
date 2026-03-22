package org.datathorn.claimstore.service;

import org.datathorn.claimstore.TebexClaimSystemPlugin;
import org.datathorn.claimstore.model.ProductDefinition;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ProductRegistry {

    private final Map<String, ProductDefinition> products = new HashMap<String, ProductDefinition>();

    public ProductRegistry(TebexClaimSystemPlugin plugin) {
        reload(plugin);
    }

    public void reload(TebexClaimSystemPlugin plugin) {
        synchronized (products) {
            products.clear();
            products.putAll(loadProducts(plugin));
        }
    }

    public ProductDefinition get(String productId) {
        synchronized (products) {
            return products.get(normalize(productId));
        }
    }

    public boolean hasProduct(String productId) {
        return get(productId) != null;
    }

    public Map<String, ProductDefinition> getProducts() {
        synchronized (products) {
            return Collections.unmodifiableMap(new HashMap<String, ProductDefinition>(products));
        }
    }

    private Map<String, ProductDefinition> loadProducts(TebexClaimSystemPlugin plugin) {
        ConfigurationSection section = plugin.getConfig().getConfigurationSection("products");
        Map<String, ProductDefinition> loaded = new HashMap<String, ProductDefinition>();
        if (section == null) {
            return loaded;
        }

        for (String id : section.getKeys(false)) {
            String path = "products." + id;
            String displayName = plugin.getConfig().getString(path + ".displayName", id);
            List<String> commands = plugin.getConfig().getStringList(path + ".commands");
            if (commands == null) {
                commands = new ArrayList<String>();
            }

            String materialName = plugin.getConfig().getString(path + ".itemMaterial", "CHEST");
            Material material = Material.matchMaterial(materialName);
            if (material == null) {
                material = Material.CHEST;
            }

            int requiredSlotsPerUnit = Math.max(0, plugin.getConfig().getInt(path + ".requiredSlotsPerUnit", 1));

            loaded.put(normalize(id), new ProductDefinition(id, displayName, commands, material, requiredSlotsPerUnit));
        }
        return loaded;
    }

    private String normalize(String productId) {
        return productId == null ? "" : productId.toLowerCase();
    }
}

