package org.drinko.util;

import com.sedmelluq.discord.lavaplayer.track.AudioTrackInfo;

import java.time.Duration;
import java.time.temporal.ChronoUnit;

public class ListFormatterUtils {

    public static String getPageNumberLine(long startPage, long endPage) {
        if (startPage > endPage) {
            startPage = endPage;
        }
        return "Page " + startPage + " of " + endPage + "\n";
    }

    public static String getFormattedTitleString(AudioTrackInfo audioTrackInfo) {
        return " **" + sanitizeMarkdownCharacters(audioTrackInfo.title) + " " + getAudioTrackLengthString(audioTrackInfo) + "**";
    }
    public static String getAudioTrackLengthString(AudioTrackInfo audioTrackInfo) {
        if (audioTrackInfo.isStream) {
            return "`[stream]`";
        }
        Duration duration = Duration.of(audioTrackInfo.length, ChronoUnit.MILLIS);
        String timeString = "`[";

        long hours = duration.toHours();
        if (hours > 0) {
            if (hours < 10) {
                timeString += "0";
            }
            timeString += hours + ":";
        }
        long minutes = duration.toMinutesPart();
        if (minutes < 10) {
            timeString += "0";
        }
        timeString += minutes + ":";

        long seconds = duration.toSecondsPart();
        if (seconds < 10) {
            timeString += "0";
        }
        timeString += seconds;
        return timeString + "]`";
    }

    public static String getPositionString(long position) {
        return "`[" + (position) + "]`";
    }

    private static String sanitizeMarkdownCharacters(String textWithMarkdown) {
        return textWithMarkdown.replace("`", "\\`")
                .replace("*", "\\*")
                .replace("_", "\\_")
                .replace(">", "\\>")
                .replace("|", "\\|")
                .replace("~", "\\~");
    }
}
