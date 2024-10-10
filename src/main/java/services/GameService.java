package services;

import domain.Hexagon;
import lombok.AllArgsConstructor;
import models.GameState;
import models.HistoryEntity;
import models.Player;
import utils.Serializer;
import utils.Threading;

import java.util.Random;
import java.util.UUID;

import static utils.Log.LOGGER;

@AllArgsConstructor
public class GameService {

    public static class MoveException extends RuntimeException {
        public MoveException(String message) {
            super(message);
        }
    }

    private static final Random RANDOM = new Random();

    private final RemoteDict remoteDict;
    private final UserDao userDao;
    private final HistoryDao historyDao;

    public String create(Boolean isFirstPlayerWhite) {
        var id = UUID.randomUUID().toString();
        var gameState = GameState.startWithGame(id);

        gameState.setIsFirstPlayerWhite(isFirstPlayerWhite);
        gameState.getGame().initPieceMoves();

        remoteDict.setGame(id, gameState);
        return id;
    }

    public GameState join(String gameId, Player player) {
        var state = remoteDict.getGame(gameId);
        if (state == null) {
            return null;
        }

        var hasWhitePlayer = state.getWhitePlayer() != null;
        var hasBlackPlayer = state.getBlackPlayer() != null;

        if (!hasWhitePlayer && !hasBlackPlayer) {
            // neither player, so join as either
            var isFirstPlayerWhite = state.getIsFirstPlayerWhite();
            boolean chooseWhite = isFirstPlayerWhite == null ? RANDOM.nextInt() % 2 == 0 : isFirstPlayerWhite;
            if (chooseWhite) {
                state.setWhitePlayer(player);
                LOGGER.info(String.format("Player %s joined as white player %s", player.getId(), gameId));
            } else {
                state.setBlackPlayer(player);
                LOGGER.info(String.format("Player %s joined as black player %s", player.getId(), gameId));
            }
        } else if (!hasBlackPlayer) {
            // no black player, so join as black
            state.setBlackPlayer(player);
            LOGGER.info(String.format("Player %s joined as black player %s", player.getId(), gameId));
        } else if (!hasWhitePlayer) {
            // no white player, so join as white
            state.setWhitePlayer(player);
            LOGGER.info(String.format("Player %s joined as white player %s", player.getId(), gameId));
        } else {
            // both players, so we cannot join... just return the game data to view
            return state;
        }

        return remoteDict.setGame(gameId, state);
    }

    public GameState makeMove(String gameId, Player player, Hexagon.Move move) {
        var state = remoteDict.getGame(gameId);
        if (state == null) {
            return null;
        }

        var game = state.getGame();

        if (state.isEnded()) {
            LOGGER.info(String.format("Move attempted on ended game %s", gameId));
            throw new MoveException("Cannot make a move on a game that is over!");
        }
        if (!state.isPlayerTurn(player)) {
            LOGGER.info(String.format("%s cannot make move on game %s, it isn't their turn", player, gameId));
            throw new MoveException("Cannot make a move when it isn't your turn!");
        }
        if (!game.isValidMove(move)) {
            LOGGER.info(String.format(" %s made invalid move %s on game %s", player, move, gameId));
            throw new MoveException("Cannot make an invalid move!");
        }

        game.makeMove(move);
        game.initPieceMoves();

        state.pushMoveHistory(move);

        if (game.isCheckmate()) {
            state.setEnded(true);
            var isWhiteWin = !game.getBoard().turn().isWhite(); // white wins if its checkmate when it's NOT their turn
            Threading.EXECUTOR.execute(() -> onFinishGame(state, isWhiteWin));
        }

        LOGGER.info(String.format("%s made move %s on game %s", player, move, gameId));
        return remoteDict.setGame(gameId, state);
    }

    public void onFinishGame(GameState state, boolean isWhiteWin) {
        try {
            var whiteId =  state.getWhitePlayer().getId();
            var blackId = state.getBlackPlayer().getId();
            var result = isWhiteWin ? HistoryEntity.WHITE_WIN : HistoryEntity.WHITE_LOSS;
            var winId = isWhiteWin ? whiteId : blackId;
            var loseId = isWhiteWin ? blackId : whiteId;

            var moveHistoryData = Serializer.serialize(state.getMoveHistory());

            var changeSet = userDao.updateStatsUsingResult(winId, loseId);
            remoteDict.updateLeaderboardUser(
                new RemoteDict.EloChangeSet(winId, changeSet.winElo),
                new RemoteDict.EloChangeSet(loseId, changeSet.loseElo));
            historyDao.insert(whiteId, blackId, result, moveHistoryData, changeSet.getWinElo(), changeSet.getLoseElo());
        } catch (Exception ex) {
            LOGGER.info("Failed to persist game results to database in background thread " + ex);
        }
    }

    public GameState forfeit(String gameId, Player player) {
        var state = remoteDict.getGame(gameId);
        if (state == null) {
            return null;
        }

        var didBlackForfeit = state.getBlackPlayer().equals(player);

        state.setEnded(true);
        onFinishGame(state, didBlackForfeit);

        return remoteDict.setGame(gameId, state); // did black forfeit? then white won.
    }

    public RemoteDict.GetGameKeysResult getManyKeys(Double cursor) {
        return remoteDict.getGameKeys(cursor, 20);
    }
}
