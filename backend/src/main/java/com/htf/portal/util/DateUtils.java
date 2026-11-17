package com.htf.portal.util;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/**
 * Turns dates into friendly text such as "Closes in 2 days". Static helpers only.
 */
public final class DateUtils {

    private DateUtils() {
    }

    // ChronoUnit.DAYS.between counts whole days from "today" to "deadline" (negative if the deadline is past).
    public static String describeDeadline(LocalDate deadline, LocalDate today) {
        long days = ChronoUnit.DAYS.between(today, deadline);
        if (days > 1) {
            return "Closes in " + days + " days";
        }
        if (days == 1) {
            return "Closes tomorrow";
        }
        if (days == 0) {
            return "Closes today";
        }
        if (days == -1) {
            return "Closed yesterday";
        }
        return "Closed " + (-days) + " days ago";
    }
}
