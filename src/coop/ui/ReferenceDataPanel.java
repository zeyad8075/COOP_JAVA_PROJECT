package coop.ui;

import coop.exception.ValidationException;
import coop.model.Advisor;
import coop.model.Organization;
import coop.model.Specialization;
import coop.service.ReferenceDataService;
import coop.ui.components.UIStyle;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;

/**
 * شاشة إدارة البيانات المرجعية الأساسية (ReferenceDataPanel).
 * تسمح بإدارة التخصصات والمرشدين الأكاديميين وجهات التدريب،
 * وتمنع حذف أي سجل مرجعي مرتبط بطلاب لحماية تكامل البيانات.
 */
public class ReferenceDataPanel extends JPanel {

    private final ReferenceDataService refService;
    private final MainFrame mainFrame;

    private JTable specTable;
    private DefaultTableModel specModel;

    private JTable advisorTable;
    private DefaultTableModel advisorModel;

    private JTable orgTable;
    private DefaultTableModel orgModel;

    public ReferenceDataPanel(ReferenceDataService refService, MainFrame mainFrame) {
        this.refService = refService;
        this.mainFrame = mainFrame;

        setLayout(new BorderLayout(15, 15));
        setBackground(UIStyle.COLOR_BG);
        setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        initComponents();
        UIStyle.applyRTL(this);
        refreshAllTables();
    }

    private void initComponents() {
        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.setFont(UIStyle.FONT_BOLD);

        // تبويب 1: التخصصات الأكاديمية
        tabbedPane.addTab("التخصصات الأكاديمية", createSpecTab());

        // تبويب 2: المرشدون الأكاديميون
        tabbedPane.addTab("المرشدون الأكاديميون", createAdvisorTab());

        // تبويب 3: جهات التدريب الميداني
        tabbedPane.addTab("جهات ومؤسسات التدريب", createOrgTab());

        add(tabbedPane, BorderLayout.CENTER);
    }

    // ==================== تبويب التخصصات ====================
    private JPanel createSpecTab() {
        JPanel pnl = new JPanel(new BorderLayout(10, 10));
        pnl.setBackground(UIStyle.COLOR_CARD_BG);
        pnl.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        String[] cols = {"المعرف", "اسم التخصص الأكاديمي"};
        specModel = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int row, int column) { return false; }
        };
        specTable = new JTable(specModel);
        UIStyle.formatTable(specTable);
        pnl.add(new JScrollPane(specTable), BorderLayout.CENTER);

        JPanel btnBar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        btnBar.setOpaque(false);

        JButton btnAdd = UIStyle.createButton("إضافة تخصص جديد", UIStyle.COLOR_PRIMARY, Color.WHITE);
        btnAdd.addActionListener(e -> onAddSpec());

        JButton btnEdit = UIStyle.createButton("تعديل التخصص", UIStyle.COLOR_ACCENT, Color.WHITE);
        btnEdit.addActionListener(e -> onEditSpec());

        JButton btnDelete = UIStyle.createButton("حذف التخصص", UIStyle.COLOR_DANGER, Color.WHITE);
        btnDelete.addActionListener(e -> onDeleteSpec());

        btnBar.add(btnDelete);
        btnBar.add(btnEdit);
        btnBar.add(btnAdd);
        pnl.add(btnBar, BorderLayout.SOUTH);

        return pnl;
    }

    // ==================== تبويب المرشدين ====================
    private JPanel createAdvisorTab() {
        JPanel pnl = new JPanel(new BorderLayout(10, 10));
        pnl.setBackground(UIStyle.COLOR_CARD_BG);
        pnl.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        String[] cols = {"المعرف", "اسم المرشد الأكاديمي"};
        advisorModel = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int row, int column) { return false; }
        };
        advisorTable = new JTable(advisorModel);
        UIStyle.formatTable(advisorTable);
        pnl.add(new JScrollPane(advisorTable), BorderLayout.CENTER);

        JPanel btnBar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        btnBar.setOpaque(false);

        JButton btnAdd = UIStyle.createButton("إضافة مرشد جديد", UIStyle.COLOR_PRIMARY, Color.WHITE);
        btnAdd.addActionListener(e -> onAddAdvisor());

        JButton btnEdit = UIStyle.createButton("تعديل اسم المرشد", UIStyle.COLOR_ACCENT, Color.WHITE);
        btnEdit.addActionListener(e -> onEditAdvisor());

        JButton btnDelete = UIStyle.createButton("حذف المرشد", UIStyle.COLOR_DANGER, Color.WHITE);
        btnDelete.addActionListener(e -> onDeleteAdvisor());

        btnBar.add(btnDelete);
        btnBar.add(btnEdit);
        btnBar.add(btnAdd);
        pnl.add(btnBar, BorderLayout.SOUTH);

        return pnl;
    }

    // ==================== تبويب جهات التدريب ====================
    private JPanel createOrgTab() {
        JPanel pnl = new JPanel(new BorderLayout(10, 10));
        pnl.setBackground(UIStyle.COLOR_CARD_BG);
        pnl.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        String[] cols = {"المعرف", "اسم جهة التدريب الميداني"};
        orgModel = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int row, int column) { return false; }
        };
        orgTable = new JTable(orgModel);
        UIStyle.formatTable(orgTable);
        pnl.add(new JScrollPane(orgTable), BorderLayout.CENTER);

        JPanel btnBar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        btnBar.setOpaque(false);

        JButton btnAdd = UIStyle.createButton("إضافة جهة جديدة", UIStyle.COLOR_PRIMARY, Color.WHITE);
        btnAdd.addActionListener(e -> onAddOrg());

        JButton btnEdit = UIStyle.createButton("تعديل اسم الجهة", UIStyle.COLOR_ACCENT, Color.WHITE);
        btnEdit.addActionListener(e -> onEditOrg());

        JButton btnDelete = UIStyle.createButton("حذف الجهة", UIStyle.COLOR_DANGER, Color.WHITE);
        btnDelete.addActionListener(e -> onDeleteOrg());

        btnBar.add(btnDelete);
        btnBar.add(btnEdit);
        btnBar.add(btnAdd);
        pnl.add(btnBar, BorderLayout.SOUTH);

        return pnl;
    }

    public void refreshAllTables() {
        // تحديث التخصصات
        specModel.setRowCount(0);
        List<Specialization> specs = refService.getAllSpecializations();
        for (Specialization s : specs) {
            specModel.addRow(new Object[]{s.getSpecId(), s.getSpecName()});
        }

        // تحديث المرشدين
        advisorModel.setRowCount(0);
        List<Advisor> advisors = refService.getAllAdvisors();
        for (Advisor a : advisors) {
            advisorModel.addRow(new Object[]{a.getAdvisorId(), a.getAdvisorName()});
        }

        // تحديث جهات التدريب
        orgModel.setRowCount(0);
        List<Organization> orgs = refService.getAllOrganizations();
        for (Organization o : orgs) {
            orgModel.addRow(new Object[]{o.getOrgId(), o.getOrgName()});
        }
    }

    // معالجات أحداث التخصصات
    private void onAddSpec() {
        String name = JOptionPane.showInputDialog(this, "أدخل اسم التخصص الأكاديمي الجديد:", "إضافة تخصص", JOptionPane.PLAIN_MESSAGE);
        if (name != null && !name.trim().isEmpty()) {
            try {
                refService.addSpecialization(name.trim());
                refreshAllTables();
                mainFrame.refreshAllScreens();
            } catch (ValidationException ve) {
                JOptionPane.showMessageDialog(this, ve.getMessage(), "تنبيه", JOptionPane.WARNING_MESSAGE);
            }
        }
    }

    private void onEditSpec() {
        int r = specTable.getSelectedRow();
        if (r < 0) { JOptionPane.showMessageDialog(this, "يرجى تحديد تخصص أولاً.", "تنبيه", JOptionPane.WARNING_MESSAGE); return; }
        int id = (int) specModel.getValueAt(r, 0);
        String oldName = (String) specModel.getValueAt(r, 1);
        String newName = JOptionPane.showInputDialog(this, "تعديل اسم التخصص:", oldName);
        if (newName != null && !newName.trim().isEmpty()) {
            try {
                refService.updateSpecialization(id, newName.trim());
                refreshAllTables();
                mainFrame.refreshAllScreens();
            } catch (ValidationException ve) {
                JOptionPane.showMessageDialog(this, ve.getMessage(), "تنبيه", JOptionPane.WARNING_MESSAGE);
            }
        }
    }

    private void onDeleteSpec() {
        int r = specTable.getSelectedRow();
        if (r < 0) { JOptionPane.showMessageDialog(this, "يرجى تحديد تخصص أولاً.", "تنبيه", JOptionPane.WARNING_MESSAGE); return; }
        int id = (int) specModel.getValueAt(r, 0);
        String name = (String) specModel.getValueAt(r, 1);

        int conf = JOptionPane.showConfirmDialog(this, "هل تريد حذف التخصص: " + name + "؟", "تأكيد الحذف", JOptionPane.YES_NO_OPTION);
        if (conf == JOptionPane.YES_OPTION) {
            try {
                refService.deleteSpecialization(id);
                refreshAllTables();
                mainFrame.refreshAllScreens();
                JOptionPane.showMessageDialog(this, "تم حذف التخصص بنجاح.", "تم الحذف", JOptionPane.INFORMATION_MESSAGE);
            } catch (ValidationException ve) {
                JOptionPane.showMessageDialog(this, ve.getMessage(), "منع الحذف", JOptionPane.WARNING_MESSAGE);
            }
        }
    }

    // معالجات أحداث المرشدين
    private void onAddAdvisor() {
        String name = JOptionPane.showInputDialog(this, "أدخل اسم المرشد الأكاديمي الجديد:", "إضافة مرشد", JOptionPane.PLAIN_MESSAGE);
        if (name != null && !name.trim().isEmpty()) {
            try {
                refService.addAdvisor(name.trim());
                refreshAllTables();
                mainFrame.refreshAllScreens();
            } catch (ValidationException ve) {
                JOptionPane.showMessageDialog(this, ve.getMessage(), "تنبيه", JOptionPane.WARNING_MESSAGE);
            }
        }
    }

    private void onEditAdvisor() {
        int r = advisorTable.getSelectedRow();
        if (r < 0) { JOptionPane.showMessageDialog(this, "يرجى تحديد مرشد أولاً.", "تنبيه", JOptionPane.WARNING_MESSAGE); return; }
        int id = (int) advisorModel.getValueAt(r, 0);
        String oldName = (String) advisorModel.getValueAt(r, 1);
        String newName = JOptionPane.showInputDialog(this, "تعديل اسم المرشد الأكاديمي:", oldName);
        if (newName != null && !newName.trim().isEmpty()) {
            try {
                refService.updateAdvisor(id, newName.trim());
                refreshAllTables();
                mainFrame.refreshAllScreens();
            } catch (ValidationException ve) {
                JOptionPane.showMessageDialog(this, ve.getMessage(), "تنبيه", JOptionPane.WARNING_MESSAGE);
            }
        }
    }

    private void onDeleteAdvisor() {
        int r = advisorTable.getSelectedRow();
        if (r < 0) { JOptionPane.showMessageDialog(this, "يرجى تحديد مرشد أولاً.", "تنبيه", JOptionPane.WARNING_MESSAGE); return; }
        int id = (int) advisorModel.getValueAt(r, 0);
        String name = (String) advisorModel.getValueAt(r, 1);

        int conf = JOptionPane.showConfirmDialog(this, "هل تريد حذف المرشد: " + name + "؟", "تأكيد الحذف", JOptionPane.YES_NO_OPTION);
        if (conf == JOptionPane.YES_OPTION) {
            try {
                refService.deleteAdvisor(id);
                refreshAllTables();
                mainFrame.refreshAllScreens();
                JOptionPane.showMessageDialog(this, "تم حذف المرشد بنجاح.", "تم الحذف", JOptionPane.INFORMATION_MESSAGE);
            } catch (ValidationException ve) {
                JOptionPane.showMessageDialog(this, ve.getMessage(), "منع الحذف", JOptionPane.WARNING_MESSAGE);
            }
        }
    }

    // معالجات أحداث جهات التدريب
    private void onAddOrg() {
        String name = JOptionPane.showInputDialog(this, "أدخل اسم جهة التدريب الجديدة:", "إضافة جهة تدريب", JOptionPane.PLAIN_MESSAGE);
        if (name != null && !name.trim().isEmpty()) {
            try {
                refService.addOrganization(name.trim());
                refreshAllTables();
                mainFrame.refreshAllScreens();
            } catch (ValidationException ve) {
                JOptionPane.showMessageDialog(this, ve.getMessage(), "تنبيه", JOptionPane.WARNING_MESSAGE);
            }
        }
    }

    private void onEditOrg() {
        int r = orgTable.getSelectedRow();
        if (r < 0) { JOptionPane.showMessageDialog(this, "يرجى تحديد جهة تدريب أولاً.", "تنبيه", JOptionPane.WARNING_MESSAGE); return; }
        int id = (int) orgModel.getValueAt(r, 0);
        String oldName = (String) orgModel.getValueAt(r, 1);
        String newName = JOptionPane.showInputDialog(this, "تعديل اسم جهة التدريب:", oldName);
        if (newName != null && !newName.trim().isEmpty()) {
            try {
                refService.updateOrganization(id, newName.trim());
                refreshAllTables();
                mainFrame.refreshAllScreens();
            } catch (ValidationException ve) {
                JOptionPane.showMessageDialog(this, ve.getMessage(), "تنبيه", JOptionPane.WARNING_MESSAGE);
            }
        }
    }

    private void onDeleteOrg() {
        int r = orgTable.getSelectedRow();
        if (r < 0) { JOptionPane.showMessageDialog(this, "يرجى تحديد جهة تدريب أولاً.", "تنبيه", JOptionPane.WARNING_MESSAGE); return; }
        int id = (int) orgModel.getValueAt(r, 0);
        String name = (String) orgModel.getValueAt(r, 1);

        int conf = JOptionPane.showConfirmDialog(this, "هل تريد حذف جهة التدريب: " + name + "؟", "تأكيد الحذف", JOptionPane.YES_NO_OPTION);
        if (conf == JOptionPane.YES_OPTION) {
            try {
                refService.deleteOrganization(id);
                refreshAllTables();
                mainFrame.refreshAllScreens();
                JOptionPane.showMessageDialog(this, "تم حذف جهة التدريب بنجاح.", "تم الحذف", JOptionPane.INFORMATION_MESSAGE);
            } catch (ValidationException ve) {
                JOptionPane.showMessageDialog(this, ve.getMessage(), "منع الحذف", JOptionPane.WARNING_MESSAGE);
            }
        }
    }
}

