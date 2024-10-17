package web;

import io.jooby.Jooby;
import io.jooby.exception.NotFoundException;
import web.State;

public class FileRouter extends Jooby {
    public FileRouter(State state) {
        var filesMap = state.getFiles();

        assets("/css/index.css", "/css/index.css");

        assets("/scripts/index.js", "/scripts/index.js");

        get("/files/flags/{name}", ctx -> {
            var name = ctx.path("name").toOptional().orElse("");
            var fileBytes = filesMap.get(name);
            if (fileBytes == null) {
                throw new NotFoundException("The requested file does not exist: " + name);
            }
            ctx.setResponseType("image/png");
            return fileBytes;
        });
    }
}
