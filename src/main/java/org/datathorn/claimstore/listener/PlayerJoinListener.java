package org.datathorn.claimstore.listener;

import org.datathorn.claimstore.service.ClaimService;
import org.datathorn.claimstore.service.MessageService;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public class PlayerJoinListener implements Listener {

    private final ClaimService claimService;
    private final MessageService messageService;

    public PlayerJoinListener(ClaimService claimService, MessageService messageService) {
        this.claimService = claimService;
        this.messageService = messageService;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        String name = event.getPlayer().getName();
        if (claimService.hasPendingClaims(name)) {
            event.getPlayer().sendMessage(messageService.get("notifyPendingOnJoin"));
        }
    }
}

