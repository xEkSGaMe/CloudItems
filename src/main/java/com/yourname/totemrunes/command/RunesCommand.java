package com.yourname.totemrunes.command;

import com.yourname.totemrunes.TotemRunesPlugin;
import com.yourname.totemrunes.rune.RuneType;
import com.yourname.totemrunes.service.RuneItemFactory;
import com.yourname.totemrunes.util.TextUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

public final class RunesCommand implements CommandExecutor, TabCompleter {

    private final TotemRunesPlugin plugin;
    private final RuneItemFactory runeItemFactory;

    public RunesCommand(TotemRunesPlugin plugin, RuneItemFactory runeItemFactory) {
        this.plugin = plugin;
        this.runeItemFactory = runeItemFactory;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!sender.hasPermission("runes.admin")) {
            sender.sendMessage(Component.text("Нет прав.", TextColor.fromHexString("#FF5555")));
            return true;
        }
        if (args.length == 0) {
            sendUsage(sender);
            return true;
        }

        String sub = args[0].toLowerCase(Locale.ROOT);
        switch (sub) {
            case "give" -> handleGive(sender, args);
            case "reload" -> handleReload(sender);
            case "list" -> handleList(sender);
            case "info" -> handleInfo(sender, args);
            default -> sendUsage(sender);
        }
        return true;
    }

    private void handleGive(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage(Component.text("Использование: /runes give <player> <rune|all> [amount]", TextColor.fromHexString("#FFAA00")));
            return;
        }
        Player target = Bukkit.getPlayerExact(args[1]);
        if (target == null) {
            sender.sendMessage(Component.text("Игрок не найден.", TextColor.fromHexString("#FF5555")));
            return;
        }

        String runeArg = args[2].toLowerCase(Locale.ROOT);
        int amount = 1;
        if (args.length >= 4) {
            try {
                amount = Math.max(1, Math.min(64, Integer.parseInt(args[3])));
            } catch (NumberFormatException ignored) {
                sender.sendMessage(Component.text("Некорректное количество.", TextColor.fromHexString("#FF5555")));
                return;
            }
        }

        List<RuneType> toGive = new ArrayList<>();
        if ("all".equals(runeArg)) {
            toGive.addAll(Arrays.asList(RuneType.values()));
        } else {
            RuneType type = RuneType.byId(runeArg);
            if (type == null) {
                sender.sendMessage(Component.text("Неизвестная руна: " + runeArg, TextColor.fromHexString("#FF5555")));
                return;
            }
            toGive.add(type);
        }

        for (RuneType type : toGive) {
            ItemStack item = runeItemFactory.createRune(type, amount);
            target.getInventory().addItem(item).values().forEach(left ->
                    target.getWorld().dropItemNaturally(target.getLocation(), left));
            String msg = plugin.getConfig().getString("messages.given", "Выдан предмет: {rune}")
                    .replace("{rune}", type.displayName());
            sender.sendMessage(Component.text(msg, TextColor.fromHexString(type.accentHex())));
        }
    }

    private void handleReload(CommandSender sender) {
        plugin.reloadPluginConfig();
        sender.sendMessage(Component.text("Конфиг TotemRunes перезагружен. Секрет и активные КД сохранены.", TextColor.fromHexString("#7CE38B")));
    }

    private void handleList(CommandSender sender) {
        sender.sendMessage(TextUtil.noItalic(Component.text("Руны TotemRunes", TextColor.fromHexString("#FFD76E")).decorate(TextDecoration.BOLD)));
        for (RuneType type : RuneType.values()) {
            sender.sendMessage(Component.text("- " + type.displayName() + " (" + type.id() + ")", TextColor.fromHexString(type.accentHex())));
        }
    }

    private void handleInfo(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(Component.text("Использование: /runes info <rune>", TextColor.fromHexString("#FFAA00")));
            return;
        }
        RuneType type = RuneType.byId(args[1]);
        if (type == null) {
            sender.sendMessage(Component.text("Неизвестная руна: " + args[1], TextColor.fromHexString("#FF5555")));
            return;
        }
        sender.sendMessage(TextUtil.noItalic(Component.text(type.displayName(), TextColor.fromHexString(type.accentHex())).decorate(TextDecoration.BOLD)));
        sender.sendMessage(Component.text(type.subtitle(), TextColor.fromHexString("#A0A0A0")));
        sender.sendMessage(Component.text(type.effectSummary(), TextColor.fromHexString("#FFFFFF")));
        sender.sendMessage(Component.text("Откат: " + runeItemFactory.cooldownSeconds(type) + " сек", TextColor.fromHexString("#A0A0A0")));
        sender.sendMessage(Component.text("\"" + type.quote() + "\"", TextColor.fromHexString("#707070")).decorate(TextDecoration.ITALIC));
    }

    private void sendUsage(CommandSender sender) {
        sender.sendMessage(Component.text("Использование: /runes <give|reload|list|info>", TextColor.fromHexString("#FFAA00")));
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        if (!sender.hasPermission("runes.admin")) {
            return List.of();
        }
        if (args.length == 1) {
            return filter(List.of("give", "reload", "list", "info"), args[0]);
        }
        if (args.length == 2 && "give".equalsIgnoreCase(args[0])) {
            List<String> players = Bukkit.getOnlinePlayers().stream().map(Player::getName).collect(Collectors.toList());
            return filter(players, args[1]);
        }
        if (args.length == 3 && "give".equalsIgnoreCase(args[0])) {
            List<String> runes = Arrays.stream(RuneType.values()).map(RuneType::id).collect(Collectors.toList());
            runes.add("all");
            return filter(runes, args[2]);
        }
        if (args.length == 4 && "give".equalsIgnoreCase(args[0])) {
            return filter(List.of("1", "8", "16", "32", "64"), args[3]);
        }
        if (args.length == 2 && "info".equalsIgnoreCase(args[0])) {
            return filter(Arrays.stream(RuneType.values()).map(RuneType::id).toList(), args[1]);
        }
        return List.of();
    }

    private List<String> filter(List<String> values, String token) {
        String lower = token.toLowerCase(Locale.ROOT);
        List<String> result = new ArrayList<>();
        for (String value : values) {
            if (value.toLowerCase(Locale.ROOT).startsWith(lower)) {
                result.add(value);
            }
        }
        return result;
    }
}
