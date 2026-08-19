package com.pickdeal.collector.ruliweb;

import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 목록의 게시 시각 표기("19:19", "2026.08.14")를 절대 시각으로 바꾼다.
 * 해석 불가한 표기는 null (호출부가 수집 시각으로 대체).
 */
public class RuliwebPostedAtResolver {

    private static final Pattern TIME_OF_DAY = Pattern.compile("^(\\d{1,2}):(\\d{2})$");
    private static final Pattern ABSOLUTE_DATE = Pattern.compile("^(\\d{4})\\.(\\d{2})\\.(\\d{2})$");

    public OffsetDateTime resolve(String text, OffsetDateTime now) {
        if (text == null) {
            return null;
        }
        String trimmed = text.trim();

        Matcher timeOfDay = TIME_OF_DAY.matcher(trimmed);
        if (timeOfDay.matches()) {
            OffsetDateTime resolved = now.truncatedTo(ChronoUnit.DAYS)
                    .plusHours(Long.parseLong(timeOfDay.group(1)))
                    .plusMinutes(Long.parseLong(timeOfDay.group(2)));
            // 시각만 주어지므로 오늘로 보되, 기준 시각보다 미래면 자정을 넘긴 어제 글이다
            return resolved.isAfter(now) ? resolved.minusDays(1) : resolved;
        }

        Matcher date = ABSOLUTE_DATE.matcher(trimmed);
        if (date.matches()) {
            return now.truncatedTo(ChronoUnit.DAYS)
                    .withYear(Integer.parseInt(date.group(1)))
                    .withMonth(Integer.parseInt(date.group(2)))
                    .withDayOfMonth(Integer.parseInt(date.group(3)));
        }
        return null;
    }
}
