package io.github.hielkemaps.racecommand.events;

import io.github.hielkemaps.racecommand.race.RaceManager;
import io.github.hielkemaps.racecommand.race.Race;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.UUID;

public class EventListener implements Listener {

    @EventHandler
    public void OnPlayerQuit(PlayerQuitEvent e) {

        UUID player = e.getPlayer().getUniqueId();
        Race race = RaceManager.getRace(player);
        if (race != null) {
            if (race.isOwner(player)) {
                RaceManager.disbandRace(player);
            } else {
                race.removePlayer(player);
            }
        }
    }
}
