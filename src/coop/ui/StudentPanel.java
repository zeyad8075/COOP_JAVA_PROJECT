package coop.ui;

import coop.model.Advisor;
import coop.model.Organization;
import coop.model.Specialization;
import coop.model.Student;
import coop.service.ReferenceDataService;
import coop.service.StudentService;
import coop.ui.components.UIStyle;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.GridLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.table.DefaultTableModel;

/**
 * شاشة إدارة سجلات الطلاب (StudentPanel).
 * تتيح عرض جميع الطلاب في جدول، البحث بالاسم، التصفية المركبة (تخصص، مرشد، جهة، شطر)،
 * وإضافة وتعديل وحذف الطلاب.
 */
public class StudentPanel extends JPanel {

    private final StudentService studentService;
    private final ReferenceDataService refService;

    private JTextField txtSearchName;
    private JComboBox<Object> cmbFilterSpec;
    private JComboBox<Object> cmbFilterAdvisor;
    private JComboBox<Object> cmbFilterOrg;
    private JComboBox<String> cmbFilterSection;

    private JTable studentsTable;
    private DefaultTableModel tableModel;
    private JLabel lblStatusCount;

    private List<Student> currentStudentList = new ArrayList<>();

    public StudentPanel(StudentService studentService, ReferenceDataService refService) {
        this.studentService = studentService;
        this.refService = refService;

        setLayout(new BorderLayout(15, 15));
        setBackground(UIStyle.COLOR_BG);
        setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        initComponents();
        loadFilterReferenceData();
        UIStyle.applyRTL(this);
        loadStudents();
    }

    private void initComponents() {
        // 1. شريط البحث والتصفية المتقدمة (Top Filter Panel)
        JPanel filterCard = new JPanel();
        filterCard.setLayout(new BoxLayout(filterCard, BoxLayout.Y_AXIS));
        filterCard.setBackground(UIStyle.COLOR_CARD_BG);
        filterCard.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UIStyle.COLOR_BORDER, 1),
                BorderFactory.createEmptyBorder(15, 15, 15, 15)
        ));

        JLabel lblFilterTitle = new JLabel("البحث والتصفية المركبة:");
        lblFilterTitle.setFont(UIStyle.FONT_SUBTITLE);
        lblFilterTitle.setForeground(UIStyle.COLOR_PRIMARY);

        JPanel filtersGrid = new JPanel(new GridLayout(2, 3, 10, 10));
        filtersGrid.setOpaque(false);

        // حقل البحث بالاسم
        JPanel pnlName = new JPanel(new BorderLayout(5, 5));
        pnlName.setOpaque(false);
        JLabel lblName = new JLabel("البحث باسم الطالب:");
        lblName.setFont(UIStyle.FONT_BOLD);
        txtSearchName = new JTextField();
        txtSearchName.setPreferredSize(new Dimension(0, 32));
        txtSearchName.addActionListener(e -> applyFilter());
        pnlName.add(lblName, BorderLayout.NORTH);
        pnlName.add(txtSearchName, BorderLayout.CENTER);

        // تصفية التخصص
        JPanel pnlSpec = new JPanel(new BorderLayout(5, 5));
        pnlSpec.setOpaque(false);
        JLabel lblSpec = new JLabel("التخصص الأكاديمي:");
        lblSpec.setFont(UIStyle.FONT_BOLD);
        cmbFilterSpec = new JComboBox<>();
        cmbFilterSpec.setPreferredSize(new Dimension(0, 32));
        cmbFilterSpec.addActionListener(e -> applyFilter());
        pnlSpec.add(lblSpec, BorderLayout.NORTH);
        pnlSpec.add(cmbFilterSpec, BorderLayout.CENTER);

        // تصفية المرشد
        JPanel pnlAdv = new JPanel(new BorderLayout(5, 5));
        pnlAdv.setOpaque(false);
        JLabel lblAdv = new JLabel("المرشد الأكاديمي:");
        lblAdv.setFont(UIStyle.FONT_BOLD);
        cmbFilterAdvisor = new JComboBox<>();
        cmbFilterAdvisor.setPreferredSize(new Dimension(0, 32));
        cmbFilterAdvisor.addActionListener(e -> applyFilter());
        pnlAdv.add(lblAdv, BorderLayout.NORTH);
        pnlAdv.add(cmbFilterAdvisor, BorderLayout.CENTER);

        // تصفية جهة التدريب
        JPanel pnlOrg = new JPanel(new BorderLayout(5, 5));
        pnlOrg.setOpaque(false);
        JLabel lblOrg = new JLabel("جهة التدريب:");
        lblOrg.setFont(UIStyle.FONT_BOLD);
        cmbFilterOrg = new JComboBox<>();
        cmbFilterOrg.setPreferredSize(new Dimension(0, 32));
        cmbFilterOrg.addActionListener(e -> applyFilter());
        pnlOrg.add(lblOrg, BorderLayout.NORTH);
        pnlOrg.add(cmbFilterOrg, BorderLayout.CENTER);

        // تصفية الشطر
        JPanel pnlSec = new JPanel(new BorderLayout(5, 5));
        pnlSec.setOpaque(false);
        JLabel lblSec = new JLabel("الشطر:");
        lblSec.setFont(UIStyle.FONT_BOLD);
        cmbFilterSection = new JComboBox<>(new String[]{"الكل", "طالب", "طالبة"});
        cmbFilterSection.setPreferredSize(new Dimension(0, 32));
        cmbFilterSection.addActionListener(e -> applyFilter());
        pnlSec.add(lblSec, BorderLayout.NORTH);
        pnlSec.add(cmbFilterSection, BorderLayout.CENTER);

        // أزرار البحث والتصفية
        JPanel pnlBtns = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 20));
        pnlBtns.setOpaque(false);
        JButton btnSearch = UIStyle.createButton("تطبيق التصفية", UIStyle.COLOR_PRIMARY, Color.WHITE);
        btnSearch.addActionListener(e -> applyFilter());
        JButton btnReset = UIStyle.createButton("إعادة تعيين", UIStyle.COLOR_CARD_BG, UIStyle.COLOR_TEXT_DARK);
        btnReset.setBorder(BorderFactory.createLineBorder(UIStyle.COLOR_BORDER, 1));
        btnReset.addActionListener(e -> resetFilters());
        pnlBtns.add(btnReset);
        pnlBtns.add(btnSearch);

        filtersGrid.add(pnlName);
        filtersGrid.add(pnlSpec);
        filtersGrid.add(pnlAdv);
        filtersGrid.add(pnlOrg);
        filtersGrid.add(pnlSec);
        filtersGrid.add(pnlBtns);

        filterCard.add(lblFilterTitle);
        filterCard.add(Box.createRigidArea(new Dimension(0, 10)));
        filterCard.add(filtersGrid);
        add(filterCard, BorderLayout.NORTH);

        // 2. جدول الطلاب الرئيسي (Table Section)
        JPanel tableContainer = new JPanel(new BorderLayout());
        tableContainer.setBackground(UIStyle.COLOR_CARD_BG);
        tableContainer.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UIStyle.COLOR_BORDER, 1),
                BorderFactory.createEmptyBorder(10, 10, 10, 10)
        ));

        String[] columns = {"رقم السجل", "اسم الطالب", "الشطر", "التخصص الأكاديمي", "المرشد الأكاديمي", "جهة التدريب الميداني"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        studentsTable = new JTable(tableModel);
        UIStyle.formatTable(studentsTable);

        // النقر المزدوج لفتح شاشة التعديل
        studentsTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2 && studentsTable.getSelectedRow() >= 0) {
                    onEditStudent();
                }
            }
        });

        JScrollPane scrollPane = new JScrollPane(studentsTable);
        scrollPane.setBorder(BorderFactory.createLineBorder(UIStyle.COLOR_BORDER, 1));
        tableContainer.add(scrollPane, BorderLayout.CENTER);
        add(tableContainer, BorderLayout.CENTER);

        // 3. الشريط السفلي للعمليات (Bottom Actions & Status Bar)
        JPanel bottomBar = new JPanel(new BorderLayout());
        bottomBar.setOpaque(false);

        lblStatusCount = new JLabel("إجمالي السجلات: 0");
        lblStatusCount.setFont(UIStyle.FONT_BOLD);
        lblStatusCount.setForeground(UIStyle.COLOR_TEXT_MUTED);

        JPanel actionsPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        actionsPanel.setOpaque(false);

        JButton btnAdd = UIStyle.createButton("+ إضافة طالب جديد", UIStyle.COLOR_PRIMARY, Color.WHITE);
        btnAdd.addActionListener(e -> onAddStudent());

        JButton btnEdit = UIStyle.createButton("تعديل المحدد", UIStyle.COLOR_ACCENT, Color.WHITE);
        btnEdit.addActionListener(e -> onEditStudent());

        JButton btnDelete = UIStyle.createButton("حذف المحدد", UIStyle.COLOR_DANGER, Color.WHITE);
        btnDelete.addActionListener(e -> onDeleteStudent());

        JButton btnRefresh = UIStyle.createButton("تحديث الجدول", UIStyle.COLOR_CARD_BG, UIStyle.COLOR_TEXT_DARK);
        btnRefresh.setBorder(BorderFactory.createLineBorder(UIStyle.COLOR_BORDER, 1));
        btnRefresh.addActionListener(e -> loadStudents());

        actionsPanel.add(btnRefresh);
        actionsPanel.add(btnDelete);
        actionsPanel.add(btnEdit);
        actionsPanel.add(btnAdd);

        bottomBar.add(lblStatusCount, BorderLayout.WEST);
        bottomBar.add(actionsPanel, BorderLayout.EAST);
        add(bottomBar, BorderLayout.SOUTH);
    }

    public void loadFilterReferenceData() {
        // تحميل التخصصات
        cmbFilterSpec.removeAllItems();
        cmbFilterSpec.addItem("الكل");
        for (Specialization s : refService.getAllSpecializations()) {
            cmbFilterSpec.addItem(s);
        }

        // تحميل المرشدين
        cmbFilterAdvisor.removeAllItems();
        cmbFilterAdvisor.addItem("الكل");
        for (Advisor a : refService.getAllAdvisors()) {
            cmbFilterAdvisor.addItem(a);
        }

        // تحميل جهات التدريب
        cmbFilterOrg.removeAllItems();
        cmbFilterOrg.addItem("الكل");
        for (Organization o : refService.getAllOrganizations()) {
            cmbFilterOrg.addItem(o);
        }
    }

    public void loadStudents() {
        setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        new SwingWorker<List<Student>, Void>() {
            @Override
            protected List<Student> doInBackground() {
                return studentService.getAllStudents();
            }

            @Override
            protected void done() {
                try {
                    currentStudentList = get();
                    displayStudentsInTable(currentStudentList);
                } catch (Exception e) {
                    JOptionPane.showMessageDialog(StudentPanel.this,
                            "فشل تحميل بيانات الطلاب: " + e.getMessage(), "خطأ", JOptionPane.ERROR_MESSAGE);
                } finally {
                    setCursor(Cursor.getDefaultCursor());
                }
            }
        }.execute();
    }

    private void applyFilter() {
        String nameQuery = txtSearchName.getText().trim();
        Integer specId = null;
        Integer advisorId = null;
        Integer orgId = null;
        String section = (String) cmbFilterSection.getSelectedItem();

        Object selSpec = cmbFilterSpec.getSelectedItem();
        if (selSpec instanceof Specialization) {
            specId = ((Specialization) selSpec).getSpecId();
        }

        Object selAdv = cmbFilterAdvisor.getSelectedItem();
        if (selAdv instanceof Advisor) {
            advisorId = ((Advisor) selAdv).getAdvisorId();
        }

        Object selOrg = cmbFilterOrg.getSelectedItem();
        if (selOrg instanceof Organization) {
            orgId = ((Organization) selOrg).getOrgId();
        }

        final Integer fSpecId = specId;
        final Integer fAdvisorId = advisorId;
        final Integer fOrgId = orgId;

        setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        new SwingWorker<List<Student>, Void>() {
            @Override
            protected List<Student> doInBackground() {
                return studentService.searchAndFilter(nameQuery, fSpecId, fAdvisorId, fOrgId, section);
            }

            @Override
            protected void done() {
                try {
                    currentStudentList = get();
                    displayStudentsInTable(currentStudentList);
                } catch (Exception e) {
                    JOptionPane.showMessageDialog(StudentPanel.this,
                            "حدث خطأ أثناء تطبيق البحث والتصفية: " + e.getMessage(), "خطأ", JOptionPane.ERROR_MESSAGE);
                } finally {
                    setCursor(Cursor.getDefaultCursor());
                }
            }
        }.execute();
    }

    private void resetFilters() {
        txtSearchName.setText("");
        cmbFilterSpec.setSelectedIndex(0);
        cmbFilterAdvisor.setSelectedIndex(0);
        cmbFilterOrg.setSelectedIndex(0);
        cmbFilterSection.setSelectedIndex(0);
        loadStudents();
    }

    private void displayStudentsInTable(List<Student> students) {
        tableModel.setRowCount(0);
        if (students != null) {
            for (Student s : students) {
                tableModel.addRow(new Object[]{
                        s.getStudentId(),
                        s.getFullName(),
                        s.getStudentSection(),
                        s.getSpecializationName(),
                        s.getAdvisorName(),
                        s.getOrganizationName()
                });
            }
            lblStatusCount.setText("إجمالي السجلات المعروضة: " + students.size());
        } else {
            lblStatusCount.setText("إجمالي السجلات المعروضة: 0");
        }
    }

    private void onAddStudent() {
        Frame topFrame = (Frame) SwingUtilities.getWindowAncestor(this);
        StudentFormDialog dialog = new StudentFormDialog(topFrame, studentService, refService, null);
        dialog.setVisible(true);
        if (dialog.isSaved()) {
            loadStudents();
            loadFilterReferenceData();
        }
    }

    private void onEditStudent() {
        int selectedRow = studentsTable.getSelectedRow();
        if (selectedRow < 0) {
            JOptionPane.showMessageDialog(this, "يرجى تحديد طالب من الجدول أولاً لتعديل بياناته.", "تنبيه", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int studentId = (int) tableModel.getValueAt(selectedRow, 0);
        Student studentToEdit = studentService.getStudentById(studentId);
        if (studentToEdit == null) {
            JOptionPane.showMessageDialog(this, "تعذر العثور على سجل الطالب المختار.", "خطأ", JOptionPane.ERROR_MESSAGE);
            return;
        }

        Frame topFrame = (Frame) SwingUtilities.getWindowAncestor(this);
        StudentFormDialog dialog = new StudentFormDialog(topFrame, studentService, refService, studentToEdit);
        dialog.setVisible(true);
        if (dialog.isSaved()) {
            loadStudents();
            loadFilterReferenceData();
        }
    }

    private void onDeleteStudent() {
        int selectedRow = studentsTable.getSelectedRow();
        if (selectedRow < 0) {
            JOptionPane.showMessageDialog(this, "يرجى تحديد طالب من الجدول أولاً لحذف سجله.", "تنبيه", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int studentId = (int) tableModel.getValueAt(selectedRow, 0);
        String studentName = (String) tableModel.getValueAt(selectedRow, 1);

        int confirm = JOptionPane.showConfirmDialog(this,
                "هل أنت متأكد من رغبتك في حذف سجل الطالب:\n" + studentName + " (رقم: " + studentId + ")؟\nلا يمكن التراجع عن هذه العملية.",
                "تأكيد الحذف", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);

        if (confirm == JOptionPane.YES_OPTION) {
            try {
                boolean deleted = studentService.deleteStudent(studentId);
                if (deleted) {
                    JOptionPane.showMessageDialog(this, "تم حذف سجل الطالب بنجاح.", "تم الحذف", JOptionPane.INFORMATION_MESSAGE);
                    loadStudents();
                } else {
                    JOptionPane.showMessageDialog(this, "فشل حذف السجل، ربما تم حذفه مسبقاً.", "تنبيه", JOptionPane.WARNING_MESSAGE);
                }
            } catch (Exception e) {
                JOptionPane.showMessageDialog(this, "حدث خطأ أثناء الحذف: " + e.getMessage(), "خطأ", JOptionPane.ERROR_MESSAGE);
            }
        }
    }
}

