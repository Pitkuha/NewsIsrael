package com.newsisrael.ui;

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
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.UIManager;
import javax.swing.WindowConstants;
import javax.swing.SpinnerDateModel;
import java.awt.BorderLayout;
import java.awt.Color;
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
    private JButton refreshButton;
    private JComboBox<Integer> newsLimitComboBox;
    private JSpinner dateSpinner;
    private final Map<LocalDate, DayNewsPanel> dayPanels;

    public NewsMainFrame() {
        super("Israel News Digest");
        this.newsService = new NewsService();
        this.summaryService = new SummaryService();
        this.dayPanels = new LinkedHashMap<>();

        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setSize(new Dimension(1200, 760));
        setMinimumSize(new Dimension(980, 620));
        setLocationRelativeTo(null);

        JPanel root = new JPanel(new BorderLayout(10, 10));
        root.setBackground(new Color(238, 242, 247));
        root.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JPanel header = buildHeader();
        root.add(header, BorderLayout.NORTH);

        tabs = new JTabbedPane();
        tabs.setFont(new Font("SansSerif", Font.PLAIN, 13));
        root.add(tabs, BorderLayout.CENTER);

        statusLabel = new JLabel("Готово");
        statusLabel.setFont(new Font("SansSerif", Font.PLAIN, 12));
        statusLabel.setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));
        root.add(statusLabel, BorderLayout.SOUTH);

        setContentPane(root);
        applyLookAndFeel();
        initDefaultTab();
    }

    private JPanel buildHeader() {
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);

        JLabel title = new JLabel("Новости Израиля");
        title.setFont(new Font("SansSerif", Font.BOLD, 24));
        title.setForeground(new Color(26, 39, 64));

        JPanel controls = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        controls.setOpaque(false);

        dateSpinner = new JSpinner(new SpinnerDateModel());
        JSpinner.DateEditor editor = new JSpinner.DateEditor(dateSpinner, "dd.MM.yyyy");
        dateSpinner.setEditor(editor);
        dateSpinner.setValue(new Date());
        dateSpinner.setPreferredSize(new Dimension(120, 30));

        JButton openDayButton = new JButton("Открыть день");
        openDayButton.addActionListener(e -> openSelectedDay());

        refreshButton = new JButton("Обновить");
        refreshButton.addActionListener(e -> refreshCurrentTab());

        newsLimitComboBox = new JComboBox<>(NEWS_LIMIT_OPTIONS);
        newsLimitComboBox.setSelectedItem(15);
        newsLimitComboBox.setPreferredSize(new Dimension(80, 30));
        newsLimitComboBox.addActionListener(e -> refreshCurrentTab());

        controls.add(new JLabel("Дата:"));
        controls.add(dateSpinner);
        controls.add(new JLabel("Кол-во:"));
        controls.add(newsLimitComboBox);
        controls.add(openDayButton);
        controls.add(refreshButton);

        header.add(title, BorderLayout.WEST);
        header.add(controls, BorderLayout.EAST);

        return header;
    }

    private void applyLookAndFeel() {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {
        }
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
            JOptionPane.showMessageDialog(this, "Нельзя открыть будущую дату.", "Ошибка", JOptionPane.WARNING_MESSAGE);
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
        dayPanels.put(date, panel);
        tabs.addTab(tabTitle(date), panel);
        tabs.setSelectedComponent(panel);
        loadDataIntoPanel(date, panel);
    }

    private void refreshCurrentTab() {
        int index = tabs.getSelectedIndex();
        if (index < 0) {
            return;
        }

        String title = tabs.getTitleAt(index);
        LocalDate date = parseDateFromTabTitle(title);
        if (date == null) {
            return;
        }

        DayNewsPanel panel = dayPanels.get(date);
        if (panel != null) {
            loadDataIntoPanel(date, panel);
        }
    }

    private LocalDate parseDateFromTabTitle(String title) {
        try {
            if ("Сегодня".equals(title)) {
                return LocalDate.now();
            }
            return LocalDate.parse(title, TAB_DATE_FORMAT);
        } catch (Exception e) {
            return null;
        }
    }

    private String tabTitle(LocalDate date) {
        return date.equals(LocalDate.now()) ? "Сегодня" : TAB_DATE_FORMAT.format(date);
    }

    private void loadDataIntoPanel(LocalDate date, DayNewsPanel panel) {
        int limit = getSelectedNewsLimit();
        panel.setLoadingState("Загрузка новостей за " + TAB_DATE_FORMAT.format(date));
        statusLabel.setText("Обновление: " + TAB_DATE_FORMAT.format(date) + " | лимит: " + limit);
        refreshButton.setEnabled(false);

        SwingWorker<List<NewsArticle>, Void> worker = new SwingWorker<>() {
            @Override
            protected List<NewsArticle> doInBackground() throws Exception {
                return newsService.loadNewsForDate(date, limit);
            }

            @Override
            protected void done() {
                refreshButton.setEnabled(true);
                try {
                    List<NewsArticle> articles = get();
                    String summary = summaryService.buildSummary(articles, date);
                    panel.setData(articles, summary);
                    statusLabel.setText("Обновлено: " + TAB_DATE_FORMAT.format(date) + " | новостей: " + articles.size());
                } catch (Exception ex) {
                    panel.setErrorState(ex.getMessage());
                    statusLabel.setText("Ошибка загрузки");
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
            // Гарантируем автоматическую первичную загрузку после открытия окна.
            SwingUtilities.invokeLater(frame::refreshCurrentTab);
        });
    }
}
