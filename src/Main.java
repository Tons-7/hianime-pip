void main() {
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
    IO.print("Paste URL: ");
    var url = scanner.nextLine().strip();
    if (url.isEmpty()) {
        System.err.println("Error: no URL provided.");
        System.exit(1);
    }

    var tmp = Files.createTempDirectory("watch_");

    IO.println("\n[1/3] Resolving stream URL...");
    var info = new StreamResolver().resolve(url);
    IO.println(" Stream URL resolved.");

    IO.println("[2/3] Fetching skip times + subtitles...");
    var luaScript = (Optional<Path>) null;
    var subArgs = (List<String>) null;

    try (var pool = Executors.newVirtualThreadPerTaskExecutor()) {
        var luaFuture = pool.submit(() -> new SkipScriptBuilder().build(info.skipTimes(), tmp));
        var subFuture = pool.submit(() -> {
            var result = new SubtitleFetcher().fetch(url, tmp);
            IO.println("  Subtitles: " + (result.isEmpty() ? "not found." : "found!"));
            return result;
        });

        try {
            luaScript = luaFuture.get();
        } catch (ExecutionException e) {
            throw (Exception) e.getCause();
        }

        try {
            subArgs = subFuture.get();
        } catch (ExecutionException e) {
            throw (Exception) e.getCause();
        }
    }

    IO.println("[3/3] Launching PiP player...\n");
    new MpvLauncher().launch(info, luaScript, subArgs);
}
