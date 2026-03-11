import java.nio.file.Path;
import java.util.*;

public class MpvLauncher {

    public void launch(StreamInfo info, Optional<Path> luaScript, List<String> subArgs) throws Exception {
        var cmd = new ArrayList<>(List.of(
            Config.MPV_PATH,
            "--ontop=yes",
            "--autofit=30%",
            "--geometry=100%:100%",
            "--sub-visibility=yes",
            "--hwdec=auto",
            "--cache=yes",
            "--demuxer-max-bytes=400MiB",
            "--http-header-fields=Referer: %s\r\nUser-Agent: %s"
                .formatted(info.referer(), info.userAgent())
        ));

        luaScript.ifPresent(p -> cmd.add("--script=" + p));
        cmd.addAll(subArgs);
        cmd.add(info.url());

        int exit = new ProcessBuilder(cmd).inheritIO().start().waitFor();
        if (exit != 0) {
            System.err.println("Warning: mpv exited with code " + exit);
        }
    }
}
