package org.datathorn.claimstore.command;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.datathorn.claimstore.TebexClaimSystemPlugin;
import org.datathorn.claimstore.gui.ClaimsGui;
import org.datathorn.claimstore.service.ClaimService;
import org.datathorn.claimstore.service.MessageService;
import org.datathorn.claimstore.service.PlayerListParser;
import org.datathorn.claimstore.service.ProductRegistry;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TebexClaimSystemCommand implements CommandExecutor, TabCompleter {

    private final TebexClaimSystemPlugin plugin;
    private final ClaimService claimService;
    private final ClaimsGui claimsGui;
    private final ProductRegistry productRegistry;
    private final MessageService messageService;
    private final PlayerListParser playerListParser;

    public TebexClaimSystemCommand(
            TebexClaimSystemPlugin plugin,
            ClaimService claimService,
            ClaimsGui claimsGui,
            ProductRegistry productRegistry,
            MessageService messageService
    ) {
        this(plugin, claimService, claimsGui, productRegistry, messageService, new PlayerListParser());
    }

    public TebexClaimSystemCommand(
            TebexClaimSystemPlugin plugin,
            ClaimService claimService,
            ClaimsGui claimsGui,
            ProductRegistry productRegistry,
            MessageService messageService,
            PlayerListParser playerListParser
    ) {
        this.plugin = plugin;
        this.claimService = claimService;
        this.claimsGui = claimsGui;
        this.productRegistry = productRegistry;
        this.messageService = messageService;
        this.playerListParser = playerListParser;
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

        if ("addfromlist".equalsIgnoreCase(args[0])) {
            if (!(sender instanceof ConsoleCommandSender) && !sender.hasPermission("tebexclaimsystem.admin")) {
                sender.sendMessage(messageService.get("noPermissionAddFromList"));
                return true;
            }

            if (args.length != 4) {
                sender.sendMessage(messageService.get("usageAddFromList"));
                return true;
            }

            String fileName = args[1];
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

            File listFile = resolveFile(fileName);
            if (!listFile.exists() || listFile.isDirectory()) {
                Map<String, String> placeholders = new HashMap<String, String>();
                placeholders.put("file", fileName);
                sender.sendMessage(messageService.get("fileNotFound", placeholders));
                return true;
            }

            List<String> usernames;
            try {
                usernames = playerListParser.parseUsernames(listFile);
            } catch (IOException ex) {
                Map<String, String> placeholders = new HashMap<String, String>();
                placeholders.put("file", fileName);
                placeholders.put("error", ex.getMessage() == null ? "Unknown I/O error" : ex.getMessage());
                sender.sendMessage(messageService.get("fileReadError", placeholders));
                return true;
            }

            if (usernames.isEmpty()) {
                Map<String, String> placeholders = new HashMap<String, String>();
                placeholders.put("file", fileName);
                sender.sendMessage(messageService.get("noUsersInFile", placeholders));
                return true;
            }

            claimService.addPendingClaims(usernames, productId, quantity);

            for (String user : usernames) {
                Player onlinePlayer = Bukkit.getPlayerExact(user);
                if (onlinePlayer != null && onlinePlayer.isOnline()) {
                    onlinePlayer.sendMessage(messageService.get("notifyPendingOnJoin"));
                }
            }

            Map<String, String> placeholders = new HashMap<String, String>();
            placeholders.put("quantity", String.valueOf(quantity));
            placeholders.put("product", productId);
            placeholders.put("count", String.valueOf(usernames.size()));
            placeholders.put("file", listFile.getName());
            sender.sendMessage(messageService.get("claimAddedFromList", placeholders));
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

    public File resolveFile(String fileName) {
        File file = new File(fileName);
        if (file.isAbsolute()) {
            return file;
        }

        File dataFolderFile = new File(plugin.getDataFolder(), fileName);
        if (dataFolderFile.exists()) {
            return dataFolderFile;
        }

        File serverRootFile = new File(fileName);
        if (serverRootFile.exists()) {
            return serverRootFile;
        }

        if (!fileName.toLowerCase().endsWith(".yml") && !fileName.toLowerCase().endsWith(".yaml")) {
            File withExtensionData = new File(plugin.getDataFolder(), fileName + ".yml");
            if (withExtensionData.exists()) {
                return withExtensionData;
            }
            File withExtensionServer = new File(fileName + ".yml");
            if (withExtensionServer.exists()) {
                return withExtensionServer;
            }
        }

        return dataFolderFile;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<String>();
        if (args.length == 1) {
            List<String> subcommands = new ArrayList<String>();
            if (sender instanceof ConsoleCommandSender) {
                subcommands.add("add");
            }
            if (sender instanceof ConsoleCommandSender || sender.hasPermission("tebexclaimsystem.admin")) {
                subcommands.add("addfromlist");
            }
            if (sender instanceof ConsoleCommandSender || sender.hasPermission("tebexclaimsystem.reload")) {
                subcommands.add("reload");
            }
            for (String sub : subcommands) {
                if (sub.toLowerCase().startsWith(args[0].toLowerCase())) {
                    completions.add(sub);
                }
            }
            return completions;
        }

        if (args.length == 2 && "addfromlist".equalsIgnoreCase(args[0])) {
            File dataFolder = plugin.getDataFolder();
            if (dataFolder.exists() && dataFolder.isDirectory()) {
                File[] files = dataFolder.listFiles();
                if (files != null) {
                    for (File f : files) {
                        if (f.isFile() && (f.getName().toLowerCase().endsWith(".yml") || f.getName().toLowerCase().endsWith(".yaml"))) {
                            if (f.getName().toLowerCase().startsWith(args[1].toLowerCase())) {
                                completions.add(f.getName());
                            }
                        }
                    }
                }
            }
            return completions;
        }

        if (args.length == 3 && "addfromlist".equalsIgnoreCase(args[0])) {
            for (String product : productRegistry.getProducts().keySet()) {
                if (product.toLowerCase().startsWith(args[2].toLowerCase())) {
                    completions.add(product);
                }
            }
            return completions;
        }

        if (args.length == 4 && "addfromlist".equalsIgnoreCase(args[0])) {
            completions.add("1");
            return completions;
        }

        if (args.length == 3 && "add".equalsIgnoreCase(args[0])) {
            for (String product : productRegistry.getProducts().keySet()) {
                if (product.toLowerCase().startsWith(args[2].toLowerCase())) {
                    completions.add(product);
                }
            }
            return completions;
        }

        if (args.length == 4 && "add".equalsIgnoreCase(args[0])) {
            completions.add("1");
            return completions;
        }

        return completions;
    }
}
