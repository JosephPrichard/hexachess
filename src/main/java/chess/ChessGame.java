package chess;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Stream;

import static chess.ChessBoard.*;
import static chess.Hexagon.Direction;

public class ChessGame {

    public final static Direction[][] ROOK_OFFSETS = {
        {Direction.UP},
        {Direction.DOWN},
        {Direction.DOWN_LEFT},
        {Direction.DOWN_RIGHT},
        {Direction.UP_LEFT},
        {Direction.UP_RIGHT}
    };
    public final static Direction[][] BISHOP_OFFSETS = {
        {Direction.UP_RIGHT, Direction.DOWN_RIGHT},
        {Direction.UP_LEFT, Direction.DOWN_LEFT},
        {Direction.UP, Direction.UP_RIGHT},
        {Direction.UP, Direction.UP_LEFT},
        {Direction.DOWN, Direction.DOWN_RIGHT},
        {Direction.DOWN, Direction.DOWN_LEFT}
    };
    public final static Direction[][] KING_OFFSETS =
        Stream.concat(Arrays.stream(ROOK_OFFSETS), Arrays.stream(BISHOP_OFFSETS)).toArray(Direction[][]::new);
    public final static Direction[][] KNIGHT_OFFSETS = {
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

    private final ChessBoard board;
    private Hexagon whiteKingHex;
    private Hexagon blackKingHex;
    private List<PieceMoves> whiteMoves = null;
    private List<PieceMoves> blackMoves = null;

    public static ChessGame start() {
        return new ChessGame(ChessBoard.initial());
    }

    public static ChessGame empty() {
        return new ChessGame(new ChessBoard(true), null, null);
    }

    public ChessGame(ChessBoard board, Hexagon whiteKingHex, Hexagon blackKingHex) {
        this.board = board;
        this.whiteKingHex = whiteKingHex;
        this.blackKingHex = blackKingHex;
    }

    public ChessGame(ChessBoard board) {
        this.board = board;
        whiteKingHex = board.findKing(true);
        blackKingHex = board.findKing(false);
    }

    public ChessBoard board() {
        return board;
    }

    public Hexagon getKingHex(boolean color) {
        return color ? whiteKingHex : blackKingHex;
    }

    public List<PieceMoves> getWhiteMoves() {
        return whiteMoves;
    }

    public List<PieceMoves> getBlackMoves() {
        return blackMoves;
    }

    public List<PieceMoves> getPieceMoves(boolean color) {
        return color ? whiteMoves : blackMoves;
    }

    public List<PieceMoves> getCurrMoves() {
        return getPieceMoves(board.color());
    }

    public List<PieceMoves> getOppositeMoves() {
        return getPieceMoves(!board.color());
    }

    public boolean canMove(boolean color) {
        return color == board.color();
    }

    public boolean canMove(boolean color, Hexagon fromHex, Hexagon toHex) {
        assert whiteMoves != null;
        assert blackMoves != null;

        if (color != board.color()) {
            return false;
        }
        var moves = getCurrMoves();

        // has a match for a move from one hexagon to another hexagon
        return moves.stream().anyMatch((pm) -> {
            var isFrom = pm.piecePos().equals(fromHex);
            var hasTo = pm.moves().stream().anyMatch((hex) -> hex.equals(toHex));
            return isFrom && hasTo;
        });
    }

    public void initPieceMoves() {
        // we find the piece moves for all other pieces besides the king
        whiteMoves = findPieceMoves(true);
        blackMoves = findPieceMoves(false);
        var currMoves = getCurrMoves();
        var oppMoves = getOppositeMoves();

        // find the moves for both kings - excluding any attacking squares
        var whiteKingMoves = new PieceMoves(whiteKingHex, findKingMoves(whiteKingHex));
        var blackKingMoves = new PieceMoves(blackKingHex, findKingMoves(blackKingHex));
        var kingHex = board.color() ? whiteKingHex : blackKingHex;

        // decide whether we will add the piece moves... are we in check?
        // we don't need to check if the opposite move is in check... it should never be!
        var isAttacking = findAttacking(oppMoves);
        var isCheck = isAttacking[kingHex.toIndex()];
        if (isCheck) {
            // if the current king is in check, we cannot move any other pieces
            // TODO: add support for maintaining all "blocking" moves
            currMoves.clear();
        }

        // now, we can add the king moves
        this.whiteMoves.add(whiteKingMoves);
        this.blackMoves.add(blackKingMoves);
    }

    public boolean[] findAttacking(List<PieceMoves> pieceMoves) {
        var isAttacked = new boolean[board.maxLength()]; // defaulted to false
        for (var pm : pieceMoves) {
            for (var move : pm.moves()) {
                // make an exception for the pawn... which does not attack when moving ahead
                var piece = board.getPiece(pm.piecePos());
                var isMovingAhead = move.rank() > pm.piecePos().rank();
                if (isPawn(piece) && isMovingAhead) {
                    continue;
                }
                isAttacked[move.toIndex()] = true;
            }
        }
        return isAttacked;
    }

    public void makeMove(Hexagon fromHex, Hexagon toHex) {
        var piece = board.getPiece(fromHex);
        board.setPiece(fromHex, EMPTY);
        board.setPiece(toHex, piece);
        board.flipTurn();

        if (piece == WHITE_KING) {
            whiteKingHex = toHex;
        } else if (piece == BLACK_KING) {
            blackKingHex = toHex;
        }

        whiteMoves = null;
        blackMoves = null;
    }

    public boolean canPromote(Hexagon hex) {
        var isLastRank = switch (hex.file()) {
            case 0, 10 -> hex.rank() >= 5;
            case 1, 9 -> hex.rank() >= 6;
            case 2, 8 -> hex.rank() >= 7;
            case 3, 7 -> hex.rank() >= 8;
            case 4, 6 -> hex.rank() >= 9;
            case 5 -> hex.rank() >= 10;
            default -> throw new IllegalStateException("Cannot promote to an invalid file " + hex.file());
        };
        var piece = board.getPiece(hex);
        return isLastRank && ChessBoard.isPawn(piece);
    }

    public boolean isCheckmate() {
        var boardColor = board.color();
        var kingHex = getKingHex(boardColor);
        var pieceMoves = getPieceMoves(boardColor);
        var oppPieceMoves = getPieceMoves(!boardColor);
        var kingMoves = pieceMoves.get(pieceMoves.size() - 1);

        // the LAST element should always be the king moves!
        assert(board.getPiece(kingMoves.piecePos()) == (boardColor ? WHITE_KING : BLACK_KING));

        // store whether each hexagon is directly attacked by the opponent
        var isAttacked = new boolean[board.maxLength()]; // defaulted to false
        for (var moves : oppPieceMoves) {
            for (var move : moves.moves()) {
                isAttacked[move.toIndex()] = true;
            }
        }

        // a king must be checked to be in checkmate
        var isChecked = isAttacked[kingHex.toIndex()];
        if (!isChecked) {
            return false;
        }

        // and all hexagons it can move to must be attacked (aka the opponent can move there)
        for (var move : kingMoves.moves()) {
            if (!isAttacked[move.toIndex()])
                return false;
        }

        // TODO: add support for maintaining all "blocking" moves
        return true;
    }

    // finds all pieces moves excluding the king moves, which are handled elsewhere
    public List<PieceMoves> findPieceMoves(boolean playerColor) {
        List<PieceMoves> pieceMoves = new ArrayList<>();
        for (var hex : Hexagon.ORDERED) {
            var piece = board.getPiece(hex);
            if (piece != EMPTY && isColor(piece, playerColor)) {
                // we check the piece type to find the right piece moves (we have already checked the color)
                switch (piece) {
                    case WHITE_ROOK, BLACK_ROOK -> pieceMoves.add(findRookMoves(hex));
                    case WHITE_BISHOP, BLACK_BISHOP -> pieceMoves.add(findBishopMoves(hex));
                    case WHITE_QUEEN, BLACK_QUEEN -> pieceMoves.add(findQueenMoves(hex));
                    case WHITE_KNIGHT, BLACK_KNIGHT -> pieceMoves.add(findKnightMoves(hex));
                    case WHITE_PAWN, BLACK_PAWN -> pieceMoves.add(findPawnMoves(hex));
                    case WHITE_KING, BLACK_KING -> {} // this is a no-op, we find the king moves in a separate function
                    default -> throw new IllegalStateException("Board has invalid piece " + piece + " at hexagon " + hex);
                }
            }
        }
        return pieceMoves;
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
        return findKingMoves(hex, (x) -> true); // we want all moves, so nothing is attacking
    }

    public List<Hexagon> findKingMoves(Hexagon hex, Function<Hexagon, Boolean> isNotAttacked) {
        return findOffsetMoves(hex, KING_OFFSETS, isNotAttacked);
    }

    public PieceMoves findPawnMoves(Hexagon hex) {
        var basePiece = board.getPiece(hex);
        List<Hexagon> moves = new ArrayList<>();

        // we can always move one rank ahead on the same file
        var move1 = hex.walk(Direction.UP);
        if (board.inBounds(move1)) {
            var piece = board.getPiece(move1);
            if (piece == EMPTY) {
                moves.add(move1);
            }
        }

        // we can move a rank ahead of that if we haven't moved yet!
        var move2 = move1.walk(Direction.UP);
        if (board.inBounds(move2) && !hasPawnMoved(hex, basePiece)) {
            var piece = board.getPiece(move2);
            if (piece == EMPTY) {
                moves.add(move2);
            }
        }

        // we can also take in adjacent ranks
        var move3 = hex.walk(Direction.UP_LEFT);
        if (board.inBounds(move3)) {
            var piece = board.getPiece(move3);
            if (piece != EMPTY && areOpposite(basePiece, piece)) {
                moves.add(move3);
            }
        }

        var move4 = hex.walk(Direction.UP_RIGHT);
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
            var canMoveHex = areOpposite(basePiece, piece) || piece == EMPTY;

            if (canMoveHex && canMoveTo.apply(move)) {
                moves.add(move);
            }
        }

        return moves;
    }
}
