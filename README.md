# anime-pip

A minimal command-line tool that plays anime from [AniWatchTV](https://aniwatchtv.to) or [Kaido](https://kaido.to) in a Picture-in-Picture window using mpv, with automatic subtitle fetching and skip intro/outro support.

## Prerequisites

| Tool                                                                           | Version | Notes |
|--------------------------------------------------------------------------------|---------|-------|
| [Java JDK](https://jdk.java.net/25/)                                           | 25+ | Must be on your `PATH` |
| [Maven](https://maven.apache.org/download.cgi)                                 | 3.8+ | Must be on your `PATH` |
| [mpv.net](https://github.com/mpvnet-player/mpv.net)                            | Latest | Windows mpv frontend |
| [yt-dlp](https://github.com/yt-dlp/yt-dlp)                                     | Latest | Used to resolve stream URLs and subtitles |
| [yt-dlp-aniwatch-kaido plugin](https://github.com/Tons-7/yt-dlp-aniwatchtv-kaido) | Latest | yt-dlp extractor plugin for AniWatchTV / Kaido — **required** |

### Install yt-dlp

Download the `.exe` directly from the [yt-dlp releases](https://github.com/yt-dlp/yt-dlp/releases) page.

Or install using Chocolatey:

```
choco install yt-dlp
```

### Install the yt-dlp plugin

yt-dlp does not support AniWatchTV or Kaido natively — this plugin is required for stream extraction to work.

**If you installed yt-dlp via Python/pip:**
```
pip install -U https://github.com/Tons-7/yt-dlp-aniwatchtv-kaido/archive/master.zip
```

**If you installed yt-dlp as a standalone `.exe`:**

1. Download the repo as a ZIP from https://github.com/Tons-7/yt-dlp-aniwatchtv-kaido and extract it
2. Create this exact folder structure (create folders if they don't exist):
   ```
   %APPDATA%\yt-dlp\plugins\aniwatch\yt_dlp_plugins\extractor\
   ```
3. Place `aniwatch.py` and `megacloud.py` into that `extractor` folder

The final structure should look like:
```
%APPDATA%\yt-dlp\plugins\aniwatch\yt_dlp_plugins\extractor\aniwatch.py
%APPDATA%\yt-dlp\plugins\aniwatch\yt_dlp_plugins\extractor\kaido.py
%APPDATA%\yt-dlp\plugins\aniwatch\yt_dlp_plugins\extractor\megacloud.py
```

The plugin is invoked automatically when an AniWatchTV or Kaido URL is detected — no extra flags needed.

## Setup

**1. Clone the repo**
```
git clone https://github.com/Tons-7/aniwatchtv-kaido-pip
cd aniwatchtv-kaido-pip
```

**2. Create your `.env` file**

Copy the example and fill in your local paths:
```
cp .env.example .env
```

Then edit `.env`:
```
MPV_PATH=C:\path\to\mpvnet.exe
YTDLP_PATH=C:\path\to\yt-dlp.exe
```

The `.env` file is gitignored and never committed.

**3. Build**
```
mvn package
```

## Running

From the project directory in CMD, use `play`:

**Interactive mode** — prompts you to paste the URL:
```
play
```

**Direct mode** — pass the URL as an argument:
```
play https://aniwatchtv.to/watch/one-piece-100?ep=2142
```

If the URL contains `&` (e.g. multiple query parameters), wrap it in quotes:
```
play "https://aniwatchtv.to/watch/one-piece-100?ep=2142&dub=true"
```

> You can also run via Maven directly: `mvn exec:java -Dexec.mainClass=Main`

The tool will:
1. Resolve the highest quality stream URL via yt-dlp
2. Fetch skip times (from yt-dlp chapters, or AniSkip API as fallback)
3. Fetch English subtitles — both run in parallel
4. Launch mpv.net in a PiP window in the bottom-right corner

## Features

- **Auto quality** — always picks the highest available resolution
- **Skip intro/outro** — press `G` when the on-screen prompt appears
- **Subtitles** — English subs fetched automatically if available
- **PiP window** — stays on top at 30% screen size, anchored to bottom-right

## How it works

```
Anime URL (aniwatchtv.to or kaido.to)
    │
    ▼
yt-dlp + plugin    → stream URL + chapters
    │
    ├── AniSkip API          → fallback skip times if no chapters
    ├── yt-dlp --write-subs  → English subtitles        ← parallel
    │
    ▼
mpv.net
    + Lua skip script
    + subtitle file
```
