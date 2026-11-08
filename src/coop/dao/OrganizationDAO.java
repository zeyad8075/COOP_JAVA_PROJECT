package coop.dao;

import coop.exception.DataAccessException;
import coop.model.Organization;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * فئة الوصول للبيانات لجدول جهات التدريب (organization).
 * تطبق واجهة Dao<Organization> وتدعم البحث بالاسم والإنشاء التلقائي وفحص الارتباط.
 */
public class OrganizationDAO implements Dao<Organization> {

    @Override
    public Organization findById(int id) {
        String sql = "SELECT org_id, org_name FROM organization WHERE org_id = ?";
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
            throw new DataAccessException("فشل البحث عن جهة التدريب بالمعرف " + id + ": " + e.getMessage(), e);
        }
    }

    @Override
    public List<Organization> findAll() {
        String sql = "SELECT org_id, org_name FROM organization ORDER BY org_name ASC";
        List<Organization> list = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(mapRow(rs));
            }
            return list;
        } catch (SQLException e) {
            throw new DataAccessException("فشل استرجاع قائمة جهات التدريب: " + e.getMessage(), e);
        }
    }

    @Override
    public Organization insert(Organization entity) {
        String sql = "INSERT INTO organization (org_name) VALUES (?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, entity.getOrgName().trim());
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    entity.setOrgId(rs.getInt(1));
                }
            }
            return entity;
        } catch (SQLException e) {
            throw new DataAccessException("فشل إضافة جهة التدريب '" + entity.getOrgName() + "': " + e.getMessage(), e);
        }
    }

    @Override
    public boolean update(Organization entity) {
        String sql = "UPDATE organization SET org_name = ? WHERE org_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, entity.getOrgName().trim());
            ps.setInt(2, entity.getOrgId());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DataAccessException("فشل تعديل جهة التدريب رقم " + entity.getOrgId() + ": " + e.getMessage(), e);
        }
    }

    @Override
    public boolean delete(int id) {
        String sql = "DELETE FROM organization WHERE org_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            if ("23503".equals(e.getSQLState()) || e.getMessage().contains("violation of foreign key")) {
                throw new DataAccessException("لا يمكن حذف جهة التدريب هذه لوجود طلاب متدربين مسجلين بها.", e);
            }
            throw new DataAccessException("فشل حذف جهة التدريب رقم " + id + ": " + e.getMessage(), e);
        }
    }

    /**
     * البحث عن جهة تدريب بالاسم.
     */
    public Organization findByName(String name) {
        if (name == null || name.trim().isEmpty()) return null;
        String sql = "SELECT org_id, org_name FROM organization WHERE LOWER(TRIM(org_name)) = LOWER(TRIM(?))";
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
            throw new DataAccessException("فشل البحث عن جهة التدريب بالاسم '" + name + "': " + e.getMessage(), e);
        }
    }

    /**
     * البحث عن جهة التدريب بالاسم أو إنشاؤها تلقائياً إذا لم تكن موجودة.
     */
    public Organization findOrCreate(String name) {
        if (name == null || name.trim().isEmpty()) return null;
        Organization existing = findByName(name);
        if (existing != null) {
            return existing;
        }
        return insert(new Organization(name.trim()));
    }

    /**
     * عدد الطلاب المسجلين في جهة التدريب هذه.
     */
    public int countStudentsUsingOrganization(int orgId) {
        String sql = "SELECT COUNT(*) FROM student WHERE org_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, orgId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
            return 0;
        } catch (SQLException e) {
            throw new DataAccessException("فشل حساب عدد طلاب جهة التدريب: " + e.getMessage(), e);
        }
    }

    private Organization mapRow(ResultSet rs) throws SQLException {
        return new Organization(rs.getInt("org_id"), rs.getString("org_name"));
    }
}

