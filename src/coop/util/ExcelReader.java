package coop.util;

import coop.exception.DataAccessException;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;

/**
 * فئة لقراءة ملفات Excel بصيغتيها الحديثة (.xlsx) والقديمة (.xls) باستخدام Apache POI.
 * مسؤولة عن اكتشاف أوراق العمل، تحديد صف الترويسة، تحويل أنواع الخلايا المختلفة،
 * وإرجاع البيانات كنصوص منظمة.
 */
public class ExcelReader {

    private final File file;
    private List<String> headers = new ArrayList<>();
    private List<List<String>> rows = new ArrayList<>();
    private String activeSheetName = "";

    public ExcelReader(File file) {
        this.file = file;
    }

    /**
     * استرجاع أسماء جميع أوراق العمل (Sheets) في ملف Excel دون قراءة البيانات كاملة.
     */
    public static List<String> getSheetNames(File file) {
        if (file == null || !file.exists() || !file.isFile()) {
            throw new DataAccessException("ملف Excel غير موجود: " + (file != null ? file.getAbsolutePath() : "null"));
        }
        List<String> names = new ArrayList<>();
        try (Workbook workbook = WorkbookFactory.create(file)) {
            int numberOfSheets = workbook.getNumberOfSheets();
            for (int i = 0; i < numberOfSheets; i++) {
                names.add(workbook.getSheetName(i));
            }
        } catch (Exception e) {
            throw new DataAccessException("فشل فتح ملف Excel واستخراج أوراق العمل: " + e.getMessage(), e);
        }
        return names;
    }

    /**
     * قراءة ورقة العمل المحددة بالاسم. إذا كان الاسم فارغاً، تُقرأ الورقة الأولى تلقائياً.
     */
    public void readSheet(String sheetName) {
        if (!file.exists() || !file.isFile()) {
            throw new DataAccessException("ملف Excel غير موجود: " + file.getAbsolutePath());
        }

        headers.clear();
        rows.clear();

        try (Workbook workbook = WorkbookFactory.create(file)) {
            Sheet sheet;
            if (sheetName == null || sheetName.trim().isEmpty()) {
                sheet = workbook.getSheetAt(0);
            } else {
                sheet = workbook.getSheet(sheetName.trim());
                if (sheet == null) {
                    throw new DataAccessException("ورقة العمل المطلوبة غير موجودة في الملف: " + sheetName);
                }
            }

            this.activeSheetName = sheet.getSheetName();
            int firstRowNum = sheet.getFirstRowNum();
            int lastRowNum = sheet.getLastRowNum();

            if (firstRowNum < 0 || lastRowNum < firstRowNum) {
                throw new DataAccessException("ورقة العمل المحددة فارغة ولا تحتوي على بيانات: " + activeSheetName);
            }

            // البحث عن أول صف غير فارغ لاعتباره صف العناوين (Header)
            int headerRowIndex = -1;
            int maxCols = 0;

            for (int r = firstRowNum; r <= lastRowNum; r++) {
                Row row = sheet.getRow(r);
                if (row != null && isRowNotEmpty(row)) {
                    headerRowIndex = r;
                    maxCols = row.getLastCellNum();
                    for (int c = 0; c < maxCols; c++) {
                        Cell cell = row.getCell(c, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
                        headers.add(cell != null ? ValidationUtil.clean(getCellValueAsString(cell)) : "");
                    }
                    break;
                }
            }

            if (headerRowIndex < 0 || headers.isEmpty()) {
                throw new DataAccessException("تعذر العثور على صف ترويسة يحتوي على عناوين أعمدة صالحة.");
            }

            // قراءة صفوف البيانات التالية لصف العناوين
            for (int r = headerRowIndex + 1; r <= lastRowNum; r++) {
                Row row = sheet.getRow(r);
                if (row == null) continue;

                List<String> rowValues = new ArrayList<>();
                boolean rowHasContent = false;

                for (int c = 0; c < maxCols; c++) {
                    Cell cell = row.getCell(c, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
                    String val = cell != null ? ValidationUtil.clean(getCellValueAsString(cell)) : "";
                    rowValues.add(val);
                    if (!val.isEmpty()) {
                        rowHasContent = false || true;
                    }
                }

                // إضافة الصف فقط إذا كان يحتوي على قيمة واحدة على الأقل
                if (rowHasContent) {
                    rows.add(rowValues);
                }
            }

        } catch (Exception e) {
            throw new DataAccessException("حدث خطأ أثناء قراءة ملف Excel: " + e.getMessage(), e);
        }
    }

    private boolean isRowNotEmpty(Row row) {
        if (row == null) return false;
        for (int c = 0; c < row.getLastCellNum(); c++) {
            Cell cell = row.getCell(c);
            if (cell != null && !getCellValueAsString(cell).trim().isEmpty()) {
                return true;
            }
        }
        return false;
    }

    /**
     * تحويل قيمة الخلية بمختلف أنواعها (نص، رقم، تاريخ، معادلة، منطقي) إلى نص منسق.
     */
    private String getCellValueAsString(Cell cell) {
        if (cell == null) return "";

        CellType type = cell.getCellType();
        if (type == CellType.FORMULA) {
            type = cell.getCachedFormulaResultType();
        }

        switch (type) {
            case STRING:
                return cell.getStringCellValue();

            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    Date date = cell.getDateCellValue();
                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
                    return sdf.format(date);
                } else {
                    double num = cell.getNumericCellValue();
                    // إذا كان الرقم عدداً صحيحاً، أزل الفاصلة العشرية والصفر .0
                    if (num == Math.floor(num) && !Double.isInfinite(num)) {
                        return String.valueOf((long) num);
                    } else {
                        return String.valueOf(num);
                    }
                }

            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());

            case BLANK:
                return "";

            default:
                return "";
        }
    }

    public List<String> getHeaders() {
        return headers;
    }

    public List<List<String>> getRows() {
        return rows;
    }

    public String getActiveSheetName() {
        return activeSheetName;
    }
}

