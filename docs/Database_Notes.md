# مذكرة التصميم الفني لقاعدة البيانات (Database Notes)

**نظام إدارة التدريب الميداني (COOP) — كلية إدارة الأعمال بينبع — جامعة طيبة**

---

## 1. نوع محرك قاعدة البيانات المستخدم

- **المحرك:** Apache Derby (المعروف أكاديمياً بـ Java DB المدمج مع بيئة NetBeans).
- **وضع التشغيل المعتمد:** **Embedded Mode**.
- **رابط الاتصال (JDBC URL):** `jdbc:derby:database/coop_db;create=true`.
- **مشغل قاعدة البيانات (JDBC Driver):** `org.apache.derby.jdbc.EmbeddedDriver`.

### مبررات اختيار الوضع المضمّن (Embedded):
1. **سهولة النقل والتسليم:** تخزن قاعدة البيانات كملفات محلية داخل مجلد `database/coop_db` داخل المشروع، مما يسمح بنسخ مجلد المشروع بالكامل إلى فلاش ميموري أو جهاز آخر وتشغيله فوراً دون الحاجة لتثبيت برامج خوادم (مثل MySQL أو SQL Server).
2. **الاستقلالية في المناقشة الأكاديمية:** يعمل النظام فور الضغط على Run في NetBeans، دون الاعتماد على تشغيل خدمة الشبكة أو منافذ الاتصال (Ports).
3. **الدعم الكامل للغة العربية:** يدعم محرك Derby ترميز UTF-8 لتخزين واسترجاع النصوص العربية والبحث فيها بدقة.

---

## 2. تفاصيل الجداول وأنواع البيانات

### 2.1 جدول `specialization` (التخصصات)
```sql
CREATE TABLE specialization (
    spec_id INT GENERATED ALWAYS AS IDENTITY (START WITH 1, INCREMENT BY 1),
    spec_name VARCHAR(100) NOT NULL,
    CONSTRAINT pk_specialization PRIMARY KEY (spec_id),
    CONSTRAINT uq_spec_name UNIQUE (spec_name)
);
```
- **`spec_id`:** مفتاح أساسي تلقائي التوليد (Identity) من نوع `INT`.
- **`spec_name`:** اسم التخصص الأكاديمي (`VARCHAR(100)`). قيد `NOT NULL` يمنع القيمة الفارغة، وقيد `UNIQUE` يمنع تكرار إدخال التخصص نفسه.

### 2.2 جدول `advisor` (المرشدون الأكاديميون)
```sql
CREATE TABLE advisor (
    advisor_id INT GENERATED ALWAYS AS IDENTITY (START WITH 1, INCREMENT BY 1),
    advisor_name VARCHAR(150) NOT NULL,
    CONSTRAINT pk_advisor PRIMARY KEY (advisor_id),
    CONSTRAINT uq_advisor_name UNIQUE (advisor_name)
);
```
- **`advisor_id`:** مفتاح أساسي تلقائي.
- **`advisor_name`:** اسم عضو هيئة التدريس (`VARCHAR(150)`). فريد ومطلوب لمنع تكرار أسماء المرشدين.

### 2.3 جدول `organization` (جهات ومؤسسات التدريب)
```sql
CREATE TABLE organization (
    org_id INT GENERATED ALWAYS AS IDENTITY (START WITH 1, INCREMENT BY 1),
    org_name VARCHAR(200) NOT NULL,
    CONSTRAINT pk_organization PRIMARY KEY (org_id),
    CONSTRAINT uq_org_name UNIQUE (org_name)
);
```
- **`org_id`:** مفتاح أساسي تلقائي.
- **`org_name`:** اسم المنشأة أو الشركة أو المستشفى (`VARCHAR(200)`). فريد ومطلوب لضمان توحيد جهات التدريب.

### 2.4 جدول `student` (سجلات الطلاب)
```sql
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
```
- **`student_id`:** رقم السجل الداخلي في النظام (Internal Primary Key).
- **`full_name`:** اسم الطالب أو الطالبة كاملاً.
- **`student_section`:** الشطر ("طالب" أو "طالبة").
- **`spec_id`, `advisor_id`, `org_id`:** مفاتيح أجنبية لربط الطالب بالجداول المرجعية الثلاثة.

---

## 3. الفهارس وتحسين الأداء (Indexing)

تم إنشاء فهارس مخصصة لتسريع استعلامات البحث والتصفية وعمليات الربط (`JOIN`):
```sql
CREATE INDEX idx_student_name ON student(full_name);
CREATE INDEX idx_student_spec ON student(spec_id);
CREATE INDEX idx_student_advisor ON student(advisor_id);
CREATE INDEX idx_student_org ON student(org_id);
```
- **فهرس الاسم (`idx_student_name`):** لتسريع البحث الجزئي بالاسم (`LIKE %query%`).
- **فهارس المفاتيح الأجنبية:** لتسريع عمليات الربط عند استرجاع الطلاب مع مراجعهم، وتسريع التصفية المجمعة وحساب الإحصائيات.

---

## 4. حماية البيانات والمعاملات (JDBC Transactions)

1. **حماية السجلات المرجعية (`ON DELETE RESTRICT`):**
   - يمنع المحرك حذف أي تخصص أو مرشد أو جهة تدريب مسجل تحتها طلاب، وتلتقط طبقة `ReferenceDataService` ذلك وتعرض رسالة واضحة للمستخدم بعدد الطلاب المرتبطين.
2. **المعاملات الآمنة أثناء الاستيراد (Transactions):**
   - في عملية الاستيراد من ملفات Excel و CSV، يتم تعطيل الحفظ التلقائي:
     `connection.setAutoCommit(false);`
   - يتم إدخال جميع السجلات الصالحة، وفي حال حدوث أي خطأ طارئ، يتم التراجع الفوري:
     `connection.rollback();`
   - في حال نجاح السجلات بالكامل، يتم الاعتماد النهائي:
     `connection.commit();`
   - هذا يضمن أن قاعدة البيانات إما أن تستورد البيانات بنجاح كامل أو تظل نظيفة ومتسقة تماماً (All or Nothing).

