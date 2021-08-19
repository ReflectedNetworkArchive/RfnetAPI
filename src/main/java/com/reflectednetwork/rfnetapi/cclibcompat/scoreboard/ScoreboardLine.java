package com.reflectednetwork.rfnetapi.cclibcompat.scoreboard;

import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

@Deprecated
@SuppressWarnings("ALL")
public class ScoreboardLine {
    final String staticHalf;
    String dynamicHalf;
    Team team;

    public ScoreboardLine(Scoreboard scoreboard, String staticHalf, String dynamicHalf) {
        this.staticHalf = staticHalf;

        team = scoreboard.registerNewTeam((staticHalf + Math.random()).substring(0, 14));
        team.setSuffix(dynamicHalf);
        team.addEntry(staticHalf);
    }

    public void setDynamicHalf(String dynamicHalf) {
        this.dynamicHalf = dynamicHalf;
        team.setSuffix(dynamicHalf);
    }

    public void getDynamicHalf(String dynamicHalf) {
        this.dynamicHalf = dynamicHalf;
        team.setSuffix(dynamicHalf);
    }

    public void addTo(Objective objective, int index) {
        objective.getScore(staticHalf).setScore(index);
    }

    public String getStaticHalf() {
        return staticHalf;
    }

    public void dispose() {
        team.unregister();
    }
}
