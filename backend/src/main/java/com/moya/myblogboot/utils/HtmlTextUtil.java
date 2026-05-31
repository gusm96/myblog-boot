package com.moya.myblogboot.utils;

import org.jsoup.Jsoup;

public final class HtmlTextUtil {
    private HtmlTextUtil() {
    }

    public static String toPlainText(String html) {
        if (html == null || html.isBlank()) {
            return "";
        }
        return Jsoup.parse(html).text().trim();
    }

    public static String summarize(String html, int maxLength) {
        String text = toPlainText(html);
        if (text.length() <= maxLength) {
            return text;
        }
        return text.substring(0, maxLength).trim() + "...";
    }
}
