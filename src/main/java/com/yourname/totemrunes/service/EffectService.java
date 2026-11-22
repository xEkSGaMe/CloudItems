package com.yourname.totemrunes.service;

import com.yourname.totemrunes.TotemRunesPlugin;
import com.yourname.totemrunes.rune.RuneKeys;
import com.yourname.totemrunes.rune.RuneType;
import com.yourname.totemrunes.util.TextUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.damage.DamageSource;
import org.bukkit.damage.DamageType;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.EquipmentSlotGroup;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.projectiles.ProjectileSource;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

public final class EffectService {

    private final TotemRunesPlugin plugin;
    private final CooldownService cooldowns;

    private final Map<UUID, EnumMap<RuneType, Long>> activeUntil = new HashMap<>();
    private final Map<UUID, Long> stubbornResistUntil = new HashMap<>();
    private final Map<UUID, Long> parryUntil = new HashMap<>();
    private final Map<UUID, Long> furyWindowUntil = new HashMap<>();
    private final Map<UUID, Byte> furyHitUsed = new HashMap<>();

    private final NamespacedKey berserkModKey;
    private final NamespacedKey stubbornSpeedModKey;
    private final NamespacedKey furyArmorModKey;

    public EffectService(TotemRunesPlugin plugin, RuneKeys keys, CooldownService cooldowns) {
        this.plugin = plugin;
        this.cooldowns = cooldowns;
        this.berserkModKey = new NamespacedKey(plugin, "berserk_damage");
        this.stubbornSpeedModKey = new NamespacedKey(plugin, "stubborn_speed");
        this.furyArmorModKey = new NamespacedKey(plugin, "fury_armor_ignore");
    }

    public void activate(Player player, RuneType type) {
        long now = System.currentTimeMillis();
        int cooldownSec = plugin.getConfig().getInt("runes." + type.id() + ".cooldown", type.defaultCooldownSeconds());
        cooldowns.setCooldown(player.getUniqueId(), type, cooldownSec * 1000L);

        switch (type) {
            case BERSERK -> activateBerserk(player, now);
            case IMMORTALITY -> activateImmortality(player, now);
            case STUBBORN -> activateStubborn(player, now);
            case PARRY -> activateParry(player, now);
            case FURY -> activateFury(player, now);
        }
        player.playSound(player.getLocation(), type.activationSound(), 1f, 1f);
    }

    private void activateBerserk(Player player, long now) {
        int duration = plugin.getConfig().getInt("runes.berserk.duration", 10);
        double bonus = plugin.getConfig().getDouble("runes.berserk.damage-bonus", 0.05);
        setActiveUntil(player, RuneType.BERSERK, now + duration * 1000L);
        AttributeInstance attr = player.getAttribute(Attribute.ATTACK_DAMAGE);
        if (attr != null) {
            attr.removeModifier(berserkModKey);
            attr.addModifier(new AttributeModifier(berserkModKey, bonus, AttributeModifier.Operation.ADD_SCALAR, EquipmentSlotGroup.ANY));
        }
    }

    private void activateImmortality(Player player, long now) {
        int healHearts = plugin.getConfig().getInt("runes.immortality.heal-hearts", 3);
        int regenDuration = plugin.getConfig().getInt("runes.immortality.regen-duration", 12);
        int regenLevel = plugin.getConfig().getInt("runes.immortality.regen-level", 2);
        setActiveUntil(player, RuneType.IMMORTALITY, now + regenDuration * 1000L);
        AttributeInstance maxHealth = player.getAttribute(Attribute.MAX_HEALTH);
        double max = maxHealth == null ? 20.0 : maxHealth.getValue();
        player.setHealth(Math.min(max, player.getHealth() + healHearts * 2.0));
        player.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, regenDuration * 20, Math.max(0, regenLevel - 1), false, true, true));
    }

    private void activateStubborn(Player player, long now) {
        int speedDuration = plugin.getConfig().getInt("runes.stubborn.speed-duration", 15);
        int resistDuration = plugin.getConfig().getInt("runes.stubborn.resist-duration", 7);
        double speedBonus = plugin.getConfig().getDouble("runes.stubborn.speed-bonus", 0.20);
        setActiveUntil(player, RuneType.STUBBORN, now + speedDuration * 1000L);
        stubbornResistUntil.put(player.getUniqueId(), now + resistDuration * 1000L);
        AttributeInstance attr = player.getAttribute(Attribute.MOVEMENT_SPEED);
        if (attr != null) {
            attr.removeModifier(stubbornSpeedModKey);
            attr.addModifier(new AttributeModifier(stubbornSpeedModKey, speedBonus, AttributeModifier.Operation.ADD_SCALAR, EquipmentSlotGroup.ANY));
        }
    }

    private void activateParry(Player player, long now) {
        int duration = plugin.getConfig().getInt("runes.parry.duration", 7);
        long until = now + duration * 1000L;
        setActiveUntil(player, RuneType.PARRY, until);
        parryUntil.put(player.getUniqueId(), until);
    }

    private void activateFury(Player player, long now) {
        int window = plugin.getConfig().getInt("runes.fury.window", 10);
        long until = now + window * 1000L;
        setActiveUntil(player, RuneType.FURY, until);
        furyWindowUntil.put(player.getUniqueId(), until);
        furyHitUsed.remove(player.getUniqueId());
    }

    public void handleDamageByEntity(EntityDamageByEntityEvent event) {
        if (event.isCancelled()) {
            return;
        }
        handleStubbornResist(event);
        handleParry(event);
        handleFury(event);
    }

    private void handleStubbornResist(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }
        long until = stubbornResistUntil.getOrDefault(player.getUniqueId(), 0L);
        if (until <= System.currentTimeMillis()) {
            return;
        }
        double reduction = plugin.getConfig().getDouble("runes.stubborn.resist-reduction", 0.10);
        event.setDamage(event.getDamage() * (1.0 - reduction));
    }

    private void handleParry(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player victim)) {
            return;
        }
        long until = parryUntil.getOrDefault(victim.getUniqueId(), 0L);
        if (until <= System.currentTimeMillis()) {
            return;
        }
        LivingEntity reflectorTarget = resolveDamager(event.getDamager());
        if (reflectorTarget == null || reflectorTarget.getUniqueId().equals(victim.getUniqueId())) {
            return;
        }
        double ratio = plugin.getConfig().getDouble("runes.parry.reflect-ratio", 0.25);
        double reflected = event.getFinalDamage() * ratio;
        if (reflected <= 0.0) {
            return;
        }
        DamageSource source = DamageSource.builder(DamageType.THORNS)
                .withCausingEntity(victim)
                .withDirectEntity(victim)
                .build();
        reflectorTarget.damage(reflected, source);
    }

    private void handleFury(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player attacker)) {
            return;
        }
        if (!(event.getEntity() instanceof LivingEntity target)) {
            return;
        }
        UUID id = attacker.getUniqueId();
        long until = furyWindowUntil.getOrDefault(id, 0L);
        if (until <= System.currentTimeMillis() || furyHitUsed.containsKey(id)) {
            return;
        }
        double armorIgnore = plugin.getConfig().getDouble("runes.fury.armor-ignore", 0.20);
        furyHitUsed.put(id, (byte) 1);
        furyWindowUntil.remove(id);
        setActiveUntil(attacker, RuneType.FURY, 0L);

        AttributeInstance armor = target.getAttribute(Attribute.ARMOR);
        if (armor != null) {
            armor.removeModifier(furyArmorModKey);
            AttributeModifier mod = new AttributeModifier(furyArmorModKey, -armorIgnore, AttributeModifier.Operation.ADD_SCALAR, EquipmentSlotGroup.ANY);
            armor.addModifier(mod);
            Bukkit.getScheduler().runTask(plugin, () -> {
                AttributeInstance a = target.getAttribute(Attribute.ARMOR);
                if (a != null) {
                    a.removeModifier(furyArmorModKey);
                }
            });
        }
    }

    private LivingEntity resolveDamager(Entity damager) {
        if (damager instanceof Projectile projectile) {
            ProjectileSource shooter = projectile.getShooter();
            if (shooter instanceof LivingEntity living) {
                return living;
            }
            return null;
        }
        if (damager instanceof LivingEntity living) {
            return living;
        }
        return null;
    }

    public void tick() {
        long now = System.currentTimeMillis();
        for (Player player : Bukkit.getOnlinePlayers()) {
            tickPlayer(player, now);
        }
        cleanupOffline(now);
    }

    private void tickPlayer(Player player, long now) {
        UUID id = player.getUniqueId();
        EnumMap<RuneType, Long> map = activeUntil.get(id);
        if (map == null) {
            return;
        }
        if (isExpired(map, RuneType.BERSERK, now)) {
            removeModifier(player, Attribute.ATTACK_DAMAGE, berserkModKey);
            map.remove(RuneType.BERSERK);
        }
        if (isExpired(map, RuneType.STUBBORN, now)) {
            removeModifier(player, Attribute.MOVEMENT_SPEED, stubbornSpeedModKey);
            map.remove(RuneType.STUBBORN);
        }
        if (stubbornResistUntil.getOrDefault(id, 0L) <= now) {
            stubbornResistUntil.remove(id);
        }
        if (parryUntil.getOrDefault(id, 0L) <= now) {
            parryUntil.remove(id);
            if (isExpired(map, RuneType.PARRY, now)) {
                map.remove(RuneType.PARRY);
            }
        }
        if (furyWindowUntil.getOrDefault(id, 0L) <= now) {
            furyWindowUntil.remove(id);
            if (isExpired(map, RuneType.FURY, now)) {
                map.remove(RuneType.FURY);
            }
        }
        if (isExpired(map, RuneType.IMMORTALITY, now)) {
            map.remove(RuneType.IMMORTALITY);
        }
        if (map.isEmpty()) {
            activeUntil.remove(id);
        }
    }

    private boolean isExpired(EnumMap<RuneType, Long> map, RuneType type, long now) {
        Long until = map.get(type);
        return until != null && until <= now;
    }

    private void removeModifier(Player player, Attribute attribute, NamespacedKey key) {
        AttributeInstance attr = player.getAttribute(attribute);
        if (attr != null) {
            attr.removeModifier(key);
        }
    }

    private void setActiveUntil(Player player, RuneType type, long until) {
        activeUntil.computeIfAbsent(player.getUniqueId(), id -> new EnumMap<>(RuneType.class)).put(type, until);
    }

    public boolean isActive(Player player, RuneType type) {
        EnumMap<RuneType, Long> map = activeUntil.get(player.getUniqueId());
        if (map == null) {
            return false;
        }
        Long until = map.get(type);
        return until != null && until > System.currentTimeMillis();
    }

    public Map<UUID, EnumMap<RuneType, Long>> getActiveUntil() {
        return activeUntil;
    }

    public void clearPlayer(UUID id) {
        Player player = Bukkit.getPlayer(id);
        if (player != null) {
            removeModifier(player, Attribute.ATTACK_DAMAGE, berserkModKey);
            removeModifier(player, Attribute.MOVEMENT_SPEED, stubbornSpeedModKey);
        }
        activeUntil.remove(id);
        stubbornResistUntil.remove(id);
        parryUntil.remove(id);
        furyWindowUntil.remove(id);
        furyHitUsed.remove(id);
    }

    public void clearAll() {
        for (UUID id : new HashMap<>(activeUntil).keySet()) {
            clearPlayer(id);
        }
        activeUntil.clear();
        stubbornResistUntil.clear();
        parryUntil.clear();
        furyWindowUntil.clear();
        furyHitUsed.clear();
    }

    private void cleanupOffline(long now) {
        Iterator<Map.Entry<UUID, EnumMap<RuneType, Long>>> it = activeUntil.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, EnumMap<RuneType, Long>> e = it.next();
            if (Bukkit.getPlayer(e.getKey()) != null) {
                continue;
            }
            // keep state cleanup simple: remove when expired
            e.getValue().entrySet().removeIf(en -> en.getValue() <= now);
            if (e.getValue().isEmpty()) {
                it.remove();
            }
        }
    }

    public void sendCooldownActionBar(Player player, RuneType triggeredType) {
        Map<RuneType, Long> remaining = cooldowns.getReadyIn(player.getUniqueId());
        if (remaining.isEmpty()) {
            return;
        }
        StringBuilder sb = new StringBuilder();
        boolean first = true;
        for (RuneType type : RuneType.values()) {
            Long rem = remaining.get(type);
            if (rem == null || rem <= 0L) {
                continue;
            }
            if (!first) sb.append(" · ");
            sb.append(type.displayName().replace("Руна ", "")).append(": ").append(TextUtil.formatTime(rem));
            first = false;
        }
        if (sb.length() == 0) {
            return;
        }
        player.sendActionBar(Component.text(sb.toString(), TextColor.fromHexString(triggeredType.accentHex())));
    }
}
