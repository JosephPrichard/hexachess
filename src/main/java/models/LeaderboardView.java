package models;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.checkerframework.checker.units.qual.A;

import java.util.ArrayList;
import java.util.List;

@Data
@AllArgsConstructor
public class LeaderboardView {
    List<StatsView> statsList;
}