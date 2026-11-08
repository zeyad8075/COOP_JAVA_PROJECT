package coop.service;

import coop.dao.AdvisorDAO;
import coop.dao.DBConnection;
import coop.dao.OrganizationDAO;
import coop.dao.SpecializationDAO;
import coop.dao.StudentDAO;
import coop.exception.DataAccessException;
import coop.model.Advisor;
import coop.model.ImportExecutionReport;
import coop.model.ImportPreviewResult;
import coop.model.ImportRow;
import coop.model.Organization;
import coop.model.Specialization;
import coop.util.CSVReader;
import coop.util.ExcelReader;
import coop.util.ValidationUtil;
import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * خدمة تنسيق وإدارة استيراد ملفات Excel و CSV.
 * مسؤولة عن:
 * 1. المطابقة التلقائية واليدوية للأعمدة مع حقول النظام.
 * 2. المعاينة والفحص الدقيق واكتشاف التكرارات والقيم الناقصة.
 * 3. اكتشاف القيم المتشابهة واقتراح توحيدها.
 * 4. تنفيذ الاستيراد كمعاملة آمنة (JDBC Transaction مع commit و rollback).
 */
public class ImportService {

    // الحقول الأساسية للنظام
    public static final String FIELD_NAME = "full_name";
    public static final String FIELD_SECTION = "student_section";
    public static final String FIELD_SPEC = "specialization";
    public static final String FIELD_ADVISOR = "advisor";
    public static final String FIELD_ORG = "organization";

    private final StudentDAO studentDAO = new StudentDAO();
    private final SpecializationDAO specDAO = new SpecializationDAO();
    private final AdvisorDAO advisorDAO = new AdvisorDAO();
    private final OrganizationDAO orgDAO = new OrganizationDAO();
    private final DataNormalizationService normService = new DataNormalizationService();

    /**
     * اكتشاف مطابقة الأعمدة تلقائياً بناءً على الكلمات المفتاحية باللغتين العربية والإنجليزية.
     */
    public Map<String, String> detectColumnMapping(List<String> fileHeaders) {
        Map<String, String> mapping = new LinkedHashMap<>();
        if (fileHeaders == null) return mapping;

        for (String h : fileHeaders) {
            String norm = ValidationUtil.clean(h).toLowerCase();

            if (!mapping.containsKey(FIELD_NAME) && (norm.contains("اسم") || norm.contains("إسم") || norm.contains("طالب") || norm.equals("name"))) {
                mapping.put(FIELD_NAME, h);
            } else if (!mapping.containsKey(FIELD_SECTION) && (norm.contains("شطر") || norm.contains("قسم") || norm.contains("جنس") || norm.equals("section"))) {
                mapping.put(FIELD_SECTION, h);
            } else if (!mapping.containsKey(FIELD_SPEC) && (norm.contains("تخصص") || norm.contains("المسار") || norm.equals("major") || norm.equals("specialization"))) {
                mapping.put(FIELD_SPEC, h);
            } else if (!mapping.containsKey(FIELD_ADVISOR) && (norm.contains("مرشد") || norm.contains("مشرف") || norm.contains("أكاديم") || norm.equals("advisor"))) {
                mapping.put(FIELD_ADVISOR, h);
            } else if (!mapping.containsKey(FIELD_ORG) && (norm.contains("جهة") || norm.contains("مؤسسة") || norm.contains("تدريب") || norm.contains("شركة") || norm.equals("organization") || norm.equals("company"))) {
                mapping.put(FIELD_ORG, h);
            }
        }
        return mapping;
    }

    /**
     * معاينة وفحص ملف Excel قبل الاستيراد.
     */
    public ImportPreviewResult previewExcel(File file, String sheetName, Map<String, String> userMapping) {
        ExcelReader reader = new ExcelReader(file);
        reader.readSheet(sheetName);
        return processPreview(file.getName(), reader.getActiveSheetName(), reader.getHeaders(), reader.getRows(), userMapping);
    }

    /**
     * معاينة وفحص ملف CSV قبل الاستيراد.
     */
    public ImportPreviewResult previewCSV(File file, Map<String, String> userMapping) {
        CSVReader reader = new CSVReader(file);
        reader.read();
        return processPreview(file.getName(), "CSV", reader.getHeaders(), reader.getRows(), userMapping);
    }

    /**
     * المعالجة المشتركة لفحص البيانات وتنظيفها وتصنيف حالتها واكتشاف التكرارات والتشابهات.
     */
    private ImportPreviewResult processPreview(String fileName, String sheetName,
                                               List<String> headers, List<List<String>> rowsData,
                                               Map<String, String> userMapping) {
        ImportPreviewResult result = new ImportPreviewResult();
        result.setFileName(fileName);
        result.setSheetName(sheetName);
        result.setFileHeaders(headers);

        // استخدام المطابقة المحددة من المستخدم أو الاكتشاف التلقائي
        Map<String, String> mapping = (userMapping != null && !userMapping.isEmpty())
                ? new LinkedHashMap<>(userMapping)
                : detectColumnMapping(headers);
        result.setColumnMapping(mapping);

        // تحديد الأعمدة غير المرتبطة بحقول النظام
        Set<String> mappedHeaderValues = new HashSet<>(mapping.values());
        List<String> unmapped = new ArrayList<>();
        for (String h : headers) {
            if (!mappedHeaderValues.contains(h)) {
                unmapped.add(h);
            }
        }
        result.setUnmappedHeaders(unmapped);

        // خريطة سريعة لمواقع الأعمدة بالترتيب
        Map<String, Integer> headerIndexMap = new HashMap<>();
        for (int i = 0; i < headers.size(); i++) {
            headerIndexMap.put(headers.get(i), i);
        }

        String nameCol = mapping.get(FIELD_NAME);
        String sectionCol = mapping.get(FIELD_SECTION);
        String specCol = mapping.get(FIELD_SPEC);
        String advisorCol = mapping.get(FIELD_ADVISOR);
        String orgCol = mapping.get(FIELD_ORG);

        // تجميع القيم المرجعية لفحص التشابه
        Set<String> fileOrgs = new HashSet<>();
        Set<String> fileAdvisors = new HashSet<>();
        Set<String> fileSpecs = new HashSet<>();
        Set<String> seenNamesInFile = new HashSet<>();

        List<ImportRow> importRows = new ArrayList<>();
        int validCnt = 0;
        int incompleteCnt = 0;
        int duplicateCnt = 0;

        for (int r = 0; r < rowsData.size(); r++) {
            List<String> rowList = rowsData.get(r);
            ImportRow impRow = new ImportRow(r + 1);

            // حفظ البيانات الخام
            for (int c = 0; c < headers.size() && c < rowList.size(); c++) {
                impRow.getRawData().put(headers.get(c), rowList.get(c));
            }

            // استخراج وتعيين القيم
            String rawName = getValue(rowList, headerIndexMap, nameCol);
            String rawSection = getValue(rowList, headerIndexMap, sectionCol);
            String rawSpec = getValue(rowList, headerIndexMap, specCol);
            String rawAdvisor = getValue(rowList, headerIndexMap, advisorCol);
            String rawOrg = getValue(rowList, headerIndexMap, orgCol);

            impRow.setOriginalName(rawName);
            impRow.setOriginalSection(rawSection);
            impRow.setOriginalSpec(rawSpec);
            impRow.setOriginalAdvisor(rawAdvisor);
            impRow.setOriginalOrg(rawOrg);

            // تنظيف القيم
            String clnName = ValidationUtil.clean(rawName);
            String clnSection = ValidationUtil.clean(rawSection);
            String clnSpec = ValidationUtil.clean(rawSpec);
            String clnAdvisor = ValidationUtil.clean(rawAdvisor);
            String clnOrg = ValidationUtil.clean(rawOrg);

            impRow.setCleanedName(clnName);
            impRow.setCleanedSection(clnSection);
            impRow.setCleanedSpec(clnSpec);
            impRow.setCleanedAdvisor(clnAdvisor);
            impRow.setCleanedOrg(clnOrg);

            // 1. التحقق من اكتمال الحقول الإلزامية
            boolean incomplete = false;
            if (ValidationUtil.isEmpty(clnName)) {
                impRow.addValidationMessage("اسم الطالب مفقود أو فارغ.");
                incomplete = true;
            }
            if (ValidationUtil.isEmpty(clnSpec)) {
                impRow.addValidationMessage("التخصص الأكاديمي غير محدد.");
                incomplete = true;
            }
            if (ValidationUtil.isEmpty(clnAdvisor)) {
                impRow.addValidationMessage("المرشد الأكاديمي غير محدد.");
                incomplete = true;
            }
            if (ValidationUtil.isEmpty(clnOrg)) {
                impRow.addValidationMessage("جهة التدريب غير محددة.");
                incomplete = true;
            }

            if (incomplete) {
                impRow.setStatus(ImportRow.RowStatus.INCOMPLETE);
                impRow.setSelectedForImport(false);
                incompleteCnt++;
            } else {
                // 2. التحقق من التكرار المحتمل (داخل الملف أو في قاعدة البيانات)
                String normName = ValidationUtil.normalizeArabicForComparison(clnName);
                if (seenNamesInFile.contains(normName)) {
                    impRow.setStatus(ImportRow.RowStatus.POTENTIAL_DUPLICATE);
                    impRow.addValidationMessage("يوجد طالب آخر بالاسم نفسه مكرر داخل هذا الملف.");
                    duplicateCnt++;
                } else if (studentDAO.existsByFullName(clnName)) {
                    impRow.setStatus(ImportRow.RowStatus.POTENTIAL_DUPLICATE);
                    impRow.addValidationMessage("يوجد طالب مسجل مسبقاً في قاعدة البيانات بالاسم نفسه.");
                    duplicateCnt++;
                } else {
                    impRow.setStatus(ImportRow.RowStatus.VALID);
                    validCnt++;
                }
                seenNamesInFile.add(normName);

                if (!clnOrg.isEmpty()) fileOrgs.add(clnOrg);
                if (!clnAdvisor.isEmpty()) fileAdvisors.add(clnAdvisor);
                if (!clnSpec.isEmpty()) fileSpecs.add(clnSpec);
            }

            importRows.add(impRow);
        }

        result.setRows(importRows);
        result.setTotalRows(importRows.size());
        result.setValidCount(validCnt);
        result.setIncompleteCount(incompleteCnt);
        result.setDuplicateCount(duplicateCnt);

        // 3. اكتشاف القيم المتشابهة واقتراح التوحيد
        List<String> dbOrgs = fetchAllDbOrgNames();
        List<String> dbAdvisors = fetchAllDbAdvisorNames();
        List<String> dbSpecs = fetchAllDbSpecNames();

        result.setOrganizationSuggestions(normService.findSimilarGroups(fileOrgs, dbOrgs));
        result.setAdvisorSuggestions(normService.findSimilarGroups(fileAdvisors, dbAdvisors));
        result.setSpecializationSuggestions(normService.findSimilarGroups(fileSpecs, dbSpecs));

        return result;
    }

    private String getValue(List<String> row, Map<String, Integer> headerMap, String colName) {
        if (colName == null || !headerMap.containsKey(colName)) return "";
        int idx = headerMap.get(colName);
        if (idx >= 0 && idx < row.size()) {
            return row.get(idx);
        }
        return "";
    }

    /**
     * تنفيذ الاستيراد الفعلي لجميع السجلات الصالحة والمختارة ضمن JDBC Transaction آمنة.
     * في حال حدوث أي خطأ غير متوقع، يتم التراجع الفوري (rollback) للحفاظ على سلامة البيانات.
     */
    public ImportExecutionReport executeImport(ImportPreviewResult preview, Map<String, String> approvedNormalizations) {
        ImportExecutionReport report = new ImportExecutionReport(preview.getFileName());

        List<ImportRow> rowsToImport = new ArrayList<>();
        for (ImportRow r : preview.getRows()) {
            if (r.isSelectedForImport() && r.getStatus() != ImportRow.RowStatus.INCOMPLETE) {
                rowsToImport.add(r);
            } else {
                report.setSkippedOrExcluded(report.getSkippedOrExcluded() + 1);
            }
        }

        report.setTotalRowsProcessed(preview.getTotalRows());

        if (rowsToImport.isEmpty()) {
            report.addErrorMessage("لم يتم تحديد أي سجلات صالحة للاستيراد.");
            return report;
        }

        // تنفيذ المعاملة الآمنة
        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false); // بدء المعاملة Transaction

            try {
                // كاش لتسريع العمليات وتجنب تكرار استعلامات المراجع
                Map<String, Integer> specCache = new HashMap<>();
                Map<String, Integer> advisorCache = new HashMap<>();
                Map<String, Integer> orgCache = new HashMap<>();

                for (ImportRow row : rowsToImport) {
                    // تطبيق التوحيد إن وجد
                    String finalSpec = normService.resolveNormalizedValue(row.getCleanedSpec(), approvedNormalizations);
                    String finalAdvisor = normService.resolveNormalizedValue(row.getCleanedAdvisor(), approvedNormalizations);
                    String finalOrg = normService.resolveNormalizedValue(row.getCleanedOrg(), approvedNormalizations);
                    String finalSection = row.getCleanedSection().isEmpty() ? "طالب" : row.getCleanedSection();
                    String finalName = row.getCleanedName();

                    // الحصول على المعرف أو إنشاؤه داخل المعاملة
                    int specId = resolveOrCreateSpec(conn, finalSpec, specCache, report);
                    int advisorId = resolveOrCreateAdvisor(conn, finalAdvisor, advisorCache, report);
                    int orgId = resolveOrCreateOrg(conn, finalOrg, orgCache, report);

                    // إدخال الطالب
                    insertStudent(conn, finalName, finalSection, specId, advisorId, orgId);
                    report.setSuccessfullyImported(report.getSuccessfullyImported() + 1);
                }

                conn.commit(); // اعتماد جميع العمليات بنجاح
            } catch (Exception ex) {
                conn.rollback(); // التراجع الكامل في حال حدوث خطأ
                report.setFailed(rowsToImport.size());
                report.setSuccessfullyImported(0);
                report.addErrorMessage("تم إلغاء العملية والتراجع عن التغييرات بسبب خطأ: " + ex.getMessage());
            } finally {
                conn.setAutoCommit(true);
            }

        } catch (SQLException e) {
            throw new DataAccessException("فشل الاتصال بقاعدة البيانات أثناء تنفيذ الاستيراد: " + e.getMessage(), e);
        }

        return report;
    }

    private int resolveOrCreateSpec(Connection conn, String name, Map<String, Integer> cache, ImportExecutionReport report) throws SQLException {
        String key = name.toLowerCase().trim();
        if (cache.containsKey(key)) return cache.get(key);

        String sel = "SELECT spec_id FROM specialization WHERE LOWER(TRIM(spec_name)) = LOWER(TRIM(?))";
        try (PreparedStatement ps = conn.prepareStatement(sel)) {
            ps.setString(1, name.trim());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    int id = rs.getInt(1);
                    cache.put(key, id);
                    return id;
                }
            }
        }

        // إنشاء جديد
        String ins = "INSERT INTO specialization (spec_name) VALUES (?)";
        try (PreparedStatement ps = conn.prepareStatement(ins, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, name.trim());
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                rs.next();
                int id = rs.getInt(1);
                cache.put(key, id);
                report.getNewSpecializations().add(name.trim());
                return id;
            }
        }
    }

    private int resolveOrCreateAdvisor(Connection conn, String name, Map<String, Integer> cache, ImportExecutionReport report) throws SQLException {
        String key = name.toLowerCase().trim();
        if (cache.containsKey(key)) return cache.get(key);

        String sel = "SELECT advisor_id FROM advisor WHERE LOWER(TRIM(advisor_name)) = LOWER(TRIM(?))";
        try (PreparedStatement ps = conn.prepareStatement(sel)) {
            ps.setString(1, name.trim());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    int id = rs.getInt(1);
                    cache.put(key, id);
                    return id;
                }
            }
        }

        String ins = "INSERT INTO advisor (advisor_name) VALUES (?)";
        try (PreparedStatement ps = conn.prepareStatement(ins, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, name.trim());
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                rs.next();
                int id = rs.getInt(1);
                cache.put(key, id);
                report.getNewAdvisors().add(name.trim());
                return id;
            }
        }
    }

    private int resolveOrCreateOrg(Connection conn, String name, Map<String, Integer> cache, ImportExecutionReport report) throws SQLException {
        String key = name.toLowerCase().trim();
        if (cache.containsKey(key)) return cache.get(key);

        String sel = "SELECT org_id FROM organization WHERE LOWER(TRIM(org_name)) = LOWER(TRIM(?))";
        try (PreparedStatement ps = conn.prepareStatement(sel)) {
            ps.setString(1, name.trim());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    int id = rs.getInt(1);
                    cache.put(key, id);
                    return id;
                }
            }
        }

        String ins = "INSERT INTO organization (org_name) VALUES (?)";
        try (PreparedStatement ps = conn.prepareStatement(ins, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, name.trim());
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                rs.next();
                int id = rs.getInt(1);
                cache.put(key, id);
                report.getNewOrganizations().add(name.trim());
                return id;
            }
        }
    }

    private void insertStudent(Connection conn, String name, String section, int specId, int advisorId, int orgId) throws SQLException {
        String sql = "INSERT INTO student (full_name, student_section, spec_id, advisor_id, org_id) VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, name);
            ps.setString(2, section);
            ps.setInt(3, specId);
            ps.setInt(4, advisorId);
            ps.setInt(5, orgId);
            ps.executeUpdate();
        }
    }

    private List<String> fetchAllDbOrgNames() {
        List<String> list = new ArrayList<>();
        for (Organization o : orgDAO.findAll()) {
            list.add(o.getOrgName());
        }
        return list;
    }

    private List<String> fetchAllDbAdvisorNames() {
        List<String> list = new ArrayList<>();
        for (Advisor a : advisorDAO.findAll()) {
            list.add(a.getAdvisorName());
        }
        return list;
    }

    private List<String> fetchAllDbSpecNames() {
        List<String> list = new ArrayList<>();
        for (Specialization s : specDAO.findAll()) {
            list.add(s.getSpecName());
        }
        return list;
    }
}

