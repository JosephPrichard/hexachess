import static io.jooby.Jooby.runApp;

public class Main {
    public static void main(String[] args) {
        runApp(args, Router::init);
    }
}