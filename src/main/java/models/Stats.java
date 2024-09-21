package models;

import lombok.*;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Stats {
    private String id;
    private String username;
    private float elo;
    private int wins;
    private int losses;
}