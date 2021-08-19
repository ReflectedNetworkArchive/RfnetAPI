package com.reflectednetwork.rfnetapi.cclibcompat.scoreboard;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;
import top.cavecraft.cclib.ICCLib;
import top.cavecraft.cclib.ICCLib.IPlayerScoreboard;

import java.util.HashMap;
import java.util.Map;

@Deprecated
@SuppressWarnings("ALL")
public class PlayerScoreboard implements IPlayerScoreboard {
    Scoreboard scoreboard;
    Objective objective;
    Map<String, ScoreboardLine> display = new HashMap<>();
    int index = 99; //This is bad. Don't do this. Figure out how to get the total but still have it be dynamic without an update function.
    int whitespaceIndex = 0;

    public PlayerScoreboard(Player player) {
        try {
            scoreboard = Bukkit.getScoreboardManager().getNewScoreboard();
            player.setScoreboard(scoreboard);
            objective = scoreboard.registerNewObjective("objective", "dummy");
            objective.setDisplaySlot(DisplaySlot.SIDEBAR);
        } catch (Exception e) {
            e.printStackTrace();System.out.println(e.getMessage());
        }
    }

    public void setTitle(String title) {
        try {
            objective.setDisplayName(title);
        } catch (Exception e) {
            e.printStackTrace();System.out.println(e.getMessage());
        }
    }

    public void addLine(String tag, String text) {
        try {
            ScoreboardLine line = new ScoreboardLine(scoreboard, text, "");
            line.addTo(objective, index);
            display.put(tag, line);
            index--;
        } catch (Exception e) {
            e.printStackTrace();System.out.println(e.getMessage());
        }
    }

    public void addBlankLine() {
        try {
            StringBuilder whitespace = new StringBuilder();
            for (int i = 0; i < whitespaceIndex; i++) {
                whitespace.append(" ");
            }
            ScoreboardLine line = new ScoreboardLine(scoreboard, whitespace.toString(), "");
            line.addTo(objective, index);
            display.put(whitespace.toString() + Math.random(), line);
            index--;
            whitespaceIndex++;
        } catch (Exception e) {
            e.printStackTrace();System.out.println(e.getMessage());
        }
    }

    public void setDynamic(String tag, String text) {
        try {
            if (display.containsKey(tag)) {
                display.get(tag).setDynamicHalf(text);
            }
        } catch (Exception e) {
            e.printStackTrace();System.out.println(e.getMessage());
        }
    }

    public void clear() {
        try {
            for (ScoreboardLine line : display.values()) {
                line.dispose();
            }
            for (String entry : scoreboard.getEntries()) {
                scoreboard.resetScores(entry);
            }
            display.clear();
            whitespaceIndex = 0;
            index = 99;
        } catch (Exception e) {
            e.printStackTrace();System.out.println(e.getMessage());
        }
    }

    @Override
    public Scoreboard getHandle() {
        return scoreboard;
    }

    @Override
    public ICCLib.IChildScoreboard createChild() {
        ChildScoreboard child = new ChildScoreboard();
        child.applyTo(this);
        return child;
    }

    @Override
    public void addChild(ICCLib.IChildScoreboard child) {
        ((ChildScoreboard)child).applyTo(this);
    }
}
