package coop.ui;

import coop.model.Student;
import coop.service.ReferenceDataService;
import coop.service.StudentService;
import coop.ui.components.UIStyle;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingWorker;
import javax.swing.table.DefaultTableModel;

/**
 * شاشة لوحة التحكم والمؤشرات الرئيسية (DashboardPanel).
 * تعرض ملخصاً إحصائياً بأعداد الطلاب والمراجع، وأزرار الوصول السريع، وقائمة أحدث السجلات.
 */
public class DashboardPanel extends JPanel {

    private final StudentService studentService;
    private final ReferenceDataService refService;
    private final MainFrame mainFrame;

    private JLabel lblTotalStudentsVal;
    private JLabel lblTotalSpecsVal;
    private JLabel lblTotalAdvisorsVal;
    private JLabel lblTotalOrgsVal;

    private JTable recentStudentsTable;
    private DefaultTableModel tableModel;

    public DashboardPanel(StudentService studentService, ReferenceDataService refService, MainFrame mainFrame) {
        this.studentService = studentService;
        this.refService = refService;
        this.mainFrame = mainFrame;

        setLayout(new BorderLayout(15, 15));
        setBackground(UIStyle.COLOR_BG);
        setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        initComponents();
        UIStyle.applyRTL(this);
        refreshData();
    }

    private void initComponents() {
        // 1. رأس الصفحة (Welcome Banner)
        JPanel headerPanel = new JPanel(new BorderLayout(20, 0));
        headerPanel.setBackground(UIStyle.COLOR_CARD_BG);
        headerPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UIStyle.COLOR_BORDER, 1),
                BorderFactory.createEmptyBorder(15, 20, 15, 20)
        ));

        JPanel textPanel = new JPanel();
        textPanel.setLayout(new BoxLayout(textPanel, BoxLayout.Y_AXIS));
        textPanel.setOpaque(false);

        JLabel lblTitle = new JLabel("لوحة مؤشرات نظام التدريب الميداني (COOP)");
        lblTitle.setFont(UIStyle.FONT_TITLE);
        lblTitle.setForeground(UIStyle.COLOR_PRIMARY);

        JLabel lblDesc = new JLabel("مقرر برمجة الأعمال المتقدمة (MIS412) — كلية إدارة الأعمال بينبع — جامعة طيبة");
        lblDesc.setFont(UIStyle.FONT_REGULAR);
        lblDesc.setForeground(UIStyle.COLOR_TEXT_MUTED);

        textPanel.add(lblTitle);
        textPanel.add(Box.createRigidArea(new Dimension(0, 5)));
        textPanel.add(lblDesc);

        headerPanel.add(textPanel, BorderLayout.CENTER);

        javax.swing.ImageIcon logoIcon = coop.ui.components.LogoHelper.getLogoIcon(48);
        if (logoIcon != null) {
            headerPanel.add(new JLabel(logoIcon), BorderLayout.WEST);
        }

        // 2. بطاقات المؤشرات الإحصائية (Stat Cards)
        JPanel statsPanel = new JPanel(new GridLayout(1, 4, 15, 0));
        statsPanel.setOpaque(false);

        JPanel cardStudents = createCard("إجمالي الطلاب", "0", UIStyle.COLOR_PRIMARY);
        lblTotalStudentsVal = (JLabel) cardStudents.getComponent(1);

        JPanel cardSpecs = createCard("التخصصات المعتمدة", "0", UIStyle.COLOR_ACCENT);
        lblTotalSpecsVal = (JLabel) cardSpecs.getComponent(1);

        JPanel cardAdvisors = createCard("المرشدون الأكاديميون", "0", UIStyle.COLOR_SUCCESS);
        lblTotalAdvisorsVal = (JLabel) cardAdvisors.getComponent(1);

        JPanel cardOrgs = createCard("جهات ومؤسسات التدريب", "0", UIStyle.COLOR_WARNING);
        lblTotalOrgsVal = (JLabel) cardOrgs.getComponent(1);

        statsPanel.add(cardStudents);
        statsPanel.add(cardSpecs);
        statsPanel.add(cardAdvisors);
        statsPanel.add(cardOrgs);

        // تجميع الجزء العلوي
        JPanel topContainer = new JPanel(new BorderLayout(0, 15));
        topContainer.setOpaque(false);
        topContainer.add(headerPanel, BorderLayout.NORTH);
        topContainer.add(statsPanel, BorderLayout.CENTER);
        add(topContainer, BorderLayout.NORTH);

        // 3. الجزء الأوسط: جدول أحدث الطلاب وقائمة الأزرار السريعة
        JPanel centerPanel = new JPanel(new BorderLayout(15, 15));
        centerPanel.setOpaque(false);

        // عنوان الجدول وقسم أحدث السجلات
        JPanel tableSection = new JPanel(new BorderLayout(0, 10));
        tableSection.setBackground(UIStyle.COLOR_CARD_BG);
        tableSection.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UIStyle.COLOR_BORDER, 1),
                BorderFactory.createEmptyBorder(15, 15, 15, 15)
        ));

        JLabel lblTableTitle = new JLabel("أحدث الطلاب المسجلين في النظام:");
        lblTableTitle.setFont(UIStyle.FONT_SUBTITLE);
        lblTableTitle.setForeground(UIStyle.COLOR_TEXT_DARK);
        tableSection.add(lblTableTitle, BorderLayout.NORTH);

        String[] columns = {"رقم السجل", "اسم الطالب", "الشطر", "التخصص", "المرشد الأكاديمي", "جهة التدريب"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        recentStudentsTable = new JTable(tableModel);
        UIStyle.formatTable(recentStudentsTable);
        JScrollPane scrollPane = new JScrollPane(recentStudentsTable);
        scrollPane.setBorder(BorderFactory.createLineBorder(UIStyle.COLOR_BORDER, 1));
        tableSection.add(scrollPane, BorderLayout.CENTER);

        centerPanel.add(tableSection, BorderLayout.CENTER);

        // 4. أزرار الوصول السريع (Quick Actions Bar)
        JPanel actionsPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        actionsPanel.setOpaque(false);

        JButton btnManageStudents = UIStyle.createButton("إدارة سجلات الطلاب", UIStyle.COLOR_PRIMARY, Color.WHITE);
        btnManageStudents.addActionListener(e -> mainFrame.showPanel("STUDENTS"));

        JButton btnImportData = UIStyle.createButton("استيراد ملفات Excel / CSV", UIStyle.COLOR_SUCCESS, Color.WHITE);
        btnImportData.addActionListener(e -> mainFrame.showPanel("IMPORT"));

        JButton btnStats = UIStyle.createButton("عرض الرسوم البيانية", UIStyle.COLOR_ACCENT, Color.WHITE);
        btnStats.addActionListener(e -> mainFrame.showPanel("STATISTICS"));

        JButton btnRefresh = UIStyle.createButton("تحديث البيانات", UIStyle.COLOR_CARD_BG, UIStyle.COLOR_TEXT_DARK);
        btnRefresh.setBorder(BorderFactory.createLineBorder(UIStyle.COLOR_BORDER, 1));
        btnRefresh.addActionListener(e -> refreshData());

        actionsPanel.add(btnRefresh);
        actionsPanel.add(btnStats);
        actionsPanel.add(btnImportData);
        actionsPanel.add(btnManageStudents);

        add(centerPanel, BorderLayout.CENTER);
        add(actionsPanel, BorderLayout.SOUTH);
    }

    private JPanel createCard(String title, String val, Color color) {
        return UIStyle.createStatCard(title, val, color);
    }

    /**
     * تحديث أرقام المؤشرات وجدول أحدث الطلاب في الخلفية عبر SwingWorker.
     */
    public void refreshData() {
        setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        new SwingWorker<Void, Void>() {
            private int studentCount;
            private int specCount;
            private int advisorCount;
            private int orgCount;
            private List<Student> recentStudents;

            @Override
            protected Void doInBackground() {
                studentCount = studentService.getTotalStudentCount();
                specCount = refService.getAllSpecializations().size();
                advisorCount = refService.getAllAdvisors().size();
                orgCount = refService.getAllOrganizations().size();
                recentStudents = studentService.getAllStudents();
                return null;
            }

            @Override
            protected void done() {
                lblTotalStudentsVal.setText(String.valueOf(studentCount));
                lblTotalSpecsVal.setText(String.valueOf(specCount));
                lblTotalAdvisorsVal.setText(String.valueOf(advisorCount));
                lblTotalOrgsVal.setText(String.valueOf(orgCount));

                tableModel.setRowCount(0);
                if (recentStudents != null) {
                    int count = 0;
                    for (Student s : recentStudents) {
                        tableModel.addRow(new Object[]{
                                s.getStudentId(),
                                s.getFullName(),
                                s.getStudentSection(),
                                s.getSpecializationName(),
                                s.getAdvisorName(),
                                s.getOrganizationName()
                        });
                        count++;
                        if (count >= 15) break; // عرض أحدث 15 سجلاً
                    }
                }
                setCursor(Cursor.getDefaultCursor());
            }
        }.execute();
    }
}

