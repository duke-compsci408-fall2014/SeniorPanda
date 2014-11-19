package com.bmh.ms101;

import android.view.View;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

/**
 * Represents an item in the log in the log tab
 */
public class LogItem implements Comparable<LogItem> {

    public static final int MEDS_TAKEN_TYPE = 1;
    public static final int SYMPTOMS_TYPE = 2;

    private long date;
    private String sDate;
    private String title;
    private String text;
    private int type;
    private String name;


    public LogItem(long date, String title, String text) {
        this.date = date;
        sDate = toStringDate(this.date);
        this.title = title;
        this.text = text;
    }

    public LogItem(long date, String title, String text, int type, String name) {
        this.date = date;
        sDate = toStringDate(this.date);
        this.title = title;
        this.text = text;
        this.type = type;
        this.name = name;
    }

    /**
     * Takes a long representation of a date and returns a string in format MM/DD/YYYY
     * @param date long time in milliseconds
     * @return Date string
     */
    private String toStringDate(long date) {
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(date);
        return new SimpleDateFormat("MM'/'dd'/'yyyy").format(c.getTime());
    }

    /*private String toStringDate(long date) {
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(date);
        return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(c.getTime());
    }*/

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

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
    /*@Override
    public int compareTo(LogItem other) {
        Long otherDate = other.getDate();
        // Set up so that when sorted, items end up with newest first.
        return otherDate.compareTo(date);
    }*/

    /*@Override
    public int compareTo(LogItem other) {
        Long otherDate = other.getDate();
        // Set up so that when sorted, items end up with newest first.
        int compare = otherDate.compareTo(date);
        System.out.println("compare " + compare);
        return compare;
    }*/

/*    @Override
    public int compareTo(LogItem other) {
        Date otherDateObj = other.getDateObj();
        // Set up so that when sorted, items end up with newest first.
        int compare = otherDateObj.compareTo(dateObj);
        System.out.println("===========================");
        System.out.println("Date " + dateObj);
        System.out.println("OtherDate " + otherDateObj);
        System.out.println("compare " + compare);
        System.out.println("this text " + text);
        System.out.println("other text " + other.getText());
        return compare;
    }*/

    /*@Override
    public int compareTo(LogItem other) {
        int compare = 0;
        Long otherDate = other.getDate();
        // Set up so that when sorted, items end up with newest first.
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(date);
        int currDayOfYear = c.get(Calendar.DAY_OF_YEAR);
        c.setTimeInMillis(otherDate);
        int otherDayOfYear = c.get(Calendar.DAY_OF_YEAR);
        if (currDayOfYear != otherDayOfYear) {
            compare = otherDayOfYear - currDayOfYear;
        } else {
            compare = type - other.getType();
        }
        System.out.println("compare " + compare);
        return compare;
    }*/

    @Override
    public int compareTo(LogItem other) {
        int compare = 0;
        Long otherDate = other.getDate();
        // Set up so that when sorted, items end up with newest first.
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(date);
        int currDayOfYear = c.get(Calendar.DAY_OF_YEAR);
        c.setTimeInMillis(otherDate);
        int otherDayOfYear = c.get(Calendar.DAY_OF_YEAR);
        if (currDayOfYear != otherDayOfYear) {
            compare = otherDayOfYear - currDayOfYear;
        } else if (type != other.getType()) {
            compare = type - other.getType();
        } else {
            compare = name.compareToIgnoreCase(other.getName());
        }
        System.out.println("compare " + compare);
        return compare;
    }



    /*Calendar c = Calendar.getInstance();
    c.setTimeInMillis(currItemDate);
    int currDayOfYear = c.get(Calendar.DAY_OF_YEAR);
    c.setTimeInMillis(prevItemDate);
    int prevDayOfYear = c.get(Calendar.DAY_OF_YEAR);
    if (currDayOfYear != prevDayOfYear) {
        viewHolder.date.setVisibility(View.VISIBLE);
    } else {
        // Otherwise hide the date header
        viewHolder.date.setVisibility(View.GONE);
    }*/
}
