package com.reflectednetwork.rfnetapi.cclibcompat.scoreboard;

@Deprecated
@SuppressWarnings("ALL")
public class ScoreboardAction {
    ActionType actionType;
    String title;
    String tag;
    String text;

    enum ActionType {
        SET_TITLE,
        ADD_LINE,
        ADD_BLANK_LINE,
        SET_DYNAMIC
    }

    public ScoreboardAction(ActionType actionType, String tag, String text) {
        this.actionType = actionType;
        this.tag = tag;
        this.text = text;
    }

    public ScoreboardAction(String title) {
        this.actionType = ActionType.SET_TITLE;
        this.title = title;
    }

    public ScoreboardAction() {
        actionType = ActionType.ADD_BLANK_LINE;
    }

    void applyTo(PlayerScoreboard scoreboard) {
        switch (actionType) {
            case SET_DYNAMIC:
                scoreboard.setDynamic(tag, text);
                break;
            case ADD_BLANK_LINE:
                scoreboard.addBlankLine();
                break;
            case ADD_LINE:
                scoreboard.addLine(tag, text);
                break;
            case SET_TITLE:
                scoreboard.setTitle(title);
                break;
        }
    }
}
