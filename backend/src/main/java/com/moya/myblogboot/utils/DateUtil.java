package com.moya.myblogboot.utils;


import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

public class DateUtil {
    private final static String DATE_PATTERN = "yyyy-MM-dd";
    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    public static String getToday() {
        return ZonedDateTime.now(KST).format(DateTimeFormatter.ofPattern(DATE_PATTERN));
    }

    public static String getPreviousDay(String date) {
        LocalDate inputDate = LocalDate.parse(date, DateTimeFormatter.ofPattern(DATE_PATTERN));
        return inputDate.minusDays(1).format(DateTimeFormatter.ofPattern(DATE_PATTERN));
    }

    public static String getTodayAndTime(){
        return ZonedDateTime.now(KST).format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }
}
