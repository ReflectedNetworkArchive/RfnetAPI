package com.reflectednetwork.rfnetapi.cclibcompat.scoreboard;

import top.cavecraft.cclib.ICCLib;

import java.util.ArrayList;
import java.util.List;

@Deprecated
@SuppressWarnings("ALL")
public class ChildScoreboard implements ICCLib.IChildScoreboard {
    private final List<PlayerScoreboard> parents;
    private final List<ScoreboardAction> actions;

    public ChildScoreboard() {
        parents = new ArrayList<>();
        actions = new ArrayList<>();
    }

    @Override
    public void setTitle(String title) {
        for (PlayerScoreboard scoreboard : parents) {
            scoreboard.setTitle(title);
        }

        actions.add(new ScoreboardAction(title));
    }

    @Override
    public void addLine(String tag, String text) {
        for (PlayerScoreboard scoreboard : parents) {
            scoreboard.addLine(tag, text);
        }

        actions.add(new ScoreboardAction(ScoreboardAction.ActionType.ADD_LINE, tag, text));
    }

    @Override
    public void addBlankLine() {
        for (PlayerScoreboard scoreboard : parents) {
            scoreboard.addBlankLine();
        }

        actions.add(new ScoreboardAction());
    }

    @Override
    public void setDynamic(String tag, String text) {
        ScoreboardAction actionToRemove = null;
        for (ScoreboardAction action : actions) {
            if (action.actionType == ScoreboardAction.ActionType.SET_DYNAMIC && tag.equals(action.tag)) {
                actionToRemove = action;
            }
        }
        if (actionToRemove != null) {
            actions.remove(actionToRemove);
        }

        for (PlayerScoreboard scoreboard : parents) {
            scoreboard.setDynamic(tag, text);
        }

        actions.add(new ScoreboardAction(ScoreboardAction.ActionType.SET_DYNAMIC, tag, text));
    }

    public void applyTo(PlayerScoreboard scoreboard) {
        for (ScoreboardAction action : actions) {
            action.applyTo(scoreboard);
        }
        parents.add(scoreboard);
    }

    public void remove(PlayerScoreboard scoreboard) {
        parents.remove(scoreboard);
    }
}
