import java.io.IOException;
import java.nio.file.*;
import java.util.HashMap;
import java.util.Map;

public final class Config {
    private Config() {}

    public static final String MPV_PATH;
    public static final String YTDLP_PATH;

    static {
        var env = loadEnv();
        MPV_PATH   = env.getOrDefault("MPV_PATH",   "");
        YTDLP_PATH = env.getOrDefault("YTDLP_PATH", "");
    }

    private static Map<String, String> loadEnv() {
        var map  = new HashMap<String, String>();
        var path = Path.of(".env");

        if (!Files.exists(path)) {
            System.err.println("Error: .env file not found. Copy .env.example to .env and fill in your paths.");
            System.exit(1);
        }

        try {
            for (var line : Files.readAllLines(path)) {
                line = line.strip();
                if (line.isEmpty() || line.startsWith("#")) continue;
                var eq = line.indexOf('=');
                if (eq < 1) continue;
                var key   = line.substring(0, eq).strip();
                var value = line.substring(eq + 1).strip();
                map.put(key, value);
            }
        } catch (IOException e) {
            System.err.println("Error: could not read .env — " + e.getMessage());
            System.exit(1);
        }

        return map;
    }

    public static void validate() {
        checkExecutable("mpv",    MPV_PATH,   "MPV_PATH");
        checkExecutable("yt-dlp", YTDLP_PATH, "YTDLP_PATH");
    }

    private static void checkExecutable(String name, String path, String key) {
        if (path.isEmpty() || !Files.isExecutable(Path.of(path))) {
            System.err.println("Error: " + name + " not found or not executable.");
            System.err.println("  Set " + key + " correctly in your .env file.");
            System.exit(1);
        }
    }
}
