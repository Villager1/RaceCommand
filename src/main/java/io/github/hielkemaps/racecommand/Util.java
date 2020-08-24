package io.github.hielkemaps.racecommand;

public class Util {

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
