package coop.ui;

import coop.exception.ValidationException;
import coop.model.Advisor;
import coop.model.Organization;
import coop.model.Specialization;
import coop.model.Student;
import coop.service.ReferenceDataService;
import coop.service.StudentService;
import coop.ui.components.UIStyle;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;

/**
 * نافذة حوارية منبثقة (Modal Dialog) لإضافة أو تعديل بيانات طالب.
 * تستخدم القوائم المنسدلة (JComboBox) للمراجع لتقليل أخطاء الإدخال،
 * وتطبق قواعد التحقق عبر طبقة StudentService.
 */
public class StudentFormDialog extends JDialog {

    private final StudentService studentService;
    private final ReferenceDataService refService;
    private final Student studentToEdit;
    private boolean saved = false;

    private JTextField txtFullName;
    private JComboBox<String> cmbSection;
    private JComboBox<Specialization> cmbSpecialization;
    private JComboBox<Advisor> cmbAdvisor;
    private JComboBox<Organization> cmbOrganization;

    public StudentFormDialog(Frame parent, StudentService studentService, ReferenceDataService refService, Student studentToEdit) {
        super(parent, studentToEdit == null ? "إضافة طالب جديد" : "تعديل بيانات الطالب", true);
        this.studentService = studentService;
        this.refService = refService;
        this.studentToEdit = studentToEdit;

        setSize(520, 420);
        setLocationRelativeTo(parent);
        setResizable(false);
        setLayout(new BorderLayout());

        initComponents();
        loadReferenceData();
        populateFieldsIfEdit();

        UIStyle.applyRTL(this);
    }

    private void initComponents() {
        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setBackground(UIStyle.COLOR_CARD_BG);
        formPanel.setBorder(BorderFactory.createEmptyBorder(25, 25, 20, 25));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 8, 8, 8);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // 1. اسم الطالب
        gbc.gridx = 0; gbc.gridy = 0; gbc.weightx = 0.3;
        JLabel lblName = new JLabel("اسم الطالب/ـة:");
        lblName.setFont(UIStyle.FONT_BOLD);
        formPanel.add(lblName, gbc);

        gbc.gridx = 1; gbc.weightx = 0.7;
        txtFullName = new JTextField();
        txtFullName.setFont(UIStyle.FONT_REGULAR);
        txtFullName.setPreferredSize(new Dimension(0, 34));
        formPanel.add(txtFullName, gbc);

        // 2. الشطر
        gbc.gridx = 0; gbc.gridy = 1; gbc.weightx = 0.3;
        JLabel lblSection = new JLabel("الشطر:");
        lblSection.setFont(UIStyle.FONT_BOLD);
        formPanel.add(lblSection, gbc);

        gbc.gridx = 1; gbc.weightx = 0.7;
        cmbSection = new JComboBox<>(new String[]{"طالب", "طالبة"});
        cmbSection.setFont(UIStyle.FONT_REGULAR);
        cmbSection.setPreferredSize(new Dimension(0, 34));
        formPanel.add(cmbSection, gbc);

        // 3. التخصص
        gbc.gridx = 0; gbc.gridy = 2; gbc.weightx = 0.3;
        JLabel lblSpec = new JLabel("التخصص الأكاديمي:");
        lblSpec.setFont(UIStyle.FONT_BOLD);
        formPanel.add(lblSpec, gbc);

        gbc.gridx = 1; gbc.weightx = 0.7;
        cmbSpecialization = new JComboBox<>();
        cmbSpecialization.setFont(UIStyle.FONT_REGULAR);
        cmbSpecialization.setPreferredSize(new Dimension(0, 34));
        formPanel.add(cmbSpecialization, gbc);

        // 4. المرشد الأكاديمي
        gbc.gridx = 0; gbc.gridy = 3; gbc.weightx = 0.3;
        JLabel lblAdvisor = new JLabel("المرشد الأكاديمي:");
        lblAdvisor.setFont(UIStyle.FONT_BOLD);
        formPanel.add(lblAdvisor, gbc);

        gbc.gridx = 1; gbc.weightx = 0.7;
        cmbAdvisor = new JComboBox<>();
        cmbAdvisor.setFont(UIStyle.FONT_REGULAR);
        cmbAdvisor.setPreferredSize(new Dimension(0, 34));
        formPanel.add(cmbAdvisor, gbc);

        // 5. جهة التدريب
        gbc.gridx = 0; gbc.gridy = 4; gbc.weightx = 0.3;
        JLabel lblOrg = new JLabel("جهة التدريب الميداني:");
        lblOrg.setFont(UIStyle.FONT_BOLD);
        formPanel.add(lblOrg, gbc);

        gbc.gridx = 1; gbc.weightx = 0.7;
        cmbOrganization = new JComboBox<>();
        cmbOrganization.setFont(UIStyle.FONT_REGULAR);
        cmbOrganization.setPreferredSize(new Dimension(0, 34));
        formPanel.add(cmbOrganization, gbc);

        add(formPanel, BorderLayout.CENTER);

        // أزرار الحفظ والإلغاء
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 15));
        btnPanel.setBackground(UIStyle.COLOR_BG);
        btnPanel.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, UIStyle.COLOR_BORDER));

        JButton btnCancel = UIStyle.createButton("إلغاء", UIStyle.COLOR_CARD_BG, UIStyle.COLOR_TEXT_DARK);
        btnCancel.setBorder(BorderFactory.createLineBorder(UIStyle.COLOR_BORDER, 1));
        btnCancel.addActionListener(e -> dispose());

        JButton btnSave = UIStyle.createButton("حفظ البيانات", UIStyle.COLOR_PRIMARY, Color.WHITE);
        btnSave.addActionListener(e -> onSave());

        btnPanel.add(btnCancel);
        btnPanel.add(btnSave);

        add(btnPanel, BorderLayout.SOUTH);
    }

    private void loadReferenceData() {
        List<Specialization> specs = refService.getAllSpecializations();
        cmbSpecialization.setModel(new DefaultComboBoxModel<>(specs.toArray(new Specialization[0])));

        List<Advisor> advisors = refService.getAllAdvisors();
        cmbAdvisor.setModel(new DefaultComboBoxModel<>(advisors.toArray(new Advisor[0])));

        List<Organization> orgs = refService.getAllOrganizations();
        cmbOrganization.setModel(new DefaultComboBoxModel<>(orgs.toArray(new Organization[0])));
    }

    private void populateFieldsIfEdit() {
        if (studentToEdit != null) {
            txtFullName.setText(studentToEdit.getFullName());
            cmbSection.setSelectedItem(studentToEdit.getStudentSection());

            if (studentToEdit.getSpecialization() != null) {
                for (int i = 0; i < cmbSpecialization.getItemCount(); i++) {
                    if (cmbSpecialization.getItemAt(i).getSpecId() == studentToEdit.getSpecialization().getSpecId()) {
                        cmbSpecialization.setSelectedIndex(i);
                        break;
                    }
                }
            }

            if (studentToEdit.getAdvisor() != null) {
                for (int i = 0; i < cmbAdvisor.getItemCount(); i++) {
                    if (cmbAdvisor.getItemAt(i).getAdvisorId() == studentToEdit.getAdvisor().getAdvisorId()) {
                        cmbAdvisor.setSelectedIndex(i);
                        break;
                    }
                }
            }

            if (studentToEdit.getOrganization() != null) {
                for (int i = 0; i < cmbOrganization.getItemCount(); i++) {
                    if (cmbOrganization.getItemAt(i).getOrgId() == studentToEdit.getOrganization().getOrgId()) {
                        cmbOrganization.setSelectedIndex(i);
                        break;
                    }
                }
            }
        }
    }

    private void onSave() {
        String fullName = txtFullName.getText().trim();
        String section = (String) cmbSection.getSelectedItem();

        Specialization spec = (Specialization) cmbSpecialization.getSelectedItem();
        Advisor advisor = (Advisor) cmbAdvisor.getSelectedItem();
        Organization org = (Organization) cmbOrganization.getSelectedItem();

        if (spec == null || advisor == null || org == null) {
            JOptionPane.showMessageDialog(this,
                    "يرجى التأكد من اختيار التخصص والمرشد وجهة التدريب. إذا كانت القوائم فارغة يرجى إضافتها أولاً من شاشة المراجع.",
                    "تنبيه نقص بيانات", JOptionPane.WARNING_MESSAGE);
            return;
        }

        try {
            if (studentToEdit == null) {
                // إضافة جديدة
                studentService.addStudent(fullName, section, spec.getSpecId(), advisor.getAdvisorId(), org.getOrgId());
                JOptionPane.showMessageDialog(this, "تمت إضافة الطالب بنجاح.", "تم الحفظ", JOptionPane.INFORMATION_MESSAGE);
            } else {
                // تعديل
                studentService.updateStudent(studentToEdit.getStudentId(), fullName, section,
                        spec.getSpecId(), advisor.getAdvisorId(), org.getOrgId());
                JOptionPane.showMessageDialog(this, "تم تحديث بيانات الطالب بنجاح.", "تم التعديل", JOptionPane.INFORMATION_MESSAGE);
            }
            saved = true;
            dispose();
        } catch (ValidationException ve) {
            JOptionPane.showMessageDialog(this, ve.getMessage(), "خطأ في البيانات المدخلة", JOptionPane.WARNING_MESSAGE);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "حدث خطأ غير متوقع أثناء الحفظ: " + ex.getMessage(), "خطأ في النظام", JOptionPane.ERROR_MESSAGE);
        }
    }

    public boolean isSaved() {
        return saved;
    }
}

