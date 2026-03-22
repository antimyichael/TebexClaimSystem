package org.datathorn.claimstore.service;

import org.datathorn.claimstore.TebexClaimSystemPlugin;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class ClaimLogger {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final File logFile;
    private final TebexClaimSystemPlugin plugin;

    public ClaimLogger(File logFile, TebexClaimSystemPlugin plugin) {
        this.logFile = logFile;
        this.plugin = plugin;
        ensureFile();
    }

    public synchronized void logClaim(String player, String product, int quantity) {
        String line = String.format("[%s] player=%s product=%s quantity=%d", FORMATTER.format(LocalDateTime.now()), player, product, quantity);
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(logFile, true))) {
            writer.write(line);
            writer.newLine();
        } catch (IOException ex) {
            plugin.getLogger().warning("Failed to write claim log: " + ex.getMessage());
        }
    }

    private void ensureFile() {
        if (!plugin.getDataFolder().exists() && !plugin.getDataFolder().mkdirs()) {
            plugin.getLogger().warning("Failed to create plugin data folder for claim logs.");
            return;
        }

        if (logFile.exists()) {
            return;
        }

        try {
            if (!logFile.createNewFile()) {
                plugin.getLogger().warning("Could not create claim log file.");
            }
        } catch (IOException ex) {
            plugin.getLogger().warning("Failed to initialize claim log file: " + ex.getMessage());
        }
    }
}

