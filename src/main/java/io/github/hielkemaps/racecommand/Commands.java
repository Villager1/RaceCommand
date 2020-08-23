package io.github.hielkemaps.racecommand;

import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.Argument;
import dev.jorel.commandapi.arguments.LiteralArgument;
import dev.jorel.commandapi.arguments.PlayerArgument;
import io.github.hielkemaps.racecommand.race.Race;
import io.github.hielkemaps.racecommand.race.RaceManager;
import io.github.hielkemaps.racecommand.wrapper.PlayerManager;
import io.github.hielkemaps.racecommand.wrapper.PlayerWrapper;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.function.Predicate;

public class Commands {

    public Commands() {

        //START
        LinkedHashMap<String, Argument> arguments = new LinkedHashMap<>();
        arguments.put("start", new LiteralArgument("start").withRequirement(playerInRace.and(playerIsRaceOwner)));
        new CommandAPICommand("race")
                .withArguments(arguments)
                .executesPlayer((p, args) -> {

                    Race race = RaceManager.getRace(p.getUniqueId());
                    Objects.requireNonNull(race).start();
                    p.sendMessage(Main.PREFIX + "Starting race...");
                }).register();

        //STOP
        arguments = new LinkedHashMap<>();
        arguments.put("stop", new LiteralArgument("stop").withRequirement(playerInRace.and(playerIsRaceOwner)));
        new CommandAPICommand("race")
                .withArguments(arguments)
                .executesPlayer((p, args) -> {

                    Race race = RaceManager.getRace(p.getUniqueId());
                    Objects.requireNonNull(race).stop();
                    p.sendMessage(Main.PREFIX + "Stopped race");
                }).register();

        //CREATE
        arguments = new LinkedHashMap<>();
        arguments.put("create", new LiteralArgument("create").withRequirement(playerInRace.negate()));
        new CommandAPICommand("race")
                .withArguments(arguments)
                .executesPlayer((p, args) -> {
                    RaceManager.addRace(new Race(p.getUniqueId()));
                    p.sendMessage(Main.PREFIX + "Created race! Invite players with /race invite");
                }).register();

        //INVITE
        arguments = new LinkedHashMap<>();
        arguments.put("invite", new LiteralArgument("invite").withRequirement(playerInRace.and(playerIsRaceOwner)));
        arguments.put("player", new PlayerArgument().overrideSuggestions(sender -> {
            Collection<? extends Player> players = Bukkit.getOnlinePlayers();
            List<String> names = new ArrayList<>();

            Race race = RaceManager.getRace(((Player) sender).getUniqueId());
            if (race == null) return new String[0];

            //Don't show players that are already in your race
            for (Player p : players) {
                if (race.hasPlayer(p.getUniqueId())) continue;
                names.add(p.getName());
            }
            return names.toArray(new String[0]);
        }));
        new CommandAPICommand("race")
                .withArguments(arguments)
                .executesPlayer((p, args) -> {
                    Player invited = (Player) args[0];

                    //If invite yourself
                    if (invited.getUniqueId().equals(p.getUniqueId())) {
                        p.sendMessage(Main.PREFIX + "You can't invite yourself");
                        return;
                    }

                    //If player is already in race
                    if (Objects.requireNonNull(RaceManager.getRace(p.getUniqueId())).hasPlayer(invited.getUniqueId())) {
                        p.sendMessage(Main.PREFIX + "That player is already in your race");
                        return;
                    }

                    //If already invited
                    if (PlayerManager.getPlayer(p.getUniqueId()).getInvitedPlayers().contains(invited.getUniqueId())) {
                        p.sendMessage(Main.PREFIX + "You have already invited that player");
                        return;
                    }

                    PlayerManager.getPlayer(p.getUniqueId()).invitePlayerToRun(invited.getUniqueId());
                    TextComponent msg = new TextComponent(Main.PREFIX + p.getName() + " wants to race! ");
                    TextComponent accept = new TextComponent(ChatColor.GREEN + "[Accept]");
                    accept.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/race join " + p.getName()));
                    msg.addExtra(accept);

                    Objects.requireNonNull(Bukkit.getPlayer(invited.getUniqueId())).spigot().sendMessage(msg);
                    p.sendMessage(Main.PREFIX + "Invited player " + invited.getName());

                }).register();

        //Join
        arguments = new LinkedHashMap<>();
        arguments.put("join", new LiteralArgument("join").withRequirement(playerHasJoinableRaces));
        arguments.put("player", new PlayerArgument().overrideSuggestions((sender) -> PlayerManager.getPlayer(((Player) sender).getUniqueId()).getJoinableRaces()));
        new CommandAPICommand("race").withArguments(arguments).executesPlayer((p, args) -> {
            Player sender = (Player) args[0];

            PlayerWrapper wPlayer = PlayerManager.getPlayer(p.getUniqueId());
            if (wPlayer.isInRace()) {

                //If already in race
                if (Objects.requireNonNull(RaceManager.getRace(sender.getUniqueId())).getPlayers().contains(p.getUniqueId())) {
                    p.sendMessage(Main.PREFIX + "You already joined this race");
                    return;
                }

                p.sendMessage(Main.PREFIX + "Can't join race: You first have to leave your race");
                return;
            }

            Race race = RaceManager.getRace(sender.getUniqueId());
            if (race == null) return;

            PlayerWrapper raceOwner = PlayerManager.getPlayer(sender.getUniqueId());
            if (race.isPublic() || raceOwner.hasInvited(p)) {

                if (wPlayer.acceptInvite(sender.getUniqueId())) {
                    p.sendMessage(Main.PREFIX + "You joined the race!");
                }

            }
        }).register();

        //DISBAND
        arguments = new LinkedHashMap<>();
        arguments.put("disband", new LiteralArgument("disband").withRequirement(playerInRace.and(playerIsRaceOwner)));
        new CommandAPICommand("race")
                .withArguments(arguments)
                .executesPlayer((p, args) -> {
                    RaceManager.disbandRace(p.getUniqueId());
                    p.sendMessage(Main.PREFIX + "You have disbanded the race");
                }).register();

        //LEAVE
        arguments = new LinkedHashMap<>();
        arguments.put("leave", new LiteralArgument("leave").withRequirement(playerInRace.and(playerIsRaceOwner.negate())));
        new CommandAPICommand("race")
                .withArguments(arguments)
                .executesPlayer((p, args) -> {
                    Objects.requireNonNull(RaceManager.getRace(p.getUniqueId())).leavePlayer(p.getUniqueId());
                    p.sendMessage(Main.PREFIX + "You have left the race");
                }).register();

        //INFO
        arguments = new LinkedHashMap<>();
        arguments.put("info", new LiteralArgument("info").withRequirement(playerInRace));
        new CommandAPICommand("race")
                .withArguments(arguments)
                .executesPlayer((p, args) -> {

                    Race race = RaceManager.getRace(p.getUniqueId());
                    if (race == null) return;

                    List<Player> players = new ArrayList<>();
                    List<UUID> uuidList = race.getPlayers();
                    uuidList.forEach(uuid -> players.add(Bukkit.getPlayer(uuid)));

                    p.sendMessage(ChatColor.RED + "" + ChatColor.BOLD + Objects.requireNonNull(Bukkit.getPlayer(race.getOwner())).getName() + "'s race");
                    p.sendMessage("Visibility: " + (race.isPublic() ? "public" : "private"));
                    p.sendMessage("Players:");

                    players.forEach(player -> {
                        StringBuilder str = new StringBuilder();
                        str.append(ChatColor.GRAY).append("-").append(player.getName());

                        if (race.isOwner(player.getUniqueId())) {
                            str.append(ChatColor.GREEN).append(" [Owner]");
                        }
                        p.sendMessage(str.toString());
                    });
                }).register();

        //KICK
        arguments = new LinkedHashMap<>();
        arguments.put("kick", new LiteralArgument("kick").withRequirement(playerInRace.and(playerIsRaceOwner).and(playerToKick)));
        arguments.put("player", new PlayerArgument().overrideSuggestions((sender) ->
        {
            List<String> names = new ArrayList<>();
            Race race = RaceManager.getRace(((Player) sender).getUniqueId());
            if (race == null) return new String[0];

            List<UUID> players = race.getPlayers();
            for (UUID uuid : players) {
                if (uuid.equals(((Player) sender).getUniqueId())) continue;
                Player p = Bukkit.getPlayer(uuid);
                if (p == null) continue;
                names.add(p.getName());
            }

            return names.toArray(new String[0]);
        }));
        new CommandAPICommand("race")
                .withArguments(arguments)
                .executesPlayer((p, args) -> {
                    Player toKick = (Player) args[0];

                    if (p.getUniqueId().equals(toKick.getUniqueId())) {
                        p.sendMessage(Main.PREFIX + "You can't kick yourself");
                        return;
                    }
                    Objects.requireNonNull(RaceManager.getRace(p.getUniqueId())).kickPlayer(toKick.getUniqueId());
                }).register();

        //Option visibility
        arguments = new LinkedHashMap<>();
        arguments.put("option", new LiteralArgument("option").withRequirement(playerInRace.and(playerIsRaceOwner)));
        arguments.put("visibility", new LiteralArgument("visibility"));
        String[] visibility = {"public", "private"};
        for (String s : visibility) {
            arguments.put("value", new LiteralArgument(s));
            new CommandAPICommand("race")
                    .withArguments(arguments)
                    .executesPlayer((p, args) -> {
                        if (Objects.requireNonNull(RaceManager.getRace(p.getUniqueId())).setIsPublic(s.equals("public"))) {
                            p.sendMessage(Main.PREFIX + "Set race visibility to " + s);
                        } else {
                            p.sendMessage(Main.PREFIX + ChatColor.RED + "Nothing changed. Race visibility was already " + s);
                        }
                    }).register();
        }
    }

    Predicate<CommandSender> playerInRace = sender -> PlayerManager.getPlayer(((Player) sender).getUniqueId()).isInRace();

    Predicate<CommandSender> playerHasJoinableRaces = sender -> PlayerManager.getPlayer(((Player) sender).getUniqueId()).hasJoinableRace();

    Predicate<CommandSender> playerIsRaceOwner = sender -> {
        Player player = (Player) sender;
        if (player == null) return false;

        Race race = RaceManager.getRace(player.getUniqueId());
        if (race == null) return false;

        return race.isOwner(player.getUniqueId());
    };

    Predicate<CommandSender> playerToKick = sender -> {
        Player player = (Player) sender;
        if (player == null) return false;

        Race race = RaceManager.getRace(player.getUniqueId());
        if (race == null) return false;

        return race.getPlayers().size() > 1;
    };
}
