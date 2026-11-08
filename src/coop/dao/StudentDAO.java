package coop.dao;

import coop.exception.DataAccessException;
import coop.model.Advisor;
import coop.model.Organization;
import coop.model.Specialization;
import coop.model.Student;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * فئة الوصول للبيانات لجدول الطلاب (student).
 * تطبق واجهة Dao<Student>، وتدعم الاستعلامات المتقدمة (الربط، البحث الجزئي،
 * التصفية المجمعة، والإحصائيات التحليلية للوحة التحكم ورسوم البيانية).
 */
public class StudentDAO implements Dao<Student> {

    private static final String BASE_SELECT =
            "SELECT s.student_id, s.full_name, s.student_section, "
            + "       sp.spec_id, sp.spec_name, "
            + "       a.advisor_id, a.advisor_name, "
            + "       o.org_id, o.org_name "
            + "FROM student s "
            + "JOIN specialization sp ON s.spec_id = sp.spec_id "
            + "JOIN advisor a ON s.advisor_id = a.advisor_id "
            + "JOIN organization o ON s.org_id = o.org_id ";

    @Override
    public Student findById(int id) {
        String sql = BASE_SELECT + "WHERE s.student_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapRow(rs);
                }
            }
            return null;
        } catch (SQLException e) {
            throw new DataAccessException("فشل استرجاع بيانات الطالب بالمعرف " + id + ": " + e.getMessage(), e);
        }
    }

    @Override
    public List<Student> findAll() {
        String sql = BASE_SELECT + "ORDER BY s.student_id ASC";
        List<Student> list = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(mapRow(rs));
            }
            return list;
        } catch (SQLException e) {
            throw new DataAccessException("فشل استرجاع قائمة جميع الطلاب: " + e.getMessage(), e);
        }
    }

    @Override
    public Student insert(Student entity) {
        String sql = "INSERT INTO student (full_name, student_section, spec_id, advisor_id, org_id) "
                   + "VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, entity.getFullName().trim());
            ps.setString(2, entity.getStudentSection().trim());
            ps.setInt(3, entity.getSpecId());
            ps.setInt(4, entity.getAdvisorId());
            ps.setInt(5, entity.getOrgId());
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    entity.setStudentId(rs.getInt(1));
                }
            }
            return entity;
        } catch (SQLException e) {
            throw new DataAccessException("فشل إضافة الطالب '" + entity.getFullName() + "': " + e.getMessage(), e);
        }
    }

    @Override
    public boolean update(Student entity) {
        String sql = "UPDATE student SET full_name = ?, student_section = ?, spec_id = ?, advisor_id = ?, org_id = ? "
                   + "WHERE student_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, entity.getFullName().trim());
            ps.setString(2, entity.getStudentSection().trim());
            ps.setInt(3, entity.getSpecId());
            ps.setInt(4, entity.getAdvisorId());
            ps.setInt(5, entity.getOrgId());
            ps.setInt(6, entity.getStudentId());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DataAccessException("فشل تحديث بيانات الطالب رقم " + entity.getStudentId() + ": " + e.getMessage(), e);
        }
    }

    @Override
    public boolean delete(int id) {
        String sql = "DELETE FROM student WHERE student_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DataAccessException("فشل حذف سجل الطالب رقم " + id + ": " + e.getMessage(), e);
        }
    }

    /**
     * بحث متقدم وتصفية مجمعة (Compound Search & Filter).
     * يستخدم PreparedStatement لجميع المعاملات لتأمين التطبيق من ثغرات SQL Injection.
     *
     * @param nameQuery نص البحث في الاسم (بحث جزئي).
     * @param specId معرف التخصص المختار (أو null لإلغاء التصفية).
     * @param advisorId معرف المرشد المختار (أو null لإلغاء التصفية).
     * @param orgId معرف جهة التدريب المختارة (أو null لإلغاء التصفية).
     * @param section الشطر المختار (أو null لإلغاء التصفية).
     * @return قائمة الطلاب المطابقين لجميع الشروط معاً.
     */
    public List<Student> searchAndFilter(String nameQuery, Integer specId, Integer advisorId, Integer orgId, String section) {
        StringBuilder sql = new StringBuilder(BASE_SELECT).append("WHERE 1=1 ");
        List<Object> params = new ArrayList<>();

        if (nameQuery != null && !nameQuery.trim().isEmpty()) {
            sql.append("AND LOWER(s.full_name) LIKE ? ");
            params.add("%" + nameQuery.trim().toLowerCase() + "%");
        }
        if (specId != null && specId > 0) {
            sql.append("AND s.spec_id = ? ");
            params.add(specId);
        }
        if (advisorId != null && advisorId > 0) {
            sql.append("AND s.advisor_id = ? ");
            params.add(advisorId);
        }
        if (orgId != null && orgId > 0) {
            sql.append("AND s.org_id = ? ");
            params.add(orgId);
        }
        if (section != null && !section.trim().isEmpty() && !"الكل".equals(section.trim())) {
            sql.append("AND s.student_section = ? ");
            params.add(section.trim());
        }

        sql.append("ORDER BY s.student_id ASC");

        List<Student> list = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) {
                ps.setObject(i + 1, params.get(i));
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRow(rs));
                }
            }
            return list;
        } catch (SQLException e) {
            throw new DataAccessException("فشل تنفيذ البحث والتصفية: " + e.getMessage(), e);
        }
    }

    /**
     * إجمالي عدد الطلاب في النظام.
     */
    public int countAll() {
        String sql = "SELECT COUNT(*) FROM student";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                return rs.getInt(1);
            }
            return 0;
        } catch (SQLException e) {
            throw new DataAccessException("فشل حساب إجمالي عدد الطلاب: " + e.getMessage(), e);
        }
    }

    /**
     * التحقق من وجود طالب بالاسم نفسه (لأغراض تنبيهات التكرار).
     */
    public boolean existsByFullName(String fullName) {
        if (fullName == null || fullName.trim().isEmpty()) return false;
        String sql = "SELECT 1 FROM student WHERE LOWER(TRIM(full_name)) = LOWER(TRIM(?))";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, fullName.trim());
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            throw new DataAccessException("فشل فحص تكرار اسم الطالب: " + e.getMessage(), e);
        }
    }

    /**
     * إحصائيات: عدد الطلاب في كل جهة تدريب (مرتبة تنازلياً حسب العدد).
     */
    public Map<String, Integer> countStudentsByOrganization() {
        String sql = "SELECT o.org_name, COUNT(s.student_id) AS cnt "
                   + "FROM organization o "
                   + "LEFT JOIN student s ON o.org_id = s.org_id "
                   + "GROUP BY o.org_name "
                   + "ORDER BY cnt DESC, o.org_name ASC";
        Map<String, Integer> stats = new LinkedHashMap<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                stats.put(rs.getString("org_name"), rs.getInt("cnt"));
            }
            return stats;
        } catch (SQLException e) {
            throw new DataAccessException("فشل استخراج إحصائيات جهات التدريب: " + e.getMessage(), e);
        }
    }

    /**
     * إحصائيات: عدد الطلاب في كل تخصص أكاديمي.
     */
    public Map<String, Integer> countStudentsBySpecialization() {
        String sql = "SELECT sp.spec_name, COUNT(s.student_id) AS cnt "
                   + "FROM specialization sp "
                   + "LEFT JOIN student s ON sp.spec_id = s.spec_id "
                   + "GROUP BY sp.spec_name "
                   + "ORDER BY cnt DESC, sp.spec_name ASC";
        Map<String, Integer> stats = new LinkedHashMap<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                stats.put(rs.getString("spec_name"), rs.getInt("cnt"));
            }
            return stats;
        } catch (SQLException e) {
            throw new DataAccessException("فشل استخراج إحصائيات التخصصات: " + e.getMessage(), e);
        }
    }

    /**
     * إحصائيات: توزيع الطلاب حسب الشطر (طالب / طالبة).
     */
    public Map<String, Integer> countStudentsBySection() {
        String sql = "SELECT student_section, COUNT(student_id) AS cnt "
                   + "FROM student "
                   + "GROUP BY student_section "
                   + "ORDER BY cnt DESC";
        Map<String, Integer> stats = new LinkedHashMap<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                stats.put(rs.getString("student_section"), rs.getInt("cnt"));
            }
            return stats;
        } catch (SQLException e) {
            throw new DataAccessException("فشل استخراج إحصائيات الشطر: " + e.getMessage(), e);
        }
    }

    /**
     * تحويل صف من ResultSet إلى كائن Student مكتمل بالكيانات المرتبطة.
     */
    private Student mapRow(ResultSet rs) throws SQLException {
        Specialization spec = new Specialization(rs.getInt("spec_id"), rs.getString("spec_name"));
        Advisor advisor = new Advisor(rs.getInt("advisor_id"), rs.getString("advisor_name"));
        Organization org = new Organization(rs.getInt("org_id"), rs.getString("org_name"));

        return new Student(
                rs.getInt("student_id"),
                rs.getString("full_name"),
                rs.getString("student_section"),
                spec,
                advisor,
                org
        );
    }
}

