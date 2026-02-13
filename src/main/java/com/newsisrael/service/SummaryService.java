package com.newsisrael.service;

import com.newsisrael.model.NewsArticle;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class SummaryService {
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

    public String buildSummary(List<NewsArticle> articles) {
        if (articles.isEmpty()) {
            return "За выбранный день новостей по Израилю не найдено.";
        }

        List<String> lines = new ArrayList<>();
        lines.add("Найдено публикаций: " + articles.size() + ".");

        List<NewsArticle> top = articles.stream().limit(5).toList();
        lines.add("Ключевые заголовки:");
        for (int i = 0; i < top.size(); i++) {
            lines.add((i + 1) + ". " + top.get(i).title());
        }

        List<String> topics = extractTopics(articles);
        if (!topics.isEmpty()) {
            lines.add("");
            lines.add("Основные темы дня: " + String.join(", ", topics) + ".");
        }

        lines.add("");
        lines.add("Краткий вывод:");
        lines.add(buildConclusion(articles, topics));

        return String.join("\n", lines);
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

    private String buildConclusion(List<NewsArticle> articles, List<String> topics) {
        List<String> summaryTopics = topics.stream().limit(3).toList();

        Map<String, Long> sourceCounts = articles.stream()
                .collect(Collectors.groupingBy(NewsArticle::source, Collectors.counting()));
        List<String> topSources = sourceCounts.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue(Comparator.reverseOrder()))
                .limit(2)
                .map(Map.Entry::getKey)
                .toList();

        String firstSentence;
        if (summaryTopics.isEmpty()) {
            firstSentence = "По публикациям за этот день повестка остается динамичной, без явного доминирования одной темы.";
        } else {
            firstSentence = "По публикациям за этот день в центре внимания: " + String.join(", ", summaryTopics) + ".";
        }

        String secondSentence;
        if (topSources.isEmpty()) {
            secondSentence = "Информация поступает из разных источников, поэтому картину важно отслеживать в обновлениях.";
        } else {
            secondSentence = "Наибольшее число сообщений пришло от " + String.join(" и ", topSources) + ".";
        }

        String thirdSentence = "Ситуация развивается в течение дня, поэтому полезно периодически обновлять ленту.";
        return firstSentence + " " + secondSentence + " " + thirdSentence;
    }
}
