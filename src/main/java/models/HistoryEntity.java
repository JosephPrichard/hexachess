package models;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.sql.Timestamp;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class HistoryEntity {
    public static final int WHITE_WIN = 0;
    public static final int WHITE_LOSS = 1;
    public static final int BOTH_DRAW = 2;

    long id;
    String whiteId;
    String blackId;
    String whiteName;
    String blackName;
    int result;
    byte[] data;
    float winElo;
    float loseElo;
    @EqualsAndHashCode.Exclude
    Timestamp playedOn;
}