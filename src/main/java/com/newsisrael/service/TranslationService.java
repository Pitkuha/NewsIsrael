package com.newsisrael.service;

import com.newsisrael.i18n.AppLanguage;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class TranslationService {
    private static final String TRANSLATE_URL =
            "https://translate.googleapis.com/translate_a/single?client=gtx&sl=en&tl=%s&dt=t&q=%s";
    private static final int MAX_REQUEST_CHARS = 700;

    private final HttpClient httpClient;
    private final Map<String, String> cache;

    public TranslationService(HttpClient httpClient) {
        this.httpClient = httpClient;
        this.cache = new LinkedHashMap<>(256, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<String, String> eldest) {
                return size() > 1000;
            }
        };
    }

    public String translate(String text, AppLanguage language) {
        if (text == null || text.isBlank()) {
            return "";
        }

        String normalized = text.trim();
        if (language == AppLanguage.ENGLISH || looksLikeTargetLanguage(normalized, language)) {
            return normalized;
        }

        String cacheKey = language.code() + "|" + normalized;
        String cached = cache.get(cacheKey);
        if (cached != null) {
            return cached;
        }

        String requestText = normalized.length() > MAX_REQUEST_CHARS
                ? normalized.substring(0, MAX_REQUEST_CHARS)
                : normalized;

        try {
            String encodedText = URLEncoder.encode(requestText, StandardCharsets.UTF_8);
            String url = TRANSLATE_URL.formatted(language.code(), encodedText);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("User-Agent", "NewsIsraelApp/1.0")
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                return normalized;
            }

            String translated = parseTranslatedText(response.body());
            if (translated.isBlank() || translated.equalsIgnoreCase(normalized)) {
                return normalized;
            }

            cache.put(cacheKey, translated);
            return translated;
        } catch (IOException | InterruptedException e) {
            return normalized;
        }
    }

    public String translateToRussian(String text) {
        return translate(text, AppLanguage.RUSSIAN);
    }

    private String parseTranslatedText(String rawBody) {
        String body = rawBody == null ? "" : rawBody.trim();
        if (body.isBlank() || !body.startsWith("[")) {
            return "";
        }

        String firstElement = extractFirstArrayElement(body);
        if (firstElement.isBlank() || !firstElement.startsWith("[")) {
            return "";
        }

        List<String> chunks = splitTopLevelElements(firstElement);
        StringBuilder merged = new StringBuilder();
        for (String chunk : chunks) {
            String translatedChunk = extractFirstString(chunk);
            if (translatedChunk.isBlank()) {
                continue;
            }
            if (!merged.isEmpty()) {
                merged.append(' ');
            }
            merged.append(translatedChunk);
        }
        return merged.toString().replaceAll("\\s+", " ").trim();
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

    private boolean looksLikeTargetLanguage(String text, AppLanguage language) {
        return switch (language) {
            case RUSSIAN -> hasScriptRatio(text, Character.UnicodeBlock.CYRILLIC, 0.35);
            case HEBREW -> hasScriptRatio(text, Character.UnicodeBlock.HEBREW, 0.25);
            case ARABIC -> hasScriptRatio(text, Character.UnicodeBlock.ARABIC, 0.25);
            case ENGLISH -> true;
        };
    }

    private boolean hasScriptRatio(String text, Character.UnicodeBlock block, double threshold) {
        int scriptCount = 0;
        int letters = 0;
        for (int i = 0; i < text.length(); i++) {
            char ch = text.charAt(i);
            if (Character.isLetter(ch)) {
                letters++;
                if (Character.UnicodeBlock.of(ch) == block) {
                    scriptCount++;
                }
            }
        }
        return letters > 0 && ((double) scriptCount / letters) > threshold;
    }

    private String extractFirstArrayElement(String jsonArray) {
        List<String> elements = splitTopLevelElements(jsonArray);
        return elements.isEmpty() ? "" : elements.get(0);
    }

    private List<String> splitTopLevelElements(String jsonArray) {
        List<String> elements = new ArrayList<>();
        if (jsonArray == null || jsonArray.length() < 2 || jsonArray.charAt(0) != '[') {
            return elements;
        }

        int depth = 0;
        int start = -1;
        boolean inString = false;
        boolean escaped = false;

        for (int i = 0; i < jsonArray.length(); i++) {
            char ch = jsonArray.charAt(i);

            if (inString) {
                if (escaped) {
                    escaped = false;
                } else if (ch == '\\') {
                    escaped = true;
                } else if (ch == '"') {
                    inString = false;
                }
                continue;
            }

            if (ch == '"') {
                inString = true;
                continue;
            }

            if (ch == '[') {
                depth++;
                if (depth == 1) {
                    start = i + 1;
                }
                continue;
            }

            if (ch == ']') {
                if (depth == 1 && start >= 0 && i >= start) {
                    String tail = jsonArray.substring(start, i).trim();
                    if (!tail.isBlank()) {
                        elements.add(tail);
                    }
                }
                depth--;
                if (depth == 0) {
                    break;
                }
                continue;
            }

            if (ch == ',' && depth == 1 && start >= 0) {
                String value = jsonArray.substring(start, i).trim();
                if (!value.isBlank()) {
                    elements.add(value);
                }
                start = i + 1;
            }
        }
        return elements;
    }

    private String extractFirstString(String jsonArrayElement) {
        if (jsonArrayElement == null || jsonArrayElement.isBlank()) {
            return "";
        }

        int firstQuote = -1;
        boolean escaped = false;
        for (int i = 0; i < jsonArrayElement.length(); i++) {
            char ch = jsonArrayElement.charAt(i);
            if (firstQuote < 0) {
                if (ch == '"') {
                    firstQuote = i;
                }
                continue;
            }

            if (escaped) {
                escaped = false;
                continue;
            }
            if (ch == '\\') {
                escaped = true;
                continue;
            }
            if (ch == '"') {
                String raw = jsonArrayElement.substring(firstQuote + 1, i);
                return unescapeJson(raw).trim();
            }
        }
        return "";
    }
}
