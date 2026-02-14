package com.newsisrael.ui;

import com.newsisrael.i18n.AppLanguage;
import com.newsisrael.i18n.I18n;
import com.newsisrael.model.NewsArticle;

import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.JEditorPane;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.ListSelectionModel;
import javax.swing.event.HyperlinkEvent;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Desktop;
import java.awt.Font;
import java.io.IOException;
import java.net.URI;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class DayNewsPanel extends JPanel {
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm");

    private final JLabel titleLabel;
    private final JEditorPane summaryPane;
    private final DefaultListModel<NewsArticle> listModel;
    private final JList<NewsArticle> newsList;
    private final JEditorPane detailsPane;
    private final JScrollPane listScroll;
    private final JScrollPane detailScroll;

    private AppLanguage language;

    public DayNewsPanel() {
        setLayout(new BorderLayout(12, 12));
        setBackground(new Color(246, 248, 251));
        setBorder(BorderFactory.createEmptyBorder(14, 14, 14, 14));

        titleLabel = new JLabel();
        titleLabel.setFont(new Font("SansSerif", Font.BOLD, 18));
        titleLabel.setForeground(new Color(26, 39, 64));

        summaryPane = new JEditorPane();
        summaryPane.setContentType("text/html");
        summaryPane.setEditable(false);
        summaryPane.setFont(new Font("SansSerif", Font.PLAIN, 14));
        summaryPane.setBackground(Color.WHITE);
        summaryPane.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(220, 226, 235)),
                BorderFactory.createEmptyBorder(10, 10, 10, 10)
        ));

        listModel = new DefaultListModel<>();
        newsList = new JList<>(listModel);
        newsList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        newsList.setFont(new Font("SansSerif", Font.PLAIN, 14));
        newsList.setBackground(Color.WHITE);
        newsList.setCellRenderer(new DefaultListCellRenderer() {
            @Override
            public java.awt.Component getListCellRendererComponent(
                    JList<?> list,
                    Object value,
                    int index,
                    boolean isSelected,
                    boolean cellHasFocus
            ) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof NewsArticle article) {
                    setText(article.title());
                }
                return this;
            }
        });

        detailsPane = new JEditorPane();
        detailsPane.setContentType("text/html");
        detailsPane.setEditable(false);
        detailsPane.setBackground(Color.WHITE);
        detailsPane.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(220, 226, 235)),
                BorderFactory.createEmptyBorder(10, 10, 10, 10)
        ));
        detailsPane.addHyperlinkListener(event -> {
            if (event.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
                openUrl(event.getURL().toString());
            }
        });

        newsList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                showDetails(newsList.getSelectedValue());
            }
        });

        listScroll = new JScrollPane(newsList);
        detailScroll = new JScrollPane(detailsPane);

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, listScroll, detailScroll);
        splitPane.setResizeWeight(0.45);
        splitPane.setContinuousLayout(true);

        JPanel topPanel = new JPanel(new BorderLayout(10, 10));
        topPanel.setBackground(getBackground());
        topPanel.add(titleLabel, BorderLayout.NORTH);
        topPanel.add(new JScrollPane(summaryPane), BorderLayout.CENTER);

        add(topPanel, BorderLayout.NORTH);
        add(splitPane, BorderLayout.CENTER);

        setLanguage(AppLanguage.defaultLanguage());
    }

    public void setLanguage(AppLanguage language) {
        this.language = language;
        listScroll.setBorder(BorderFactory.createTitledBorder(I18n.panelNewsSection(language)));
        detailScroll.setBorder(BorderFactory.createTitledBorder(I18n.panelDetailsSection(language)));

        // Keep overall layout stable; apply direction only to text-heavy components.
        if (language.isRtl()) {
            summaryPane.setComponentOrientation(java.awt.ComponentOrientation.RIGHT_TO_LEFT);
            newsList.setComponentOrientation(java.awt.ComponentOrientation.RIGHT_TO_LEFT);
        } else {
            summaryPane.setComponentOrientation(java.awt.ComponentOrientation.LEFT_TO_RIGHT);
            newsList.setComponentOrientation(java.awt.ComponentOrientation.LEFT_TO_RIGHT);
        }
        detailsPane.setComponentOrientation(java.awt.ComponentOrientation.LEFT_TO_RIGHT);
    }

    public void setLoadingState(String text) {
        titleLabel.setText(text);
        setSummaryText(I18n.panelLoadingText(language));
        summaryPane.setCaretPosition(0);
        listModel.clear();
        detailsPane.setText("");
        detailsPane.setCaretPosition(0);
    }

    public void setErrorState(String message) {
        titleLabel.setText(I18n.panelErrorTitle(language));
        setSummaryText(message);
        summaryPane.setCaretPosition(0);
        listModel.clear();
        detailsPane.setText(renderSimpleMessage(I18n.panelLoadFailed(language)));
        detailsPane.setCaretPosition(0);
    }

    public void setData(List<NewsArticle> articles, String summary) {
        titleLabel.setText(I18n.panelNewsAndSummaryTitle(language));
        setSummaryText(summary);
        summaryPane.setCaretPosition(0);
        listModel.clear();
        for (NewsArticle article : articles) {
            listModel.addElement(article);
        }
        if (!listModel.isEmpty()) {
            newsList.setSelectedIndex(0);
        } else {
            detailsPane.setText(renderSimpleMessage(I18n.panelNoPublications(language)));
            detailsPane.setCaretPosition(0);
        }
    }

    private void showDetails(NewsArticle article) {
        if (article == null) {
            detailsPane.setText("");
            return;
        }

        String published = TIME_FORMAT.format(article.publishedAt().atZone(ZoneId.systemDefault()));
        String description = article.description().isBlank() ? I18n.descriptionMissing(language) : article.description();
        String url = article.url().isBlank() ? "" : article.url();

        String dir = language.isRtl() ? "rtl" : "ltr";
        String align = language.isRtl() ? "right" : "left";

        StringBuilder html = new StringBuilder();
        html.append("<html><body dir='").append(dir)
                .append("' style='font-family:SansSerif; color:#1f2a44; text-align:")
                .append(align)
                .append("; margin:0;'>");
        html.append("<h2 style='margin-top:0;'>").append(escape(article.title())).append("</h2>");
        html.append("<p><b>").append(escape(I18n.sourceLabel(language))).append("</b> ").append(escape(article.source())).append("</p>");
        html.append("<p><b>").append(escape(I18n.timeLabel(language))).append("</b> ").append(escape(published)).append("</p>");
        html.append("<p>").append(escape(description)).append("</p>");
        if (!url.isBlank()) {
            html.append("<p><a href='").append(escape(url)).append("'>")
                    .append(escape(I18n.openOriginal(language)))
                    .append("</a></p>");
        }
        html.append("</body></html>");
        detailsPane.setText(html.toString());
        detailsPane.setCaretPosition(0);
    }

    private void openUrl(String url) {
        try {
            if (Desktop.isDesktopSupported()) {
                Desktop.getDesktop().browse(URI.create(url));
            }
        } catch (IOException ignored) {
        }
    }

    private String escape(String text) {
        return text.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }

    private String renderSimpleMessage(String message) {
        String dir = language.isRtl() ? "rtl" : "ltr";
        String align = language.isRtl() ? "right" : "left";
        return "<html><body dir='" + dir + "' style='font-family:SansSerif; text-align:" + align + ";'>"
                + "<p>" + escape(message) + "</p></body></html>";
    }

    private void setSummaryText(String text) {
        String dir = language.isRtl() ? "rtl" : "ltr";
        String align = language.isRtl() ? "right" : "left";
        String safe = escape(text == null ? "" : text).replace("\n", "<br/>");
        summaryPane.setText("<html><body dir='" + dir + "' style='font-family:SansSerif; text-align:" + align
                + "; margin:0;'>" + safe + "</body></html>");
    }
}
