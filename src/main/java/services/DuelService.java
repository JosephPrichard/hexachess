package services;

import java.util.Random;
import java.util.UUID;

import static utils.Log.LOGGER;
import static chess.Hexagon.*;

public class DuelService {

    private final DuelStore matchStorage;
    private final Random rand = new Random();

    public DuelService(DuelStore matchStorage) {
        this.matchStorage = matchStorage;
    }

    public static class MatchException extends RuntimeException {
        public MatchException(String message) {
            super(message);
        }
    }

    public String create() {
        var game = Duel.start();
        var id = UUID.randomUUID().toString();
        matchStorage.setDuel(id, game);
        return id;
    }

    public Duel join(String matchId, Duel.Player player) {
        var match = matchStorage.getDuel(matchId);
        if (match == null) {
            return null;
        }

        var hasWhitePlayer = match.getBlackPlayer() != null;
        var hasBlackPlayer = match.getWhitePlayer() != null;

        if (hasWhitePlayer && hasBlackPlayer) {
            var chooseWhite = rand.nextInt() % 2 == 0;
            if (chooseWhite) {
                match.setWhitePlayer(player);
            } else {
                match.setBlackPlayer(player);
            }
        } else if (hasWhitePlayer) {
            match.setBlackPlayer(player);
        } else if (hasBlackPlayer) {
            match.setWhitePlayer(player);
        } else {
            // we can't join this game, so just return the match data
            return match;
        }

        matchStorage.setDuel(matchId, match);
        return match;
    }

    public Duel makeMove(String matchId, Duel.Player player, Move move) {
        var match = matchStorage.getDuel(matchId);
        if (match == null) {
            return null;
        }

        var game = match.getGame();

        if (match.isEnded()) {
            LOGGER.info(String.format("Move attempted on ended game %s", matchId));
            throw new MatchException("Cannot make a move on a game that is over!");
        }
        if (!match.isPlayerTurn(player)) {
            LOGGER.info(String.format("Player %s is cannot make move on game %s it isn't their turn", player, matchId));
            throw new MatchException("Cannot make a move when it isn't your turn!");
        }
        if (!game.isValidMove(move)) {
            LOGGER.info(String.format("Move %s on game %s is invalid", move, matchId));
            throw new MatchException("Cannot make an invalid move!");
        }

        game.makeMove(move);

        matchStorage.setDuel(matchId, match);
        return match;
    }

    public Duel forfeit(String matchId) {
        var match = matchStorage.getDuel(matchId);
        if (match == null) {
            return null;
        }

        match.setEnded(true);
        matchStorage.setDuel(matchId, match);
        return match;
    }

    public DuelStore.ScanResult getMany(String cursor) {
        return matchStorage.scanDuels(cursor);
    }
}
