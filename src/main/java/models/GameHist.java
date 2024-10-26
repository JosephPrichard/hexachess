package models;

import com.fasterxml.jackson.core.JsonProcessingException;
import domain.ChessBoard;
import domain.Move;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static utils.Globals.JSON_MAPPER;

@Data
@AllArgsConstructor
public class GameHist {
    ChessBoard initialBoard;
    List<Move> moveList;

    public static GameHist start(ChessBoard initialBoard) {
        return new GameHist(initialBoard, new ArrayList<>());
    }

    public GameHist deepCopy() {
        return new GameHist(initialBoard.deepCopy(), moveList.stream().map(Move::deepCopy).toList());
    }

    public static String randomAsJson() {
        try {
            var newGame = GameState.startWithGame(UUID.randomUUID().toString()).applyRandomSequence(10);
            return JSON_MAPPER.writeValueAsString(newGame.getHistory());
        } catch (JsonProcessingException ex) {
            throw new RuntimeException(ex);
        }
    }
}