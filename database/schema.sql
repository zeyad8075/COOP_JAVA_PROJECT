-- ============================================================================
-- نظام إدارة التدريب الميداني (COOP) - كلية إدارة الأعمال بينبع (جامعة طيبة)
-- مقرر برمجة الأعمال المتقدمة (MIS412)
-- سكربت إنشاء جداول قاعدة البيانات (Apache Derby Embedded Database)
-- متوافق مع مبادئ التطبيع حتى المستوى الثالث (3NF)
-- ============================================================================

-- 1. جدول التخصصات الأكاديمية (specialization)
CREATE TABLE specialization (
    spec_id INT GENERATED ALWAYS AS IDENTITY (START WITH 1, INCREMENT BY 1),
    spec_name VARCHAR(100) NOT NULL,
    CONSTRAINT pk_specialization PRIMARY KEY (spec_id),
    CONSTRAINT uq_spec_name UNIQUE (spec_name)
);

-- 2. جدول المرشدين الأكاديميين (advisor)
CREATE TABLE advisor (
    advisor_id INT GENERATED ALWAYS AS IDENTITY (START WITH 1, INCREMENT BY 1),
    advisor_name VARCHAR(150) NOT NULL,
    CONSTRAINT pk_advisor PRIMARY KEY (advisor_id),
    CONSTRAINT uq_advisor_name UNIQUE (advisor_name)
);

-- 3. جدول جهات ومؤسسات التدريب الميداني (organization)
CREATE TABLE organization (
    org_id INT GENERATED ALWAYS AS IDENTITY (START WITH 1, INCREMENT BY 1),
    org_name VARCHAR(200) NOT NULL,
    CONSTRAINT pk_organization PRIMARY KEY (org_id),
    CONSTRAINT uq_org_name UNIQUE (org_name)
);

-- 4. جدول بيانات الطلاب المتدربين (student)
CREATE TABLE student (
    student_id INT GENERATED ALWAYS AS IDENTITY (START WITH 1, INCREMENT BY 1),
    full_name VARCHAR(150) NOT NULL,
    student_section VARCHAR(50) NOT NULL,
    spec_id INT NOT NULL,
    advisor_id INT NOT NULL,
    org_id INT NOT NULL,
    CONSTRAINT pk_student PRIMARY KEY (student_id),
    CONSTRAINT fk_student_spec FOREIGN KEY (spec_id) 
        REFERENCES specialization(spec_id) ON DELETE RESTRICT,
    CONSTRAINT fk_student_advisor FOREIGN KEY (advisor_id) 
        REFERENCES advisor(advisor_id) ON DELETE RESTRICT,
    CONSTRAINT fk_student_org FOREIGN KEY (org_id) 
        REFERENCES organization(org_id) ON DELETE RESTRICT
);

-- فهارس لتحسين أداء البحث والاستعلام والتصفية
CREATE INDEX idx_student_name ON student(full_name);
CREATE INDEX idx_student_spec ON student(spec_id);
CREATE INDEX idx_student_advisor ON student(advisor_id);
CREATE INDEX idx_student_org ON student(org_id);

