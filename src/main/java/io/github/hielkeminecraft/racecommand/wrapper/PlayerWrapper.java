package io.github.hielkeminecraft.racecommand.wrapper;

import org.bukkit.entity.Player;

public class PlayerWrapper {

    private Player player;
    private boolean inRace = false;

    public PlayerWrapper(Player player){
     this.player = player;
    }

    public Player getPlayer() {
        return player;
    }

    public boolean isInRace(){
        return inRace;
    }

    public void setInRace(boolean b) {
        inRace = b;
    }
}
