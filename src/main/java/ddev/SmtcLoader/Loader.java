package ddev.SmtcLoader;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import lombok.Getter;
import lombok.SneakyThrows;

import java.io.*;

public class Loader {
    private static final String DEFAULT_ARTIST_TEXT = "Witting Information...";
    private static final String NO_MEDIA_TEXT = "No media playing";
    private static final String[] BLOCKED_KEYWORDS = {"抖音", "快手", "bilibili", "哔哩哔哩"};
    private static final long UPDATE_INTERVAL_MS = 1000;

    public static String titleText = "";
    public static String lastTitle = null;
    public static String artistText = "";
    public static String lastArtist = null;
    public static String totalTimeText = "";
    public static String lastTotalTime = null;
    public static String passTimeText = "";
    public static String lastBase64 = "";
    public static String base64 = "";
    public static String stateText = "";
    public static String sourceApp = "";
    public static int progress = 0;
    public static boolean changed = false;

    public Thread musicInfoThread;
    @Getter
    public MediaInfo currentMediaInfo = new MediaInfo("", "", "", "", 0, "", State.Unknown, false, "");

    public static double currentPositionSeconds;

    public native String getMediaInfo();

    static {
        loadNativeLibrary();
    }

    private static void loadNativeLibrary() {
        String dllPath = "/native/smtc.dll";
        try (InputStream in = Loader.class.getResourceAsStream(dllPath)) {
            if (in == null) {
                throw new FileNotFoundException("DLL not found: " + dllPath);
            }

            File tempDll = File.createTempFile("smtc", ".dll");
            tempDll.deleteOnExit();

            try (OutputStream out = new FileOutputStream(tempDll)) {
                byte[] buffer = new byte[4096];
                int bytesRead;
                while ((bytesRead = in.read(buffer)) != -1) {
                    out.write(buffer, 0, bytesRead);
                }
            }
            System.load(tempDll.getAbsolutePath());
        } catch (IOException e) {
            throw new RuntimeException("Failed to load DLL", e);
        }
    }

    public Loader() {
        startMusicInfoThread();
    }

    private void startMusicInfoThread() {
        musicInfoThread = new Thread(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                updateMediaInfo();
                try {
                    Thread.sleep(UPDATE_INTERVAL_MS);
                } catch (InterruptedException e) {
                    System.err.println("SMTC thread interrupted: " + e.getMessage());
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }, "SMTC Music Catcher");
        musicInfoThread.start();
    }

    @SneakyThrows
    public void updateMediaInfo() {
        String info = getMediaInfo();
        if (shouldClearMediaInfo(info)) {
            clearMediaInfo();
            return;
        }

        try {
            JsonObject json = JsonParser.parseString(info).getAsJsonObject();
            processMediaJson(json);
        } catch (JsonSyntaxException e) {
            System.err.println("SMTC JSON Error: " + e.getMessage());
            clearMediaInfo();
        }
    }

    private boolean shouldClearMediaInfo(String info) {
        if (info == null || info.equals("{}")) return true;
        for (String keyword : BLOCKED_KEYWORDS) {
            if (info.contains(keyword)) return true;
        }
        return false;
    }

    private void processMediaJson(JsonObject json) {
        double totalTimeSeconds = getDoubleValue(json, "totalTime");
        currentPositionSeconds = getDoubleValue(json, "currentPosition");
        String base64Data = getStringValue(json, "thumbnail");
        String state = getStringValue(json, "playbackStatus");
        sourceApp = getStringValue(json, "sourceApp");
        stateText = state.replace("Unknown", "Closed");
        changed = getBooleanValue(json, "changed");

        if (isPlaybackStopped(stateText)) {
            clearMediaInfo();
            return;
        }

        updateTitle(json);
        updateArtist(json);
        updateTotalTime(totalTimeSeconds);
        updateThumbnail(base64Data);
        updateProgressAndPassTime(totalTimeSeconds);

        currentMediaInfo = new MediaInfo(
                titleText, artistText, totalTimeText, passTimeText,
                progress, base64, mapState(stateText), changed, sourceApp
        );
    }

    private boolean isPlaybackStopped(String state) {
        return state.equalsIgnoreCase("Closed") ||
               state.equalsIgnoreCase("Opened") ||
               state.equalsIgnoreCase("Stopped");
    }

    private void updateTitle(JsonObject json) {
        String newTitle = getStringValue(json, "title");
        titleText = newTitle;
        lastTitle = newTitle;
    }

    private void updateArtist(JsonObject json) {
        String newArtist = getStringValue(json, "artist");
        if (newArtist != null) {
            artistText = newArtist;
            lastArtist = newArtist;
        } else {
            artistText = DEFAULT_ARTIST_TEXT;
        }
    }

    private void updateTotalTime(double totalTimeSeconds) {
        String newTotalTime = formatTime(totalTimeSeconds);
        if (!newTotalTime.equals(lastTotalTime)) {
            totalTimeText = newTotalTime;
            lastTotalTime = newTotalTime;
        }
    }

    private void updateThumbnail(String base64Data) {
        if (base64Data != null && !base64Data.equals(lastBase64)) {
            base64 = base64Data;
            lastBase64 = base64Data;
        }
    }

    private void updateProgressAndPassTime(double totalTimeSeconds) {
        passTimeText = formatTime(currentPositionSeconds + 1.0);
        progress = totalTimeSeconds > 0 ?
            (int) ((currentPositionSeconds / totalTimeSeconds) * 100) : 0;
    }

    private static State mapState(String state) {
        switch (state) {
            case "Playing": return State.Playing;
            case "Paused": return State.Paused;
            case "Stopped": return State.Stopped;
            default: return State.Unknown;
        }
    }

    public static String formatTime(double seconds) {
        int minutes = (int) (seconds / 60);
        int remainingSeconds = (int) (seconds % 60);
        return String.format("%02d:%02d", minutes, remainingSeconds);
    }

    public void clearMediaInfo() {
        titleText = NO_MEDIA_TEXT;
        artistText = "";
        totalTimeText = "";
        passTimeText = "";
        base64 = "";
        progress = 0;
        lastTitle = null;
        lastArtist = null;
        lastTotalTime = null;
        lastBase64 = "";
        changed = false;
        sourceApp = "";

        currentMediaInfo = new MediaInfo(
                titleText, artistText, totalTimeText, passTimeText,
                progress, base64, mapState(stateText), changed, sourceApp
        );
    }

    private String getStringValue(JsonObject json, String key) {
        return json.has(key) ? json.get(key).getAsString() : "";
    }

    private double getDoubleValue(JsonObject json, String key) {
        return json.has(key) ? json.get(key).getAsDouble() : 0.0;
    }

    private boolean getBooleanValue(JsonObject json, String key) {
        return json.has(key) && json.get(key).getAsBoolean();
    }

    public static Loader startSmtc() {
        return new Loader();
    }

    public record MediaInfo(
            String title,
            String artist,
            String totalTime,
            String passTime,
            int progress,
            String base64,
            State state,
            boolean changed,
            String sourceApp
    ) {
        @Override
        public String toString() {
            return "MediaInfo{" +
                    "title='" + title + '\'' +
                    ", artist='" + artist + '\'' +
                    ", totalTime='" + totalTime + '\'' +
                    ", passTime='" + passTime + '\'' +
                    ", progress=" + progress +
                    ", base64 length='" + base64.length() + '\'' +
                    ", state='" + state.getDisplayName() + '\'' +
                    ", changed='" + changed + '\'' +
                    ", sourceApp='" + sourceApp + '\'' +
                    '}';
        }
    }

    public enum State {
        Playing("Playing"),
        Paused("Paused"),
        Stopped("Stopped"),
        Unknown("Unknown");

        @Getter
        private final String displayName;

        State(String displayName) {
            this.displayName = displayName;
        }
    }
}