package models;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
@AllArgsConstructor
public class StatsView {
    private String id;
    private String place;
    private String username;
    private String country;
    private String elo;
    private int wins;
    private int losses;
    private int total;
    private int winRate;

    public static StatsView fromEntity(StatsEntity entity, String place) {
        var total = entity.getWins() + entity.getLosses();
        return new StatsView(
            entity.getId(),
            place,
            entity.getUsername(),
            entity.getCountry(),
            Integer.toString(Math.round(entity.getElo())),
            entity.getWins(),
            entity.getLosses(),
            total,
            entity.getWins() * 100 / total);
    }

    public static List<StatsView> fromEntityList(List<StatsEntity> entityList) {
        List<StatsView> viewList = new ArrayList<>();
        int place = 1;
        for (var entity : entityList) {
            viewList.add(fromEntity(entity, "#" + place));
            place++;
        }
        return viewList;
    }

    public static List<StatsView> fromSortedEntityList(List<StatsEntity> entityList) {
        return fromSortedEntityList(entityList, 1);
    }

    public static List<StatsView> fromSortedEntityList(List<StatsEntity> entityList, int startPlace) {
        List<StatsView> viewList = new ArrayList<>();
        Float prevElo = null;
        int place = startPlace;
        for (var entity : entityList) {
            var placeStr = "=";
            if (prevElo == null || prevElo != entity.getElo()) {
                placeStr = "#" + place;
                place++;
            }
            viewList.add(fromEntity(entity, placeStr));
            prevElo = entity.getElo();
        }
        return viewList;
    }
}