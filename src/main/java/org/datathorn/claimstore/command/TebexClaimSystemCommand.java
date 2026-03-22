package org.datathorn.claimstore.command;

import org.datathorn.claimstore.TebexClaimSystemPlugin;
import org.datathorn.claimstore.gui.ClaimsGui;
import org.datathorn.claimstore.service.ClaimService;
import org.datathorn.claimstore.service.MessageService;
import org.datathorn.claimstore.service.ProductRegistry;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;

public class TebexClaimSystemCommand implements CommandExecutor {

    private final TebexClaimSystemPlugin plugin;
    private final ClaimService claimService;
    private final ClaimsGui claimsGui;
    private final ProductRegistry productRegistry;
    private final MessageService messageService;

    public TebexClaimSystemCommand(
            TebexClaimSystemPlugin plugin,
            ClaimService claimService,
            ClaimsGui claimsGui,
            ProductRegistry productRegistry,
            MessageService messageService
    ) {
        this.plugin = plugin;
        this.claimService = claimService;
        this.claimsGui = claimsGui;
        this.productRegistry = productRegistry;
        this.messageService = messageService;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            if (!(sender instanceof Player)) {
                sender.sendMessage(messageService.get("usageConsole"));
                return true;
            }
            claimsGui.open((Player) sender);
            return true;
        }

        if ("add".equalsIgnoreCase(args[0])) {
            if (!(sender instanceof ConsoleCommandSender)) {
                sender.sendMessage(messageService.get("consoleOnlyAdd"));
                return true;
            }

            if (args.length != 4) {
                sender.sendMessage(messageService.get("usageConsole"));
                return true;
            }

            String playerName = args[1];
            String productId = args[2];
            int quantity;

            try {
                quantity = Integer.parseInt(args[3]);
            } catch (NumberFormatException ex) {
                sender.sendMessage(messageService.get("invalidQuantity"));
                return true;
            }

            if (quantity <= 0) {
                sender.sendMessage(messageService.get("invalidQuantity"));
                return true;
            }

            if (!productRegistry.hasProduct(productId)) {
                Map<String, String> placeholders = new HashMap<String, String>();
                placeholders.put("product", productId);
                sender.sendMessage(messageService.get("unknownProduct", placeholders));
                return true;
            }

            claimService.addPendingClaim(playerName, productId, quantity);

            Map<String, String> placeholders = new HashMap<String, String>();
            placeholders.put("quantity", String.valueOf(quantity));
            placeholders.put("product", productId);
            placeholders.put("player", playerName);
            sender.sendMessage(messageService.get("claimAdded", placeholders));
            return true;
        }

        if ("reload".equalsIgnoreCase(args[0])) {
            if (!(sender instanceof ConsoleCommandSender) && !sender.hasPermission("tebexclaimsystem.reload")) {
                sender.sendMessage(messageService.get("noPermissionReload"));
                return true;
            }

            if (plugin.reloadPluginConfiguration()) {
                sender.sendMessage(messageService.get("reloadSuccess"));
            } else {
                sender.sendMessage(messageService.get("reloadFailed"));
            }
            return true;
        }

        if (!(sender instanceof Player)) {
            sender.sendMessage(messageService.get("usageConsole"));
            return true;
        }

        claimsGui.open((Player) sender);
        return true;
    }
}


