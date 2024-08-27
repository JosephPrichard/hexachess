package chess;

public class ChessBoard {

    // white pieces have an odd parity
    // black pieces have an even parity
    public final static byte EMPTY = 0;
    public final static byte WHITE_PAWN = 1;
    public final static byte BLACK_PAWN = 2;
    public final static byte WHITE_KNIGHT = 3;
    public final static byte BLACK_KNIGHT = 4;
    public final static byte WHITE_BISHOP = 5;
    public final static byte BLACK_BISHOP = 6;
    public final static byte WHITE_ROOK = 7;
    public final static byte BLACK_ROOK = 8;
    public final static byte WHITE_QUEEN = 9;
    public final static byte BLACK_QUEEN = 10;
    public final static byte WHITE_KING = 11;
    public final static byte BLACK_KING = 12;

    // should always be equal to each other, lest weird things happen.
    public final static int RANKS = 11; // marked by numbers 1-11
    public final static int FILES = 11; // marked by letters A-L
    public final static int ROWS = RANKS + FILES - 1; // the number of vertical rows in the hexagonal board

    private final byte[] board = new byte[RANKS * FILES];
    private boolean boardColor = true;

    public ChessBoard(boolean boardColor) {
        this.boardColor = boardColor;
    }

    public static ChessBoard initial() {
        var board = new ChessBoard(true);

        board.setPiece("b1", WHITE_PAWN);
        board.setPiece("c2", WHITE_PAWN);
        board.setPiece("d3", WHITE_PAWN);
        board.setPiece("e4", WHITE_PAWN);
        board.setPiece("f5", WHITE_PAWN);
        board.setPiece("g4", WHITE_PAWN);
        board.setPiece("h3", WHITE_PAWN);
        board.setPiece("i2", WHITE_PAWN);
        board.setPiece("j1", WHITE_PAWN);

        board.setPiece("c1", WHITE_ROOK);
        board.setPiece("d1", WHITE_KNIGHT);
        board.setPiece("e1", WHITE_QUEEN);
        board.setPiece("f1", WHITE_BISHOP);
        board.setPiece("f2", WHITE_BISHOP);
        board.setPiece("f3", WHITE_BISHOP);
        board.setPiece("g1", WHITE_KING);
        board.setPiece("h1", WHITE_KNIGHT);
        board.setPiece("i1", WHITE_ROOK);

        board.setPiece("b7", BLACK_PAWN);
        board.setPiece("c7", BLACK_PAWN);
        board.setPiece("d7", BLACK_PAWN);
        board.setPiece("e7", BLACK_PAWN);
        board.setPiece("f7", BLACK_PAWN);
        board.setPiece("g7", BLACK_PAWN);
        board.setPiece("h7", BLACK_PAWN);
        board.setPiece("i7", BLACK_PAWN);
        board.setPiece("j7", BLACK_PAWN);

        board.setPiece("c8", BLACK_ROOK);
        board.setPiece("d9", BLACK_KNIGHT);
        board.setPiece("e10", BLACK_QUEEN);
        board.setPiece("f11", BLACK_BISHOP);
        board.setPiece("f10", BLACK_BISHOP);
        board.setPiece("f9", BLACK_BISHOP);
        board.setPiece("g10", BLACK_KING);
        board.setPiece("h9", BLACK_KNIGHT);
        board.setPiece("i8", BLACK_ROOK);

        return board;
    }

    public boolean color() {
        return boardColor;
    }

    public int length() {
        return board.length;
    }

    public void flipTurn() {
        boardColor = !boardColor;
    }

    public void setPiece(Hexagon hex, byte piece) {
        setPiece(hex.toIndex(), piece);
    }

    public byte getPiece(int file, int rank) {
        return getPiece(Hexagon.toIndex(file, rank));
    }

    public byte getPiece(Hexagon hex) {
        return getPiece(hex.toIndex());
    }

    public void setPiece(String notation, byte piece) {
        setPiece(Hexagon.fromNotation(notation), piece);
    }

    public byte getPiece(String notation) {
        return getPiece(Hexagon.fromNotation(notation));
    }

    public void setPiece(int index, byte piece) {
        board[index] = piece;
    }

    public byte getPiece(int index) {
        return board[index];
    }

    public static boolean hasPawnMoved(Hexagon pawnHex, byte piece) {
        // pawns have moved if they exit the rank threshold, based on what color the pawn is
        if (isWhite(piece)) {
            var minRank = switch (pawnHex.file()) {
                case 1, 9 -> 0;
                case 2, 8 -> 1;
                case 3, 7 -> 2;
                case 4, 6 -> 3;
                case 5 -> 4;
                default -> -1; // if a pawn is on any other file it must have moved
            };
            return pawnHex.rank() > minRank;
        } else {
            return pawnHex.rank() < 7;
        }
    }

    public Hexagon findKingHex(boolean pieceColor) {
        for (int i = 0; i < board.length; i++) {
            if (board[i] == WHITE_KING && pieceColor || board[i] == BLACK_KING && !pieceColor) {
                return Hexagon.fromIndex(i);
            }
        }
        throw new IllegalStateException("Board doesn't have a king");
    }

    public static boolean isColor(byte piece, boolean pieceColor) {
        // white is true, black is false
        return piece % 2 == 0 && pieceColor || piece % 2 == 1 && !pieceColor;
    }

    public static boolean isWhite(byte piece) {
        return piece % 2 == 1;
    }

    public static boolean areOpposite(byte piece1, byte piece2) {
        return piece1 % 2 != piece2 % 2;
    }

    public static boolean inBounds(int file, int rank) {
        return rank >= 0 && rank < RANKS && file >= 0 && file < FILES;
    }

    public static boolean isPawn(byte piece) {
        return piece == WHITE_PAWN || piece == BLACK_PAWN;
    }

    public static boolean isKnight(byte piece) {
        return piece == WHITE_KNIGHT || piece == BLACK_KNIGHT;
    }

    public static boolean isBishop(byte piece) {
        return piece == WHITE_BISHOP || piece == BLACK_BISHOP;
    }

    public static boolean isRook(byte piece) {
        return piece == WHITE_ROOK || piece == BLACK_ROOK;
    }

    public static boolean isQueen(byte piece) {
        return piece == WHITE_QUEEN || piece == BLACK_QUEEN;
    }

    public static boolean isKing(byte piece) {
        return piece == WHITE_KING || piece == BLACK_KING;
    }

    public static char pieceToChar(byte piece) {
        return switch (piece) {
            case EMPTY -> '.';
            case WHITE_PAWN -> 'P';
            case BLACK_PAWN -> 'p';
            case WHITE_KNIGHT -> 'N';
            case BLACK_KNIGHT -> 'n';
            case WHITE_BISHOP -> 'B';
            case BLACK_BISHOP -> 'b';
            case WHITE_ROOK -> 'R';
            case BLACK_ROOK -> 'r';
            case WHITE_QUEEN -> 'Q';
            case BLACK_QUEEN -> 'q';
            case WHITE_KING -> 'K';
            case BLACK_KING -> 'k';
            default -> throw new IllegalStateException("Invalid piece: " + piece);
        };
    }

    @Override
    public String toString() {
        return toRectString();
    }

    // print out the hexagon board to a string that makes it easy to track row and file!
    public String toRectString() {
        var sb = new StringBuilder();
        sb.append("   ");

        for (int file = 0; file < ChessBoard.FILES; file++) {
            char fileChar = (char) (file + 'a');
            sb.append(fileChar).append("  ");
        }
        sb.append("\n");

        for (int rank = 0; rank < ChessBoard.RANKS; rank++) {
            var rankStr = Integer.toString(rank + 1);
            sb.append(rankStr).append(" ".repeat(Math.max(0, 3 - rankStr.length())));

            for (int file = 0; file < ChessBoard.FILES; file++) {
                var piece = getPiece(file, rank);
                sb.append(pieceToChar(piece)).append("  ");
            }
            sb.append("\n");
        }

        return sb.toString();
    }

    // print out the hexagon board to a string that looks the way the board is supposed to!
    public String toHexString() {
        var sb = new StringBuilder();

        var startFile = 5;
        var startRank = 11;
        var count = 1;

        for (int i = 0; i < ChessBoard.ROWS; i++) {
            if (i % 2 == 0) {
                sb.append("  ");
            }

            sb.append(" ".repeat(Math.max(0, startFile)));

            var file = startFile;
            var rank = startRank;
            for (int j = 0; j < count; j++) {
                var piece = getPiece(file, rank);
                sb.append(" ")
                    .append(pieceToChar(piece))
                    .append(" ");

                file += 2;
                if (file < 5) {
                    rank += 1;
                } else {
                    rank -= 1;
                }
            }
            sb.append("\n");

            if (i < 5) {
                startFile -= 1;
                startRank -= 1;
                count += 1;
            } else if (i >= 15) {
                startFile += 1;
                startRank += 1;
                count -= 1;
            }

        }

        return sb.toString();
    }
}
