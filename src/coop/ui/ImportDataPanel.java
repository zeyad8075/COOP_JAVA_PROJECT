package coop.ui;

import coop.model.ImportExecutionReport;
import coop.model.ImportPreviewResult;
import coop.model.ImportRow;
import coop.service.ImportService;
import coop.ui.components.UIStyle;
import coop.util.ExcelReader;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.GridLayout;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;

/**
 * شاشة استيراد البيانات الذكية (ImportDataPanel).
 * تدعم ملفات Excel (.xlsx, .xls) وملفات CSV بترميز UTF-8.
 * تطبق مراحل العمل المنضبطة: اختيار الملف -> تحديد ورقة العمل -> مطابقة الأعمدة
 * -> المعاينة والفحص وتحديد التكرارات -> مراجعة توحيد الأسماء المتشابهة -> الاعتماد والتنفيذ الآمن.
 */
public class ImportDataPanel extends JPanel {

    private final ImportService importService;
    private final MainFrame mainFrame;

    private File selectedFile;
    private JTextField txtFilePath;
    private JComboBox<String> cmbSheets;
    private JButton btnBrowse;
    private JButton btnPreview;
    private JButton btnReviewNormalization;
    private JButton btnExecuteImport;
    private JProgressBar progressBar;

    private JLabel lblTotalCount;
    private JLabel lblValidCount;
    private JLabel lblIncompleteCount;
    private JLabel lblDuplicateCount;
    private JLabel lblUnmappedColumns;

    private JTable previewTable;
    private DefaultTableModel tableModel;

    private ImportPreviewResult currentPreviewResult;
    private Map<String, String> approvedNormalizations = new HashMap<>();

    public ImportDataPanel(ImportService importService, MainFrame mainFrame) {
        this.importService = importService;
        this.mainFrame = mainFrame;

        setLayout(new BorderLayout(15, 15));
        setBackground(UIStyle.COLOR_BG);
        setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        initComponents();
        UIStyle.applyRTL(this);
    }

    private void initComponents() {
        // 1. لوحة اختيار الملف وورقة العمل (File Selection Card)
        JPanel fileCard = new JPanel();
        fileCard.setLayout(new BoxLayout(fileCard, BoxLayout.Y_AXIS));
        fileCard.setBackground(UIStyle.COLOR_CARD_BG);
        fileCard.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UIStyle.COLOR_BORDER, 1),
                BorderFactory.createEmptyBorder(15, 15, 15, 15)
        ));

        JLabel lblStep1 = new JLabel("الخطوة 1: اختيار ملف البيانات المصدر (Excel أو CSV)");
        lblStep1.setFont(UIStyle.FONT_SUBTITLE);
        lblStep1.setForeground(UIStyle.COLOR_PRIMARY);

        JPanel row1 = new JPanel(new BorderLayout(10, 0));
        row1.setOpaque(false);

        txtFilePath = new JTextField();
        txtFilePath.setEditable(false);
        txtFilePath.setFont(UIStyle.FONT_REGULAR);
        txtFilePath.setPreferredSize(new Dimension(0, 34));

        btnBrowse = UIStyle.createButton("استعراض الملفات...", UIStyle.COLOR_PRIMARY, Color.WHITE);
        btnBrowse.addActionListener(e -> chooseFile());

        row1.add(btnBrowse, BorderLayout.WEST);
        row1.add(txtFilePath, BorderLayout.CENTER);

        // ورقة العمل وأزرار المعاينة
        JPanel row2 = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 10));
        row2.setOpaque(false);

        JLabel lblSheet = new JLabel("ورقة العمل (Sheet):");
        lblSheet.setFont(UIStyle.FONT_BOLD);
        cmbSheets = new JComboBox<>();
        cmbSheets.setPreferredSize(new Dimension(200, 32));
        cmbSheets.setEnabled(false);

        btnPreview = UIStyle.createButton("معاينة وفحص البيانات", UIStyle.COLOR_ACCENT, Color.WHITE);
        btnPreview.setEnabled(false);
        btnPreview.addActionListener(e -> runPreview());

        btnReviewNormalization = UIStyle.createButton("مراجعة توحيد الأسماء المتشابهة (0)", UIStyle.COLOR_WARNING, Color.WHITE);
        btnReviewNormalization.setEnabled(false);
        btnReviewNormalization.addActionListener(e -> openNormalizationDialog());

        row2.add(btnReviewNormalization);
        row2.add(btnPreview);
        row2.add(cmbSheets);
        row2.add(lblSheet);

        fileCard.add(lblStep1);
        fileCard.add(Box.createRigidArea(new Dimension(0, 10)));
        fileCard.add(row1);
        fileCard.add(row2);

        // شريط التحميل (Progress Bar)
        progressBar = new JProgressBar();
        progressBar.setIndeterminate(true);
        progressBar.setVisible(false);
        fileCard.add(Box.createRigidArea(new Dimension(0, 5)));
        fileCard.add(progressBar);

        // 2. بطاقة المؤشرات والنتائج (Summary Status Banner)
        JPanel bannerCard = new JPanel(new GridLayout(1, 5, 10, 0));
        bannerCard.setBackground(new Color(240, 244, 248));
        bannerCard.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UIStyle.COLOR_BORDER, 1),
                BorderFactory.createEmptyBorder(10, 15, 10, 15)
        ));

        lblTotalCount = new JLabel("إجمالي الصفوف: 0");
        lblTotalCount.setFont(UIStyle.FONT_BOLD);
        lblValidCount = new JLabel("صالحة للاستيراد: 0");
        lblValidCount.setFont(UIStyle.FONT_BOLD);
        lblValidCount.setForeground(UIStyle.COLOR_SUCCESS);
        lblIncompleteCount = new JLabel("ناقصة/مستبعدة: 0");
        lblIncompleteCount.setFont(UIStyle.FONT_BOLD);
        lblIncompleteCount.setForeground(UIStyle.COLOR_DANGER);
        lblDuplicateCount = new JLabel("تكرار محتمل: 0");
        lblDuplicateCount.setFont(UIStyle.FONT_BOLD);
        lblDuplicateCount.setForeground(UIStyle.COLOR_WARNING);
        lblUnmappedColumns = new JLabel("أعمدة إضافية: لا يوجد");
        lblUnmappedColumns.setFont(UIStyle.FONT_SMALL);
        lblUnmappedColumns.setForeground(UIStyle.COLOR_TEXT_MUTED);

        bannerCard.add(lblTotalCount);
        bannerCard.add(lblValidCount);
        bannerCard.add(lblDuplicateCount);
        bannerCard.add(lblIncompleteCount);
        bannerCard.add(lblUnmappedColumns);

        JPanel topContainer = new JPanel(new BorderLayout(0, 10));
        topContainer.setOpaque(false);
        topContainer.add(fileCard, BorderLayout.NORTH);
        topContainer.add(bannerCard, BorderLayout.SOUTH);
        add(topContainer, BorderLayout.NORTH);

        // 3. جدول المعاينة وتفاصيل الفحص (Preview JTable)
        JPanel tableContainer = new JPanel(new BorderLayout(0, 10));
        tableContainer.setBackground(UIStyle.COLOR_CARD_BG);
        tableContainer.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UIStyle.COLOR_BORDER, 1),
                BorderFactory.createEmptyBorder(10, 10, 10, 10)
        ));

        JLabel lblTableInfo = new JLabel("جدول فحص ومعاينة السجلات (حدد السجلات التي ترغب باعتماد استيرادها):");
        lblTableInfo.setFont(UIStyle.FONT_SUBTITLE);
        lblTableInfo.setForeground(UIStyle.COLOR_TEXT_DARK);
        tableContainer.add(lblTableInfo, BorderLayout.NORTH);

        String[] columns = {"استيراد؟", "السطر", "حالة السجل", "اسم الطالب", "الشطر", "التخصص", "المرشد الأكاديمي", "جهة التدريب", "ملاحظات الفحص"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public Class<?> getColumnClass(int columnIndex) {
                return columnIndex == 0 ? Boolean.class : super.getColumnClass(columnIndex);
            }

            @Override
            public boolean isCellEditable(int row, int column) {
                return column == 0; // فقط مربع الاختيار قابل للنقر
            }
        };

        previewTable = new JTable(tableModel);
        UIStyle.formatTable(previewTable);
        previewTable.getColumnModel().getColumn(0).setPreferredWidth(60);
        previewTable.getColumnModel().getColumn(1).setPreferredWidth(50);
        previewTable.getColumnModel().getColumn(2).setPreferredWidth(130);
        previewTable.getColumnModel().getColumn(8).setPreferredWidth(250);

        // تلوين الصفوف حسب حالتها
        previewTable.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                setHorizontalAlignment(JLabel.RIGHT);
                if (!isSelected) {
                    String status = String.valueOf(table.getValueAt(row, 2));
                    if (status.contains("ناقصة") || status.contains("غير صالح")) {
                        c.setBackground(new Color(253, 237, 236)); // أحمر فاتح
                    } else if (status.contains("مكرر")) {
                        c.setBackground(new Color(254, 249, 231)); // أصفر/برتقالي فاتح
                    } else {
                        c.setBackground(Color.WHITE);
                    }
                }
                return c;
            }
        });

        JScrollPane scrollPane = new JScrollPane(previewTable);
        scrollPane.setBorder(BorderFactory.createLineBorder(UIStyle.COLOR_BORDER, 1));
        tableContainer.add(scrollPane, BorderLayout.CENTER);
        add(tableContainer, BorderLayout.CENTER);

        // 4. الشريط السفلي لاعتماد الاستيراد
        JPanel bottomBar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        bottomBar.setOpaque(false);

        JButton btnSelectAll = UIStyle.createButton("تحديد الكل", UIStyle.COLOR_CARD_BG, UIStyle.COLOR_TEXT_DARK);
        btnSelectAll.setBorder(BorderFactory.createLineBorder(UIStyle.COLOR_BORDER, 1));
        btnSelectAll.addActionListener(e -> setAllSelected(true));

        JButton btnDeselectAll = UIStyle.createButton("إلغاء تحديد الكل", UIStyle.COLOR_CARD_BG, UIStyle.COLOR_TEXT_DARK);
        btnDeselectAll.setBorder(BorderFactory.createLineBorder(UIStyle.COLOR_BORDER, 1));
        btnDeselectAll.addActionListener(e -> setAllSelected(false));

        btnExecuteImport = UIStyle.createButton("اعتماد وبدء الاستيراد النهائي", UIStyle.COLOR_SUCCESS, Color.WHITE);
        btnExecuteImport.setEnabled(false);
        btnExecuteImport.addActionListener(e -> executeImport());

        bottomBar.add(btnDeselectAll);
        bottomBar.add(btnSelectAll);
        bottomBar.add(btnExecuteImport);

        add(bottomBar, BorderLayout.SOUTH);
    }

    private void chooseFile() {
        JFileChooser chooser = new JFileChooser(selectedFile != null ? selectedFile.getParentFile() : new File("."));
        chooser.setDialogTitle("اختيار ملف بيانات التدريب الميداني");
        chooser.setFileFilter(new FileNameExtensionFilter("ملفات البيانات (Excel و CSV)", "xlsx", "xls", "csv"));

        int res = chooser.showOpenDialog(this);
        if (res == JFileChooser.APPROVE_OPTION) {
            selectedFile = chooser.getSelectedFile();
            txtFilePath.setText(selectedFile.getAbsolutePath());
            cmbSheets.removeAllItems();

            String name = selectedFile.getName().toLowerCase();
            if (name.endsWith(".xlsx") || name.endsWith(".xls")) {
                try {
                    List<String> sheets = ExcelReader.getSheetNames(selectedFile);
                    for (String s : sheets) {
                        cmbSheets.addItem(s);
                    }
                    cmbSheets.setEnabled(true);
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(this, "تعذر قراءة أوراق العمل في ملف Excel: " + ex.getMessage(), "خطأ", JOptionPane.ERROR_MESSAGE);
                }
            } else {
                cmbSheets.addItem("(ملف CSV - لا توجد أوراق عمل)");
                cmbSheets.setEnabled(false);
            }

            btnPreview.setEnabled(true);
            btnExecuteImport.setEnabled(false);
            btnReviewNormalization.setEnabled(false);
        }
    }

    private void runPreview() {
        if (selectedFile == null || !selectedFile.exists()) {
            JOptionPane.showMessageDialog(this, "يرجى اختيار ملف صالح أولاً.", "تنبيه", JOptionPane.WARNING_MESSAGE);
            return;
        }

        setBusy(true);
        String selectedSheet = (String) cmbSheets.getSelectedItem();

        new SwingWorker<ImportPreviewResult, Void>() {
            @Override
            protected ImportPreviewResult doInBackground() {
                String fname = selectedFile.getName().toLowerCase();
                if (fname.endsWith(".csv")) {
                    return importService.previewCSV(selectedFile, null);
                } else {
                    return importService.previewExcel(selectedFile, selectedSheet, null);
                }
            }

            @Override
            protected void done() {
                try {
                    currentPreviewResult = get();
                    displayPreview(currentPreviewResult);
                } catch (Exception e) {
                    JOptionPane.showMessageDialog(ImportDataPanel.this,
                            "حدث خطأ أثناء معاينة وفحص الملف: " + e.getMessage(), "خطأ في المعاينة", JOptionPane.ERROR_MESSAGE);
                } finally {
                    setBusy(false);
                }
            }
        }.execute();
    }

    private void displayPreview(ImportPreviewResult result) {
        lblTotalCount.setText("إجمالي الصفوف: " + result.getTotalRows());
        lblValidCount.setText("صالحة للاستيراد: " + result.getValidCount());
        lblDuplicateCount.setText("تكرار محتمل: " + result.getDuplicateCount());
        lblIncompleteCount.setText("ناقصة/مستبعدة: " + result.getIncompleteCount());

        if (result.getUnmappedHeaders().isEmpty()) {
            lblUnmappedColumns.setText("أعمدة إضافية: لا يوجد");
        } else {
            lblUnmappedColumns.setText("أعمدة إضافية غير مرتبطة: " + String.join("، ", result.getUnmappedHeaders()));
        }

        tableModel.setRowCount(0);
        for (ImportRow r : result.getRows()) {
            tableModel.addRow(new Object[]{
                    r.isSelectedForImport(),
                    r.getRowNumber(),
                    r.getStatus().getDisplayName(),
                    r.getCleanedName(),
                    r.getCleanedSection(),
                    r.getCleanedSpec(),
                    r.getCleanedAdvisor(),
                    r.getCleanedOrg(),
                    String.join(" | ", r.getValidationMessages())
            });
        }

        int totalSuggestions = result.getOrganizationSuggestions().size()
                + result.getAdvisorSuggestions().size()
                + result.getSpecializationSuggestions().size();

        btnReviewNormalization.setText(String.format("مراجعة توحيد الأسماء المتشابهة (%d)", totalSuggestions));
        btnReviewNormalization.setEnabled(totalSuggestions > 0);
        btnExecuteImport.setEnabled(result.getTotalRows() > 0);
    }

    private void openNormalizationDialog() {
        if (currentPreviewResult == null) return;

        JDialog dlg = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), "مراجعة واقتراح توحيد الأسماء المتشابهة", true);
        dlg.setSize(650, 480);
        dlg.setLocationRelativeTo(this);
        dlg.setLayout(new BorderLayout(10, 10));

        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        JLabel info = new JLabel("<html><b>اكتشف النظام قيماً متشابهة في الكتابة الإملائية.</b><br/>"
                + "ضع علامة صح أمام الاقتراحات التي توافق على توحيدها إلى الاسم القياسي، أو اتركها لتبقى كما هي:</html>");
        info.setFont(UIStyle.FONT_REGULAR);
        content.add(info);
        content.add(Box.createRigidArea(new Dimension(0, 10)));

        List<JCheckBox> checkBoxes = new ArrayList<>();
        Map<JCheckBox, String[]> boxMapping = new HashMap<>();

        // 1. جهات التدريب
        for (Map.Entry<String, List<String>> e : currentPreviewResult.getOrganizationSuggestions().entrySet()) {
            String std = e.getKey();
            for (String var : e.getValue()) {
                if (var.equals(std)) continue;
                JCheckBox cb = new JCheckBox("جهة تدريب: توحيد [" + var + "] إلى -> [" + std + "]");
                cb.setSelected(true); // محدد افتراضياً كأفضل ممارسة
                cb.setFont(UIStyle.FONT_REGULAR);
                content.add(cb);
                checkBoxes.add(cb);
                boxMapping.put(cb, new String[]{var, std});
            }
        }

        // 2. المرشدون
        for (Map.Entry<String, List<String>> e : currentPreviewResult.getAdvisorSuggestions().entrySet()) {
            String std = e.getKey();
            for (String var : e.getValue()) {
                if (var.equals(std)) continue;
                JCheckBox cb = new JCheckBox("مرشد أكاديمي: توحيد [" + var + "] إلى -> [" + std + "]");
                cb.setSelected(true);
                cb.setFont(UIStyle.FONT_REGULAR);
                content.add(cb);
                checkBoxes.add(cb);
                boxMapping.put(cb, new String[]{var, std});
            }
        }

        JScrollPane sp = new JScrollPane(content);
        dlg.add(sp, BorderLayout.CENTER);

        JPanel btnBar = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton btnOk = UIStyle.createButton("اعتماد الخيارات المحددة", UIStyle.COLOR_PRIMARY, Color.WHITE);
        btnOk.addActionListener(ev -> {
            approvedNormalizations.clear();
            for (JCheckBox cb : checkBoxes) {
                if (cb.isSelected()) {
                    String[] pair = boxMapping.get(cb);
                    approvedNormalizations.put(pair[0], pair[1]);
                }
            }
            JOptionPane.showMessageDialog(dlg, "تم اعتماد " + approvedNormalizations.size() + " توحيداً بنجاح.", "تم", JOptionPane.INFORMATION_MESSAGE);
            dlg.dispose();
        });
        btnBar.add(btnOk);
        dlg.add(btnBar, BorderLayout.SOUTH);

        UIStyle.applyRTL(dlg);
        dlg.setVisible(true);
    }

    private void executeImport() {
        if (currentPreviewResult == null) return;

        // مزامنة حالة الاختيار مع الجدول
        for (int r = 0; r < tableModel.getRowCount(); r++) {
            boolean checked = Boolean.TRUE.equals(tableModel.getValueAt(r, 0));
            currentPreviewResult.getRows().get(r).setSelectedForImport(checked);
        }

        int confirm = JOptionPane.showConfirmDialog(this,
                "هل أنت متأكد من رغبتك في بدء استيراد السجلات المحددة إلى قاعدة البيانات؟",
                "تأكيد الاستيراد", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);

        if (confirm != JOptionPane.YES_OPTION) return;

        setBusy(true);
        new SwingWorker<ImportExecutionReport, Void>() {
            @Override
            protected ImportExecutionReport doInBackground() {
                return importService.executeImport(currentPreviewResult, approvedNormalizations);
            }

            @Override
            protected void done() {
                try {
                    ImportExecutionReport report = get();
                    JOptionPane.showMessageDialog(ImportDataPanel.this,
                            report.getFormattedSummary(), "نتيجة الاستيراد", JOptionPane.INFORMATION_MESSAGE);
                    mainFrame.refreshAllScreens();
                } catch (Exception e) {
                    JOptionPane.showMessageDialog(ImportDataPanel.this,
                            "فشل تنفيذ الاستيراد: " + e.getMessage(), "خطأ حرج", JOptionPane.ERROR_MESSAGE);
                } finally {
                    setBusy(false);
                }
            }
        }.execute();
    }

    private void setAllSelected(boolean selected) {
        for (int r = 0; r < tableModel.getRowCount(); r++) {
            tableModel.setValueAt(selected, r, 0);
        }
    }

    private void setBusy(boolean busy) {
        progressBar.setVisible(busy);
        btnBrowse.setEnabled(!busy);
        btnPreview.setEnabled(!busy);
        btnExecuteImport.setEnabled(!busy && currentPreviewResult != null);
        setCursor(busy ? Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR) : Cursor.getDefaultCursor());
    }
}
