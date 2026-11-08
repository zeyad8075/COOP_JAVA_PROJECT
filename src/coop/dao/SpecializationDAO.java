package coop.dao;

import coop.exception.DataAccessException;
import coop.model.Specialization;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * فئة الوصول للبيانات لجدول التخصصات (specialization).
 * تطبق واجهة Dao<Specialization> وتوفر استعلامات التحقق والبحث بالاسم.
 */
public class SpecializationDAO implements Dao<Specialization> {

    @Override
    public Specialization findById(int id) {
        String sql = "SELECT spec_id, spec_name FROM specialization WHERE spec_id = ?";
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
            throw new DataAccessException("فشل البحث عن التخصص بالمعرف " + id + ": " + e.getMessage(), e);
        }
    }

    @Override
    public List<Specialization> findAll() {
        String sql = "SELECT spec_id, spec_name FROM specialization ORDER BY spec_name ASC";
        List<Specialization> list = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(mapRow(rs));
            }
            return list;
        } catch (SQLException e) {
            throw new DataAccessException("فشل استرجاع قائمة التخصصات: " + e.getMessage(), e);
        }
    }

    @Override
    public Specialization insert(Specialization entity) {
        String sql = "INSERT INTO specialization (spec_name) VALUES (?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, entity.getSpecName().trim());
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    entity.setSpecId(rs.getInt(1));
                }
            }
            return entity;
        } catch (SQLException e) {
            throw new DataAccessException("فشل إضافة التخصص '" + entity.getSpecName() + "': " + e.getMessage(), e);
        }
    }

    @Override
    public boolean update(Specialization entity) {
        String sql = "UPDATE specialization SET spec_name = ? WHERE spec_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, entity.getSpecName().trim());
            ps.setInt(2, entity.getSpecId());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DataAccessException("فشل تعديل التخصص رقم " + entity.getSpecId() + ": " + e.getMessage(), e);
        }
    }

    @Override
    public boolean delete(int id) {
        String sql = "DELETE FROM specialization WHERE spec_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            if ("23503".equals(e.getSQLState()) || e.getMessage().contains("violation of foreign key")) {
                throw new DataAccessException("لا يمكن حذف هذا التخصص لوجود طلاب مسجلين تحته.", e);
            }
            throw new DataAccessException("فشل حذف التخصص رقم " + id + ": " + e.getMessage(), e);
        }
    }

    /**
     * البحث عن تخصص بواسطة الاسم الدقيق.
     */
    public Specialization findByName(String name) {
        if (name == null || name.trim().isEmpty()) return null;
        String sql = "SELECT spec_id, spec_name FROM specialization WHERE LOWER(TRIM(spec_name)) = LOWER(TRIM(?))";
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
            throw new DataAccessException("فشل البحث عن التخصص بالاسم '" + name + "': " + e.getMessage(), e);
        }
    }

    /**
     * البحث عن التخصص بالاسم أو إنشاؤه تلقائياً إذا لم يكن موجوداً.
     */
    public Specialization findOrCreate(String name) {
        if (name == null || name.trim().isEmpty()) return null;
        Specialization existing = findByName(name);
        if (existing != null) {
            return existing;
        }
        return insert(new Specialization(name.trim()));
    }

    /**
     * عدد الطلاب المرتبطين بهذا التخصص.
     */
    public int countStudentsUsingSpecialization(int specId) {
        String sql = "SELECT COUNT(*) FROM student WHERE spec_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, specId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
            return 0;
        } catch (SQLException e) {
            throw new DataAccessException("فشل حساب عدد الطلاب في التخصص: " + e.getMessage(), e);
        }
    }

    private Specialization mapRow(ResultSet rs) throws SQLException {
        return new Specialization(rs.getInt("spec_id"), rs.getString("spec_name"));
    }
}

