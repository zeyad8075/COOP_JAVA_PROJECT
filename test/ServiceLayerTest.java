package test;

import coop.dao.DBConnection;
import coop.exception.ValidationException;
import coop.model.Advisor;
import coop.model.Organization;
import coop.model.Specialization;
import coop.model.Student;
import coop.service.ReferenceDataService;
import coop.service.StudentService;

/**
 * اختبارات المرحلة الخامسة: طبقة الخدمات (Service Layer).
 * التحقق من قواعد التحقق الصارمة، معالجة الأخطاء عبر ValidationException،
 * ومنع حذف السجلات المرجعية المرتبطة بطلاب برسائل واضحة.
 */
public class ServiceLayerTest {

    public static void main(String[] args) {
        System.out.println("==================================================");
        System.out.println("بدء اختبارات المرحلة الخامسة: طبقة الخدمات (Services)");
        System.out.println("==================================================");

        int passed = 0;
        int failed = 0;

        ReferenceDataService refService = new ReferenceDataService();
        StudentService studentService = new StudentService();

        Specialization spec = null;
        Advisor advisor = null;
        Organization org = null;
        Student student = null;

        // اختبار 1: التحقق من رفض إدخال طالب باسم فارغ (ValidationException)
        try {
            studentService.addStudent("", "طالب", 1, 1, 1);
            System.err.println("[اختبار 1 فشل] تم قبول اسم فارغ دون اعتراض!");
            failed++;
        } catch (ValidationException e) {
            System.out.println("[اختبار 1] نجاح التحقق من منع الاسم الفارغ: " + e.getMessage());
            passed++;
        } catch (Exception e) {
            System.err.println("[اختبار 1 فشل] حدث استثناء غير متوقع: " + e.getMessage());
            failed++;
        }

        // اختبار 2: إضافة مراجع جديدة عبر ReferenceDataService
        try {
            spec = refService.addSpecialization("إدارة الموارد البشرية - خدمة");
            advisor = refService.addAdvisor("د. ماجد الحربي - خدمة");
            org = refService.addOrganization("ميناء ينبع التجاري - خدمة");

            if (spec.getSpecId() <= 0 || advisor.getAdvisorId() <= 0 || org.getOrgId() <= 0) {
                throw new RuntimeException("فشل توليد المعرفات للمراجع المضافة!");
            }

            System.out.println("[اختبار 2] نجاح إضافة السجلات المرجعية عبر ReferenceDataService:");
            System.out.printf("           تخصص: %s | مرشد: %s | جهة: %s%n",
                    spec.getSpecName(), advisor.getAdvisorName(), org.getOrgName());
            passed++;
        } catch (Exception e) {
            System.err.println("[اختبار 2 فشل] ReferenceDataService Add: " + e.getMessage());
            failed++;
        }

        // اختبار 3: إضافة طالب جديد والتحقق من العمليات المجمعة عبر StudentService
        try {
            student = studentService.addStudent("عمر خالد الصبحي", "طالب",
                    spec.getSpecId(), advisor.getAdvisorId(), org.getOrgId());

            if (student.getStudentId() <= 0 || !"عمر خالد الصبحي".equals(student.getFullName())) {
                throw new RuntimeException("فشل إضافة الطالب عبر StudentService!");
            }

            // تعديل بيانات الطالب
            boolean updated = studentService.updateStudent(student.getStudentId(),
                    "عمر بن خالد الصبحي", "طالب",
                    spec.getSpecId(), advisor.getAdvisorId(), org.getOrgId());
            if (!updated) {
                throw new RuntimeException("فشل تعديل بيانات الطالب عبر StudentService!");
            }

            System.out.println("[اختبار 3] نجاح إضافة وتعديل بيانات الطالب عبر StudentService.");
            passed++;
        } catch (Exception e) {
            System.err.println("[اختبار 3 فشل] StudentService Add/Update: " + e.getMessage());
            failed++;
        }

        // اختبار 4: التحقق من حماية السجلات المرجعية من الحذف برسائل واضحة
        try {
            // محاولة حذف جهة التدريب وهي مرتبطة بالطالب عمر
            refService.deleteOrganization(org.getOrgId());
            System.err.println("[اختبار 4 فشل] تم حذف جهة التدريب رغم وجود طالب مرتبط بها!");
            failed++;
        } catch (ValidationException e) {
            System.out.println("[اختبار 4] نجاح منع حذف جهة التدريب المرتبطة وإظهار رسالة واضحة: " + e.getMessage());
            passed++;
        } catch (Exception e) {
            System.err.println("[اختبار 4 فشل] استثناء غير متوقع في منع الحذف: " + e.getMessage());
            failed++;
        }

        // اختبار 5: حذف الطالب ثم حذف المراجع بأمان
        try {
            // حذف الطالب أولاً
            boolean deletedStudent = studentService.deleteStudent(student.getStudentId());
            if (!deletedStudent) {
                throw new RuntimeException("فشل حذف الطالب!");
            }

            // الآن يمكن حذف المراجع دون استثناء
            boolean delOrg = refService.deleteOrganization(org.getOrgId());
            boolean delAdvisor = refService.deleteAdvisor(advisor.getAdvisorId());
            boolean delSpec = refService.deleteSpecialization(spec.getSpecId());

            if (!delOrg || !delAdvisor || !delSpec) {
                throw new RuntimeException("فشل حذف المراجع بعد إزالة الارتباط!");
            }

            System.out.println("[اختبار 5] نجاح حذف الطالب ثم حذف المراجع بأمان بعد زوال الارتباط.");
            passed++;
        } catch (Exception e) {
            System.err.println("[اختبار 5 فشل] Clean up: " + e.getMessage());
            failed++;
        } finally {
            DBConnection.shutdown();
        }

        System.out.println("==================================================");
        System.out.printf("نتيجة اختبارات المرحلة الخامسة: %d ناجح | %d فاشل%n", passed, failed);
        System.out.println("==================================================");

        if (failed > 0) {
            System.exit(1);
        }
    }
}

