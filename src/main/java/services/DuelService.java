package services;

import domain.Hexagon;
import models.Duel;

import java.util.Random;
import java.util.UUID;

import static utils.Log.LOGGER;

public class DuelService {

    public static class MoveException extends RuntimeException {
        public MoveException(String message) {
            super(message);
        }
    }

    private final DictService dictService;
    private final Random rand = new Random();

    public DuelService(DictService dictService) {
        this.dictService = dictService;
    }

    public String create(Boolean isFirstPlayerWhite) {
        var id = UUID.randomUUID().toString();
        var duel = Duel.start(id);

        duel.setIsFirstPlayerWhite(isFirstPlayerWhite);
        duel.getGame().initPieceMoves();

        dictService.setDuel(id, duel);
        return id;
    }

    public Duel join(String duelId, Duel.Player player) {
        var duel = dictService.getDuel(duelId);
        if (duel == null) {
            return null;
        }

        var hasWhitePlayer = duel.getWhitePlayer() != null;
        var hasBlackPlayer = duel.getBlackPlayer() != null;

        if (!hasWhitePlayer && !hasBlackPlayer) {
            // neither player, so join as either
            var isFirstPlayerWhite = duel.getIsFirstPlayerWhite();
            boolean chooseWhite = isFirstPlayerWhite == null ? rand.nextInt() % 2 == 0 : isFirstPlayerWhite;
            if (chooseWhite) {
                duel.setWhitePlayer(player);
                LOGGER.info(String.format("Player %s joined as white player %s", player.getId(), duelId));
            } else {
                duel.setBlackPlayer(player);
                LOGGER.info(String.format("Player %s joined as black player %s", player.getId(), duelId));
            }
        } else if (!hasBlackPlayer) {
            // no black player, so join as black
            duel.setBlackPlayer(player);
            LOGGER.info(String.format("Player %s joined as black player %s", player.getId(), duelId));
        } else if (!hasWhitePlayer) {
            // no white player, so join as white
            duel.setWhitePlayer(player);
            LOGGER.info(String.format("Player %s joined as white player %s", player.getId(), duelId));
        } else {
            // both players, so we cannot join... just return the duel data to view
            return duel;
        }

        return dictService.setDuel(duelId, duel);
    }

    public Duel makeMove(String duelId, Duel.Player player, Hexagon.Move move) {
        var duel = dictService.getDuel(duelId);
        if (duel == null) {
            return null;
        }

        var game = duel.getGame();

        if (duel.isEnded()) {
            LOGGER.info(String.format("Move attempted on ended duel %s", duelId));
            throw new MoveException("Cannot make a move on a game that is over!");
        }
        if (!duel.isPlayerTurn(player)) {
            LOGGER.info(String.format("Player %s cannot make move on duel %s, it isn't their turn", player, duelId));
            throw new MoveException("Cannot make a move when it isn't your turn!");
        }
        if (!game.isValidMove(move)) {
            LOGGER.info(String.format("Player %s made invalid move %s on duel %s", player, move, duelId));
            throw new MoveException("Cannot make an invalid move!");
        }

        game.makeMove(move);
        game.initPieceMoves();

        LOGGER.info(String.format("Player %s made move %s on duel %s", player, move, duelId));
        return dictService.setDuel(duelId, duel);
    }

    public Duel forfeit(String duelId) {
        var duel = dictService.getDuel(duelId);
        if (duel == null) {
            return null;
        }

        duel.setEnded(true);
        return dictService.setDuel(duelId, duel);
    }

    public DictService.GetDuelKeysResult getManyKeys(Double cursor) {
        return dictService.getDuelKeys(cursor, 20);
    }
}
