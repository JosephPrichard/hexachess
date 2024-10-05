package models;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class StatsEntity {
    private String id;
    private String username;
    private String country;
    private float elo;
    private int wins;
    private int losses;
}