package coop.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * يمثل التقرير النهائي الصادر بعد تنفيذ عملية استيراد البيانات إلى قاعدة البيانات.
 * يوثق عدد السجلات المستوردة، المستبعدة، الفاشلة، والمراجع الجديدة التي تم إنشاؤها.
 */
public class ImportExecutionReport implements Serializable {

    private String fileName;
    private int totalRowsProcessed = 0;
    private int successfullyImported = 0;
    private int skippedOrExcluded = 0;
    private int failed = 0;

    private List<String> newOrganizations = new ArrayList<>();
    private List<String> newAdvisors = new ArrayList<>();
    private List<String> newSpecializations = new ArrayList<>();
    private List<String> errorMessages = new ArrayList<>();

    public ImportExecutionReport() {
    }

    public ImportExecutionReport(String fileName) {
        this.fileName = fileName;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public int getTotalRowsProcessed() {
        return totalRowsProcessed;
    }

    public void setTotalRowsProcessed(int totalRowsProcessed) {
        this.totalRowsProcessed = totalRowsProcessed;
    }

    public int getSuccessfullyImported() {
        return successfullyImported;
    }

    public void setSuccessfullyImported(int successfullyImported) {
        this.successfullyImported = successfullyImported;
    }

    public int getSkippedOrExcluded() {
        return skippedOrExcluded;
    }

    public void setSkippedOrExcluded(int skippedOrExcluded) {
        this.skippedOrExcluded = skippedOrExcluded;
    }

    public int getFailed() {
        return failed;
    }

    public void setFailed(int failed) {
        this.failed = failed;
    }

    public List<String> getNewOrganizations() {
        return newOrganizations;
    }

    public List<String> getNewAdvisors() {
        return newAdvisors;
    }

    public List<String> getNewSpecializations() {
        return newSpecializations;
    }

    public List<String> getErrorMessages() {
        return errorMessages;
    }

    public void addErrorMessage(String msg) {
        this.errorMessages.add(msg);
    }

    public boolean isSuccess() {
        return failed == 0 && successfullyImported > 0;
    }

    /**
     * إخراج ملخص نصي منسق للعرض في رسائل التأكيد والتقارير.
     */
    public String getFormattedSummary() {
        StringBuilder sb = new StringBuilder();
        sb.append("تقرير استيراد البيانات:\n");
        sb.append("----------------------------------------\n");
        sb.append("اسم الملف: ").append(fileName).append("\n");
        sb.append("إجمالي السجلات المعالجة: ").append(totalRowsProcessed).append("\n");
        sb.append("الطلاب المستوردون بنجاح: ").append(successfullyImported).append("\n");
        sb.append("السجلات المستبعدة أو التي تم تخطيها: ").append(skippedOrExcluded).append("\n");
        sb.append("السجلات الفاشلة: ").append(failed).append("\n");
        sb.append("----------------------------------------\n");
        sb.append("تخصصات جديدة تم إنشاؤها: ").append(newSpecializations.size()).append("\n");
        sb.append("مرشدون أكاديميون جدد: ").append(newAdvisors.size()).append("\n");
        sb.append("جهات تدريب جديدة: ").append(newOrganizations.size()).append("\n");

        if (!errorMessages.isEmpty()) {
            sb.append("\nتفاصيل الأخطاء والملاحظات:\n");
            for (String err : errorMessages) {
                sb.append(" - ").append(err).append("\n");
            }
        }
        return sb.toString();
    }
}

