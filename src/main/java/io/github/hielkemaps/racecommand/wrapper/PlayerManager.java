package io.github.hielkemaps.racecommand.wrapper;

import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PlayerManager {

    private static final Map<UUID, PlayerWrapper> players = new HashMap<>();

    public static void insertPlayer (UUID uuid) {
        if (!players.containsKey(uuid)) {
            players.put(uuid, new PlayerWrapper(uuid));
        }
    }

    public static PlayerWrapper getPlayer(UUID uuid) {
        if (!players.containsKey(uuid)) {
            players.put(uuid, new PlayerWrapper(uuid));
        }
        return players.get(uuid);
    }

    public static void removePlayer(Player player) {
        players.remove(player.getUniqueId());
    }
}
