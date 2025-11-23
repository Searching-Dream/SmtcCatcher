package ddev.SmtcLoader;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import lombok.Getter;
import lombok.SneakyThrows;

import java.io.*;

public class Loader {
    public static String titleText = "";
    public static String lastTitle = null;
    public static String artistText = "";
    public static String lastArtist = null;
    public static String totalTimeText = "";
    public static String lastTotalTime = null;
    public static String passTimeText = "";
    public static String lastbase64 = "";
    public static String base64 = "";
    public static String stateText = "";
    public static int progress = 0;
    public static boolean changed = false;

    public Thread musicInfoThread;
    @Getter
    public MediaInfo currentMediaInfo = new MediaInfo("", "", "", "", 0, "", State.Unknown, false);

    public static double currentPositionSeconds;

    public native String getMediaInfo();

    static {
        try {
            String dllPath = "/native/smtc.dll";

            InputStream in = Loader.class.getResourceAsStream(dllPath);

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
        musicInfoThread = new Thread(() -> {
            while (true) {
                updateMediaInfo();
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    System.err.println("SMTC Error: " + e);
                    break;
                }
            }
        }, "SMTC Music Catcher");

        musicInfoThread.start();
    }

    @SneakyThrows
    public void updateMediaInfo() {
        String info = getMediaInfo();
        if (info == null || info.equals("{}") || info.contains("抖音") || info.contains("快手") || info.contains("bilibili") || info.contains("哔哩哔哩")) {
            clearMediaInfo();
            return;
        }

        try {
            JsonObject json = JsonParser.parseString(info).getAsJsonObject();

            double totalTimeSeconds = json.has("totalTime") ? json.get("totalTime").getAsDouble() : 0;
            currentPositionSeconds = json.has("currentPosition") ? json.get("currentPosition").getAsDouble() : 0;
            String base64Data = json.has("thumbnail") ? json.get("thumbnail").getAsString() : null;
            String state = json.has("playbackStatus") ? json.get("playbackStatus").getAsString() : "";
            stateText = state.replace("Unknown", "Closed");
            changed = json.has("changed") && json.get("changed").getAsBoolean();
            if (stateText.equalsIgnoreCase("Closed") || stateText.equalsIgnoreCase("Opened") || stateText.equalsIgnoreCase("Stopped")) {
                clearMediaInfo();
                return;
            }

            String newTitle = json.has("title") ? json.get("title").getAsString() : "";
            titleText = newTitle;
            lastTitle = newTitle;

            String newArtist = json.has("artist") ? json.get("artist").getAsString() : null;
            if (newArtist != null) {
                artistText = newArtist;
                lastArtist = newArtist;
            } else {
                artistText = "Witting Information...";
            }

            String newTotalTime = formatTime(totalTimeSeconds);
            if (!newTotalTime.equals(lastTotalTime)) {
                totalTimeText = newTotalTime;
                lastTotalTime = newTotalTime;
            }

            if (base64Data != null && !base64Data.equals(lastbase64)) {
                base64 = base64Data;
                lastbase64 = base64Data;
            }

            passTimeText = formatTime(currentPositionSeconds + 1.0);
            progress = (int) ((currentPositionSeconds / totalTimeSeconds) * 100);

            currentMediaInfo = new MediaInfo(titleText, artistText, totalTimeText, passTimeText, progress, base64, markState(stateText), changed);
        } catch (JsonSyntaxException e) {
            System.out.println("SMTC Error: " + String.valueOf(e));
            clearMediaInfo();
        }
    }

    private static State markState(String state) {
        switch (state) {
            case "Playing":
                return State.Playing;
            case "Paused":
                return State.Paused;
            case "Stopped":
                return State.Stopped;
            default:
                return State.Unknown;
        }
    }

    public static String formatTime(double seconds) {
        int minutes = (int) (seconds / 60);
        int remainingSeconds = (int) (seconds % 60);
        return String.format("%02d:%02d", minutes, remainingSeconds);
    }

    public void clearMediaInfo() {
        titleText = "No media playing";
        artistText = "";
        totalTimeText = "";
        passTimeText = "";
        base64 = "";
        progress = 0;
        lastTitle = null;
        lastArtist = null;
        lastTotalTime = null;
        lastbase64 = "";
        changed = false;

        currentMediaInfo = new MediaInfo(titleText, artistText, totalTimeText, passTimeText, progress, base64, markState(stateText), changed);
    }

    public static Loader startSmtc() {
        return new Loader();
    }

    public record MediaInfo(String title, String artist, String totalTime, String passTime, int progress, String base64,
                            State state, boolean changed) {
        @Override
        public String toString() {
            return "MediaInfo{" +
                    "title='" + title + '\'' +
                    ", artist='" + artist + '\'' +
                    ", totalTime='" + totalTime + '\'' +
                    ", passTime='" + passTime + '\'' +
                    ", progress=" + progress +
                    ", base64 length='" + base64.length() + '\'' +
                    ", state='" + state().getDisplayName() + '\'' +
                    ", changed='" + changed + '\'' +
                    '}';
        }
    }

    public enum State {
        Playing("Playing"),
        Paused("Paused"),
        Stopped("Stopped"),
        Unknown("Unknown");

        @Getter
        public final String displayName;

        State(String displayName) {
            this.displayName = displayName;
        }
    }
}
