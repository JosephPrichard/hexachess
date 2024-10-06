package model;

import models.StatsEntity;
import models.StatsView;
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

        var actualViewList = StatsView.fromSortedEntityList(entityList);
        var expectedViewList = List.of(
            new StatsView("1", "#1", "player1", "us", "2400", 150, 30, 180, 83),
            new StatsView("2", "#2", "player2", "uk", "2200", 140, 40, 180, 77),
            new StatsView("3", "=", "player3", "us", "2200", 130, 50, 180, 72),
            new StatsView("4", "#3", "player4", "uk", "2100", 120, 60, 180, 66));

        Assertions.assertEquals(expectedViewList, actualViewList);
    }
}
