package com.newsisrael.service;

import com.newsisrael.model.NewsArticle;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AiOpinionService {
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd.MM.yyyy");
    private static final Pattern OPENAI_CONTENT_PATTERN = Pattern.compile("\\\"content\\\"\\s*:\\s*\\\"((?:\\\\.|[^\\\"])*)\\\"");
    private static final Pattern OLLAMA_RESPONSE_PATTERN = Pattern.compile("\\\"response\\\"\\s*:\\s*\\\"((?:\\\\.|[^\\\"])*)\\\"");

    private final HttpClient httpClient;

    public AiOpinionService(HttpClient httpClient) {
        this.httpClient = httpClient;
    }

    public String buildOpinion(LocalDate date, List<NewsArticle> articles) {
        if (articles == null || articles.isEmpty()) {
            return "AI-мнение недоступно: нет новостей для анализа.";
        }

        String prompt = buildPrompt(date, articles);

        String fromOpenAi = requestOpenAi(prompt);
        if (!fromOpenAi.isBlank()) {
            return fromOpenAi;
        }

        String fromOllama = requestOllama(prompt);
        if (!fromOllama.isBlank()) {
            return fromOllama;
        }

        return fallbackOpinion(date, articles);
    }

    private String buildPrompt(LocalDate date, List<NewsArticle> articles) {
        StringBuilder sb = new StringBuilder();
        sb.append("Дата: ").append(DATE_FORMAT.format(date)).append(".\\n");
        sb.append("Ниже заголовки и краткие описания новостей про Израиль за день. ");
        sb.append("Проанализируй и дай свое мнение об обстановке в Израиле на сегодня. ");
        sb.append("Ответ только на русском, 3-5 предложений, без списков и без дисклеймеров.\\n\\n");

        int max = Math.min(articles.size(), 25);
        for (int i = 0; i < max; i++) {
            NewsArticle article = articles.get(i);
            sb.append(i + 1)
                    .append(") Заголовок: ")
                    .append(trim(article.title(), 220))
                    .append("; Описание: ")
                    .append(trim(article.description(), 260))
                    .append("\\n");
        }
        return sb.toString();
    }

    private String requestOpenAi(String prompt) {
        String apiKey = System.getenv("OPENAI_API_KEY");
        if (apiKey == null || apiKey.isBlank()) {
            return "";
        }

        String model = getenvOrDefault("OPENAI_MODEL", "gpt-4o-mini");
        String baseUrl = getenvOrDefault("OPENAI_BASE_URL", "https://api.openai.com/v1");
        String url = baseUrl.endsWith("/") ? baseUrl + "chat/completions" : baseUrl + "/chat/completions";

        String body = "{" +
                "\\\"model\\\":\\\"" + jsonEscape(model) + "\\\"," +
                "\\\"temperature\\\":0.2," +
                "\\\"messages\\\":[{" +
                "\\\"role\\\":\\\"user\\\"," +
                "\\\"content\\\":\\\"" + jsonEscape(prompt) + "\\\"" +
                "}]" +
                "}";

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                return "";
            }
            return parseFirstStringByPattern(response.body(), OPENAI_CONTENT_PATTERN);
        } catch (IOException | InterruptedException e) {
            return "";
        }
    }

    private String requestOllama(String prompt) {
        String url = getenvOrDefault("OLLAMA_URL", "http://localhost:11434/api/generate");
        String model = getenvOrDefault("OLLAMA_MODEL", "llama3.1");

        String body = "{" +
                "\\\"model\\\":\\\"" + jsonEscape(model) + "\\\"," +
                "\\\"prompt\\\":\\\"" + jsonEscape(prompt) + "\\\"," +
                "\\\"stream\\\":false" +
                "}";

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                return "";
            }
            return parseFirstStringByPattern(response.body(), OLLAMA_RESPONSE_PATTERN);
        } catch (IOException | InterruptedException e) {
            return "";
        }
    }

    private String parseFirstStringByPattern(String json, Pattern pattern) {
        Matcher matcher = pattern.matcher(json == null ? "" : json);
        if (!matcher.find()) {
            return "";
        }
        return unescapeJson(matcher.group(1)).trim();
    }

    private String fallbackOpinion(LocalDate date, List<NewsArticle> articles) {
        int total = articles.size();
        long escalationSignals = articles.stream()
                .map(a -> (a.title() + " " + a.description()).toLowerCase(Locale.ROOT))
                .filter(text -> text.contains("удар")
                        || text.contains("атака")
                        || text.contains("обстрел")
                        || text.contains("конфликт")
                        || text.contains("эвакуац"))
                .count();

        if (escalationSignals > 0) {
            return "На " + DATE_FORMAT.format(date)
                    + " информационная повестка по Израилю выглядит напряженной: значимая часть сообщений касается вопросов безопасности и эскалационных рисков. "
                    + "При этом поток новостей неоднородный и отражает сразу несколько параллельных сюжетов во внутренней и внешней политике. "
                    + "Общая картина дня: высокий уровень неопределенности, за развитием событий важно следить в динамике.";
        }

        return "На " + DATE_FORMAT.format(date)
                + " новостной фон по Израилю выглядит смешанным и многослойным: в центре внимания сразу несколько тем без полной доминации одной повестки. "
                + "По " + total + " публикациям заметно, что ситуация развивается поступательно, с регулярными обновлениями по политике, безопасности и международной реакции. "
                + "Общий вывод: обстановка остается динамичной и требует постоянного мониторинга новых сообщений.";
    }

    private String trim(String text, int maxLen) {
        if (text == null) {
            return "";
        }
        String normalized = text.replaceAll("\\s+", " ").trim();
        if (normalized.length() <= maxLen) {
            return normalized;
        }
        return normalized.substring(0, maxLen) + "...";
    }

    private String jsonEscape(String value) {
        if (value == null) {
            return "";
        }
        return value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    private String unescapeJson(String value) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (c == '\\' && i + 1 < value.length()) {
                char n = value.charAt(++i);
                switch (n) {
                    case '"' -> sb.append('"');
                    case '\\' -> sb.append('\\');
                    case '/' -> sb.append('/');
                    case 'b' -> sb.append('\b');
                    case 'f' -> sb.append('\f');
                    case 'n' -> sb.append('\n');
                    case 'r' -> sb.append('\r');
                    case 't' -> sb.append('\t');
                    case 'u' -> {
                        if (i + 4 < value.length()) {
                            String hex = value.substring(i + 1, i + 5);
                            try {
                                sb.append((char) Integer.parseInt(hex, 16));
                                i += 4;
                            } catch (NumberFormatException ex) {
                                sb.append('u').append(hex);
                                i += 4;
                            }
                        } else {
                            sb.append('u');
                        }
                    }
                    default -> sb.append(n);
                }
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    private String getenvOrDefault(String name, String defaultValue) {
        String value = System.getenv(name);
        return (value == null || value.isBlank()) ? defaultValue : value;
    }
}
