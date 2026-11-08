package coop.dao;

import coop.exception.DataAccessException;
import coop.model.Advisor;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * فئة الوصول للبيانات لجدول المرشدين الأكاديميين (advisor).
 * تطبق واجهة Dao<Advisor> وتدعم البحث بالاسم والإنشاء التلقائي وفحص الارتباط.
 */
public class AdvisorDAO implements Dao<Advisor> {

    @Override
    public Advisor findById(int id) {
        String sql = "SELECT advisor_id, advisor_name FROM advisor WHERE advisor_id = ?";
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
            throw new DataAccessException("فشل البحث عن المرشد بالمعرف " + id + ": " + e.getMessage(), e);
        }
    }

    @Override
    public List<Advisor> findAll() {
        String sql = "SELECT advisor_id, advisor_name FROM advisor ORDER BY advisor_name ASC";
        List<Advisor> list = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(mapRow(rs));
            }
            return list;
        } catch (SQLException e) {
            throw new DataAccessException("فشل استرجاع قائمة المرشدين الأكاديميين: " + e.getMessage(), e);
        }
    }

    @Override
    public Advisor insert(Advisor entity) {
        String sql = "INSERT INTO advisor (advisor_name) VALUES (?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, entity.getAdvisorName().trim());
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    entity.setAdvisorId(rs.getInt(1));
                }
            }
            return entity;
        } catch (SQLException e) {
            throw new DataAccessException("فشل إضافة المرشد الأكاديمي '" + entity.getAdvisorName() + "': " + e.getMessage(), e);
        }
    }

    @Override
    public boolean update(Advisor entity) {
        String sql = "UPDATE advisor SET advisor_name = ? WHERE advisor_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, entity.getAdvisorName().trim());
            ps.setInt(2, entity.getAdvisorId());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DataAccessException("فشل تعديل بيانات المرشد رقم " + entity.getAdvisorId() + ": " + e.getMessage(), e);
        }
    }

    @Override
    public boolean delete(int id) {
        String sql = "DELETE FROM advisor WHERE advisor_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            if ("23503".equals(e.getSQLState()) || e.getMessage().contains("violation of foreign key")) {
                throw new DataAccessException("لا يمكن حذف هذا المرشد الأكاديمي لوجود طلاب مرتبطين به.", e);
            }
            throw new DataAccessException("فشل حذف المرشد رقم " + id + ": " + e.getMessage(), e);
        }
    }

    /**
     * البحث عن مرشد أكاديمي بالاسم.
     */
    public Advisor findByName(String name) {
        if (name == null || name.trim().isEmpty()) return null;
        String sql = "SELECT advisor_id, advisor_name FROM advisor WHERE LOWER(TRIM(advisor_name)) = LOWER(TRIM(?))";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, name.trim());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapRow(rs);
                }
            }
            return null;
        } catch (SQLException e) {
            throw new DataAccessException("فشل البحث عن المرشد بالاسم '" + name + "': " + e.getMessage(), e);
        }
    }

    /**
     * البحث عن المرشد بالاسم أو إنشاؤه تلقائياً إذا لم يكن موجوداً.
     */
    public Advisor findOrCreate(String name) {
        if (name == null || name.trim().isEmpty()) return null;
        Advisor existing = findByName(name);
        if (existing != null) {
            return existing;
        }
        return insert(new Advisor(name.trim()));
    }

    /**
     * عدد الطلاب الذين يشرف عليهم هذا المرشد.
     */
    public int countStudentsUsingAdvisor(int advisorId) {
        String sql = "SELECT COUNT(*) FROM student WHERE advisor_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, advisorId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
            return 0;
        } catch (SQLException e) {
            throw new DataAccessException("فشل حساب عدد طلاب المرشد الأكاديمي: " + e.getMessage(), e);
        }
    }

    private Advisor mapRow(ResultSet rs) throws SQLException {
        return new Advisor(rs.getInt("advisor_id"), rs.getString("advisor_name"));
    }
}

