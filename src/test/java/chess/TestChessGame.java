package chess;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static chess.ChessBoard.*;
import static chess.Hexagon.*;

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
    public void testGetSetPieces() {
        var board = ChessBoard.initial();

        board.setPiece("f3", WHITE_BISHOP);
        var piece = board.getPiece("f3");

        Assertions.assertEquals(WHITE_BISHOP, piece);
    }

    // assert only checking REGULAR piece moves
    private void assertPieceMoves(List<Hexagon> actualMoves, String... expMoves) {
        var expectedMoves = Stream.of(expMoves)
            .map(Hexagon::fromNotation)
            .toList();
        Assertions.assertEquals(expectedMoves, actualMoves);
    }

    @Test
    public void testFindRookMoves() {
        // given
        var game = ChessGame.start();

        // when
        var centerMoves = game.findRookMoves(Hexagon.fromNotation("f6")).getMoves();
        var leftMoves = game.findRookMoves(Hexagon.fromNotation("c8")).getMoves();
        var rightMoves = game.findRookMoves(Hexagon.fromNotation("h4")).getMoves();

        System.out.println(game.getBoard().toMovesString(centerMoves));
        System.out.println(game.getBoard().toMovesString(leftMoves));
        System.out.println(game.getBoard().toMovesString(rightMoves));

        // then
        assertPieceMoves(centerMoves,
            "f5", "e5", "d4", "c3", "b2", "a1", "g5", "h4", "i3", "j2", "k1",
            "e6", "d6", "c6", "b6", "a6", "g6", "h6", "i6", "j6", "k6");

        assertPieceMoves(leftMoves, "d8", "e8", "f8");

        assertPieceMoves(rightMoves,
            "h5", "h6", "h3", "g4", "i3", "j2", "k1", "g5", "f6", "e6",
            "d6", "c6", "b6", "a6", "i4", "j4", "k4");
    }

    @Test
    public void testFindBishopMoves() {
        // given
        var game = ChessGame.start();

        // when
        var centerMoves = game.findBishopMoves(Hexagon.fromNotation("f6")).getMoves();
        var leftMoves = game.findBishopMoves(Hexagon.fromNotation("c8")).getMoves();
        var rightMoves = game.findBishopMoves(Hexagon.fromNotation("h4")).getMoves();

        System.out.println(game.getBoard().toMovesString(centerMoves));
        System.out.println(game.getBoard().toMovesString(leftMoves));
        System.out.println(game.getBoard().toMovesString(rightMoves));

        // then
        assertPieceMoves(centerMoves,
            "h5", "j4", "d5", "b4", "g4", "e4");

        assertPieceMoves(leftMoves,
            "e9", "g9", "b6", "a4");

        assertPieceMoves(rightMoves,
            "j3", "f5", "i5", "j6", "g6", "f8", "e9", "i2", "g3", "f2");
    }

    @Test
    public void testFindKingMoves() {
        // given
        var game = ChessGame.empty();

        // when
        var centerMoves = game.findKingMoves(Hexagon.fromNotation("f6"));
        var leftMoves = game.findKingMoves(Hexagon.fromNotation("d3"));
        var rightMoves = game.findKingMoves(Hexagon.fromNotation("h7"));

        System.out.println(game.getBoard().toMovesString(centerMoves));
        System.out.println(game.getBoard().toMovesString(leftMoves));
        System.out.println(game.getBoard().toMovesString(rightMoves));

        // then
        assertPieceMoves(centerMoves,
            "f7", "f5", "e5", "g5", "e6", "g6", "h5", "d5", "g7", "e7", "g4", "e4");

        assertPieceMoves(leftMoves,
            "d4", "d2", "c2", "e3", "c3", "e4", "f4", "b2", "e5", "c4", "e2", "c1");

        assertPieceMoves(rightMoves,
            "h8", "h6", "g7", "i6", "g8", "i7", "j6", "f8", "i8", "g9", "i5", "g6");
    }

    @Test
    public void testFindKnightMoves() {
        // given
        var game = ChessGame.empty();

        // when
        var centerMoves = game.findKnightMoves(Hexagon.fromNotation("f6")).getMoves();
        var leftMoves = game.findKnightMoves(Hexagon.fromNotation("d3")).getMoves();
        var rightMoves = game.findKnightMoves(Hexagon.fromNotation("h7")).getMoves();

        System.out.println(game.getBoard().toMovesString(centerMoves));
        System.out.println(game.getBoard().toMovesString(leftMoves));
        System.out.println(game.getBoard().toMovesString(rightMoves));

        // then
        assertPieceMoves(centerMoves,
            "h7", "g8", "h3", "g3", "d7", "e8", "d3", "e3", "c5", "c4", "i5", "i4");

        assertPieceMoves(leftMoves,
            "f6", "e6", "f2", "e1", "b4", "c5", "a2", "a1", "g4", "g3");

        assertPieceMoves(rightMoves,
            "j4", "i4", "f10", "g10", "f6", "g5", "e8", "e7", "k6", "k5");
    }

    @Test
    public void testFindPawnMoves() {
        // given
        var game = ChessGame.empty();
        game.getBoard().setPiece("c4", BLACK_PAWN);
        game.getBoard().setPiece("e5", WHITE_PAWN);

        // when
        var firstMoves = game.findPawnMoves(Hexagon.fromNotation("g4")).getMoves();
        var takeMoves = game.findPawnMoves(Hexagon.fromNotation("d4")).getMoves();

        System.out.println(game.getBoard().toMovesString(firstMoves));
        System.out.println(game.getBoard().toMovesString(takeMoves));

        // then
        assertPieceMoves(firstMoves, "g5");

        assertPieceMoves(takeMoves, "d5", "e5");
    }

    @Test
    public void testIsCheckmate() {
        var game = ChessGame.empty();
        var board = game.getBoard();
        board.setPiece("f6", WHITE_KING);
        board.setPiece("f4", BLACK_QUEEN);
        board.setPiece("f8", BLACK_QUEEN);
        board.setPiece("b4", BLACK_BISHOP);
        board.setPiece("j4", BLACK_BISHOP);
        board.setPiece("f9", BLACK_KING);

        game.initPieceMoves();

        System.out.println(game.getBoard());
        System.out.println(game.getBoard().toPieceMovesString(game.getOppositeMoves()));

        var isCheckmate = game.isCheckmate();
        Assertions.assertTrue(isCheckmate);
    }

    @Test
    public void testInitPieceMoves() {
        // given
        var game = ChessGame.start();

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
