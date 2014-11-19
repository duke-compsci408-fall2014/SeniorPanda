package com.bmh.ms101;

import java.text.SimpleDateFormat;
import java.util.Calendar;

/**
 * Represents an item in the log in the log tab
 */
public class LogItem implements Comparable<LogItem> {

    private long date;
    private String sDate;
    private String title;
    private String text;


    public LogItem(long date, String title, String text) {
        this.date = date;
        sDate = toStringDate(this.date);
        this.title = title;
        this.text = text;
    }

    /**
     * Takes a long representation of a date and returns a string in format MM/DD/YYYY
     * @param date long time in milliseconds
     * @return Date string
     */
   /* private String toStringDate(long date) {
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(date);
        return new SimpleDateFormat("MM'/'dd'/'yyyy").format(c.getTime());
    }*/

    private String toStringDate(long date) {
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(date);
        return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(c.getTime());
    }

    public long getDate() {
        return date;
    }

    public String getDateString() {
        return sDate;
    }

    public String getTitle() {
        return title;
    }

    public String getText() {
        return text;
    }

    @Override
    public int compareTo(LogItem other) {
        Long otherDate = other.getDate();
        // Set up so that when sorted, items end up with newest first.
        return otherDate.compareTo(date);
    }
}
