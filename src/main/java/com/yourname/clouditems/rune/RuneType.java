package com.yourname.clouditems.rune;

import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.inventory.meta.trim.TrimPattern;

import java.util.Locale;

public enum RuneType {
    BERSERK(
            "berserk",
            "Руна Берсерка",
            "Страж · древняя отделка",
            Material.SENTRY_ARMOR_TRIM_SMITHING_TEMPLATE,
            TrimPattern.SENTRY,
            10001,
            "#FF3B30",
            "Остриё стража жаждет крови",
            "+5% урона · 10 сек",
            120,
            Sound.ENTITY_PLAYER_LEVELUP
    ),
    IMMORTALITY(
            "immortality",
            "Руна Бессмертия",
            "Хранитель · древняя отделка",
            Material.WARD_ARMOR_TRIM_SMITHING_TEMPLATE,
            TrimPattern.WARD,
            10002,
            "#7CE38B",
            "Жизнь не отпускает того, кто держится",
            "+3 сердца сразу, Регенерация II на 12 сек",
            120,
            Sound.ITEM_TOTEM_USE
    ),
    STUBBORN(
            "stubborn",
            "Руна Упрямства",
            "Вредина · древняя отделка",
            Material.VEX_ARMOR_TRIM_SMITHING_TEMPLATE,
            TrimPattern.VEX,
            10003,
            "#35D7E8",
            "Шаг вперёд, даже сквозь бурю",
            "+20% скорости 15 сек; -10% входящего урона от существ 7 сек",
            120,
            Sound.ENTITY_EXPERIENCE_ORB_PICKUP
    ),
    PARRY(
            "parry",
            "Руна Парирования",
            "Поток · древняя отделка",
            Material.FLOW_ARMOR_TRIM_SMITHING_TEMPLATE,
            TrimPattern.FLOW,
            10004,
            "#B57BFF",
            "Ответный удар рождается в тишине",
            "Отражение 25% урона любому источнику-сущности, 7 сек",
            150,
            Sound.BLOCK_ANVIL_USE
    ),
    FURY(
            "fury",
            "Руна Ярости",
            "Шпиль · древняя отделка",
            Material.SPIRE_ARMOR_TRIM_SMITHING_TEMPLATE,
            TrimPattern.SPIRE,
            10005,
            "#FF8A1F",
            "Гнев прорубает даже сталь",
            "Следующий удар игнорирует 20% брони, окно 10 сек",
            150,
            Sound.ENTITY_PLAYER_LEVELUP
    );

    private final String id;
    private final String displayName;
    private final String subtitle;
    private final Material material;
    private final TrimPattern trimPattern;
    private final int defaultModelData;
    private final String accentHex;
    private final String quote;
    private final String effectSummary;
    private final int defaultCooldownSeconds;
    private final Sound activationSound;

    RuneType(String id, String displayName, String subtitle, Material material, TrimPattern trimPattern,
             int defaultModelData, String accentHex, String quote, String effectSummary,
             int defaultCooldownSeconds, Sound activationSound) {
        this.id = id;
        this.displayName = displayName;
        this.subtitle = subtitle;
        this.material = material;
        this.trimPattern = trimPattern;
        this.defaultModelData = defaultModelData;
        this.accentHex = accentHex;
        this.quote = quote;
        this.effectSummary = effectSummary;
        this.defaultCooldownSeconds = defaultCooldownSeconds;
        this.activationSound = activationSound;
    }

    public String id() {
        return id;
    }

    public String displayName() {
        return displayName;
    }

    public String subtitle() {
        return subtitle;
    }

    public Material material() {
        return material;
    }

    public TrimPattern trimPattern() {
        return trimPattern;
    }

    public int defaultModelData() {
        return defaultModelData;
    }

    public String accentHex() {
        return accentHex;
    }

    public String quote() {
        return quote;
    }

    public String effectSummary() {
        return effectSummary;
    }

    public int defaultCooldownSeconds() {
        return defaultCooldownSeconds;
    }

    public Sound activationSound() {
        return activationSound;
    }

    public Color accentColor() {
        return Color.fromRGB(
                Integer.parseInt(accentHex.substring(1, 3), 16),
                Integer.parseInt(accentHex.substring(3, 5), 16),
                Integer.parseInt(accentHex.substring(5, 7), 16)
        );
    }

    public Particle.DustOptions dustOptions(float size) {
        return new Particle.DustOptions(accentColor(), size);
    }

    public static RuneType byId(String id) {
        if (id == null) {
            return null;
        }
        String normalized = id.toLowerCase(Locale.ROOT);
        for (RuneType type : values()) {
            if (type.id.equals(normalized)) {
                return type;
            }
        }
        return null;
    }
}
