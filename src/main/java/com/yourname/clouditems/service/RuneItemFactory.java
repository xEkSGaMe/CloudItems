package com.yourname.clouditems.service;

import com.yourname.clouditems.CloudItems;
import com.yourname.clouditems.rune.RuneKeys;
import com.yourname.clouditems.rune.RuneType;
import com.yourname.clouditems.resourcepack.ResourcePackModels;
import com.yourname.clouditems.util.SignatureUtil;
import com.yourname.clouditems.util.TextUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;

public final class RuneItemFactory {

    private final CloudItems plugin;
    private final RuneKeys keys;

    public RuneItemFactory(CloudItems plugin, RuneKeys keys) {
        this.plugin = plugin;
        this.keys = keys;
    }

    public ItemStack createRune(RuneType type, int amount) {
        ItemStack item = new ItemStack(type.material(), Math.max(1, amount));
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return item;
        }

        TextColor accent = TextColor.fromHexString(type.accentHex());
        meta.displayName(TextUtil.noItalic(Component.text(type.displayName(), accent)));

        List<Component> lore = new ArrayList<>();
        lore.add(TextUtil.text(type.effectSummary(), "#FFFFFF"));
        lore.add(TextUtil.text("Откат: " + cooldownSeconds(type) + "с", "#A0A0A0"));
        lore.add(Component.empty());
        lore.add(TextUtil.noItalic(Component.text("\"" + type.quote() + "\"", TextColor.fromHexString("#707070")).decorate(TextDecoration.ITALIC)));
        lore.add(Component.empty());
        lore.add(TextUtil.text("Наложи на Талисман", "#8A8A8A"));
        lore.add(TextUtil.text("на наковальне", "#8A8A8A"));
        meta.lore(lore);

        // Ванильные подсказки шаблона кузнеца не относятся к руне — скрываем их.
        meta.addItemFlags(
                ItemFlag.HIDE_ADDITIONAL_TOOLTIP,
                ItemFlag.HIDE_ARMOR_TRIM,
                ItemFlag.HIDE_ATTRIBUTES,
                ItemFlag.HIDE_ENCHANTS
        );

        meta.setEnchantmentGlintOverride(true);
        meta.setCustomModelData(modelData(type));
        // CloudPack: all runes share one custom item definition/texture.
        meta.setItemModel(ResourcePackModels.RUNE);

        String secret = plugin.getConfig().getString("secret", "");
        meta.getPersistentDataContainer().set(keys.runeId(), PersistentDataType.STRING, type.id());
        meta.getPersistentDataContainer().set(keys.signature(), PersistentDataType.STRING, SignatureUtil.sign(secret, type.id()));

        item.setItemMeta(meta);
        return item;
    }

    public boolean isRune(ItemStack item) {
        return getRuneType(item) != null;
    }

    public RuneType getRuneType(ItemStack item) {
        if (item == null || item.getType().isAir() || !item.hasItemMeta()) {
            return null;
        }
        ItemMeta meta = item.getItemMeta();
        String runeId = meta.getPersistentDataContainer().get(keys.runeId(), PersistentDataType.STRING);
        String sig = meta.getPersistentDataContainer().get(keys.signature(), PersistentDataType.STRING);
        RuneType type = RuneType.byId(runeId);
        if (type == null) {
            return null;
        }
        if (item.getType() != type.material()) {
            return null;
        }
        if (!meta.hasCustomModelData() || meta.getCustomModelData() != modelData(type)) {
            return null;
        }
        String secret = plugin.getConfig().getString("secret", "");
        if (!SignatureUtil.verify(secret, type.id(), sig)) {
            return null;
        }
        return type;
    }

    public int modelData(RuneType type) {
        return plugin.getConfig().getInt("rune-model-data." + type.id(), type.defaultModelData());
    }

    public int cooldownSeconds(RuneType type) {
        return plugin.getConfig().getInt("runes." + type.id() + ".cooldown", type.defaultCooldownSeconds());
    }
}
