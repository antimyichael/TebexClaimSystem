package org.datathorn.claimstore.model;

import org.bukkit.Material;

import java.util.Collections;
import java.util.List;

public class ProductDefinition {

    private final String id;
    private final String displayName;
    private final List<String> commands;
    private final Material material;
    private final int requiredSlotsPerUnit;

    public ProductDefinition(String id, String displayName, List<String> commands, Material material, int requiredSlotsPerUnit) {
        this.id = id;
        this.displayName = displayName;
        this.commands = commands;
        this.material = material;
        this.requiredSlotsPerUnit = requiredSlotsPerUnit;
    }

    public String getId() {
        return id;
    }

    public String getDisplayName() {
        return displayName;
    }

    public List<String> getCommands() {
        return Collections.unmodifiableList(commands);
    }

    public Material getMaterial() {
        return material;
    }

    public int getRequiredSlotsPerUnit() {
        return requiredSlotsPerUnit;
    }
}

