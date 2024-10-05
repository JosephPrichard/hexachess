package web;

import io.jooby.Jooby;
import io.jooby.exception.NotFoundException;

public class FileRouter extends Jooby {
    public FileRouter(State state) {
        var filesMap = state.getFilesMap();

        get("/files/flags/{name}", ctx -> {
            var name = ctx.path("name").toOptional().orElse("");
            var fileBytes = filesMap.get(name);
            if (fileBytes == null) {
                throw new NotFoundException("The requested file does not exist");
            }
            ctx.setResponseType("image/png");
            return fileBytes;
        });
    }
}
