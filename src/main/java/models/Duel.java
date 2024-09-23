package models;

import domain.ChessGame;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Duel {

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Player {
        private String id;
        @EqualsAndHashCode.Exclude
        private String name;
    }

    private String id;
    private ChessGame game;
    private Player whitePlayer = null;
    private Player blackPlayer = null;
    private boolean isEnded;
    private Boolean isFirstPlayerWhite = null; // true - first player joining should be white... false - first player joining should be black... null - random...
    @EqualsAndHashCode.Exclude
    private double touch;

    public static Duel start(String id) {
        return new Duel(id, ChessGame.start(), null, null, false, null, 0);
    }

    public static Duel ofPlayers(Player whitePlayer, Player blackPlayer) {
        return new Duel("", null, whitePlayer, blackPlayer, false, null, 0);
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
}
