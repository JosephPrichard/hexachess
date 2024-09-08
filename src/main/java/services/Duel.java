package services;

import chess.ChessGame;
import lombok.*;

@Getter
@Setter
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
public class Duel {
    private ChessGame game;
    private Player whitePlayer = null;
    private Player blackPlayer = null;
    private boolean isEnded;

    @Getter
    @ToString
    @EqualsAndHashCode
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Player {
        private String id;
        private String name;
    }

    public static Duel start() {
        return new Duel(ChessGame.start(), null, null, false);
    }

    public Player getCurrPlayer() {
        return game.getBoard().turn().isWhite() ? whitePlayer : blackPlayer;
    }

    public boolean isPlayerTurn(Duel.Player player) {
        var currPlayer = getCurrPlayer();
        if (currPlayer == null) {
            return false;
        }
        return currPlayer.equals(player);
    }
}
