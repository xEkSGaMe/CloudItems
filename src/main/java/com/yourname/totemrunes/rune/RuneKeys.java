package com.yourname.totemrunes.rune;

import org.bukkit.NamespacedKey;
import org.bukkit.plugin.java.JavaPlugin;

public final class RuneKeys {

    private final NamespacedKey runeId;
    private final NamespacedKey signature;
    private final NamespacedKey imprinted;

    public RuneKeys(JavaPlugin plugin) {
        this.runeId = new NamespacedKey(plugin, "rune_id");
        this.signature = new NamespacedKey(plugin, "sig");
        this.imprinted = new NamespacedKey(plugin, "imprinted");
    }

    public NamespacedKey runeId() {
        return runeId;
    }

    public NamespacedKey signature() {
        return signature;
    }

    public NamespacedKey imprinted() {
        return imprinted;
    }
}
