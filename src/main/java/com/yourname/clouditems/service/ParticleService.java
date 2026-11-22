package com.yourname.clouditems.service;

import com.yourname.clouditems.CloudItems;
import com.yourname.clouditems.rune.RuneType;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.EnumMap;
import java.util.Map;
import java.util.UUID;

public final class ParticleService {

    private static final float DUST_SIZE = 0.5f;

    private final CloudItems plugin;
    private final EffectService effectService;
    private BukkitTask task;
    private double angle = 0.0;

    public ParticleService(CloudItems plugin, EffectService effectService) {
        this.plugin = plugin;
        this.effectService = effectService;
    }

    public void start() {
        stop();
        task = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            angle += Math.PI / 16;
            effectService.tick();
            for (Player player : Bukkit.getOnlinePlayers()) {
                spawnActive(player);
            }
        }, 2L, 2L);
    }

    public void stop() {
        if (task != null) {
            task.cancel();
            task = null;
        }
    }

    public void spawnActivationBurst(Player player, RuneType type) {
        Location base = player.getLocation().add(0, 0.8, 0);

        // Один тихий круг в цвете руны — общий для всех эффектов.
        spawnRing(base, type, 0.8, 10);

        // Небольшая деталь, соответствующая именно этому эффекту.
        switch (type) {
            case BERSERK -> spawnBurst(base.clone().add(0, 0.6, 0), Particle.CRIT, 5, 0.25, 0.05);
            case IMMORTALITY -> spawnBurst(base.clone().add(0, 0.7, 0), Particle.HEART, 3, 0.2, 0.03);
            case STUBBORN -> spawnBurst(base, Particle.SMOKE, 4, 0.25, 0.02);
            case PARRY -> spawnBurst(base, Particle.CRIT, 4, 0.3, 0.04);
            case FURY -> spawnBurst(base, Particle.CRIT, 5, 0.25, 0.05);
        }
    }

    private void spawnActive(Player player) {
        UUID id = player.getUniqueId();
        EnumMap<RuneType, Long> map = effectService.getActiveUntil().get(id);
        if (map == null || map.isEmpty()) {
            return;
        }
        long now = System.currentTimeMillis();
        Location base = player.getLocation().add(0, 0.8, 0);
        for (Map.Entry<RuneType, Long> entry : map.entrySet()) {
            if (entry.getValue() <= now) {
                continue;
            }
            RuneType type = entry.getKey();
            switch (type) {
                case BERSERK -> spawnRingPoint(base, type, 0.75, angle);
                case IMMORTALITY -> {
                    spawnRingPoint(base, type, 0.75, angle + Math.PI);
                    if (player.getTicksLived() % 20 == 0) {
                        spawnBurst(base.clone().add(0, 0.7, 0), Particle.HEART, 1, 0.15, 0.0);
                    }
                }
                case STUBBORN -> spawnRingPoint(base, type, 0.75, angle + Math.PI / 2);
                case PARRY -> spawnRingPoint(base, type, 0.85, -angle);
                case FURY -> spawnRingPoint(base, type, 0.75, angle * 1.2);
            }
        }
    }

    private void spawnRing(Location center, RuneType type, double radius, int points) {
        for (int i = 0; i < points; i++) {
            double a = 2 * Math.PI * i / points;
            Location p = center.clone().add(Math.cos(a) * radius, 0, Math.sin(a) * radius);
            p.getWorld().spawnParticle(Particle.DUST, p, 1, 0, 0, 0, 0, type.dustOptions(DUST_SIZE));
        }
    }

    private void spawnRingPoint(Location center, RuneType type, double radius, double a) {
        Location p = center.clone().add(Math.cos(a) * radius, 0.05, Math.sin(a) * radius);
        p.getWorld().spawnParticle(Particle.DUST, p, 1, 0, 0, 0, 0, type.dustOptions(DUST_SIZE));
    }

    private void spawnBurst(Location base, Particle particle, int count, double spread, double speed) {
        base.getWorld().spawnParticle(particle, base, count, spread, spread, spread, speed);
    }
}
