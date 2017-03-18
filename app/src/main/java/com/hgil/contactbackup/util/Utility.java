package com.hgil.contactbackup.util;

/**
 * Created by mohan.giri on 17-03-2017.
 */

public class Utility {

    /* get duration in hour, minutes and seconds*/
    public static String timeDuration(String duration) {
        long longVal = Long.valueOf(duration);
        int hours = (int) longVal / 3600;
        int remainder = (int) longVal - hours * 3600;
        int mins = remainder / 60;
        remainder = remainder - mins * 60;
        int secs = remainder;

        String callDuration = String.format("%02d:%02d:%02d", hours, mins, secs);
        return callDuration;
    }

}
