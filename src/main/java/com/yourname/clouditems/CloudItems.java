package com.yourname.clouditems;

import com.yourname.clouditems.command.RunesCommand;
import com.yourname.clouditems.listener.AnvilListener;
import com.yourname.clouditems.listener.CombatListener;
import com.yourname.clouditems.listener.InteractListener;
import com.yourname.clouditems.listener.PlayerStateListener;
import com.yourname.clouditems.listener.SmithingListener;
import com.yourname.clouditems.rune.RuneKeys;
import com.yourname.clouditems.service.CooldownService;
import com.yourname.clouditems.service.EffectService;
import com.yourname.clouditems.service.ParticleService;
import com.yourname.clouditems.service.RuneItemFactory;
import com.yourname.clouditems.service.TotemItemFactory;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

public final class CloudItems extends JavaPlugin {

    private RuneKeys keys;
    private CooldownService cooldownService;
    private EffectService effectService;
    private ParticleService particleService;
    private RuneItemFactory runeItemFactory;
    private TotemItemFactory totemItemFactory;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        ensureSecret();

        this.keys = new RuneKeys(this);
        this.cooldownService = new CooldownService();
        this.runeItemFactory = new RuneItemFactory(this, keys);
        this.totemItemFactory = new TotemItemFactory(this, keys, runeItemFactory);
        this.effectService = new EffectService(this, keys, cooldownService);
        this.particleService = new ParticleService(this, effectService);
        this.particleService.start();

        getServer().getPluginManager().registerEvents(new AnvilListener(this, keys, runeItemFactory, totemItemFactory), this);
        getServer().getPluginManager().registerEvents(new SmithingListener(this, keys, runeItemFactory), this);
        getServer().getPluginManager().registerEvents(new InteractListener(this, keys, runeItemFactory, cooldownService, effectService), this);
        getServer().getPluginManager().registerEvents(new CombatListener(effectService), this);
        getServer().getPluginManager().registerEvents(new PlayerStateListener(effectService), this);

        RunesCommand command = new RunesCommand(this, runeItemFactory);
        PluginCommand runes = getCommand("runes");
        if (runes != null) {
            runes.setExecutor(command);
            runes.setTabCompleter(command);
        }
    }

    @Override
    public void onDisable() {
        if (particleService != null) {
            particleService.stop();
        }
        if (effectService != null) {
            effectService.clearAll();
        }
    }

    public void reloadPluginConfig() {
        reloadConfig();
        ensureSecret();
    }

    private void ensureSecret() {
        String secret = getConfig().getString("secret", "");
        if (secret == null || secret.isBlank()) {
            secret = java.util.UUID.randomUUID().toString().replace("-", "")
                    + java.util.UUID.randomUUID().toString().replace("-", "");
            getConfig().set("secret", secret);
            saveConfig();
        }
    }

    public RuneKeys getKeys() {
        return keys;
    }

    public CooldownService getCooldownService() {
        return cooldownService;
    }

    public EffectService getEffectService() {
        return effectService;
    }

    public RuneItemFactory getRuneItemFactory() {
        return runeItemFactory;
    }

    public TotemItemFactory getTotemItemFactory() {
        return totemItemFactory;
    }

    public ParticleService getParticleService() {
        return particleService;
    }
}
