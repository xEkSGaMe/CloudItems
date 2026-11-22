package com.yourname.totemrunes.listener;

import com.yourname.totemrunes.TotemRunesPlugin;
import com.yourname.totemrunes.rune.RuneKeys;
import com.yourname.totemrunes.service.RuneItemFactory;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.PrepareSmithingEvent;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class SmithingListener implements Listener {

    private final TotemRunesPlugin plugin;
    private final RuneItemFactory runeItemFactory;
    private final Map<UUID, Long> messageThrottle = new HashMap<>();

    public SmithingListener(TotemRunesPlugin plugin, RuneKeys keys, RuneItemFactory runeItemFactory) {
        this.plugin = plugin;
        this.runeItemFactory = runeItemFactory;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPrepareSmithing(PrepareSmithingEvent event) {
        ItemStack template = event.getInventory().getInputTemplate();
        if (!runeItemFactory.isRune(template)) {
            return;
        }
        event.setResult(null);
        if (event.getView().getPlayer() instanceof Player player) {
            long now = System.currentTimeMillis();
            long last = messageThrottle.getOrDefault(player.getUniqueId(), 0L);
            if (now - last >= 1000L) {
                messageThrottle.put(player.getUniqueId(), now);
                String msg = plugin.getConfig().getString("messages.smithing-blocked", "Руны нельзя использовать на кузнечном столе");
                player.sendActionBar(Component.text(msg, TextColor.fromHexString("#FF5555")));
            }
        }
    }
}
