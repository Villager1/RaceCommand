package io.github.hielkemaps.racecommand.race;

import dev.jorel.commandapi.CommandAPI;
import io.github.hielkemaps.racecommand.Main;
import io.github.hielkemaps.racecommand.wrapper.PlayerManager;
import io.github.hielkemaps.racecommand.wrapper.PlayerWrapper;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

public class Race {

    private final UUID owner;
    private final List<UUID> players = new ArrayList<>();
    private boolean isPublic = false;
    private int countDown = 5;

    private BukkitTask countDownTask;
    private BukkitTask tickTask;

    public Race(UUID owner) {
        this.owner = owner;
        players.add(owner);

        PlayerWrapper pw = PlayerManager.getPlayer(owner);
        pw.setInRace(true);
    }

    public void start() {

        AtomicInteger seconds = new AtomicInteger(countDown);

        //Tp players to start
        for (UUID uuid : players) {
            Player player = Bukkit.getPlayer(uuid);
            if (player == null) continue;

            player.performCommand("/trigger restart");
        }

        //Countdown
        countDownTask = Bukkit.getScheduler().runTaskTimerAsynchronously(Main.getInstance(), () -> {
            for (UUID uuid : players) {
                Player player = Bukkit.getPlayer(uuid);
                if (player == null) continue;

                StringBuilder sb = new StringBuilder();

                if (seconds.get() == 1) sb.append(ChatColor.RED);
                else if (seconds.get() == 2) sb.append(ChatColor.GOLD);
                else if (seconds.get() == 3) sb.append(ChatColor.YELLOW);
                else if (seconds.get() > 3) sb.append(ChatColor.GREEN);

                sb.append(ChatColor.BOLD).append(seconds.toString());
                player.sendTitle(" ", sb.toString(), 2, 18, 2);

                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_HARP, 1, 1);
            }
            seconds.getAndDecrement();
        }, 0, 20);

        //Stop countdown
        Bukkit.getScheduler().runTaskLaterAsynchronously(Main.getInstance(), () -> {
            countDownTask.cancel();

            for (UUID uuid : players) {
                Player player = Bukkit.getPlayer(uuid);
                if (player == null) continue;

                player.sendTitle(" ", ChatColor.BOLD + "GO", 2, 18, 2);
                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_HARP, 1, 2);
            }
        }, 20 * countDown);

        //Every second
        tickTask = Bukkit.getScheduler().runTaskTimerAsynchronously(Main.getInstance(), () -> {
            for (UUID uuid : players) {
                Player player = Bukkit.getPlayer(uuid);
                if (player == null) continue;

                player.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, 20, 1, true, false, false));
            }
        }, 0, 20);
    }

    public void stop() {
        cancelTasks();
    }

    public boolean setCountDown(int value) {
        if (countDown == value) return true;

        countDown = value;
        return true;
    }

    public void addPlayer(UUID uuid) {
        players.forEach(p -> {
            Objects.requireNonNull(Bukkit.getPlayer(p)).sendMessage(Main.PREFIX + Objects.requireNonNull(Bukkit.getPlayer(uuid)).getName() + " has joined the race");
        });
        players.add(uuid);

        PlayerWrapper pw = PlayerManager.getPlayer(uuid);
        pw.setInRace(true);

        PlayerManager.getPlayer(owner).updateRequirements();
    }

    public boolean hasPlayer(UUID uuid) {
        return players.contains(uuid);
    }

    public void leavePlayer(UUID uuid) {
        removePlayer(uuid);

        players.forEach(p -> {
            Objects.requireNonNull(Bukkit.getPlayer(p)).sendMessage(Main.PREFIX + Objects.requireNonNull(Bukkit.getPlayer(uuid)).getName() + " has left the race");
        });
    }

    public void kickPlayer(UUID uuid) {
        removePlayer(uuid);

        Objects.requireNonNull(Bukkit.getPlayer(uuid)).sendMessage(Main.PREFIX + "You have been kicked from the race");

        players.forEach(p -> {
            Objects.requireNonNull(Bukkit.getPlayer(p)).sendMessage(Main.PREFIX + Objects.requireNonNull(Bukkit.getPlayer(uuid)).getName() + " has been kicked from the race");
        });
    }

    private void removePlayer(UUID uuid) {
        players.remove(uuid);

        PlayerWrapper pw = PlayerManager.getPlayer(uuid);
        pw.setInRace(false);

        PlayerManager.getPlayer(owner).updateRequirements();
    }

    public List<UUID> getPlayers() {
        return players;
    }

    protected void disband() {
        cancelTasks();
        
        //get race off public visibility
        setIsPublic(false);

        //Clear outgoing invites
        PlayerManager.getPlayer(owner).getInvitedPlayers().forEach(uuid -> {
            PlayerWrapper wPlayer = PlayerManager.getPlayer(uuid);
            wPlayer.removeInvite(owner);
        });

        players.forEach(p -> {
            PlayerWrapper pw = PlayerManager.getPlayer(p);
            pw.setInRace(false);

            //Tell other players in race
            if (!p.equals(owner)) {
                Objects.requireNonNull(Bukkit.getPlayer(p)).sendMessage(Main.PREFIX + Objects.requireNonNull(Bukkit.getPlayer(owner)).getName() + " has disbanded the race");
            }
        });
    }

    private void cancelTasks() {
        countDownTask.cancel();
        tickTask.cancel();
    }

    public boolean isOwner(UUID uniqueId) {
        return uniqueId.equals(owner);
    }

    public UUID getOwner() {
        return owner;
    }

    public boolean isPublic() {
        return isPublic;
    }

    public boolean setIsPublic(boolean value) {

        //Same value, do nothing
        if (value == isPublic) return false;

        this.isPublic = value;

        if (value) {
            RaceManager.publicRaces.add(owner);
        } else {
            RaceManager.publicRaces.remove(owner);
        }

        //Let everyone know
        for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
            CommandAPI.updateRequirements(onlinePlayer);
        }
        return true;
    }
}
