import java.nio.file.Files;
import java.util.Scanner;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;

public class Main {

    public static void main(String[] args) {
        try {
            run();
        } catch (Exception e) {
            var cause = (e instanceof ExecutionException ee && ee.getCause() != null)
                ? ee.getCause() : e;
            System.err.println("\nError: " + cause.getMessage());
            System.exit(1);
        }
    }

    private static void run() throws Exception {
        Config.validate();

        var scanner = new Scanner(System.in);
        System.out.print("Paste HiAnime URL: ");
        var url = scanner.nextLine().strip();
        if (url.isEmpty()) {
            System.err.println("Error: no URL provided.");
            System.exit(1);
        }

        var tmp = Files.createTempDirectory("watch_");

        System.out.println("\n[1/3] Resolving stream URL...");
        var info = new StreamResolver().resolve(url);
        System.out.println(" Stream URL resolved.");

        System.out.println("[2/3] Fetching skip times + subtitles...");
        var luaScript = (java.util.Optional<java.nio.file.Path>) null;
        var subArgs   = (java.util.List<String>) null;

        try (var pool = Executors.newVirtualThreadPerTaskExecutor()) {
            var luaFuture = pool.submit(() -> new SkipScriptBuilder().build(info.skipTimes(), tmp));
            var subFuture = pool.submit(() -> {
                var result = new SubtitleFetcher().fetch(url, tmp);
                System.out.println("  Subtitles: " + (result.isEmpty() ? "not found." : "found!"));
                return result;
            });

            try { luaScript = luaFuture.get(); }
            catch (ExecutionException e) { throw (Exception) e.getCause(); }

            try { subArgs = subFuture.get(); }
            catch (ExecutionException e) { throw (Exception) e.getCause(); }
        }

        System.out.println("[3/3] Launching PiP player...\n");
        new MpvLauncher().launch(info, luaScript, subArgs);
    }
}
