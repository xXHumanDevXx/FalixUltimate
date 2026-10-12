package de.falixultimate;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/** /ultimate | info <Spieler> | grant <Spieler> <Tage> | remove <Spieler> | list | reload */
public class UltimateCommand implements CommandExecutor, TabCompleter {

    private record Target(UUID id, String name) { }

    private final FalixUltimate plugin;

    public UltimateCommand(FalixUltimate plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender s, Command c, String label, String[] a) {
        if (a.length == 0) {
            status(s);
            return true;
        }
        switch (a[0].toLowerCase(Locale.ROOT)) {
            case "info" -> {
                if (!admin(s)) return true;
                if (a.length < 2) { usage(s, "/ultimate info <Spieler>"); return true; }
                Target t = resolve(s, a[1]);
                if (t == null) return true;
                UltimateManager.Entry e = plugin.manager().get(t.id());
                if (e != null && e.active()) {
                    s.sendMessage(plugin.msg("info-active", "{player}", t.name(),
                            "{until}", plugin.manager().expiryText(e)));
                } else {
                    s.sendMessage(plugin.msg("info-inactive", "{player}", t.name()));
                }
            }
            case "grant" -> grant(s, a);
            case "remove" -> {
                if (!admin(s)) return true;
                if (a.length < 2) { usage(s, "/ultimate remove <Spieler>"); return true; }
                Target t = resolve(s, a[1]);
                if (t == null) return true;
                if (plugin.manager().revoke(t.id())) {
                    s.sendMessage(plugin.msg("revoked", "{player}", t.name()));
                } else {
                    s.sendMessage(plugin.msg("not-active", "{player}", t.name()));
                }
            }
            case "list" -> {
                if (!admin(s)) return true;
                list(s);
            }
            case "reload" -> {
                if (!admin(s)) return true;
                plugin.reloadConfig();
                s.sendMessage(plugin.msg("reloaded"));
            }
            default -> usage(s, "/ultimate [info | grant | remove | list | reload]");
        }
        return true;
    }

    private void status(CommandSender s) {
        if (!(s instanceof Player p)) {
            usage(s, "/ultimate [info | grant | remove | list | reload]");
            return;
        }
        UltimateManager.Entry e = plugin.manager().get(p.getUniqueId());
        if (e != null && e.active()) {
            p.sendMessage(plugin.msg("status-active", "{until}", plugin.manager().expiryText(e)));
        } else {
            p.sendMessage(plugin.msg("status-inactive"));
        }
    }

    private void grant(CommandSender s, String[] a) {
        if (!admin(s)) return;
        if (a.length < 3) { usage(s, "/ultimate grant <Spieler> <Tage>"); return; }
        int days;
        try {
            days = Integer.parseInt(a[2]);
        } catch (NumberFormatException ex) {
            s.sendMessage(plugin.msg("invalid-days"));
            return;
        }
        if (days <= 0) {
            s.sendMessage(plugin.msg("invalid-days"));
            return;
        }
        Target t = resolve(s, a[1]);
        if (t == null) return;

        plugin.manager().grant(t.id(), t.name(), days);
        UltimateManager.Entry e = plugin.manager().get(t.id());
        String until = plugin.manager().expiryText(e);

        s.sendMessage(plugin.msg("granted", "{player}", t.name(),
                "{days}", String.valueOf(days), "{until}", until));
        Player online = Bukkit.getPlayer(t.id());
        if (online != null) {
            online.sendMessage(plugin.msg("granted-you", "{days}", String.valueOf(days), "{until}", until));
        }
    }

    private void list(CommandSender s) {
        List<UUID> active = new ArrayList<>();
        for (Map.Entry<UUID, UltimateManager.Entry> en : plugin.manager().all().entrySet()) {
            if (en.getValue().active()) active.add(en.getKey());
        }
        if (active.isEmpty()) {
            s.sendMessage(plugin.msg("list-none"));
            return;
        }
        s.sendMessage(plugin.msg("list-header", "{count}", String.valueOf(active.size())));
        for (UUID id : active) {
            UltimateManager.Entry e = plugin.manager().get(id);
            s.sendMessage(plugin.msg("list-entry", "{player}", e.name(),
                    "{until}", plugin.manager().expiryText(e)));
        }
    }

    private boolean admin(CommandSender s) {
        if (s.hasPermission("falixultimate.admin")) return true;
        s.sendMessage(plugin.msg("no-permission"));
        return false;
    }

    private void usage(CommandSender s, String text) {
        s.sendMessage(plugin.msg("usage", "{usage}", text));
    }

    /** Findet Online- und Offline-Spieler, die schon einmal auf dem Server waren. */
    private Target resolve(CommandSender s, String name) {
        Player online = Bukkit.getPlayerExact(name);
        if (online != null) return new Target(online.getUniqueId(), online.getName());
        OfflinePlayer op = Bukkit.getOfflinePlayer(name);
        if (!op.hasPlayedBefore()) {
            s.sendMessage(plugin.msg("player-not-found", "{player}", name));
            return null;
        }
        return new Target(op.getUniqueId(), name);
    }

    @Override
    public List<String> onTabComplete(CommandSender s, Command c, String label, String[] a) {
        List<String> out = new ArrayList<>();
        if (a.length == 1) {
            for (String o : List.of("info", "grant", "remove", "list", "reload")) {
                if (o.startsWith(a[0].toLowerCase(Locale.ROOT))) out.add(o);
            }
        }
        return out;
    }
}
