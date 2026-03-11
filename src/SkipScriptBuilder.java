import java.io.IOException;
import java.nio.file.*;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class SkipScriptBuilder {

    public Optional<Path> build(List<SkipTime> skipTimes, Path tmp) throws IOException {
        if (skipTimes.isEmpty()) {
            System.out.println("  Skip times: none found.");
            return Optional.empty();
        }

        var labels = skipTimes.stream()
            .map(s -> s.label().replace("Skip ", "").toLowerCase()) // "intro", "outro"
            .collect(Collectors.joining(" + "));
        System.out.println("  Skip times: " + labels + " found.");

        var entries = new StringBuilder();
        for (var s : skipTimes) {
            entries.append("  {label=\"%s\", start=%s, stop=%s},\n"
                .formatted(s.label(), s.start(), s.stop()));
        }

        var lua = """
                local skips = {
                %s}
                local skip_shown = {}
                mp.observe_property("time-pos", "number", function(_, pos)
                    if pos == nil then return end
                    for i, s in ipairs(skips) do
                        if pos >= s.start and pos < s.stop and not skip_shown[i] then
                            skip_shown[i] = true
                            mp.osd_message("[Press G to " .. s.label .. "]", 9999)
                            mp.add_forced_key_binding("g", "do_skip_" .. i, function()
                                mp.set_property("time-pos", s.stop)
                                mp.osd_message("Skipped!", 2)
                                mp.remove_key_binding("do_skip_" .. i)
                            end)
                        elseif (pos < s.start or pos >= s.stop) and skip_shown[i] then
                            mp.remove_key_binding("do_skip_" .. i)
                            mp.osd_message("", 0)
                        end
                    end
                end)
                """.formatted(entries);

        var script = tmp.resolve("skip.lua");
        Files.writeString(script, lua);
        return Optional.of(script);
    }
}
