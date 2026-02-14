package com.newsisrael.service;

import com.newsisrael.i18n.AppLanguage;
import com.newsisrael.model.NewsArticle;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.IOException;
import java.io.StringReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class NewsService {
    private static final List<FeedSource> FEEDS = List.of(
            new FeedSource("Google News", "https://news.google.com/rss/search?q=Israel&hl=en-US&gl=US&ceid=US:en"),
            new FeedSource("BBC World Middle East", "https://feeds.bbci.co.uk/news/world/middle_east/rss.xml"),
            new FeedSource("NYT World", "https://rss.nytimes.com/services/xml/rss/nyt/World.xml")
    );

    private final HttpClient httpClient;
    private final TranslationService translationService;

    public NewsService() {
        this.httpClient = HttpClient.newHttpClient();
        this.translationService = new TranslationService(httpClient);
    }

    public List<NewsArticle> loadNewsForDate(LocalDate date) throws IOException, InterruptedException {
        return loadNewsForDate(date, Integer.MAX_VALUE, AppLanguage.defaultLanguage());
    }

    public List<NewsArticle> loadNewsForDate(LocalDate date, int limit) throws IOException, InterruptedException {
        return loadNewsForDate(date, limit, AppLanguage.defaultLanguage());
    }

    public List<NewsArticle> loadNewsForDate(LocalDate date, int limit, AppLanguage language) throws IOException, InterruptedException {
        List<NewsArticle> result = new ArrayList<>();

        for (FeedSource feed : FEEDS) {
            try {
                String xml = download(feed.url());
                result.addAll(parseFeed(xml, feed.name(), date));
            } catch (Exception ignored) {
                // Игнорируем проблемы отдельной ленты и продолжаем с остальными.
            }
        }

        Map<String, NewsArticle> unique = new LinkedHashMap<>();
        for (NewsArticle article : result) {
            String key = normalize(article.title()) + "|" + normalize(article.url());
            unique.putIfAbsent(key, article);
        }

        List<NewsArticle> deduplicated = new ArrayList<>(unique.values());
        deduplicated.sort(Comparator.comparing(NewsArticle::publishedAt).reversed());
        int safeLimit = Math.max(1, limit);
        List<NewsArticle> limited = deduplicated.size() > safeLimit
                ? deduplicated.subList(0, safeLimit)
                : deduplicated;
        return translateArticles(limited, language);
    }

    private String download(String url) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("User-Agent", "NewsIsraelApp/1.0")
                .GET()
                .build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            throw new IOException("Feed unavailable: HTTP " + response.statusCode());
        }
        return response.body();
    }

    private List<NewsArticle> parseFeed(String xml, String fallbackSource, LocalDate date) throws Exception {
        List<NewsArticle> list = new ArrayList<>();

        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
        factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
        factory.setXIncludeAware(false);
        factory.setExpandEntityReferences(false);

        Document doc = factory.newDocumentBuilder().parse(new InputSource(new StringReader(xml)));
        doc.getDocumentElement().normalize();

        NodeList items = doc.getElementsByTagName("item");
        if (items.getLength() > 0) {
            for (int i = 0; i < items.getLength(); i++) {
                Node node = items.item(i);
                if (node instanceof Element item) {
                    NewsArticle article = fromRssItem(item, fallbackSource);
                    if (article != null && isForDate(article, date) && isIsraelRelated(article)) {
                        list.add(article);
                    }
                }
            }
            return list;
        }

        NodeList entries = doc.getElementsByTagName("entry");
        for (int i = 0; i < entries.getLength(); i++) {
            Node node = entries.item(i);
            if (node instanceof Element entry) {
                NewsArticle article = fromAtomEntry(entry, fallbackSource);
                if (article != null && isForDate(article, date) && isIsraelRelated(article)) {
                    list.add(article);
                }
            }
        }

        return list;
    }

    private NewsArticle fromRssItem(Element item, String fallbackSource) {
        String title = text(item, "title");
        String description = stripHtml(text(item, "description"));
        String url = text(item, "link");
        String source = text(item, "source");
        String pubDate = text(item, "pubDate");

        if (title.isBlank()) {
            return null;
        }

        if (source.isBlank()) {
            source = fallbackSource;
        }

        Instant publishedAt = parseDate(pubDate);
        if (publishedAt == null) {
            return null;
        }

        return new NewsArticle(title.trim(), description.trim(), source.trim(), publishedAt, url.trim());
    }

    private NewsArticle fromAtomEntry(Element entry, String fallbackSource) {
        String title = text(entry, "title");
        String description = stripHtml(text(entry, "summary"));
        if (description.isBlank()) {
            description = stripHtml(text(entry, "content"));
        }

        String url = "";
        NodeList links = entry.getElementsByTagName("link");
        for (int i = 0; i < links.getLength(); i++) {
            Node node = links.item(i);
            if (node instanceof Element link) {
                String href = link.getAttribute("href");
                if (!href.isBlank()) {
                    url = href;
                    break;
                }
            }
        }

        String updated = text(entry, "updated");
        if (updated.isBlank()) {
            updated = text(entry, "published");
        }

        if (title.isBlank()) {
            return null;
        }

        Instant publishedAt = parseDate(updated);
        if (publishedAt == null) {
            return null;
        }

        return new NewsArticle(title.trim(), description.trim(), fallbackSource, publishedAt, url.trim());
    }

    private boolean isForDate(NewsArticle article, LocalDate date) {
        LocalDate publishedDate = article.publishedAt().atZone(ZoneId.systemDefault()).toLocalDate();
        return publishedDate.equals(date);
    }

    private boolean isIsraelRelated(NewsArticle article) {
        String text = (article.title() + " " + article.description()).toLowerCase(Locale.ROOT);
        return text.contains("israel")
                || text.contains("israeli")
                || text.contains("jerusalem")
                || text.contains("gaza")
                || text.contains("west bank")
                || text.contains("tel aviv");
    }

    private Instant parseDate(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }

        try {
            return Instant.parse(raw.trim());
        } catch (DateTimeParseException ignored) {
        }

        try {
            return ZonedDateTime.parse(raw.trim(), DateTimeFormatter.RFC_1123_DATE_TIME).toInstant();
        } catch (DateTimeParseException ignored) {
        }

        try {
            return ZonedDateTime.parse(raw.trim(), DateTimeFormatter.ISO_OFFSET_DATE_TIME).toInstant();
        } catch (DateTimeParseException ignored) {
        }

        return null;
    }

    private String text(Element element, String tagName) {
        NodeList nodes = element.getElementsByTagName(tagName);
        if (nodes.getLength() == 0) {
            return "";
        }
        String value = nodes.item(0).getTextContent();
        return value == null ? "" : value;
    }

    private String stripHtml(String input) {
        if (input == null || input.isBlank()) {
            return "";
        }
        return input.replaceAll("<[^>]*>", " ")
                .replace("&nbsp;", " ")
                .replace("&amp;", "&")
                .replace("&quot;", "\"")
                .replace("&#39;", "'")
                .replaceAll("\\s+", " ")
                .trim();
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    private List<NewsArticle> translateArticles(List<NewsArticle> articles, AppLanguage language) {
        List<NewsArticle> translated = new ArrayList<>(articles.size());
        for (NewsArticle article : articles) {
            String titleRu = translationService.translate(article.title(), language);
            String descriptionRu = translationService.translate(article.description(), language);
            translated.add(new NewsArticle(
                    titleRu.isBlank() ? article.title() : titleRu,
                    descriptionRu.isBlank() ? article.description() : descriptionRu,
                    article.source(),
                    article.publishedAt(),
                    article.url()
            ));
        }
        return translated;
    }

    private record FeedSource(String name, String url) {
    }
}
