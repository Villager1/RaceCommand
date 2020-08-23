package io.github.hielkemaps.racecommand.wrapper;

import dev.jorel.commandapi.CommandAPI;
import io.github.hielkemaps.racecommand.race.Race;
import io.github.hielkemaps.racecommand.race.RaceManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.*;

public class PlayerWrapper {

    private final UUID uuid;
    private boolean inRace = false;
    private final Set<UUID> raceInvited = new HashSet<>();
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

    public void invitePlayerToRun(UUID invited) {
        raceInvited.add(invited);
        PlayerManager.getPlayer(invited).addInvite(getPlayer());
    }

    public boolean hasJoinableRace() {
        return !raceInvites.isEmpty() || RaceManager.hasJoinablePublicRace(uuid);
    }

    public String[] getJoinableRaces() {

        List<String> names = new ArrayList<>();

        //Add invites
        for (UUID uuid : raceInvites) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null) names.add(p.getName());
        }

        //Add open races
        for (UUID uuid : RaceManager.publicRaces) {
            Player owner = Bukkit.getPlayer(uuid);
            if(owner != null){
                //Exclude when player is owner
                if (!uuid.equals(this.uuid)) {
                    names.add(owner.getName());
                }
            }
        }
        return names.toArray(new String[0]);
    }

    private void addInvite(Player sender) {
        raceInvites.add(sender.getUniqueId());
        updateRequirements();
    }

    public boolean acceptInvite(UUID sender) {
        Race race = RaceManager.getRace(sender);
        if(race == null) return false;

        //Join race
        race.addPlayer(uuid);

        //Update requirements
        PlayerManager.getPlayer(sender).removeInvited(uuid);
        raceInvites.remove(sender);
        updateRequirements();
        return true;
    }

    private void removeInvited(UUID invited) {
        raceInvited.remove(invited);
    }

    public boolean hasInvited(Player p) {
        return raceInvited.contains(p.getUniqueId());
    }

    public Set<UUID> getInvitedPlayers() {
        return raceInvited;
    }

    public void removeInvite(UUID from) {
        raceInvites.remove(from);

    }

    public void updateRequirements(){
        Player player = getPlayer();
        if(player == null)return;

        CommandAPI.updateRequirements(player);
    }

    public Set<UUID> getInvites() {
        return raceInvites;
    }
}
