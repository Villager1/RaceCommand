package io.github.hielkemaps.racecommand.race;

import java.util.HashMap;
import java.util.UUID;

public class RaceManager {

    public static HashMap<UUID, Race> races = new HashMap<>();

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
}
