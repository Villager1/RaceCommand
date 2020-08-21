package io.github.hielkemaps.racecommand.wrapper;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PlayerManager {

    private static final Map<UUID, PlayerWrapper> players = new HashMap<>();

    public static PlayerWrapper getPlayer(Player player) {
        if (!players.containsKey(player.getUniqueId())) {
            players.put(player.getUniqueId(), new PlayerWrapper(player));
        }
        return players.get(player.getUniqueId());
    }

    public static PlayerWrapper getPlayer(UUID uuid) {
        if (!players.containsKey(uuid)) {
            players.put(uuid, new PlayerWrapper(Bukkit.getPlayer(uuid)));
        }
        return players.get(uuid);
    }
}
