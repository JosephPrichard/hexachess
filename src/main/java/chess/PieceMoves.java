package chess;

import java.util.List;

public record PieceMoves(Hexagon piecePos, List<Hexagon> moves) {
}
