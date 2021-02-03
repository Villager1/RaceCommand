package io.github.hielkemaps.racecommand.race;

import io.github.hielkemaps.racecommand.Util;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;

import java.util.UUID;

public class RaceResult implements Comparable<RaceResult> {

    private final int place;
    private final int time;
    private final UUID uuid;

    public RaceResult(UUID uuid, int place, int time) {
        this.place = place;
        this.time = time;
        this.uuid = uuid;
    }

    public UUID getUUID() {
        return uuid;
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
        String name = Bukkit.getOfflinePlayer(uuid).getName();
        if (name == null) name = "unknown";

        StringBuilder s = new StringBuilder();

        if (place == 1) s.append(ChatColor.GOLD);
        else if (place == 2) s.append(ChatColor.GRAY);
        else if (place == 3) s.append(ChatColor.of("#a46628"));
        else s.append(ChatColor.DARK_GRAY);

        s.append(ChatColor.BOLD);

        s.append(Util.ordinal(place)).append(": ").append(ChatColor.RESET).append(name).append(ChatColor.DARK_GRAY).append(" - ").append(ChatColor.GRAY).append(Util.getTimeString(time));
        return s.toString();
    }

    public int getPlace() {
        return place;
    }
}
