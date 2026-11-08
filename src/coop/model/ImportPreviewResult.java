package coop.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * يمثل نتيجة معاينة وفحص ملف البيانات قبل الاعتماد النهائي للاستيراد.
 * يتضمن الإحصائيات الشاملة، السجلات المعاينة، الأعمدة غير المرتبطة، واقتراحات التوحيد.
 */
public class ImportPreviewResult implements Serializable {

    private String fileName;
    private String sheetName;
    private List<String> fileHeaders = new ArrayList<>();
    private List<String> unmappedHeaders = new ArrayList<>();
    private Map<String, String> columnMapping = new LinkedHashMap<>();

    private List<ImportRow> rows = new ArrayList<>();

    private int totalRows = 0;
    private int validCount = 0;
    private int incompleteCount = 0;
    private int duplicateCount = 0;
    private int similarReviewCount = 0;

    // اقتراحات توحيد القيم: (الاسم الموحد المقترح -> قائمة بالصيغ المختلفة الموجودة)
    private Map<String, List<String>> organizationSuggestions = new LinkedHashMap<>();
    private Map<String, List<String>> advisorSuggestions = new LinkedHashMap<>();
    private Map<String, List<String>> specializationSuggestions = new LinkedHashMap<>();

    public ImportPreviewResult() {
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getSheetName() {
        return sheetName;
    }

    public void setSheetName(String sheetName) {
        this.sheetName = sheetName;
    }

    public List<String> getFileHeaders() {
        return fileHeaders;
    }

    public void setFileHeaders(List<String> fileHeaders) {
        this.fileHeaders = fileHeaders;
    }

    public List<String> getUnmappedHeaders() {
        return unmappedHeaders;
    }

    public void setUnmappedHeaders(List<String> unmappedHeaders) {
        this.unmappedHeaders = unmappedHeaders;
    }

    public Map<String, String> getColumnMapping() {
        return columnMapping;
    }

    public void setColumnMapping(Map<String, String> columnMapping) {
        this.columnMapping = columnMapping;
    }

    public List<ImportRow> getRows() {
        return rows;
    }

    public void setRows(List<ImportRow> rows) {
        this.rows = rows;
    }

    public int getTotalRows() {
        return totalRows;
    }

    public void setTotalRows(int totalRows) {
        this.totalRows = totalRows;
    }

    public int getValidCount() {
        return validCount;
    }

    public void setValidCount(int validCount) {
        this.validCount = validCount;
    }

    public int getIncompleteCount() {
        return incompleteCount;
    }

    public void setIncompleteCount(int incompleteCount) {
        this.incompleteCount = incompleteCount;
    }

    public int getDuplicateCount() {
        return duplicateCount;
    }

    public void setDuplicateCount(int duplicateCount) {
        this.duplicateCount = duplicateCount;
    }

    public int getSimilarReviewCount() {
        return similarReviewCount;
    }

    public void setSimilarReviewCount(int similarReviewCount) {
        this.similarReviewCount = similarReviewCount;
    }

    public Map<String, List<String>> getOrganizationSuggestions() {
        return organizationSuggestions;
    }

    public void setOrganizationSuggestions(Map<String, List<String>> organizationSuggestions) {
        this.organizationSuggestions = organizationSuggestions;
    }

    public Map<String, List<String>> getAdvisorSuggestions() {
        return advisorSuggestions;
    }

    public void setAdvisorSuggestions(Map<String, List<String>> advisorSuggestions) {
        this.advisorSuggestions = advisorSuggestions;
    }

    public Map<String, List<String>> getSpecializationSuggestions() {
        return specializationSuggestions;
    }

    public void setSpecializationSuggestions(Map<String, List<String>> specializationSuggestions) {
        this.specializationSuggestions = specializationSuggestions;
    }
}

