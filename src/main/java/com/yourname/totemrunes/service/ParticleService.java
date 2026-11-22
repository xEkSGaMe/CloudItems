package com.yourname.totemrunes.service;

import com.yourname.totemrunes.TotemRunesPlugin;
import com.yourname.totemrunes.rune.RuneType;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.EnumMap;
import java.util.Map;
import java.util.UUID;

public final class ParticleService {

    private final TotemRunesPlugin plugin;
    private final EffectService effectService;
    private BukkitTask task;
    private double angle = 0.0;

    public ParticleService(TotemRunesPlugin plugin, EffectService effectService) {
        this.plugin = plugin;
        this.effectService = effectService;
    }

    public void start() {
        stop();
        task = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            angle += Math.PI / 8;
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
        Location base = player.getLocation().add(0, 1.0, 0);
        switch (type) {
            case BERSERK -> {
                spawnRing(base, type, 1.2, 24);
                spawnColumn(base, Particle.FLAME, 30);
            }
            case IMMORTALITY -> {
                spawnSphere(base, Particle.HEART, 1.1, 20);
                spawnSphere(base, Particle.GLOW, 1.2, 20);
                spawnSphere(base, Particle.TOTEM_OF_UNDYING, 1.0, 25);
            }
            case STUBBORN -> {
                spawnRing(base, type, 1.1, 24);
                spawnColumn(base, Particle.END_ROD, 30);
            }
            case PARRY -> {
                spawnSphere(base, Particle.CRIT, 1.2, 30);
                spawnSphere(base, Particle.ENCHANTED_HIT, 1.2, 30);
            }
            case FURY -> {
                spawnRing(base, type, 1.2, 20);
                spawnColumn(base, Particle.FLAME, 20);
                spawnColumn(base, Particle.LAVA, 12);
                player.getWorld().spawnParticle(Particle.ANGRY_VILLAGER, base.clone().add(0, 1.0, 0), 8, 0.4, 0.4, 0.4, 0.0);
            }
        }
    }

    private void spawnActive(Player player) {
        UUID id = player.getUniqueId();
        EnumMap<RuneType, Long> map = effectService.getActiveUntil().get(id);
        if (map == null || map.isEmpty()) {
            return;
        }
        long now = System.currentTimeMillis();
        Location base = player.getLocation().add(0, 1.0, 0);
        for (Map.Entry<RuneType, Long> entry : map.entrySet()) {
            if (entry.getValue() <= now) {
                continue;
            }
            RuneType type = entry.getKey();
            switch (type) {
                case BERSERK -> {
                    spawnRingPoint(base, type, 1.0, angle);
                    player.getWorld().spawnParticle(Particle.FLAME, base.clone().add(0, 0.2, 0), 1, 0.15, 0.2, 0.15, 0.01);
                }
                case IMMORTALITY -> {
                    if (player.getTicksLived() % 4 == 0) {
                        player.getWorld().spawnParticle(Particle.HEART, base.clone().add(0, 0.8, 0), 1, 0.4, 0.3, 0.4, 0.0);
                        player.getWorld().spawnParticle(Particle.GLOW, base, 2, 0.4, 0.5, 0.4, 0.0);
                    }
                }
                case STUBBORN -> {
                    spawnRingPoint(base, type, 0.9, angle + Math.PI);
                    player.getWorld().spawnParticle(Particle.END_ROD, base.clone().add(0, 0.2, 0), 1, 0.2, 0.2, 0.2, 0.0);
                }
                case PARRY -> {
                    spawnRingPoint(base, type, 1.1, -angle);
                    player.getWorld().spawnParticle(Particle.CRIT, base, 1, 0.4, 0.5, 0.4, 0.0);
                }
                case FURY -> {
                    spawnRingPoint(base, type, 1.0, angle * 1.2);
                    player.getWorld().spawnParticle(Particle.FLAME, base, 1, 0.25, 0.3, 0.25, 0.01);
                }
            }
        }
    }

    private void spawnRing(Location center, RuneType type, double radius, int points) {
        for (int i = 0; i < points; i++) {
            double a = 2 * Math.PI * i / points;
            Location p = center.clone().add(Math.cos(a) * radius, 0, Math.sin(a) * radius);
            center.getWorld().spawnParticle(Particle.DUST, p, 1, 0, 0, 0, 0, type.dustOptions(1.0f));
        }
    }

    private void spawnRingPoint(Location center, RuneType type, double radius, double a) {
        Location p = center.clone().add(Math.cos(a) * radius, Math.sin(a * 2) * 0.2, Math.sin(a) * radius);
        center.getWorld().spawnParticle(Particle.DUST, p, 1, 0, 0, 0, 0, type.dustOptions(1.0f));
    }

    private void spawnColumn(Location base, Particle particle, int count) {
        Location p = base.clone();
        for (int i = 0; i < count; i++) {
            double y = i * 0.08;
            double a = i * 0.35;
            Location loc = p.clone().add(Math.cos(a) * 0.3, y, Math.sin(a) * 0.3);
            p.getWorld().spawnParticle(particle, loc, 1, 0.02, 0.02, 0.02, 0.0);
        }
    }

    private void spawnSphere(Location center, Particle particle, double radius, int count) {
        for (int i = 0; i < count; i++) {
            double u = Math.random();
            double v = Math.random();
            double theta = 2 * Math.PI * u;
            double phi = Math.acos(2 * v - 1);
            double x = radius * Math.sin(phi) * Math.cos(theta);
            double y = radius * Math.cos(phi);
            double z = radius * Math.sin(phi) * Math.sin(theta);
            center.getWorld().spawnParticle(particle, center.clone().add(x, y, z), 1, 0, 0, 0, 0);
        }
    }
}
