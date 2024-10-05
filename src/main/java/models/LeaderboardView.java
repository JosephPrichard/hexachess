package models;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.checkerframework.checker.units.qual.A;

import java.util.ArrayList;
import java.util.List;

@Data
@AllArgsConstructor
public class LeaderboardView {
    private List<StatsView> statsList;

    @Data
    @AllArgsConstructor
    public static class StatsView {
        private String id;
        private String place;
        private String username;
        private String country;
        private float elo;
        private int wins;
        private int losses;
        private int total;

        public static StatsView fromEntity(StatsEntity entity, String place) {
            return new LeaderboardView.StatsView(entity.getId(),
                place, entity.getUsername(), entity.getCountry(), entity.getElo(),
                entity.getWins(), entity.getLosses(), entity.getWins() + entity.getLosses());
        }

        public static List<StatsView> fromEntityList(List<StatsEntity> entityList) {
            return fromEntityList(entityList, 1);
        }

        public static List<StatsView> fromEntityList(List<StatsEntity> entityList, int startPlace) {
            List<StatsView> viewList = new ArrayList<>();
            Float prevElo = null;
            int place = startPlace;
            for (var entity : entityList) {
                var placeStr = "=";
                if (prevElo == null || prevElo != entity.getElo()) {
                    placeStr = Integer.toString(place);
                    place++;
                }
                viewList.add(fromEntity(entity, placeStr));
                prevElo = entity.getElo();
            }
            return viewList;
        }
    }
}