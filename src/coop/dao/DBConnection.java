package coop.dao;

import coop.exception.DataAccessException;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * فئة إدارة الاتصال بقاعدة بيانات Apache Derby (Embedded Mode).
 * مسؤولة عن إنشاء الاتصالات، فحص وتهيئة الجداول تلقائياً، وإغلاق القاعدة بأمان.
 */
public class DBConnection {

    private static final String DRIVER = "org.apache.derby.jdbc.EmbeddedDriver";
    private static final String DB_DIR = "database/coop_db";
    private static final String DB_URL = "jdbc:derby:" + DB_DIR + ";create=true";
    private static final String SHUTDOWN_URL = "jdbc:derby:;shutdown=true";

    private static boolean driverLoaded = false;
    private static boolean initialized = false;

    // منع إنشاء كائنات من الفئة (Utility Pattern)
    private DBConnection() {
    }

    /**
     * تحميل مشغل Derby والتأكد من توفره وتسجيله في DriverManager.
     */
    private static synchronized void loadDriver() {
        if (!driverLoaded) {
            try {
                // استدعاء المنشئ لضمان التسجيل التلقائي لدى DriverManager حتى بعد الـ shutdown
                Class<?> driverClass = Class.forName(DRIVER);
                driverClass.getDeclaredConstructor().newInstance();
                driverLoaded = true;
            } catch (Exception e) {
                throw new DataAccessException("تعذر العثور على أو تسجيل مشغل قاعدة البيانات Apache Derby: " + DRIVER, e);
            }
        }
    }

    /**
     * الحصول على اتصال جديد بقاعدة البيانات.
     *
     * @return كائن Connection مفتوح.
     * @throws DataAccessException في حال فشل الاتصال.
     */
    public static Connection getConnection() {
        loadDriver();
        try {
            Connection conn = DriverManager.getConnection(DB_URL);
            // تهيئة الجداول تلقائياً عند أول اتصال ناجح
            if (!initialized) {
                initializeDatabase(conn);
                initialized = true;
            }
            return conn;
        } catch (SQLException e) {
            throw new DataAccessException("فشل الاتصال بقاعدة بيانات التدريب الميداني: " + e.getMessage(), e);
        }
    }

    /**
     * فحص وجود الجداول الأساسية وإنشائها تلقائياً إذا لم تكن موجودة.
     *
     * @param conn الاتصال المستخدم للتهيئة.
     */
    public static synchronized void initializeDatabase(Connection conn) {
        try {
            DatabaseMetaData meta = conn.getMetaData();
            if (!tableExists(meta, "SPECIALIZATION")) {
                createTables(conn);
            }
        } catch (SQLException e) {
            throw new DataAccessException("حدث خطأ أثناء فحص أو تهيئة جداول قاعدة البيانات: " + e.getMessage(), e);
        }
    }

    /**
     * التحقق مما إذا كان الجدول موجوداً بالفعل في قاعدة البيانات.
     */
    private static boolean tableExists(DatabaseMetaData meta, String tableName) throws SQLException {
        try (ResultSet rs = meta.getTables(null, "APP", tableName.toUpperCase(), new String[]{"TABLE"})) {
            return rs.next();
        }
    }

    /**
     * إنشاء الجداول الأربعة والفهارس وفقاً لمخطط 3NF.
     */
    private static void createTables(Connection conn) throws SQLException {
        try (Statement stmt = conn.createStatement()) {
            // 1. جدول التخصصات
            stmt.executeUpdate(
                "CREATE TABLE specialization ("
                + "  spec_id INT GENERATED ALWAYS AS IDENTITY (START WITH 1, INCREMENT BY 1),"
                + "  spec_name VARCHAR(100) NOT NULL,"
                + "  CONSTRAINT pk_specialization PRIMARY KEY (spec_id),"
                + "  CONSTRAINT uq_spec_name UNIQUE (spec_name)"
                + ")"
            );

            // 2. جدول المرشدين الأكاديميين
            stmt.executeUpdate(
                "CREATE TABLE advisor ("
                + "  advisor_id INT GENERATED ALWAYS AS IDENTITY (START WITH 1, INCREMENT BY 1),"
                + "  advisor_name VARCHAR(150) NOT NULL,"
                + "  CONSTRAINT pk_advisor PRIMARY KEY (advisor_id),"
                + "  CONSTRAINT uq_advisor_name UNIQUE (advisor_name)"
                + ")"
            );

            // 3. جدول جهات التدريب
            stmt.executeUpdate(
                "CREATE TABLE organization ("
                + "  org_id INT GENERATED ALWAYS AS IDENTITY (START WITH 1, INCREMENT BY 1),"
                + "  org_name VARCHAR(200) NOT NULL,"
                + "  CONSTRAINT pk_organization PRIMARY KEY (org_id),"
                + "  CONSTRAINT uq_org_name UNIQUE (org_name)"
                + ")"
            );

            // 4. جدول الطلاب
            stmt.executeUpdate(
                "CREATE TABLE student ("
                + "  student_id INT GENERATED ALWAYS AS IDENTITY (START WITH 1, INCREMENT BY 1),"
                + "  full_name VARCHAR(150) NOT NULL,"
                + "  student_section VARCHAR(50) NOT NULL,"
                + "  spec_id INT NOT NULL,"
                + "  advisor_id INT NOT NULL,"
                + "  org_id INT NOT NULL,"
                + "  CONSTRAINT pk_student PRIMARY KEY (student_id),"
                + "  CONSTRAINT fk_student_spec FOREIGN KEY (spec_id) REFERENCES specialization(spec_id) ON DELETE RESTRICT,"
                + "  CONSTRAINT fk_student_advisor FOREIGN KEY (advisor_id) REFERENCES advisor(advisor_id) ON DELETE RESTRICT,"
                + "  CONSTRAINT fk_student_org FOREIGN KEY (org_id) REFERENCES organization(org_id) ON DELETE RESTRICT"
                + ")"
            );

            // فهارس البحث والتصفية
            stmt.executeUpdate("CREATE INDEX idx_student_name ON student(full_name)");
            stmt.executeUpdate("CREATE INDEX idx_student_spec ON student(spec_id)");
            stmt.executeUpdate("CREATE INDEX idx_student_advisor ON student(advisor_id)");
            stmt.executeUpdate("CREATE INDEX idx_student_org ON student(org_id)");
        }
    }

    /**
     * إغلاق قاعدة بيانات Derby Embedded بشكل سليم وآمن عند انتهاء التطبيق.
     * ملاحظة: إغلاق Derby يُرجع كود الخطأ الطبيعي 08006 أو XJ015 دلالة على الإغلاق الناجح.
     */
    public static void shutdown() {
        try {
            DriverManager.getConnection(SHUTDOWN_URL);
        } catch (SQLException e) {
            // كود 08006 أو XJ015 يشير إلى نجاح إغلاق محرك Derby
            if ("XJ015".equals(e.getSQLState()) || "08006".equals(e.getSQLState())) {
                // إغلاق ناجح وطبيعي
                initialized = false;
                driverLoaded = false;
            } else {
                System.err.println("ملاحظة أثناء إغلاق قاعدة البيانات: " + e.getMessage());
            }
        }
    }
}
