package io.github.hielkemaps.racecommand.race;

import com.sun.tools.javac.jvm.Items;
import dev.jorel.commandapi.CommandAPI;
import io.github.hielkemaps.racecommand.Main;
import io.github.hielkemaps.racecommand.Util;
import io.github.hielkemaps.racecommand.wrapper.PlayerManager;
import io.github.hielkemaps.racecommand.wrapper.PlayerWrapper;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.scoreboard.*;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

public class Race {

    private final String name;
    private final UUID owner;
    private final List<UUID> players = new ArrayList<>();
    private List<UUID> finishedPlayers = new ArrayList<>();
    private List<UUID> huntedPlayers = new ArrayList<>();
    private boolean isPublic = false;
    private int countDown = 5;
    private boolean isStarting = false;
    private boolean hasStarted = false;
    private final Set<UUID> InvitedPlayers = new HashSet<>();
    private boolean pvp = false;
    private String mode = "normal";
    private boolean broadcast = false;
    private boolean ghostPlayers = false;
    private int place = 1;
    private int huntedPlace = 1;
    private List<RaceResult> results = new ArrayList<>();
    private List<RaceResult> hunted = new ArrayList<>();
    private BukkitTask countDownTask;
    private BukkitTask countDownStopTask;
    private BukkitTask playingTask;

    public Race(UUID owner, String name) {
        this.owner = owner;
        players.add(owner);
        this.name = name;

        PlayerWrapper pw = PlayerManager.getPlayer(owner);
        pw.setInRace(true);
    }

    public void start() {
        place = 1;
        huntedPlace = 1;
        finishedPlayers = new ArrayList<>();
        huntedPlayers = new ArrayList<>();
        results = new ArrayList<>();
        isStarting = true;

        AtomicInteger seconds = new AtomicInteger(countDown);

        sendMessage(Main.PREFIX + "Starting race...");

        for(UUID uuid : players) {
            Player player = Bukkit.getPlayer(uuid);
            player.removeScoreboardTag("hunter");
            player.removeScoreboardTag("runner");
        }

        //Set hunter for manhunt races
        if(variantType() == "manhunt") {
            int hunterRandomizer = (int) Math.random() * players.size();
            int selectedPlayer = 0;
            for (UUID uuid : players) {
                Player player = Bukkit.getPlayer(uuid);
                if (player != null) {
                    if (selectedPlayer == hunterRandomizer) {
                        player.addScoreboardTag("hunter");
                    } else {
                        player.addScoreboardTag("runner");
                    }
                    selectedPlayer++;
                }
            }
        }

        //Tp players to start
        for (UUID uuid : players) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null) player.performCommand("restart");
            if(player.getScoreboardTags().contains("hunter")) player.performCommand("reset");
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
                executeStartFunction(player);

                isStarting = false;
                hasStarted = true;
            }

            sendMessage(Main.PREFIX + "Race has started");
        }, 20 * countDown);

        playingTask = Bukkit.getScheduler().runTaskTimer(Main.getInstance(), () -> {

            //stop race if everyone has finished
            List<UUID> runners = new ArrayList<>();
            for(UUID uuid : players) {
                Player player = Bukkit.getPlayer(uuid);
                if(!player.getScoreboardTags().contains("hunter")) runners.add(uuid);
            }
            if (finishedPlayers.containsAll(runners) || runners.size() <= 0) {
                stop();
                sendMessage(Main.PREFIX + "Race has ended");
                for(UUID uuid : players) {
                    Player player = Bukkit.getPlayer(uuid);
                    player.removeScoreboardTag("hunter");
                    player.removeScoreboardTag("runner");
                }
            }

            for (UUID uuid : players) {
                if (finishedPlayers.contains(uuid)) continue;

                Player player = Bukkit.getPlayer(uuid);
                if (player == null) continue;

                //No spectators in race
                if (player.getGameMode() == GameMode.SPECTATOR) {
                    player.setGameMode(GameMode.ADVENTURE);
                }

                if (ghostPlayers) {
                    player.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, 50, 0, true, false, false));
                }

                if(player.getScoreboardTags().contains("hunter")) {
                    player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 50, 0, true, false, true));

                    //Set hunter's helmet
                    ItemStack playerHelmet = new ItemStack(Material.ZOMBIE_HEAD);
                    playerHelmet.getItemMeta().setDisplayName(ChatColor.RED + "Hunter\'s Mask");
                    player.getInventory().setHelmet(playerHelmet);

                    //Set hunter's axe
                    ItemStack playerAxe = new ItemStack(Material.NETHERITE_AXE);
                    playerAxe.getItemMeta().setDisplayName(ChatColor.RED + "Hunter\'s Axe");
                    ArrayList<String> axeLore = new ArrayList<>();
                    axeLore.add(ChatColor.YELLOW + "Hunter\'s Touch");
                    playerAxe.getItemMeta().setLore(axeLore);
                    player.getInventory().setItem(0, playerAxe);
                }

                if(player.getScoreboardTags().contains("hunter") && !huntedPlayers.contains(player) && !finishedPlayers.contains(player)) {
                    hasFinished(player);
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

        if(finished.getScoreboardTags().contains("hunter")) {
            //Let players know
            sendMessage(Main.PREFIX + ChatColor.RED + finished.getName() + " has become a hunter!" + ChatColor.WHITE + " (" + Util.getTimeString(time) + ")");

            huntedPlayers.add(finished.getUniqueId());
            hunted.add(0, new RaceResult(finished.getUniqueId(), huntedPlace, time));

            huntedPlace++;
        } else {
            //Let players know
            sendMessage(Main.PREFIX + ChatColor.GREEN + finished.getName() + " finished " + Util.ordinal(place) + " place!" + ChatColor.WHITE + " (" + Util.getTimeString(time) + ")");

            finishedPlayers.add(finished.getUniqueId());
            results.add(new RaceResult(finished.getUniqueId(), place, time));

            place++;
        }
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
        //show results if any
        if (results.size() > 0) printResults();

        for (UUID p : players) {
            Player player = Bukkit.getPlayer(p);
            if (player != null) player.removeScoreboardTag("inRace");
        }
        cancelTasks();
        isStarting = false;
        hasStarted = false;
        updateRequirements();
    }

    public void setCountDown(int value) {
        countDown = value;
    }

    public void addPlayer(UUID uuid) {
        Player addedPlayer = Bukkit.getPlayer(uuid);
        if (addedPlayer == null) return;

        sendMessageToRaceMembers(Main.PREFIX + addedPlayer.getName() + " has joined the race");
        players.add(uuid);

        PlayerWrapper pw = PlayerManager.getPlayer(uuid);
        pw.setInRace(true);

        //If joined during countdown, tp to start
        if (isStarting) {
            addedPlayer.performCommand("restart");
        }

        PlayerManager.getPlayer(owner).updateRequirements();
    }

    public boolean hasPlayer(UUID uuid) {
        return players.contains(uuid);
    }

    public void leavePlayer(UUID uuid) {
        removePlayer(uuid);
        OfflinePlayer leftPlayer = Bukkit.getOfflinePlayer(uuid);
        sendMessageToRaceMembers(Main.PREFIX + leftPlayer.getName() + " has left the race");
    }

    public void kickPlayer(UUID uuid) {
        removePlayer(uuid);

        OfflinePlayer kickedPlayer = Bukkit.getOfflinePlayer(uuid);
        if (kickedPlayer.isOnline()) {
            kickedPlayer.getPlayer().sendMessage(Main.PREFIX + "You have been kicked from the race");
        }

        sendMessageToRaceMembers(Main.PREFIX + kickedPlayer.getName() + " has been kicked from the race");
    }

    private void removePlayer(UUID uuid) {
        players.remove(uuid);

        PlayerWrapper pw = PlayerManager.getPlayer(uuid);
        pw.setInRace(false);

        Player player = Bukkit.getPlayer(uuid);
        if (player != null) {
            player.removeScoreboardTag("inRace");
        }

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

        for (UUID p : players) {
            PlayerWrapper pw = PlayerManager.getPlayer(p);
            pw.setInRace(false);

            Player player = Bukkit.getPlayer(p);
            if (player == null) continue; //we can do nothing with offline players

            player.removeScoreboardTag("inRace");

            if (!p.equals(owner)) player.sendMessage(Main.PREFIX + this.name + " has disbanded the race");
        }
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

    public String variantType() { return mode; }

    public boolean setVariantType(String value) {
        if (value == mode) return false;

        mode = value;
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

    public boolean hasBeenHunted(UUID player) {
        return huntedPlayers.contains(player);
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
            sendMessageToRaceMembers(message);
        }
    }

    public void sendMessageToRaceMembers(String message) {
        for (UUID uuid : players) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null) p.sendMessage(message);
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

    public void printResults() {
        sendMessage(ChatColor.GOLD + "" + ChatColor.STRIKETHROUGH + "           " + ChatColor.RESET + "" + ChatColor.BOLD + " Results " + ChatColor.GOLD + "" + ChatColor.STRIKETHROUGH + "           ");
        if(variantType() == "manhunt") {
            sendMessage(ChatColor.GREEN + "" + ChatColor.UNDERLINE + "" + ChatColor.BOLD + "Finished");
        }
        results.forEach(raceResult -> sendMessage(raceResult.toString()));
        if(variantType() == "manhunt") {
            sendMessage(ChatColor.RED + "" + ChatColor.UNDERLINE + "" + ChatColor.BOLD + "Hunted");
            hunted.forEach(raceResult -> sendMessage(raceResult.toString()));
        }
        sendMessage(ChatColor.GOLD + "" + ChatColor.STRIKETHROUGH + "                                    ");
    }

    /**
     * @param excludedPlayer player in race who's time won't be picked
     * @return time objective score for all players in race, -1 if not found
     */
    public int getCurrentObjective(UUID excludedPlayer, String name) {
        int time = -1;

        ScoreboardManager sm = Bukkit.getScoreboardManager();
        if (sm == null) return time;

        Scoreboard s = sm.getMainScoreboard();
        Objective timeObj = s.getObjective(name);
        if (timeObj == null) return time;


        for (UUID uuid : players) {
            if (uuid.equals(excludedPlayer)) continue;

            //if player is online
            Player player = Bukkit.getPlayer(uuid);
            if (player != null) {
                time = timeObj.getScore(player.getName()).getScore(); //we found our time!
                break;
            }
        }

        return time;
    }

    public int getOnlinePlayerCount() {
        int count = 0;
        for (UUID player : players) {
            if (Bukkit.getPlayer(player) != null) count++;
        }
        return count;
    }

    public String getName() {
        return name;
    }

    /**
     * Syncs a player's time and time_tick values with the rest of the race
     *
     * @param player player which scoreboard values will be updated
     */
    public void syncTime(Player player) {
        UUID uuid = player.getUniqueId();

        int timeToSet = getCurrentObjective(uuid, "time");
        int ticksToSet = getCurrentObjective(uuid, "time_tick");

        // should never happen
        if (timeToSet == -1 || ticksToSet == -1) {
            Bukkit.getLogger().warning("[Race] OnPlayerJoin objective result is -1, something is wrong!");
            return;
        }

        ScoreboardManager sm = Bukkit.getScoreboardManager();
        if (sm != null) {
            Scoreboard s = sm.getMainScoreboard();

            Objective timeObj = s.getObjective("time");
            if (timeObj != null) {
                Score score = timeObj.getScore(player.getName());
                score.setScore(timeToSet);
            }

            Objective tickObj = s.getObjective("time_tick");
            if (tickObj != null) {
                Score score = tickObj.getScore(player.getName());
                score.setScore(ticksToSet);
            }
        }
    }

    public void executeStartFunction(Player player) {
        if (Main.startFunction != null && !Main.startFunction.equals("")) {
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "execute as " + player.getName() + " at @s run function " + Main.startFunction);
        }
    }
}
