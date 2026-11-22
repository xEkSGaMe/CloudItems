package com.yourname.totemrunes.listener;

import com.yourname.totemrunes.TotemRunesPlugin;
import com.yourname.totemrunes.rune.RuneKeys;
import com.yourname.totemrunes.rune.RuneType;
import com.yourname.totemrunes.service.CooldownService;
import com.yourname.totemrunes.service.EffectService;
import com.yourname.totemrunes.service.RuneItemFactory;
import com.yourname.totemrunes.util.TextUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.Set;

public final class InteractListener implements Listener {

    private final TotemRunesPlugin plugin;
    private final RuneKeys keys;
    private final RuneItemFactory runeItemFactory;
    private final CooldownService cooldownService;
    private final EffectService effectService;

    public InteractListener(TotemRunesPlugin plugin, RuneKeys keys, RuneItemFactory runeItemFactory,
                            CooldownService cooldownService, EffectService effectService) {
        this.plugin = plugin;
        this.keys = keys;
        this.runeItemFactory = runeItemFactory;
        this.cooldownService = cooldownService;
        this.effectService = effectService;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onInteract(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.OFF_HAND) {
            return;
        }
        Action action = event.getAction();
        boolean allowBlock = plugin.getConfig().getBoolean("activation.allow-block-click", true);
        if (action != Action.RIGHT_CLICK_AIR && !(allowBlock && action == Action.RIGHT_CLICK_BLOCK)) {
            return;
        }

        Player player = event.getPlayer();
        if (!player.hasPermission("runes.use")) {
            return;
        }
        if (plugin.getConfig().getBoolean("activation.require-sneak", false) && !player.isSneaking()) {
            return;
        }

        ItemStack offhand = player.getInventory().getItemInOffHand();
        if (offhand.getType() != Material.TOTEM_OF_UNDYING || !offhand.hasItemMeta()) {
            return;
        }

        PersistentDataContainer root = offhand.getItemMeta().getPersistentDataContainer();
        PersistentDataContainer imprinted = root.get(keys.imprinted(), PersistentDataType.TAG_CONTAINER);
        if (imprinted == null) {
            return;
        }

        boolean activatedAny = false;
        TextComponent.Builder cooldownBar = Component.text();
        boolean firstCooldown = true;

        for (RuneType type : RuneType.values()) {
            Byte flag = imprinted.get(new org.bukkit.NamespacedKey(plugin, "rune_" + type.id()), PersistentDataType.BYTE);
            if (flag == null || flag != (byte) 1) {
                continue;
            }
            long rem = cooldownService.remainingMillis(player.getUniqueId(), type);
            if (rem > 0L) {
                if (!firstCooldown) {
                    cooldownBar.append(Component.text(" · ", TextColor.fromHexString("#B0B0B0")));
                }
                cooldownBar.append(Component.text(type.displayName().replace("Руна ", "") + ": " + TextUtil.formatTime(rem),
                        TextColor.fromHexString(type.accentHex())));
                firstCooldown = false;
                continue;
            }
            effectService.activate(player, type);
            if (plugin.getParticleService() != null) {
                plugin.getParticleService().spawnActivationBurst(player, type);
            }
            activatedAny = true;
        }

        if (!firstCooldown) {
            player.sendActionBar(cooldownBar.build());
        }
    }

}
