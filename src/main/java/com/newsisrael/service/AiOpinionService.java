package com.newsisrael.service;

import com.newsisrael.i18n.AppLanguage;
import com.newsisrael.i18n.I18n;
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

    public String buildOpinion(LocalDate date, List<NewsArticle> articles, AppLanguage language) {
        if (articles == null || articles.isEmpty()) {
            return I18n.aiUnavailable(language);
        }

        String prompt = buildPrompt(date, articles, language);

        String fromOpenAi = requestOpenAi(prompt);
        if (!fromOpenAi.isBlank()) {
            return fromOpenAi;
        }

        String fromOllama = requestOllama(prompt);
        if (!fromOllama.isBlank()) {
            return fromOllama;
        }

        return fallbackOpinion(date, articles, language);
    }

    public String buildOpinion(LocalDate date, List<NewsArticle> articles) {
        return buildOpinion(date, articles, AppLanguage.defaultLanguage());
    }

    private String buildPrompt(LocalDate date, List<NewsArticle> articles, AppLanguage language) {
        StringBuilder sb = new StringBuilder();
        sb.append(promptHeader(language, date)).append("\n\n");

        int max = Math.min(articles.size(), 25);
        for (int i = 0; i < max; i++) {
            NewsArticle article = articles.get(i);
            sb.append(i + 1)
                    .append(") ")
                    .append(promptTitleLabel(language))
                    .append(trim(article.title(), 220))
                    .append("; ")
                    .append(promptDescriptionLabel(language))
                    .append(trim(article.description(), 260))
                    .append("\n");
        }
        return sb.toString();
    }

    private String promptHeader(AppLanguage language, LocalDate date) {
        String dateText = DATE_FORMAT.format(date);
        return switch (language) {
            case RUSSIAN -> "Дата: " + dateText + ". Ниже новости про Израиль за день. Проанализируй их и дай свое мнение об обстановке в Израиле на сегодня. Ответ только на русском, 3-5 предложений, без списков и без дисклеймеров.";
            case ENGLISH -> "Date: " + dateText + ". Below are Israel-related articles for the day. Analyze them and provide your view of the current situation in Israel today. Answer in English, 3-5 sentences, no lists, no disclaimers.";
            case HEBREW -> "תאריך: " + dateText + ". להלן חדשות על ישראל לאותו יום. נתח אותן וכתוב הערכה על המצב בישראל היום. תשובה בעברית בלבד, 3-5 משפטים, ללא רשימות וללא הסתייגויות.";
            case ARABIC -> "التاريخ: " + dateText + ". فيما يلي أخبار متعلقة بإسرائيل خلال اليوم. حلّلها وقدّم رأيك حول الوضع الحالي في إسرائيل. الإجابة بالعربية فقط، 3-5 جمل، بدون قوائم وبدون تنبيهات.";
        };
    }

    private String promptTitleLabel(AppLanguage language) {
        return switch (language) {
            case RUSSIAN -> "Заголовок: ";
            case ENGLISH -> "Title: ";
            case HEBREW -> "כותרת: ";
            case ARABIC -> "العنوان: ";
        };
    }

    private String promptDescriptionLabel(AppLanguage language) {
        return switch (language) {
            case RUSSIAN -> "Описание: ";
            case ENGLISH -> "Description: ";
            case HEBREW -> "תיאור: ";
            case ARABIC -> "الوصف: ";
        };
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

    private String fallbackOpinion(LocalDate date, List<NewsArticle> articles, AppLanguage language) {
        int total = articles.size();
        long escalationSignals = articles.stream()
                .map(a -> (a.title() + " " + a.description()).toLowerCase(Locale.ROOT))
                .filter(this::containsEscalationKeyword)
                .count();

        return switch (language) {
            case RUSSIAN -> fallbackRu(date, total, escalationSignals);
            case ENGLISH -> fallbackEn(date, total, escalationSignals);
            case HEBREW -> fallbackHe(date, total, escalationSignals);
            case ARABIC -> fallbackAr(date, total, escalationSignals);
        };
    }

    private boolean containsEscalationKeyword(String text) {
        return text.contains("удар") || text.contains("атака") || text.contains("обстрел") || text.contains("конфликт")
                || text.contains("эвакуац") || text.contains("attack") || text.contains("strike") || text.contains("clash")
                || text.contains("conflict") || text.contains("evac") || text.contains("هجوم") || text.contains("قصف")
                || text.contains("تصعيد") || text.contains("صراع") || text.contains("התקפה") || text.contains("הסלמה")
                || text.contains("עימות");
    }

    private String fallbackRu(LocalDate date, int total, long escalationSignals) {
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

    private String fallbackEn(LocalDate date, int total, long escalationSignals) {
        if (escalationSignals > 0) {
            return "On " + DATE_FORMAT.format(date)
                    + ", the information picture around Israel appears tense, with a meaningful share of reports focused on security and escalation risks. "
                    + "At the same time, the coverage is diverse and reflects several parallel political and regional tracks. "
                    + "Overall, uncertainty remains high and developments should be monitored continuously.";
        }

        return "On " + DATE_FORMAT.format(date)
                + ", the Israel-related news flow appears mixed and multi-layered, with no single narrative fully dominating the agenda. "
                + "Across " + total + " articles, updates continue to evolve across security, politics, and international responses. "
                + "Overall, the situation remains dynamic and requires ongoing monitoring.";
    }

    private String fallbackHe(LocalDate date, int total, long escalationSignals) {
        if (escalationSignals > 0) {
            return "נכון ל-" + DATE_FORMAT.format(date)
                    + ", תמונת החדשות סביב ישראל נראית מתוחה, וחלק משמעותי מהדיווחים עוסק בסוגיות ביטחוניות ובסיכוני הסלמה. "
                    + "במקביל, הסיקור מגוון ומשקף כמה צירים פוליטיים ואזוריים במקביל. "
                    + "בסך הכול רמת אי-הוודאות גבוהה, ולכן חשוב לעקוב אחר ההתפתחויות באופן רציף.";
        }

        return "נכון ל-" + DATE_FORMAT.format(date)
                + ", הזרם החדשותי על ישראל נראה מעורב ורב-שכבתי, ללא נרטיב אחד דומיננטי באופן מלא. "
                + "לאורך " + total + " כתבות ניכרת התפתחות מתמשכת בנושאי ביטחון, פוליטיקה ותגובות בינלאומיות. "
                + "המסקנה הכללית: המצב דינמי ודורש מעקב שוטף.";
    }

    private String fallbackAr(LocalDate date, int total, long escalationSignals) {
        if (escalationSignals > 0) {
            return "في " + DATE_FORMAT.format(date)
                    + " تبدو الصورة الإخبارية حول إسرائيل متوترة، إذ يركّز جزء مهم من التغطية على قضايا الأمن ومخاطر التصعيد. "
                    + "وفي الوقت نفسه، يبقى التدفق الإعلامي متنوعًا ويعكس عدة مسارات سياسية وإقليمية متزامنة. "
                    + "الخلاصة العامة: مستوى عدم اليقين مرتفع، ومن الضروري متابعة التطورات بشكل مستمر.";
        }

        return "في " + DATE_FORMAT.format(date)
                + " يظهر المشهد الإخباري المرتبط بإسرائيل بصورة مركبة ومتعددة المستويات، دون هيمنة كاملة لسردية واحدة. "
                + "وعبر " + total + " خبرًا، تتواصل التحديثات في ملفات الأمن والسياسة وردود الفعل الدولية. "
                + "الخلاصة: الوضع ديناميكي ويتطلب متابعة مستمرة.";
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
