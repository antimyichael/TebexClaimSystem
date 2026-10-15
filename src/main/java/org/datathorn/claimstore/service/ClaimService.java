package org.datathorn.claimstore.service;

import org.datathorn.claimstore.TebexClaimSystemPlugin;
import org.datathorn.claimstore.model.ProductDefinition;
import org.datathorn.claimstore.storage.PendingClaimsRepository;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class ClaimService {

    private final TebexClaimSystemPlugin plugin;
    private final PendingClaimsRepository repository;
    private final ProductRegistry productRegistry;
    private final MessageService messageService;
    private final ClaimLogger claimLogger;

    public ClaimService(
            TebexClaimSystemPlugin plugin,
            PendingClaimsRepository repository,
            ProductRegistry productRegistry,
            MessageService messageService,
            ClaimLogger claimLogger
    ) {
        this.plugin = plugin;
        this.repository = repository;
        this.productRegistry = productRegistry;
        this.messageService = messageService;
        this.claimLogger = claimLogger;
    }

    public void addPendingClaim(String playerName, String productId, int quantity) {
        repository.addClaim(playerName, productId, quantity);
    }

    public void addPendingClaims(Collection<String> playerNames, String productId, int quantity) {
        repository.addClaims(playerNames, productId, quantity);
    }

    public Map<String, Integer> getPendingClaims(String playerName) {
        return repository.getClaims(playerName);
    }

    public boolean hasPendingClaims(String playerName) {
        return repository.hasAnyClaims(playerName);
    }

    public boolean claimSingle(Player player, String productId) {
        return claimProduct(player, productId, false);
    }

    public boolean claimOne(Player player, String productId) {
        return claimProduct(player, productId, true);
    }

    private boolean claimProduct(Player player, String productId, boolean oneOnlyWhenPossible) {
        Map<String, Integer> claims = repository.getClaims(player.getName());
        int quantity = claims.getOrDefault(productId.toLowerCase(), 0);

        if (quantity <= 0) {
            player.sendMessage(messageService.get("noClaimForProduct"));
            return false;
        }

        ProductDefinition product = productRegistry.get(productId);
        if (product == null) {
            player.sendMessage(messageService.get("unknownProduct", singleton("product", productId)));
            return false;
        }

        int claimQuantity = oneOnlyWhenPossible && quantity > 1 ? 1 : quantity;

        int neededSlots = product.getRequiredSlotsPerUnit() * claimQuantity;
        if (getFreeSlots(player) < neededSlots) {
            player.sendMessage(messageService.get("inventoryFull", singleton("neededSlots", String.valueOf(neededSlots))));
            return false;
        }

        dispatchCommands(player, product, claimQuantity);
        if (claimQuantity >= quantity) {
            repository.removeClaim(player.getName(), product.getId());
        } else {
            repository.decrementClaim(player.getName(), product.getId(), claimQuantity);
        }
        claimLogger.logClaim(player.getName(), product.getId(), claimQuantity);

        Map<String, String> placeholders = new HashMap<String, String>();
        placeholders.put("quantity", String.valueOf(claimQuantity));
        placeholders.put("productDisplay", messageService.color(product.getDisplayName()));
        player.sendMessage(messageService.get("claimSuccess", placeholders));
        return true;
    }

    public boolean claimAll(Player player) {
        Map<String, Integer> claims = repository.getClaims(player.getName());
        if (claims.isEmpty()) {
            player.sendMessage(messageService.get("claimAllNothing"));
            return false;
        }

        int neededSlots = 0;
        int configuredProducts = 0;
        for (Map.Entry<String, Integer> entry : claims.entrySet()) {
            ProductDefinition product = productRegistry.get(entry.getKey());
            if (product == null) {
                continue;
            }
            configuredProducts++;
            neededSlots += product.getRequiredSlotsPerUnit() * entry.getValue();
        }

        if (configuredProducts == 0) {
            player.sendMessage(messageService.get("claimAllNothing"));
            return false;
        }

        if (getFreeSlots(player) < neededSlots) {
            player.sendMessage(messageService.get("inventoryFullAll", singleton("neededSlots", String.valueOf(neededSlots))));
            return false;
        }

        for (Map.Entry<String, Integer> entry : claims.entrySet()) {
            ProductDefinition product = productRegistry.get(entry.getKey());
            if (product == null) {
                continue;
            }
            int quantity = entry.getValue();
            dispatchCommands(player, product, quantity);
            repository.removeClaim(player.getName(), product.getId());
            claimLogger.logClaim(player.getName(), product.getId(), quantity);
        }

        player.sendMessage(messageService.get("claimAllSuccess", singleton("count", String.valueOf(configuredProducts))));
        return true;
    }

    private void dispatchCommands(Player player, ProductDefinition product, int quantity) {
        for (int i = 0; i < quantity; i++) {
            for (String command : product.getCommands()) {
                String parsed = command
                        .replace("{player}", player.getName())
                        .replace("{quantity}", String.valueOf(quantity));
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), parsed);
            }
        }
    }

    private int getFreeSlots(Player player) {
        int free = 0;
        for (ItemStack stack : player.getInventory().getContents()) {
            if (stack == null || stack.getType() == Material.AIR) {
                free++;
            }
        }
        return free;
    }

    private Map<String, String> singleton(String key, String value) {
        Map<String, String> map = new HashMap<String, String>();
        map.put(key, value);
        return map;
    }
}


