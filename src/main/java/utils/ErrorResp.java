package utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jooby.StatusCode;
import io.jooby.exception.StatusCodeException;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;

import static utils.Log.LOGGER;

@Data
@AllArgsConstructor
public class ErrorResp {
    private int status;
    private String message;

    public ErrorResp(String message) {
        this.message = message;
    }

    public static void throwJson(StatusCode code, String message, ObjectMapper jsonMapper) {
        try {
           var errorJson = jsonMapper.writeValueAsString(new ErrorResp(code.value(), message));
           throw new StatusCodeException(code, errorJson);
        } catch (JsonProcessingException e) {
            LOGGER.error(String.format("Failed to serialize json: %s", e.getMessage()));
            throw new StatusCodeException(StatusCode.SERVER_ERROR);
        }
    }
}

