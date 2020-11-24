package io.github.hielkemaps.racecommand.race;

import io.github.hielkemaps.racecommand.Util;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.entity.Player;

public class RaceResult implements Comparable<RaceResult> {

    private final int place;
    private final int time;
    private final Player player;

    public RaceResult(Player player, int place, int time) {
        this.place = place;
        this.time = time;
        this.player = player;
    }

    public Player getPlayer() {
        return player;
    }

    public int getTime() {
        return time;
    }

    @Override
    public int compareTo(RaceResult o) {
        return Integer.compare(this.place, o.place);
    }

    @Override
    public String toString() {
        StringBuilder s = new StringBuilder();

        if (place == 1) s.append(ChatColor.GOLD);
        else if (place == 2) s.append(ChatColor.GRAY);
        else if (place == 3) s.append(ChatColor.of("#a46628"));
        else s.append(ChatColor.DARK_GRAY);

        s.append(ChatColor.BOLD);

        s.append(Util.ordinal(place)).append(": ").append(ChatColor.RESET).append(player.getName()).append(ChatColor.DARK_GRAY).append(" - ").append(ChatColor.GRAY).append(Util.getTimeString(time));
        return s.toString();
    }

    public int getPlace() {
        return place;
    }
}
