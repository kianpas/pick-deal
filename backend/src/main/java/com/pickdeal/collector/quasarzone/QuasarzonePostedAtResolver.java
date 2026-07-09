package com.pickdeal.collector.quasarzone;

import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 목록의 게시 시각 표기("30분 전", "5시간 전", "07-08")를 기준 시각(now) 기준의
 * 근사 절대 시각으로 변환한다. 해석 불가한 표기는 null (호출부가 수집 시각으로 대체).
 */
public class QuasarzonePostedAtResolver {

    private static final Pattern MINUTES_AGO = Pattern.compile("^(\\d+)분 전$");
    private static final Pattern HOURS_AGO = Pattern.compile("^(\\d+)시간 전$");
    private static final Pattern MONTH_DAY = Pattern.compile("^(\\d{2})-(\\d{2})$");

    public OffsetDateTime resolve(String text, OffsetDateTime now) {
        if (text == null) {
            return null;
        }
        String trimmed = text.trim();

        Matcher minutes = MINUTES_AGO.matcher(trimmed);
        if (minutes.matches()) {
            return now.minusMinutes(Long.parseLong(minutes.group(1)));
        }
        Matcher hours = HOURS_AGO.matcher(trimmed);
        if (hours.matches()) {
            return now.minusHours(Long.parseLong(hours.group(1)));
        }
        Matcher monthDay = MONTH_DAY.matcher(trimmed);
        if (monthDay.matches()) {
            OffsetDateTime resolved = now.withMonth(Integer.parseInt(monthDay.group(1)))
                    .withDayOfMonth(Integer.parseInt(monthDay.group(2)))
                    .truncatedTo(ChronoUnit.DAYS);
            // 연도 표기가 없어 기준 연도로 해석하되, 미래가 되면 작년 게시글이다
            return resolved.isAfter(now) ? resolved.minusYears(1) : resolved;
        }
        return null;
    }
}
