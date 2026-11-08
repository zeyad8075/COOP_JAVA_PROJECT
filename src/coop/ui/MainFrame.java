package coop.ui;

import coop.dao.DBConnection;
import coop.service.ImportService;
import coop.service.ReferenceDataService;
import coop.service.StudentService;
import coop.ui.components.UIStyle;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.HashMap;
import java.util.Map;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;

/**
 * الإطار الرئيسي للتطبيق (MainFrame).
 * يحتوي على شريط التنقل العلوي/الجانبي، وشاشة العرض الديناميكية (CardLayout)،
 * ويدير دورة حياة التطبيق وإغلاق قاعدة بيانات Derby بأمان عند الخروج.
 */
public class MainFrame extends JFrame {

    private final StudentService studentService;
    private final ReferenceDataService refService;
    private final ImportService importService;

    private JPanel contentPanel;
    private CardLayout cardLayout;

    private DashboardPanel dashboardPanel;
    private StudentPanel studentPanel;
    private ImportDataPanel importDataPanel;
    private StatisticsPanel statisticsPanel;
    private ReferenceDataPanel referenceDataPanel;

    private final Map<String, JButton> navButtons = new HashMap<>();

    public MainFrame() {
        super("نظام إدارة التدريب الميداني (COOP) — كلية إدارة الأعمال بينبع — جامعة طيبة");

        this.studentService = new StudentService();
        this.refService = new ReferenceDataService();
        this.importService = new ImportService();

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1180, 780);
        setMinimumSize(new Dimension(980, 680));
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        initComponents();

        // إغلاق قاعدة بيانات Derby بأمان عند إغلاق النافذة
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                DBConnection.shutdown();
            }
        });

        UIStyle.applyRTL(this);
    }

    private void initComponents() {
        // 1. شريط العنوان والتنقل العلوي (Top Header & Navigation Bar)
        JPanel topHeader = new JPanel(new BorderLayout(0, 0));
        topHeader.setBackground(UIStyle.COLOR_PRIMARY_DARK);

        // شعار واسم الكلية والنظام
        JPanel brandPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 15, 8));
        brandPanel.setOpaque(false);

        // إضافة شعار الجامعة من ملف University_Logo.svg
        JPanel logoBadge = coop.ui.components.LogoHelper.createHeaderLogoBadge(40);
        if (logoBadge != null) {
            brandPanel.add(logoBadge);
        }

        JLabel lblUni = new JLabel("كلية إدارة الأعمال بينبع — جامعة طيبة");
        lblUni.setFont(UIStyle.FONT_REGULAR);
        lblUni.setForeground(new Color(200, 215, 235));

        JLabel lblLogo = new JLabel("نظام التدريب الميداني (COOP)");
        lblLogo.setFont(new Font("Tahoma", Font.BOLD, 17));
        lblLogo.setForeground(Color.WHITE);

        brandPanel.add(lblUni);
        brandPanel.add(new JLabel(" | ") {{ setForeground(Color.LIGHT_GRAY); }});
        brandPanel.add(lblLogo);
        topHeader.add(brandPanel, BorderLayout.EAST);

        // شريط أزرار التنقل (Navigation Tabs Bar)
        JPanel navBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 8));
        navBar.setBackground(UIStyle.COLOR_PRIMARY);

        navBar.add(createNavButton("DASHBOARD", "لوحة المؤشرات"));
        navBar.add(createNavButton("STUDENTS", "إدارة الطلاب"));
        navBar.add(createNavButton("IMPORT", "استيراد البيانات"));
        navBar.add(createNavButton("STATISTICS", "الرسوم البيانية"));
        navBar.add(createNavButton("REFERENCE", "البيانات المرجعية"));

        topHeader.add(navBar, BorderLayout.SOUTH);
        add(topHeader, BorderLayout.NORTH);

        // 2. الحاوية الرئيسية للشاشات (CardLayout Content Panel)
        cardLayout = new CardLayout();
        contentPanel = new JPanel(cardLayout);
        contentPanel.setBackground(UIStyle.COLOR_BG);

        dashboardPanel = new DashboardPanel(studentService, refService, this);
        studentPanel = new StudentPanel(studentService, refService);
        importDataPanel = new ImportDataPanel(importService, this);
        statisticsPanel = new StatisticsPanel(studentService);
        referenceDataPanel = new ReferenceDataPanel(refService, this);

        contentPanel.add(dashboardPanel, "DASHBOARD");
        contentPanel.add(studentPanel, "STUDENTS");
        contentPanel.add(importDataPanel, "IMPORT");
        contentPanel.add(statisticsPanel, "STATISTICS");
        contentPanel.add(referenceDataPanel, "REFERENCE");

        add(contentPanel, BorderLayout.CENTER);

        // تفعيل الشاشة الافتراضية
        showPanel("DASHBOARD");
    }

    private JButton createNavButton(String key, String text) {
        JButton btn = new JButton(text);
        btn.setFont(UIStyle.FONT_BOLD);
        btn.setForeground(Color.WHITE);
        btn.setBackground(UIStyle.COLOR_PRIMARY);
        btn.setFocusPainted(false);
        btn.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(255, 255, 255, 40), 1),
                BorderFactory.createEmptyBorder(7, 18, 7, 18)
        ));
        btn.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        btn.addActionListener(e -> showPanel(key));
        navButtons.put(key, btn);
        return btn;
    }

    /**
     * التبديل بين شاشات النظام وتحديث البيانات تلقائياً.
     */
    public void showPanel(String key) {
        cardLayout.show(contentPanel, key);

        // تحديث مظهر الأزرار لتمييز الزر النشط
        for (Map.Entry<String, JButton> entry : navButtons.entrySet()) {
            if (entry.getKey().equals(key)) {
                entry.getValue().setBackground(UIStyle.COLOR_ACCENT);
                entry.getValue().setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(Color.WHITE, 1),
                        BorderFactory.createEmptyBorder(7, 18, 7, 18)
                ));
            } else {
                entry.getValue().setBackground(UIStyle.COLOR_PRIMARY);
                entry.getValue().setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(new Color(255, 255, 255, 40), 1),
                        BorderFactory.createEmptyBorder(7, 18, 7, 18)
                ));
            }
        }

        // تحديث محتوى الشاشة المعروضة عند فتحها
        switch (key) {
            case "DASHBOARD":
                dashboardPanel.refreshData();
                break;
            case "STUDENTS":
                studentPanel.loadStudents();
                studentPanel.loadFilterReferenceData();
                break;
            case "STATISTICS":
                statisticsPanel.refreshStatistics();
                break;
            case "REFERENCE":
                referenceDataPanel.refreshAllTables();
                break;
        }
    }

    /**
     * تحديث بيانات جميع الشاشات (مثلاً بعد نجاح عملية الاستيراد أو تعديل مراجع).
     */
    public void refreshAllScreens() {
        dashboardPanel.refreshData();
        studentPanel.loadStudents();
        studentPanel.loadFilterReferenceData();
        statisticsPanel.refreshStatistics();
        referenceDataPanel.refreshAllTables();
    }
}

