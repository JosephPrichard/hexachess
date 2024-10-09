package models;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
@AllArgsConstructor
public class StatsView {
    String id;
    int rank;
    String username;
    String country;
    String elo;
    int wins;
    int losses;
    int total;
    int winRate;

    public static StatsView fromEntity(StatsEntity entity) {
        var total = entity.getWins() + entity.getLosses();
        return new StatsView(
            entity.getId(),
            entity.getRank(),
            entity.getUsername(),
            entity.getCountry(),
            Integer.toString(Math.round(entity.getElo())),
            entity.getWins(),
            entity.getLosses(),
            total,
            total == 0 ? 0 : entity.getWins() * 100 / total);
    }

    public static List<StatsView> fromEntityList(List<StatsEntity> entityList) {
        return entityList.stream().map(StatsView::fromEntity).toList();
    }
}