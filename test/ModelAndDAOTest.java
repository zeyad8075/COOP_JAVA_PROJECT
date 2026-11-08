package test;

import coop.dao.AdvisorDAO;
import coop.dao.DBConnection;
import coop.dao.OrganizationDAO;
import coop.dao.SpecializationDAO;
import coop.dao.StudentDAO;
import coop.model.Advisor;
import coop.model.Organization;
import coop.model.Person;
import coop.model.Specialization;
import coop.model.Student;
import java.util.List;
import java.util.Map;

/**
 * اختبارات المرحلة الثالثة: التحقق من كلاسات الـ Model وتطبيق الوراثة والتغليف،
 * وعمليات الـ DAO الأربعة (SpecializationDAO, AdvisorDAO, OrganizationDAO, StudentDAO).
 */
public class ModelAndDAOTest {

    public static void main(String[] args) {
        System.out.println("==================================================");
        System.out.println("بدء اختبارات المرحلة الثالثة: كلاسات Model و DAO");
        System.out.println("==================================================");

        int passed = 0;
        int failed = 0;

        // 1. اختبار مفاهيم OOP في الـ Model
        try {
            Student s = new Student();
            s.setId(10);
            s.setName("سارة الأحمدي");
            s.setStudentSection("طالبة");

            // فحص الوراثة
            if (!(s instanceof Person)) {
                throw new RuntimeException("Student يجب أن يرث من Person!");
            }
            if (s.getStudentId() != 10 || !"سارة الأحمدي".equals(s.getFullName())) {
                throw new RuntimeException("فشل دوال الوصول والتغليف (Getters/Setters)!");
            }

            Advisor a = new Advisor(5, "د. خالد السالم");
            if (!(a instanceof Person) || a.getAdvisorId() != 5) {
                throw new RuntimeException("Advisor يجب أن يرث من Person!");
            }

            System.out.println("[اختبار 1] نجاح التحقق من كلاسات Model وتطبيق الوراثة والتغليف (Inheritance & Encapsulation).");
            passed++;
        } catch (Exception e) {
            System.err.println("[اختبار 1 فشل] " + e.getMessage());
            failed++;
        }

        // 2. اختبار SpecializationDAO
        SpecializationDAO specDAO = new SpecializationDAO();
        Specialization specMIS = null;
        try {
            specMIS = specDAO.findOrCreate("نظم المعلومات الإدارية - اختبار");
            if (specMIS == null || specMIS.getSpecId() <= 0) {
                throw new RuntimeException("فشل إدخال أو استرجاع التخصص!");
            }

            // فحص findByName
            Specialization found = specDAO.findByName("نظم المعلومات الإدارية - اختبار");
            if (found == null || found.getSpecId() != specMIS.getSpecId()) {
                throw new RuntimeException("فشل البحث بالاسم في SpecializationDAO!");
            }

            System.out.println("[اختبار 2] نجاح عمليات SpecializationDAO (إدخال وبحث بالاسم).");
            passed++;
        } catch (Exception e) {
            System.err.println("[اختبار 2 فشل] SpecializationDAO: " + e.getMessage());
            failed++;
        }

        // 3. اختبار AdvisorDAO
        AdvisorDAO advisorDAO = new AdvisorDAO();
        Advisor advisorTest = null;
        try {
            advisorTest = advisorDAO.findOrCreate("د. فهد البلوي - اختبار");
            if (advisorTest == null || advisorTest.getAdvisorId() <= 0) {
                throw new RuntimeException("فشل إدخال أو استرجاع المرشد الأكاديمي!");
            }

            Advisor found = advisorDAO.findByName("د. فهد البلوي - اختبار");
            if (found == null || found.getAdvisorId() != advisorTest.getAdvisorId()) {
                throw new RuntimeException("فشل البحث بالاسم في AdvisorDAO!");
            }

            System.out.println("[اختبار 3] نجاح عمليات AdvisorDAO (إدخال وبحث بالاسم).");
            passed++;
        } catch (Exception e) {
            System.err.println("[اختبار 3 فشل] AdvisorDAO: " + e.getMessage());
            failed++;
        }

        // 4. اختبار OrganizationDAO
        OrganizationDAO orgDAO = new OrganizationDAO();
        Organization orgRC = null;
        try {
            orgRC = orgDAO.findOrCreate("الهيئة الملكية بينبع - اختبار");
            if (orgRC == null || orgRC.getOrgId() <= 0) {
                throw new RuntimeException("فشل إدخال أو استرجاع جهة التدريب!");
            }

            Organization found = orgDAO.findByName("الهيئة الملكية بينبع - اختبار");
            if (found == null || found.getOrgId() != orgRC.getOrgId()) {
                throw new RuntimeException("فشل البحث بالاسم في OrganizationDAO!");
            }

            System.out.println("[اختبار 4] نجاح عمليات OrganizationDAO (إدخال وبحث بالاسم).");
            passed++;
        } catch (Exception e) {
            System.err.println("[اختبار 4 فشل] OrganizationDAO: " + e.getMessage());
            failed++;
        }

        // 5. اختبار StudentDAO (CRUD، البحث المركب، والتصفية)
        StudentDAO studentDAO = new StudentDAO();
        Student student1 = null;
        Student student2 = null;
        try {
            student1 = new Student("عبدالله أحمد الصبحي", "طالب", specMIS, advisorTest, orgRC);
            student1 = studentDAO.insert(student1);

            student2 = new Student("نورة بنت صالح الجهني", "طالبة", specMIS, advisorTest, orgRC);
            student2 = studentDAO.insert(student2);

            if (student1.getStudentId() <= 0 || student2.getStudentId() <= 0) {
                throw new RuntimeException("فشل إدخال الطلاب عبر StudentDAO!");
            }

            // قراءة الطالب بالمعرف والتحقق من الربط
            Student fetched = studentDAO.findById(student1.getStudentId());
            if (fetched == null || !student1.getFullName().equals(fetched.getFullName()) ||
                !specMIS.getSpecName().equals(fetched.getSpecializationName())) {
                throw new RuntimeException("فشل استرجاع بيانات الطالب بالربط!");
            }

            // تعديل بيانات الطالب
            fetched.setFullName("عبدالله بن أحمد الصبحي (معدل)");
            boolean updated = studentDAO.update(fetched);
            if (!updated) {
                throw new RuntimeException("فشل تعديل سجل الطالب في StudentDAO!");
            }

            System.out.println("[اختبار 5] نجاح عمليات StudentDAO (إضافة، استرجاع بالربط، وتعديل).");
            passed++;
        } catch (Exception e) {
            System.err.println("[اختبار 5 فشل] StudentDAO CRUD: " + e.getMessage());
            failed++;
        }

        // 6. اختبار البحث الجزئي والتصفية المركبة
        try {
            // بحث جزئي بالاسم
            List<Student> results1 = studentDAO.searchAndFilter("الصبحي", null, null, null, null);
            if (results1.isEmpty()) {
                throw new RuntimeException("البحث الجزئي بالاسم لم يجد الطالب!");
            }

            // تصفية مركبة: بالتخصص والشطر
            List<Student> results2 = studentDAO.searchAndFilter(null, specMIS.getSpecId(), null, null, "طالبة");
            if (results2.isEmpty() || !results2.get(0).getFullName().contains("نورة")) {
                throw new RuntimeException("التصفية المركبة بالتخصص والشطر لم تُرجع النتائج الصحيحة!");
            }

            System.out.println("[اختبار 6] نجاح البحث الجزئي بالاسم والتصفية المركبة (Compound Search & Filter).");
            passed++;
        } catch (Exception e) {
            System.err.println("[اختبار 6 فشل] البحث والتصفية: " + e.getMessage());
            failed++;
        }

        // 7. اختبار الإحصائيات (Group by counts)
        try {
            Map<String, Integer> orgStats = studentDAO.countStudentsByOrganization();
            Map<String, Integer> specStats = studentDAO.countStudentsBySpecialization();
            Map<String, Integer> sectionStats = studentDAO.countStudentsBySection();

            if (!orgStats.containsKey(orgRC.getOrgName()) || orgStats.get(orgRC.getOrgName()) != 2) {
                throw new RuntimeException("إحصائيات جهات التدريب غير دقيقة!");
            }
            if (!specStats.containsKey(specMIS.getSpecName()) || specStats.get(specMIS.getSpecName()) != 2) {
                throw new RuntimeException("إحصائيات التخصصات غير دقيقة!");
            }
            if (!sectionStats.containsKey("طالب") || !sectionStats.containsKey("طالبة")) {
                throw new RuntimeException("إحصائيات الشطر غير دقيقة!");
            }

            System.out.println("[اختبار 7] نجاح استخراج الإحصائيات التحليلية (حسب الجهة والتخصص والشطر):");
            System.out.println("           توزيع الجهات: " + orgStats);
            System.out.println("           توزيع التخصصات: " + specStats);
            System.out.println("           توزيع الشطر: " + sectionStats);
            passed++;
        } catch (Exception e) {
            System.err.println("[اختبار 7 فشل] الإحصائيات: " + e.getMessage());
            failed++;
        }

        // 8. تنظيف السجلات التجريبية
        try {
            if (student1 != null && student1.getStudentId() > 0) studentDAO.delete(student1.getStudentId());
            if (student2 != null && student2.getStudentId() > 0) studentDAO.delete(student2.getStudentId());
            if (orgRC != null && orgRC.getOrgId() > 0) orgDAO.delete(orgRC.getOrgId());
            if (advisorTest != null && advisorTest.getAdvisorId() > 0) advisorDAO.delete(advisorTest.getAdvisorId());
            if (specMIS != null && specMIS.getSpecId() > 0) specDAO.delete(specMIS.getSpecId());

            System.out.println("[اختبار 8] نجاح تنظيف بيانات الاختبار وإعادة قاعدة البيانات نظيفة تماماً.");
            passed++;
        } catch (Exception e) {
            System.err.println("[اختبار 8 فشل] تنظيف البيانات: " + e.getMessage());
            failed++;
        }

        DBConnection.shutdown();

        System.out.println("==================================================");
        System.out.printf("نتيجة اختبارات المرحلة الثالثة: %d ناجح | %d فاشل%n", passed, failed);
        System.out.println("==================================================");

        if (failed > 0) {
            System.exit(1);
        }
    }
}

