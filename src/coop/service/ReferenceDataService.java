package coop.service;

import coop.dao.AdvisorDAO;
import coop.dao.OrganizationDAO;
import coop.dao.SpecializationDAO;
import coop.exception.ValidationException;
import coop.model.Advisor;
import coop.model.Organization;
import coop.model.Specialization;
import coop.util.ValidationUtil;
import java.util.List;

/**
 * طبقة الخدمة لإدارة البيانات المرجعية (التخصصات، المرشدين، جهات التدريب).
 * تضمن حماية السجلات المرجعية من الحذف إذا كانت مستخدمة ومرتبطة بطلاب،
 * وتوفر رسائل تنبيه مفهومة للمستخدم.
 */
public class ReferenceDataService {

    private final SpecializationDAO specDAO;
    private final AdvisorDAO advisorDAO;
    private final OrganizationDAO orgDAO;

    public ReferenceDataService() {
        this.specDAO = new SpecializationDAO();
        this.advisorDAO = new AdvisorDAO();
        this.orgDAO = new OrganizationDAO();
    }

    public ReferenceDataService(SpecializationDAO specDAO, AdvisorDAO advisorDAO, OrganizationDAO orgDAO) {
        this.specDAO = specDAO;
        this.advisorDAO = advisorDAO;
        this.orgDAO = orgDAO;
    }

    // ==================== التخصصات (Specializations) ====================

    public List<Specialization> getAllSpecializations() {
        return specDAO.findAll();
    }

    public Specialization addSpecialization(String name) throws ValidationException {
        String cln = ValidationUtil.clean(name);
        if (ValidationUtil.isEmpty(cln)) {
            throw new ValidationException("اسم التخصص الأكاديمي مطلوب ولا يمكن تركه فارغاً.");
        }
        if (specDAO.findByName(cln) != null) {
            throw new ValidationException("التخصص '" + cln + "' مسجل مسبقاً في النظام.");
        }
        return specDAO.insert(new Specialization(cln));
    }

    public boolean updateSpecialization(int id, String newName) throws ValidationException {
        String cln = ValidationUtil.clean(newName);
        if (ValidationUtil.isEmpty(cln)) {
            throw new ValidationException("اسم التخصص الجديد لا يمكن أن يكون فارغاً.");
        }
        Specialization existing = specDAO.findByName(cln);
        if (existing != null && existing.getSpecId() != id) {
            throw new ValidationException("يوجد تخصص آخر مسجل بالاسم نفسه: " + cln);
        }
        return specDAO.update(new Specialization(id, cln));
    }

    public boolean deleteSpecialization(int id) throws ValidationException {
        int studentCount = specDAO.countStudentsUsingSpecialization(id);
        if (studentCount > 0) {
            throw new ValidationException(String.format("لا يمكن حذف هذا التخصص؛ يوجد حالياً %d طلاب مسجلين تحته.", studentCount));
        }
        return specDAO.delete(id);
    }

    // ==================== المرشدون الأكاديميون (Advisors) ====================

    public List<Advisor> getAllAdvisors() {
        return advisorDAO.findAll();
    }

    public Advisor addAdvisor(String name) throws ValidationException {
        String cln = ValidationUtil.clean(name);
        if (ValidationUtil.isEmpty(cln)) {
            throw new ValidationException("اسم المرشد الأكاديمي مطلوب ولا يمكن تركه فارغاً.");
        }
        if (advisorDAO.findByName(cln) != null) {
            throw new ValidationException("المرشد '" + cln + "' مسجل مسبقاً في النظام.");
        }
        return advisorDAO.insert(new Advisor(cln));
    }

    public boolean updateAdvisor(int id, String newName) throws ValidationException {
        String cln = ValidationUtil.clean(newName);
        if (ValidationUtil.isEmpty(cln)) {
            throw new ValidationException("اسم المرشد الجديد لا يمكن أن يكون فارغاً.");
        }
        Advisor existing = advisorDAO.findByName(cln);
        if (existing != null && existing.getAdvisorId() != id) {
            throw new ValidationException("يوجد مرشد أكاديمي آخر مسجل بالاسم نفسه: " + cln);
        }
        return advisorDAO.update(new Advisor(id, cln));
    }

    public boolean deleteAdvisor(int id) throws ValidationException {
        int studentCount = advisorDAO.countStudentsUsingAdvisor(id);
        if (studentCount > 0) {
            throw new ValidationException(String.format("لا يمكن حذف هذا المرشد الأكاديمي؛ يشرف حالياً على %d طلاب.", studentCount));
        }
        return advisorDAO.delete(id);
    }

    // ==================== جهات التدريب (Organizations) ====================

    public List<Organization> getAllOrganizations() {
        return orgDAO.findAll();
    }

    public Organization addOrganization(String name) throws ValidationException {
        String cln = ValidationUtil.clean(name);
        if (ValidationUtil.isEmpty(cln)) {
            throw new ValidationException("اسم جهة التدريب مطلوب ولا يمكن تركه فارغاً.");
        }
        if (orgDAO.findByName(cln) != null) {
            throw new ValidationException("جهة التدريب '" + cln + "' مسجلة مسبقاً في النظام.");
        }
        return orgDAO.insert(new Organization(cln));
    }

    public boolean updateOrganization(int id, String newName) throws ValidationException {
        String cln = ValidationUtil.clean(newName);
        if (ValidationUtil.isEmpty(cln)) {
            throw new ValidationException("اسم جهة التدريب الجديد لا يمكن أن يكون فارغاً.");
        }
        Organization existing = orgDAO.findByName(cln);
        if (existing != null && existing.getOrgId() != id) {
            throw new ValidationException("توجد جهة تدريب أخرى مسجلة بالاسم نفسه: " + cln);
        }
        return orgDAO.update(new Organization(id, cln));
    }

    public boolean deleteOrganization(int id) throws ValidationException {
        int studentCount = orgDAO.countStudentsUsingOrganization(id);
        if (studentCount > 0) {
            throw new ValidationException(String.format("لا يمكن حذف جهة التدريب هذه؛ يوجد حالياً %d طلاب متدربين ملتحقين بها.", studentCount));
        }
        return orgDAO.delete(id);
    }
}

