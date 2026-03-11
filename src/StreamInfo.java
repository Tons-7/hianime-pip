import java.util.List;

public record StreamInfo(
    String url,
    String referer,
    String userAgent,
    List<SkipTime> skipTimes
) {}
