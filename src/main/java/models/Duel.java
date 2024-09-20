package models;

import domain.ChessGame;
import lombok.*;

@Data
@AllArgsConstructor
public class Duel {

    @Data
    @AllArgsConstructor
    public static class Player {
        private String id;
        @EqualsAndHashCode.Exclude
        private String name;
    }

    private ChessGame game;
    private Player whitePlayer = null;
    private Player blackPlayer = null;
    private boolean isEnded;

    public static Duel start() {
        return new Duel(ChessGame.start(), null, null, false);
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
