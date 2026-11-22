package com.yourname.clouditems.listener;

import com.yourname.clouditems.CloudItems;
import com.yourname.clouditems.rune.RuneKeys;
import com.yourname.clouditems.rune.RuneType;
import com.yourname.clouditems.service.RuneItemFactory;
import com.yourname.clouditems.service.TotemItemFactory;
import com.yourname.clouditems.util.TextUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.PrepareAnvilEvent;
import org.bukkit.inventory.AnvilInventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.view.AnvilView;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class AnvilListener implements Listener {

    private final CloudItems plugin;
    private final RuneItemFactory runeItemFactory;
    private final TotemItemFactory totemItemFactory;
    private final Map<UUID, Long> messageThrottle = new HashMap<>();

    public AnvilListener(CloudItems plugin, RuneKeys keys, RuneItemFactory runeItemFactory, TotemItemFactory totemItemFactory) {
        this.plugin = plugin;
        this.runeItemFactory = runeItemFactory;
        this.totemItemFactory = totemItemFactory;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPrepareAnvil(PrepareAnvilEvent event) {
        AnvilInventory inv = event.getInventory();
        ItemStack first = inv.getItem(0);
        ItemStack second = inv.getItem(1);

        boolean firstRune = runeItemFactory.isRune(first);
        boolean secondRune = runeItemFactory.isRune(second);
        boolean firstTotem = isTotem(first);
        boolean secondTotem = isTotem(second);

        if (!firstRune && !secondRune) {
            return;
        }

        // rune + rune or totem + totem or invalid combos -> null
        if (firstRune && secondRune) {
            event.setResult(null);
            return;
        }
        if (firstTotem && secondTotem) {
            event.setResult(null);
            return;
        }

        ItemStack runeItem = firstRune ? first : second;
        ItemStack totemItem = firstTotem ? first : second;

        if (runeItem == null || totemItem == null || !isTotem(totemItem)) {
            event.setResult(null);
            return;
        }

        RuneType runeType = runeItemFactory.getRuneType(runeItem);
        if (runeType == null) {
            event.setResult(null);
            return;
        }

        if (totemItemFactory.hasRune(totemItem, runeType)) {
            event.setResult(null);
            sendThrottled(event, plugin.getConfig().getString("messages.already-imprinted", "Эта руна уже наложена на Талисман"));
            return;
        }

        ItemStack result = totemItemFactory.createImprintedTotem(totemItem, runeType);

        // Preserve rename text if player renamed the totem
        String renameText = null;
        if (event.getView() instanceof AnvilView view) {
            renameText = view.getRenameText();
        }
        if (renameText != null && !renameText.isBlank()) {
            ItemStack resultMetaItem = result;
            var meta = resultMetaItem.getItemMeta();
            if (meta != null) {
                meta.displayName(TextUtil.noItalic(Component.text(renameText)));
                resultMetaItem.setItemMeta(meta);
            }
        }

        int runeCount = totemItemFactory.getImprintedRunes(result).size();
        int cost = runeLevelCost(runeCount);

        // Стоимость наковальни — в уровнях опыта, как и ванильная стоимость.
        if (event.getView().getPlayer() instanceof Player player && player.getLevel() < cost) {
            event.setResult(null);
            sendThrottled(event, plugin.getConfig().getString("messages.no-experience",
                    "Недостаточно уровня опыта для наложения руны"));
            return;
        }

        event.setResult(result);
        event.getInventory().setResult(result);

        applyAnvilCost(event.getInventory(), event.getView(), cost);

        ItemStack firstSnapshot = first == null ? null : first.clone();
        ItemStack secondSnapshot = second == null ? null : second.clone();
        ItemStack resultSnapshot = result.clone();
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            if (!(event.getView().getPlayer() instanceof Player player)) {
                return;
            }
            if (!player.isOnline()) {
                return;
            }
            if (!(player.getOpenInventory().getTopInventory() instanceof AnvilInventory openInv)) {
                return;
            }
            ItemStack openFirst = openInv.getItem(0);
            ItemStack openSecond = openInv.getItem(1);
            if (!sameItem(openFirst, firstSnapshot) || !sameItem(openSecond, secondSnapshot)) {
                return;
            }
            if (player.getLevel() < cost) {
                openInv.setResult(null);
                return;
            }
            openInv.setResult(resultSnapshot.clone());
            applyAnvilCost(openInv, player.getOpenInventory(), cost);
        });
    }

    private static int runeLevelCost(int runeCount) {
        return switch (runeCount) {
            case 1 -> 5;
            case 2 -> 10;
            case 3 -> 15;
            case 4 -> 30;
            case 5 -> 35;
            default -> 35;
        };
    }

    private boolean isTotem(ItemStack item) {
        return item != null && item.getType() == Material.TOTEM_OF_UNDYING;
    }

    private void applyAnvilCost(AnvilInventory inventory, org.bukkit.inventory.InventoryView view, int cost) {
        inventory.setRepairCost(cost);
        inventory.setRepairCostAmount(cost);
        inventory.setMaximumRepairCost(Math.max(inventory.getMaximumRepairCost(), 40));
        if (view instanceof AnvilView anvilView) {
            anvilView.setRepairCost(cost);
            anvilView.setRepairItemCountCost(1);
            anvilView.setMaximumRepairCost(Math.max(anvilView.getMaximumRepairCost(), 40));
        }
    }

    private boolean sameItem(ItemStack a, ItemStack b) {
        if (a == null && b == null) {
            return true;
        }
        if (a == null || b == null) {
            return false;
        }
        return a.isSimilar(b) && a.getAmount() == b.getAmount();
    }

    private void sendThrottled(PrepareAnvilEvent event, String message) {
        if (!(event.getView().getPlayer() instanceof Player player)) {
            return;
        }
        long now = System.currentTimeMillis();
        long last = messageThrottle.getOrDefault(player.getUniqueId(), 0L);
        if (now - last < 1000L) {
            return;
        }
        messageThrottle.put(player.getUniqueId(), now);
        player.sendActionBar(Component.text(message, TextColor.fromHexString("#FF5555")));
    }
}
