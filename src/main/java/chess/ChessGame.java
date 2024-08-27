package chess;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Stream;

import static chess.ChessBoard.*;

public class ChessGame {

    public final static int[][] ROOK_OFFSETS = {{0, 1}, {0, -1}, {-1, 0}, {1, 0}, {-1, -1}, {1, -1}};
    public final static int[][] BISHOP_OFFSETS = {{1, 1}, {-1, 1}, {-2, 0}, {2, 0}, {-1, -2}, {1, -2}};
    public final static int[][] ROYAL_OFFSETS = Stream.concat(Arrays.stream(ROOK_OFFSETS), Arrays.stream(BISHOP_OFFSETS)).toArray(int[][]::new);
    public final static int[][] KNIGHT_OFFSETS = {{-3, -2}, {-3, -1}, {-2, -3}, {-2, 1}, {-1, -3}, {-1, 2}, {1, -3}, {1, 2}, {2, -3}, {2, 1}, {3, -2}, {3, -1}};

    private final ChessBoard board;
    private Hexagon whiteKingHex;
    private Hexagon blackKingHex;
    private List<PieceMoves> whiteMoves = null;
    private List<PieceMoves> blackMoves = null;

    public static ChessGame start() {
        return new ChessGame(ChessBoard.initial());
    }

    public static ChessGame empty() {
        return new ChessGame(new ChessBoard(true));
    }

    public ChessGame(ChessBoard board) {
        this.board = board;
        whiteKingHex = board.findKingHex(true);
        blackKingHex = board.findKingHex(false);
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
        var isAttacked = new boolean[board.length()]; // defaulted to false
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
        var isAttacked = new boolean[board.length()]; // defaulted to false
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
        for (int i = 0; i < board.length(); i++) {
            var piece = board.getPiece(i);
            if (piece != EMPTY && isColor(piece, playerColor)) {
                var hex = Hexagon.fromIndex(i);
                // we check the piece type to find the right piece moves (we have already checked the color)
                switch (piece) {
                    case WHITE_ROOK, BLACK_ROOK ->
                        pieceMoves.add(new PieceMoves(hex, findMovesByTraveling(hex, ROOK_OFFSETS)));
                    case WHITE_BISHOP, BLACK_BISHOP ->
                        pieceMoves.add(new PieceMoves(hex, findMovesByTraveling(hex, BISHOP_OFFSETS)));
                    case WHITE_QUEEN, BLACK_QUEEN ->
                        pieceMoves.add(new PieceMoves(hex, findMovesByTraveling(hex, ROYAL_OFFSETS)));
                    case WHITE_KNIGHT, BLACK_KNIGHT ->
                        pieceMoves.add(new PieceMoves(hex, findOffsetMoves(hex, KNIGHT_OFFSETS, (x) -> false)));
                    case WHITE_PAWN, BLACK_PAWN ->
                        pieceMoves.add(new PieceMoves(hex, findPawnMoves(hex)));
                    case WHITE_KING, BLACK_KING -> {} // this is a no-op, we find the king moves in a separate function
                    default -> throw new IllegalStateException("Board has invalid piece " + piece + " at hexagon " + hex);
                }
            }
        }
        return pieceMoves;
    }

    // finds ALL the king moves and returns it directly to a list
    public List<Hexagon> findKingMoves(Hexagon hex) {
        return findOffsetMoves(hex, ROYAL_OFFSETS, (x) -> false); // we want all moves, so nothing is attacking
    }

    public List<Hexagon> findKingMoves(Hexagon hex, Function<Hexagon, Boolean> isAttacking) {
        return findOffsetMoves(hex, ROYAL_OFFSETS, isAttacking);
    }

    public List<Hexagon> findPawnMoves(Hexagon hex) {
        var basePiece = board.getPiece(hex);
        List<Hexagon> moves = new ArrayList<>();

        // we can always move one rank ahead on the same file
        var file = hex.file();
        var rank = hex.rank() + 1;
        if (inBounds(file, rank)) {
            var piece = board.getPiece(file, rank);
            if (piece == EMPTY) {
                moves.add(new Hexagon(file, rank));
            }
        }

        // we can move a rank ahead of that if we haven't moved yet!
        var file1 = hex.file();
        var rank1 = hex.rank() + 2;
        if (inBounds(file1, rank1) && !hasPawnMoved(hex, basePiece)) {
            var piece = board.getPiece(file1, rank1);
            if (piece == EMPTY) {
                moves.add(new Hexagon(file1, rank1));
            }
        }

        // we can also take in adjacent ranks
        var file2 = hex.file() - 1;
        var rank2 = hex.rank();
        if (inBounds(file2, rank2)) {
            var piece = board.getPiece(file2, rank2);
            if (piece != EMPTY && areOpposite(basePiece, piece)) {
                // can only move if there is a piece to eat here
                moves.add(new Hexagon(file2, rank2));
            }
        }

        var file3 = hex.file() + 1;
        var rank3 = hex.rank() + 1;
        if (inBounds(file3, rank3)) {
            var piece = board.getPiece(file3, rank3);
            if (piece != EMPTY && areOpposite(basePiece, piece)) {
                // can only move if there is a piece to eat here
                moves.add(new Hexagon(file3, rank3));
            }
        }

        return moves;
    }

    // travel along the offsets - aka keep on going until we can't move in that direction
    public List<Hexagon> findMovesByTraveling(Hexagon hex, int[][] offsets) {
        var basePiece = board.getPiece(hex);
        List<Hexagon> moves = new ArrayList<>();

        for (int[] offset : offsets) {
            var multiplier = 1;

            while (true) {
                var file = hex.file() + offset[0] * multiplier;
                var rank = hex.rank() + offset[1] * multiplier;

                if (!inBounds(file, rank)) {
                    break;
                }

                var piece = board.getPiece(file, rank);
                if (piece == EMPTY) {
                    // we can move here and keep going!
                    var move = new Hexagon(file, rank);
                    moves.add(move);
                } else {
                    // we cannot keep going - maybe we can take if the piece is opposite color
                    if (areOpposite(piece, basePiece)) {
                        var move = new Hexagon(file, rank);
                        moves.add(move);
                    }
                    break;
                }

                multiplier += 1;
            }
        }

        return moves;
    }

    public List<Hexagon> findOffsetMoves(Hexagon hex, int[][] offsets, Function<Hexagon, Boolean> isAttacking) {
        var basePiece = board.getPiece(hex);
        List<Hexagon> moves = new ArrayList<>();

        for (int[] offset : offsets) {
            var file = hex.file() + offset[0];
            var rank = hex.rank() + offset[1];

            if (!inBounds(file, rank)) {
                break;
            }

            var piece = board.getPiece(file, rank);
            if ((areOpposite(basePiece, piece) || piece == EMPTY) && !isAttacking.apply(new Hexagon(file, rank))) {
                moves.add(new Hexagon(file, rank));
            }
        }

        return moves;
    }
}
