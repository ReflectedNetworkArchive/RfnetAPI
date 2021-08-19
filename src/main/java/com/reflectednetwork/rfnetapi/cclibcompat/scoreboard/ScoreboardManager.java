package com.reflectednetwork.rfnetapi.cclibcompat.scoreboard;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerKickEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import top.cavecraft.cclib.ICCLib.IPlayerScoreboard;
import top.cavecraft.cclib.ICCLib.IScoreboardManager;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@SuppressWarnings("ALL")
@Deprecated
public class ScoreboardManager implements IScoreboardManager, Listener {
    Map<UUID, PlayerScoreboard> playerScoreboards = new HashMap<>();
    ChildScoreboard base = new ChildScoreboard();

    @EventHandler
    public void playerJoin(PlayerJoinEvent event) {
        try {
            PlayerScoreboard playerScoreboard = new PlayerScoreboard(event.getPlayer());
//             event.getPlayer().setScoreboard(playerScoreboard.scoreboard);
            playerScoreboard.addChild(base);
            playerScoreboards.put(event.getPlayer().getUniqueId(), playerScoreboard);
        } catch (Exception e) {
            e.printStackTrace();System.out.println(e.getMessage());
        }
    }

    @EventHandler
    public void playerLeave(PlayerQuitEvent event) {
        try {
            base.remove(playerScoreboards.get(event.getPlayer().getUniqueId()));
            playerScoreboards.remove(event.getPlayer().getUniqueId());
        } catch (Exception e) {
            e.printStackTrace();System.out.println(e.getMessage());
        }
    }

    @EventHandler
    public void playerKick(PlayerKickEvent event) {
        try {
            base.remove(playerScoreboards.get(event.getPlayer().getUniqueId()));
            playerScoreboards.remove(event.getPlayer().getUniqueId());
        } catch (Exception e) {
            e.printStackTrace();System.out.println(e.getMessage());
        }
    }

    public void addLineToAll(String tag, String text) {
        try {
            base.addLine(tag, text);
        } catch (Exception e) {
            e.printStackTrace();System.out.println(e.getMessage());
        }
    }

    public void addBlankLineToAll() {
        try {
            base.addBlankLine();
        } catch (Exception e) {
            e.printStackTrace();System.out.println(e.getMessage());
        }
    }

    @Override
    public void clearAll() {
        try {
            for (PlayerScoreboard scoreboard : playerScoreboards.values()) {
                scoreboard.clear();
                base.remove(scoreboard);
            }
            base = null;
            base = new ChildScoreboard();
            for (PlayerScoreboard scoreboard : playerScoreboards.values()) {
                base.applyTo(scoreboard);
            }
        } catch (Exception e) {
            e.printStackTrace();System.out.println(e.getMessage());
        }
    }

    public void setTitleOfAll(String title) {
        try {
            base.setTitle(title);
        } catch (Exception e) {
            e.printStackTrace();System.out.println(e.getMessage());
        }
    }

    public void setDynamicOfAll(String tag, String text) {
        try {
            base.setDynamic(tag, text);
        } catch (Exception e) {
            e.printStackTrace();System.out.println(e.getMessage());
        }
    }

    public IPlayerScoreboard getScoreboardFor(Player player) {
        try {
            return playerScoreboards.get(player.getUniqueId());
        } catch (Exception e) {
            e.printStackTrace();System.out.println(e.getMessage());
            return null;
        }
    }

    public void enableCompat() {
        for (UUID player : playerScoreboards.keySet()) {
            Bukkit.getPlayer(player).setScoreboard(playerScoreboards.get(player).scoreboard);
        }
    }
}
