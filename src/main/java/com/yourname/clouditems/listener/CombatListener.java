package com.yourname.clouditems.listener;

import com.yourname.clouditems.service.EffectService;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

public final class CombatListener implements Listener {

    private final EffectService effectService;

    public CombatListener(EffectService effectService) {
        this.effectService = effectService;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onDamage(EntityDamageByEntityEvent event) {
        effectService.handleDamageByEntity(event);
    }
}
