package com.newsisrael.model;

import java.time.Instant;

public record NewsArticle(
        String title,
        String description,
        String source,
        Instant publishedAt,
        String url
) {
}
