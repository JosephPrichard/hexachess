package model;

import models.LeaderboardView;
import models.StatsEntity;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;

public class TestLeaderboardView {

    @Test
    public void testFromEntityList() {
        List<StatsEntity> entityList = List.of(
            new StatsEntity("1", "player1", "us", 2400.0f, 150, 30),
            new StatsEntity("2", "player2", "uk", 2200.0f, 140, 40),
            new StatsEntity("3", "player3", "us", 2200.0f, 130, 50),
            new StatsEntity("4", "player4", "uk", 2100.0f, 120, 60));

        var actualViewList = LeaderboardView.StatsView.fromEntityList(entityList);
        var expectedViewList = List.of(
            new LeaderboardView.StatsView("1", "1", "player1", "us", 2400.0f, 150, 30, 180),
            new LeaderboardView.StatsView("2", "2", "player2", "uk", 2200.0f, 140, 40, 180),
            new LeaderboardView.StatsView("3", "=", "player3", "us", 2200.0f, 130, 50, 180),
            new LeaderboardView.StatsView("4", "3", "player4", "uk", 2100.0f, 120, 60, 180));

        Assertions.assertEquals(expectedViewList, actualViewList);
    }
}
