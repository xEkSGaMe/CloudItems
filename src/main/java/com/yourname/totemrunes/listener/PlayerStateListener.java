package com.yourname.totemrunes.listener;

import com.yourname.totemrunes.service.EffectService;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public final class PlayerStateListener implements Listener {

    private final EffectService effectService;

    public PlayerStateListener(EffectService effectService) {
        this.effectService = effectService;
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        effectService.clearPlayer(event.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        effectService.clearPlayer(event.getEntity().getUniqueId());
    }
}
