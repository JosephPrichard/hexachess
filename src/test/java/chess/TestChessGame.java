package chess;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static chess.ChessBoard.WHITE_BISHOP;

public class TestChessGame {

    @Test
    public void testHexagonToString() {
        String str;

        str = new Hexagon(5, 8).toString();
        Assertions.assertEquals(str, "f9");

        str = new Hexagon(6, 9).toString();
        Assertions.assertEquals(str, "g10");

        str = new Hexagon(4, 4).toString();
        Assertions.assertEquals(str, "e5");

        str = new Hexagon(6, 0).toString();
        Assertions.assertEquals(str, "g1");
    }

    @Test
    public void testHexagonFromString() {
        Hexagon hex;

        hex = Hexagon.fromNotation("f9");
        Assertions.assertEquals(hex, new Hexagon(5, 8));

        hex = Hexagon.fromNotation("g10");
        Assertions.assertEquals(hex, new Hexagon(6, 9));

        hex = Hexagon.fromNotation("e5");
        Assertions.assertEquals(hex, new Hexagon(4, 4));
    }

    @Test
    public void testHexagonIndices() {
        var hexagon = new Hexagon(5, 8);
        int index = hexagon.toIndex();

        Assertions.assertEquals(hexagon, Hexagon.fromIndex(index));
    }

    @Test
    public void testGetSetPieces() {
        var board = ChessBoard.initial();

        board.setPiece("f3", WHITE_BISHOP);
        var piece = board.getPiece("f3");

        Assertions.assertEquals(WHITE_BISHOP, piece);
    }

    @Test
    public void testFindMovesByTraveling() {
        // given
        var game = ChessGame.start();
        System.out.println(game.board());

        // when
//        var moves1 = game.findMovesByTraveling(Hexagon.fromNotation("f6"), ChessGame.ROOK_OFFSETS);
        var moves2 = game.findMovesByTraveling(Hexagon.fromNotation("c8"), ChessGame.ROOK_OFFSETS);

        // then
//        var stream1 = Stream.of("f5", "e6", "d6", "c6", "b6", "a6", "g6",
//            "h6", "i6", "j6", "k6", "e5", "d4", "c3", "b2", "a1", "g5", "h4", "i3", "j2", "k1");
//        List<Hexagon> expectedMoves1 = stream1.map(Hexagon::fromNotation).toList();
//        Assertions.assertEquals(expectedMoves1, moves1);

        var stream2 = Stream.of("f5");
        List<Hexagon> expectedMoves2 = stream2.map(Hexagon::fromNotation).toList();
        Assertions.assertEquals(expectedMoves2, moves2);
    }

    @Test
    public void testInitPieceMoves() {
        // given
        var game = ChessGame.start();
        System.out.println(game.board());

        // when
        game.initPieceMoves();
        var currMoves = game.getCurrMoves();
        var oppMoves = game.getOppositeMoves();

        // then
        List<PieceMoves> expectedCurrMoves = new ArrayList<>();
        List<PieceMoves> expectedOppMoves = new ArrayList<>();
        Assertions.assertEquals(expectedCurrMoves, currMoves);
        Assertions.assertEquals(expectedOppMoves, oppMoves);
    }
}
