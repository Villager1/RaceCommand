package io.github.hielkemaps.racecommand;

import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.Argument;
import dev.jorel.commandapi.arguments.LiteralArgument;
import dev.jorel.commandapi.arguments.PlayerArgument;
import io.github.hielkemaps.racecommand.race.RaceManager;
import io.github.hielkemaps.racecommand.wrapper.PlayerManager;
import io.github.hielkemaps.racecommand.race.Race;
import io.github.hielkemaps.racecommand.wrapper.PlayerWrapper;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.util.*;

public class Commands {

    public Commands() {

        //CREATE
        LinkedHashMap<String, Argument> arguments = new LinkedHashMap<>();
        arguments.put("create", new LiteralArgument("create"));
        new CommandAPICommand("race").withArguments(arguments).executesPlayer((p, args) -> {

            PlayerWrapper wPlayer = PlayerManager.getPlayer(p);
            if (wPlayer.isInRace()) {
                p.sendMessage(Main.PREFIX + "Can't create race: You are already in one");
            } else {
                RaceManager.addRace(new Race(p.getUniqueId()));
                p.sendMessage(Main.PREFIX + "Created race! Invite players with /race invite <player>");
            }
        }).register();

        //INVITE
        arguments = new LinkedHashMap<>();
        arguments.put("invite", new LiteralArgument("invite"));
        arguments.put("player", new PlayerArgument());
        new CommandAPICommand("race").withArguments(arguments).executesPlayer((p, args) -> {
            Player invited = (Player) args[0];

            if (invited.getUniqueId().equals(p.getUniqueId())) {
                p.sendMessage(Main.PREFIX + "You can't invite yourself dummy!");
                return;
            }

            Race race = RaceManager.getRace(p.getUniqueId());
            if (race == null) {
                RaceManager.addRace(new Race(p.getUniqueId()));
                p.sendMessage(Main.PREFIX + "Created race!");
            } else {
                //Only the owner can invite other players
                if (!race.isOwner(p.getUniqueId())) {
                    p.sendMessage(Main.PREFIX + "Only the owner of the race can invite others");
                    return;
                }
            }

            TextComponent msg = new TextComponent(ChatColor.GRAY + p.getName() + " has wants to race! ");
            TextComponent accept = new TextComponent(ChatColor.GREEN + "[Accept]");
            accept.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/race accept " + p.getName()));
            msg.addExtra(accept);

            Objects.requireNonNull(Bukkit.getPlayer(invited.getUniqueId())).spigot().sendMessage(msg);
            p.sendMessage(Main.PREFIX + "Invited player " + invited.getName());

        }).register();

        //Accept
        arguments = new LinkedHashMap<>();
        arguments.put("accept", new LiteralArgument("accept"));
        arguments.put("player", new PlayerArgument());
        new CommandAPICommand("race").withArguments(arguments).executesPlayer((p, args) -> {
            Player sender = (Player) args[0];

            PlayerWrapper wPlayer = PlayerManager.getPlayer(p);

            if (wPlayer.isInRace()) {
                p.sendMessage(Main.PREFIX + "Can't join race: You are already in a race");
                return;
            }

            Race race = RaceManager.getRace(sender.getUniqueId());
            if (race != null) {
                race.addPlayer(p.getUniqueId());
                p.sendMessage(Main.PREFIX + "You joined the race!");
            } else {
                p.sendMessage(Main.PREFIX + "Invalid invite");
            }
        }).register();

        //DISBAND
        arguments = new LinkedHashMap<>();
        arguments.put("disband", new LiteralArgument("disband"));
        new CommandAPICommand("race").withArguments(arguments).executesPlayer((p, args) -> {
            Race race = RaceManager.getRace(p.getUniqueId());
            if (race != null) {
                if (race.isOwner(p.getUniqueId())) {
                    RaceManager.disbandRace(p.getUniqueId());
                    p.sendMessage(Main.PREFIX + "You have disbanded the race");
                } else {
                    p.sendMessage(Main.PREFIX + "Only the owner can disband the race");
                }
            }
        }).register();

        //LEAVE
        arguments = new LinkedHashMap<>();
        arguments.put("leave", new LiteralArgument("leave"));
        new CommandAPICommand("race").withArguments(arguments).executesPlayer((p, args) -> {
            if (PlayerManager.getPlayer(p).isInRace()) {
                Race race = RaceManager.getRace(p.getUniqueId());

                //Should never happen, inRace wrong
                if(race == null){
                    p.sendMessage(Main.PREFIX + "Can't leave race: you aren't in one");
                    PlayerManager.getPlayer(p).setInRace(false);
                    return;
                }

                if(race.isOwner(p.getUniqueId())){
                    p.sendMessage(Main.PREFIX + "You cannot leave your own race. Use /race disband");
                    return;
                }

                race.removePlayer(p.getUniqueId());
                p.sendMessage(Main.PREFIX + "You have left the race");
            } else {
                p.sendMessage(Main.PREFIX + "Can't leave race: you aren't in one");
            }
        }).register();

        //Info
        arguments = new LinkedHashMap<>();
        arguments.put("info", new LiteralArgument("info"));
        new CommandAPICommand("race").withArguments(arguments).executesPlayer((p, args) -> {

            Race race = RaceManager.getRace(p.getUniqueId());
            if (race != null) {

                List<Player> players = new ArrayList<>();
                List<UUID> uuidList = race.getPlayers();
                uuidList.forEach(uuid -> players.add(Bukkit.getPlayer(uuid)));

                p.sendMessage(ChatColor.RED + ""  + ChatColor.BOLD + Objects.requireNonNull(Bukkit.getPlayer(race.getOwner())).getName() + "'s race");
                p.sendMessage("Players:");

                players.forEach(player -> {
                    StringBuilder str = new StringBuilder();
                    str.append(ChatColor.GRAY).append("-").append(player.getName());

                    if (race.isOwner(player.getUniqueId())) {
                        str.append(ChatColor.GREEN).append(" [Owner]");
                    }
                    p.sendMessage(str.toString());
                });
            } else {
                p.sendMessage(Main.PREFIX + "You're not in a race");
            }
        }).register();
    }
}
