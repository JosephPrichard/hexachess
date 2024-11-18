package models;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Player {
    String id;
    @EqualsAndHashCode.Exclude
    String name;

    public Player deepCopy() {
        return new Player(id, name);
    }
}