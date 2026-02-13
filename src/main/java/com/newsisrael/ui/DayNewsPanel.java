package com.newsisrael.ui;

import com.newsisrael.model.NewsArticle;

import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JEditorPane;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.ListSelectionModel;
import javax.swing.DefaultListCellRenderer;
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

    public DayNewsPanel() {
        setLayout(new BorderLayout(12, 12));
        setBackground(new Color(246, 248, 251));
        setBorder(BorderFactory.createEmptyBorder(14, 14, 14, 14));

        titleLabel = new JLabel("Загрузка...");
        titleLabel.setFont(new Font("SansSerif", Font.BOLD, 18));
        titleLabel.setForeground(new Color(26, 39, 64));

        summaryPane = new JEditorPane();
        summaryPane.setContentType("text/plain");
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

        JScrollPane listScroll = new JScrollPane(newsList);
        listScroll.setBorder(BorderFactory.createTitledBorder("Новости"));

        JScrollPane detailScroll = new JScrollPane(detailsPane);
        detailScroll.setBorder(BorderFactory.createTitledBorder("Подробности"));

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, listScroll, detailScroll);
        splitPane.setResizeWeight(0.45);
        splitPane.setContinuousLayout(true);

        JPanel topPanel = new JPanel(new BorderLayout(10, 10));
        topPanel.setBackground(getBackground());
        topPanel.add(titleLabel, BorderLayout.NORTH);
        topPanel.add(new JScrollPane(summaryPane), BorderLayout.CENTER);

        add(topPanel, BorderLayout.NORTH);
        add(splitPane, BorderLayout.CENTER);
    }

    public void setLoadingState(String text) {
        titleLabel.setText(text);
        summaryPane.setText("Идет загрузка...");
        listModel.clear();
        detailsPane.setText("");
    }

    public void setErrorState(String message) {
        titleLabel.setText("Ошибка");
        summaryPane.setText(message);
        listModel.clear();
        detailsPane.setText("<html><body><p>Не удалось загрузить новости.</p></body></html>");
    }

    public void setData(List<NewsArticle> articles, String summary) {
        titleLabel.setText("Новости и сводка");
        summaryPane.setText(summary);
        listModel.clear();
        for (NewsArticle article : articles) {
            listModel.addElement(article);
        }
        if (!listModel.isEmpty()) {
            newsList.setSelectedIndex(0);
        } else {
            detailsPane.setText("<html><body><p>Публикаций за этот день не найдено.</p></body></html>");
        }
    }

    private void showDetails(NewsArticle article) {
        if (article == null) {
            detailsPane.setText("");
            return;
        }

        String published = TIME_FORMAT.format(article.publishedAt().atZone(ZoneId.systemDefault()));
        String description = article.description().isBlank() ? "Описание отсутствует." : article.description();
        String url = article.url().isBlank() ? "" : article.url();

        StringBuilder html = new StringBuilder();
        html.append("<html><body style='font-family:SansSerif; color:#1f2a44;'>");
        html.append("<h2 style='margin-top:0;'>").append(escape(article.title())).append("</h2>");
        html.append("<p><b>Источник:</b> ").append(escape(article.source())).append("</p>");
        html.append("<p><b>Время:</b> ").append(escape(published)).append("</p>");
        html.append("<p>").append(escape(description)).append("</p>");
        if (!url.isBlank()) {
            html.append("<p><a href='").append(escape(url)).append("'>Открыть оригинал</a></p>");
        }
        html.append("</body></html>");
        detailsPane.setText(html.toString());
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
}
