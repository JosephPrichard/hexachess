package models;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.sql.Timestamp;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class History {
    public static final int WIN = 0;
    public static final int LOSS = 1;
    public static final int DRAW = 2;

    private long id;
    private String whiteId;
    private String blackId;
    private String whiteName;
    private String blackName;
    private int result;
    private String data;
    @EqualsAndHashCode.Exclude
    private Timestamp playedOn;
}