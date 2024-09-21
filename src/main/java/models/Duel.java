package models;

import domain.ChessGame;
import lombok.*;

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

    private ChessGame game;
    private Player whitePlayer = null;
    private Player blackPlayer = null;
    private boolean isEnded;
    @EqualsAndHashCode.Exclude
    private double touch;

    public static Duel start() {
        return new Duel(ChessGame.start(), null, null, false, 0);
    }

    public static Duel ofPlayers(Player whitePlayer, Player blackPlayer) {
        return new Duel(null, whitePlayer, blackPlayer, false, 0);
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
