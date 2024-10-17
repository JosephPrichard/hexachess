package domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PieceMoves {
    Hexagon hex;
    List<Hexagon> moves;

    public PieceMoves deepCopy() {
        return new PieceMoves(hex.deepCopy(), moves.stream().map(Hexagon::deepCopy).toList());
    }
}