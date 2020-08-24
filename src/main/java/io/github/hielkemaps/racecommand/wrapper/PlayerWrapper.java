package io.github.hielkemaps.racecommand.wrapper;

import dev.jorel.commandapi.CommandAPI;
import io.github.hielkemaps.racecommand.race.Race;
import io.github.hielkemaps.racecommand.race.RaceManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.ScoreboardManager;
import org.bukkit.scoreboard.Team;

import java.util.*;

public class PlayerWrapper {

    private final UUID uuid;
    private boolean inRace = false;
    private final Set<UUID> raceInvites = new HashSet<>();

    public PlayerWrapper(UUID uuid) {
        this.uuid = uuid;
    }

    public Player getPlayer() {
        return Bukkit.getPlayer(uuid);
    }

    public boolean isInRace() {
        return inRace;
    }

    public void setInRace(boolean b) {
        inRace = b;
        updateRequirements();
    }

    public boolean hasJoinableRace() {
        return !raceInvites.isEmpty() || RaceManager.hasJoinablePublicRace(uuid);
    }

    public String[] getJoinableRaces() {
        List<String> joinable = new ArrayList<>();

        //Add invites
        for (UUID uuid : raceInvites) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null) {

                Race race = RaceManager.getRace(p.getUniqueId());
                if (race != null && !race.hasStarted()) {
                    joinable.add(p.getName());
                }
            }
        }
        //Add open races
        for (UUID uuid : RaceManager.publicRaces) {
            Player owner = Bukkit.getPlayer(uuid);
            if (owner != null) {
                //Exclude when player is owner
                if (!uuid.equals(this.uuid)) {

                    Race race = RaceManager.getRace(owner.getUniqueId());
                    if (race != null && !race.hasStarted()) {
                        joinable.add(owner.getName());
                    }
                }
            }
        }
        return joinable.toArray(new String[0]);
    }

    public void addInvite(UUID sender) {
        raceInvites.add(sender);
        updateRequirements();
    }

    public boolean acceptInvite(UUID sender) {
        Race race = RaceManager.getRace(sender);
        if (race == null) return false;

        //Join race
        race.addPlayer(uuid);
        race.removeInvited(uuid);

        //Update requirements
        raceInvites.remove(sender);
        updateRequirements();
        return true;
    }

    public void removeInvite(UUID from) {
        raceInvites.remove(from);

    }

    public void updateRequirements() {
        Player player = getPlayer();
        if (player == null) return;

        CommandAPI.updateRequirements(player);
    }

    public Set<UUID> getInvites() {
        return raceInvites;
    }

    public Team getTeam() {
        Player p = Bukkit.getPlayer(uuid);
        if(p == null) return null;

        ScoreboardManager manager = Bukkit.getServer().getScoreboardManager();
        if(manager == null) return null;

        Scoreboard scoreboard = manager.getMainScoreboard();

        for (Team team : scoreboard.getTeams()) {
            if (team.hasEntry(p.getName())) return team;
        }
        return null;

    }
}
