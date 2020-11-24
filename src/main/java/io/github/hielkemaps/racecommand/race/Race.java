package io.github.hielkemaps.racecommand.race;

import dev.jorel.commandapi.CommandAPI;
import io.github.hielkemaps.racecommand.Main;
import io.github.hielkemaps.racecommand.Util;
import io.github.hielkemaps.racecommand.wrapper.PlayerManager;
import io.github.hielkemaps.racecommand.wrapper.PlayerWrapper;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Score;
import org.bukkit.scoreboard.ScoreboardManager;
import org.bukkit.scoreboard.Team;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

public class Race {

    private final UUID owner;
    private final List<UUID> players = new ArrayList<>();
    private List<UUID> finishedPlayers = new ArrayList<>();
    private boolean isPublic = false;
    private int countDown = 5;
    private boolean isStarting = false;
    private boolean hasStarted = false;
    private final Set<UUID> InvitedPlayers = new HashSet<>();
    private boolean pvp = false;
    private boolean broadcast = false;
    private boolean ghostPlayers = true;
    private int place = 1;
    private List<RaceResult> results = new ArrayList<>();
    private BukkitTask countDownTask;
    private BukkitTask countDownStopTask;
    private BukkitTask playingTask;

    public Race(UUID owner) {
        this.owner = owner;
        players.add(owner);

        PlayerWrapper pw = PlayerManager.getPlayer(owner);
        pw.setInRace(true);
    }

    public void start() {
        place = 1;
        finishedPlayers = new ArrayList<>();
        results = new ArrayList<>();
        isStarting = true;

        AtomicInteger seconds = new AtomicInteger(countDown);

        sendMessage(Main.PREFIX + "Starting race...");

        //Tp players to start
        for (UUID uuid : players) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null) player.performCommand("restart");
        }

        //Countdown
        countDownTask = Bukkit.getScheduler().runTaskTimer(Main.getInstance(), () -> {
            for (UUID uuid : players) {
                Player player = Bukkit.getPlayer(uuid);
                if (player == null) continue;

                //Always invisible during countdown
                player.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, 100, 0, true, false, false));

                StringBuilder sb = new StringBuilder();

                if (seconds.get() == 1) sb.append(ChatColor.RED);
                else if (seconds.get() == 2) sb.append(ChatColor.GOLD);
                else if (seconds.get() == 3) sb.append(ChatColor.YELLOW);
                else if (seconds.get() > 3) sb.append(ChatColor.GREEN);

                sb.append(ChatColor.BOLD).append(seconds.toString());

                if (seconds.get() <= 10) {
                    player.sendTitle(" ", sb.toString(), 2, 18, 2);
                    player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_HARP, 1, 1);
                } else {
                    player.sendTitle(ChatColor.YELLOW + "Race starting in", sb.toString(), 0, 30, 0);
                    player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_HAT, 0.1f, 1f);
                }
            }
            seconds.getAndDecrement();
        }, 0, 20);

        //Stop countdown
        countDownStopTask = Bukkit.getScheduler().runTaskLater(Main.getInstance(), () -> {
            countDownTask.cancel();

            for (UUID uuid : players) {
                Player player = Bukkit.getPlayer(uuid);
                if (player == null) continue;

                player.sendTitle(" ", ChatColor.BOLD + "GO", 2, 18, 2);
                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_HARP, 1, 2);
                player.addScoreboardTag("inRace");

                if (Main.startFunction != null && !Main.startFunction.equals("")) {
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "execute as " + player.getName() + " run function " + Main.startFunction);
                }

                isStarting = false;
                hasStarted = true;
            }

            sendMessage(Main.PREFIX + "Race has started");
        }, 20 * countDown);

        playingTask = Bukkit.getScheduler().runTaskTimer(Main.getInstance(), () -> {

            //stop race if everyone has finished
            if (finishedPlayers.containsAll(players)) {
                stop();
                sendMessage(Main.PREFIX + "Race has ended");
                sendMessage(ChatColor.GOLD + "" + ChatColor.STRIKETHROUGH + "           " + ChatColor.RESET + "" + ChatColor.BOLD + " Results " + ChatColor.GOLD + "" + ChatColor.STRIKETHROUGH + "           ");
                results.forEach(raceResult -> sendMessage(raceResult.toString()));
                sendMessage(ChatColor.GOLD + "" + ChatColor.STRIKETHROUGH + "                                    ");
            }

            for (UUID uuid : players) {
                if (finishedPlayers.contains(uuid)) continue;

                Player player = Bukkit.getPlayer(uuid);
                if (player == null) continue;

                //No spectators in race
                if(player.getGameMode() == GameMode.SPECTATOR){
                    player.setGameMode(GameMode.ADVENTURE);
                }

                if (ghostPlayers) {
                    player.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, 50, 0, true, false, false));
                }

                Team team = PlayerManager.getPlayer(uuid).getTeam();
                if (team.getName().equals("finished")) {
                    hasFinished(player);
                }
            }
        }, countDown * 20, 1);
        updateRequirements();
    }

    private void hasFinished(Player finished) {
        //Make player visible
        finished.removePotionEffect(PotionEffectType.INVISIBILITY);

        //Get finish time
        int time = -1;
        ScoreboardManager m = Bukkit.getScoreboardManager();
        if (m != null) {
            Objective timeObjective = m.getMainScoreboard().getObjective("time");
            if (timeObjective != null) {
                Score score = timeObjective.getScore(finished.getName());
                if (score.isScoreSet()) {
                    time = score.getScore();
                }
            }
        }

        //Let players know
        sendMessage(Main.PREFIX + ChatColor.GREEN + finished.getName() + " finished " + Util.ordinal(place) + " place!" + ChatColor.WHITE + " (" + Util.getTimeString(time) + ")");

        finishedPlayers.add(finished.getUniqueId());
        results.add(new RaceResult(finished, place, time));

        place++;
    }

    private void updateRequirements() {
        //If race is public, update everyone
        if (isPublic) {
            for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
                CommandAPI.updateRequirements(onlinePlayer);
            }
        } else {
            //Otherwise update invited
            for (UUID uuid : InvitedPlayers) {
                PlayerManager.getPlayer(uuid).updateRequirements();
            }
            //Update start/stop requirement for owner only
            PlayerManager.getPlayer(owner).updateRequirements();
        }
    }

    public void stop() {
        players.forEach(p -> Objects.requireNonNull(Bukkit.getPlayer(p)).removeScoreboardTag("inRace"));
        cancelTasks();
        isStarting = false;
        hasStarted = false;
        updateRequirements();
    }

    public void setCountDown(int value) {
        countDown = value;
    }

    public void addPlayer(UUID uuid) {
        players.forEach(p -> Objects.requireNonNull(Bukkit.getPlayer(p)).sendMessage(Main.PREFIX + Objects.requireNonNull(Bukkit.getPlayer(uuid)).getName() + " has joined the race"));
        players.add(uuid);

        PlayerWrapper pw = PlayerManager.getPlayer(uuid);
        pw.setInRace(true);

        //If joined in countdown, but to start
        if (isStarting) pw.getPlayer().performCommand("restart");

        PlayerManager.getPlayer(owner).updateRequirements();
    }

    public boolean hasPlayer(UUID uuid) {
        return players.contains(uuid);
    }

    public void leavePlayer(UUID uuid) {
        removePlayer(uuid);

        players.forEach(p -> Objects.requireNonNull(Bukkit.getPlayer(p)).sendMessage(Main.PREFIX + Objects.requireNonNull(Bukkit.getPlayer(uuid)).getName() + " has left the race"));
    }

    public void kickPlayer(UUID uuid) {
        removePlayer(uuid);

        Objects.requireNonNull(Bukkit.getPlayer(uuid)).sendMessage(Main.PREFIX + "You have been kicked from the race");

        players.forEach(p -> Objects.requireNonNull(Bukkit.getPlayer(p)).sendMessage(Main.PREFIX + Objects.requireNonNull(Bukkit.getPlayer(uuid)).getName() + " has been kicked from the race"));
    }

    private void removePlayer(UUID uuid) {
        players.remove(uuid);

        PlayerWrapper pw = PlayerManager.getPlayer(uuid);
        pw.getPlayer().removeScoreboardTag("inRace");
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
        getInvitedPlayers().forEach(uuid -> {
            PlayerWrapper wPlayer = PlayerManager.getPlayer(uuid);
            wPlayer.removeInvite(owner);
        });

        players.forEach(p -> {
            PlayerWrapper pw = PlayerManager.getPlayer(p);
            pw.setInRace(false);
            pw.getPlayer().removeScoreboardTag("inRace");

            //Tell other players in race
            if (!p.equals(owner)) {
                Objects.requireNonNull(Bukkit.getPlayer(p)).sendMessage(Main.PREFIX + Objects.requireNonNull(Bukkit.getPlayer(owner)).getName() + " has disbanded the race");
            }
        });
    }

    private void cancelTasks() {
        if (countDownTask != null) countDownTask.cancel();
        if (countDownStopTask != null) countDownStopTask.cancel();
        if (playingTask != null) playingTask.cancel();
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
        if (value == isPublic) return false;

        isPublic = value;

        if (value) {
            RaceManager.publicRaces.add(owner);
        } else {
            RaceManager.publicRaces.remove(owner);
        }

        updateRequirements();
        return true;
    }

    public boolean isPvp() {
        return pvp;
    }

    public boolean setPvp(boolean value) {
        if (value == pvp) return false;

        pvp = value;
        return true;
    }

    public boolean setBroadcast(boolean value) {
        if (value == broadcast) return false;

        broadcast = value;
        return true;
    }

    public boolean isStarting() {
        return isStarting;
    }

    public boolean hasStarted() {
        return hasStarted;
    }

    public boolean hasFinished(UUID player) {
        return finishedPlayers.contains(player);
    }

    public void invitePlayer(UUID invited) {
        InvitedPlayers.add(invited);
        PlayerManager.getPlayer(invited).addInvite(owner);
    }

    public void removeInvited(UUID invited) {
        InvitedPlayers.remove(invited);
    }

    public boolean hasInvited(Player p) {
        return InvitedPlayers.contains(p.getUniqueId());
    }

    public Set<UUID> getInvitedPlayers() {
        return InvitedPlayers;
    }

    public void sendMessage(String message) {

        if (broadcast) {
            Bukkit.getOnlinePlayers().forEach(player -> player.sendMessage(message));
        } else {
            for (UUID uuid : players) {
                Player p = Bukkit.getPlayer(uuid);
                if (p != null) p.sendMessage(message);
            }
        }
    }

    public boolean setGhostPlayers(boolean value) {
        if (value == ghostPlayers) return false;

        ghostPlayers = value;
        return true;
    }

    public List<RaceResult> getResults() {
        return results;
    }
}
