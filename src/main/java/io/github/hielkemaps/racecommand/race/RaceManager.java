package io.github.hielkemaps.racecommand.race;

import org.bukkit.entity.Player;

import java.util.*;

public class RaceManager {

    public static HashMap<UUID, Race> races = new HashMap<>();
    public static final List<UUID> publicRaces = new ArrayList<>();

    public static void addRace(Race race){
        races.put(race.getOwner(),race);
    }

    public static Race getRace(UUID player) {
        Race race = races.get(player);

        //If player is owner
        if(race != null) return race;

        //Else we have to search
        for(Race r : races.values())
        {
            if(r.hasPlayer(player)){
                return r;
            }
        }
        return null;
    }

    public static void disbandRace(UUID uniqueId) {
        Race race = races.get(uniqueId);
        if(race != null){
            race.disband();
            races.remove(uniqueId);
        }
    }

    public static boolean hasJoinablePublicRace(UUID uuid) {

        for(UUID owner: publicRaces){
            //If player is not owner or player in public race
            if(!owner.equals(uuid) && !races.get(owner).getPlayers().contains(uuid)) {
                return true;
            }
        }
        return false;
    }
}
