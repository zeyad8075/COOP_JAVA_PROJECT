package test;

import coop.dao.DBConnection;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * اختبارات المرحلة الثانية: التحقق من اتصال قاعدة البيانات، الجداول،
 * القيود (Foreign Keys / Unique / Restrict)، دعم النصوص العربية، وحفظ البيانات (Persistence).
 */
public class DBConnectionTest {

    public static void main(String[] args) {
        System.out.println("==================================================");
        System.out.println("بدء اختبارات المرحلة الثانية: قاعدة البيانات (Derby)");
        System.out.println("==================================================");

        int passed = 0;
        int failed = 0;

        // اختبار 1: الاتصال وتهيئة الجداول
        try (Connection conn = DBConnection.getConnection()) {
            if (conn != null && !conn.isClosed()) {
                System.out.println("[اختبار 1] نجاح الاتصال وتهيئة الجداول تلقائياً.");
                passed++;
            } else {
                throw new RuntimeException("الاتصال بقاعدة البيانات غير نشط أو مغلق!");
            }
        } catch (Exception e) {
            System.err.println("[اختبار 1 فشل] خطأ في الاتصال: " + e.getMessage());
            failed++;
        }

        // اختبار 2: التحقق من وجود الجداول الأربعة
        try (Connection conn = DBConnection.getConnection()) {
            String[] tables = {"SPECIALIZATION", "ADVISOR", "ORGANIZATION", "STUDENT"};
            for (String table : tables) {
                try (ResultSet rs = conn.getMetaData().getTables(null, "APP", table, null)) {
                    if (rs.next()) {
                        System.out.println("[اختبار 2] تم التحقق من وجود جدول: " + table);
                    } else {
                        throw new RuntimeException("الجدول غير موجود: " + table);
                    }
                }
            }
            passed++;
        } catch (Exception e) {
            System.err.println("[اختبار 2 فشل] فحص الجداول: " + e.getMessage());
            failed++;
        }

        // اختبار 3: إدخال واسترجاع بيانات تجريبية بالعربية (CRUD)
        int specId = -1;
        int advisorId = -1;
        int orgId = -1;
        int studentId = -1;

        try (Connection conn = DBConnection.getConnection()) {
            // إدخال تخصص
            try (PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO specialization (spec_name) VALUES (?)",
                    Statement.RETURN_GENERATED_KEYS)) {
                ps.setString(1, "نظم المعلومات الإدارية - تجريبي");
                ps.executeUpdate();
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (rs.next()) specId = rs.getInt(1);
                }
            }

            // إدخال مرشد
            try (PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO advisor (advisor_name) VALUES (?)",
                    Statement.RETURN_GENERATED_KEYS)) {
                ps.setString(1, "د. أحمد الجهني - تجريبي");
                ps.executeUpdate();
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (rs.next()) advisorId = rs.getInt(1);
                }
            }

            // إدخال جهة تدريب
            try (PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO organization (org_name) VALUES (?)",
                    Statement.RETURN_GENERATED_KEYS)) {
                ps.setString(1, "الهيئة الملكية بينبع - تجريبي");
                ps.executeUpdate();
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (rs.next()) orgId = rs.getInt(1);
                }
            }

            // إدخال طالب يربط المعرفات الثلاثة
            try (PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO student (full_name, student_section, spec_id, advisor_id, org_id) VALUES (?, ?, ?, ?, ?)",
                    Statement.RETURN_GENERATED_KEYS)) {
                ps.setString(1, "فيصل بن محمد الحربي");
                ps.setString(2, "طالب");
                ps.setInt(3, specId);
                ps.setInt(4, advisorId);
                ps.setInt(5, orgId);
                ps.executeUpdate();
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (rs.next()) studentId = rs.getInt(1);
                }
            }

            // التحقق من صحة القراءة والنص العربي
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT s.full_name, s.student_section, sp.spec_name, a.advisor_name, o.org_name "
                    + "FROM student s "
                    + "JOIN specialization sp ON s.spec_id = sp.spec_id "
                    + "JOIN advisor a ON s.advisor_id = a.advisor_id "
                    + "JOIN organization o ON s.org_id = o.org_id "
                    + "WHERE s.student_id = ?")) {
                ps.setInt(1, studentId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        String name = rs.getString("full_name");
                        String section = rs.getString("student_section");
                        String spec = rs.getString("spec_name");
                        String advisor = rs.getString("advisor_name");
                        String org = rs.getString("org_name");

                        if ("فيصل بن محمد الحربي".equals(name) && "طالب".equals(section)) {
                            System.out.println("[اختبار 3] نجاح الإدخال والاسترجاع مع الربط ودعم النص العربي:");
                            System.out.printf("           الطالب: %s | الشطر: %s | التخصص: %s | المرشد: %s | الجهة: %s%n",
                                    name, section, spec, advisor, org);
                            passed++;
                        } else {
                            throw new RuntimeException("البيانات المسترجعة لا تطابق المدخلات!");
                        }
                    } else {
                        throw new RuntimeException("لم يتم العثور على سجل الطالب المدخل!");
                    }
                }
            }

        } catch (Exception e) {
            System.err.println("[اختبار 3 فشل] إدخال واسترجاع البيانات: " + e.getMessage());
            failed++;
        }

        // اختبار 4: اختبار قيد التكامل المرجعي (ON DELETE RESTRICT)
        try (Connection conn = DBConnection.getConnection()) {
            boolean constraintEnforced = false;
            try (PreparedStatement ps = conn.prepareStatement("DELETE FROM organization WHERE org_id = ?")) {
                ps.setInt(1, orgId);
                ps.executeUpdate();
            } catch (SQLException e) {
                // يجب أن يفشل الحذف بسبب وجود طالب مرتبط بالجهة (SQLState 23503)
                if ("23503".equals(e.getSQLState()) || e.getMessage().contains("violation of foreign key")) {
                    constraintEnforced = true;
                    System.out.println("[اختبار 4] نجاح منع حذف جهة تدريب مرتبطة بطالب (Referential Integrity RESTRICT).");
                    passed++;
                }
            }

            if (!constraintEnforced) {
                System.err.println("[اختبار 4 فشل] تم حذف جهة التدريب بالرغم من ارتباطها بطالب!");
                failed++;
            }
        } catch (Exception e) {
            System.err.println("[اختبار 4 فشل] اختبار القيد المرجعي: " + e.getMessage());
            failed++;
        }

        // اختبار 5: تنظيف السجلات التجريبية واختبار الحذف السليم
        try (Connection conn = DBConnection.getConnection()) {
            // حذف الطالب أولاً
            try (PreparedStatement ps = conn.prepareStatement("DELETE FROM student WHERE student_id = ?")) {
                ps.setInt(1, studentId);
                ps.executeUpdate();
            }
            // الآن يمكن حذف الجهة والمرشد والتخصص بأمان
            try (PreparedStatement ps = conn.prepareStatement("DELETE FROM organization WHERE org_id = ?")) {
                ps.setInt(1, orgId);
                ps.executeUpdate();
            }
            try (PreparedStatement ps = conn.prepareStatement("DELETE FROM advisor WHERE advisor_id = ?")) {
                ps.setInt(1, advisorId);
                ps.executeUpdate();
            }
            try (PreparedStatement ps = conn.prepareStatement("DELETE FROM specialization WHERE spec_id = ?")) {
                ps.setInt(1, specId);
                ps.executeUpdate();
            }
            System.out.println("[اختبار 5] نجاح تنظيف وحذف السجلات التجريبية بعد إزالة الارتباط.");
            passed++;
        } catch (Exception e) {
            System.err.println("[اختبار 5 فشل] تنظيف السجلات: " + e.getMessage());
            failed++;
        }

        // اختبار 6: اختبار بقاء البيانات بعد إعادة الاتصال (Persistence Test)
        try {
            int testSpecId;
            String persistentName = "تخصص اختبار الاستمرارية " + System.currentTimeMillis();
            try (Connection conn = DBConnection.getConnection()) {
                try (PreparedStatement ps = conn.prepareStatement(
                        "INSERT INTO specialization (spec_name) VALUES (?)",
                        Statement.RETURN_GENERATED_KEYS)) {
                    ps.setString(1, persistentName);
                    ps.executeUpdate();
                    try (ResultSet rs = ps.getGeneratedKeys()) {
                        rs.next();
                        testSpecId = rs.getInt(1);
                    }
                }
            }

            // إغلاق محرك Derby بالكامل ثم إعادة فتحه للتأكد من حفظ البيانات في القرص
            DBConnection.shutdown();

            // إعادة الاتصال
            try (Connection conn = DBConnection.getConnection()) {
                try (PreparedStatement ps = conn.prepareStatement(
                        "SELECT spec_name FROM specialization WHERE spec_id = ?")) {
                    ps.setInt(1, testSpecId);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next() && persistentName.equals(rs.getString(1))) {
                            System.out.println("[اختبار 6] نجاح اختبار دوام وحفظ البيانات (Persistence) بعد إغلاق محرك Derby وإعادة تشغيله.");
                            passed++;
                        } else {
                            throw new RuntimeException("البيانات لم تُحفظ بعد إعادة فتح المحرك!");
                        }
                    }
                }
                // تنظيف
                try (PreparedStatement ps = conn.prepareStatement("DELETE FROM specialization WHERE spec_id = ?")) {
                    ps.setInt(1, testSpecId);
                    ps.executeUpdate();
                }
            }
        } catch (Exception e) {
            System.err.println("[اختبار 6 فشل] اختبار دوام البيانات: " + e.getMessage());
            failed++;
        }

        // إغلاق المحرك في النهاية
        DBConnection.shutdown();

        System.out.println("==================================================");
        System.out.printf("نتيجة اختبارات المرحلة الثانية: %d ناجح | %d فاشل%n", passed, failed);
        System.out.println("==================================================");

        if (failed > 0) {
            System.exit(1);
        }
    }
}
