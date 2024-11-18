package models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import domain.ChessGame;
import domain.Move;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GameState {
    String id;
    ChessGame game;
    Player whitePlayer = null;
    Player blackPlayer = null;
    boolean isEnded;
    @JsonIgnore
    Boolean isFirstPlayerWhite = null; // true - first player joining should be white... false - first player joining should be black... null - random...
    @EqualsAndHashCode.Exclude
    @JsonIgnore
    double touch;
    @JsonIgnore
    GameHist history;

    public GameState deepCopy() {
        return new GameState(id,
            game != null ? game.deepCopy() : null,
            whitePlayer != null ? whitePlayer.deepCopy() : null,
            blackPlayer != null ? blackPlayer.deepCopy() : null,
            isEnded,
            isFirstPlayerWhite,
            touch,
            history != null ? history.deepCopy() : null);
    }

    public static GameState startWithGame(String id) {
        var game = ChessGame.start();
        var history = GameHist.start(game.getBoard());
        return new GameState(id, game, null, null, false, null, 0, history);
    }

    public static GameState ofPlayers(Player whitePlayer, Player blackPlayer) {
        return new GameState("", null, whitePlayer, blackPlayer, false, null, 0, null);
    }

    public Player getCurrPlayer() {
        return game.getBoard().turn().isWhite() ? whitePlayer : blackPlayer;
    }

    public boolean isPlayerTurn(Player player) {
        var currPlayer = getCurrPlayer();
        if (currPlayer == null) {
            return false;
        }
        return currPlayer.equals(player);
    }

    public void pushMoveHistory(Move move) {
        history.getMoveList().add(move);
    }

    public GameState applyRandomSequence(int count) {
        for (int i = 0; i < count; i++) {
            game.initPieceMoves();

            var currMoves = game.getCurrMoves();

            var pm = currMoves.stream().filter((x) -> !x.getMoves().isEmpty()).findFirst().orElseThrow(); // we're going to assume there is at least one piece
            var from = pm.getHex();
            var to = pm.getMoves().getFirst(); // make the first move (we already know there is at least one)

            var move = new Move(from, to);
            game.makeMove(move);
            history.getMoveList().add(move);
        }
        return this;
    }
}
