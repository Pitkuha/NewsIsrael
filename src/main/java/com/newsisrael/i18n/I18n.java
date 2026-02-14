package com.newsisrael.i18n;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public final class I18n {
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd.MM.yyyy");

    private I18n() {
    }

    public static String appTitle(AppLanguage lang) {
        return switch (lang) {
            case RUSSIAN -> "Новости Израиля";
            case ENGLISH -> "Israel News";
            case HEBREW -> "חדשות ישראל";
            case ARABIC -> "أخبار إسرائيل";
        };
    }

    public static String dateLabel(AppLanguage lang) {
        return switch (lang) {
            case RUSSIAN -> "Дата:";
            case ENGLISH -> "Date:";
            case HEBREW -> "תאריך:";
            case ARABIC -> "التاريخ:";
        };
    }

    public static String countLabel(AppLanguage lang) {
        return switch (lang) {
            case RUSSIAN -> "Кол-во:";
            case ENGLISH -> "Count:";
            case HEBREW -> "כמות:";
            case ARABIC -> "العدد:";
        };
    }

    public static String languageLabel(AppLanguage lang) {
        return switch (lang) {
            case RUSSIAN -> "Язык:";
            case ENGLISH -> "Language:";
            case HEBREW -> "שפה:";
            case ARABIC -> "اللغة:";
        };
    }

    public static String openDayButton(AppLanguage lang) {
        return switch (lang) {
            case RUSSIAN -> "Открыть день";
            case ENGLISH -> "Open day";
            case HEBREW -> "פתח יום";
            case ARABIC -> "افتح اليوم";
        };
    }

    public static String refreshButton(AppLanguage lang) {
        return switch (lang) {
            case RUSSIAN -> "Обновить";
            case ENGLISH -> "Refresh";
            case HEBREW -> "רענן";
            case ARABIC -> "تحديث";
        };
    }

    public static String statusReady(AppLanguage lang) {
        return switch (lang) {
            case RUSSIAN -> "Готово";
            case ENGLISH -> "Ready";
            case HEBREW -> "מוכן";
            case ARABIC -> "جاهز";
        };
    }

    public static String statusUpdating(AppLanguage lang, LocalDate date, int limit) {
        String dateText = DATE_FORMAT.format(date);
        return switch (lang) {
            case RUSSIAN -> "Обновление: " + dateText + " | лимит: " + limit;
            case ENGLISH -> "Updating: " + dateText + " | limit: " + limit;
            case HEBREW -> "מתעדכן: " + dateText + " | מגבלה: " + limit;
            case ARABIC -> "جارٍ التحديث: " + dateText + " | الحد: " + limit;
        };
    }

    public static String statusUpdated(AppLanguage lang, LocalDate date, int count) {
        String dateText = DATE_FORMAT.format(date);
        return switch (lang) {
            case RUSSIAN -> "Обновлено: " + dateText + " | новостей: " + count;
            case ENGLISH -> "Updated: " + dateText + " | articles: " + count;
            case HEBREW -> "עודכן: " + dateText + " | כתבות: " + count;
            case ARABIC -> "تم التحديث: " + dateText + " | الأخبار: " + count;
        };
    }

    public static String statusError(AppLanguage lang) {
        return switch (lang) {
            case RUSSIAN -> "Ошибка загрузки";
            case ENGLISH -> "Loading error";
            case HEBREW -> "שגיאת טעינה";
            case ARABIC -> "خطأ في التحميل";
        };
    }

    public static String warningTitle(AppLanguage lang) {
        return switch (lang) {
            case RUSSIAN -> "Ошибка";
            case ENGLISH -> "Warning";
            case HEBREW -> "אזהרה";
            case ARABIC -> "تحذير";
        };
    }

    public static String futureDateMessage(AppLanguage lang) {
        return switch (lang) {
            case RUSSIAN -> "Нельзя открыть будущую дату.";
            case ENGLISH -> "You cannot open a future date.";
            case HEBREW -> "לא ניתן לפתוח תאריך עתידי.";
            case ARABIC -> "لا يمكن فتح تاريخ في المستقبل.";
        };
    }

    public static String todayTab(AppLanguage lang) {
        return switch (lang) {
            case RUSSIAN -> "Сегодня";
            case ENGLISH -> "Today";
            case HEBREW -> "היום";
            case ARABIC -> "اليوم";
        };
    }

    public static String panelLoadingText(AppLanguage lang) {
        return switch (lang) {
            case RUSSIAN -> "Идет загрузка...";
            case ENGLISH -> "Loading...";
            case HEBREW -> "טוען...";
            case ARABIC -> "جارٍ التحميل...";
        };
    }

    public static String panelErrorTitle(AppLanguage lang) {
        return switch (lang) {
            case RUSSIAN -> "Ошибка";
            case ENGLISH -> "Error";
            case HEBREW -> "שגיאה";
            case ARABIC -> "خطأ";
        };
    }

    public static String panelNewsAndSummaryTitle(AppLanguage lang) {
        return switch (lang) {
            case RUSSIAN -> "Новости и сводка";
            case ENGLISH -> "News and summary";
            case HEBREW -> "חדשות וסיכום";
            case ARABIC -> "الأخبار والملخص";
        };
    }

    public static String panelNewsSection(AppLanguage lang) {
        return switch (lang) {
            case RUSSIAN -> "Новости";
            case ENGLISH -> "News";
            case HEBREW -> "חדשות";
            case ARABIC -> "الأخبار";
        };
    }

    public static String panelDetailsSection(AppLanguage lang) {
        return switch (lang) {
            case RUSSIAN -> "Подробности";
            case ENGLISH -> "Details";
            case HEBREW -> "פרטים";
            case ARABIC -> "التفاصيل";
        };
    }

    public static String panelLoadFailed(AppLanguage lang) {
        return switch (lang) {
            case RUSSIAN -> "Не удалось загрузить новости.";
            case ENGLISH -> "Failed to load news.";
            case HEBREW -> "טעינת החדשות נכשלה.";
            case ARABIC -> "تعذر تحميل الأخبار.";
        };
    }

    public static String panelNoPublications(AppLanguage lang) {
        return switch (lang) {
            case RUSSIAN -> "Публикаций за этот день не найдено.";
            case ENGLISH -> "No articles found for this day.";
            case HEBREW -> "לא נמצאו כתבות ליום זה.";
            case ARABIC -> "لم يتم العثور على أخبار لهذا اليوم.";
        };
    }

    public static String descriptionMissing(AppLanguage lang) {
        return switch (lang) {
            case RUSSIAN -> "Описание отсутствует.";
            case ENGLISH -> "Description is missing.";
            case HEBREW -> "אין תיאור.";
            case ARABIC -> "لا يوجد وصف.";
        };
    }

    public static String sourceLabel(AppLanguage lang) {
        return switch (lang) {
            case RUSSIAN -> "Источник:";
            case ENGLISH -> "Source:";
            case HEBREW -> "מקור:";
            case ARABIC -> "المصدر:";
        };
    }

    public static String timeLabel(AppLanguage lang) {
        return switch (lang) {
            case RUSSIAN -> "Время:";
            case ENGLISH -> "Time:";
            case HEBREW -> "שעה:";
            case ARABIC -> "الوقت:";
        };
    }

    public static String openOriginal(AppLanguage lang) {
        return switch (lang) {
            case RUSSIAN -> "Открыть оригинал";
            case ENGLISH -> "Open original";
            case HEBREW -> "פתח מקור";
            case ARABIC -> "افتح المصدر";
        };
    }

    public static String loadingForDate(AppLanguage lang, LocalDate date) {
        String dateText = DATE_FORMAT.format(date);
        return switch (lang) {
            case RUSSIAN -> "Загрузка новостей за " + dateText;
            case ENGLISH -> "Loading news for " + dateText;
            case HEBREW -> "טוען חדשות עבור " + dateText;
            case ARABIC -> "جارٍ تحميل الأخبار ليوم " + dateText;
        };
    }

    public static String aiUnavailable(AppLanguage lang) {
        return switch (lang) {
            case RUSSIAN -> "AI-мнение недоступно: нет новостей для анализа.";
            case ENGLISH -> "AI opinion unavailable: there are no articles to analyze.";
            case HEBREW -> "חוות דעת AI אינה זמינה: אין חדשות לניתוח.";
            case ARABIC -> "رأي الذكاء الاصطناعي غير متاح: لا توجد أخبار للتحليل.";
        };
    }
}
