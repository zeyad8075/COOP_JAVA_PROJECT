package coop.model;

import java.util.Objects;

/**
 * فئة تمثل الطالب المتدرب (Student).
 * ترث من الفئة المجردة Person وتجسد مفهوم الوراثة والتركيب (Composition) والتغليف.
 */
public class Student extends Person {

    private String studentSection;
    private Specialization specialization;
    private Advisor advisor;
    private Organization organization;

    public Student() {
        super();
    }

    public Student(int id, String fullName, String studentSection,
                   Specialization specialization, Advisor advisor, Organization organization) {
        super(id, fullName);
        this.studentSection = studentSection;
        this.specialization = specialization;
        this.advisor = advisor;
        this.organization = organization;
    }

    public Student(String fullName, String studentSection,
                   Specialization specialization, Advisor advisor, Organization organization) {
        super(fullName);
        this.studentSection = studentSection;
        this.specialization = specialization;
        this.advisor = advisor;
        this.organization = organization;
    }

    // دوال تيسيرية للوصول للمعرف والاسم
    public int getStudentId() {
        return getId();
    }

    public void setStudentId(int studentId) {
        setId(studentId);
    }

    public String getFullName() {
        return getName();
    }

    public void setFullName(String fullName) {
        setName(fullName);
    }

    public String getStudentSection() {
        return studentSection;
    }

    public void setStudentSection(String studentSection) {
        this.studentSection = studentSection;
    }

    public Specialization getSpecialization() {
        return specialization;
    }

    public void setSpecialization(Specialization specialization) {
        this.specialization = specialization;
    }

    public Advisor getAdvisor() {
        return advisor;
    }

    public void setAdvisor(Advisor advisor) {
        this.advisor = advisor;
    }

    public Organization getOrganization() {
        return organization;
    }

    public void setOrganization(Organization organization) {
        this.organization = organization;
    }

    // دوال تيسيرية للحصول على المعرفات المرتبطة
    public int getSpecId() {
        return specialization != null ? specialization.getSpecId() : 0;
    }

    public int getAdvisorId() {
        return advisor != null ? advisor.getAdvisorId() : 0;
    }

    public int getOrgId() {
        return organization != null ? organization.getOrgId() : 0;
    }

    // دوال تيسيرية للحصول على الأسماء المرجعية
    public String getSpecializationName() {
        return specialization != null ? specialization.getSpecName() : "";
    }

    public String getAdvisorName() {
        return advisor != null ? advisor.getAdvisorName() : "";
    }

    public String getOrganizationName() {
        return organization != null ? organization.getOrgName() : "";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Student)) return false;
        if (!super.equals(o)) return false;
        Student student = (Student) o;
        return Objects.equals(studentSection, student.studentSection) &&
               Objects.equals(specialization, student.specialization) &&
               Objects.equals(advisor, student.advisor) &&
               Objects.equals(organization, student.organization);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), studentSection, specialization, advisor, organization);
    }

    @Override
    public String toString() {
        return String.format("Student{id=%d, name='%s', section='%s', spec='%s', advisor='%s', org='%s'}",
                getStudentId(), getFullName(), studentSection,
                getSpecializationName(), getAdvisorName(), getOrganizationName());
    }
}

