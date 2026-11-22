package com.yourname.clouditems.service;

import com.yourname.clouditems.CloudItems;
import com.yourname.clouditems.rune.RuneKeys;
import com.yourname.clouditems.rune.RuneType;
import com.yourname.clouditems.resourcepack.ResourcePackModels;
import com.yourname.clouditems.util.TextUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

public final class TotemItemFactory {

    private final CloudItems plugin;
    private final RuneKeys keys;
    private final RuneItemFactory runeItemFactory;

    public TotemItemFactory(CloudItems plugin, RuneKeys keys, RuneItemFactory runeItemFactory) {
        this.plugin = plugin;
        this.keys = keys;
        this.runeItemFactory = runeItemFactory;
    }

    public boolean isImprintedTotem(ItemStack item) {
        if (item == null || item.getType() != Material.TOTEM_OF_UNDYING || !item.hasItemMeta()) {
            return false;
        }
        return !getImprintedRunes(item).isEmpty();
    }

    public Set<RuneType> getImprintedRunes(ItemStack item) {
        EnumSet<RuneType> result = EnumSet.noneOf(RuneType.class);
        if (item == null || item.getType() != Material.TOTEM_OF_UNDYING || !item.hasItemMeta()) {
            return result;
        }
        PersistentDataContainer root = item.getItemMeta().getPersistentDataContainer();
        PersistentDataContainer container = root.get(keys.imprinted(), PersistentDataType.TAG_CONTAINER);
        if (container == null) {
            return result;
        }
        for (RuneType type : RuneType.values()) {
            NamespacedBoolean has = new NamespacedBoolean(container, type);
            if (has.value) {
                // only accept if rune definition still valid and modeled
                if (runeItemFactory.modelData(type) > 0) {
                    result.add(type);
                }
            }
        }
        return result;
    }

    public boolean hasRune(ItemStack item, RuneType type) {
        return getImprintedRunes(item).contains(type);
    }

    public ItemStack createImprintedTotem(ItemStack baseTotem, RuneType toAdd) {
        ItemStack base = baseTotem == null ? new ItemStack(Material.TOTEM_OF_UNDYING) : baseTotem.clone();
        base.setType(Material.TOTEM_OF_UNDYING);
        base.setAmount(1);

        ItemMeta meta = base.getItemMeta();
        if (meta == null) {
            return base;
        }

        Set<RuneType> runes = getImprintedRunes(base);
        runes.add(toAdd);

        PersistentDataContainer root = meta.getPersistentDataContainer();
        PersistentDataContainer imprinted = root.getAdapterContext().newPersistentDataContainer();
        for (RuneType type : RuneType.values()) {
            imprinted.set(keyFor(type), PersistentDataType.BYTE, (byte) (runes.contains(type) ? 1 : 0));
        }
        root.set(keys.imprinted(), PersistentDataType.TAG_CONTAINER, imprinted);

        applyLore(meta, runes);

        // CloudPack: only a totem with at least one imprinted rune gets the
        // custom totem model. Plain vanilla totems keep their vanilla texture.
        if (!runes.isEmpty()) {
            meta.setItemModel(ResourcePackModels.TOTEM);
        } else {
            meta.setItemModel(null);
        }

        base.setItemMeta(meta);
        return base;
    }

    public void applyLore(ItemMeta meta, Set<RuneType> runes) {
        meta.displayName(talismanDisplayName());
        List<Component> lore = new ArrayList<>();
        lore.add(TextUtil.text("Руны: " + runes.size() + "/5", "#A0A0A0"));
        lore.add(Component.empty());
        for (RuneType type : RuneType.values()) {
            if (runes.contains(type)) {
                lore.add(TextUtil.noItalic(Component.text("- " + type.displayName(), TextColor.fromHexString(type.accentHex()))));
            }
        }
        lore.add(Component.empty());
        lore.add(TextUtil.text("ПКМ в левой руке - активировать", "#8A8A8A"));
        meta.lore(lore);
    }

    private static Component talismanDisplayName() {
        String name = "Талисман";
        String[] colors = {
                "#E9C911", "#E8C414", "#E7BF16", "#E6BA19",
                "#E5B61C", "#E4B11F", "#E3AC21", "#E2A724"
        };
        var builder = Component.text();
        for (int i = 0; i < name.length(); i++) {
            builder.append(Component.text(String.valueOf(name.charAt(i)), TextColor.fromHexString(colors[i]))
                    .decorate(TextDecoration.BOLD));
        }
        return TextUtil.noItalic(builder.build());
    }

    private org.bukkit.NamespacedKey keyFor(RuneType type) {
        return new org.bukkit.NamespacedKey(plugin, "rune_" + type.id());
    }

    private final class NamespacedBoolean {
        private final boolean value;

        private NamespacedBoolean(PersistentDataContainer container, RuneType type) {
            Byte b = container.get(keyFor(type), PersistentDataType.BYTE);
            this.value = b != null && b == (byte) 1;
        }
    }
}
