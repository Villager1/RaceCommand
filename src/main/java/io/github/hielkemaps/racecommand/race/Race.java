package io.github.hielkemaps.racecommand.race;

import io.github.hielkemaps.racecommand.Main;
import io.github.hielkemaps.racecommand.wrapper.PlayerManager;
import io.github.hielkemaps.racecommand.wrapper.PlayerWrapper;
import org.bukkit.Bukkit;

import java.util.*;

public class Race {

    private UUID owner;
    private List<UUID> players = new ArrayList<>();

    public Race(UUID owner){
        this.owner = owner;
        players.add(owner);

        PlayerWrapper pw = PlayerManager.getPlayer(owner);
        pw.setInRace(true);
    }

    public void start(){

    }

    public void stop(){

    }

    public void addPlayer(UUID uuid){
        players.forEach(p -> {
            Objects.requireNonNull(Bukkit.getPlayer(p)).sendMessage( Main.PREFIX + Objects.requireNonNull(Bukkit.getPlayer(uuid)).getName() + " has joined the race");
        });
        players.add(uuid);

        PlayerWrapper pw = PlayerManager.getPlayer(uuid);
        pw.setInRace(true);
    }

    public boolean hasPlayer(UUID uuid){
        return players.contains(uuid);
    }

    public void removePlayer(UUID uuid){
        players.forEach(p -> {
            Objects.requireNonNull(Bukkit.getPlayer(p)).sendMessage( Main.PREFIX + Objects.requireNonNull(Bukkit.getPlayer(uuid)).getName() + " has left the race");
        });
        players.remove(uuid);

        PlayerWrapper pw = PlayerManager.getPlayer(uuid);
        pw.setInRace(false);
    }

    public List<UUID> getPlayers(){
        return players;
    }

    protected void disband() {
        players.forEach(p -> {
            PlayerWrapper pw = PlayerManager.getPlayer(p);
            pw.setInRace(false);

            //Tell other players in race
            if(!p.equals(owner)){
                Objects.requireNonNull(Bukkit.getPlayer(p)).sendMessage( Main.PREFIX + Objects.requireNonNull(Bukkit.getPlayer(owner)).getName() + " has disbanded the race");
            }
        });
    }

    public boolean isOwner(UUID uniqueId) {
        return uniqueId.equals(owner);
    }

    public UUID getOwner() {
        return owner;
    }
}
