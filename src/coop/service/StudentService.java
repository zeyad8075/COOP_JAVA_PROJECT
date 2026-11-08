package coop.service;

import coop.dao.AdvisorDAO;
import coop.dao.OrganizationDAO;
import coop.dao.SpecializationDAO;
import coop.dao.StudentDAO;
import coop.exception.ValidationException;
import coop.model.Advisor;
import coop.model.Organization;
import coop.model.Specialization;
import coop.model.Student;
import coop.util.ValidationUtil;
import java.util.List;
import java.util.Map;

/**
 * طبقة الخدمة لإدارة بيانات الطلاب (StudentService).
 * تفصل منطق الأعمال والتحقق من صحة المدخلات عن واجهات المستخدم (Swing UI) وطبقة الوصول للبيانات (DAO).
 * تضمن تطبيق نفس قواعد التحقق الصارمة في الإدخال اليدوي والاستيراد.
 */
public class StudentService {

    private final StudentDAO studentDAO;
    private final SpecializationDAO specDAO;
    private final AdvisorDAO advisorDAO;
    private final OrganizationDAO orgDAO;

    public StudentService() {
        this.studentDAO = new StudentDAO();
        this.specDAO = new SpecializationDAO();
        this.advisorDAO = new AdvisorDAO();
        this.orgDAO = new OrganizationDAO();
    }

    public StudentService(StudentDAO studentDAO, SpecializationDAO specDAO, AdvisorDAO advisorDAO, OrganizationDAO orgDAO) {
        this.studentDAO = studentDAO;
        this.specDAO = specDAO;
        this.advisorDAO = advisorDAO;
        this.orgDAO = orgDAO;
    }

    /**
     * إضافة طالب جديد مع التحقق الشامل من صحة المدخلات ووجود الكيانات المرجعية.
     */
    public Student addStudent(String fullName, String section, int specId, int advisorId, int orgId) throws ValidationException {
        validateStudentInput(fullName, section, specId, advisorId, orgId);

        String cleanedName = ValidationUtil.clean(fullName);
        String cleanedSection = ValidationUtil.clean(section);

        Specialization spec = specDAO.findById(specId);
        if (spec == null) {
            throw new ValidationException("التخصص المختار غير موجود في النظام.");
        }

        Advisor advisor = advisorDAO.findById(advisorId);
        if (advisor == null) {
            throw new ValidationException("المرشد الأكاديمي المختار غير موجود في النظام.");
        }

        Organization org = orgDAO.findById(orgId);
        if (org == null) {
            throw new ValidationException("جهة التدريب المختارة غير موجودة في النظام.");
        }

        Student student = new Student(cleanedName, cleanedSection, spec, advisor, org);
        return studentDAO.insert(student);
    }

    /**
     * تعديل بيانات طالب موجود.
     */
    public boolean updateStudent(int studentId, String fullName, String section, int specId, int advisorId, int orgId) throws ValidationException {
        if (studentId <= 0) {
            throw new ValidationException("رقم معرف الطالب غير صالح للتعديل.");
        }

        validateStudentInput(fullName, section, specId, advisorId, orgId);

        String cleanedName = ValidationUtil.clean(fullName);
        String cleanedSection = ValidationUtil.clean(section);

        Specialization spec = specDAO.findById(specId);
        if (spec == null) {
            throw new ValidationException("التخصص المختار غير موجود في النظام.");
        }

        Advisor advisor = advisorDAO.findById(advisorId);
        if (advisor == null) {
            throw new ValidationException("المرشد الأكاديمي المختار غير موجود في النظام.");
        }

        Organization org = orgDAO.findById(orgId);
        if (org == null) {
            throw new ValidationException("جهة التدريب المختارة غير موجودة في النظام.");
        }

        Student student = new Student(studentId, cleanedName, cleanedSection, spec, advisor, org);
        return studentDAO.update(student);
    }

    /**
     * حذف طالب بالمعرف.
     */
    public boolean deleteStudent(int studentId) throws ValidationException {
        if (studentId <= 0) {
            throw new ValidationException("رقم معرف الطالب غير صالح للحذف.");
        }
        return studentDAO.delete(studentId);
    }

    /**
     * استرجاع جميع الطلاب مع الكيانات المرتبطة.
     */
    public List<Student> getAllStudents() {
        return studentDAO.findAll();
    }

    /**
     * استرجاع طالب بواسطة معرفه الأساسي.
     */
    public Student getStudentById(int id) {
        return studentDAO.findById(id);
    }

    /**
     * البحث المركب والتصفية المجمعة.
     */
    public List<Student> searchAndFilter(String nameQuery, Integer specId, Integer advisorId, Integer orgId, String section) {
        return studentDAO.searchAndFilter(nameQuery, specId, advisorId, orgId, section);
    }

    /**
     * إجمالي عدد الطلاب في النظام.
     */
    public int getTotalStudentCount() {
        return studentDAO.countAll();
    }

    /**
     * فحص ما إذا كان اسم الطالب مسجلاً مسبقاً (للتحذير من التكرار).
     */
    public boolean isStudentNameExists(String name) {
        return studentDAO.existsByFullName(name);
    }

    /**
     * إحصائيات توزيع الطلاب على جهات التدريب.
     */
    public Map<String, Integer> getStatisticsByOrganization() {
        return studentDAO.countStudentsByOrganization();
    }

    /**
     * إحصائيات توزيع الطلاب على التخصصات.
     */
    public Map<String, Integer> getStatisticsBySpecialization() {
        return studentDAO.countStudentsBySpecialization();
    }

    /**
     * إحصائيات توزيع الطلاب على الشطر.
     */
    public Map<String, Integer> getStatisticsBySection() {
        return studentDAO.countStudentsBySection();
    }

    /**
     * قواعد التحقق الأساسية المشتركة.
     */
    private void validateStudentInput(String fullName, String section, int specId, int advisorId, int orgId) throws ValidationException {
        if (ValidationUtil.isEmpty(fullName)) {
            throw new ValidationException("حقل اسم الطالب مطلوب ولا يمكن أن يكون فارغاً.");
        }
        if (!ValidationUtil.isValidName(fullName)) {
            throw new ValidationException("اسم الطالب المدخل قصير جداً، يجب إدخال اسم ثلاثي أو رباعي واضح.");
        }
        if (ValidationUtil.isEmpty(section)) {
            throw new ValidationException("يرجى تحديد الشطر (طالب / طالبة).");
        }
        if (specId <= 0) {
            throw new ValidationException("يرجى اختيار التخصص الأكاديمي.");
        }
        if (advisorId <= 0) {
            throw new ValidationException("يرجى اختيار المرشد الأكاديمي.");
        }
        if (orgId <= 0) {
            throw new ValidationException("يرجى اختيار جهة التدريب الميداني.");
        }
    }
}

