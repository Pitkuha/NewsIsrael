package com.newsisrael.i18n;

public enum AppLanguage {
    RUSSIAN("ru", "Русский", false),
    ENGLISH("en", "English", false),
    HEBREW("he", "עברית", true),
    ARABIC("ar", "العربية", true);

    private final String code;
    private final String displayName;
    private final boolean rtl;

    AppLanguage(String code, String displayName, boolean rtl) {
        this.code = code;
        this.displayName = displayName;
        this.rtl = rtl;
    }

    public String code() {
        return code;
    }

    public boolean isRtl() {
        return rtl;
    }

    public static AppLanguage defaultLanguage() {
        return RUSSIAN;
    }

    @Override
    public String toString() {
        return displayName;
    }
}
