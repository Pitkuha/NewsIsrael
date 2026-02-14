package com.newsisrael.ui;

import com.newsisrael.i18n.AppLanguage;
import com.newsisrael.i18n.I18n;
import com.newsisrael.model.NewsArticle;
import com.newsisrael.service.NewsService;
import com.newsisrael.service.SummaryService;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JTabbedPane;
import javax.swing.SpinnerDateModel;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.UIManager;
import javax.swing.WindowConstants;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class NewsMainFrame extends JFrame {
    private static final DateTimeFormatter TAB_DATE_FORMAT = DateTimeFormatter.ofPattern("dd.MM.yyyy");
    private static final Integer[] NEWS_LIMIT_OPTIONS = {5, 10, 15, 20, 30, 50};

    private final NewsService newsService;
    private final SummaryService summaryService;
    private final JTabbedPane tabs;
    private final JLabel statusLabel;
    private final JPanel rootPanel;

    private JLabel headerTitle;
    private JLabel dateLabel;
    private JLabel countLabel;
    private JLabel languageLabel;
    private JButton openDayButton;
    private JButton refreshButton;
    private JComboBox<Integer> newsLimitComboBox;
    private JComboBox<AppLanguage> languageComboBox;
    private JSpinner dateSpinner;

    private final Map<LocalDate, DayNewsPanel> dayPanels;
    private AppLanguage currentLanguage;

    public NewsMainFrame() {
        super("Israel News");
        this.newsService = new NewsService();
        this.summaryService = new SummaryService();
        this.dayPanels = new LinkedHashMap<>();
        this.currentLanguage = AppLanguage.defaultLanguage();

        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setSize(new Dimension(1200, 760));
        setMinimumSize(new Dimension(980, 620));
        setLocationRelativeTo(null);

        rootPanel = new JPanel(new BorderLayout(10, 10));
        rootPanel.setBackground(new Color(238, 242, 247));
        rootPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JPanel header = buildHeader();
        rootPanel.add(header, BorderLayout.NORTH);

        tabs = new JTabbedPane();
        tabs.setFont(new Font("SansSerif", Font.PLAIN, 13));
        rootPanel.add(tabs, BorderLayout.CENTER);

        statusLabel = new JLabel();
        statusLabel.setFont(new Font("SansSerif", Font.PLAIN, 12));
        statusLabel.setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));
        rootPanel.add(statusLabel, BorderLayout.SOUTH);

        setContentPane(rootPanel);
        applyLookAndFeel();
        applyLanguage();
        initDefaultTab();
    }

    private JPanel buildHeader() {
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);

        headerTitle = new JLabel();
        headerTitle.setFont(new Font("SansSerif", Font.BOLD, 24));
        headerTitle.setForeground(new Color(26, 39, 64));

        JPanel controls = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        controls.setOpaque(false);

        dateLabel = new JLabel();
        dateSpinner = new JSpinner(new SpinnerDateModel());
        JSpinner.DateEditor editor = new JSpinner.DateEditor(dateSpinner, "dd.MM.yyyy");
        dateSpinner.setEditor(editor);
        dateSpinner.setValue(new Date());
        dateSpinner.setPreferredSize(new Dimension(120, 30));

        countLabel = new JLabel();
        newsLimitComboBox = new JComboBox<>(NEWS_LIMIT_OPTIONS);
        newsLimitComboBox.setSelectedItem(15);
        newsLimitComboBox.setPreferredSize(new Dimension(80, 30));
        newsLimitComboBox.addActionListener(e -> refreshCurrentTab());

        languageLabel = new JLabel();
        languageComboBox = new JComboBox<>(AppLanguage.values());
        languageComboBox.setSelectedItem(currentLanguage);
        languageComboBox.setPreferredSize(new Dimension(140, 30));
        languageComboBox.addActionListener(e -> {
            AppLanguage selected = (AppLanguage) languageComboBox.getSelectedItem();
            if (selected != null && selected != currentLanguage) {
                currentLanguage = selected;
                applyLanguage();
                refreshCurrentTab();
            }
        });

        openDayButton = new JButton();
        openDayButton.addActionListener(e -> openSelectedDay());

        refreshButton = new JButton();
        refreshButton.addActionListener(e -> refreshCurrentTab());

        controls.add(dateLabel);
        controls.add(dateSpinner);
        controls.add(countLabel);
        controls.add(newsLimitComboBox);
        controls.add(languageLabel);
        controls.add(languageComboBox);
        controls.add(openDayButton);
        controls.add(refreshButton);

        header.add(headerTitle, BorderLayout.WEST);
        header.add(controls, BorderLayout.EAST);

        return header;
    }

    private void applyLookAndFeel() {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {
        }
    }

    private void applyLanguage() {
        setTitle(I18n.appTitle(currentLanguage));
        headerTitle.setText(I18n.appTitle(currentLanguage));
        dateLabel.setText(I18n.dateLabel(currentLanguage));
        countLabel.setText(I18n.countLabel(currentLanguage));
        languageLabel.setText(I18n.languageLabel(currentLanguage));
        openDayButton.setText(I18n.openDayButton(currentLanguage));
        refreshButton.setText(I18n.refreshButton(currentLanguage));
        statusLabel.setText(I18n.statusReady(currentLanguage));

        for (DayNewsPanel panel : dayPanels.values()) {
            panel.setLanguage(currentLanguage);
        }
        updateTabTitles();
    }

    private void initDefaultTab() {
        openDate(LocalDate.now());
    }

    private void openSelectedDay() {
        Date selected = (Date) dateSpinner.getValue();
        LocalDate day = Instant.ofEpochMilli(selected.getTime())
                .atZone(ZoneId.systemDefault())
                .toLocalDate();

        if (day.isAfter(LocalDate.now())) {
            JOptionPane.showMessageDialog(this,
                    I18n.futureDateMessage(currentLanguage),
                    I18n.warningTitle(currentLanguage),
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        openDate(day);
    }

    private void openDate(LocalDate date) {
        DayNewsPanel existing = dayPanels.get(date);
        if (existing != null) {
            tabs.setSelectedComponent(existing);
            return;
        }

        DayNewsPanel panel = new DayNewsPanel();
        panel.setLanguage(currentLanguage);
        dayPanels.put(date, panel);
        tabs.addTab(tabTitle(date), panel);
        tabs.setSelectedComponent(panel);
        loadDataIntoPanel(date, panel);
    }

    private void refreshCurrentTab() {
        if (tabs == null) {
            return;
        }

        LocalDate date = getSelectedDate();
        if (date == null) {
            return;
        }

        DayNewsPanel panel = dayPanels.get(date);
        if (panel != null) {
            panel.setLanguage(currentLanguage);
            loadDataIntoPanel(date, panel);
        }
    }

    private LocalDate getSelectedDate() {
        Component selected = tabs.getSelectedComponent();
        if (selected == null) {
            return null;
        }

        for (Map.Entry<LocalDate, DayNewsPanel> entry : dayPanels.entrySet()) {
            if (entry.getValue() == selected) {
                return entry.getKey();
            }
        }
        return null;
    }

    private String tabTitle(LocalDate date) {
        return date.equals(LocalDate.now()) ? I18n.todayTab(currentLanguage) : TAB_DATE_FORMAT.format(date);
    }

    private void updateTabTitles() {
        for (Map.Entry<LocalDate, DayNewsPanel> entry : dayPanels.entrySet()) {
            int index = tabs.indexOfComponent(entry.getValue());
            if (index >= 0) {
                tabs.setTitleAt(index, tabTitle(entry.getKey()));
            }
        }
    }

    private void loadDataIntoPanel(LocalDate date, DayNewsPanel panel) {
        int limit = getSelectedNewsLimit();
        panel.setLoadingState(I18n.loadingForDate(currentLanguage, date));
        statusLabel.setText(I18n.statusUpdating(currentLanguage, date, limit));
        refreshButton.setEnabled(false);

        SwingWorker<List<NewsArticle>, Void> worker = new SwingWorker<>() {
            @Override
            protected List<NewsArticle> doInBackground() throws Exception {
                return newsService.loadNewsForDate(date, limit, currentLanguage);
            }

            @Override
            protected void done() {
                refreshButton.setEnabled(true);
                try {
                    List<NewsArticle> articles = get();
                    String summary = summaryService.buildSummary(articles, date, currentLanguage);
                    panel.setData(articles, summary);
                    statusLabel.setText(I18n.statusUpdated(currentLanguage, date, articles.size()));
                } catch (Exception ex) {
                    panel.setErrorState(ex.getMessage());
                    statusLabel.setText(I18n.statusError(currentLanguage));
                }
            }
        };
        worker.execute();
    }

    private int getSelectedNewsLimit() {
        Object value = newsLimitComboBox.getSelectedItem();
        if (value instanceof Integer n && n > 0) {
            return n;
        }
        return 15;
    }

    public static void start() {
        SwingUtilities.invokeLater(() -> {
            NewsMainFrame frame = new NewsMainFrame();
            frame.setVisible(true);
            SwingUtilities.invokeLater(frame::refreshCurrentTab);
        });
    }
}
