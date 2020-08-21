package io.github.hielkemaps.racecommand;

import io.github.hielkemaps.racecommand.events.EventListener;
import org.bukkit.ChatColor;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

@SuppressWarnings("unused")
public class Main extends JavaPlugin {

    private static Plugin instance;

    public Main() {
        instance = this;
    }

    public static Plugin getInstance() {
        return instance;
    }

    public static String PREFIX = ChatColor.YELLOW + "" + ChatColor.BOLD + "[RACE] " + ChatColor.RESET + "" + ChatColor.GRAY;

    @Override
    public void onEnable() {

        //Register commands
        new Commands();

        //Register EventListener
        getServer().getPluginManager().registerEvents(new EventListener(),this);
    }
}