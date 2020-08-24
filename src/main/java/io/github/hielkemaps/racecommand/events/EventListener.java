package io.github.hielkemaps.racecommand.events;

import dev.jorel.commandapi.CommandAPI;
import io.github.hielkemaps.racecommand.race.RaceManager;
import io.github.hielkemaps.racecommand.race.Race;
import io.github.hielkemaps.racecommand.wrapper.PlayerManager;
import org.bukkit.Location;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.UUID;

public class EventListener implements Listener {

    @EventHandler
    public void OnPlayerJoin(PlayerJoinEvent e) {

        PlayerManager.insertPlayer(e.getPlayer().getUniqueId());
        CommandAPI.updateRequirements(e.getPlayer());
    }

    @EventHandler
    public void OnPlayerQuit(PlayerQuitEvent e) {

        UUID player = e.getPlayer().getUniqueId();
        Race race = RaceManager.getRace(player);
        if (race != null) {
            if (race.isOwner(player)) {
                RaceManager.disbandRace(player);
            } else {
                race.leavePlayer(player);
            }
        }
    }

    @EventHandler
    public void OnPlayerMove(PlayerMoveEvent e) {

        //Freeze players in starting race
        if(PlayerManager.getPlayer(e.getPlayer().getUniqueId()).isInRace()) {

            Race race = RaceManager.getRace(e.getPlayer().getUniqueId());
            if (race == null) return;

            if (race.isStarting()) {
                Location to = e.getFrom();
                to.setPitch(e.getTo().getPitch());
                to.setYaw(e.getTo().getYaw());
                e.setTo(to);
            }
        }
    }
}
