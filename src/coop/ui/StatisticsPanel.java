package coop.ui;

import coop.service.StudentService;
import coop.ui.components.UIStyle;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.util.Map;
import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;

/**
 * شاشة الإحصائيات والرسوم البيانية التفاعلية (StatisticsPanel).
 * ترسم الرسوم البيانية برمجياً باستخدام Java Graphics2D دون الحاجة لمكتبات خارجية ثقيلة،
 * مما يسهل شرح الخوارزمية في المناقشة الأكاديمية.
 */
public class StatisticsPanel extends JPanel {

    private final StudentService studentService;

    private JRadioButton rbByOrg;
    private JRadioButton rbBySpec;
    private JRadioButton rbBySection;

    private ChartCanvas chartCanvas;
    private JTable statsTable;
    private DefaultTableModel tableModel;

    public StatisticsPanel(StudentService studentService) {
        this.studentService = studentService;

        setLayout(new BorderLayout(15, 15));
        setBackground(UIStyle.COLOR_BG);
        setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        initComponents();
        UIStyle.applyRTL(this);
        refreshStatistics();
    }

    private void initComponents() {
        // 1. شريط التحكم العلوي واختيار نوع الرسم البياني
        JPanel topBar = new JPanel(new BorderLayout());
        topBar.setBackground(UIStyle.COLOR_CARD_BG);
        topBar.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UIStyle.COLOR_BORDER, 1),
                BorderFactory.createEmptyBorder(12, 15, 12, 15)
        ));

        JLabel lblTitle = new JLabel("الرسوم البيانية لتوزيع الطلاب:");
        lblTitle.setFont(UIStyle.FONT_SUBTITLE);
        lblTitle.setForeground(UIStyle.COLOR_PRIMARY);

        JPanel radioPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 15, 0));
        radioPanel.setOpaque(false);

        rbByOrg = new JRadioButton("حسب جهات التدريب", true);
        rbByOrg.setFont(UIStyle.FONT_BOLD);
        rbByOrg.setOpaque(false);
        rbByOrg.addActionListener(e -> refreshStatistics());

        rbBySpec = new JRadioButton("حسب التخصصات الأكاديمية");
        rbBySpec.setFont(UIStyle.FONT_BOLD);
        rbBySpec.setOpaque(false);
        rbBySpec.addActionListener(e -> refreshStatistics());

        rbBySection = new JRadioButton("حسب الشطر (طلاب / طالبات)");
        rbBySection.setFont(UIStyle.FONT_BOLD);
        rbBySection.setOpaque(false);
        rbBySection.addActionListener(e -> refreshStatistics());

        ButtonGroup bg = new ButtonGroup();
        bg.add(rbByOrg);
        bg.add(rbBySpec);
        bg.add(rbBySection);

        radioPanel.add(rbBySection);
        radioPanel.add(rbBySpec);
        radioPanel.add(rbByOrg);

        JButton btnRefresh = UIStyle.createButton("تحديث الإحصائيات", UIStyle.COLOR_CARD_BG, UIStyle.COLOR_TEXT_DARK);
        btnRefresh.setBorder(BorderFactory.createLineBorder(UIStyle.COLOR_BORDER, 1));
        btnRefresh.addActionListener(e -> refreshStatistics());

        topBar.add(lblTitle, BorderLayout.WEST);
        topBar.add(radioPanel, BorderLayout.CENTER);
        topBar.add(btnRefresh, BorderLayout.EAST);
        add(topBar, BorderLayout.NORTH);

        // 2. الجزء الأوسط: تقسيم الشاشة بين الرسم البياني والجدول التفصيلي
        chartCanvas = new ChartCanvas();
        chartCanvas.setBackground(UIStyle.COLOR_CARD_BG);
        chartCanvas.setBorder(BorderFactory.createLineBorder(UIStyle.COLOR_BORDER, 1));

        String[] cols = {"البيان / الفئة", "عدد الطلاب", "النسبة المئوية"};
        tableModel = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        statsTable = new JTable(tableModel);
        UIStyle.formatTable(statsTable);
        JScrollPane tableScroll = new JScrollPane(statsTable);
        tableScroll.setBorder(BorderFactory.createLineBorder(UIStyle.COLOR_BORDER, 1));
        tableScroll.setPreferredSize(new Dimension(340, 0));

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, tableScroll, chartCanvas);
        splitPane.setResizeWeight(0.3);
        splitPane.setOpaque(false);
        splitPane.setBorder(null);

        add(splitPane, BorderLayout.CENTER);
    }

    public void refreshStatistics() {
        Map<String, Integer> data;
        String chartTitle;

        if (rbBySpec.isSelected()) {
            data = studentService.getStatisticsBySpecialization();
            chartTitle = "توزيع الطلاب على التخصصات الأكاديمية";
        } else if (rbBySection.isSelected()) {
            data = studentService.getStatisticsBySection();
            chartTitle = "توزيع الطلاب حسب الشطر";
        } else {
            data = studentService.getStatisticsByOrganization();
            chartTitle = "توزيع الطلاب على جهات ومؤسسات التدريب الميداني";
        }

        // تحديث جدول الأرقام والنسب المئوية
        tableModel.setRowCount(0);
        int total = 0;
        for (int count : data.values()) {
            total += count;
        }

        for (Map.Entry<String, Integer> e : data.entrySet()) {
            double pct = total > 0 ? (e.getValue() * 100.0 / total) : 0.0;
            tableModel.addRow(new Object[]{
                    e.getKey(),
                    e.getValue(),
                    String.format("%.1f%%", pct)
            });
        }

        chartCanvas.setData(data, chartTitle, total);
        chartCanvas.repaint();
    }

    /**
     * لوحة رسم مخصصة تعتمد على Graphics2D لرسم أعمدة بيانية أفقية ناعمة ومنسقة باللغة العربية.
     */
    private static class ChartCanvas extends JPanel {
        private Map<String, Integer> data;
        private String title = "";
        private int total = 0;

        private static final Color[] PALETTE = {
                new Color(41, 128, 185),
                new Color(39, 174, 96),
                new Color(230, 126, 34),
                new Color(142, 68, 173),
                new Color(22, 160, 133),
                new Color(211, 84, 0),
                new Color(52, 73, 94),
                new Color(192, 57, 43)
        };

        public void setData(Map<String, Integer> data, String title, int total) {
            this.data = data;
            this.title = title;
            this.total = total;
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (data == null || data.isEmpty()) {
                g.setColor(UIStyle.COLOR_TEXT_MUTED);
                g.setFont(UIStyle.FONT_SUBTITLE);
                g.drawString("لا توجد بيانات كافية لعرض الرسم البياني حالياً.", getWidth() / 3, getHeight() / 2);
                return;
            }

            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            // عنوان الرسم
            g2.setFont(UIStyle.FONT_SUBTITLE);
            g2.setColor(UIStyle.COLOR_PRIMARY);
            FontMetrics fmTitle = g2.getFontMetrics();
            int titleWidth = fmTitle.stringWidth(title);
            g2.drawString(title, getWidth() - titleWidth - 25, 30);

            // رسم الأعمدة الأفقية
            int maxVal = 1;
            for (int v : data.values()) {
                if (v > maxVal) maxVal = v;
            }

            int topMargin = 55;
            int rightMargin = 220; // مساحة لأسماء التصنيفات
            int leftMargin = 70;   // مساحة للأرقام والنسب
            int availableWidth = getWidth() - rightMargin - leftMargin;
            int availableHeight = getHeight() - topMargin - 20;

            int itemsCount = Math.min(data.size(), 14); // عرض حتى 14 جهة منعاً للازدحام
            int barHeight = Math.max(16, Math.min(28, availableHeight / Math.max(1, itemsCount) - 8));
            int gap = Math.max(4, (availableHeight - (itemsCount * barHeight)) / Math.max(1, itemsCount));

            int y = topMargin;
            int colorIdx = 0;

            g2.setFont(UIStyle.FONT_REGULAR);
            FontMetrics fm = g2.getFontMetrics();

            for (Map.Entry<String, Integer> e : data.entrySet()) {
                if (colorIdx >= itemsCount) break;

                String label = e.getKey();
                int val = e.getValue();
                double pct = total > 0 ? (val * 100.0 / total) : 0;

                // اقتصار النص إذا كان طويلاً جداً
                if (fm.stringWidth(label) > rightMargin - 15) {
                    while (label.length() > 3 && fm.stringWidth(label + "...") > rightMargin - 15) {
                        label = label.substring(0, label.length() - 1);
                    }
                    label += "...";
                }

                // رسم الاسم على اليمين
                g2.setColor(UIStyle.COLOR_TEXT_DARK);
                int labelWidth = fm.stringWidth(label);
                g2.drawString(label, getWidth() - labelWidth - 15, y + barHeight - 4);

                // حساب عرض العمود
                int barWidth = (int) (((double) val / maxVal) * availableWidth);
                int barX = getWidth() - rightMargin - barWidth;

                // رسم العمود مع خلفية وظلال ناعمة
                Color barColor = PALETTE[colorIdx % PALETTE.length];
                g2.setColor(barColor);
                g2.fillRoundRect(barX, y, barWidth, barHeight, 6, 6);

                // كتابة الرقم والنسبة المئوية بجانب العمود
                g2.setColor(UIStyle.COLOR_TEXT_DARK);
                g2.setFont(UIStyle.FONT_BOLD);
                String valStr = String.format("%d (%.1f%%)", val, pct);
                g2.drawString(valStr, barX - fm.stringWidth(valStr) - 8, y + barHeight - 4);
                g2.setFont(UIStyle.FONT_REGULAR);

                y += barHeight + gap;
                colorIdx++;
            }

            g2.dispose();
        }
    }
}

