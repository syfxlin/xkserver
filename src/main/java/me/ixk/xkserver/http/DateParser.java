/*
 * Copyright (c) 2020, Otstar Lin (syfxlin@gmail.com). All Rights Reserved.
 *
 */

package me.ixk.xkserver.http;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

/**
 * DateParser
 *
 * @author Otstar Lin
 * @date 2020/11/15 下午 7:53
 */
public class DateParser {
    private static final TimeZone GMT = TimeZone.getTimeZone("GMT");

    static {
        GMT.setID("GMT");
    }

    static final String[] DATE_RECEIVE_FMT = {
        "EEE, dd MMM yyyy HH:mm:ss zzz",
        "EEE, dd-MMM-yy HH:mm:ss",
        "EEE MMM dd HH:mm:ss yyyy",
        "EEE, dd MMM yyyy HH:mm:ss",
        "EEE dd MMM yyyy HH:mm:ss zzz",
        "EEE dd MMM yyyy HH:mm:ss",
        "EEE MMM dd yyyy HH:mm:ss zzz",
        "EEE MMM dd yyyy HH:mm:ss",
        "EEE MMM-dd-yyyy HH:mm:ss zzz",
        "EEE MMM-dd-yyyy HH:mm:ss",
        "dd MMM yyyy HH:mm:ss zzz",
        "dd MMM yyyy HH:mm:ss",
        "dd-MMM-yy HH:mm:ss zzz",
        "dd-MMM-yy HH:mm:ss",
        "MMM dd HH:mm:ss yyyy zzz",
        "MMM dd HH:mm:ss yyyy",
        "EEE MMM dd HH:mm:ss yyyy zzz",
        "EEE, MMM dd HH:mm:ss yyyy zzz",
        "EEE, MMM dd HH:mm:ss yyyy",
        "EEE, dd-MMM-yy HH:mm:ss zzz",
        "EEE dd-MMM-yy HH:mm:ss zzz",
        "EEE dd-MMM-yy HH:mm:ss",
    };

    public static long parseDate(String date) {
        return DATE_PARSER.get().parse(date);
    }

    private static final ThreadLocal<DateParser> DATE_PARSER = new ThreadLocal<DateParser>() {

        @Override
        protected DateParser initialValue() {
            return new DateParser();
        }
    };

    final SimpleDateFormat[] dateReceive = new SimpleDateFormat[DATE_RECEIVE_FMT.length];

    private long parse(final String dateVal) {
        for (int i = 0; i < dateReceive.length; i++) {
            if (dateReceive[i] == null) {
                dateReceive[i] =
                    new SimpleDateFormat(DATE_RECEIVE_FMT[i], Locale.US);
                dateReceive[i].setTimeZone(GMT);
            }

            try {
                Date date = (Date) dateReceive[i].parseObject(dateVal);
                return date.getTime();
            } catch (Exception ignored) {}
        }

        if (dateVal.endsWith(" GMT")) {
            final String val = dateVal.substring(0, dateVal.length() - 4);

            for (SimpleDateFormat element : dateReceive) {
                try {
                    Date date = (Date) element.parseObject(val);
                    return date.getTime();
                } catch (Exception ignored) {}
            }
        }
        return -1;
    }
}
