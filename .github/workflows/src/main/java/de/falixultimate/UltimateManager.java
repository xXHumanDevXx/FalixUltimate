package de.falixultimate;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.permissions.PermissionAttachment;

import java.io.File;
import java.io.IOException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Verwaltet Abos, speichert sie in plugins/FalixUltimate/data.yml und vergibt die Perk-Rechte. */
public class UltimateManager {

    public record Entry(String name, long until) {
        public boolean active() {
            return until > System.currentTimeMillis();
        }
    }

    private static final DateTimeFormatter FMT =
            DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm").withZone(ZoneId.systemDefault());

    private final FalixUltimate plugin;
    private final File file;
    private final Map<UUID, Entry> entries = new HashMap<>();
    private final Map<UUID, PermissionAttachment> attachments = new HashMap<>();

    public UltimateManager(FalixUltimate plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "data.yml");
    }

    public void load() {
        if (!file.exists()) return;
        YamlConfiguration y = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection sec = y.getConfigurationSection("players");
        if (sec == null) return;
        for (String key : sec.getKeys(false)) {
            try {
                entries.put(UUID.fromString(key),
                        new Entry(sec.getString(key + ".name", "?"), sec.getLong(key + ".until", 0L)));
            } catch (IllegalArgumentException ignored) { }
        }
    }

    public synchronized void save() {
        YamlConfiguration y = new YamlConfiguration();
        for (Map.Entry<UUID, Entry> en : entries.entrySet()) {
            String base = "players." + en.getKey() + ".";
            y.set(base + "name", en.getValue().name());
            y.set(base + "until", en.getValue().until());
        }
        try {
            plugin.getDataFolder().mkdirs();
            y.save(file);
        } catch (IOException ex) {
            plugin.getLogger().severe("data.yml konnte nicht gespeichert werden: " + ex.getMessage());
        }
    }

    /** Verlängert das Abo. Läuft noch eines, wird ab dessen Ende addiert. */
    public void grant(UUID id, String name, int days) {
        Entry old = entries.get(id);
        long now = System.currentTimeMillis();
        long base = (old != null && old.until() > now) ? old.until() : now;
        entries.put(id, new Entry(name, base + days * 86_400_000L));
        save();
        Player p = Bukkit.getPlayer(id);
        if (p != null) apply(p);
    }

    public boolean revoke(UUID id) {
        Entry removed = entries.remove(id);
        save();
        Player p = Bukkit.getPlayer(id);
        if (p != null) clear(p);
        return removed != null;
    }

    public Entry get(UUID id) {
        return entries.get(id);
    }

    public boolean isActive(UUID id) {
        Entry e = entries.get(id);
        return e != null && e.active();
    }

    public Map<UUID, Entry> all() {
        return Collections.unmodifiableMap(entries);
    }

    public String expiryText(Entry e) {
        return FMT.format(Instant.ofEpochMilli(e.until()));
    }

    /** Gibt dem Spieler die Perk-Rechte, wenn sein Abo aktiv ist. */
    public void apply(Player p) {
        clear(p);
        if (!isActive(p.getUniqueId())) return;
        PermissionAttachment att = p.addAttachment(plugin);
        for (String node : plugin.getConfig().getStringList("perks.permissions")) {
            att.setPermission(node, true);
        }
        attachments.put(p.getUniqueId(), att);
    }

    /** Entfernt die Perk-Rechte des Spielers. */
    public void clear(Player p) {
        PermissionAttachment att = attachments.remove(p.getUniqueId());
        if (att != null) p.removeAttachment(att);
    }

    /** Entfernt abgelaufene Abos und informiert online Spieler. */
    public void checkExpired() {
        List<UUID> expired = new ArrayList<>();
        for (Map.Entry<UUID, Entry> en : entries.entrySet()) {
            if (!en.getValue().active()) expired.add(en.getKey());
        }
        if (expired.isEmpty()) return;
        for (UUID id : expired) {
            entries.remove(id);
            Player p = Bukkit.getPlayer(id);
            if (p != null) {
                clear(p);
                p.sendMessage(plugin.msg("expired"));
            }
        }
        save();
    }
}
