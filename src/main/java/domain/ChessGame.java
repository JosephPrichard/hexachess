package domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Stream;

import static domain.ChessBoard.*;
import static domain.Hexagon.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChessGame {

    private final static Direction[][] ROOK_OFFSETS = {
        {Direction.UP},
        {Direction.DOWN},
        {Direction.DOWN_LEFT},
        {Direction.DOWN_RIGHT},
        {Direction.UP_LEFT},
        {Direction.UP_RIGHT}
    };
    private final static Direction[][] BISHOP_OFFSETS = {
        {Direction.UP_RIGHT, Direction.DOWN_RIGHT},
        {Direction.UP_LEFT, Direction.DOWN_LEFT},
        {Direction.UP, Direction.UP_RIGHT},
        {Direction.UP, Direction.UP_LEFT},
        {Direction.DOWN, Direction.DOWN_RIGHT},
        {Direction.DOWN, Direction.DOWN_LEFT}
    };
    private final static Direction[][] KING_OFFSETS =
        Stream.concat(Arrays.stream(ROOK_OFFSETS), Arrays.stream(BISHOP_OFFSETS)).toArray(Direction[][]::new);
    private final static Direction[][] KNIGHT_OFFSETS = {
        {Direction.UP_RIGHT, Direction.UP_RIGHT, Direction.UP},
        {Direction.UP_RIGHT, Direction.UP, Direction.UP},
        {Direction.DOWN_RIGHT, Direction.DOWN_RIGHT, Direction.DOWN},
        {Direction.DOWN_RIGHT, Direction.DOWN, Direction.DOWN},
        {Direction.UP_LEFT, Direction.UP_LEFT, Direction.UP},
        {Direction.UP_LEFT, Direction.UP, Direction.UP},
        {Direction.DOWN_LEFT, Direction.DOWN_LEFT, Direction.DOWN},
        {Direction.DOWN_LEFT, Direction.DOWN, Direction.DOWN},
        {Direction.UP_LEFT, Direction.UP_LEFT, Direction.DOWN_LEFT},
        {Direction.DOWN_LEFT, Direction.DOWN_LEFT, Direction.UP_LEFT},
        {Direction.UP_RIGHT, Direction.UP_RIGHT, Direction.DOWN_RIGHT},
        {Direction.DOWN_RIGHT, Direction.DOWN_RIGHT, Direction.UP_RIGHT},
    };

    private ChessBoard board;
    private List<PieceMoves> whiteMoves = null;
    private List<PieceMoves> blackMoves = null;

    public ChessGame deepCopy() {
        return new ChessGame(board != null ? board.copy() : null,
            whiteMoves != null ? whiteMoves.stream().map(PieceMoves::deepCopy).toList() : null,
            blackMoves != null ? blackMoves.stream().map(PieceMoves::deepCopy).toList() : null);
    }

    public static ChessGame start() {
        return new ChessGame(ChessBoard.initial());
    }

    public static ChessGame empty() {
        return new ChessGame(new ChessBoard(Turn.WHITE));
    }

    public ChessGame(ChessBoard board) {
        this.board = board;
    }

    public List<PieceMoves> getPieceMoves(Turn turn) {
        return turn.isWhite() ? whiteMoves : blackMoves;
    }

    public List<PieceMoves> getCurrMoves() {
        return getPieceMoves(board.turn());
    }

    public List<PieceMoves> getOppositeMoves() {
        return getPieceMoves(board.turn().opposite());
    }

    public ChessGame setPiece(String notation, byte piece) {
        board.setPiece(notation, piece);
        return this;
    }

    public boolean isValidMove(Move move) {
        assert move != null;
        assert whiteMoves != null;
        assert blackMoves != null;

        var moves = getCurrMoves();

        // has a match for a move from one hexagon to another hexagon
        return moves.stream()
            .anyMatch((pm) -> {
                var isFrom = pm.getHex().equals(move.getFrom());
                var hasTo = pm.getMoves().stream().anyMatch((m) -> m.equals(move.getTo()));
                return isFrom && hasTo;
            });
    }

    public void makeMove(Move move) {
        var piece = board.getPiece(move.getFrom());
        board.setPiece(move.getFrom(), EMPTY);
        board.setPiece(move.getTo(), piece);
        board.flipTurn();

        whiteMoves = null;
        blackMoves = null;
    }

    public void initPieceMoves() {
        // we find the piece moves for all other pieces besides the king
        whiteMoves = findPieceMoves(Turn.WHITE);
        blackMoves = findPieceMoves(Turn.BLACK);

        var whiteKingHex = board.findKing(Turn.WHITE);
        var blackKingHex = board.findKing(Turn.BLACK);

        // find the moves for both kings - excluding any attacking squares
        var whiteKingMoves = new PieceMoves(whiteKingHex, findKingMoves(whiteKingHex));
        var blackKingMoves = new PieceMoves(blackKingHex, findKingMoves(blackKingHex));
        var kingHex = board.turn().isWhite() ? whiteKingHex : blackKingHex;

        // decide whether we will add the piece moves... are we in check?
        // we don't need to check if the opposite move is in check... it should never be!
        var currMoves = getCurrMoves();
        var oppMoves = getOppositeMoves();
        var isAttacked = findAttacking(oppMoves);
        var isCheck = isAttacked[kingHex.getFile()][kingHex.getRank()];
        if (isCheck) {
            // if the current king is in check, we cannot move any other pieces
            // TODO: add support for maintaining all "blocking" moves
            currMoves.clear();
        }

        // now, we can add the king moves
        whiteMoves.add(whiteKingMoves);
        blackMoves.add(blackKingMoves);
    }

    public boolean[][] findAttacking(List<PieceMoves> moves) {
        var isAttacked = new boolean[FILES][];
        for (var i = 0; i < isAttacked.length; i++) {
            isAttacked[i] = new boolean[RANKS_PER_FILE[i]]; // defaulted to false
        }

        for (var pm : moves) {
            for (var move : pm.getMoves()) {
                // make an exception for the pawn... which does not attack when moving ahead
                var piece = board.getPiece(pm.getHex());
                var isMovingAhead = move.getRank() > pm.getHex().getRank();
                if (isPawn(piece) && isMovingAhead) {
                    continue;
                }
                isAttacked[move.getFile()][move.getRank()] = true;
            }
        }
        return isAttacked;
    }

    public boolean isCheckmate() {
        var turn = board.turn();
        var kingHex = board.findKing(turn);
        var pieceMoves = getPieceMoves(turn);
        var oppPieceMoves = getPieceMoves(turn.opposite());
        var kingMoves = pieceMoves.get(pieceMoves.size() - 1);

        // the LAST element should always be the king moves!
        assert(board.getPiece(kingMoves.getHex()) == (turn.isWhite() ? WHITE_KING : BLACK_KING));

        // a king must be checked to be in checkmate
        var isAttacked = findAttacking(oppPieceMoves);
        var isChecked = isAttacked[kingHex.getFile()][kingHex.getRank()];
        if (!isChecked) {
            return false;
        }

        // and all hexagons it can move to must be attacked (aka the opponent can move there)
        for (var move : kingMoves.getMoves()) {
            if (!isAttacked[move.getFile()][move.getRank()])
                return false;
        }

        // TODO: add support for maintaining all "blocking" moves
        // TODO: add support for preventing taking defended attackers
        return true;
    }

    // finds all pieces moves excluding the king moves, which are handled elsewhere
    public List<PieceMoves> findPieceMoves(Turn turn) {
        List<PieceMoves> moves = new ArrayList<>();
        for (var hex : Hexagon.ORDERED) {
            var piece = board.getPiece(hex);
            if (piece != EMPTY && isPieceTurn(piece, turn)) {
                // we check the piece type to find the right piece moves (we have already checked the color)
                switch (piece) {
                    case WHITE_ROOK, BLACK_ROOK -> moves.add(findRookMoves(hex));
                    case WHITE_BISHOP, BLACK_BISHOP -> moves.add(findBishopMoves(hex));
                    case WHITE_QUEEN, BLACK_QUEEN -> moves.add(findQueenMoves(hex));
                    case WHITE_KNIGHT, BLACK_KNIGHT -> moves.add(findKnightMoves(hex));
                    case WHITE_PAWN, BLACK_PAWN -> moves.add(findPawnMoves(hex, turn));
                    case WHITE_KING, BLACK_KING -> {} // this is a no-op, we find the king moves in a separate function
                    default -> throw new IllegalStateException("Board has invalid piece " + piece + " at hexagon " + hex);
                }
            }
        }
        return moves;
    }

    public PieceMoves findRookMoves(Hexagon hex) {
        return new PieceMoves(hex, findMovesByTraveling(hex, ROOK_OFFSETS));
    }

    public PieceMoves findBishopMoves(Hexagon hex) {
        return new PieceMoves(hex, findMovesByTraveling(hex, BISHOP_OFFSETS));
    }

    public PieceMoves findQueenMoves(Hexagon hex) {
        return new PieceMoves(hex, findMovesByTraveling(hex, KING_OFFSETS));
    }

    public PieceMoves findKnightMoves(Hexagon hex) {
        return new PieceMoves(hex, findOffsetMoves(hex, KNIGHT_OFFSETS));
    }

    // finds ALL the king moves and returns it directly to a list
    public List<Hexagon> findKingMoves(Hexagon hex) {
        // we want all moves, so nothing is attacking
        return findKingMoves(hex, (x) -> true);
    }

    public List<Hexagon> findKingMoves(Hexagon hex, Function<Hexagon, Boolean> isNotAttacked) {
        return findOffsetMoves(hex, KING_OFFSETS, isNotAttacked);
    }

    private static final Direction[] WHITE_AHEAD = {Direction.UP};
    private static final Direction[] WHITE_TAKE_LEFT = {Direction.UP_LEFT};
    private static final Direction[] WHITE_TAKE_RIGHT = {Direction.UP_RIGHT};
    private static final Direction[] BLACK_AHEAD = {Direction.DOWN};
    private static final Direction[] BLACK_TAKE_LEFT = {Direction.DOWN_LEFT};
    private static final Direction[] BLACK_TAKE_RIGHT = {Direction.DOWN_RIGHT};

    public PieceMoves findPawnMoves(Hexagon hex, Turn turn) {
        var basePiece = board.getPiece(hex);
        List<Hexagon> moves = new ArrayList<>();

        // we can always move one rank ahead on the same file
        var move1 = hex.walk(turn.isWhite() ? WHITE_AHEAD : BLACK_AHEAD);
        if (board.inBounds(move1)) {
            var piece = board.getPiece(move1);
            if (piece == EMPTY) {
                moves.add(move1);
            }
        }

        // we can move a rank ahead of that if we haven't moved yet!
        var move2 = move1.walk(turn.isWhite() ? WHITE_AHEAD : BLACK_AHEAD);
        if (board.inBounds(move2) && !hasPawnMoved(hex, basePiece)) {
            var piece = board.getPiece(move2);
            if (piece == EMPTY) {
                moves.add(move2);
            }
        }

        // we can also take in adjacent ranks
        var move3 = hex.walk(turn.isWhite() ? WHITE_TAKE_LEFT : BLACK_TAKE_LEFT);
        if (board.inBounds(move3)) {
            var piece = board.getPiece(move3);
            if (piece != EMPTY && areOpposite(basePiece, piece)) {
                moves.add(move3);
            }
        }

        var move4 = hex.walk(turn.isWhite() ? WHITE_TAKE_RIGHT : BLACK_TAKE_RIGHT);
        if (board.inBounds(move4)) {
            var piece = board.getPiece(move4);
            if (piece != EMPTY && areOpposite(basePiece, piece)) {
                moves.add(move4);
            }
        }

        return new PieceMoves(hex, moves);
    }

    // travel along the offsets - aka keep on going until we can't move in that direction
    public List<Hexagon> findMovesByTraveling(Hexagon hex, Direction[][] directions) {
        var basePiece = board.getPiece(hex);
        List<Hexagon> moves = new ArrayList<>();

        for (var direction : directions) {
            var move = hex;

            while (true) {
                move = move.walk(direction);

                if (!board.inBounds(move)) {
                    break;
                }

                var piece = board.getPiece(move);
                if (piece == EMPTY) {
                    // we can move here and keep going!
                    moves.add(move);
                } else {
                    // we cannot keep going - maybe we can take if the piece is opposite color
                    if (areOpposite(piece, basePiece)) {
                        moves.add(move);
                    }
                    break;
                }
            }
        }

        return moves;
    }

    public List<Hexagon> findOffsetMoves(Hexagon hex, Direction[][] directions) {
        return findOffsetMoves(hex, directions, (x) -> true);
    }

    public List<Hexagon> findOffsetMoves(Hexagon hex, Direction[][] directions, Function<Hexagon, Boolean> canMoveTo) {
        var basePiece = board.getPiece(hex);
        List<Hexagon> moves = new ArrayList<>();

        for (var direction : directions) {
            var move = hex.walk(direction);
            if (!board.inBounds(move)) {
                continue;
            }

            var piece = board.getPiece(move);
            var canMoveHex = piece == EMPTY || areOpposite(basePiece, piece); // short-circuiting prevents us from checking if an empty piece is opposite

            if (canMoveHex && canMoveTo.apply(move)) {
                moves.add(move);
            }
        }

        return moves;
    }
}
