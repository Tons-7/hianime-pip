import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.Executors;

public class StreamResolver {

    private static final ObjectMapper JSON = new ObjectMapper();

    private final SkipTimeFetcher skipTimeFetcher = new SkipTimeFetcher();

    public StreamInfo resolve(String url) throws Exception {
        var proc = new ProcessBuilder(Config.YTDLP_PATH, "--dump-json", url).start();

        String stdout, stderr;
        try (var pool = Executors.newVirtualThreadPerTaskExecutor()) {
            var outFuture = pool.submit(() -> new String(proc.getInputStream().readAllBytes()));
            var errFuture = pool.submit(() -> new String(proc.getErrorStream().readAllBytes()));
            stdout = outFuture.get();
            stderr = errFuture.get();
        }
        proc.waitFor();

        var jsonLine = Arrays.stream(stdout.strip().split("\\r?\\n"))
            .filter(l -> l.startsWith("{"))
            .reduce((a, b) -> b)
            .orElseThrow(() -> new RuntimeException(
                "yt-dlp returned no JSON.\n" + (stderr.isBlank() ? stdout : stderr).strip()
            ));

        var root    = JSON.readTree(jsonLine);
        var formats = parseFormats(root);

        var chosen = formats.stream()
            .max(Comparator.comparingInt(Format::height))
            .orElseThrow(() -> new RuntimeException("No playable formats found."));

        System.out.println("  Quality: " + chosen.label());

        var referer   = chosen.httpHeaders() != null
            ? JSON.readTree(chosen.httpHeaders()).path("Referer").asText("https://aniwatchtv.to/")
            : "https://aniwatchtv.to/";
        var userAgent = chosen.httpHeaders() != null
            ? JSON.readTree(chosen.httpHeaders()).path("User-Agent").asText("Mozilla/5.0")
            : "Mozilla/5.0";

        var skipTimes = skipTimeFetcher.fetch(root);

        return new StreamInfo(chosen.url(), referer, userAgent, List.copyOf(skipTimes));
    }

    private List<Format> parseFormats(JsonNode root) {
        var formats = new ArrayList<Format>();
        var formatsNode = root.path("formats");

        if (formatsNode.isArray()) {
            for (var f : formatsNode) {
                var url = f.path("url").asText(null);
                if (url == null) continue;

                var height  = f.path("height").asInt(0);
                var headers = f.has("http_headers") ? f.get("http_headers").toString() : null;
                var label   = height > 0 ? height + "p" : f.path("format_id").asText("unknown");

                var existing = formats.stream().filter(x -> x.label().equals(label)).findFirst();
                if (existing.isPresent()) {
                    var tbr = f.path("tbr").asDouble(0);
                    if (tbr <= existing.get().height()) continue;
                    formats.remove(existing.get());
                }

                formats.add(new Format(url, height, label, headers));
            }
        }

        if (formats.isEmpty() && root.has("url")) {
            var headers = root.has("http_headers") ? root.get("http_headers").toString() : null;
            formats.add(new Format(root.get("url").asText(), 0, "default", headers));
        }

        return formats;
    }
}
