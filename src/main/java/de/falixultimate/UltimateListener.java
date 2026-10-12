package de.falixultimate;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

/** Vergibt beim Join die Rechte und räumt beim Verlassen auf. */
public class UltimateListener implements Listener {

    private final FalixUltimate plugin;

    public UltimateListener(FalixUltimate plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        plugin.manager().apply(e.getPlayer());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        plugin.manager().clear(e.getPlayer());
    }
}
