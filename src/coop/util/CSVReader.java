package coop.util;

import coop.exception.DataAccessException;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * فئة لقراءة ملفات CSV بترميز UTF-8 مع دعم الحقول المقتبسة والفواصل المتعددة (فاصلة، فاصلة منقوطة، Tab).
 * تحافظ على النصوص العربية كاملة وتتجاهل علامة BOM إن وجدت.
 */
public class CSVReader {

    private final File file;
    private char delimiter = ',';
    private List<String> headers = new ArrayList<>();
    private List<List<String>> rows = new ArrayList<>();

    public CSVReader(File file) {
        this.file = file;
    }

    public CSVReader(File file, char delimiter) {
        this.file = file;
        this.delimiter = delimiter;
    }

    /**
     * قراءة الملف بالكامل واستخراج الترويسة والصفوف.
     */
    public void read() {
        if (!file.exists() || !file.isFile()) {
            throw new DataAccessException("ملف CSV غير موجود: " + file.getAbsolutePath());
        }

        headers.clear();
        rows.clear();

        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8))) {

            String line;
            boolean isFirstLine = true;

            while ((line = br.readLine()) != null) {
                // إزالة BOM من أول سطر
                if (isFirstLine && line.startsWith("\uFEFF")) {
                    line = line.substring(1);
                }

                if (line.trim().isEmpty()) {
                    continue;
                }

                // اكتشاف الفاصل تلقائياً في السطر الأول إذا لم يتم تحديده مسبقاً
                if (isFirstLine) {
                    delimiter = detectDelimiter(line);
                    List<String> parsedHeader = parseLine(line, delimiter);
                    for (String h : parsedHeader) {
                        headers.add(ValidationUtil.clean(h));
                    }
                    isFirstLine = false;
                } else {
                    List<String> parsedRow = parseLine(line, delimiter);
                    // تجاهل الصفوف الفارغة بالكامل
                    boolean allEmpty = true;
                    List<String> cleanedRow = new ArrayList<>();
                    for (String cell : parsedRow) {
                        String cln = ValidationUtil.clean(cell);
                        cleanedRow.add(cln);
                        if (!cln.isEmpty()) {
                            allEmpty = false;
                        }
                    }
                    if (!allEmpty) {
                        rows.add(cleanedRow);
                    }
                }
            }

            if (headers.isEmpty()) {
                throw new DataAccessException("ملف CSV فارغ أو لا يحتوي على صف ترويسة صالح.");
            }

        } catch (IOException e) {
            throw new DataAccessException("حدث خطأ أثناء قراءة ملف CSV: " + e.getMessage(), e);
        }
    }

    /**
     * اكتشاف الفاصل المستخدم تلقائياً من خلال عدد تكراره في السطر الأول.
     */
    private char detectDelimiter(String line) {
        int commas = countOccurrences(line, ',');
        int semicolons = countOccurrences(line, ';');
        int tabs = countOccurrences(line, '\t');

        if (semicolons > commas && semicolons > tabs) {
            return ';';
        }
        if (tabs > commas && tabs > semicolons) {
            return '\t';
        }
        return ',';
    }

    private int countOccurrences(String str, char ch) {
        int count = 0;
        for (int i = 0; i < str.length(); i++) {
            if (str.charAt(i) == ch) count++;
        }
        return count;
    }

    /**
     * تفكيك سطر CSV مع دعم علامات الاقتباس والفواصل داخل النصوص.
     */
    private List<String> parseLine(String line, char delim) {
        List<String> tokens = new ArrayList<>();
        StringBuilder sb = new StringBuilder();
        boolean inQuotes = false;

        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);

            if (c == '"') {
                // إذا كان هناك اقتباس مزدوج متتالي داخل نص مقتبس "" يُعتبر اقتباساً مفرداً
                if (inQuotes && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    sb.append('"');
                    i++;
                } else {
                    inQuotes = !inQuotes;
                }
            } else if (c == delim && !inQuotes) {
                tokens.add(sb.toString());
                sb.setLength(0);
            } else {
                sb.append(c);
            }
        }
        tokens.add(sb.toString());
        return tokens;
    }

    public List<String> getHeaders() {
        return headers;
    }

    public List<List<String>> getRows() {
        return rows;
    }

    public char getDelimiter() {
        return delimiter;
    }
}

