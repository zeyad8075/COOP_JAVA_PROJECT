package coop.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * يمثل صفاً واحداً مستخرجاً من ملف البيانات أثناء مرحلة المعاينة والفحص.
 * يحتفظ بالبيانات الأصلية والبيانات المنظفة وحالة الفحص والملاحظات.
 */
public class ImportRow implements Serializable {

    public enum RowStatus {
        VALID("صالح للاستيراد"),
        POTENTIAL_DUPLICATE("مكرر محتمل"),
        INCOMPLETE("بيانات ناقصة"),
        SIMILAR_VALUE_REVIEW("يحتاج مراجعة توحيد"),
        INVALID("غير صالح");

        private final String displayName;

        RowStatus(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    private int rowNumber;
    private Map<String, String> rawData = new LinkedHashMap<>();

    private String originalName;
    private String originalSection;
    private String originalSpec;
    private String originalAdvisor;
    private String originalOrg;

    private String cleanedName;
    private String cleanedSection;
    private String cleanedSpec;
    private String cleanedAdvisor;
    private String cleanedOrg;

    private RowStatus status = RowStatus.VALID;
    private List<String> validationMessages = new ArrayList<>();
    private boolean selectedForImport = true;

    public ImportRow() {
    }

    public ImportRow(int rowNumber) {
        this.rowNumber = rowNumber;
    }

    public int getRowNumber() {
        return rowNumber;
    }

    public void setRowNumber(int rowNumber) {
        this.rowNumber = rowNumber;
    }

    public Map<String, String> getRawData() {
        return rawData;
    }

    public void setRawData(Map<String, String> rawData) {
        this.rawData = rawData;
    }

    public String getOriginalName() {
        return originalName;
    }

    public void setOriginalName(String originalName) {
        this.originalName = originalName;
    }

    public String getOriginalSection() {
        return originalSection;
    }

    public void setOriginalSection(String originalSection) {
        this.originalSection = originalSection;
    }

    public String getOriginalSpec() {
        return originalSpec;
    }

    public void setOriginalSpec(String originalSpec) {
        this.originalSpec = originalSpec;
    }

    public String getOriginalAdvisor() {
        return originalAdvisor;
    }

    public void setOriginalAdvisor(String originalAdvisor) {
        this.originalAdvisor = originalAdvisor;
    }

    public String getOriginalOrg() {
        return originalOrg;
    }

    public void setOriginalOrg(String originalOrg) {
        this.originalOrg = originalOrg;
    }

    public String getCleanedName() {
        return cleanedName;
    }

    public void setCleanedName(String cleanedName) {
        this.cleanedName = cleanedName;
    }

    public String getCleanedSection() {
        return cleanedSection;
    }

    public void setCleanedSection(String cleanedSection) {
        this.cleanedSection = cleanedSection;
    }

    public String getCleanedSpec() {
        return cleanedSpec;
    }

    public void setCleanedSpec(String cleanedSpec) {
        this.cleanedSpec = cleanedSpec;
    }

    public String getCleanedAdvisor() {
        return cleanedAdvisor;
    }

    public void setCleanedAdvisor(String cleanedAdvisor) {
        this.cleanedAdvisor = cleanedAdvisor;
    }

    public String getCleanedOrg() {
        return cleanedOrg;
    }

    public void setCleanedOrg(String cleanedOrg) {
        this.cleanedOrg = cleanedOrg;
    }

    public RowStatus getStatus() {
        return status;
    }

    public void setStatus(RowStatus status) {
        this.status = status;
    }

    public List<String> getValidationMessages() {
        return validationMessages;
    }

    public void addValidationMessage(String message) {
        this.validationMessages.add(message);
    }

    public boolean isSelectedForImport() {
        return selectedForImport;
    }

    public void setSelectedForImport(boolean selectedForImport) {
        this.selectedForImport = selectedForImport;
    }
}

