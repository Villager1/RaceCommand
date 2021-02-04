package io.github.hielkemaps.racecommand.events;

import dev.jorel.commandapi.CommandAPI;
import io.github.hielkemaps.racecommand.race.Race;
import io.github.hielkemaps.racecommand.race.RaceManager;
import io.github.hielkemaps.racecommand.wrapper.PlayerManager;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Score;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.ScoreboardManager;

import java.util.UUID;

public class EventListener implements Listener {

    @EventHandler
    public void OnPlayerJoin(PlayerJoinEvent e) {

        //Remove inRace tag if player is not in current active race
        boolean removeTag = true;
        Race race = RaceManager.getRace(e.getPlayer().getUniqueId());
        if (race != null) {
            if (race.hasStarted()) {
                removeTag = false;

                //if player rejoins in active race, we must sync times with the other players
                race.syncTime(e.getPlayer());
            }
        }
        if (removeTag) e.getPlayer().removeScoreboardTag("inRace");

        PlayerManager.insertPlayer(e.getPlayer().getUniqueId());
        CommandAPI.updateRequirements(e.getPlayer());
    }

    @EventHandler
    public void OnPlayerQuit(PlayerQuitEvent e) {

        //Leave race when race hasn't started
        //Otherwise you could easily cheat because you don't get tped when the race starts
        UUID player = e.getPlayer().getUniqueId();
        Race race = RaceManager.getRace(player);
        if (race != null) {

            //if after leaving there are no players left in the race, we disband it
            if (race.getOnlinePlayerCount() == 1) RaceManager.disbandRace(race.getOwner());

            if (!race.isOwner(player)) {
                if (!race.hasStarted()) {
                    race.leavePlayer(player); //player leaves the race if it hasn't started yet
                }
            }
        }
    }

    @EventHandler
    public void OnPlayerDamaged(EntityDamageByEntityEvent e) {

        //If player damages another player
        if (e.getDamager() instanceof Player && e.getEntity() instanceof Player) {
            UUID player = e.getEntity().getUniqueId();
            UUID attacker = e.getDamager().getUniqueId();

            Race playerRace = RaceManager.getRace(player);

            //If race has pvp enabled
            if (playerRace != null && playerRace.isPvp()) {

                //If both players are in the same race
                if (playerRace.getPlayers().contains(attacker)) {

                    //if race has started
                    if (playerRace.hasStarted()) {

                        //If both players are ingame
                        if (!playerRace.hasFinished(player) && (!playerRace.hasFinished(attacker) || (playerRace.variantType().equals("manhunt") && (e.getDamager().getScoreboardTags().contains("hunter") && e.getEntity().getScoreboardTags().contains("runner"))))) {
                            e.setCancelled(false); //allow pvp
                            if(playerRace.variantType().equals("manhunt")) {
                                e.getEntity().removeScoreboardTag("runner");
                                e.getEntity().addScoreboardTag("hunter");
                            }
                            return;
                        }
                    }
                }
            }
            e.setCancelled(true); //disable pvp
        }
    }

    @EventHandler
    public void OnPlayerMove(PlayerMoveEvent e) {

        //Freeze players when starting race
        if (PlayerManager.getPlayer(e.getPlayer().getUniqueId()).isInRace()) {

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
