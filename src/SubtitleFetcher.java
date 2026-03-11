import java.nio.file.*;
import java.util.List;

public class SubtitleFetcher {

    public List<String> fetch(String url, Path tmp) throws Exception {
        new ProcessBuilder(
            Config.YTDLP_PATH,
            "--write-subs", "--sub-langs", "en",
            "--skip-download",
            "-o", tmp.resolve("sub").toString(),
            url
        ).redirectErrorStream(true).start().waitFor();

        try (var walk = Files.walk(tmp, 1)) {
            return walk
                .filter(p -> p.getFileName().toString().matches(".*\\.(vtt|ass)"))
                .map(p -> "--sub-file=" + p)
                .limit(1)
                .toList();
        }
    }
}
