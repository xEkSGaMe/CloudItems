package com.yourname.totemrunes.service;

import com.yourname.totemrunes.rune.RuneType;
import org.bukkit.entity.Player;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class CooldownService {

    private final Map<UUID, EnumMap<RuneType, Long>> cooldowns = new HashMap<>();

    public boolean isReady(UUID playerId, RuneType type) {
        return remainingMillis(playerId, type) <= 0L;
    }

    public long remainingMillis(UUID playerId, RuneType type) {
        Map<RuneType, Long> map = cooldowns.get(playerId);
        if (map == null) {
            return 0L;
        }
        long until = map.getOrDefault(type, 0L);
        long remaining = until - System.currentTimeMillis();
        return Math.max(0L, remaining);
    }

    public void setCooldown(UUID playerId, RuneType type, long cooldownMillis) {
        cooldowns.computeIfAbsent(playerId, id -> new EnumMap<>(RuneType.class))
                .put(type, System.currentTimeMillis() + Math.max(0L, cooldownMillis));
    }

    public void clearPlayer(UUID playerId) {
        cooldowns.remove(playerId);
    }

    public Map<RuneType, Long> getReadyIn(UUID playerId) {
        EnumMap<RuneType, Long> result = new EnumMap<>(RuneType.class);
        Map<RuneType, Long> map = cooldowns.get(playerId);
        if (map == null) {
            return result;
        }
        long now = System.currentTimeMillis();
        for (Map.Entry<RuneType, Long> entry : map.entrySet()) {
            long rem = entry.getValue() - now;
            if (rem > 0L) {
                result.put(entry.getKey(), rem);
            }
        }
        return result;
    }

    public void clearAll() {
        cooldowns.clear();
    }

    public void onQuit(Player player) {
        // Cooldowns are per player in memory and should remain while server runs? Spec says
        // "смена/поломка тотема КД не сбрасывает. /runes reload КД не трогает." It doesn't say quit clears.
        // Keep them.
    }
}
