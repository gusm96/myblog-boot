package com.moya.myblogboot.utils;


import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class DateUtil {
    private final static String DATE_PATTERN = "yyyy-MM-dd";

    public static String getToday() {
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern(DATE_PATTERN));
    }

    public static String getPreviousDay(String date) {
        LocalDateTime inputDate = LocalDate.parse(date, DateTimeFormatter.ofPattern(DATE_PATTERN)).atStartOfDay();
        LocalDateTime previousDay = inputDate.minusDays(1);
        return previousDay.format(DateTimeFormatter.ofPattern(DATE_PATTERN));
    }
}
