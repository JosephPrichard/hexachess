package models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import domain.ChessGame;
import domain.Hexagon;
import domain.Move;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

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
    List<Move> moveHistory;

    public GameState deepCopy() {
        return new GameState(id,
            game != null ? game.deepCopy() : null,
            whitePlayer != null ? whitePlayer.deepCopy() : null,
            blackPlayer != null ? blackPlayer.deepCopy() : null,
            isEnded,
            isFirstPlayerWhite,
            touch,
            moveHistory.stream().map(Move::deepCopy).toList());
    }

    public static GameState start(String id) {
        return new GameState(id, null, null, null, false, null, 0, new ArrayList<>());
    }

    public static GameState startWithGame(String id) {
        return new GameState(id, ChessGame.start(), null, null, false, null, 0, new ArrayList<>());
    }

    public static GameState ofPlayers(Player whitePlayer, Player blackPlayer) {
        return new GameState("", null, whitePlayer, blackPlayer, false, null, 0, new ArrayList<>());
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
        moveHistory.add(move);
    }
}
