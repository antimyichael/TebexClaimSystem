package org.datathorn.claimstore;

import org.datathorn.claimstore.command.TebexClaimSystemCommand;
import org.datathorn.claimstore.gui.ClaimsGui;
import org.datathorn.claimstore.listener.ClaimGuiListener;
import org.datathorn.claimstore.listener.PlayerJoinListener;
import org.datathorn.claimstore.service.ClaimLogger;
import org.datathorn.claimstore.service.ClaimService;
import org.datathorn.claimstore.service.MessageService;
import org.datathorn.claimstore.service.PlayerListParser;
import org.datathorn.claimstore.service.ProductRegistry;
import org.datathorn.claimstore.storage.PendingClaimsRepository;
import org.bukkit.entity.Player;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;

public class TebexClaimSystemPlugin extends JavaPlugin {

    private static final long CLAIM_REMINDER_INTERVAL_TICKS = 30L * 60L * 20L;

    private ProductRegistry productRegistry;
    private BukkitTask claimReminderTask;
    private ClaimService claimService;
    private MessageService messageService;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        if (!new File(getDataFolder(), "access_list.yml").exists()) {
            saveResource("access_list.yml", false);
        }

        messageService = new MessageService(this);
        productRegistry = new ProductRegistry(this);
        PendingClaimsRepository repository = new PendingClaimsRepository(new File(getDataFolder(), "pending-claims.yml"), this);
        ClaimLogger claimLogger = new ClaimLogger(new File(getDataFolder(), "claim-log.txt"), this);
        claimService = new ClaimService(this, repository, productRegistry, messageService, claimLogger);
        ClaimsGui claimsGui = new ClaimsGui(this, claimService, productRegistry, messageService);

        PlayerListParser playerListParser = new PlayerListParser();
        TebexClaimSystemCommand command = new TebexClaimSystemCommand(
                this, claimService, claimsGui, productRegistry, messageService, playerListParser
        );
        PluginCommand rootCommand = getCommand("tebexclaimsystem");
        if (rootCommand != null) {
            rootCommand.setExecutor(command);
            rootCommand.setTabCompleter(command);
        } else {
            getLogger().severe("Command tebexclaimsystem not found in plugin.yml");
        }

        getServer().getPluginManager().registerEvents(new PlayerJoinListener(claimService, messageService), this);
        getServer().getPluginManager().registerEvents(new ClaimGuiListener(this, claimsGui, claimService), this);

        scheduleClaimReminders();
    }

    @Override
    public void onDisable() {
        cancelClaimReminderTask();
    }

    public boolean reloadPluginConfiguration() {
        try {
            reloadConfig();
            productRegistry.reload(this);
            scheduleClaimReminders();
            return true;
        } catch (Exception ex) {
            getLogger().severe("Failed to reload config: " + ex.getMessage());
            return false;
        }
    }

    private void scheduleClaimReminders() {
        cancelClaimReminderTask();
        claimReminderTask = getServer().getScheduler().runTaskTimer(this, new Runnable() {
            @Override
            public void run() {
                for (Player player : getServer().getOnlinePlayers()) {
                    if (claimService.hasPendingClaims(player.getName())) {
                        player.sendMessage(messageService.get("notifyPendingReminder"));
                    }
                }
            }
        }, CLAIM_REMINDER_INTERVAL_TICKS, CLAIM_REMINDER_INTERVAL_TICKS);
    }

    private void cancelClaimReminderTask() {
        if (claimReminderTask != null) {
            claimReminderTask.cancel();
            claimReminderTask = null;
        }
    }
}




