package web;

import io.jooby.Jooby;
import io.jooby.exception.NotFoundException;

public class FileRouter extends Jooby {
    public FileRouter(State state) {
        var filesMap = state.getFiles();

        assets("/css/index.css", "/css/index.css");

        assets("/scripts/chess.js", "/scripts/chess.js");

        assets("/scripts/session.js", "/scripts/session.js");

        get("/files/flags/{name}", ctx -> {
            ctx.setResponseType("image/png");

            var name = ctx.path("name").toOptional().orElse("");
            var fileBytes = filesMap.get(name);
            if (fileBytes == null) {
                throw new NotFoundException("The requested file does not exist: " + name);
            }
            return fileBytes;
        });
    }
}
