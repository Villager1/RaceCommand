package io.github.hielkemaps.racecommand;

public class Util {

    public static String ordinal(int i) {
        String[] suffixes = new String[] { "th", "st", "nd", "rd", "th", "th", "th", "th", "th", "th" };
        switch (i % 100) {
            case 11:
            case 12:
            case 13:
                return i + "th";
            default:
                return i + suffixes[i % 10];

        }
    }

    public static String getTimeString(int totalSecs) {
        if(totalSecs == -1) return "undefined";

        int hours = totalSecs / 3600;
        int minutes = (totalSecs % 3600) / 60;
        int seconds = totalSecs % 60;

        if(hours == 0){
            return String.format("%02d:%02d", minutes, seconds);
        }

        return String.format("%02d:%02d:%02d", hours, minutes, seconds);
    }
}
