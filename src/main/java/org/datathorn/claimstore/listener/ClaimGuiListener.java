package org.datathorn.claimstore.listener;

import org.datathorn.claimstore.TebexClaimSystemPlugin;
import org.datathorn.claimstore.gui.ClaimsGui;
import org.datathorn.claimstore.service.ClaimService;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;

public class ClaimGuiListener implements Listener {

    private final TebexClaimSystemPlugin plugin;
    private final ClaimsGui claimsGui;
    private final ClaimService claimService;

    public ClaimGuiListener(TebexClaimSystemPlugin plugin, ClaimsGui claimsGui, ClaimService claimService) {
        this.plugin = plugin;
        this.claimsGui = claimsGui;
        this.claimService = claimService;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }

        if (!claimsGui.isClaimsGui(event.getInventory())) {
            return;
        }

        event.setCancelled(true);

        int slot = event.getRawSlot();
        if (slot < 0 || slot >= event.getInventory().getSize()) {
            return;
        }

        if (!event.isLeftClick() && !event.isRightClick()) {
            return;
        }

        Player player = (Player) event.getWhoClicked();

        if (claimsGui.isClaimAllSlot(event.getInventory(), slot)) {
            boolean claimed = claimService.claimAll(player);
            refresh(player, claimed);
            return;
        }

        String productId = claimsGui.getProductForSlot(event.getInventory(), slot);
        if (productId == null) {
            return;
        }

        boolean claimed = event.isRightClick()
                ? claimService.claimOne(player, productId)
                : claimService.claimSingle(player, productId);
        refresh(player, claimed);
    }

    private void refresh(final Player player, boolean claimed) {
        if (!claimed) {
            return;
        }

        player.closeInventory();
        player.getServer().getScheduler().runTaskLater(plugin, new Runnable() {
            @Override
            public void run() {
                claimsGui.open(player);
            }
        }, 1L);
    }
}


