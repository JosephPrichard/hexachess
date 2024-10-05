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

    private long id;
    private String whiteId;
    private String blackId;
    private String whiteName;
    private String blackName;
    private int result;
    private byte[] data;
    @EqualsAndHashCode.Exclude
    private Timestamp playedOn;
}