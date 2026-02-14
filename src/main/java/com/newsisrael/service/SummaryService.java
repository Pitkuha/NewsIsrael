package com.newsisrael.service;

import com.newsisrael.i18n.AppLanguage;
import com.newsisrael.model.NewsArticle;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class SummaryService {
    private final AiOpinionService aiOpinionService;

    private static final Set<String> STOP_WORDS = Set.of(
            "the", "and", "for", "with", "from", "that", "this", "into", "about", "after", "before",
            "over", "under", "news", "israel", "israeli", "said", "says", "will", "have", "has", "had",
            "its", "their", "they", "were", "was", "are", "been", "also", "not", "than", "but", "you",
            "your", "his", "her", "she", "him", "our", "out", "who", "what", "when", "where", "why",
            "how", "new", "more", "amid", "during",
            "это", "этот", "сегодня", "израиль", "израиля", "израиле", "израильский", "новости",
            "также", "который", "которая", "которые", "после", "перед", "среди", "вокруг", "через",
            "между", "сообщил", "сообщает", "заявил", "заявила", "более", "менее", "свои", "своих",
            "была", "были", "быть", "этого", "этой", "этом"
    );

    public SummaryService() {
        this.aiOpinionService = new AiOpinionService(java.net.http.HttpClient.newHttpClient());
    }

    public String buildSummary(List<NewsArticle> articles, LocalDate date, AppLanguage language) {
        if (articles.isEmpty()) {
            return noNewsText(language);
        }

        List<String> lines = new ArrayList<>();
        lines.add(foundPublicationsText(language, articles.size()));

        List<NewsArticle> top = articles.stream().limit(5).toList();
        lines.add(headlinesTitle(language));
        for (int i = 0; i < top.size(); i++) {
            lines.add((i + 1) + ". " + top.get(i).title());
        }

        List<String> topics = extractTopics(articles);
        if (!topics.isEmpty()) {
            lines.add("");
            lines.add(topicsTitle(language) + String.join(", ", topics) + ".");
        }

        lines.add("");
        lines.add(conclusionTitle(language));
        lines.add(buildConclusion(articles, topics, language));

        lines.add("");
        lines.add(aiOpinionTitle(language));
        lines.add(aiOpinionService.buildOpinion(date, articles, language));

        return String.join("\n", lines);
    }

    public String buildSummary(List<NewsArticle> articles, LocalDate date) {
        return buildSummary(articles, date, AppLanguage.defaultLanguage());
    }

    public String buildSummary(List<NewsArticle> articles) {
        return buildSummary(articles, LocalDate.now(), AppLanguage.defaultLanguage());
    }

    private List<String> extractTopics(List<NewsArticle> articles) {
        Map<String, Integer> freq = new HashMap<>();
        for (NewsArticle article : articles) {
            String text = (article.title() + " " + article.description()).toLowerCase(Locale.ROOT);
            String[] words = text.split("[^\\p{L}]+");
            for (String raw : words) {
                if (raw.isBlank() || raw.length() < 4 || STOP_WORDS.contains(raw)) {
                    continue;
                }
                freq.merge(raw, 1, Integer::sum);
            }
        }

        return freq.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue(Comparator.reverseOrder()))
                .limit(6)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
    }

    private String buildConclusion(List<NewsArticle> articles, List<String> topics, AppLanguage language) {
        List<String> summaryTopics = topics.stream().limit(3).toList();

        Map<String, Long> sourceCounts = articles.stream()
                .collect(Collectors.groupingBy(NewsArticle::source, Collectors.counting()));
        List<String> topSources = sourceCounts.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue(Comparator.reverseOrder()))
                .limit(2)
                .map(Map.Entry::getKey)
                .toList();

        return switch (language) {
            case ENGLISH -> buildConclusionEn(summaryTopics, topSources);
            case HEBREW -> buildConclusionHe(summaryTopics, topSources);
            case ARABIC -> buildConclusionAr(summaryTopics, topSources);
            case RUSSIAN -> buildConclusionRu(summaryTopics, topSources);
        };
    }

    private String buildConclusionRu(List<String> topics, List<String> sources) {
        String firstSentence = topics.isEmpty()
                ? "По публикациям за этот день повестка остается динамичной, без явного доминирования одной темы."
                : "По публикациям за этот день в центре внимания: " + String.join(", ", topics) + ".";

        String secondSentence = sources.isEmpty()
                ? "Информация поступает из разных источников, поэтому картину важно отслеживать в обновлениях."
                : "Наибольшее число сообщений пришло от " + String.join(" и ", sources) + ".";

        return firstSentence + " " + secondSentence + " Ситуация развивается в течение дня, поэтому полезно периодически обновлять ленту.";
    }

    private String buildConclusionEn(List<String> topics, List<String> sources) {
        String firstSentence = topics.isEmpty()
                ? "The daily agenda remains dynamic with no single dominant storyline."
                : "The main focus areas today are: " + String.join(", ", topics) + ".";

        String secondSentence = sources.isEmpty()
                ? "Coverage comes from multiple outlets, so updates should be tracked continuously."
                : "The largest volume of updates came from " + String.join(" and ", sources) + ".";

        return firstSentence + " " + secondSentence + " Overall, the situation is evolving throughout the day.";
    }

    private String buildConclusionHe(List<String> topics, List<String> sources) {
        String firstSentence = topics.isEmpty()
                ? "סדר היום החדשותי דינמי, ללא נושא אחד דומיננטי באופן מובהק."
                : "הנושאים המרכזיים היום הם: " + String.join(", ", topics) + ".";

        String secondSentence = sources.isEmpty()
                ? "המידע מגיע ממקורות שונים ולכן חשוב לעקוב אחר עדכונים באופן רציף."
                : "עיקר הדיווחים הגיעו מ-" + String.join(" ו-", sources) + ".";

        return firstSentence + " " + secondSentence + " התמונה הכללית ממשיכה להתפתח במהלך היום.";
    }

    private String buildConclusionAr(List<String> topics, List<String> sources) {
        String firstSentence = topics.isEmpty()
                ? "الأجندة الإخبارية اليومية متحركة ولا يوجد موضوع واحد مهيمن بشكل واضح."
                : "أبرز محاور اليوم هي: " + String.join(", ", topics) + ".";

        String secondSentence = sources.isEmpty()
                ? "تأتي المعلومات من مصادر متعددة، لذلك من المهم متابعة التحديثات باستمرار."
                : "أكبر حجم من التغطية جاء من " + String.join(" و", sources) + ".";

        return firstSentence + " " + secondSentence + " بشكل عام، الصورة تتطور على مدار اليوم.";
    }

    private String noNewsText(AppLanguage language) {
        return switch (language) {
            case RUSSIAN -> "За выбранный день новостей по Израилю не найдено.";
            case ENGLISH -> "No Israel-related news found for the selected day.";
            case HEBREW -> "לא נמצאו חדשות על ישראל עבור התאריך שנבחר.";
            case ARABIC -> "لم يتم العثور على أخبار عن إسرائيل في اليوم المحدد.";
        };
    }

    private String foundPublicationsText(AppLanguage language, int count) {
        return switch (language) {
            case RUSSIAN -> "Найдено публикаций: " + count + ".";
            case ENGLISH -> "Articles found: " + count + ".";
            case HEBREW -> "נמצאו כתבות: " + count + ".";
            case ARABIC -> "عدد الأخبار: " + count + ".";
        };
    }

    private String headlinesTitle(AppLanguage language) {
        return switch (language) {
            case RUSSIAN -> "Ключевые заголовки:";
            case ENGLISH -> "Top headlines:";
            case HEBREW -> "כותרות מרכזיות:";
            case ARABIC -> "أبرز العناوين:";
        };
    }

    private String topicsTitle(AppLanguage language) {
        return switch (language) {
            case RUSSIAN -> "Основные темы дня: ";
            case ENGLISH -> "Main topics of the day: ";
            case HEBREW -> "הנושאים המרכזיים של היום: ";
            case ARABIC -> "الموضوعات الرئيسية لليوم: ";
        };
    }

    private String conclusionTitle(AppLanguage language) {
        return switch (language) {
            case RUSSIAN -> "Краткий вывод:";
            case ENGLISH -> "Brief conclusion:";
            case HEBREW -> "מסקנה קצרה:";
            case ARABIC -> "خلاصة قصيرة:";
        };
    }

    private String aiOpinionTitle(AppLanguage language) {
        return switch (language) {
            case RUSSIAN -> "AI-мнение:";
            case ENGLISH -> "AI opinion:";
            case HEBREW -> "חוות דעת AI:";
            case ARABIC -> "رأي الذكاء الاصطناعي:";
        };
    }
}
