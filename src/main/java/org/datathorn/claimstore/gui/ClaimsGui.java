package org.datathorn.claimstore.gui;

import org.datathorn.claimstore.TebexClaimSystemPlugin;
import org.datathorn.claimstore.model.ProductDefinition;
import org.datathorn.claimstore.service.ClaimService;
import org.datathorn.claimstore.service.MessageService;
import org.datathorn.claimstore.service.ProductRegistry;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ClaimsGui {

    private final TebexClaimSystemPlugin plugin;
    private final ClaimService claimService;
    private final ProductRegistry productRegistry;
    private final MessageService messageService;

    public ClaimsGui(TebexClaimSystemPlugin plugin, ClaimService claimService, ProductRegistry productRegistry, MessageService messageService) {
        this.plugin = plugin;
        this.claimService = claimService;
        this.productRegistry = productRegistry;
        this.messageService = messageService;
    }

    public void open(Player player) {
        Map<String, Integer> claims = claimService.getPendingClaims(player.getName());
        if (claims.isEmpty()) {
            player.sendMessage(messageService.get("noPendingClaims"));
            return;
        }

        int size = normalizeSize(plugin.getConfig().getInt("gui.size", 54));
        String title = messageService.color(plugin.getConfig().getString("gui.title", "&8Claim Store"));
        int claimAllSlot = normalizeSlot(plugin.getConfig().getInt("gui.claimAllSlot", size - 5), size);

        ClaimInventoryHolder holder = new ClaimInventoryHolder(claimAllSlot);
        Inventory inventory = Bukkit.createInventory(holder, size, title);

        fillBorder(inventory);

        int slot = 0;
        for (Map.Entry<String, Integer> entry : claims.entrySet()) {
            if (slot >= size) {
                break;
            }

            while (slot < size && (slot == claimAllSlot || isBorderSlot(slot, size))) {
                slot++;
            }

            if (slot >= size) {
                break;
            }

            String productId = entry.getKey();
            int quantity = entry.getValue();
            ProductDefinition product = productRegistry.get(productId);

            if (product == null) {
                inventory.setItem(slot, buildUnknownItem(productId, quantity));
                holder.mapSlot(slot, productId);
                slot++;
                continue;
            }

            inventory.setItem(slot, buildProductItem(product, quantity));
            holder.mapSlot(slot, product.getId());
            slot++;
        }

        inventory.setItem(claimAllSlot, buildClaimAllItem());
        player.openInventory(inventory);
    }

    private void fillBorder(Inventory inventory) {
        ItemStack border = buildBorderItem();
        int size = inventory.getSize();
        for (int slot = 0; slot < size; slot++) {
            if (isBorderSlot(slot, size)) {
                inventory.setItem(slot, border);
            }
        }
    }

    private ItemStack buildBorderItem() {
        ItemStack stack = new ItemStack(Material.STAINED_GLASS_PANE, 1, (short) 7);
        ItemMeta meta = stack.getItemMeta();
        meta.setDisplayName(" ");
        stack.setItemMeta(meta);
        return stack;
    }

    private boolean isBorderSlot(int slot, int size) {
        int columns = 9;
        int rows = size / columns;
        int row = slot / columns;
        int column = slot % columns;
        return row == 0 || row == rows - 1 || column == 0 || column == columns - 1;
    }

    public boolean isClaimsGui(Inventory inventory) {
        return inventory != null && inventory.getHolder() instanceof ClaimInventoryHolder;
    }

    public String getProductForSlot(Inventory inventory, int slot) {
        ClaimInventoryHolder holder = (ClaimInventoryHolder) inventory.getHolder();
        return holder.getProductForSlot(slot);
    }

    public boolean isClaimAllSlot(Inventory inventory, int slot) {
        ClaimInventoryHolder holder = (ClaimInventoryHolder) inventory.getHolder();
        return holder.isClaimAllSlot(slot);
    }

    private ItemStack buildProductItem(ProductDefinition product, int quantity) {
        ItemStack stack = new ItemStack(product.getMaterial(), Math.min(64, Math.max(1, quantity)));
        ItemMeta meta = stack.getItemMeta();
        meta.setDisplayName(messageService.color(product.getDisplayName()));

        List<String> lore = new ArrayList<String>();
        String quantityLine = plugin.getConfig().getString("gui.quantityLore", "&7Quantity: &f{quantity}")
                .replace("{quantity}", String.valueOf(quantity));
        lore.add(messageService.color(quantityLine));
        String clickHint = plugin.getConfig().getString("messages.clickHint", "&eLeft-click to claim all");
        lore.add(messageService.color(clickHint));
        if (quantity > 1) {
            String rightClickHint = plugin.getConfig().getString("messages.clickHintRight", "&eRight-click to claim 1");
            lore.add(messageService.color(rightClickHint));
        }
        meta.setLore(lore);
        stack.setItemMeta(meta);
        return stack;
    }

    private ItemStack buildUnknownItem(String productId, int quantity) {
        ItemStack stack = new ItemStack(Material.BARRIER, 1);
        ItemMeta meta = stack.getItemMeta();
        meta.setDisplayName(messageService.color("&cUnknown Product"));
        List<String> lore = new ArrayList<String>();
        lore.add(messageService.color("&7ID: &f" + productId));
        lore.add(messageService.color("&7Quantity: &f" + quantity));
        meta.setLore(lore);
        stack.setItemMeta(meta);
        return stack;
    }

    private ItemStack buildClaimAllItem() {
        String materialName = plugin.getConfig().getString("gui.claimAllMaterial", "EMERALD_BLOCK");
        Material material = Material.matchMaterial(materialName);
        if (material == null) {
            material = Material.EMERALD_BLOCK;
        }

        ItemStack stack = new ItemStack(material, 1);
        ItemMeta meta = stack.getItemMeta();
        meta.setDisplayName(messageService.color(plugin.getConfig().getString("gui.claimAllName", "&aClaim All")));
        stack.setItemMeta(meta);
        return stack;
    }

    private int normalizeSize(int size) {
        if (size < 9) {
            return 9;
        }
        if (size > 54) {
            return 54;
        }
        int mod = size % 9;
        return mod == 0 ? size : size + (9 - mod);
    }

    private int normalizeSlot(int slot, int size) {
        if (slot < 0 || slot >= size) {
            return size - 1;
        }
        return slot;
    }

    private static class ClaimInventoryHolder implements InventoryHolder {

        private final Map<Integer, String> slotProductMap = new HashMap<Integer, String>();
        private final int claimAllSlot;

        private ClaimInventoryHolder(int claimAllSlot) {
            this.claimAllSlot = claimAllSlot;
        }

        @Override
        public Inventory getInventory() {
            return null;
        }

        private void mapSlot(int slot, String productId) {
            slotProductMap.put(slot, productId);
        }

        private String getProductForSlot(int slot) {
            return slotProductMap.get(slot);
        }

        private boolean isClaimAllSlot(int slot) {
            return claimAllSlot == slot;
        }
    }
}

