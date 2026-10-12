package de.falixultimate;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

/** Hauptklasse: lädt Abos, registriert Befehl und Listener, prüft Abläufe. */
public class FalixUltimate extends JavaPlugin {

    private UltimateManager manager;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        manager = new UltimateManager(this);
        manager.load();

        Bukkit.getPluginManager().registerEvents(new UltimateListener(this), this);

        UltimateCommand executor = new UltimateCommand(this);
        PluginCommand cmd = getCommand("ultimate");
        if (cmd != null) {
            cmd.setExecutor(executor);
            cmd.setTabCompleter(executor);
        }

        for (Player p : Bukkit.getOnlinePlayers()) {
            manager.apply(p);
        }

        // Jede Minute prüfen, ob Abos abgelaufen sind (1200 Ticks = 60 Sekunden)
        Bukkit.getScheduler().runTaskTimer(this, manager::checkExpired, 1200L, 1200L);
        getLogger().info("FalixUltimate aktiviert.");
    }

    @Override
    public void onDisable() {
        if (manager != null) manager.save();
    }

    public UltimateManager manager() {
        return manager;
    }

    public String color(String s) {
        return ChatColor.translateAlternateColorCodes('&', s);
    }

    /** Nachricht aus messages.<key>, Platzhalter als Paare: "{player}", name, ... */
    public String msg(String key, String... pairs) {
        String raw = getConfig().getString("messages." + key, "&cFehlende Nachricht: " + key);
        raw = raw.replace("{prefix}", getConfig().getString("messages.prefix", ""));
        raw = raw.replace("{name}", getConfig().getString("perks.name", "Ultimate"));
        raw = raw.replace("{price}", getConfig().getString("perks.price", ""));
        for (int i = 0; i + 1 < pairs.length; i += 2) {
            raw = raw.replace(pairs[i], pairs[i + 1]);
        }
        return color(raw);
    }
}
