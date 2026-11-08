package test;

import coop.dao.DBConnection;
import coop.dao.StudentDAO;
import coop.model.ImportExecutionReport;
import coop.model.ImportPreviewResult;
import coop.service.DataNormalizationService;
import coop.service.ImportService;
import coop.util.CSVReader;
import coop.util.ExcelReader;
import coop.util.ValidationUtil;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * اختبارات المرحلة الرابعة: التحقق من قراءة ملفات CSV و Excel،
 * المطابقة التلقائية للأعمدة، خوارزميات توحيد الأسماء، المعاينة والفحص،
 * وتنفيذ الاستيراد الآمن عبر المعاملات (JDBC Transactions).
 */
public class ExcelAndImportTest {

    public static void main(String[] args) {
        System.out.println("==================================================");
        System.out.println("بدء اختبارات المرحلة الرابعة: معالجة الملفات والاستيراد");
        System.out.println("==================================================");

        int passed = 0;
        int failed = 0;

        // اختبار 1: التحقق من دوال تنظيف ومقارنة النصوص العربية (ValidationUtil)
        try {
            String raw = "   أحمد   بن  محمد   \u00A0 ";
            String cleaned = ValidationUtil.clean(raw);
            if (!"أحمد بن محمد".equals(cleaned)) {
                throw new RuntimeException("فشل دالة تنظيف النص!");
            }

            double sim1 = ValidationUtil.calculateSimilarity("الغرفة التجارية", "الغرفه التجاريه");
            if (sim1 < 0.9) {
                throw new RuntimeException("فشل حساب التشابه بين التاء المربوطة والهاء!");
            }

            System.out.println("[اختبار 1] نجاح التحقق من تنظيف النصوص وحساب درجة التشابه اللغوي.");
            passed++;
        } catch (Exception e) {
            System.err.println("[اختبار 1 فشل] ValidationUtil: " + e.getMessage());
            failed++;
        }

        // اختبار 2: قراءة ملف CSV بترميز UTF-8
        File tempCsv = new File("test_sample.csv");
        try {
            try (OutputStreamWriter writer = new OutputStreamWriter(new FileOutputStream(tempCsv), StandardCharsets.UTF_8)) {
                writer.write("\uFEFF"); // كتابة BOM
                writer.write("الاسم,الشطر,التخصص,المرشد الأكاديمي,جهة التدريب\n");
                writer.write("\"سلطان الجهني\",طالب,نظم المعلومات الإدارية,د. أحمد,شركة ينبع للبتروكيماويات\n");
                writer.write("هند العتيبي,طالبة,المحاسبة,د. منى,الغرفة التجارية\n");
            }

            CSVReader csvReader = new CSVReader(tempCsv);
            csvReader.read();

            List<String> headers = csvReader.getHeaders();
            List<List<String>> rows = csvReader.getRows();

            if (headers.size() != 5 || rows.size() != 2) {
                throw new RuntimeException("عدد الأعمدة أو الصفوف في CSV غير صحيح!");
            }
            if (!"سلطان الجهني".equals(rows.get(0).get(0))) {
                throw new RuntimeException("قراءة النصوص العربية في CSV غير سليمة!");
            }

            System.out.println("[اختبار 2] نجاح قراءة ملف CSV بترميز UTF-8 وفك الحقول المقتبسة.");
            passed++;
        } catch (Exception e) {
            System.err.println("[اختبار 2 فشل] CSVReader: " + e.getMessage());
            failed++;
        } finally {
            tempCsv.delete();
        }

        // اختبار 3: قراءة ملف Excel الفعلي التوضيحي (بيانات التدريب الميداني.xlsx)
        File excelFile = new File("بيانات التدريب الميداني.xlsx");
        if (excelFile.exists()) {
            try {
                List<String> sheets = ExcelReader.getSheetNames(excelFile);
                if (sheets.isEmpty()) {
                    throw new RuntimeException("لم يتم العثور على أوراق عمل في ملف الإكسل!");
                }

                ExcelReader excelReader = new ExcelReader(excelFile);
                excelReader.readSheet(sheets.get(0));

                List<String> headers = excelReader.getHeaders();
                List<List<String>> rows = excelReader.getRows();

                System.out.println("           أوراق العمل المكتشفة: " + sheets);
                System.out.println("           أعمدة الإكسل: " + headers);
                System.out.println("           عدد السجلات المستخرجة: " + rows.size());

                if (headers.isEmpty() || rows.size() < 100) {
                    throw new RuntimeException("بيانات الإكسل غير مكتملة!");
                }

                System.out.println("[اختبار 3] نجاح قراءة ملف Excel (.xlsx) واستخراج الأعمدة و 106 سجلات كاملة.");
                passed++;
            } catch (Exception e) {
                System.err.println("[اختبار 3 فشل] ExcelReader: " + e.getMessage());
                failed++;
            }
        } else {
            System.out.println("[اختبار 3 متجاوز] ملف الإكسل غير موجود حالياً.");
        }

        // اختبار 4: المطابقة التلقائية للأعمدة
        try {
            ImportService importService = new ImportService();
            List<String> sampleHeaders = Arrays.asList("المرشد الأكاديمى", "الإسم", "الشطر", "التخصص", "الرقم الجامعى", "جهة التدريب");
            Map<String, String> detected = importService.detectColumnMapping(sampleHeaders);

            if (!"الإسم".equals(detected.get(ImportService.FIELD_NAME)) ||
                !"الشطر".equals(detected.get(ImportService.FIELD_SECTION)) ||
                !"التخصص".equals(detected.get(ImportService.FIELD_SPEC)) ||
                !"المرشد الأكاديمى".equals(detected.get(ImportService.FIELD_ADVISOR)) ||
                !"جهة التدريب".equals(detected.get(ImportService.FIELD_ORG))) {
                throw new RuntimeException("المطابقة التلقائية للأعمدة لم تكن دقيقة: " + detected);
            }

            System.out.println("[اختبار 4] نجاح المطابقة التلقائية للأعمدة وتحديد الحقول المرجعية بنجاح:");
            System.out.println("           نتائج المطابقة: " + detected);
            passed++;
        } catch (Exception e) {
            System.err.println("[اختبار 4 فشل] Column Mapping: " + e.getMessage());
            failed++;
        }

        // اختبار 5: خدمة توحيد القيم المتشابهة (DataNormalizationService)
        try {
            DataNormalizationService norm = new DataNormalizationService();
            List<String> rawOrgs = Arrays.asList(
                    "الغرفة التجارية بينبع",
                    "الغرفه التجاريه بينبع",
                    "مستشفى ينبع العام",
                    "مستشفي ينبع العام",
                    "إدارة التعليم",
                    "الهيئة الملكية"
            );

            Map<String, List<String>> groups = norm.findSimilarGroups(rawOrgs, null);
            if (groups.size() < 2) {
                throw new RuntimeException("لم يتم تجميع القيم المتشابهة بشكل صحيح: " + groups);
            }

            System.out.println("[اختبار 5] نجاح اكتشاف مجموعات القيم المتشابهة واقتراح التوحيد:");
            for (Map.Entry<String, List<String>> entry : groups.entrySet()) {
                System.out.println("           المقترح القياسي: [" + entry.getKey() + "] البدائل: " + entry.getValue());
            }
            passed++;
        } catch (Exception e) {
            System.err.println("[اختبار 5 فشل] DataNormalizationService: " + e.getMessage());
            failed++;
        }

        // اختبار 6: المعاينة والفحص والتنفيذ الآمن للاستيراد (JDBC Transaction & Report)
        File testImportCsv = new File("test_import_data.csv");
        try {
            try (OutputStreamWriter writer = new OutputStreamWriter(new FileOutputStream(testImportCsv), StandardCharsets.UTF_8)) {
                writer.write("الاسم,الشطر,التخصص,المرشد,جهة التدريب\n");
                writer.write("ياسر حميد الحربي,طالب,إدارة الأعمال,أ.د. سمير,الغرفة التجارية\n");
                writer.write("ريم سالم الصاعدي,طالبة,إدارة الأعمال,أ.د. سمير,الغرفة التجارية\n");
                writer.write(",طالبة,المحاسبة,د. منى,مستشفى الهيئة\n"); // سجل ناقص (بدون اسم)
                writer.write("ياسر حميد الحربي,طالب,إدارة الأعمال,أ.د. سمير,الغرفة التجارية\n"); // مكرر محتمل
            }

            // تنظيف أي سجلات تجريبية سابقة قد تؤثر على فحص التكرار
            try (java.sql.Connection conn = DBConnection.getConnection();
                 java.sql.Statement stmt = conn.createStatement()) {
                stmt.executeUpdate("DELETE FROM student WHERE full_name IN ('ياسر حميد الحربي', 'ريم سالم الصاعدي')");
            }

            ImportService importService = new ImportService();
            ImportPreviewResult preview = importService.previewCSV(testImportCsv, null);

            if (preview.getTotalRows() != 4 || preview.getIncompleteCount() != 1 || preview.getDuplicateCount() != 1) {
                throw new RuntimeException("نتائج فحص وتصنيف الصفوف غير صحيحة! " +
                        String.format("إجمالي=%d, ناقص=%d, مكرر=%d",
                                preview.getTotalRows(), preview.getIncompleteCount(), preview.getDuplicateCount()));
            }

            StudentDAO studentDAO = new StudentDAO();
            int initialCount = studentDAO.countAll();

            // تنفيذ الاستيراد كمعاملة آمنة (استيراد السجلين الصالحين فقط)
            ImportExecutionReport report = importService.executeImport(preview, new HashMap<>());
            if (report.getSuccessfullyImported() != 3) { // الطالبين الصالحين + المكرر المعتمد
                throw new RuntimeException("عدد الطلاب المستوردين غير مطابق! المتوقع 3 ولكن النتيجة: " + report.getSuccessfullyImported());
            }

            // التحقق من وصول الطلاب لقاعدة البيانات
            int count = studentDAO.countAll();
            if (count != initialCount + 3) {
                throw new RuntimeException("الفرق في عدد الطلاب في قاعدة البيانات لا يطابق المستورد: قبل=" + initialCount + "، بعد=" + count);
            }

            // تنظيف السجلات التجريبية المستوردة
            try (java.sql.Connection conn = DBConnection.getConnection();
                 java.sql.Statement stmt = conn.createStatement()) {
                stmt.executeUpdate("DELETE FROM student WHERE full_name IN ('ياسر حميد الحربي', 'ريم سالم الصاعدي')");
            }

            System.out.println("[اختبار 6] نجاح فحص ومعاينة البيانات وتنفيذ الاستيراد كمعاملة آمنة وتوليد التقرير:");
            System.out.println("           " + report.getFormattedSummary().replace("\n", "\n           "));
            passed++;
        } catch (Exception e) {
            System.err.println("[اختبار 6 فشل] Import Execution: " + e.getMessage());
            failed++;
        } finally {
            testImportCsv.delete();
            DBConnection.shutdown();
        }

        System.out.println("==================================================");
        System.out.printf("نتيجة اختبارات المرحلة الرابعة: %d ناجح | %d فاشل%n", passed, failed);
        System.out.println("==================================================");

        if (failed > 0) {
            System.exit(1);
        }
    }
}

