package io.github.createmeow.timex_rebirth.calendar;

/**
 * 年份系统：一年按 366 天计算（每年都是闰年）。
 * 月份天数：31,29,31,30,31,30,31,31,30,31,30,31（合计 366）。
 * 世界第 0 天 = 年份 0 的 1 月 1 日。
 */
public class CalendarSystem {
    public static final int DAYS_PER_YEAR = 366;
    public static final int MONTHS = 12;
    public static final int[] MONTH_DAYS = {31, 29, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31};

    public record DateInfo(int year, int month, int dayOfMonth) {}

    public static int daysInMonth(int month) { // month: 1..12
        if (month < 1 || month > MONTHS) return 0;
        return MONTH_DAYS[month - 1];
    }

    public static DateInfo fromDayIndex(long dayIndex) {
        long year = Math.floorDiv(dayIndex, DAYS_PER_YEAR);
        int dayOfYear = (int) Math.floorMod(dayIndex, DAYS_PER_YEAR);
        int month = 0;
        while (month < MONTHS - 1 && dayOfYear >= MONTH_DAYS[month]) {
            dayOfYear -= MONTH_DAYS[month];
            month++;
        }
        return new DateInfo((int) year, month + 1, dayOfYear + 1);
    }

    public static long toDayIndex(int year, int month, int day) {
        long days = (long) year * DAYS_PER_YEAR;
        for (int i = 0; i < month - 1; i++) {
            days += MONTH_DAYS[i];
        }
        days += day - 1;
        return days;
    }
}
