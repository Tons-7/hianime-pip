import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.http.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class SkipTimeFetcher {

    private static final Set<String> INTRO_KEYWORDS = Set.of("intro", "opening", "op", "cold open", "prologue");
    private static final Set<String> OUTRO_KEYWORDS = Set.of("outro", "ending", "ed", "credits", "end credits", "epilogue");

    private static final ObjectMapper JSON = new ObjectMapper();
    private static final HttpClient HTTP   = HttpClient.newHttpClient();

    public List<SkipTime> fetch(JsonNode root) throws Exception {
        var fromChapters = fromChapters(root);
        if (!fromChapters.isEmpty()) return fromChapters;

        return fromAniSkip(root);
    }

    private List<SkipTime> fromChapters(JsonNode root) {
        var skipTimes = new ArrayList<SkipTime>();
        var chapters  = root.path("chapters");

        if (!chapters.isArray() || chapters.isEmpty()) return skipTimes;

        for (var ch : chapters) {
            var title = ch.path("title").asText("");
            var start = ch.path("start_time").asDouble();
            var end   = ch.path("end_time").asDouble();

            if (end <= start) continue;
            var key = title.strip().toLowerCase();

            if (INTRO_KEYWORDS.stream().anyMatch(key::contains)) {
                skipTimes.add(new SkipTime("Skip Intro", start, end));
            } else if (OUTRO_KEYWORDS.stream().anyMatch(key::contains)) {
                skipTimes.add(new SkipTime("Skip Outro", start, end));
            }
        }

        return skipTimes;
    }

    private List<SkipTime> fromAniSkip(JsonNode root) throws Exception {
        var malId   = malId(root);
        var episode = episodeNumber(root);

        if (malId == null) return List.of();

        var url = "https://api.aniskip.com/v2/skip-times/%s/%s?types[]=op&types[]=ed&episodeLength=0"
            .formatted(malId, episode);

        var response = HTTP.send(
            HttpRequest.newBuilder(URI.create(url)).GET().build(),
            HttpResponse.BodyHandlers.ofString()
        );

        if (response.statusCode() != 200) return List.of();

        var body    = JSON.readTree(response.body());
        var results = body.path("results");

        if (!body.path("found").asBoolean() || !results.isArray()) return List.of();

        var skipTimes = new ArrayList<SkipTime>();
        for (var r : results) {
            var type     = r.path("skip_type").asText("");
            var interval = r.path("interval");
            var start    = interval.path("start_time").asDouble();
            var end      = interval.path("end_time").asDouble();

            switch (type) {
                case "op" -> skipTimes.add(new SkipTime("Skip Intro", start, end));
                case "ed" -> skipTimes.add(new SkipTime("Skip Outro", start, end));
            }
        }

        return List.copyOf(skipTimes);
    }

    private String malId(JsonNode root) {
        for (var field : List.of("mal_id", "malId", "myanimelist_id")) {
            if (root.hasNonNull(field)) return root.get(field).asText();
        }
        var meta = root.path("series_metadata");
        if (!meta.isMissingNode() && meta.hasNonNull("mal_id"))
            return meta.get("mal_id").asText();
        return null;
    }

    private int episodeNumber(JsonNode root) {
        for (var field : List.of("episode_number", "episode", "episodeNumber")) {
            if (root.hasNonNull(field)) return root.get(field).asInt(1);
        }
        return 1;
    }
}
